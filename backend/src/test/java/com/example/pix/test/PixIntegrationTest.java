package com.example.pix.test;

import com.example.pix.api.CreatePixRequest;
import com.example.pix.api.PixResponse;
import com.example.pix.domain.PixStatus;
import com.example.pix.events.BacenResponseEvent;
import com.example.pix.repo.PixTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PixIntegrationTest extends ContainersConfig {

  @Autowired com.example.pix.service.PixService pixService;
  @Autowired PixTransactionRepository txRepo;
  @Autowired RabbitTemplate rabbit;
  @Autowired ObjectMapper om;

  @Test
  void shouldCreatePix_andConfirmAfterBacenResponse() throws Exception {
    var idem = UUID.randomUUID().toString();
    PixResponse created = pixService.createPix(new CreatePixRequest(
      "123", "email@pix.com", new BigDecimal("10.00"), "Pedido 1001", idem
    ));

    assertThat(created.status()).isIn(PixStatus.APPROVED, PixStatus.FRAUD_REJECTED);

    // Como o mock de fraude aprova valores baixos, esperamos APPROVED
    assertThat(created.status()).isEqualTo(PixStatus.APPROVED);

    // publica resposta "BACEN"
    BacenResponseEvent resp = new BacenResponseEvent(created.id(), "CONFIRMED", "E2E123", "00");
    rabbit.convertAndSend("pix.exchange", "pix.response", om.writeValueAsString(resp));

    Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      var tx = txRepo.findById(created.id()).orElseThrow();
      assertThat(tx.getStatus()).isEqualTo(PixStatus.CONFIRMED);
      assertThat(tx.getEndToEndId()).isEqualTo("E2E123");
    });
  }

  @Test
  void shouldBeIdempotent() {
    String idem = UUID.randomUUID().toString();

    PixResponse one = pixService.createPix(new CreatePixRequest(
      "123", "email@pix.com", new BigDecimal("10.00"), "Pedido 1001", idem
    ));
    PixResponse two = pixService.createPix(new CreatePixRequest(
      "123", "email@pix.com", new BigDecimal("10.00"), "Pedido 1001", idem
    ));

    assertThat(two.id()).isEqualTo(one.id());
  }

  @Test
  void dlqFlow_shouldSendToDlqAfterMaxRetries() throws Exception {
    // cria PIX pra ter transactionId
    String idem = UUID.randomUUID().toString();
    PixResponse created = pixService.createPix(new CreatePixRequest(
      "123", "email@pix.com", new BigDecimal("10.00"), "Pedido 1001", idem
    ));

    // manda resposta com erro simulado (consumer irá reenviar ao DLX retry e depois DLQ)
    BacenResponseEvent bad = new BacenResponseEvent(created.id(), "CONFIRMED", "E2E123", "SIMULATE_ERROR");
    rabbit.convertAndSend("pix.exchange", "pix.response", om.writeValueAsString(bad));

    // Espera que alguma mensagem apareça em DLQ depois dos retries
    Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
      Object msg = rabbit.receiveAndConvert("pix.dlq.queue");
      assertThat(msg).isNotNull();
    });
  }
}
