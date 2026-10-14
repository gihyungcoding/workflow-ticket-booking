package com.example.ticket_booking.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.List;

public class CorsFilter implements Filter {

  private final List<String> allowedOrigins;

  public CorsFilter(List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  /**
   * CORS_ALLOWED_ORIGINS 환경변수 원문(쉼표 구분)을 파싱해 CorsFilter 를 만든다. 각 원소를 trim 하고
   * 빈 원소를 제거해야 한다(ADR-0016).
   */
  public static CorsFilter fromCommaSeparated(String raw) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    throw new UnsupportedOperationException();
  }
}
