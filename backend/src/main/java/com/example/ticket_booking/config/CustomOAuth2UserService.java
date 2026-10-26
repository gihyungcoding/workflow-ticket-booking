package com.example.ticket_booking.config;

import com.example.ticket_booking.domain.SocialProvider;
import com.example.ticket_booking.service.SocialProfile;
import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * 제공자마다 다른 사용자 정보(attributes)를 같은 모양의 {@link SocialProfile} 로 바꾼다. 제공자가 준 값은 외부 입력이다 — 문자열 규칙은
 * {@link SocialInputRules} 가 정하고(타입·제어 문자·서로게이트·이메일 형식, ADR-0019), 어긋나면 조용히 바꿔 통과시키지 않고 거부한다.
 */
public final class CustomOAuth2UserService {

  private CustomOAuth2UserService() {}

  /**
   * registrationId(kakao·naver·google)와 제공자 attributes 로 정규화된 프로필을 만든다. 이상하면
   * OAuth2AuthenticationException.
   */
  public static SocialProfile toProfile(String registrationId, Map<String, Object> attributes) {
    if (registrationId == null || attributes == null) {
      throw invalid("제공자 응답이 비었다");
    }
    return switch (registrationId) {
      case "google" -> googleProfile(attributes);
      case "kakao" -> kakaoProfile(attributes);
      case "naver" -> naverProfile(attributes);
      default -> throw invalid("지원하지 않는 제공자다");
    };
  }

  private static SocialProfile googleProfile(Map<?, ?> attributes) {
    return new SocialProfile(
        SocialProvider.GOOGLE,
        providerUserId(attributes.get("sub")),
        email(attributes.get("email")),
        name(attributes.get("name")),
        Boolean.TRUE.equals(attributes.get("email_verified")));
  }

  /** 카카오는 id 가 숫자이고 계정 정보가 kakao_account 아래 중첩된다. 검증된 이메일은 두 플래그가 모두 true 일 때만이다. */
  private static SocialProfile kakaoProfile(Map<?, ?> attributes) {
    Map<?, ?> account = object(attributes.get("kakao_account"));
    Map<?, ?> profile = object(account.get("profile"));
    return new SocialProfile(
        SocialProvider.KAKAO,
        kakaoId(attributes.get("id")),
        email(account.get("email")),
        name(profile.get("nickname")),
        Boolean.TRUE.equals(account.get("is_email_verified"))
            && Boolean.TRUE.equals(account.get("is_email_valid")));
  }

  /** 네이버는 사용자 정보가 response 래퍼 아래 있고 이메일 검증 여부를 알려주지 않는다 — 자동 연결 대상이 아니다. */
  private static SocialProfile naverProfile(Map<?, ?> attributes) {
    Map<?, ?> response = object(attributes.get("response"));
    return new SocialProfile(
        SocialProvider.NAVER,
        providerUserId(response.get("id")),
        email(response.get("email")),
        name(response.get("name")),
        false);
  }

  private static Map<?, ?> object(Object value) {
    if (value instanceof Map<?, ?> map) {
      return map;
    }
    throw invalid("제공자 응답의 구조가 다르다");
  }

  /** 카카오 id: 양의 정수(Long 범위)만. 실수·문자열·불리언은 거부한다. */
  private static String kakaoId(Object value) {
    if (value instanceof Byte
        || value instanceof Short
        || value instanceof Integer
        || value instanceof Long) {
      long id = ((Number) value).longValue();
      if (id > 0) {
        return Long.toString(id);
      }
    } else if (value instanceof java.math.BigInteger big
        && big.signum() > 0
        && big.bitLength() < Long.SIZE) {
      return big.toString();
    }
    throw invalid("제공자 사용자 ID 가 올바르지 않다");
  }

  private static String providerUserId(Object value) {
    return required(SocialInputRules.providerUserId(value), "제공자 사용자 ID 가 올바르지 않다");
  }

  private static String name(Object value) {
    return required(SocialInputRules.name(value), "이름이 올바르지 않다");
  }

  private static String email(Object value) {
    return required(SocialInputRules.email(value), "이메일이 올바르지 않다");
  }

  private static String required(String value, String description) {
    if (value == null) {
      throw invalid(description);
    }
    return value;
  }

  private static OAuth2AuthenticationException invalid(String description) {
    return new OAuth2AuthenticationException(
        new OAuth2Error("invalid_user_info_response", description, null));
  }
}
