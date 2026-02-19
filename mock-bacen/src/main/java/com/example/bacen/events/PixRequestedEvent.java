package com.example.bacen.events;

import java.math.BigDecimal;
import java.util.UUID;

public record PixRequestedEvent(
  UUID transactionId,
  String receiverKey,
  BigDecimal amount,
  String description,
  String idempotencyKey
) {}
