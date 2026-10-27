package com.example.ticket_booking.config;

import com.example.ticket_booking.service.AuthService;
import com.example.ticket_booking.service.SocialLoginResult;
import com.example.ticket_booking.service.SocialProfile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * 제공자 인증이 끝나면 판정(AuthService.loginWithSocial)한 결과를 프론트로 리다이렉트한다. 이동할 주소는 설정에서 온 기준
 * 주소(FRONTEND_BASE_URL)뿐이다 — 요청의 쿼리·헤더는 쓰지 않는다. 제공자 응답이 이상하면 아무것도 만들지 않고 실패 화면으로 보낸다.
 */
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final AuthService authService;
  private final String frontendBaseUrl;

  public OAuth2LoginSuccessHandler(AuthService authService, String frontendBaseUrl) {
    this.authService = authService;
    this.frontendBaseUrl = frontendBaseUrl;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    response.sendRedirect(redirectLocation(request, authentication));
  }

  private String redirectLocation(HttpServletRequest request, Authentication authentication) {
    try {
      if (!(authentication instanceof OAuth2AuthenticationToken token)) {
        return OAuth2LoginFailureHandler.failureLocation(frontendBaseUrl);
      }
      String registrationId = token.getAuthorizedClientRegistrationId();
      SocialProfile profile =
          CustomOAuth2UserService.toProfile(
              registrationId,
              token.getPrincipal().getAttributes(),
              appleUser(request, registrationId));
      SocialLoginResult result = authService.loginWithSocial(profile);
      return result.accessToken() != null
          ? frontendBaseUrl + "/oauth2/redirect?token=" + encode(result.accessToken())
          : frontendBaseUrl + "/oauth2/redirect?pendingToken=" + encode(result.pendingToken());
    } catch (OAuth2AuthenticationException exception) {
      return OAuth2LoginFailureHandler.failureLocation(frontendBaseUrl);
    }
  }

  /** 애플의 최초 인가 콜백(form_post)이 싣는 user 파라미터. 애플의 POST 콜백에서만 읽는다 — GET 쿼리나 다른 제공자의 값은 무시한다. */
  private static String appleUser(HttpServletRequest request, String registrationId) {
    boolean appleForm =
        "apple".equals(registrationId) && "POST".equalsIgnoreCase(request.getMethod());
    return appleForm ? request.getParameter("user") : null;
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
