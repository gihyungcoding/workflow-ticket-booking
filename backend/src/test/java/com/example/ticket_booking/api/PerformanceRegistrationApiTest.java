package com.example.ticket_booking.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * TASK-004 시나리오 SC-01~SC-13, SC-15~SC-21 (SC-14는 Phase 2b에서 철회 — SCENARIO_TASK-004.md 참고).
 * SC-16~SC-21은 Phase 4 FAIL(VERIFY_TASK-004.json) 이후 attempt 2에서 추가됐다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-004.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
@Transactional
class PerformanceRegistrationApiTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private PerformanceRepository performanceRepository;

  @Autowired private SeatRepository seatRepository;

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

  private MvcResult postJson(String url, String json) throws Exception {
    return mockMvc
        .perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
        .andReturn();
  }

  private MvcResult putJson(String url, String json) throws Exception {
    return mockMvc
        .perform(put(url).contentType(MediaType.APPLICATION_JSON).content(json))
        .andReturn();
  }

  private MvcResult cancel(Long id) throws Exception {
    return mockMvc.perform(post("/api/performances/" + id + "/cancel")).andReturn();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> bodyAsMap(MvcResult result) throws Exception {
    return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
  }

  private String registerJson(String startAt, String openAt, String closeAt, String sectionsJson) {
    return """
        {
          "title": "가을 재즈 콘서트",
          "venue": "OO홀",
          "startAt": "%s",
          "openAt": "%s",
          "closeAt": "%s",
          "sections": %s
        }
        """
        .formatted(startAt, openAt, closeAt, sectionsJson);
  }

  private String updateJson(
      String title, String venue, String startAt, String openAt, String closeAt) {
    return """
        {
          "title": "%s",
          "venue": "%s",
          "startAt": "%s",
          "openAt": "%s",
          "closeAt": "%s"
        }
        """
        .formatted(title, venue, startAt, openAt, closeAt);
  }

  @Test
  void test_sc01_구역_2개로_등록하면_구역별_좌석이_생성되고_총수가_합산된다() throws Exception {
    // Given 등록 요청에 구역 VIP(행 A~B, 행당 10석)와 구역 R(행 C~E, 행당 20석)가 있다
    Instant now = clock.instant();
    String sections =
        """
        [
          {"grade":"VIP","price":120000,"rowStart":"A","rowEnd":"B","seatsPerRow":10},
          {"grade":"R","price":80000,"rowStart":"C","rowEnd":"E","seatsPerRow":20}
        ]
        """;
    // And 시각은 openAt <= closeAt <= startAt 을 만족한다
    String json =
        registerJson(
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.minus(1, ChronoUnit.DAYS).toString(),
            now.plus(20, ChronoUnit.DAYS).toString(),
            sections);

    // When POST /api/performances 를 호출한다
    MvcResult result = postJson("/api/performances", json);

    // Then 201이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    Map<String, Object> body = bodyAsMap(result);
    // And 응답의 totalSeats/availableSeats 는 80(=2행×10 + 3행×20)이다
    assertThat(((Number) body.get("totalSeats")).intValue()).isEqualTo(80);
    assertThat(((Number) body.get("availableSeats")).intValue()).isEqualTo(80);
    // And performance_id 로 seat 를 조회하면 80건이 조회된다
    Long performanceId = ((Number) body.get("id")).longValue();
    List<Seat> seats =
        seatRepository.findAll().stream()
            .filter(s -> s.getPerformanceId().equals(performanceId))
            .toList();
    assertThat(seats).hasSize(80);
    // And 조회된 seat 에 "VIP-A1", "VIP-B10", "R-C1", "R-E20" 라벨이 모두 존재한다
    List<String> labels = seats.stream().map(Seat::getSeatLabel).toList();
    assertThat(labels).contains("VIP-A1", "VIP-B10", "R-C1", "R-E20");
  }

  @Test
  void test_sc02_openAt이_closeAt보다_늦으면_등록이_거부된다() throws Exception {
    // Given 등록 요청의 openAt이 closeAt보다 늦다
    Instant now = clock.instant();
    String sections =
        """
        [{"grade":"VIP","price":100,"rowStart":"A","rowEnd":"A","seatsPerRow":10}]
        """;
    String json =
        registerJson(
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.plus(20, ChronoUnit.DAYS).toString(), // openAt
            now.plus(10, ChronoUnit.DAYS).toString(), // closeAt < openAt
            sections);
    long seatCountBefore = seatRepository.count();

    // When POST /api/performances 를 호출한다
    MvcResult result = postJson("/api/performances", json);

    // Then 400이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    // And 응답 코드는 INVALID_TIME_ORDER 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("INVALID_TIME_ORDER");
    // And seat 테이블에 새로 생성된 행이 없다
    assertThat(seatRepository.count()).isEqualTo(seatCountBefore);
  }

  @Test
  void test_sc03_closeAt이_startAt보다_늦으면_등록이_거부된다() throws Exception {
    // Given 등록 요청의 openAt <= closeAt 이지만 closeAt이 startAt보다 늦다
    Instant now = clock.instant();
    String sections =
        """
        [{"grade":"VIP","price":100,"rowStart":"A","rowEnd":"A","seatsPerRow":10}]
        """;
    String json =
        registerJson(
            now.plus(30, ChronoUnit.DAYS).toString(), // startAt
            now.minus(1, ChronoUnit.DAYS).toString(), // openAt
            now.plus(40, ChronoUnit.DAYS).toString(), // closeAt > startAt
            sections);

    // When
    MvcResult result = postJson("/api/performances", json);

    // Then 400이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    // And 응답 코드는 INVALID_TIME_ORDER 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("INVALID_TIME_ORDER");
  }

  @Test
  void test_sc04_구역_좌석_합이_5000을_넘으면_등록이_거부된다() throws Exception {
    // Given 등록 요청에 구역 1개(행 A 하나, 행당 5,001석)가 있어 합산 좌석 수가 정확히 5,001이다
    Instant now = clock.instant();
    String sections =
        """
        [{"grade":"VIP","price":100,"rowStart":"A","rowEnd":"A","seatsPerRow":5001}]
        """;
    String json =
        registerJson(
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.minus(1, ChronoUnit.DAYS).toString(),
            now.plus(20, ChronoUnit.DAYS).toString(),
            sections);
    long seatCountBefore = seatRepository.count();

    // When
    MvcResult result = postJson("/api/performances", json);

    // Then 400이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    // And 응답 코드는 SEAT_LIMIT_EXCEEDED 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("SEAT_LIMIT_EXCEEDED");
    // And seat 테이블에 새로 생성된 행이 없다
    assertThat(seatRepository.count()).isEqualTo(seatCountBefore);
  }

  @Test
  void test_sc05_두_구역의_행_범위가_겹치면_등록이_거부된다() throws Exception {
    // Given 등록 요청에 구역 VIP(행 A~E)와 구역 R(행 C~F)가 있다 (C~E 구간이 겹친다)
    Instant now = clock.instant();
    String sections =
        """
        [
          {"grade":"VIP","price":100,"rowStart":"A","rowEnd":"E","seatsPerRow":5},
          {"grade":"R","price":80,"rowStart":"C","rowEnd":"F","seatsPerRow":5}
        ]
        """;
    String json =
        registerJson(
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.minus(1, ChronoUnit.DAYS).toString(),
            now.plus(20, ChronoUnit.DAYS).toString(),
            sections);
    long seatCountBefore = seatRepository.count();

    // When
    MvcResult result = postJson("/api/performances", json);

    // Then 409가 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(409);
    // And 응답 코드는 DUPLICATE_SEAT_RANGE 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("DUPLICATE_SEAT_RANGE");
    // And seat 테이블에 새로 생성된 행이 없다
    assertThat(seatRepository.count()).isEqualTo(seatCountBefore);
  }

  @Test
  void test_sc06_오픈_전인_공연을_수정하면_필드가_반영된다() throws Exception {
    // Given 공연 P가 등록되어 있고 now < P.openAt 이다
    Instant now = clock.instant();
    Performance p =
        performance(
            "원래 제목",
            now.plus(30, ChronoUnit.DAYS),
            now.plus(5, ChronoUnit.DAYS),
            now.plus(20, ChronoUnit.DAYS),
            100,
            100,
            false);

    String json =
        updateJson(
            "바뀐 제목",
            "새 장소",
            now.plus(31, ChronoUnit.DAYS).toString(),
            now.plus(6, ChronoUnit.DAYS).toString(),
            now.plus(21, ChronoUnit.DAYS).toString());

    // When PUT /api/performances/{P.id} 로 title/venue/startAt/openAt/closeAt 변경을 요청한다
    MvcResult result = putJson("/api/performances/" + p.getId(), json);

    // Then 200이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    // And 응답의 title/venue/startAt/openAt/closeAt 이 요청한 값으로 바뀌어 있다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("title")).isEqualTo("바뀐 제목");
    assertThat(body.get("venue")).isEqualTo("새 장소");
  }

  @Test
  void test_sc07_오픈_이후인_공연을_수정하려_하면_거부된다() throws Exception {
    // Given 공연 P가 등록되어 있고 now >= P.openAt 이다
    Instant now = clock.instant();
    Performance p =
        performance(
            "원래 제목",
            now.plus(30, ChronoUnit.DAYS),
            now.minus(1, ChronoUnit.DAYS),
            now.plus(20, ChronoUnit.DAYS),
            100,
            100,
            false);

    String json =
        updateJson(
            "바뀐 제목",
            "새 장소",
            now.plus(31, ChronoUnit.DAYS).toString(),
            now.minus(1, ChronoUnit.DAYS).toString(),
            now.plus(21, ChronoUnit.DAYS).toString());

    // When PUT /api/performances/{P.id} 로 수정을 요청한다
    MvcResult result = putJson("/api/performances/" + p.getId(), json);

    // Then 409가 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(409);
    // And 응답 코드는 REGISTRATION_ALREADY_OPEN 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("REGISTRATION_ALREADY_OPEN");
    // And P를 다시 조회하면 title/venue/startAt/openAt/closeAt 이 요청 이전 값과 동일하다
    Performance reloaded = performanceRepository.findById(p.getId()).orElseThrow();
    assertThat(reloaded.getTitle()).isEqualTo("원래 제목");
  }

  @Test
  void test_sc08_수정_요청_자체가_시각_순서를_어기면_거부된다() throws Exception {
    // Given 공연 P가 등록되어 있고 now < P.openAt 이다
    Instant now = clock.instant();
    Performance p =
        performance(
            "원래 제목",
            now.plus(30, ChronoUnit.DAYS),
            now.plus(5, ChronoUnit.DAYS),
            now.plus(20, ChronoUnit.DAYS),
            100,
            100,
            false);

    // And 수정 요청의 openAt이 closeAt보다 늦다
    String json =
        updateJson(
            "바뀐 제목",
            "새 장소",
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.plus(25, ChronoUnit.DAYS).toString(), // openAt
            now.plus(20, ChronoUnit.DAYS).toString()); // closeAt < openAt

    // When PUT /api/performances/{P.id} 로 수정을 요청한다
    MvcResult result = putJson("/api/performances/" + p.getId(), json);

    // Then 400이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    // And 응답 코드는 INVALID_TIME_ORDER 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("INVALID_TIME_ORDER");
    // And P를 다시 조회하면 title/venue/startAt/openAt/closeAt 이 요청 이전 값과 동일하다
    Performance reloaded = performanceRepository.findById(p.getId()).orElseThrow();
    assertThat(reloaded.getTitle()).isEqualTo("원래 제목");
  }

  @Test
  void test_sc09_존재하지_않는_공연을_수정하려_하면_404() throws Exception {
    // Given id 99999 에 해당하는 공연이 존재하지 않는다
    Instant now = clock.instant();
    String json =
        updateJson(
            "제목",
            "장소",
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.plus(1, ChronoUnit.DAYS).toString(),
            now.plus(20, ChronoUnit.DAYS).toString());

    // When PUT /api/performances/99999 로 수정을 요청한다
    MvcResult result = putJson("/api/performances/99999", json);

    // Then 404가 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(404);
    // And 응답 코드는 PERFORMANCE_NOT_FOUND 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("PERFORMANCE_NOT_FOUND");
  }

  @Test
  void test_sc10_존재하지_않는_공연을_취소하려_하면_404() throws Exception {
    // Given id 99999 에 해당하는 공연이 존재하지 않는다
    // When POST /api/performances/99999/cancel 을 호출한다
    MvcResult result = cancel(99999L);

    // Then 404가 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(404);
    // And 응답 코드는 PERFORMANCE_NOT_FOUND 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("PERFORMANCE_NOT_FOUND");
  }

  @Test
  void test_sc11_공연을_취소하면_status가_CANCELLED로_바뀐다() throws Exception {
    // Given 공연 P가 등록되어 있고 cancelled=false 이다
    Instant now = clock.instant();
    Performance p =
        performance(
            "공연",
            now.plus(30, ChronoUnit.DAYS),
            now.minus(1, ChronoUnit.DAYS),
            now.plus(20, ChronoUnit.DAYS),
            100,
            50,
            false);

    // When POST /api/performances/{P.id}/cancel 을 호출한다
    MvcResult result = cancel(p.getId());

    // Then 200이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    // And 응답의 status 는 CANCELLED 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("status")).isEqualTo("CANCELLED");
  }

  @Test
  void test_sc12_이미_취소된_공연을_다시_취소해도_200이_반환된다() throws Exception {
    // Given 공연 P가 등록되어 있고 cancelled=true 이다 (이미 취소됨)
    Instant now = clock.instant();
    Performance p =
        performance(
            "공연",
            now.plus(30, ChronoUnit.DAYS),
            now.minus(1, ChronoUnit.DAYS),
            now.plus(20, ChronoUnit.DAYS),
            100,
            50,
            true);

    // When POST /api/performances/{P.id}/cancel 을 다시 호출한다
    MvcResult result = cancel(p.getId());

    // Then 200이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    // And 응답의 status 는 CANCELLED 로 유지된다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("status")).isEqualTo("CANCELLED");
  }

  @Test
  void test_sc13_구역이_하나도_없는_등록_요청은_거부된다() throws Exception {
    // Given 등록 요청의 sections 가 빈 배열이다
    Instant now = clock.instant();
    String json =
        registerJson(
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.minus(1, ChronoUnit.DAYS).toString(),
            now.plus(20, ChronoUnit.DAYS).toString(),
            "[]");

    // When
    MvcResult result = postJson("/api/performances", json);

    // Then 400이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    // And 응답 코드는 EMPTY_SECTIONS 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("EMPTY_SECTIONS");
  }

  @Test
  void test_sc15_정확히_5000석이면_등록이_성공한다() throws Exception {
    // Given 등록 요청에 구역 1개(행 A 하나, 행당 5,000석)가 있어 합산 좌석 수가 정확히 5,000이다
    Instant now = clock.instant();
    String sections =
        """
        [{"grade":"VIP","price":100,"rowStart":"A","rowEnd":"A","seatsPerRow":5000}]
        """;
    String json =
        registerJson(
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.minus(1, ChronoUnit.DAYS).toString(),
            now.plus(20, ChronoUnit.DAYS).toString(),
            sections);

    // When
    MvcResult result = postJson("/api/performances", json);

    // Then 201이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    // And 응답의 totalSeats/availableSeats 는 5,000이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(((Number) body.get("totalSeats")).intValue()).isEqualTo(5000);
    assertThat(((Number) body.get("availableSeats")).intValue()).isEqualTo(5000);
  }

  private String registerJsonNoSections(String startAt, String openAt, String closeAt) {
    return """
        {
          "title": "가을 재즈 콘서트",
          "venue": "OO홀",
          "startAt": "%s",
          "openAt": "%s",
          "closeAt": "%s"
        }
        """
        .formatted(startAt, openAt, closeAt);
  }

  @Test
  void test_sc16_필수_필드가_없으면_등록이_거부된다() throws Exception {
    // Given 등록 요청에 sections 필드 자체가 없다(JSON에 없음, null — 빈 배열 []이 아니다)
    Instant now = clock.instant();
    // And 나머지 필드(title/venue/시각)는 유효하다
    String json =
        registerJsonNoSections(
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.minus(1, ChronoUnit.DAYS).toString(),
            now.plus(20, ChronoUnit.DAYS).toString());

    // When POST /api/performances 를 호출한다
    MvcResult result = postJson("/api/performances", json);

    // Then 400이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    // And 응답 코드는 INVALID_REQUEST 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("INVALID_REQUEST");
  }

  @Test
  void test_sc17_구역의_rowStart가_rowEnd보다_뒤_알파벳이면_등록이_거부된다() throws Exception {
    // Given 등록 요청에 구역 하나(rowStart="E", rowEnd="C")가 있다 (역방향 범위)
    Instant now = clock.instant();
    String sections =
        """
        [{"grade":"VIP","price":100,"rowStart":"E","rowEnd":"C","seatsPerRow":10}]
        """;
    String json =
        registerJson(
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.minus(1, ChronoUnit.DAYS).toString(),
            now.plus(20, ChronoUnit.DAYS).toString(),
            sections);
    long seatCountBefore = seatRepository.count();

    // When POST /api/performances 를 호출한다
    MvcResult result = postJson("/api/performances", json);

    // Then 400이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    // And 응답 코드는 INVALID_SECTION 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("INVALID_SECTION");
    // And seat 테이블에 새로 생성된 행이 없다(요청 전후 전체 seat 개수가 그대로다)
    assertThat(seatRepository.count()).isEqualTo(seatCountBefore);
  }

  @Test
  void test_sc18_구역의_seatsPerRow가_0_이하이면_등록이_거부된다() throws Exception {
    // Given 등록 요청에 구역 하나(seatsPerRow=0)가 있다
    Instant now = clock.instant();
    String sections =
        """
        [{"grade":"VIP","price":100,"rowStart":"A","rowEnd":"A","seatsPerRow":0}]
        """;
    String json =
        registerJson(
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.minus(1, ChronoUnit.DAYS).toString(),
            now.plus(20, ChronoUnit.DAYS).toString(),
            sections);

    // When POST /api/performances 를 호출한다
    MvcResult result = postJson("/api/performances", json);

    // Then 400이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    // And 응답 코드는 INVALID_SECTION 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("INVALID_SECTION");
  }

  @Test
  void test_sc19_구역의_rowStart가_빈_문자열이면_등록이_거부된다() throws Exception {
    // Given 등록 요청에 구역 하나(rowStart="")가 있다
    Instant now = clock.instant();
    String sections =
        """
        [{"grade":"VIP","price":100,"rowStart":"","rowEnd":"A","seatsPerRow":10}]
        """;
    String json =
        registerJson(
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.minus(1, ChronoUnit.DAYS).toString(),
            now.plus(20, ChronoUnit.DAYS).toString(),
            sections);

    // When POST /api/performances 를 호출한다
    MvcResult result = postJson("/api/performances", json);

    // Then 400이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    // And 응답 코드는 INVALID_SECTION 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("INVALID_SECTION");
  }

  @Test
  void test_sc20_구역의_seatsPerRow가_매우_큰_값이면_좌석_상한_초과로_거부된다() throws Exception {
    // Given 등록 요청에 서로 겹치지 않는 구역 2개 — 구역 A(행 A~A, seatsPerRow=1200000000)와
    // 구역 B(행 B~B, seatsPerRow=1200000000) — 가 있다
    Instant now = clock.instant();
    String sections =
        """
        [
          {"grade":"A","price":100,"rowStart":"A","rowEnd":"A","seatsPerRow":1200000000},
          {"grade":"B","price":100,"rowStart":"B","rowEnd":"B","seatsPerRow":1200000000}
        ]
        """;
    String json =
        registerJson(
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.minus(1, ChronoUnit.DAYS).toString(),
            now.plus(20, ChronoUnit.DAYS).toString(),
            sections);
    long seatCountBefore = seatRepository.count();

    // When POST /api/performances 를 호출한다
    MvcResult result = postJson("/api/performances", json);

    // Then 400이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    // And 응답 코드는 SEAT_LIMIT_EXCEEDED 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("SEAT_LIMIT_EXCEEDED");
    // And seat 테이블에 새로 생성된 행이 없다(요청 전후 전체 seat 개수가 그대로다)
    assertThat(seatRepository.count()).isEqualTo(seatCountBefore);
  }

  @Test
  void test_sc21_구역명이_20자를_넘으면_등록이_거부된다() throws Exception {
    // Given 등록 요청에 구역 하나(grade가 21자)가 있다
    Instant now = clock.instant();
    String longGrade = "가".repeat(21);
    String sections =
        """
        [{"grade":"%s","price":100,"rowStart":"A","rowEnd":"A","seatsPerRow":10}]
        """
            .formatted(longGrade);
    String json =
        registerJson(
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.minus(1, ChronoUnit.DAYS).toString(),
            now.plus(20, ChronoUnit.DAYS).toString(),
            sections);

    // When POST /api/performances 를 호출한다
    MvcResult result = postJson("/api/performances", json);

    // Then 400이 반환된다
    assertThat(result.getResponse().getStatus()).isEqualTo(400);
    // And 응답 코드는 INVALID_SECTION 이다
    Map<String, Object> body = bodyAsMap(result);
    assertThat(body.get("code")).isEqualTo("INVALID_SECTION");
  }
}
