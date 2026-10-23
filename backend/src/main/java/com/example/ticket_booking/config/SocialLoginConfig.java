package com.example.ticket_booking.config;

import com.example.ticket_booking.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * 소셜 로그인 제공자 등록과 프론트 리다이렉트 설정(ADR-0013). 제공자는 환경변수 {@code <KAKAO|NAVER|GOOGLE>_CLIENT_ID}·{@code
 * _CLIENT_SECRET} 둘 다 (공백 제거 뒤) 값이 있을 때만 등록한다 — 하나도 없으면 이 설정의 빈이 하나도 만들어지지 않고 서버는 이메일 인증만으로 기동한다.
 * 하나라도 있으면 프론트 기준 주소 FRONTEND_BASE_URL 이 올바라야 하고, 아니면 기동이 실패한다 — 리다이렉트 주소를 요청에서 얻지 않으므로 이 값이 유일한
 * 출처다.
 */
@Configuration
public class SocialLoginConfig {

  private static final String CALLBACK = "{baseUrl}/login/oauth2/code/{registrationId}";

  @Bean
  @Conditional(AnyProviderConfigured.class)
  public ClientRegistrationRepository clientRegistrationRepository(Environment environment) {
    List<ClientRegistration> registrations = new ArrayList<>();
    if (configured(environment, "KAKAO")) {
      registrations.add(kakao(id(environment, "KAKAO"), secret(environment, "KAKAO")));
    }
    if (configured(environment, "NAVER")) {
      registrations.add(naver(id(environment, "NAVER"), secret(environment, "NAVER")));
    }
    if (configured(environment, "GOOGLE")) {
      registrations.add(google(id(environment, "GOOGLE"), secret(environment, "GOOGLE")));
    }
    return new InMemoryClientRegistrationRepository(registrations);
  }

  /**
   * 등록되지 않은 제공자(foo·apple·대소문자 변형) 경로는 Spring 기본 동작이 500 이다(InvalidClientRegistrationIdException —
   * 패키지 비공개이고 IllegalArgumentException 이다) — 인가 요청을 만들지 않고 다음 필터로 넘겨 다른 없는 경로처럼 404 가 되게 한다.
   */
  @Bean
  @Conditional(AnyProviderConfigured.class)
  public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
      ClientRegistrationRepository clientRegistrations) {
    OAuth2AuthorizationRequestResolver delegate =
        new DefaultOAuth2AuthorizationRequestResolver(clientRegistrations, "/oauth2/authorization");
    return new OAuth2AuthorizationRequestResolver() {
      @Override
      public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        try {
          return delegate.resolve(request);
        } catch (IllegalArgumentException exception) {
          return null;
        }
      }

      @Override
      public OAuth2AuthorizationRequest resolve(
          HttpServletRequest request, String clientRegistrationId) {
        try {
          return delegate.resolve(request, clientRegistrationId);
        } catch (IllegalArgumentException exception) {
          return null;
        }
      }
    };
  }

  @Bean
  @Conditional(AnyProviderConfigured.class)
  public OAuth2LoginSuccessHandler oauth2LoginSuccessHandler(
      AuthService authService, Environment environment) {
    return new OAuth2LoginSuccessHandler(authService, frontendBaseUrl(environment));
  }

  @Bean
  @Conditional(AnyProviderConfigured.class)
  public OAuth2LoginFailureHandler oauth2LoginFailureHandler(Environment environment) {
    return new OAuth2LoginFailureHandler(frontendBaseUrl(environment));
  }

  /**
   * 구글은 OIDC(openid 스코프)가 아니라 userinfo 엔드포인트로 사용자 정보를 읽는다 — 세 제공자가 같은 경로(OAuth2UserService →
   * attributes)를 탄다.
   */
  private static ClientRegistration google(String clientId, String clientSecret) {
    return CommonOAuth2Provider.GOOGLE
        .getBuilder("google")
        .clientId(clientId)
        .clientSecret(clientSecret)
        .scope("profile", "email")
        .build();
  }

  private static ClientRegistration kakao(String clientId, String clientSecret) {
    return ClientRegistration.withRegistrationId("kakao")
        .clientId(clientId)
        .clientSecret(clientSecret)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri(CALLBACK)
        .scope("profile_nickname", "account_email")
        .authorizationUri("https://kauth.kakao.com/oauth/authorize")
        .tokenUri("https://kauth.kakao.com/oauth/token")
        .userInfoUri("https://kapi.kakao.com/v2/user/me")
        .userNameAttributeName("id")
        .clientName("Kakao")
        .build();
  }

  private static ClientRegistration naver(String clientId, String clientSecret) {
    return ClientRegistration.withRegistrationId("naver")
        .clientId(clientId)
        .clientSecret(clientSecret)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri(CALLBACK)
        .scope("name", "email")
        .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
        .tokenUri("https://nid.naver.com/oauth2.0/token")
        .userInfoUri("https://openapi.naver.com/v1/nid/me")
        .userNameAttributeName("response")
        .clientName("Naver")
        .build();
  }

  /** 제공자 하나라도 등록되면 기준 주소가 올바라야 한다. 앞뒤 공백과 끝 슬래시는 정리해 돌려준다. */
  static String frontendBaseUrl(Environment environment) {
    String raw = environment.getProperty("FRONTEND_BASE_URL");
    if (raw == null || raw.isBlank()) {
      throw new IllegalStateException("FRONTEND_BASE_URL 이 필요하다 (소셜 로그인 제공자가 설정돼 있다)");
    }
    String trimmed = raw.trim();
    URI uri;
    try {
      uri = new URI(trimmed);
    } catch (URISyntaxException exception) {
      throw invalidBaseUrl("URL 형식이 아니다");
    }
    String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
    if (!scheme.equals("http") && !scheme.equals("https")) {
      throw invalidBaseUrl("스킴은 http 또는 https 여야 한다");
    }
    if (uri.getHost() == null || uri.getUserInfo() != null) {
      throw invalidBaseUrl("호스트가 있어야 하고 사용자 정보를 포함할 수 없다");
    }
    // URI 는 빈 쿼리("?")·빈 프래그먼트("#")를 null 이 아닌 값으로 돌려주지만 문자열로도 막는다
    if (trimmed.contains("?") || trimmed.contains("#")) {
      throw invalidBaseUrl("쿼리와 프래그먼트를 포함할 수 없다");
    }
    int end = trimmed.length();
    while (end > 0 && trimmed.charAt(end - 1) == '/') {
      end--;
    }
    return trimmed.substring(0, end);
  }

  private static IllegalStateException invalidBaseUrl(String reason) {
    return new IllegalStateException("FRONTEND_BASE_URL 이 올바르지 않다: " + reason);
  }

  private static boolean configured(Environment environment, String prefix) {
    return hasText(environment.getProperty(prefix + "_CLIENT_ID"))
        && hasText(environment.getProperty(prefix + "_CLIENT_SECRET"));
  }

  private static String id(Environment environment, String prefix) {
    return environment.getProperty(prefix + "_CLIENT_ID").trim();
  }

  private static String secret(Environment environment, String prefix) {
    return environment.getProperty(prefix + "_CLIENT_SECRET").trim();
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  /** 제공자 환경변수가 한 쌍이라도 갖춰졌는가. */
  static class AnyProviderConfigured implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      Environment environment = context.getEnvironment();
      return configured(environment, "KAKAO")
          || configured(environment, "NAVER")
          || configured(environment, "GOOGLE");
    }
  }
}
