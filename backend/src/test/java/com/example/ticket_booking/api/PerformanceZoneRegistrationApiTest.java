package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.accessTokenOf;
import static com.example.ticket_booking.support.AuthTestHelper.login;
import static com.example.ticket_booking.support.AuthTestHelper.signUp;
import static com.example.ticket_booking.support.JsonText.raw;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
import java.util.function.Consumer;
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
 * TASK-019 시나리오 SC-02~SC-09 — 등록 API 의 posterUrl·zones.
 *
 * <p>클래스 레벨 {@code @Transactional} 을 두지 않는다 — 거부 경로의 "행이 생기지 않는다" 와 구역 행 수는 리포지토리 count 로 새로
 * 읽는다(CLAUDE.md 리뷰 지시). 표의 행마다 테이블을 비우고 {@link SoftAssertions} 로 독립 단언한다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-019.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class PerformanceZoneRegistrationApiTest {

  private static final Instant T = Instant.parse("2026-11-11T08:00:00Z");
  // TASK-023: 등록 응답이 주최자 응답이라 published·editable·visibilityChangeable 이 더해진다
  private static final Set<String> PERFORMANCE_KEYS =
      Set.of(
          "published",
          "editable",
          "visibilityChangeable",
          "id",
          "title",
          "venue",
          "startAt",
          "openAt",
          "closeAt",
          "totalSeats",
          "availableSeats",
          "status");
  private static final Set<String> DEFAULT_SECTION_KEYS =
      Set.of("grade", "price", "seatCount", "area", "stageOrder");

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

  // ---------- 규약 ----------

  private static Map<String, Object> section(String grade, String row, int seatsPerRow, int price) {
    Map<String, Object> section = new LinkedHashMap<>();
    section.put("grade", grade);
    section.put("price", price);
    section.put("rowStart", row);
    section.put("rowEnd", row);
    section.put("seatsPerRow", seatsPerRow);
    return section;
  }

  private static List<Object> s0() {
    return List.of(
        section("2층", "C", 10, 77000),
        section("F1", "A", 10, 150000),
        section("1층", "B", 10, 99000));
  }

  private static List<Object> s1() {
    return List.of(
        section("2층", "C", 10, 77000),
        section("F2", "B", 10, 120000),
        section("F1", "A", 10, 150000),
        section("1층", "D", 10, 99000));
  }

  /** 규약 R0 — 키 순서를 지키는 가변 맵. */
  private static Map<String, Object> r0() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("title", "루미너스 콘서트");
    body.put("venue", "KSPO DOME");
    body.put("startAt", T.plus(Duration.ofDays(30)).toString());
    body.put("openAt", T.plus(Duration.ofDays(7)).toString());
    body.put("closeAt", T.plus(Duration.ofDays(29)).toString());
    body.put("sections", s0());
    return body;
  }

  private static Map<String, Object> zone(String area, String note, List<?> grades) {
    Map<String, Object> zone = new LinkedHashMap<>();
    zone.put("area", area);
    if (note != null) {
      zone.put("note", note);
    }
    zone.put("grades", grades);
    return zone;
  }

  private static Map<String, Object> with(
      Map<String, Object> body, Consumer<Map<String, Object>> edit) {
    edit.accept(body);
    return body;
  }

  // ---------- 호출 ----------

  private MvcResult register(Map<String, Object> body) throws Exception {
    return mockMvc
        .perform(
            post("/api/performances")
                .header("Authorization", "Bearer " + organizerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonText.of(body)))
        .andReturn();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> json(MvcResult result) throws Exception {
    return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> detail(Object id) throws Exception {
    // TASK-023: 등록한 공연은 비공개라 관객 단건 조회는 404 다 — 주최자 단건 조회로 읽는다
    MvcResult result =
        mockMvc
            .perform(
                get("/api/organizer/performances/" + id)
                    .header("Authorization", "Bearer " + organizerToken))
            .andReturn();
    return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> sections(Map<String, Object> detail) {
    Object sections = detail.get("sections");
    return sections == null ? List.of() : (List<Map<String, Object>>) sections;
  }

  private static List<String> column(List<Map<String, Object>> rows, String key) {
    List<String> values = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      values.add(String.valueOf(row.get(key)));
    }
    return values;
  }

  private static Map<String, Object> sectionOf(List<Map<String, Object>> rows, String grade) {
    return rows.stream().filter(row -> grade.equals(row.get("grade"))).findFirst().orElse(Map.of());
  }

  private void assertRejected(
      SoftAssertions softly, String label, MvcResult result, String expectedCode) throws Exception {
    softly.assertThat(result.getResponse().getStatus()).as("%s 상태", label).isEqualTo(400);
    String body = result.getResponse().getContentAsString();
    softly
        .assertThat(body.isEmpty() ? null : json(result).get("code"))
        .as("%s code", label)
        .isEqualTo(expectedCode);
    softly.assertThat(performanceRepository.count()).as("%s 공연 행", label).isZero();
    softly.assertThat(seatRepository.count()).as("%s 좌석 행", label).isZero();
    softly.assertThat(zoneRepository.count()).as("%s 구역 행", label).isZero();
  }

  // ---------- SC-02 ----------

  /**
   * SC-02 (happy)
   *
   * <p>Given 공연·좌석·구역 행이 없다 When 표의 본문으로 등록한다 Then 201, 응답 키는 기존 9개(posterUrl 없음), 상세 posterUrl 없음,
   * sections 는 2층(1)·F1(2)·1층(3) 이고 area = grade, 키는 grade·price·seatCount·area·stageOrder, 구역 행 3개
   */
  @Test
  void test_sc02_포스터_구역_없이_등록하면_등록_순서대로_기본_구역이_매겨진다() throws Exception {
    Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
    rows.put("키 없음", r0());
    rows.put(
        "null",
        with(
            r0(),
            body -> {
              body.put("posterUrl", null);
              body.put("zones", null);
            }));
    rows.put("빈 배열", with(r0(), body -> body.put("zones", List.of())));

    SoftAssertions softly = new SoftAssertions();
    for (Map.Entry<String, Map<String, Object>> row : rows.entrySet()) {
      clearTables();
      String label = row.getKey();

      // When
      MvcResult result = register(row.getValue());

      // Then
      softly.assertThat(result.getResponse().getStatus()).as("%s 상태", label).isEqualTo(201);
      if (result.getResponse().getStatus() != 201) {
        continue;
      }
      Map<String, Object> created = json(result);
      softly.assertThat(created.keySet()).as("%s 응답 키", label).isEqualTo(PERFORMANCE_KEYS);
      Map<String, Object> detail = detail(created.get("id"));
      softly.assertThat(detail).as("%s 상세 posterUrl", label).doesNotContainKey("posterUrl");
      List<Map<String, Object>> sections = sections(detail);
      softly
          .assertThat(column(sections, "grade"))
          .as("%s 순서", label)
          .containsExactly("2층", "F1", "1층");
      softly
          .assertThat(column(sections, "stageOrder"))
          .as("%s stageOrder", label)
          .containsExactly("1", "2", "3");
      softly
          .assertThat(column(sections, "area"))
          .as("%s area", label)
          .containsExactly("2층", "F1", "1층");
      for (Map<String, Object> section : sections) {
        softly
            .assertThat(section.keySet())
            .as("%s section 키", label)
            .isEqualTo(DEFAULT_SECTION_KEYS);
      }
      softly.assertThat(zoneRepository.count()).as("%s 구역 행", label).isEqualTo(3);
    }
    softly.assertAll();
  }

  // ---------- SC-03 ----------

  /**
   * SC-03 (happy)
   *
   * <p>Given 행이 없다 When S1 + posterUrl + zones[플로어(F1,F2), 2층 지정석(2층)] 로 등록한다 Then 201, 응답
   * posterUrl, 상세 sections 는 F1·F2·2층·1층 순서·값, 없음 행은 areaNote 키 없음, 상세 posterUrl
   */
  @Test
  void test_sc03_구역을_주면_펼친_순서로_정렬되고_빠진_등급은_뒤에_붙는다() throws Exception {
    // Given
    String poster = "https://cdn.example.com/luminous.jpg";
    String note = "무대 앞 바닥, F1이 가장 앞";
    Map<String, Object> body =
        with(
            r0(),
            b -> {
              b.put("sections", s1());
              b.put("posterUrl", poster);
              b.put(
                  "zones",
                  List.of(
                      zone("플로어", note, List.of("F1", "F2")), zone("2층 지정석", null, List.of("2층"))));
            });

    // When
    MvcResult result = register(body);

    // Then
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(result.getResponse().getStatus()).as("상태").isEqualTo(201);
    if (result.getResponse().getStatus() == 201) {
      Map<String, Object> created = json(result);
      softly.assertThat(created.get("posterUrl")).as("응답 posterUrl").isEqualTo(poster);
      Map<String, Object> detail = detail(created.get("id"));
      List<Map<String, Object>> sections = sections(detail);
      softly
          .assertThat(column(sections, "grade"))
          .as("grade")
          .containsExactly("F1", "F2", "2층", "1층");
      softly
          .assertThat(column(sections, "price"))
          .as("price")
          .containsExactly("150000", "120000", "77000", "99000");
      softly
          .assertThat(column(sections, "seatCount"))
          .as("seatCount")
          .containsExactly("10", "10", "10", "10");
      softly
          .assertThat(column(sections, "area"))
          .as("area")
          .containsExactly("플로어", "플로어", "2층 지정석", "1층");
      softly
          .assertThat(column(sections, "stageOrder"))
          .as("stageOrder")
          .containsExactly("1", "2", "3", "4");
      softly
          .assertThat(sectionOf(sections, "F1").get("areaNote"))
          .as("F1 areaNote")
          .isEqualTo(note);
      softly
          .assertThat(sectionOf(sections, "F2").get("areaNote"))
          .as("F2 areaNote")
          .isEqualTo(note);
      softly.assertThat(sectionOf(sections, "2층")).as("2층 areaNote").doesNotContainKey("areaNote");
      softly.assertThat(sectionOf(sections, "1층")).as("1층 areaNote").doesNotContainKey("areaNote");
      softly.assertThat(detail.get("posterUrl")).as("상세 posterUrl").isEqualTo(poster);
    }
    softly.assertAll();
  }

  // ---------- SC-04 ----------

  /**
   * SC-04 (boundary)
   *
   * <p>Given 행이 없다 When sections R(A,80000)·VIP(B,120000)·R(C,70000) 로 등록한다 Then 201, 상세 sections 는
   * R 80000(1)·R 70000(1)·VIP 120000(2), area = grade, 구역 행 2개
   */
  @Test
  void test_sc04_같은_등급이_두_구역_행이면_구역은_하나이고_그_안에서는_가격순이다() throws Exception {
    // Given
    Map<String, Object> body =
        with(
            r0(),
            b ->
                b.put(
                    "sections",
                    List.of(
                        section("R", "A", 10, 80000),
                        section("VIP", "B", 10, 120000),
                        section("R", "C", 10, 70000))));

    // When
    MvcResult result = register(body);

    // Then
    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(result.getResponse().getStatus()).as("상태").isEqualTo(201);
    if (result.getResponse().getStatus() == 201) {
      List<Map<String, Object>> sections = sections(detail(json(result).get("id")));
      softly.assertThat(column(sections, "grade")).as("grade").containsExactly("R", "R", "VIP");
      softly
          .assertThat(column(sections, "price"))
          .as("price")
          .containsExactly("80000", "70000", "120000");
      softly
          .assertThat(column(sections, "stageOrder"))
          .as("stageOrder")
          .containsExactly("1", "1", "2");
      softly.assertThat(column(sections, "area")).as("area").containsExactly("R", "R", "VIP");
      softly.assertThat(zoneRepository.count()).as("구역 행").isEqualTo(2);
    }
    softly.assertAll();
  }

  // ---------- SC-05 ----------

  /**
   * SC-05 (error)
   *
   * <p>Given 행이 없다 When R0 + 표의 posterUrl 로 등록한다 Then 400 INVALID_POSTER_URL, 공연·좌석·구역 0행
   */
  @Test
  void test_sc05_포스터_주소가_https_절대_URL_이_아니면_INVALID_POSTER_URL() throws Exception {
    String prefix = "https://cdn.example.com/";
    Map<String, String> rows = new LinkedHashMap<>();
    rows.put("a http", "http://cdn.example.com/a.jpg");
    rows.put("b 상대 경로", "/posters/a.jpg");
    rows.put("c 스킴 없음", "cdn.example.com/a.jpg");
    rows.put("d 501자", prefix + "a".repeat(477));
    rows.put("e 빈 값", "");
    rows.put("f 공백만", "   ");
    rows.put("g 호스트 없음", "https:///a.jpg");
    rows.put("h ftp", "ftp://cdn.example.com/a.jpg");
    rows.put("i javascript", "javascript:alert(1)");
    rows.put("j 앞 공백", " https://cdn.example.com/a.jpg");
    rows.put("k 파싱 불가 문자", "https://cdn.example.com/a b.jpg");
    rows.put("l NUL", "https://cdn.example.com/a\u0000.jpg");
    rows.put("m 서로게이트", "https://cdn.example.com/a\uD800.jpg");
    rows.put("n 이모지 501 코드 유닛", prefix + "🎤".repeat(238) + "a");
    rows.put("o 호스트 퍼센트 인코딩", "https://ex%41mple.com/a.jpg");
    rows.put("p 빈 userinfo", "https://@cdn.example.com/a.jpg");
    rows.put("q 빈 포트", "https://cdn.example.com:/a.jpg");
    rows.put("r 호스트 없이 포트만", "https://:8443/a.jpg");
    rows.put("s 숫자가 아닌 포트", "https://cdn.example.com:abc/a.jpg");
    rows.put("t 닫히지 않은 IPv6", "https://[::1/a.jpg");
    rows.put("u 호스트에 콜론 여럿", "https://a:1:2/a.jpg");
    rows.put("v 콜론으로 시작하는 호스트와 포트 둘", "https://:80:90/a.jpg");
    rows.put("w 아랍-인도 숫자 포트", "https://cdn.example.com:\u0668\u0660/a.jpg");
    rows.put("x 전각 숫자 포트", "https://cdn.example.com:\uFF18\uFF10/a.jpg");
    rows.put("y userinfo 안의 @", "https://a@b@cdn.example.com/a.jpg");

    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(rows.get("d 501자")).as("d 길이").hasSize(501);
    softly.assertThat(rows.get("n 이모지 501 코드 유닛")).as("n 길이").hasSize(501);
    for (Map.Entry<String, String> row : rows.entrySet()) {
      clearTables();
      // When
      MvcResult result = register(with(r0(), b -> b.put("posterUrl", row.getValue())));
      // Then
      assertRejected(softly, row.getKey(), result, "INVALID_POSTER_URL");
    }
    softly.assertAll();
  }

  // ---------- SC-06 ----------

  /**
   * SC-06 (boundary)
   *
   * <p>Given 행이 없다 When R0 + 표의 필드(구역은 F1 한 묶음)로 등록한다 Then 201, 상세 posterUrl·F1 의 area·areaNote 가
   * 보낸 문자열과 같다
   */
  @Test
  void test_sc06_상한_안의_값과_표기_변형은_받은_그대로_저장된다() throws Exception {
    record Row(String label, String posterUrl, String area, String note) {}
    String prefix = "https://cdn.example.com/";
    List<Row> rows =
        List.of(
            new Row("a 500자", prefix + "a".repeat(476), null, null),
            new Row("b 대문자 스킴", "HTTPS://CDN.EXAMPLE.COM/A.JPG", null, null),
            new Row("c 비 ASCII 경로", "https://cdn.example.com/포스터.jpg", null, null),
            new Row("d area 30자", null, "가".repeat(30), null),
            new Row("e note 60자", null, null, "나".repeat(60)),
            new Row("f area 앞뒤 공백", null, " 플로어 ", null),
            new Row("g 이모지", null, "플로어 🎤", "무대 🎤 앞"),
            new Row("h note 앞뒤 공백", null, null, " 무대 앞 "),
            new Row("i area 이모지 30 코드 유닛", null, "🎤".repeat(15), null),
            new Row("j 호스트 밑줄", "https://cdn_1.example.com/a.jpg", null, null),
            new Row("k 비 ASCII 호스트", "https://포스터.example.com/a.jpg", null, null),
            new Row("l IPv6 + 포트", "https://[::1]:8443/a.jpg", null, null),
            new Row("m userinfo + 포트", "https://user@cdn.example.com:8443/a.jpg", null, null),
            new Row("n 공백 글자가 섞인 area·note", null, "\u00A0플로어", "무대\u3000앞"));

    SoftAssertions softly = new SoftAssertions();
    softly.assertThat(rows.get(0).posterUrl()).as("a 길이").hasSize(500);
    softly.assertThat(rows.get(8).area()).as("i 길이").hasSize(30);
    for (Row row : rows) {
      clearTables();
      String area = row.area() == null ? "플로어" : row.area();
      Map<String, Object> body =
          with(
              r0(),
              b -> {
                if (row.posterUrl() != null) {
                  b.put("posterUrl", row.posterUrl());
                }
                b.put("zones", List.of(zone(area, row.note(), List.of("F1"))));
              });

      // When
      MvcResult result = register(body);

      // Then
      softly.assertThat(result.getResponse().getStatus()).as("%s 상태", row.label()).isEqualTo(201);
      if (result.getResponse().getStatus() != 201) {
        continue;
      }
      Map<String, Object> detail = detail(json(result).get("id"));
      Map<String, Object> f1 = sectionOf(sections(detail), "F1");
      softly
          .assertThat(detail.get("posterUrl"))
          .as("%s posterUrl", row.label())
          .isEqualTo(row.posterUrl());
      softly.assertThat(f1.get("area")).as("%s area", row.label()).isEqualTo(area);
      if (row.note() == null) {
        softly.assertThat(f1).as("%s areaNote 키", row.label()).doesNotContainKey("areaNote");
      } else {
        softly.assertThat(f1.get("areaNote")).as("%s areaNote", row.label()).isEqualTo(row.note());
      }
    }
    softly.assertAll();
  }

  // ---------- SC-07 ----------

  /**
   * SC-07 (error)
   *
   * <p>Given 행이 없다 When R0 + 표의 필드(다른 JSON 타입)로 등록한다 Then 400 INVALID_REQUEST, 공연·좌석·구역 0행
   */
  @Test
  void test_sc07_포스터_구역_필드의_JSON_타입이_다르면_INVALID_REQUEST() throws Exception {
    Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
    rows.put("a posterUrl 숫자", with(r0(), b -> b.put("posterUrl", raw("123"))));
    rows.put("b posterUrl 불리언", with(r0(), b -> b.put("posterUrl", raw("true"))));
    rows.put("c posterUrl 객체", with(r0(), b -> b.put("posterUrl", raw("{}"))));
    rows.put(
        "d posterUrl 배열",
        with(r0(), b -> b.put("posterUrl", List.of("https://cdn.example.com/a.jpg"))));
    rows.put("e zones 문자열", with(r0(), b -> b.put("zones", "플로어")));
    rows.put("f zones 객체", with(r0(), b -> b.put("zones", raw("{}"))));
    rows.put("g zones 숫자", with(r0(), b -> b.put("zones", raw("1"))));
    rows.put(
        "h area 숫자",
        with(
            r0(), b -> b.put("zones", List.of(Map.of("area", raw("1"), "grades", List.of("F1"))))));
    rows.put(
        "i note 불리언",
        with(
            r0(),
            b ->
                b.put(
                    "zones",
                    List.of(
                        new LinkedHashMap<>(
                            Map.of(
                                "area", "플로어", "note", raw("true"), "grades", List.of("F1")))))));
    rows.put(
        "j grades 문자열",
        with(r0(), b -> b.put("zones", List.of(Map.of("area", "플로어", "grades", "F1")))));
    rows.put(
        "k grades 숫자 원소",
        with(
            r0(),
            b -> {
              b.put(
                  "sections",
                  List.of(
                      section("2층", "C", 10, 77000),
                      section("1", "A", 10, 150000),
                      section("1층", "B", 10, 99000)));
              b.put("zones", List.of(Map.of("area", "플로어", "grades", List.of(raw("1")))));
            }));
    rows.put("l zones 문자열 원소", with(r0(), b -> b.put("zones", List.of("플로어"))));

    SoftAssertions softly = new SoftAssertions();
    for (Map.Entry<String, Map<String, Object>> row : rows.entrySet()) {
      clearTables();
      // When
      MvcResult result = register(row.getValue());
      // Then
      assertRejected(softly, row.getKey(), result, "INVALID_REQUEST");
    }
    softly.assertAll();
  }

  // ---------- SC-08 ----------

  /**
   * SC-08 (error)
   *
   * <p>Given 행이 없다 When R0 + 표의 zones({area 플로어, grades [F1]} 에서 표의 칸만 바꾼 것)로 등록한다 Then 400
   * INVALID_REQUEST, 공연·좌석·구역 0행
   */
  @Test
  void test_sc08_구역_원소의_형태가_틀리면_INVALID_REQUEST() throws Exception {
    Map<String, List<Object>> rows = new LinkedHashMap<>();
    java.util.ArrayList<Object> nullElement = new java.util.ArrayList<>();
    nullElement.add(null);
    rows.put("a null 원소", nullElement);
    rows.put("b area 누락", List.of(zoneWithout("area")));
    rows.put("c area null", List.of(zoneWith("area", null)));
    rows.put("d area 빈 값", List.of(zoneWith("area", "")));
    rows.put("e area 공백만", List.of(zoneWith("area", "   ")));
    rows.put("e2 area 전각 공백만", List.of(zoneWith("area", "\u3000")));
    rows.put("e3 area NBSP 만", List.of(zoneWith("area", "\u00A0\u00A0")));
    rows.put("e4 area 제로폭 공백만", List.of(zoneWith("area", "\u200B")));
    rows.put("f area 31자", List.of(zoneWith("area", "가".repeat(31))));
    rows.put("f2 area 이모지 31 코드 유닛", List.of(zoneWith("area", "🎤".repeat(15) + "가")));
    rows.put("g area NUL", List.of(zoneWith("area", "플로\u0000어")));
    rows.put("h area 서로게이트", List.of(zoneWith("area", "플로\uD800")));
    rows.put("i note 빈 값", List.of(zoneWith("note", "")));
    rows.put("j note 공백만", List.of(zoneWith("note", "   ")));
    rows.put("j2 note 전각 공백만", List.of(zoneWith("note", "\u3000")));
    rows.put("j3 note EM SPACE 만", List.of(zoneWith("note", "\u2003")));
    rows.put("j4 note 줄 구분자만", List.of(zoneWith("note", "\u2028")));
    rows.put("j5 note NBSP 만", List.of(zoneWith("note", "\u00A0")));
    rows.put("j6 note 제로폭 공백과 공백", List.of(zoneWith("note", "\u200B ")));
    rows.put("k note 61자", List.of(zoneWith("note", "나".repeat(61))));
    rows.put("k2 note 이모지 61 코드 유닛", List.of(zoneWith("note", "🎤".repeat(30) + "나")));
    rows.put("l note NUL", List.of(zoneWith("note", "무대\u0000")));
    rows.put("m note 서로게이트", List.of(zoneWith("note", "무대\uDC00")));
    rows.put("n grades 누락", List.of(zoneWithout("grades")));
    rows.put("o grades null", List.of(zoneWith("grades", null)));
    rows.put("p grades 빈 배열", List.of(zoneWith("grades", List.of())));
    java.util.ArrayList<Object> nullGrade = new java.util.ArrayList<>();
    nullGrade.add(null);
    rows.put("q grades null 원소", List.of(zoneWith("grades", nullGrade)));
    rows.put("r grades 원소 공백만", List.of(zoneWith("grades", List.of("  "))));

    SoftAssertions softly = new SoftAssertions();
    softly.assertThat("🎤".repeat(15) + "가").as("f2 길이").hasSize(31);
    softly.assertThat("🎤".repeat(30) + "나").as("k2 길이").hasSize(61);
    for (Map.Entry<String, List<Object>> row : rows.entrySet()) {
      clearTables();
      // When
      MvcResult result = register(with(r0(), b -> b.put("zones", row.getValue())));
      // Then
      assertRejected(softly, row.getKey(), result, "INVALID_REQUEST");
    }
    softly.assertAll();
  }

  private static Map<String, Object> baseZone() {
    Map<String, Object> zone = new LinkedHashMap<>();
    zone.put("area", "플로어");
    zone.put("grades", List.of("F1"));
    return zone;
  }

  private static Map<String, Object> zoneWith(String key, Object value) {
    Map<String, Object> zone = baseZone();
    zone.put(key, value);
    return zone;
  }

  private static Map<String, Object> zoneWithout(String key) {
    Map<String, Object> zone = baseZone();
    zone.remove(key);
    return zone;
  }

  // ---------- SC-09 ----------

  /**
   * SC-09 (error)
   *
   * <p>Given 행이 없다 When R0(S0) + 표의 zones(묶음 area 는 플로어·A·B·C 순)로 등록한다 Then 400, code 는 표의 값,
   * 공연·좌석·구역 0행
   */
  @Test
  void test_sc09_구역의_등급이_sections_에_없거나_겹치면_등급_오류() throws Exception {
    record Row(
        String label,
        List<List<String>> grades,
        Consumer<Map<String, Object>> extra,
        String code) {}
    Consumer<Map<String, Object>> none = b -> {};
    List<Row> rows =
        List.of(
            new Row("a 없는 등급", List.of(List.of("VIP")), none, "ZONE_GRADE_UNKNOWN"),
            new Row("b 대소문자", List.of(List.of("f1")), none, "ZONE_GRADE_UNKNOWN"),
            new Row("c 끝 공백", List.of(List.of("F1 ")), none, "ZONE_GRADE_UNKNOWN"),
            new Row("d NUL", List.of(List.of("F1\u0000")), none, "ZONE_GRADE_UNKNOWN"),
            new Row("e 서로게이트", List.of(List.of("F1\uD800")), none, "ZONE_GRADE_UNKNOWN"),
            new Row("f 묶음 안 중복", List.of(List.of("F1", "F1")), none, "ZONE_GRADE_DUPLICATED"),
            new Row(
                "g 묶음 사이 중복", List.of(List.of("F1"), List.of("F1")), none, "ZONE_GRADE_DUPLICATED"),
            new Row(
                "h 등급 수 + 1",
                List.of(List.of("F1", "2층", "1층", "F1")),
                none,
                "ZONE_GRADE_DUPLICATED"),
            new Row(
                "i 묶음 수 = 등급 수 + 1",
                List.of(List.of("F1"), List.of("2층"), List.of("1층"), List.of("F1")),
                none,
                "ZONE_GRADE_DUPLICATED"),
            new Row(
                "j 미지 등급이 먼저",
                List.of(List.of("F1", "VIP"), List.of("F1")),
                none,
                "ZONE_GRADE_UNKNOWN"),
            new Row(
                "k 중복이 먼저",
                List.of(List.of("F1", "F1"), List.of("VIP")),
                none,
                "ZONE_GRADE_DUPLICATED"),
            new Row(
                "l 포스터 검증이 먼저",
                List.of(List.of("VIP")),
                b -> b.put("posterUrl", "http://a.jpg"),
                "INVALID_POSTER_URL"),
            new Row(
                "m 기존 검증이 먼저",
                List.of(List.of("VIP")),
                b -> b.put("closeAt", T.plus(Duration.ofDays(31)).toString()),
                "INVALID_TIME_ORDER"));
    String[] areas = {"플로어", "A", "B", "C"};

    SoftAssertions softly = new SoftAssertions();
    for (Row row : rows) {
      clearTables();
      List<Object> zones = new ArrayList<>();
      for (int i = 0; i < row.grades().size(); i++) {
        zones.add(zone(areas[i], null, row.grades().get(i)));
      }
      Map<String, Object> body =
          with(
              r0(),
              b -> {
                b.put("zones", zones);
                row.extra().accept(b);
              });
      // When
      MvcResult result = register(body);
      // Then
      assertRejected(softly, row.label(), result, row.code());
    }
    softly.assertAll();
  }
}
