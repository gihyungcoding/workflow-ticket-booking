package com.example.ticket_booking.config;

import com.example.ticket_booking.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/** Red 단계 컴파일용 스켈레톤 — 판정 결과에 따른 프론트 리다이렉트는 Phase 3 에서 구현한다. */
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final AuthService authService;
  private final String frontendBaseUrl;

  public OAuth2LoginSuccessHandler(AuthService authService, String frontendBaseUrl) {
    this.authService = authService;
    this.frontendBaseUrl = frontendBaseUrl;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    throw new UnsupportedOperationException();
  }
}
