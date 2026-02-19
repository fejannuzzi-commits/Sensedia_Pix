package com.example.bacen.events;

import java.util.UUID;

public record BacenResponseEvent(
  UUID transactionId,
  String status,
  String endToEndId,
  String reasonCode
) {}
