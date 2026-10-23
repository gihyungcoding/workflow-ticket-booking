package com.example.ticket_booking.config;

import com.example.ticket_booking.domain.SocialProvider;
import com.example.ticket_booking.service.SocialProfile;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * 제공자마다 다른 사용자 정보(attributes)를 같은 모양의 {@link SocialProfile} 로 바꾼다. 제공자가 준 값은 외부 입력이다 — 타입을 엄격히
 * 보고(문자열이어야 할 값의 숫자·불리언·객체는 거부), 제어 문자·짝 없는 서로게이트는 변환 전의 원문에서 거부하며(ADR-0019), 조용히 바꿔 통과시키지 않는다.
 */
public final class CustomOAuth2UserService {

  private static final int MAX_EMAIL_LENGTH = 255;
  private static final int MAX_NAME_LENGTH = 100;
  private static final int MAX_PROVIDER_USER_ID_LENGTH = 255;

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
    String text = text(value, "제공자 사용자 ID");
    if (text.isBlank() || text.length() > MAX_PROVIDER_USER_ID_LENGTH) {
      throw invalid("제공자 사용자 ID 가 올바르지 않다");
    }
    return text;
  }

  private static String name(Object value) {
    String text = text(value, "이름");
    if (text.isBlank() || text.length() > MAX_NAME_LENGTH) {
      throw invalid("이름이 올바르지 않다");
    }
    return text;
  }

  /** users 와 같은 정규화(앞뒤 공백 제거·소문자) 뒤 길이를 본다 — 소문자화로 길어지는 문자가 있다. */
  private static String email(Object value) {
    String normalized = text(value, "이메일").trim().toLowerCase(Locale.ROOT);
    if (normalized.isEmpty() || normalized.length() > MAX_EMAIL_LENGTH) {
      throw invalid("이메일이 올바르지 않다");
    }
    return normalized;
  }

  /** 문자열이어야 하고 제어 문자·짝 없는 서로게이트가 없어야 한다. trim 이 앞뒤 NUL 을 지우기 전의 원문에서 본다. */
  private static String text(Object value, String label) {
    if (!(value instanceof String text)) {
      throw invalid(label + " 이(가) 문자열이 아니다");
    }
    for (int index = 0; index < text.length(); ) {
      int codePoint = text.codePointAt(index);
      // 짝이 맞는 서로게이트 쌍은 보조 평면 코드포인트 하나로 읽힌다 — BMP 범위의 서로게이트 값이 나오면 짝이 없는 것이다
      boolean loneSurrogate =
          codePoint < Character.MIN_SUPPLEMENTARY_CODE_POINT
              && Character.isSurrogate((char) codePoint);
      if (loneSurrogate || Character.getType(codePoint) == Character.CONTROL) {
        throw invalid(label + " 에 허용되지 않는 문자가 있다");
      }
      index += Character.charCount(codePoint);
    }
    return text;
  }

  private static OAuth2AuthenticationException invalid(String description) {
    return new OAuth2AuthenticationException(
        new OAuth2Error("invalid_user_info_response", description, null));
  }
}
