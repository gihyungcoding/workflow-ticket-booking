package com.example.ticket_booking.config;

import com.example.ticket_booking.domain.SocialProvider;
import com.example.ticket_booking.service.SocialProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 제공자마다 다른 사용자 정보(attributes)를 같은 모양의 {@link SocialProfile} 로 바꾼다. 제공자가 준 값은 외부 입력이다 — 문자열 규칙은
 * {@link SocialInputRules} 가 정하고(타입·제어 문자·서로게이트·이메일 형식, ADR-0019), 어긋나면 조용히 바꿔 통과시키지 않고 거부한다.
 */
public final class CustomOAuth2UserService {

  /** 애플이 이름을 주지 않았을 때(재로그인 등) 신규 가입 대기에 쓰는 이름. users.name 은 NOT NULL 이다. */
  static final String APPLE_NO_NAME = "Apple 사용자";

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private CustomOAuth2UserService() {}

  /**
   * registrationId(kakao·naver·google·apple)와 제공자 attributes 로 정규화된 프로필을 만든다. 이상하면
   * OAuth2AuthenticationException.
   */
  public static SocialProfile toProfile(String registrationId, Map<String, Object> attributes) {
    return toProfile(registrationId, attributes, null);
  }

  /**
   * 애플은 이름을 id_token 이 아니라 최초 인가 콜백의 {@code user} 폼 파라미터로만 준다. appleUser 는 그 원문이고 registrationId 가
   * apple 일 때만 쓴다(없으면 null).
   */
  public static SocialProfile toProfile(
      String registrationId, Map<String, Object> attributes, String appleUser) {
    if (registrationId == null || attributes == null) {
      throw invalid("제공자 응답이 비었다");
    }
    return switch (registrationId) {
      case "google" -> googleProfile(attributes);
      case "kakao" -> kakaoProfile(attributes);
      case "naver" -> naverProfile(attributes);
      case "apple" -> appleProfile(attributes, appleUser);
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

  /**
   * 애플은 id_token 클레임이 평면이고 이름이 없다. email_verified 는 불리언이 아니라 문자열 "true" 로 올 수 있어, 불리언 true 이거나 정확히
   * 문자열 "true" 일 때만 검증된 것으로 본다(잘못 true 면 미검증 이메일이 기존 계정에 연결된다 — ADR-0020). Spring 의 기본 id_token
   * 디코더는 이 클레임을 먼저 Boolean 으로 바꿔 버리므로(대소문자 무시) 원문 타입이 도달하도록
   * SocialLoginConfig.appleIdTokenDecoderFactory 가 그 변환을 뺀다.
   */
  private static SocialProfile appleProfile(Map<?, ?> attributes, String appleUser) {
    Object verified = attributes.get("email_verified");
    return new SocialProfile(
        SocialProvider.APPLE,
        providerUserId(attributes.get("sub")),
        email(attributes.get("email")),
        appleName(appleUser),
        Boolean.TRUE.equals(verified) || "true".equals(verified));
  }

  /**
   * {@code user} 파라미터(JSON)의 firstName·lastName 을 공백 1칸으로 이어 붙인다. 서명되지 않은 값이라 이메일은 읽지 않고, 부분을 변환하지
   * 않으며 (ADR-0019), 구조가 이상하거나 규칙 밖이면 거부한다. 비었거나 이름이 없으면 {@link #APPLE_NO_NAME}.
   */
  private static String appleName(String user) {
    if (user == null || user.isBlank()) {
      return APPLE_NO_NAME;
    }
    JsonNode root;
    try {
      root = JSON.readTree(user);
    } catch (JacksonException exception) {
      throw invalid("애플 이름 정보의 형식이 다르다");
    }
    if (root == null || !root.isObject()) {
      throw invalid("애플 이름 정보의 형식이 다르다");
    }
    JsonNode name = root.get("name");
    if (name == null || name.isNull()) {
      return APPLE_NO_NAME;
    }
    if (!name.isObject()) {
      throw invalid("애플 이름 정보의 형식이 다르다");
    }
    List<String> parts = new ArrayList<>();
    addNamePart(parts, name.get("firstName"));
    addNamePart(parts, name.get("lastName"));
    if (parts.isEmpty()) {
      return APPLE_NO_NAME;
    }
    return name(String.join(" ", parts));
  }

  private static void addNamePart(List<String> parts, JsonNode node) {
    if (node == null || node.isNull()) {
      return;
    }
    if (!node.isString()) {
      throw invalid("이름이 올바르지 않다");
    }
    String text = SocialInputRules.text(node.stringValue());
    if (text == null) {
      throw invalid("이름이 올바르지 않다");
    }
    if (!text.isBlank()) {
      parts.add(text);
    }
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
