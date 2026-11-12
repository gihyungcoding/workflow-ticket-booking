package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.accessTokenOf;
import static com.example.ticket_booking.support.AuthTestHelper.login;
import static com.example.ticket_booking.support.AuthTestHelper.signUp;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.domain.Seat;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.repository.PerformanceZoneRepository;
import com.example.ticket_booking.repository.SeatRepository;
import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.JsonText;
import com.example.ticket_booking.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * TASK-019 시나리오 SC-10 · SC-15 — 목록·상세 응답의 posterUrl 과 구역 행 없는 기존 공연.
 *
 * <p>클래스 레벨 {@code @Transactional} 을 두지 않는다 — 등록 API 로 만든 공연을 조회 API 로 다시 읽는다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-019.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class PerformanceBrowseResponseApiTest {

  private static final Instant T = Instant.parse("2026-11-11T08:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private PerformanceRepository performanceRepository;
  @Autowired private SeatRepository seatRepository;
  @Autowired private PerformanceZoneRepository zoneRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private MutableClock clock;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  /** TASK-022 — 공연 등록은 주최자 토큰이 있어야 한다(ADR-0029). */
  private String organizerToken;

  @BeforeEach
  void setUp() throws Exception {
    clock.setInstant(T);
    clearTables();
    signUp(mockMvc, "organizer@example.com", "password123", "한도윤", "ORGANIZER");
    organizerToken = accessTokenOf(login(mockMvc, "organizer@example.com", "password123"));
  }

  @AfterEach
  void tearDown() {
    clearTables();
  }

  private void clearTables() {
    zoneRepository.deleteAll();
    seatRepository.deleteAll();
    performanceRepository.deleteAll();
    userRepository.deleteAll();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getJson(String url) throws Exception {
    MvcResult result = mockMvc.perform(get(url)).andReturn();
    return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
  }

  @SuppressWarnings("unchecked")
  private Object registerR0(String title, String posterUrl) throws Exception {
    Map<String, Object> section = new LinkedHashMap<>();
    section.put("grade", "F1");
    section.put("price", 150000);
    section.put("rowStart", "A");
    section.put("rowEnd", "A");
    section.put("seatsPerRow", 10);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("title", title);
    body.put("venue", "KSPO DOME");
    body.put("startAt", T.plus(Duration.ofDays(30)).toString());
    body.put("openAt", T.plus(Duration.ofDays(7)).toString());
    body.put("closeAt", T.plus(Duration.ofDays(29)).toString());
    body.put("sections", List.of(section));
    if (posterUrl != null) {
      body.put("posterUrl", posterUrl);
    }
    MvcResult result =
        mockMvc
            .perform(
                post("/api/performances")
                    .header("Authorization", "Bearer " + organizerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonText.of(body)))
            .andReturn();
    return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class).get("id");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> itemOf(Map<String, Object> list, Object id) {
    return ((List<Map<String, Object>>) list.get("content"))
        .stream()
            .filter(item -> String.valueOf(id).equals(String.valueOf(item.get("id"))))
            .findFirst()
            .orElse(Map.of());
  }

  // ---------- SC-10 ----------

  /**
   * SC-10 (happy)
   *
   * <p>Given 공연 A(posterUrl 있음)·B(없음)가 등록돼 있다 When 목록을 조회한다 Then A 의 posterUrl, B 는 키 없음, 상세도 같다
   */
  @Test
  void test_sc10_목록_상세에_포스터가_있고_없는_공연은_키가_없다() throws Exception {
    // Given
    String poster = "https://cdn.example.com/a.jpg";
    Object a = registerR0("공연 A", poster);
    Object b = registerR0("공연 B", null);

    // When
    Map<String, Object> list = getJson("/api/performances");

    // Then
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(itemOf(list, a).get("posterUrl")).as("목록 A").isEqualTo(poster);
    softly.assertThat(itemOf(list, b)).as("목록 B").isNotEmpty().doesNotContainKey("posterUrl");
    softly
        .assertThat(getJson("/api/performances/" + a).get("posterUrl"))
        .as("상세 A")
        .isEqualTo(poster);
    softly
        .assertThat(getJson("/api/performances/" + b))
        .as("상세 B")
        .containsKey("id")
        .doesNotContainKey("posterUrl");
    softly.assertAll();
  }

  // ---------- SC-15 ----------

  /**
   * SC-15 (regression)
   *
   * <p>Given 공연 L 과 좌석(R 80000 10 · VIP 120000 10 · S 80000 5)을 저장소로 넣었다(구역 행·posterUrl 없음) When
   * 상세를 조회한다 Then posterUrl 키 없음, sections 는 VIP(1)·R(2)·S(3), area = grade, section 키 5개, 목록 항목 키
   * 9개
   */
  @Test
  @SuppressWarnings("unchecked")
  void test_sc15_구역_행이_없는_기존_공연의_응답은_기본_구역값만_더해진다() throws Exception {
    // Given
    Performance legacy =
        performanceRepository.save(
            new Performance(
                "기존 공연",
                "테스트홀",
                T.plus(Duration.ofDays(30)),
                T.plus(Duration.ofDays(7)),
                T.plus(Duration.ofDays(29)),
                25,
                25,
                false));
    List<Seat> seats = new ArrayList<>();
    addSeats(seats, legacy, "R", "A", 10, 80000);
    addSeats(seats, legacy, "VIP", "B", 10, 120000);
    addSeats(seats, legacy, "S", "C", 5, 80000);
    seatRepository.saveAll(seats);

    // When
    Map<String, Object> detail = getJson("/api/performances/" + legacy.getId());

    // Then
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(detail).as("posterUrl").containsKey("id").doesNotContainKey("posterUrl");
    List<Map<String, Object>> sections =
        detail.get("sections") == null
            ? List.of()
            : (List<Map<String, Object>>) detail.get("sections");
    softly
        .assertThat(sections)
        .extracting(s -> s.get("grade"))
        .as("순서")
        .containsExactly("VIP", "R", "S");
    softly
        .assertThat(sections)
        .extracting(s -> String.valueOf(s.get("stageOrder")))
        .as("stageOrder")
        .containsExactly("1", "2", "3");
    softly
        .assertThat(sections)
        .extracting(s -> s.get("area"))
        .as("area")
        .containsExactly("VIP", "R", "S");
    for (Map<String, Object> section : sections) {
      softly
          .assertThat(section.keySet())
          .as("section 키")
          .isEqualTo(Set.of("grade", "price", "seatCount", "area", "stageOrder"));
    }
    Map<String, Object> item = itemOf(getJson("/api/performances"), legacy.getId());
    softly
        .assertThat(item.keySet())
        .as("목록 항목 키")
        .isEqualTo(
            Set.of(
                "id",
                "title",
                "venue",
                "startAt",
                "openAt",
                "closeAt",
                "totalSeats",
                "availableSeats",
                "status"));
    softly.assertAll();
  }

  private static void addSeats(
      List<Seat> seats, Performance performance, String grade, String row, int count, int price) {
    for (int number = 1; number <= count; number++) {
      seats.add(new Seat(performance, grade, row, number, grade + "-" + row + number, price));
    }
  }
}
