package com.example.bacen.mq;

import com.example.bacen.events.BacenResponseEvent;
import com.example.bacen.events.PixRequestedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class BacenSimulator {

  private final RabbitTemplate rabbit;
  private final ObjectMapper om;

  @Value("${mock.pix.exchange}") private String exchange;
  @Value("${mock.pix.responseRoutingKey}") private String responseRoutingKey;

  @Value("${mock.pix.minDelayMs}") private long minDelayMs;
  @Value("${mock.pix.maxDelayMs}") private long maxDelayMs;

  @Value("${mock.pix.timeoutDelayMs}") private long timeoutDelayMs;
  @Value("${mock.pix.timeoutRate}") private double timeoutRate;

  @Value("${mock.pix.errorRate}") private double errorRate;
  @Value("${mock.pix.malformedRate}") private double malformedRate;
  @Value("${mock.pix.dropRate}") private double dropRate;
  @Value("${mock.pix.duplicateRate}") private double duplicateRate;

  @RabbitListener(queues = "${mock.pix.sendQueue}")
  public void onPixSend(String message) throws Exception {
    PixRequestedEvent req = om.readValue(message, PixRequestedEvent.class);

    // 1) Drop (sem resposta)
    if (chance(dropRate)) {
      return;
    }

    // 2) Latência: variável + "timeout" (resposta muito atrasada)
    long delay = randomBetween(minDelayMs, maxDelayMs);
    if (chance(timeoutRate)) {
      delay = Math.max(delay, timeoutDelayMs);
    }
    Thread.sleep(Math.max(0, delay));

    // 3) Malformed JSON
    if (chance(malformedRate)) {
      rabbit.convertAndSend(exchange, responseRoutingKey, "{ this_is: not_json }");
      return;
    }

    // 4) Decide status
    String status = (req.amount() != null && req.amount().doubleValue() >= 500.0) ? "FAILED" : "CONFIRMED";

    // 5) Erro intermitente (força consumer a falhar e acionar retry/DLQ)
    String reasonCode = chance(errorRate) ? "SIMULATE_ERROR" : "00";

    BacenResponseEvent resp = new BacenResponseEvent(
      req.transactionId(),
      status,
      "E" + UUID.randomUUID().toString().replace("-", "").substring(0, 32),
      reasonCode
    );

    String payload = om.writeValueAsString(resp);
    rabbit.convertAndSend(exchange, responseRoutingKey, payload);

    // 6) Duplicidade eventual (para testar idempotência/robustez do consumer)
    if (chance(duplicateRate)) {
      rabbit.convertAndSend(exchange, responseRoutingKey, payload);
    }
  }

  private boolean chance(double rate) {
    if (rate <= 0.0) return false;
    return ThreadLocalRandom.current().nextDouble() < rate;
  }

  private long randomBetween(long min, long max) {
    if (max <= min) return min;
    return ThreadLocalRandom.current().nextLong(min, max + 1);
  }
}
