package com.example.fraud.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record FraudCheckRequest(
  @NotNull UUID transactionId,
  @NotBlank String payerId,
  @NotBlank String receiverKey,
  @NotNull BigDecimal amount
) {}
