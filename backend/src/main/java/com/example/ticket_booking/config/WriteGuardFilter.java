package com.example.ticket_booking.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;

public class WriteGuardFilter implements Filter {

  private final boolean enabled;
  private final String writeApiKey;

  public WriteGuardFilter(boolean enabled, String writeApiKey) {
    this.enabled = enabled;
    this.writeApiKey = writeApiKey;
  }

  /**
   * WRITE_GUARD_ENABLED 환경변수 원문을 파싱해 WriteGuardFilter 를 만든다. 값이 정확히 "true"(대소문자
   * 무관)일 때만 활성화되고, 그 외 값은 비활성으로 취급한다(ADR-0016). enabled 가 true 인데 apiKey 가
   * 비어 있으면(null 또는 공백만) 예외를 던져 기동을 실패시킨다.
   */
  public static WriteGuardFilter fromEnv(String enabledRaw, String apiKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    throw new UnsupportedOperationException();
  }
}
