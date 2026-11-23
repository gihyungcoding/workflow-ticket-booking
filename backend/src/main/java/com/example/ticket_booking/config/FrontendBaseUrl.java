package com.example.ticket_booking.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.springframework.core.env.Environment;

/**
 * 프론트 기준 주소 FRONTEND_BASE_URL 의 검증·정리 — 소셜 로그인 이동과 계정 메일 링크가 같은 규칙을 쓴다(TASK-027 에서
 * SocialLoginConfig 에서 옮겼다). 제공자 유무와 무관하게 항상 필수다(사용자 결정 2026-11-19). 요청의 Host 를 쓰지 않으므로 이 값이 링크·이동
 * 주소의 유일한 출처다.
 */
public final class FrontendBaseUrl {

  private FrontendBaseUrl() {}

  /**
   * 기준 주소를 읽어 검증하고 앞뒤 공백과 끝 슬래시를 정리해 돌려준다. 없거나 공백뿐이거나, 스킴이 http/https 가 아니거나(대소문자 무시), 호스트가 없거나,
   * 사용자 정보·쿼리·프래그먼트가 있으면 IllegalStateException(메시지에 FRONTEND_BASE_URL) — 기동 실패다.
   */
  public static String from(Environment environment) {
    String raw = environment.getProperty("FRONTEND_BASE_URL");
    if (raw == null || raw.isBlank()) {
      throw new IllegalStateException("FRONTEND_BASE_URL 이 필요하다 (메일 링크와 소셜 로그인 이동의 기준 주소)");
    }
    String trimmed = raw.trim();
    URI uri;
    try {
      uri = new URI(trimmed);
    } catch (URISyntaxException exception) {
      throw invalid("URL 형식이 아니다");
    }
    String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
    if (!scheme.equals("http") && !scheme.equals("https")) {
      throw invalid("스킴은 http 또는 https 여야 한다");
    }
    if (uri.getHost() == null || uri.getUserInfo() != null) {
      throw invalid("호스트가 있어야 하고 사용자 정보를 포함할 수 없다");
    }
    // URI 는 빈 쿼리("?")·빈 프래그먼트("#")를 null 이 아닌 값으로 돌려주지만 문자열로도 막는다
    if (trimmed.contains("?") || trimmed.contains("#")) {
      throw invalid("쿼리와 프래그먼트를 포함할 수 없다");
    }
    int end = trimmed.length();
    while (end > 0 && trimmed.charAt(end - 1) == '/') {
      end--;
    }
    return trimmed.substring(0, end);
  }

  private static IllegalStateException invalid(String reason) {
    return new IllegalStateException("FRONTEND_BASE_URL 이 올바르지 않다: " + reason);
  }
}
