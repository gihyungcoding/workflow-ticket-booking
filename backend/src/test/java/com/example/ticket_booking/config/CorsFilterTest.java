package com.example.ticket_booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * TASK-014 시나리오 SC-01, SC-02, SC-13, SC-14, SC-15, SC-19, SC-20, SC-23.
 *
 * <p>CorsFilter 는 아직 Spring 컨텍스트에 등록되지 않고(Green 단계에서 CorsConfig 가 wiring 한다),
 * 여기서는 필터 인스턴스를 직접 호출해 핵심 분기를 검증한다(test_hints 참고). doFilter()/
 * fromCommaSeparated() 본문이 전부 UnsupportedOperationException 을 던지므로, 아래 테스트는 전부
 * 그 예외로 실패한다 — 로직이 한 줄도 없다는 뜻이다(TEST_TASK-014.json 의 purity_check 참고).
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-014.md
 */
class CorsFilterTest {

  private final FilterChain chain = (req, resp) -> {};

  @Test
  void test_sc01_허용된_출처의_OPTIONS_사전요청이_헤더와_함께_통과한다() throws Exception {
    // Given CORS_ALLOWED_ORIGINS 가 https://app.example.com,https://admin.example.com 으로 설정되어
    // 있다
    CorsFilter filter =
        new CorsFilter(List.of("https://app.example.com", "https://admin.example.com"));

    // When 브라우저가 Origin: https://admin.example.com 으로 PUT 요청 전에 OPTIONS 사전 요청을 보낸다
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/performances/1");
    request.addHeader("Origin", "https://admin.example.com");
    request.addHeader("Access-Control-Request-Method", "PUT");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    // Then 응답에 Access-Control-Allow-Origin: https://admin.example.com 이 포함된다
    assertThat(response.getHeader("Access-Control-Allow-Origin"))
        .isEqualTo("https://admin.example.com");
    // Then Access-Control-Allow-Headers 에 Content-Type, Authorization, X-Write-Key 가 포함된다
    assertThat(response.getHeader("Access-Control-Allow-Headers"))
        .contains("Content-Type", "Authorization", "X-Write-Key");
    // Then 응답 상태가 2xx다
    assertThat(response.getStatus()).isBetween(200, 299);
  }

  @Test
  void test_sc02_허용되지_않은_출처는_CORS_헤더를_받지_못한다() throws Exception {
    // Given CORS_ALLOWED_ORIGINS 가 https://app.example.com 으로 설정되어 있다
    CorsFilter filter = new CorsFilter(List.of("https://app.example.com"));

    // When Origin: https://evil.example.com 으로 요청한다
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/performances");
    request.addHeader("Origin", "https://evil.example.com");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    // Then 응답에 Access-Control-Allow-Origin 헤더가 없다
    assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
  }

  @Test
  void test_sc13_Origin_헤더가_없는_요청은_CORS_로직_없이_그대로_처리된다() throws Exception {
    // Given CORS_ALLOWED_ORIGINS 가 https://app.example.com 으로 설정되어 있다
    CorsFilter filter = new CorsFilter(List.of("https://app.example.com"));

    // When Origin 헤더 없이 GET /api/performances 를 호출한다
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/performances");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    // Then 200이 반환된다
    assertThat(response.getStatus()).isEqualTo(200);
    // Then 응답에 Access-Control-Allow-Origin 헤더가 없다
    assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
  }

  @Test
  void test_sc14_CORS_ALLOWED_ORIGINS_가_비어있으면_모든_출처가_거부된다() throws Exception {
    // Given CORS_ALLOWED_ORIGINS 가 미설정(또는 빈 문자열)이다
    CorsFilter filter = CorsFilter.fromCommaSeparated("");

    // When Origin: https://app.example.com 으로 요청한다
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/performances");
    request.addHeader("Origin", "https://app.example.com");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    // Then 응답에 Access-Control-Allow-Origin 헤더가 없다
    assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
  }

  @Test
  void test_sc15_허용_출처를_부분_문자열로_포함하는_다른_출처는_거부된다() throws Exception {
    // Given CORS_ALLOWED_ORIGINS 가 https://app.example.com 으로 설정되어 있다
    CorsFilter filter = new CorsFilter(List.of("https://app.example.com"));

    // When Origin: https://app.example.com.evil.com 으로 요청한다 (접두사로 포함하지만 완전히 같지는 않음)
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/performances");
    request.addHeader("Origin", "https://app.example.com.evil.com");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    // Then 응답에 Access-Control-Allow-Origin 헤더가 없다
    assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
  }

  @Test
  void test_sc19_쉼표_목록의_빈_원소가_모든_출처를_허용하는_결함을_만들지_않는다() throws Exception {
    // Given CORS_ALLOWED_ORIGINS 가 https://app.example.com,,https://admin.example.com 으로 설정되어
    // 있다 (연속 콤마로 빈 원소 포함)
    CorsFilter filter =
        CorsFilter.fromCommaSeparated("https://app.example.com,,https://admin.example.com");

    // When Origin: https://unrelated.example.com 으로 요청한다
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/performances");
    request.addHeader("Origin", "https://unrelated.example.com");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    // Then 응답에 Access-Control-Allow-Origin 헤더가 없다
    assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
  }

  @Test
  void test_sc20_쉼표_뒤_공백이_있어도_출처가_올바르게_매칭된다() throws Exception {
    // Given CORS_ALLOWED_ORIGINS 가 "https://app.example.com, https://admin.example.com" 으로
    // 설정되어 있다 (두 번째 값 앞에 공백)
    CorsFilter filter =
        CorsFilter.fromCommaSeparated("https://app.example.com, https://admin.example.com");

    // When Origin: https://admin.example.com 으로 요청한다
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/performances");
    request.addHeader("Origin", "https://admin.example.com");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, chain);

    // Then 응답에 Access-Control-Allow-Origin: https://admin.example.com 이 포함된다
    assertThat(response.getHeader("Access-Control-Allow-Origin"))
        .isEqualTo("https://admin.example.com");
  }

  @Test
  void test_sc23_Origin_헤더가_빈_값이거나_공백만이면_허용_목록과_일치하지_않는다() throws Exception {
    // Given CORS_ALLOWED_ORIGINS 가 https://app.example.com 으로 설정되어 있다
    CorsFilter filter = new CorsFilter(List.of("https://app.example.com"));

    // When Origin: (빈 문자열) 으로 요청한다
    MockHttpServletRequest emptyOriginRequest = new MockHttpServletRequest("GET", "/api/performances");
    emptyOriginRequest.addHeader("Origin", "");
    MockHttpServletResponse emptyOriginResponse = new MockHttpServletResponse();
    filter.doFilter(emptyOriginRequest, emptyOriginResponse, chain);
    // Then 응답에 Access-Control-Allow-Origin 헤더가 없다
    assertThat(emptyOriginResponse.getHeader("Access-Control-Allow-Origin")).isNull();

    // When Origin: (공백만) 으로 요청한다
    MockHttpServletRequest blankOriginRequest = new MockHttpServletRequest("GET", "/api/performances");
    blankOriginRequest.addHeader("Origin", "   ");
    MockHttpServletResponse blankOriginResponse = new MockHttpServletResponse();
    filter.doFilter(blankOriginRequest, blankOriginResponse, chain);
    // Then 응답에 Access-Control-Allow-Origin 헤더가 없다
    assertThat(blankOriginResponse.getHeader("Access-Control-Allow-Origin")).isNull();
  }
}
