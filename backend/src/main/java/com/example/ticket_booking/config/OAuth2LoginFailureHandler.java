package com.example.ticket_booking.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/** Red 단계 컴파일용 스켈레톤 — 실패 시 프론트 로그인 화면 리다이렉트는 Phase 3 에서 구현한다. */
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

  private final String frontendBaseUrl;

  public OAuth2LoginFailureHandler(String frontendBaseUrl) {
    this.frontendBaseUrl = frontendBaseUrl;
  }

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
    throw new UnsupportedOperationException();
  }
}
