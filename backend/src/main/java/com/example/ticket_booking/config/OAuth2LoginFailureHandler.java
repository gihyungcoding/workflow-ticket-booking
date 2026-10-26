package com.example.ticket_booking.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/** 제공자 인증이 실패하거나 취소되면 프론트 로그인 화면으로 돌려보낸다. 이동할 주소는 설정에서 온 기준 주소뿐이다. */
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

  private final String frontendBaseUrl;

  public OAuth2LoginFailureHandler(String frontendBaseUrl) {
    this.frontendBaseUrl = frontendBaseUrl;
  }

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    response.sendRedirect(failureLocation(frontendBaseUrl));
  }

  static String failureLocation(String frontendBaseUrl) {
    return frontendBaseUrl + "/login?error=social";
  }
}
