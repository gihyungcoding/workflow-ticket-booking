package com.example.ticket_booking.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.domain.Seat;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.repository.SeatRepository;
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
 * TASK-008 시나리오 SC-01~SC-04.
 *
 * <p>SC-02/SC-03은 이번 태스크가 손대지 않는 기존 동작(404, 목록 응답 형태)을 검증하는
 * 회귀 시나리오라 Red 단계에서도 즉시 통과한다 — SC-02는 PerformanceApiTest의 test_sc09와
 * 동일한 경로를 검증한다(TASK-001에서 이미 구현됨). SC-01/SC-04만 이번 태스크가 실제로
 * 구현해야 하는 부분(sections 필드)이라 Red다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-008.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
@Transactional
class PerformanceSectionSummaryApiTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private PerformanceRepository performanceRepository;

  @Autowired private SeatRepository seatRepository;

  @Autowired private MutableClock clock;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  @BeforeEach
  void resetClock() {
    clock.setInstant(Instant.now());
  }

  private Performance performance(String title, int totalSeats, int availableSeats) {
    Instant now = clock.instant();
    return performanceRepository.save(
        new Performance(
            title,
            "테스트홀",
            now.plus(5, ChronoUnit.DAYS),
            now.minus(1, ChronoUnit.DAYS),
            now.plus(10, ChronoUnit.DAYS),
            totalSeats,
            availableSeats,
            false));
  }

  private void seats(Performance performance, String grade, String row, int count, int price) {
    for (int number = 1; number <= count; number++) {
      seatRepository.save(
          new Seat(performance, grade, row, number, grade + "-" + row + number, price));
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> detailBody(Long id) throws Exception {
    String body =
        mockMvc
            .perform(get("/api/performances/{id}", id))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readValue(body, Map.class);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> listBody() throws Exception {
    String body =
        mockMvc
            .perform(get("/api/performances"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readValue(body, Map.class);
  }

  @Test
  void test_sc01_상세_조회_시_구역별_좌석_요약이_포함된다() throws Exception {
    // Given 공연 P가 구역 2개로 등록되어 있다 — VIP(가격 120000원, 좌석 10석)와 R(가격 80000원, 좌석 30석)
    Performance p = performance("가을 재즈 콘서트", 40, 40);
    seats(p, "VIP", "A", 10, 120000);
    seats(p, "R", "B", 30, 80000);

    // When GET /api/performances/{P.id} 를 호출한다
    Map<String, Object> json = detailBody(p.getId());

    // Then 응답 본문의 sections 배열에 {grade: "VIP", price: 120000, seatCount: 10} 이 포함된다
    // Then 같은 배열에 {grade: "R", price: 80000, seatCount: 30} 이 포함된다
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> sections = (List<Map<String, Object>>) json.get("sections");
    assertThat(sections).isNotNull();
    assertThat(sections)
        .anySatisfy(
            section -> {
              assertThat(section.get("grade")).isEqualTo("VIP");
              assertThat(((Number) section.get("price")).intValue()).isEqualTo(120000);
              assertThat(((Number) section.get("seatCount")).longValue()).isEqualTo(10L);
            });
    assertThat(sections)
        .anySatisfy(
            section -> {
              assertThat(section.get("grade")).isEqualTo("R");
              assertThat(((Number) section.get("price")).intValue()).isEqualTo(80000);
              assertThat(((Number) section.get("seatCount")).longValue()).isEqualTo(30L);
            });
  }

  @Test
  void test_sc02_존재하지_않는_공연_조회_시_404가_반환된다() throws Exception {
    // Given id=999 인 공연이 존재하지 않는다
    // When GET /api/performances/999 를 호출한다
    // Then 404가 반환된다
    // Then 응답 본문의 code 가 "PERFORMANCE_NOT_FOUND" 이다
    mockMvc
        .perform(get("/api/performances/{id}", 999L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PERFORMANCE_NOT_FOUND"));
  }

  @Test
  void test_sc03_목록_조회_응답에는_sections_키가_없다() throws Exception {
    // Given 공연 P가 구역 1개 이상으로 등록되어 있고, GET /api/performances 결과 페이지에 P가 포함된다
    Performance p = performance("가을 재즈 콘서트", 10, 10);
    seats(p, "VIP", "A", 10, 120000);

    // When GET /api/performances 를 호출한다
    Map<String, Object> json = listBody();

    // Then 응답 본문의 content 배열에서 P에 해당하는 항목에 sections 키가 없다
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> content = (List<Map<String, Object>>) json.get("content");
    Map<String, Object> row =
        content.stream()
            .filter(item -> "가을 재즈 콘서트".equals(item.get("title")))
            .findFirst()
            .orElseThrow();
    assertThat(row).doesNotContainKey("sections");
  }

  @Test
  void test_sc04_서로_다른_구역이_같은_등급_가격이면_좌석_수가_합산된다() throws Exception {
    // Given 공연 P가 구역 2개로 등록되어 있다 — 둘 다 등급 R·가격 80000원이지만 행 범위가 달라
    // 첫 구역은 좌석 10석, 둘째 구역은 좌석 20석이다
    Performance p = performance("가을 재즈 콘서트", 30, 30);
    seats(p, "R", "A", 10, 80000);
    seats(p, "R", "B", 20, 80000);

    // When GET /api/performances/{P.id} 를 호출한다
    Map<String, Object> json = detailBody(p.getId());

    // Then 응답 본문의 sections 배열에 grade: "R", price: 80000 인 항목이 하나만 있다
    // Then 그 항목의 seatCount 는 30이다
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> sections = (List<Map<String, Object>>) json.get("sections");
    assertThat(sections).isNotNull();
    List<Map<String, Object>> rSections =
        sections.stream()
            .filter(
                section ->
                    "R".equals(section.get("grade"))
                        && ((Number) section.get("price")).intValue() == 80000)
            .toList();
    assertThat(rSections).hasSize(1);
    assertThat(((Number) rSections.get(0).get("seatCount")).longValue()).isEqualTo(30L);
  }
}
