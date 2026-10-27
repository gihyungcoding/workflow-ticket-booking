package com.example.ticket_booking.config;

import java.util.Locale;

/**
 * 소셜 로그인에서 외부로부터 들어오는 문자열(이메일·이름·제공자 사용자 ID)의 규칙을 한 곳에 둔다. 제공자 응답(CustomOAuth2UserService)과 서명된
 * pendingToken(JwtTokenProvider) 이 같은 규칙을 거쳐야 한쪽을 통과한 값이 다른 쪽에서 DB 오류나 다른 값으로 바뀌지 않는다(ADR-0019,
 * ADR-0020). 통과하지 못한 값은 null 이다 — 어떤 예외로 바꿀지는 부르는 쪽이 정한다.
 */
final class SocialInputRules {

  static final int MAX_EMAIL_LENGTH = 255;
  static final int MAX_NAME_LENGTH = 100;
  static final int MAX_PROVIDER_USER_ID_LENGTH = 255;

  private SocialInputRules() {}

  /** users 와 같은 정규화(앞뒤 공백 제거·소문자) 뒤의 이메일. 길이는 소문자화로 늘어날 수 있어 정규화 후에 본다. */
  static String email(Object value) {
    String text = text(value);
    if (text == null) {
      return null;
    }
    String normalized = text.trim().toLowerCase(Locale.ROOT);
    if (normalized.isEmpty() || normalized.length() > MAX_EMAIL_LENGTH) {
      return null;
    }
    int at = normalized.indexOf('@');
    if (at < 1 || at != normalized.lastIndexOf('@') || at == normalized.length() - 1) {
      return null;
    }
    // 주소 안의 공백류(NBSP 등)와 서식 문자(제로폭 공백, 방향 제어)는 눈으로 구별되지 않는 다른 계정을 만든다
    boolean invisible = normalized.codePoints().anyMatch(SocialInputRules::isSeparatorOrFormat);
    return invisible ? null : normalized;
  }

  static String name(Object value) {
    String text = text(value);
    return text == null || text.isBlank() || text.length() > MAX_NAME_LENGTH ? null : text;
  }

  static String providerUserId(Object value) {
    String text = text(value);
    return text == null || text.isBlank() || text.length() > MAX_PROVIDER_USER_ID_LENGTH
        ? null
        : text;
  }

  /** 문자열이어야 하고 제어 문자·짝 없는 서로게이트가 없어야 한다. trim 이 앞뒤 NUL 을 지우기 전의 원문에서 본다. */
  static String text(Object value) {
    if (!(value instanceof String text)) {
      return null;
    }
    for (int index = 0; index < text.length(); ) {
      int codePoint = text.codePointAt(index);
      // 짝이 맞는 서로게이트 쌍은 보조 평면 코드포인트 하나로 읽힌다 — BMP 범위의 서로게이트 값이 나오면 짝이 없는 것이다
      boolean loneSurrogate =
          codePoint < Character.MIN_SUPPLEMENTARY_CODE_POINT
              && Character.isSurrogate((char) codePoint);
      if (loneSurrogate || Character.getType(codePoint) == Character.CONTROL) {
        return null;
      }
      index += Character.charCount(codePoint);
    }
    return text;
  }

  private static boolean isSeparatorOrFormat(int codePoint) {
    return switch (Character.getType(codePoint)) {
      case Character.SPACE_SEPARATOR,
              Character.LINE_SEPARATOR,
              Character.PARAGRAPH_SEPARATOR,
              Character.FORMAT ->
          true;
      default -> false;
    };
  }
}
