package com.example.ticket_booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class WriteGuardConfig {

  /** CORS 필터 바로 뒤에 둔다 — 401 응답에도 CORS 헤더가 붙고, 사전 요청은 CORS 필터가 먼저 처리한다. */
  @Bean
  public FilterRegistrationBean<WriteGuardFilter> writeGuardFilter(
      @Value("${WRITE_GUARD_ENABLED:false}") String enabled,
      @Value("${WRITE_API_KEY:}") String apiKey) {
    FilterRegistrationBean<WriteGuardFilter> registration =
        new FilterRegistrationBean<>(WriteGuardFilter.fromEnv(enabled, apiKey));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    return registration;
  }
}
