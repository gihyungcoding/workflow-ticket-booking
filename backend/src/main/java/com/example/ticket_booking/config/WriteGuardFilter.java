package com.example.ticket_booking.config;

import com.example.ticket_booking.api.dto.ErrorResponse;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class WriteGuardFilter implements Filter {

  private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
  private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

  private final boolean enabled;
  private final String writeApiKey;

  public WriteGuardFilter(boolean enabled, String writeApiKey) {
    this.enabled = enabled;
    this.writeApiKey = writeApiKey;
  }

  /**
   * WRITE_GUARD_ENABLED 환경변수 원문을 파싱해 WriteGuardFilter 를 만든다. 값이 정확히 "true"(대소문자 무관)일 때만 활성화되고, 그 외
   * 값은 비활성으로 취급한다(ADR-0016). enabled 가 true 인데 apiKey 가 비어 있으면(null 또는 공백만) 예외를 던져 기동을 실패시킨다.
   */
  public static WriteGuardFilter fromEnv(String enabledRaw, String apiKey) {
    boolean enabled = enabledRaw != null && "true".equalsIgnoreCase(enabledRaw.trim());
    if (enabled && (apiKey == null || apiKey.isBlank())) {
      throw new IllegalStateException(
          "WRITE_GUARD_ENABLED=true 이면 WRITE_API_KEY 가 필요하다 (비어 있으면 가드가 열린 채 기동된다)");
    }
    return new WriteGuardFilter(enabled, apiKey);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    if (!enabled || !WRITE_METHODS.contains(httpRequest.getMethod())) {
      chain.doFilter(request, response);
      return;
    }

    String provided = httpRequest.getHeader("X-Write-Key");
    if (provided != null && keyMatches(provided)) {
      chain.doFilter(request, response);
      return;
    }

    HttpServletResponse httpResponse = (HttpServletResponse) response;
    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    httpResponse.setContentType("application/json");
    httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
    httpResponse
        .getWriter()
        .write(
            OBJECT_MAPPER.writeValueAsString(
                new ErrorResponse("WRITE_KEY_INVALID", "X-Write-Key 가 없거나 올바르지 않습니다")));
  }

  private boolean keyMatches(String provided) {
    return MessageDigest.isEqual(
        provided.getBytes(StandardCharsets.UTF_8), writeApiKey.getBytes(StandardCharsets.UTF_8));
  }
}
