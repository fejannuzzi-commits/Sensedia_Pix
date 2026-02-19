package com.example.fraud.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fraud")
public class FraudController {

  /**
   * Regras simples:
   * - amount >= 1000 -> rejeita
   * - receiverKey contém "fraud" -> rejeita
   * - senão aprova
   */
  @PostMapping("/check")
  public ResponseEntity<FraudDecision> check(@Valid @RequestBody FraudCheckRequest req) {
    if (req.amount().doubleValue() >= 1000.0) {
      return ResponseEntity.ok(new FraudDecision(false, "AMOUNT_THRESHOLD"));
    }
    if (req.receiverKey().toLowerCase().contains("fraud")) {
      return ResponseEntity.ok(new FraudDecision(false, "RECEIVER_SUSPICIOUS"));
    }
    return ResponseEntity.ok(new FraudDecision(true, "OK"));
  }
}
