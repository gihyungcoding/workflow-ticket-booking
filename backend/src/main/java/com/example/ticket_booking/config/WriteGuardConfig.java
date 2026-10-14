package com.example.ticket_booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
public class WriteGuardConfig {

  /** CORS 필터 바로 뒤에 둔다 — 사전 요청(OPTIONS)은 CORS 필터가 먼저 처리한다. */
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE + 1)
  public WriteGuardFilter writeGuardFilter(
      @Value("${WRITE_GUARD_ENABLED:false}") String enabled,
      @Value("${WRITE_API_KEY:}") String apiKey) {
    return WriteGuardFilter.fromEnv(enabled, apiKey);
  }
}
