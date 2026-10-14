package com.example.ticket_booking.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * TASK-014 시나리오 SC-05, SC-06, SC-07, SC-16, SC-18, SC-21, SC-24.
 *
 * <p>WriteGuardFilter 역시 아직 Spring 컨텍스트에 등록되지 않았다. 필터 인스턴스를 직접 호출해 핵심
 * 분기를 검증한다. chain 호출 여부를 "다음 단계(컨트롤러·DB)로 넘어갔는가"의 대리 지표로 쓴다 — 가드가
 * 거부하면 chain.doFilter() 가 호출되지 않아야 하고, 그래야 DB 변경도 일어나지 않는다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-014.md
 */
class WriteGuardFilterTest {

  private static final List<String> WRITE_METHODS = List.of("POST", "PUT", "PATCH", "DELETE");

  @Test
  void test_sc05_GET_조회는_가드가_켜져_있어도_키_없이_통과한다() throws Exception {
    // Given WRITE_GUARD_ENABLED=true, WRITE_API_KEY=secret123 로 기동했다
    WriteGuardFilter filter = new WriteGuardFilter(true, "secret123");

    // When X-Write-Key 헤더 없이 GET /api/performances 를 호출한다
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/performances");
    MockHttpServletResponse response = new MockHttpServletResponse();
    boolean[] chainCalled = {false};

    filter.doFilter(request, response, (req, resp) -> chainCalled[0] = true);

    // Then 200이 반환된다 (= 다음 체인으로 통과했다)
    assertThat(chainCalled[0]).isTrue();
  }

  @Test
  void test_sc06_쓰기_메서드는_키가_없거나_틀리면_401로_거부된다() throws Exception {
    // Given WRITE_GUARD_ENABLED=true, WRITE_API_KEY=secret123 로 기동했다
    WriteGuardFilter filter = new WriteGuardFilter(true, "secret123");

    // When 메서드(POST/PUT/PATCH/DELETE) × 키상태(헤더없음/틀림) 8개 조합으로 요청한다
    for (String method : WRITE_METHODS) {
      for (String keyValue : new String[] {null, "wrong"}) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/performances/1");
        if (keyValue != null) {
          request.addHeader("X-Write-Key", keyValue);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainCalled = {false};

        filter.doFilter(request, response, (req, resp) -> chainCalled[0] = true);

        // Then 표의 모든 조합에서 401이 반환된다
        assertThat(response.getStatus()).as("%s key=%s", method, keyValue).isEqualTo(401);
        // Then 각 요청 모두 DB에 어떤 변경도 생기지 않는다(= chain 으로 넘어가지 않았다)
        assertThat(chainCalled[0]).as("%s key=%s chain", method, keyValue).isFalse();
      }
    }
  }

  @Test
  void test_sc07_키가_비어_있으면_애플리케이션이_기동에_실패한다() {
    // Given WRITE_GUARD_ENABLED=true 이고 WRITE_API_KEY 가 미설정(null)이다
    // When fromEnv 로 필터를 구성한다 (이 메서드는 Green 단계에서 WriteGuardConfig 의 @Bean 팩토리가
    // 호출하게 되며, 여기서 예외가 나면 Spring 컨텍스트 초기화 자체가 실패한다)
    // Then 애플리케이션 컨텍스트 로딩이 실패한다 (IllegalStateException 으로 구성 실패를 알린다)
    assertThatThrownBy(() -> WriteGuardFilter.fromEnv("true", null))
        .isInstanceOf(IllegalStateException.class);

    // Given WRITE_GUARD_ENABLED=true 이고 WRITE_API_KEY 가 공백 문자열만이다
    // Then 이 경우도 동일하게 실패한다
    assertThatThrownBy(() -> WriteGuardFilter.fromEnv("true", "   "))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void test_sc16_OPTIONS_사전요청은_가드가_켜져_있어도_키_없이_통과한다() throws Exception {
    // Given WRITE_GUARD_ENABLED=true, WRITE_API_KEY=secret123 로 기동했다
    WriteGuardFilter filter = new WriteGuardFilter(true, "secret123");

    // When PUT /api/performances/{id} 앞에 X-Write-Key 헤더 없이 OPTIONS 사전 요청을 보낸다
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/performances/1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    boolean[] chainCalled = {false};

    filter.doFilter(request, response, (req, resp) -> chainCalled[0] = true);

    // Then 2xx로 통과한다 (= chain 으로 넘어갔다)
    assertThat(chainCalled[0]).isTrue();
  }

  @Test
  void test_sc18_HEAD_요청은_가드가_켜져_있어도_키_없이_통과한다() throws Exception {
    // Given WRITE_GUARD_ENABLED=true, WRITE_API_KEY=secret123 로 기동했다
    WriteGuardFilter filter = new WriteGuardFilter(true, "secret123");

    // When X-Write-Key 헤더 없이 HEAD /api/performances 를 호출한다
    MockHttpServletRequest request = new MockHttpServletRequest("HEAD", "/api/performances");
    MockHttpServletResponse response = new MockHttpServletResponse();
    boolean[] chainCalled = {false};

    filter.doFilter(request, response, (req, resp) -> chainCalled[0] = true);

    // Then 2xx로 통과한다
    assertThat(chainCalled[0]).isTrue();
  }

  @Test
  void test_sc21_true가_아닌_값은_가드_비활성으로_처리된다() throws Exception {
    // Given WRITE_GUARD_ENABLED=1 로 기동했다 (Spring Boot 표준 boolean 바인딩에서 "true"가 아닌 값은
    // false)
    WriteGuardFilter filter = WriteGuardFilter.fromEnv("1", "secret123");

    // When X-Write-Key 헤더 없이 POST /api/performances 를 호출한다
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/performances");
    MockHttpServletResponse response = new MockHttpServletResponse();
    boolean[] chainCalled = {false};

    filter.doFilter(request, response, (req, resp) -> chainCalled[0] = true);

    // Then 가드가 꺼진 것과 동일하게 요청이 정상 처리된다(chain 으로 통과했다)
    assertThat(chainCalled[0]).isTrue();
  }

  @Test
  void test_sc24_WRITE_GUARD_ENABLED_는_대소문자와_무관하게_true면_가드가_켜진다() throws Exception {
    // Given WRITE_GUARD_ENABLED=TRUE (대문자), WRITE_API_KEY=secret123 로 기동했다
    WriteGuardFilter filter = WriteGuardFilter.fromEnv("TRUE", "secret123");

    // When X-Write-Key 헤더 없이 POST /api/performances 를 호출한다
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/performances");
    MockHttpServletResponse response = new MockHttpServletResponse();
    boolean[] chainCalled = {false};

    filter.doFilter(request, response, (req, resp) -> chainCalled[0] = true);

    // Then 401이 반환된다
    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(chainCalled[0]).isFalse();
  }
}
