package com.example.pix.api;

import com.example.pix.audit.PixAuditLog;
import com.example.pix.audit.PixAuditLogRepository;
import com.example.pix.events.BacenResponseEvent;
import com.example.pix.events.PixRequestedEvent;
import com.example.pix.repo.PixTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/bacen")
@RequiredArgsConstructor
@Tag(name = "BACEN")
public class BacenController {

  private final PixTransactionRepository txRepo;
  private final PixAuditLogRepository auditRepo;
  private final RabbitTemplate rabbit;
  private final ObjectMapper om;

  @Value("${pix.mq.exchange}") private String exchange;
  @Value("${pix.mq.sendRoutingKey}") private String sendRoutingKey;

  @PostMapping("/send/{pixId}")
  @Operation(summary = "Publica a requisição do PIX para o BACEN via filas (RabbitMQ)")
  public ResponseEntity<String> sendToBacen(@PathVariable UUID pixId,
                                            @Valid @RequestBody BacenSendRequest req) throws Exception {

    var tx = txRepo.findById(pixId).orElseThrow(() -> new IllegalArgumentException("PIX not found"));

    var event = new PixRequestedEvent(
      tx.getId(),
      tx.getReceiverKey(),
      tx.getAmount(),
      tx.getDescription(),
      tx.getIdempotencyKey()
    );

    String payload = om.writeValueAsString(event);

    auditRepo.save(PixAuditLog.builder()
      .id(UUID.randomUUID())
      .transactionId(tx.getId())
      .kind("BACEN_REQUEST_MANUAL")
      .payload(payload)
      .createdAt(Instant.now())
      .build());

    rabbit.convertAndSend(exchange, sendRoutingKey, payload, msg -> {
      msg.getMessageProperties().setHeader("correlationId", req.correlationId());
      msg.getMessageProperties().setHeader("pixId", tx.getId().toString());
      return msg;
    });

    return ResponseEntity.ok("Published to BACEN send queue. correlationId=" + req.correlationId());
  }

  @PostMapping("/response")
  @Operation(summary = "Publica uma resposta mock do BACEN na fila pix.response (para testes manuais)")
  public ResponseEntity<String> publishResponse(@RequestBody BacenResponseEvent resp) throws Exception {
    String payload = om.writeValueAsString(resp);

    rabbit.convertAndSend(exchange, "pix.response", payload);

    auditRepo.save(PixAuditLog.builder()
      .id(UUID.randomUUID())
      .transactionId(resp.transactionId())
      .kind("BACEN_RESPONSE_MANUAL")
      .payload(payload)
      .createdAt(Instant.now())
      .build());

    return ResponseEntity.ok("Published BACEN response to pix.response");
  }
}
