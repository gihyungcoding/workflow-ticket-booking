package com.example.ticket_booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/** 서블릿 필터 수준 CORS — 추후 Spring Security 가 들어와도 사전 요청이 거부되지 않는다(ADR-0016). */
@Configuration
public class CorsConfig {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public CorsFilter corsFilter(@Value("${CORS_ALLOWED_ORIGINS:}") String allowedOrigins) {
    return CorsFilter.fromCommaSeparated(allowedOrigins);
  }
}
