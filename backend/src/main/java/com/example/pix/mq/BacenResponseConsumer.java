package com.example.pix.mq;

import com.example.pix.audit.PixAuditLog;
import com.example.pix.audit.PixAuditLogRepository;
import com.example.pix.domain.PixStatus;
import com.example.pix.events.BacenResponseEvent;
import com.example.pix.repo.PixTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BacenResponseConsumer {

  private final PixTransactionRepository txRepo;
  private final PixAuditLogRepository auditRepo;
  private final ObjectMapper om;
  private final RabbitTemplate rabbit;

  @Value("${pix.mq.dlx}") private String dlx;
  @Value("${pix.mq.retryRoutingKey}") private String retryRoutingKey;
  @Value("${pix.mq.dlRoutingKey}") private String dlRoutingKey;

  private static final int MAX_RETRIES = 3;

  @RabbitListener(queues = "${pix.mq.responseQueue}")
  @Transactional
  public void onMessage(String message,
                        @Header(name="x-death", required=false) Object xDeathRaw) throws Exception {

    int deaths = extractDeaths(xDeathRaw);

    try {
      var ev = om.readValue(message, BacenResponseEvent.class);

      auditRepo.save(PixAuditLog.builder()
        .id(UUID.randomUUID())
        .transactionId(ev.transactionId())
        .kind("BACEN_RESPONSE")
        .payload(message)
        .createdAt(Instant.now())
        .build());

      // Simulador de falha: reasonCode = SIMULATE_ERROR -> força retry/DLQ
      if ("SIMULATE_ERROR".equalsIgnoreCase(ev.reasonCode())) {
        throw new RuntimeException("Simulated processing error");
      }

      var tx = txRepo.findById(ev.transactionId()).orElseThrow();
      if ("CONFIRMED".equalsIgnoreCase(ev.status())) {
        tx.setStatus(PixStatus.CONFIRMED);
        tx.setEndToEndId(ev.endToEndId());
      } else {
        tx.setStatus(PixStatus.FAILED);
      }
      tx.setUpdatedAt(Instant.now());
      txRepo.save(tx);

    } catch (Exception ex) {
      // Publica no DLX para retry ou DLQ
      if (deaths >= MAX_RETRIES) {
        rabbit.convertAndSend(dlx, dlRoutingKey, message);
      } else {
        rabbit.convertAndSend(dlx, retryRoutingKey, message);
      }

      // lança exceção para o container registrar falha e (por padrão) requeue=false se configurado;
      // como estamos repassando para DLX, precisamos evitar reprocessamento imediato.
      throw ex;
    }
  }

  @SuppressWarnings("unchecked")
  private int extractDeaths(Object xDeathRaw) {
    if (xDeathRaw == null) return 0;
    try {
      if (xDeathRaw instanceof List<?> list && !list.isEmpty()) {
        Object first = list.get(0);
        if (first instanceof Map<?,?> map) {
          Object count = map.get("count");
          if (count instanceof Long l) return l.intValue();
          if (count instanceof Integer i) return i;
        }
      }
    } catch (Exception ignored) {}
    return 0;
  }
}
