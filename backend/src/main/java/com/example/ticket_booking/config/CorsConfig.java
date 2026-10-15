package com.example.ticket_booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** 서블릿 필터 수준 CORS — 추후 Spring Security 가 들어와도 사전 요청이 거부되지 않는다(ADR-0016). */
@Configuration
public class CorsConfig {

  /** 순서는 레지스트레이션에 명시한다 — @Bean 메서드의 @Order 는 필터 체인 순서에 반영되지 않는다. */
  @Bean
  public FilterRegistrationBean<CorsFilter> corsFilter(
      @Value("${CORS_ALLOWED_ORIGINS:}") String allowedOrigins) {
    FilterRegistrationBean<CorsFilter> registration =
        new FilterRegistrationBean<>(CorsFilter.fromCommaSeparated(allowedOrigins));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
}
