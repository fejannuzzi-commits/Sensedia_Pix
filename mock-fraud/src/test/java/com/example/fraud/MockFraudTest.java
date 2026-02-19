package com.example.fraud;

import com.example.fraud.api.FraudCheckRequest;
import com.example.fraud.api.FraudDecision;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MockFraudTest {

  @Autowired TestRestTemplate rest;

  @Test
  void shouldRejectLargeAmount() {
    FraudDecision d = rest.postForObject("/fraud/check",
      new FraudCheckRequest(UUID.randomUUID(),"p","k", new BigDecimal("1000.00")),
      FraudDecision.class
    );
    assertThat(d.approved()).isFalse();
  }

  @Test
  void shouldApproveSmallAmount() {
    FraudDecision d = rest.postForObject("/fraud/check",
      new FraudCheckRequest(UUID.randomUUID(),"p","email@pix.com", new BigDecimal("10.00")),
      FraudDecision.class
    );
    assertThat(d.approved()).isTrue();
  }
}
