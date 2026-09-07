package com.example.ticket_booking.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * TASK-001 시나리오 SC-01~SC-09, SC-12~SC-14.
 *
 * <p>SC-01~09는 attempt 1에서 이미 Green. SC-12~14(attempt 2, Phase 4 REJECT 대응)는 기존 구현이 이미 올바르게 처리하던 필터
 * 분기라 추가 즉시 통과한다 — CLOSED 분기에서만 결함이 있었고 UPCOMING/SOLD_OUT/CANCELLED 분기 자체는 처음부터
 * 맞았다(TEST_TASK-001.json 참고).
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-001.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
@Transactional
class PerformanceApiTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private PerformanceRepository performanceRepository;

  @Autowired private MutableClock clock;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  @BeforeEach
  void resetClock() {
    clock.setInstant(Instant.now());
  }

  private Performance performance(
      String title,
      Instant startAt,
      Instant openAt,
      Instant closeAt,
      int totalSeats,
      int availableSeats,
      boolean cancelled) {
    return performanceRepository.save(
        new Performance(
            title, "테스트홀", startAt, openAt, closeAt, totalSeats, availableSeats, cancelled));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> listBody(String... statusParam) throws Exception {
    var request = get("/api/performances");
    if (statusParam.length == 2) {
      request = request.param(statusParam[0], statusParam[1]);
    }
    String body =
        mockMvc
            .perform(request)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readValue(body, Map.class);
  }

  @Test
  void test_sc01_목록이_기본_페이지_형식으로_반환된다() throws Exception {
    // Given 공연 A, B가 등록되어 있고 둘 다 startAt >= now 이다
    Instant now = clock.instant();
    performance(
        "공연 A",
        now.plus(1, ChronoUnit.DAYS),
        now.minus(1, ChronoUnit.DAYS),
        now.plus(10, ChronoUnit.DAYS),
        100,
        50,
        false);
    performance(
        "공연 B",
        now.plus(2, ChronoUnit.DAYS),
        now.minus(1, ChronoUnit.DAYS),
        now.plus(10, ChronoUnit.DAYS),
        100,
        50,
        false);

    // When GET /api/performances 를 파라미터 없이 호출한다
    Map<String, Object> json = listBody();

    // Then 응답 바디의 최상위 필드는 content/page/size/totalElements 정확히 4개뿐이고 그 외 필드는 없다
    assertThat(json.keySet()).containsExactlyInAnyOrder("content", "page", "size", "totalElements");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) json.get("content");
    // Then content 에는 A, B 가 모두 포함된다
    assertThat(content)
        .extracting(row -> row.get("title"))
        .containsExactlyInAnyOrder("공연 A", "공연 B");
    // Then totalElements 는 2이다
    assertThat(((Number) json.get("totalElements")).longValue()).isEqualTo(2L);
  }

  @Test
  void test_sc02_statusOPEN_필터는_계산된_상태를_기준으로_적용된다() throws Exception {
    Instant now = clock.instant();
    // Given 공연 A는 openAt <= now <= closeAt 이고 availableSeats = 5 이다 (계산상 OPEN)
    performance(
        "공연 A",
        now.plus(5, ChronoUnit.DAYS),
        now.minus(1, ChronoUnit.DAYS),
        now.plus(1, ChronoUnit.DAYS),
        100,
        5,
        false);
    // Given 공연 B는 openAt <= now <= closeAt 이고 availableSeats = 0 이다 (계산상 SOLD_OUT)
    performance(
        "공연 B",
        now.plus(5, ChronoUnit.DAYS),
        now.minus(1, ChronoUnit.DAYS),
        now.plus(1, ChronoUnit.DAYS),
        100,
        0,
        false);

    // When GET /api/performances?status=OPEN 을 호출한다
    Map<String, Object> json = listBody("status", "OPEN");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) json.get("content");
    // Then content 에는 공연 A만 포함된다
    assertThat(content).extracting(row -> row.get("title")).containsExactly("공연 A");
    // Then totalElements 는 1이다
    assertThat(((Number) json.get("totalElements")).longValue()).isEqualTo(1L);
  }

  @Test
  void test_sc03_매진된_공연은_SOLD_OUT_으로_표시된다() throws Exception {
    Instant now = clock.instant();
    // Given 공연 C는 openAt <= now <= closeAt 이고 availableSeats = 0 이다
    Performance c =
        performance(
            "공연 C",
            now.plus(5, ChronoUnit.DAYS),
            now.minus(1, ChronoUnit.DAYS),
            now.plus(1, ChronoUnit.DAYS),
            100,
            0,
            false);

    // When GET /api/performances/{C.id} 를 호출한다
    // Then 200이 반환된다
    // Then 응답의 status 는 "SOLD_OUT" 이다
    mockMvc
        .perform(get("/api/performances/{id}", c.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SOLD_OUT"));
  }

  @Test
  void test_sc04_취소된_공연은_CANCELLED_로_표시된다() throws Exception {
    Instant now = clock.instant();
    // Given 공연 D는 cancelled = true 이다
    // Given 공연 D의 openAt <= now <= closeAt 이고 availableSeats = 10 이다 (조건만 보면 OPEN처럼 보인다)
    Performance d =
        performance(
            "공연 D",
            now.plus(5, ChronoUnit.DAYS),
            now.minus(1, ChronoUnit.DAYS),
            now.plus(1, ChronoUnit.DAYS),
            100,
            10,
            true);

    // When GET /api/performances/{D.id} 를 호출한다
    // Then 응답의 status 는 "CANCELLED" 이다
    mockMvc
        .perform(get("/api/performances/{id}", d.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void test_sc05_지난_공연은_필터_없는_목록에서_제외된다() throws Exception {
    Instant now = clock.instant();
    // Given 공연 E의 startAt < now 이다 (지난 공연)
    performance(
        "공연 E",
        now.minus(1, ChronoUnit.DAYS),
        now.minus(10, ChronoUnit.DAYS),
        now.minus(2, ChronoUnit.DAYS),
        100,
        50,
        false);
    // Given 공연 F의 startAt >= now 이다
    performance(
        "공연 F",
        now.plus(1, ChronoUnit.DAYS),
        now.minus(1, ChronoUnit.DAYS),
        now.plus(10, ChronoUnit.DAYS),
        100,
        50,
        false);

    // When GET /api/performances 를 파라미터 없이 호출한다
    Map<String, Object> json = listBody();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) json.get("content");
    // Then content 에는 공연 F만 포함된다
    // Then 공연 E는 content 에 없다
    assertThat(content).extracting(row -> row.get("title")).containsExactly("공연 F");
  }

  @Test
  void test_sc06_status필터가_일치해도_지난_공연은_여전히_제외된다() throws Exception {
    Instant now = clock.instant();
    // Given 공연 E의 startAt < now 이고 now > closeAt 이다 (지난 공연이면서 마감도 지났다)
    performance(
        "공연 E",
        now.minus(5, ChronoUnit.DAYS),
        now.minus(10, ChronoUnit.DAYS),
        now.minus(6, ChronoUnit.DAYS),
        100,
        50,
        false);
    // Given 공연 F의 startAt >= now 이고 now > closeAt 이다 (아직 시작 전이지만 마감 시각은 지난 '조기 마감' 케이스)
    performance(
        "공연 F",
        now.plus(5, ChronoUnit.DAYS),
        now.minus(10, ChronoUnit.DAYS),
        now.minus(1, ChronoUnit.DAYS),
        100,
        50,
        false);

    // When GET /api/performances?status=CLOSED 를 호출한다
    Map<String, Object> json = listBody("status", "CLOSED");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) json.get("content");
    // Then content 에는 공연 F만 포함된다
    // Then 공연 E는 content 에 없다
    assertThat(content).extracting(row -> row.get("title")).containsExactly("공연 F");
  }

  @Test
  void test_sc07_오픈_정각에_상태가_OPEN_으로_전환된다() throws Exception {
    // Given Clock 이 공연 G의 openAt 과 정확히 같은 시각 T로 고정되어 있다 (now == openAt == T)
    // Given T는 실제 시스템 시각과 다르다 (예: 몇 년 뒤의 미래 시각으로 openAt/closeAt/T를 함께 잡는다)
    Instant t = Instant.parse("2030-01-01T00:00:00Z");
    Instant openAt = t;
    Instant closeAt = t.plus(10, ChronoUnit.DAYS);
    // Given 공연 G의 availableSeats >= 1 이고 now <= closeAt 이다
    Performance g =
        performance("공연 G", closeAt.plus(1, ChronoUnit.DAYS), openAt, closeAt, 100, 1, false);
    clock.setInstant(t);

    // When GET /api/performances/{G.id} 를 호출한다
    // Then 응답의 status 는 "OPEN" 이다 (UPCOMING 이 아니다)
    mockMvc
        .perform(get("/api/performances/{id}", g.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OPEN"));
  }

  @Test
  void test_sc08_마감_1초_전까지는_CLOSED_로_전환되지_않는다() throws Exception {
    Instant closeAt = Instant.parse("2030-06-01T00:00:00Z");
    // Given Clock 이 공연 H의 closeAt 보다 1초 이른 시각 T로 고정되어 있다 (now == closeAt - 1s == T)
    // Given T는 실제 시스템 시각과 다르다
    Instant t = closeAt.minusSeconds(1);
    Instant openAt = t.minus(10, ChronoUnit.DAYS);
    // Given 공연 H의 availableSeats >= 1 이고 now >= openAt 이다
    Performance h =
        performance("공연 H", closeAt.plus(1, ChronoUnit.DAYS), openAt, closeAt, 100, 1, false);
    clock.setInstant(t);

    // When GET /api/performances/{H.id} 를 호출한다
    // Then 응답의 status 는 "OPEN" 이다 (CLOSED 가 아니다)
    mockMvc
        .perform(get("/api/performances/{id}", h.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OPEN"));
  }

  @Test
  void test_sc09_존재하지_않는_공연을_조회하면_404가_반환된다() throws Exception {
    // Given id 999 에 해당하는 공연이 존재하지 않는다
    // When GET /api/performances/999 를 호출한다
    // Then 404가 반환된다
    // Then 응답 바디의 code 는 "PERFORMANCE_NOT_FOUND" 이다
    mockMvc
        .perform(get("/api/performances/{id}", 999L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PERFORMANCE_NOT_FOUND"));
  }

  @Test
  void test_sc12_statusUPCOMING_필터가_정확히_적용된다() throws Exception {
    Instant now = clock.instant();
    // Given 공연 A는 now < openAt 이다 (계산상 UPCOMING)
    performance(
        "공연 A",
        now.plus(10, ChronoUnit.DAYS),
        now.plus(1, ChronoUnit.DAYS),
        now.plus(20, ChronoUnit.DAYS),
        100,
        50,
        false);
    // Given 공연 B는 openAt <= now <= closeAt 이고 availableSeats > 0 이다 (계산상 OPEN — 양성 대조군)
    performance(
        "공연 B",
        now.plus(5, ChronoUnit.DAYS),
        now.minus(1, ChronoUnit.DAYS),
        now.plus(1, ChronoUnit.DAYS),
        100,
        50,
        false);

    // When GET /api/performances?status=UPCOMING 를 호출한다
    Map<String, Object> json = listBody("status", "UPCOMING");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) json.get("content");
    // Then content 에는 공연 A만 포함된다
    assertThat(content).extracting(row -> row.get("title")).containsExactly("공연 A");
    // Then totalElements 는 1이다
    assertThat(((Number) json.get("totalElements")).longValue()).isEqualTo(1L);
    // Then content 의 공연 A 항목의 status 는 "UPCOMING" 이다
    assertThat(content.get(0).get("status")).isEqualTo("UPCOMING");
  }

  @Test
  void test_sc13_statusSOLD_OUT_필터가_정확히_적용된다() throws Exception {
    Instant now = clock.instant();
    // Given 공연 A는 openAt <= now <= closeAt 이고 availableSeats = 0 이다 (계산상 SOLD_OUT)
    performance(
        "공연 A",
        now.plus(5, ChronoUnit.DAYS),
        now.minus(1, ChronoUnit.DAYS),
        now.plus(1, ChronoUnit.DAYS),
        100,
        0,
        false);
    // Given 공연 B는 openAt <= now <= closeAt 이고 availableSeats > 0 이다 (계산상 OPEN — 양성 대조군)
    performance(
        "공연 B",
        now.plus(5, ChronoUnit.DAYS),
        now.minus(1, ChronoUnit.DAYS),
        now.plus(1, ChronoUnit.DAYS),
        100,
        50,
        false);

    // When GET /api/performances?status=SOLD_OUT 를 호출한다
    Map<String, Object> json = listBody("status", "SOLD_OUT");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) json.get("content");
    // Then content 에는 공연 A만 포함된다
    assertThat(content).extracting(row -> row.get("title")).containsExactly("공연 A");
    // Then totalElements 는 1이다
    assertThat(((Number) json.get("totalElements")).longValue()).isEqualTo(1L);
  }

  @Test
  void test_sc14_statusCANCELLED_필터가_정확히_적용된다() throws Exception {
    Instant now = clock.instant();
    // Given 공연 A는 cancelled = true 이고, openAt <= now <= closeAt 이며 availableSeats > 0 이다 (조건만 보면
    // OPEN처럼 보인다)
    performance(
        "공연 A",
        now.plus(5, ChronoUnit.DAYS),
        now.minus(1, ChronoUnit.DAYS),
        now.plus(1, ChronoUnit.DAYS),
        100,
        50,
        true);
    // Given 공연 B는 cancelled = false 이고, openAt <= now <= closeAt 이며 availableSeats > 0 이다 (계산상 OPEN
    // — 양성 대조군)
    performance(
        "공연 B",
        now.plus(5, ChronoUnit.DAYS),
        now.minus(1, ChronoUnit.DAYS),
        now.plus(1, ChronoUnit.DAYS),
        100,
        50,
        false);

    // When GET /api/performances?status=CANCELLED 를 호출한다
    Map<String, Object> json = listBody("status", "CANCELLED");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) json.get("content");
    // Then content 에는 공연 A만 포함된다
    assertThat(content).extracting(row -> row.get("title")).containsExactly("공연 A");
    // Then totalElements 는 1이다
    assertThat(((Number) json.get("totalElements")).longValue()).isEqualTo(1L);
  }
}
