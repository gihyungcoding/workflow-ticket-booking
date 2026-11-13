package com.example.ticket_booking.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * TASK-023 시나리오 SC-21 — 관객 목록의 page·size 경계(Phase 4 RC-2). 주최자 목록(OrganizerVisibilityApiTest
 * SC-07)과 같은 표다.
 *
 * <p>클래스 레벨 {@code @Transactional} 을 두지 않는다 — 공연 수는 리포지토리 count 로 새로 읽는다(CLAUDE.md 리뷰 지시). 처리되지 않은
 * 예외로 MockMvc 가 던지면 그 요청은 500 으로 센다(운영에서는 500 응답이 된다).
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-023.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class PerformanceListPagingApiTest {

  private static final Instant T = Instant.parse("2026-11-13T09:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private PerformanceRepository performanceRepository;
  @Autowired private MutableClock clock;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  @BeforeEach
  void setUp() {
    clock.setInstant(T);
    performanceRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    performanceRepository.deleteAll();
  }

  /** 응답이 오지 않고 처리되지 않은 예외가 올라오면 null — 운영에서는 500 이다. */
  private MvcResult call(String query) {
    try {
      return mockMvc.perform(get("/api/performances" + query)).andReturn();
    } catch (Exception exception) {
      return null;
    }
  }

  private static int status(MvcResult result) {
    return result == null ? 500 : result.getResponse().getStatus();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> json(MvcResult result) throws Exception {
    if (result == null || result.getResponse().getContentAsString().isEmpty()) {
      return Map.of();
    }
    return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
  }

  private static long num(Map<String, Object> body, String key) {
    Object value = body.get(key);
    return value == null ? -1L : ((Number) value).longValue();
  }

  private int contentSize(MvcResult result) throws Exception {
    Object content = json(result).get("content");
    return content == null ? -1 : ((List<?>) content).size();
  }

  /**
   * SC-21 (error)
   *
   * <p>Given 공개 공연 25개 When 토큰 없이 표의 쿼리로 GET /api/performances 를 부른다 Then a~c·h 는 400
   * INVALID_REQUEST, i~l·n·o 는 400, d~g·m 은 표와 같은 200 응답, 요청 뒤 공연 수는 25 그대로
   */
  @Test
  void test_sc21_관객_목록의_page_size_경계도_400_이거나_자른_값으로_응답한다() throws Exception {
    // Given
    for (int i = 0; i < 25; i++) {
      performanceRepository.save(
          new Performance(
              "공연 " + i,
              "롤링홀",
              T.plus(Duration.ofDays(30 + i)),
              T.plus(Duration.ofDays(7)),
              T.plus(Duration.ofDays(29)),
              100,
              100,
              false));
    }
    SoftAssertions soft = new SoftAssertions();

    // When·Then 400 INVALID_REQUEST 행 (a·b·c·h)
    for (String query : new String[] {"?page=-1", "?size=0", "?size=-1", "?page=2147483647"}) {
      MvcResult result = call(query);
      soft.assertThat(status(result)).as(query + " 상태").isEqualTo(400);
      soft.assertThat(json(result).get("code")).as(query + " code").isEqualTo("INVALID_REQUEST");
    }
    // And 타입 불일치 400 행 (i·j·k·l·n·o)
    for (String query :
        new String[] {
          "?page=2147483648",
          "?page=1.5",
          "?size=1.5",
          "?page=9223372036854775808",
          "?size=9223372036854775808",
          "?size=2147483648"
        }) {
      soft.assertThat(status(call(query))).as(query + " 상태").isEqualTo(400);
    }
    // And 200 행 (d·e·f·g·m)
    MvcResult first = call("?page=0");
    soft.assertThat(status(first)).as("page=0").isEqualTo(200);
    soft.assertThat(num(json(first), "page")).isEqualTo(0L);
    soft.assertThat(contentSize(first)).as("page=0 content").isEqualTo(20);
    MvcResult second = call("?page=1");
    soft.assertThat(contentSize(second)).as("page=1 content").isEqualTo(5);
    soft.assertThat(num(json(second), "totalElements")).isEqualTo(25L);
    MvcResult far = call("?page=1000");
    soft.assertThat(status(far)).as("page=1000").isEqualTo(200);
    soft.assertThat(contentSize(far)).as("page=1000 content").isEqualTo(0);
    soft.assertThat(num(json(far), "totalElements")).isEqualTo(25L);
    MvcResult big = call("?size=101");
    soft.assertThat(status(big)).as("size=101").isEqualTo(200);
    soft.assertThat(num(json(big), "size")).isEqualTo(100L);
    soft.assertThat(contentSize(big)).as("size=101 content").isEqualTo(25);
    MvcResult blank = call("?page=&size=");
    soft.assertThat(status(blank)).as("빈 값").isEqualTo(200);
    soft.assertThat(num(json(blank), "page")).isEqualTo(0L);
    soft.assertThat(num(json(blank), "size")).isEqualTo(20L);
    // And 요청 뒤 공연 수는 25 그대로다
    soft.assertThat(performanceRepository.count()).isEqualTo(25L);
    soft.assertAll();
  }
}
