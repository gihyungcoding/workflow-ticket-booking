package com.example.ticket_booking.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.repository.PerformanceZoneRepository;
import com.example.ticket_booking.repository.SeatRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
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
 * TASK-019 시나리오 SC-11~SC-13 — 홈 머리글 후보 GET /api/performances/next-opening.
 *
 * <p>공연은 저장소로 직접 넣는다(SC-12 f·g 는 등록 API 가 막는 시각 조합). 다른 테스트 클래스가 남긴 공연이 후보로 끼지 않도록 매번 테이블을 비운다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-019.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class PerformanceNextOpeningApiTest {

  private static final Instant T = Instant.parse("2026-11-11T08:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private PerformanceRepository performanceRepository;
  @Autowired private SeatRepository seatRepository;
  @Autowired private PerformanceZoneRepository zoneRepository;
  @Autowired private MutableClock clock;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  @BeforeEach
  void setUp() {
    clock.setInstant(T);
    clearTables();
  }

  @AfterEach
  void tearDown() {
    clearTables();
  }

  private void clearTables() {
    zoneRepository.deleteAll();
    seatRepository.deleteAll();
    performanceRepository.deleteAll();
  }

  private static Instant at(Duration offset) {
    return T.plus(offset);
  }

  private Performance save(
      String title, boolean cancelled, Instant openAt, Instant closeAt, Instant startAt) {
    return performanceRepository.save(
        new Performance(title, "테스트홀", startAt, openAt, closeAt, 100, 100, cancelled));
  }

  private MvcResult nextOpening() throws Exception {
    return mockMvc.perform(get("/api/performances/next-opening")).andReturn();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> json(MvcResult result) throws Exception {
    return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
  }

  private void saveExcluded() {
    save("P3", true, at(Duration.ofMinutes(30)), at(Duration.ofDays(5)), at(Duration.ofDays(6)));
    save("P4", false, at(Duration.ofHours(-1)), at(Duration.ofDays(5)), at(Duration.ofDays(6)));
    save("P5", false, at(Duration.ofDays(-3)), at(Duration.ofDays(-2)), at(Duration.ofDays(-1)));
  }

  // ---------- SC-11 ----------

  /**
   * SC-11 (happy)
   *
   * <p>Given 시계 T, 표의 P1~P5 (P2 는 posterUrl 있음) When 인증 없이 next-opening 을 호출한다 Then 200, id P2,
   * status UPCOMING, posterUrl, sections 키 없음
   */
  @Test
  void test_sc11_머리글_후보는_아직_열리지_않은_공연_중_가장_이른_오픈이다() throws Exception {
    // Given
    save("P1", false, at(Duration.ofHours(2)), at(Duration.ofDays(5)), at(Duration.ofDays(6)));
    Performance p2 =
        performanceRepository.save(
            new Performance(
                "P2",
                "테스트홀",
                at(Duration.ofDays(6)),
                at(Duration.ofHours(1)),
                at(Duration.ofDays(5)),
                100,
                100,
                false,
                "https://cdn.example.com/p2.jpg"));
    saveExcluded();

    // When
    MvcResult result = nextOpening();

    // Then
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(result.getResponse().getStatus()).as("상태").isEqualTo(200);
    if (result.getResponse().getStatus() == 200) {
      Map<String, Object> body = json(result);
      softly
          .assertThat(String.valueOf(body.get("id")))
          .as("id")
          .isEqualTo(String.valueOf(p2.getId()));
      softly.assertThat(body.get("status")).as("status").isEqualTo("UPCOMING");
      softly
          .assertThat(body.get("posterUrl"))
          .as("posterUrl")
          .isEqualTo("https://cdn.example.com/p2.jpg");
      softly.assertThat(body).as("sections").doesNotContainKey("sections");
    }
    softly.assertAll();
  }

  // ---------- SC-12 ----------

  /**
   * SC-12 (boundary)
   *
   * <p>Given 시계 T, 표의 행마다 그 공연만 저장돼 있다 When next-opening 을 호출한다 Then 표의 결과 (204 면 본문 비어 있음)
   */
  @Test
  void test_sc12_머리글_후보의_시각_경계와_같은_오픈_시각() throws Exception {
    Duration d5 = Duration.ofDays(5);
    Duration d6 = Duration.ofDays(6);
    Duration h1 = Duration.ofHours(1);
    SoftAssertions softly = new SoftAssertions();

    // a openAt == now → 204
    clearTables();
    save("Q", false, T, at(d5), at(d6));
    expectNoContent(softly, "a");

    // b now 바로 뒤 → Q
    clearTables();
    Performance b = save("Q", false, at(Duration.ofMillis(1)), at(d5), at(d6));
    expectChosen(softly, "b", b);

    // c 같은 시각의 열린 공연이 끼지 않는다 → R
    clearTables();
    save("Q", false, T, at(d5), at(d6));
    Performance c = save("R", false, at(h1), at(d5), at(d6));
    expectChosen(softly, "c", c);

    // d 같은 openAt → id 작은 X
    clearTables();
    Performance dx = save("X", false, at(h1), at(d5), at(d6));
    save("Y", false, at(h1), at(d5), at(d6));
    expectChosen(softly, "d", dx);

    // e openAt 이 id 보다 먼저 → Y
    clearTables();
    save("X", false, at(Duration.ofHours(2)), at(d5), at(d6));
    Performance ey = save("Y", false, at(h1), at(d5), at(d6));
    expectChosen(softly, "e", ey);

    // f startAt == now → Q
    clearTables();
    Performance f = save("Q", false, at(h1), at(h1), T);
    expectChosen(softly, "f", f);

    // g startAt < now → 204
    clearTables();
    save("Q", false, at(h1), at(h1), at(Duration.ofMillis(-1)));
    expectNoContent(softly, "g");

    softly.assertAll();
  }

  private void expectChosen(SoftAssertions softly, String label, Performance expected)
      throws Exception {
    MvcResult result = nextOpening();
    softly.assertThat(result.getResponse().getStatus()).as("%s 상태", label).isEqualTo(200);
    if (result.getResponse().getStatus() == 200) {
      softly
          .assertThat(String.valueOf(json(result).get("id")))
          .as("%s id", label)
          .isEqualTo(String.valueOf(expected.getId()));
    }
  }

  private void expectNoContent(SoftAssertions softly, String label) throws Exception {
    MvcResult result = nextOpening();
    softly.assertThat(result.getResponse().getStatus()).as("%s 상태", label).isEqualTo(204);
    softly.assertThat(result.getResponse().getContentAsByteArray()).as("%s 본문", label).isEmpty();
  }

  // ---------- SC-13 ----------

  /**
   * SC-13 (error)
   *
   * <p>Given 시계 T, 표의 상태(a 없음 · b P3·P4·P5 만) When next-opening 을 호출한다 Then 204, 본문 길이 0
   */
  @Test
  void test_sc13_후보가_없으면_204_이고_본문이_비어_있다() throws Exception {
    SoftAssertions softly = new SoftAssertions();

    // a 없음
    clearTables();
    expectNoContent(softly, "a");

    // b P3·P4·P5 만
    clearTables();
    saveExcluded();
    expectNoContent(softly, "b");

    softly.assertAll();
  }
}
