package com.example.pix.api;

import jakarta.validation.constraints.NotBlank;

public record BacenSendRequest(
  @NotBlank String correlationId
) {}
