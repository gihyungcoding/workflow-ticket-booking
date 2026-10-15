package com.example.ticket_booking.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CorsFilter implements Filter {

  private static final String ALLOW_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS";
  private static final String ALLOW_HEADERS = "Content-Type, Authorization, X-Write-Key";

  private final List<String> allowedOrigins;

  public CorsFilter(List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  /**
   * CORS_ALLOWED_ORIGINS 환경변수 원문(쉼표 구분)을 파싱해 CorsFilter 를 만든다. 각 원소를 trim 하고 빈 원소를 제거해야
   * 한다(ADR-0016).
   */
  public static CorsFilter fromCommaSeparated(String raw) {
    if (raw == null) {
      return new CorsFilter(List.of());
    }
    return new CorsFilter(
        Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String origin = httpRequest.getHeader("Origin");
    if (origin == null || !allowedOrigins.contains(origin)) {
      chain.doFilter(request, response);
      return;
    }

    httpResponse.setHeader("Access-Control-Allow-Origin", origin);
    httpResponse.addHeader("Vary", "Origin");
    boolean preflight =
        "OPTIONS".equals(httpRequest.getMethod())
            && httpRequest.getHeader("Access-Control-Request-Method") != null;
    if (preflight) {
      httpResponse.setHeader("Access-Control-Allow-Methods", ALLOW_METHODS);
      httpResponse.setHeader("Access-Control-Allow-Headers", ALLOW_HEADERS);
      httpResponse.setStatus(HttpServletResponse.SC_OK);
      return;
    }
    chain.doFilter(request, response);
  }
}
