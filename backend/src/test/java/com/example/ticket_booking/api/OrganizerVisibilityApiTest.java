package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.accessTokenOf;
import static com.example.ticket_booking.support.AuthTestHelper.login;
import static com.example.ticket_booking.support.AuthTestHelper.signUp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.repository.PerformanceZoneRepository;
import com.example.ticket_booking.repository.SeatRepository;
import com.example.ticket_booking.repository.SocialAccountRepository;
import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.JsonText;
import com.example.ticket_booking.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * TASK-023 시나리오 SC-02~SC-19 — 공연 공개 여부(published)와 주최자 조회·공개 전환. SC-01(V8 마이그레이션)은
 * PerformancePublishedMigrationTest 에 둔다.
 *
 * <p>클래스 레벨 {@code @Transactional} 을 두지 않는다 — "DB 가 바뀌지 않는다" 와 "DB 에 반영된다" 는 dirty checking 으로 커밋이
 * 미뤄지는 UPDATE 경로라, 리포지토리 findById·count 로 새로 읽어 확인한다(CLAUDE.md 리뷰 지시). 시각은 MutableClock 으로
 * 고정한다(ADR-0005). 주최자·관객 토큰은 실제 가입·로그인으로 얻는다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-023.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class OrganizerVisibilityApiTest {

  private static final Instant T = Instant.parse("2026-11-13T09:00:00Z");
  private static final String PASSWORD = "password123";
  private static final String TITLE = "소리풍경 밴드 나이트";
  private static final String ENCORE = "소리풍경 밴드 나이트 앙코르";
  private static final String NOT_FOUND = "PERFORMANCE_NOT_FOUND";
  private static final Set<String> ORGANIZER_KEYS =
      Set.of(
          "id",
          "title",
          "venue",
          "startAt",
          "openAt",
          "closeAt",
          "totalSeats",
          "availableSeats",
          "status",
          "published",
          "editable",
          "visibilityChangeable");
  private static final Set<String> AUDIENCE_KEYS =
      Set.of(
          "id",
          "title",
          "venue",
          "startAt",
          "openAt",
          "closeAt",
          "totalSeats",
          "availableSeats",
          "status");

  @Autowired private MockMvc mockMvc;
  @Autowired private PerformanceRepository performanceRepository;
  @Autowired private SeatRepository seatRepository;
  @Autowired private PerformanceZoneRepository zoneRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private SocialAccountRepository socialAccountRepository;
  @Autowired private MutableClock clock;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  private String o1;
  private String o2;
  private String a1;
  private Long o1Id;
  private Long o2Id;

  @BeforeEach
  void setUp() throws Exception {
    clock.setInstant(T);
    clearTables();
    o1 = tokenOf("o1@example.com", "ORGANIZER");
    o2 = tokenOf("o2@example.com", "ORGANIZER");
    a1 = tokenOf("a1@example.com", "AUDIENCE");
    o1Id = userRepository.findByEmail("o1@example.com").orElseThrow().getId();
    o2Id = userRepository.findByEmail("o2@example.com").orElseThrow().getId();
  }

  @AfterEach
  void tearDown() {
    clearTables();
  }

  private void clearTables() {
    zoneRepository.deleteAll();
    seatRepository.deleteAll();
    performanceRepository.deleteAll();
    socialAccountRepository.deleteAll();
    userRepository.deleteAll();
  }

  // ---------- 규약·픽스처 ----------

  private String tokenOf(String email, String role) throws Exception {
    signUp(mockMvc, email, PASSWORD, "한도윤", role);
    return accessTokenOf(login(mockMvc, email, PASSWORD));
  }

  private static Instant d(long days) {
    return T.plus(Duration.ofDays(days));
  }

  /** 기본값: 좌석 100/100, startAt T+30d, closeAt T+29d. 소유자·공개·취소·openAt 만 바꾼다. */
  private Performance saved(Long owner, boolean published, boolean cancelled, Instant openAt) {
    return saved(owner, published, cancelled, openAt, d(29), d(30), 100);
  }

  private Performance saved(
      Long owner,
      boolean published,
      boolean cancelled,
      Instant openAt,
      Instant closeAt,
      Instant startAt,
      int available) {
    return performanceRepository.save(
        new Performance(
            owner, TITLE, "롤링홀", startAt, openAt, closeAt, 100, available, cancelled, null,
            published));
  }

  private Performance savedDefault(Long owner, boolean published) {
    return saved(owner, published, false, d(7));
  }

  /** 소유자 NULL 인 기존 공연(기존 생성자 — 공개). */
  private Performance savedWithoutOwner() {
    return performanceRepository.save(
        new Performance(TITLE, "롤링홀", d(30), d(7), d(29), 100, 100, false));
  }

  private static Map<String, Object> section(
      String grade, int price, String rowStart, String rowEnd, int seatsPerRow) {
    Map<String, Object> section = new LinkedHashMap<>();
    section.put("grade", grade);
    section.put("price", price);
    section.put("rowStart", rowStart);
    section.put("rowEnd", rowEnd);
    section.put("seatsPerRow", seatsPerRow);
    return section;
  }

  /** 규약 R0 — R 등급 A~B열 열당 10석 55000. */
  private static Map<String, Object> r0() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("title", TITLE);
    body.put("venue", "롤링홀");
    body.put("startAt", d(30).toString());
    body.put("openAt", d(7).toString());
    body.put("closeAt", d(29).toString());
    body.put("sections", List.of(section("R", 55000, "A", "B", 10)));
    return body;
  }

  /** 규약 U0 — 공연의 시각은 그대로 두고 title 만 바꾼 수정 본문. */
  private static Map<String, Object> u0(Performance performance) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("title", ENCORE);
    body.put("venue", "롤링홀");
    body.put("startAt", performance.getStartAt().toString());
    body.put("openAt", performance.getOpenAt().toString());
    body.put("closeAt", performance.getCloseAt().toString());
    return body;
  }

  private Long registeredBy(String token) throws Exception {
    MvcResult result = register(token, r0());
    assertThat(status(result)).as("픽스처 등록").isEqualTo(201);
    return ((Number) json(result).get("id")).longValue();
  }

  // ---------- 호출 ----------

  private static String bearer(String token) {
    return token == null ? null : "Bearer " + token;
  }

  private MvcResult send(MockHttpServletRequestBuilder builder, String token) throws Exception {
    if (token != null) {
      builder = builder.header("Authorization", bearer(token));
    }
    return mockMvc.perform(builder).andReturn();
  }

  private MvcResult getAs(String token, String path) throws Exception {
    return send(get(path), token);
  }

  private MvcResult postAs(String token, String path) throws Exception {
    return send(post(path), token);
  }

  private MvcResult register(String token, Map<String, Object> body) throws Exception {
    return send(
        post("/api/performances")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonText.of(body)),
        token);
  }

  private MvcResult update(String token, Object id, Map<String, Object> body) throws Exception {
    return send(
        put("/api/performances/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonText.of(body)),
        token);
  }

  private MvcResult publish(String token, Object id) throws Exception {
    return postAs(token, "/api/performances/" + id + "/publish");
  }

  private MvcResult unpublish(String token, Object id) throws Exception {
    return postAs(token, "/api/performances/" + id + "/unpublish");
  }

  private MvcResult organizerGet(String token, Object id) throws Exception {
    return getAs(token, "/api/organizer/performances/" + id);
  }

  private MvcResult organizerList(String token, String query) throws Exception {
    return getAs(token, "/api/organizer/performances" + query);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> json(MvcResult result) throws Exception {
    String body = result.getResponse().getContentAsString();
    return body.isEmpty() ? Map.of() : objectMapper.readValue(body, Map.class);
  }

  private String code(MvcResult result) throws Exception {
    Object code = json(result).get("code");
    return code == null ? null : String.valueOf(code);
  }

  /** 응답에 키가 없으면(엔드포인트가 아직 없을 때) NPE 대신 -1 을 돌려준다 — 실패가 단언으로 드러나게 한다. */
  private static long num(Map<String, Object> body, String key) {
    Object value = body.get(key);
    return value == null ? -1L : ((Number) value).longValue();
  }

  private static int status(MvcResult result) {
    return result.getResponse().getStatus();
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> rows(MvcResult result, String key) throws Exception {
    Object rows = json(result).get(key);
    return rows == null ? List.of() : (List<Map<String, Object>>) rows;
  }

  private static List<Long> idsOf(List<Map<String, Object>> rows) {
    List<Long> ids = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      ids.add(((Number) row.get("id")).longValue());
    }
    return ids;
  }

  private List<Long> organizerIds(String token, String query) throws Exception {
    return idsOf(rows(organizerList(token, query), "items"));
  }

  private List<Long> audienceIds(String query) throws Exception {
    return idsOf(rows(getAs(null, "/api/performances" + query), "content"));
  }

  private Performance reload(Long id) {
    return performanceRepository.findById(id).orElseThrow();
  }

  // ---------- SC-02 ----------

  /**
   * SC-02 (happy)
   *
   * <p>Given 주최자 O1 이 로그인해 있다 When O1 이 POST /api/performances 로 R0 을 등록한다 Then 201, 응답은 주최자 응답
   * 키·published false·editable true·visibilityChangeable true·status UPCOMING, ownerId 키 없음, 저장된 행의
   * published 는 false
   */
  @Test
  void test_sc02_등록한_공연은_비공개로_저장되고_응답이_그_상태를_알려_준다() throws Exception {
    // When
    MvcResult result = register(o1, r0());

    // Then 201 이다
    assertThat(status(result)).isEqualTo(201);
    Map<String, Object> body = json(result);
    // And 응답 본문은 주최자 응답 키를 가지고 published false, editable true, visibilityChangeable true
    assertThat(body.keySet()).containsAll(ORGANIZER_KEYS);
    assertThat(body.get("published")).isEqualTo(false);
    assertThat(body.get("editable")).isEqualTo(true);
    assertThat(body.get("visibilityChangeable")).isEqualTo(true);
    assertThat(body.get("status")).isEqualTo("UPCOMING");
    // And 응답에 ownerId 키가 없다
    assertThat(body).doesNotContainKey("ownerId");
    // And 저장된 행의 published 는 false 다(트랜잭션 밖에서 다시 읽는다)
    Long id = ((Number) body.get("id")).longValue();
    assertThat(reload(id).getPublished()).isFalse();
  }

  // ---------- SC-03 ----------

  /**
   * SC-03 (happy)
   *
   * <p>Given 비공개 D(openAt T+1d)·공개 V(T+2d)·공개 취소 X When 토큰 없이 목록·next-opening·단건 Then 목록에 V·X 만,
   * status 필터에도 D 없음, next-opening 은 V, D 단건 404, V 200, X 200·CANCELLED, V·X 를 지우면 next-opening
   * 204
   */
  @Test
  void test_sc03_비공개_공연은_관객_조회에_나오지_않고_공개된_공연은_나온다() throws Exception {
    // Given
    Performance dd = saved(o1Id, false, false, d(1));
    Performance v = saved(o1Id, true, false, d(2));
    Performance x = saved(o1Id, true, true, d(3));

    // When
    MvcResult list = getAs(null, "/api/performances");
    MvcResult upcoming = getAs(null, "/api/performances?status=UPCOMING");
    MvcResult cancelled = getAs(null, "/api/performances?status=CANCELLED");
    MvcResult next = getAs(null, "/api/performances/next-opening");
    MvcResult detailD = getAs(null, "/api/performances/" + dd.getId());
    MvcResult detailV = getAs(null, "/api/performances/" + v.getId());
    MvcResult detailX = getAs(null, "/api/performances/" + x.getId());

    // Then 목록은 V·X 를 포함하고 D 를 포함하지 않으며 totalElements 도 D 를 센 값이 아니다
    List<Long> ids = idsOf(rows(list, "content"));
    assertThat(ids).contains(v.getId(), x.getId()).doesNotContain(dd.getId());
    assertThat(((Number) json(list).get("totalElements")).longValue()).isEqualTo(2L);
    // And status 로 걸러도 D 는 나오지 않는다
    assertThat(idsOf(rows(upcoming, "content"))).doesNotContain(dd.getId());
    assertThat(idsOf(rows(cancelled, "content"))).doesNotContain(dd.getId());
    // And next-opening 은 V 를 돌려준다
    assertThat(status(next)).isEqualTo(200);
    assertThat(((Number) json(next).get("id")).longValue()).isEqualTo(v.getId());
    // And D 단건은 404 PERFORMANCE_NOT_FOUND 다
    assertThat(status(detailD)).isEqualTo(404);
    assertThat(code(detailD)).isEqualTo(NOT_FOUND);
    // And V 단건은 200, X 단건은 200 이며 status 는 CANCELLED 다
    assertThat(status(detailV)).isEqualTo(200);
    assertThat(status(detailX)).isEqualTo(200);
    assertThat(json(detailX).get("status")).isEqualTo("CANCELLED");
    // And V·X 를 지워 D 만 남기면 next-opening 은 204 다
    performanceRepository.deleteById(v.getId());
    performanceRepository.deleteById(x.getId());
    assertThat(status(getAs(null, "/api/performances/next-opening"))).isEqualTo(204);
  }

  // ---------- SC-04 ----------

  /**
   * SC-04 (happy)
   *
   * <p>Given O1 소유 P1·P2·P3(지난 공연)·P4, O2 소유 Q1, 소유자 NULL N1 When O1 이 주최자 목록을 부른다 Then 200, id 순서
   * P1·P4·P2·P3, Q1·N1 없음, totalElements 4·page 0·size 20, 항목은 주최자 응답 키·sections 없음, O2 는 Q1 만, 소유
   * 공연 없는 주최자는 빈 배열
   */
  @Test
  void test_sc04_주최자_목록은_요청자_소유_공연만_startAt_내림차순으로_돌려준다() throws Exception {
    // Given
    Performance p1 = saved(o1Id, false, false, d(7), d(29), d(30), 100);
    Performance p2 = saved(o1Id, true, false, d(7), d(9), d(10), 100);
    Performance p3 = saved(o1Id, false, false, d(-40), d(-25), d(-20), 100);
    Performance p4 = saved(o1Id, false, false, d(7), d(9), d(10), 100);
    Performance q1 = savedDefault(o2Id, false);
    Performance n1 = savedWithoutOwner();

    // When
    MvcResult result = organizerList(o1, "");

    // Then 200 이고 items 의 id 순서는 P1, P4, P2, P3 이다
    assertThat(status(result)).isEqualTo(200);
    List<Map<String, Object>> items = rows(result, "items");
    assertThat(idsOf(items)).containsExactly(p1.getId(), p4.getId(), p2.getId(), p3.getId());
    // And Q1·N1 은 없고 지난 공연 P3 은 있다
    assertThat(idsOf(items)).doesNotContain(q1.getId(), n1.getId()).contains(p3.getId());
    // And totalElements 는 4, page 는 0, size 는 20 이다
    Map<String, Object> body = json(result);
    assertThat(((Number) body.get("totalElements")).longValue()).isEqualTo(4L);
    assertThat(((Number) body.get("page")).intValue()).isEqualTo(0);
    assertThat(((Number) body.get("size")).intValue()).isEqualTo(20);
    // And 각 항목은 주최자 응답 키를 가지되 sections 키는 없다
    for (Map<String, Object> item : items) {
      assertThat(item.keySet()).containsAll(ORGANIZER_KEYS).doesNotContain("sections", "ownerId");
    }
    // And O2 가 같은 요청을 하면 Q1 만 나온다
    assertThat(organizerIds(o2, "")).containsExactly(q1.getId());
    // And 소유 공연이 하나도 없는 주최자는 items 가 빈 배열, totalElements 는 0 이다
    String o3 = tokenOf("o3@example.com", "ORGANIZER");
    MvcResult empty = organizerList(o3, "");
    assertThat(rows(empty, "items")).isEmpty();
    assertThat(((Number) json(empty).get("totalElements")).longValue()).isEqualTo(0L);
  }

  // ---------- SC-05 ----------

  /**
   * SC-05 (happy)
   *
   * <p>Given O1 소유 공연 a~i(공개·취소·시각·남은 좌석이 표와 같다) When filter 를 값마다 바꿔 부른다 Then id 집합이 표와 같고, now 를
   * T+1ms 로 올리면 i 가 CLOSED 로 옮겨 가며, totalElements 는 집합 크기와 같다, a·b 는 둘 다 UPCOMING 이고 published 만
   * 다르다
   */
  @Test
  void test_sc05_filter_는_값마다_정해진_조건으로_걸러지고_공개_여부와_무관하다() throws Exception {
    // Given
    Map<String, Long> id = new LinkedHashMap<>();
    id.put("a", saved(o1Id, false, false, d(7), d(29), d(30), 100).getId());
    id.put("b", saved(o1Id, true, false, d(7), d(29), d(30), 100).getId());
    id.put("c", saved(o1Id, false, false, d(-1), d(5), d(30), 10).getId());
    id.put("d", saved(o1Id, true, false, d(-1), d(5), d(30), 10).getId());
    id.put("e", saved(o1Id, true, false, d(-1), d(5), d(30), 0).getId());
    id.put("f", saved(o1Id, true, false, d(-10), d(-1), d(30), 10).getId());
    id.put("g", saved(o1Id, true, true, d(7), d(29), d(30), 100).getId());
    id.put("h", saved(o1Id, false, true, d(7), d(29), d(30), 100).getId());
    id.put("i", saved(o1Id, true, false, d(-3), T, d(30), 10).getId());
    Map<String, String> expected = new LinkedHashMap<>();
    expected.put("", "abcdefghi");
    expected.put("?filter=ALL", "abcdefghi");
    expected.put("?filter=DRAFT", "ac");
    expected.put("?filter=UPCOMING", "ab");
    expected.put("?filter=OPEN", "cdi");
    expected.put("?filter=SOLD_OUT", "e");
    expected.put("?filter=CLOSED", "f");
    expected.put("?filter=CANCELLED", "gh");
    SoftAssertions soft = new SoftAssertions();

    // When·Then filter 별 id 집합이 표와 같다
    for (Map.Entry<String, String> row : expected.entrySet()) {
      MvcResult result = organizerList(o1, row.getKey());
      Set<Long> want = new LinkedHashSet<>();
      for (char c : row.getValue().toCharArray()) {
        want.add(id.get(String.valueOf(c)));
      }
      soft.assertThat(status(result)).as("%s 상태", row.getKey()).isEqualTo(200);
      soft.assertThat(new LinkedHashSet<>(idsOf(rows(result, "items"))))
          .as("%s 집합", row.getKey())
          .isEqualTo(want);
      // And totalElements 는 집합 크기와 같다
      soft.assertThat(num(json(result), "totalElements"))
          .as("%s totalElements", row.getKey())
          .isEqualTo((long) want.size());
    }

    // And a 와 b 의 status 는 둘 다 UPCOMING 이고 published 만 다르다
    Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
    for (Map<String, Object> item : rows(organizerList(o1, ""), "items")) {
      byId.put(((Number) item.get("id")).longValue(), item);
    }
    soft.assertThat(byId.getOrDefault(id.get("a"), Map.of()).get("status")).isEqualTo("UPCOMING");
    soft.assertThat(byId.getOrDefault(id.get("b"), Map.of()).get("status")).isEqualTo("UPCOMING");
    soft.assertThat(byId.getOrDefault(id.get("a"), Map.of()).get("published")).isEqualTo(false);
    soft.assertThat(byId.getOrDefault(id.get("b"), Map.of()).get("published")).isEqualTo(true);

    // And now 를 T+1ms 로 올리면 i 가 CLOSED 에 나오고 OPEN 에는 없다
    clock.setInstant(T.plusMillis(1));
    soft.assertThat(organizerIds(o1, "?filter=CLOSED")).as("T+1ms CLOSED").contains(id.get("i"));
    soft.assertThat(organizerIds(o1, "?filter=OPEN")).as("T+1ms OPEN").doesNotContain(id.get("i"));
    soft.assertAll();
  }

  // ---------- SC-06 ----------

  /**
   * SC-06 (error)
   *
   * <p>Given O1 이 로그인해 있다 When 표의 filter 로 목록을 부른다 Then a NOPE·b open·c Draft 는 400, d 빈 값은 200 이고
   * ALL 과 같은 결과
   */
  @Test
  void test_sc06_filter_가_정의_밖이면_400_이고_빈_값은_전체로_본다() throws Exception {
    // Given
    Performance p = savedDefault(o1Id, false);
    SoftAssertions soft = new SoftAssertions();

    // When·Then 정의 밖 값은 400 이다
    for (String filter : new String[] {"NOPE", "open", "Draft"}) {
      soft.assertThat(status(organizerList(o1, "?filter=" + filter)))
          .as("filter=%s", filter)
          .isEqualTo(400);
    }
    // And 빈 값은 200 이고 ALL 과 같은 결과다
    MvcResult empty = organizerList(o1, "?filter=");
    soft.assertThat(status(empty)).as("filter= 상태").isEqualTo(200);
    soft.assertThat(idsOf(rows(empty, "items"))).as("filter= 결과").containsExactly(p.getId());
    soft.assertAll();
  }

  // ---------- SC-07 ----------

  /**
   * SC-07 (error)
   *
   * <p>Given O1 소유 공연 25개 When 표의 쿼리로 목록을 부른다 Then a~c·h 는 400 INVALID_REQUEST, i·j·k·l·n·o 는 400,
   * d·e·f·g·m 은 표와 같은 200 응답, 요청 뒤 공연 수는 25 그대로
   */
  @Test
  void test_sc07_page_size_의_경계는_400_이거나_자른_값으로_응답한다() throws Exception {
    // Given
    for (int i = 0; i < 25; i++) {
      saved(o1Id, false, false, d(7), d(29), d(30 + i), 100);
    }
    SoftAssertions soft = new SoftAssertions();

    // When·Then 400 INVALID_REQUEST 행
    for (String query : new String[] {"?page=-1", "?size=0", "?size=-1", "?page=2147483647"}) {
      MvcResult result = organizerList(o1, query);
      soft.assertThat(status(result)).as(query + " 상태").isEqualTo(400);
      soft.assertThat(code(result)).as(query + " code").isEqualTo("INVALID_REQUEST");
    }
    // And 타입 불일치 400 행
    for (String query :
        new String[] {
          "?page=2147483648",
          "?page=1.5",
          "?size=1.5",
          "?page=9223372036854775808",
          "?size=9223372036854775808",
          "?size=2147483648"
        }) {
      soft.assertThat(status(organizerList(o1, query))).as(query + " 상태").isEqualTo(400);
    }
    // And 200 행
    MvcResult first = organizerList(o1, "?page=0");
    soft.assertThat(status(first)).as("page=0").isEqualTo(200);
    soft.assertThat(num(json(first), "page")).isEqualTo(0L);
    soft.assertThat(rows(first, "items")).as("page=0 items").hasSize(20);
    MvcResult second = organizerList(o1, "?page=1");
    soft.assertThat(rows(second, "items")).as("page=1 items").hasSize(5);
    soft.assertThat(num(json(second), "totalElements")).isEqualTo(25L);
    MvcResult far = organizerList(o1, "?page=1000");
    soft.assertThat(status(far)).as("page=1000").isEqualTo(200);
    soft.assertThat(rows(far, "items")).as("page=1000 items").isEmpty();
    soft.assertThat(num(json(far), "totalElements")).isEqualTo(25L);
    MvcResult big = organizerList(o1, "?size=101");
    soft.assertThat(status(big)).as("size=101").isEqualTo(200);
    soft.assertThat(num(json(big), "size")).isEqualTo(100L);
    soft.assertThat(rows(big, "items")).as("size=101 items").hasSize(25);
    MvcResult blank = organizerList(o1, "?page=&size=");
    soft.assertThat(status(blank)).as("빈 값").isEqualTo(200);
    soft.assertThat(num(json(blank), "page")).isEqualTo(0L);
    soft.assertThat(num(json(blank), "size")).isEqualTo(20L);
    // And 400 으로 끝난 요청이 공연 행을 바꾸지 않는다
    soft.assertThat(performanceRepository.count()).isEqualTo(25L);
    soft.assertAll();
  }

  // ---------- SC-08 ----------

  /**
   * SC-08 (happy)
   *
   * <p>Given O1 이 R(A~B열 10석 55000)·S(C열 10석 33000)로 등록한 비공개 공연 P1 When O1 이 주최자 단건을 부른다 Then 200,
   * 주최자 응답 키 + sections, 구역 둘의 seatCount 20·10·price 55000·33000, published false·editable
   * true·visibilityChangeable true, 관객 단건은 404
   */
  @Test
  @SuppressWarnings("unchecked")
  void test_sc08_주최자_단건_조회는_좌석_요약과_가능_여부_플래그를_담는다() throws Exception {
    // Given
    Map<String, Object> body = r0();
    body.put(
        "sections", List.of(section("R", 55000, "A", "B", 10), section("S", 33000, "C", "C", 10)));
    MvcResult created = register(o1, body);
    assertThat(status(created)).as("픽스처 등록").isEqualTo(201);
    Long p1 = ((Number) json(created).get("id")).longValue();

    // When
    MvcResult result = organizerGet(o1, p1);

    // Then 200 이다
    assertThat(status(result)).isEqualTo(200);
    Map<String, Object> json = json(result);
    // And 주최자 응답 키를 가지며 sections 를 포함한다
    assertThat(json.keySet()).containsAll(ORGANIZER_KEYS).contains("sections");
    // And sections 는 두 구역을 담고 seatCount 는 20·10, price 는 55000·33000 이다
    List<Map<String, Object>> sections = (List<Map<String, Object>>) json.get("sections");
    assertThat(sections).hasSize(2);
    assertThat(sections.stream().map(s -> ((Number) s.get("seatCount")).intValue()).toList())
        .containsExactly(20, 10);
    assertThat(sections.stream().map(s -> ((Number) s.get("price")).intValue()).toList())
        .containsExactly(55000, 33000);
    // And published false, editable true, visibilityChangeable true
    assertThat(json.get("published")).isEqualTo(false);
    assertThat(json.get("editable")).isEqualTo(true);
    assertThat(json.get("visibilityChangeable")).isEqualTo(true);
    // And 같은 공연이 비공개라 관객 단건은 404 다
    assertThat(status(getAs(null, "/api/performances/" + p1))).isEqualTo(404);
  }

  // ---------- SC-09 ----------

  /**
   * SC-09 (error)
   *
   * <p>Given O1 소유 P1, O2 소유 비공개 Q1, 소유자 NULL 공개 N1 When O1 이 표의 요청(조회·publish·unpublish)을 Q1·N1·없는
   * id 로 보낸다 Then 모두 404, 규약 본문, Q1·N1 의 상태 그대로, Q1 과 없는 id 의 응답 형태 같음, 자기 공연 P1 조회는 200
   */
  @Test
  void test_sc09_남의_공연_소유자_없는_공연_없는_공연은_같은_404_이고_DB_는_그대로다() throws Exception {
    // Given
    Performance p1 = savedDefault(o1Id, false);
    Performance q1 = savedDefault(o2Id, false);
    Performance n1 = savedWithoutOwner();
    long missing = 999_999L;
    SoftAssertions soft = new SoftAssertions();
    Map<String, Set<String>> shapes = new LinkedHashMap<>();

    // When·Then
    for (Long id : new Long[] {q1.getId(), n1.getId(), missing}) {
      List<MvcResult> results = List.of(organizerGet(o1, id), publish(o1, id), unpublish(o1, id));
      for (MvcResult result : results) {
        String label = result.getRequest().getRequestURI();
        soft.assertThat(status(result)).as(label + " 상태").isEqualTo(404);
        soft.assertThat(code(result)).as(label + " code").isEqualTo(NOT_FOUND);
        soft.assertThat(json(result).get("message"))
            .as(label + " message")
            .isEqualTo("Performance not found: " + id);
      }
      shapes.put(String.valueOf(id), json(results.get(0)).keySet());
    }
    // And Q1·N1 의 published·cancelled·title 이 그대로다
    Performance q1After = reload(q1.getId());
    Performance n1After = reload(n1.getId());
    soft.assertThat(q1After.getPublished()).isFalse();
    soft.assertThat(q1After.getCancelled()).isFalse();
    soft.assertThat(q1After.getTitle()).isEqualTo(TITLE);
    soft.assertThat(n1After.getPublished()).isTrue();
    soft.assertThat(n1After.getCancelled()).isFalse();
    soft.assertThat(n1After.getTitle()).isEqualTo(TITLE);
    // And Q1 에 대한 응답 본문과 없는 id 에 대한 응답 본문의 키가 같다
    soft.assertThat(shapes.get(String.valueOf(q1.getId())))
        .isEqualTo(shapes.get(String.valueOf(missing)));
    // And O1 이 자기 공연을 조회하면 200 이다(대조군)
    soft.assertThat(status(organizerGet(o1, p1.getId()))).as("대조군").isEqualTo(200);
    soft.assertAll();
  }

  // ---------- SC-10 ----------

  /**
   * SC-10 (error)
   *
   * <p>Given O1 소유 비공개 P1 When 신규 세 경로(목록·단건·publish)에 표의 Authorization 을 싣는다 Then a 헤더 없음 401
   * UNAUTHORIZED, b 관객 403 FORBIDDEN_ROLE, c 주최자 200, a·b 뒤 P1 의 published 는 그대로 false, unpublish 도
   * a·b 가 같다
   */
  @Test
  void test_sc10_신규_경로는_토큰이_없으면_401_관객이면_403_이고_DB_는_그대로다() throws Exception {
    // Given
    Performance p1 = savedDefault(o1Id, false);
    String id = String.valueOf(p1.getId());
    SoftAssertions soft = new SoftAssertions();
    List<String> getPaths =
        List.of("/api/organizer/performances", "/api/organizer/performances/" + id);
    List<String> postPaths =
        List.of("/api/performances/" + id + "/publish", "/api/performances/" + id + "/unpublish");

    // When·Then a 헤더 없음 — 401 UNAUTHORIZED
    for (String path : getPaths) {
      MvcResult result = getAs(null, path);
      soft.assertThat(status(result)).as("a " + path).isEqualTo(401);
      soft.assertThat(code(result)).as("a " + path + " code").isEqualTo("UNAUTHORIZED");
    }
    for (String path : postPaths) {
      MvcResult result = postAs(null, path);
      soft.assertThat(status(result)).as("a " + path).isEqualTo(401);
      soft.assertThat(code(result)).as("a " + path + " code").isEqualTo("UNAUTHORIZED");
    }
    // And b 관객 토큰 — 403 FORBIDDEN_ROLE
    for (String path : getPaths) {
      MvcResult result = getAs(a1, path);
      soft.assertThat(status(result)).as("b " + path).isEqualTo(403);
      soft.assertThat(code(result)).as("b " + path + " code").isEqualTo("FORBIDDEN_ROLE");
      soft.assertThat(json(result).get("message")).isEqualTo("주최자만 이용할 수 있습니다");
    }
    for (String path : postPaths) {
      MvcResult result = postAs(a1, path);
      soft.assertThat(status(result)).as("b " + path).isEqualTo(403);
      soft.assertThat(code(result)).as("b " + path + " code").isEqualTo("FORBIDDEN_ROLE");
    }
    // And a·b 요청마다 P1 의 published 가 그대로 false 다
    soft.assertThat(reload(p1.getId()).getPublished()).isFalse();
    // And c 주최자 토큰은 세 경로 모두 200 이다(대조군)
    soft.assertThat(status(getAs(o1, getPaths.get(0)))).as("c 목록").isEqualTo(200);
    soft.assertThat(status(getAs(o1, getPaths.get(1)))).as("c 단건").isEqualTo(200);
    soft.assertThat(status(postAs(o1, postPaths.get(0)))).as("c publish").isEqualTo(200);
    soft.assertAll();
  }

  // ---------- SC-11 ----------

  /**
   * SC-11 (happy)
   *
   * <p>Given O1 소유 비공개 P1(openAt T+7d), 관객 목록에 없다 When publish Then 200·published
   * true·UPCOMING·editable·visibilityChangeable true, DB true, 관객 목록·단건에 나타남 When unpublish Then
   * 200·published false, DB false, 관객 목록에서 사라지고 단건 404
   */
  @Test
  void test_sc11_오픈_전_자기_공연은_공개_비공개_전환이_DB_와_관객_목록에_반영된다() throws Exception {
    // Given
    Performance p1 = savedDefault(o1Id, false);
    assertThat(audienceIds("")).as("Given: 관객 목록에 P1 이 없다").doesNotContain(p1.getId());

    // When publish
    MvcResult published = publish(o1, p1.getId());

    // Then 200 이고 응답 published true, status UPCOMING, editable true, visibilityChangeable true
    assertThat(status(published)).isEqualTo(200);
    Map<String, Object> body = json(published);
    assertThat(body.get("published")).isEqualTo(true);
    assertThat(body.get("status")).isEqualTo("UPCOMING");
    assertThat(body.get("editable")).isEqualTo(true);
    assertThat(body.get("visibilityChangeable")).isEqualTo(true);
    // And 저장된 행의 published 는 true 다
    assertThat(reload(p1.getId()).getPublished()).isTrue();
    // And 관객 목록에 나타나고 단건은 200 이다
    assertThat(audienceIds("")).contains(p1.getId());
    assertThat(status(getAs(null, "/api/performances/" + p1.getId()))).isEqualTo(200);

    // When unpublish
    MvcResult unpublished = unpublish(o1, p1.getId());

    // Then 200 이고 응답 published 는 false 다
    assertThat(status(unpublished)).isEqualTo(200);
    assertThat(json(unpublished).get("published")).isEqualTo(false);
    // And 저장된 행의 published 는 false 이고 관객 목록에서 사라지며 단건은 404 다
    assertThat(reload(p1.getId()).getPublished()).isFalse();
    assertThat(audienceIds("")).doesNotContain(p1.getId());
    assertThat(status(getAs(null, "/api/performances/" + p1.getId()))).isEqualTo(404);
  }

  // ---------- SC-12 ----------

  /**
   * SC-12 (happy)
   *
   * <p>Given O1 소유 공개 P1·비공개 P2(openAt T+7d) When P1 publish·P2 unpublish Then 둘 다 200·published 가
   * 각각 true·false, 행 값 그대로
   */
  @Test
  void test_sc12_이미_그_상태인_공연의_전환은_변경_없이_200_이다() throws Exception {
    // Given
    Performance p1 = savedDefault(o1Id, true);
    Performance p2 = savedDefault(o1Id, false);

    // When
    MvcResult r1 = publish(o1, p1.getId());
    MvcResult r2 = unpublish(o1, p2.getId());

    // Then 둘 다 200 이고 응답 published 는 각각 true·false 다
    assertThat(status(r1)).isEqualTo(200);
    assertThat(status(r2)).isEqualTo(200);
    assertThat(json(r1).get("published")).isEqualTo(true);
    assertThat(json(r2).get("published")).isEqualTo(false);
    // And 두 행의 값이 그대로다
    Performance a1After = reload(p1.getId());
    Performance a2After = reload(p2.getId());
    assertThat(a1After.getPublished()).isTrue();
    assertThat(a1After.getTitle()).isEqualTo(TITLE);
    assertThat(a1After.getOpenAt()).isEqualTo(p1.getOpenAt());
    assertThat(a2After.getPublished()).isFalse();
    assertThat(a2After.getTitle()).isEqualTo(TITLE);
    assertThat(a2After.getOpenAt()).isEqualTo(p2.getOpenAt());
  }

  // ---------- SC-13 ----------

  /**
   * SC-13 (error)
   *
   * <p>Given 공연의 openAt 이 T, 시계를 표의 시각으로 놓는다 When 상태별로 publish·unpublish Then a(T-1ms 비공개 publish)는
   * 200, b~g 는 409 VISIBILITY_LOCKED 이고 409 뒤 published 가 그대로
   */
  @Test
  void test_sc13_오픈_뒤에는_공개_비공개_전환이_409_VISIBILITY_LOCKED_이고_DB_는_그대로다() throws Exception {
    record Row(String label, Instant now, boolean published, boolean publish, int expected) {}
    List<Row> rows =
        List.of(
            new Row("a", T.minusMillis(1), false, true, 200),
            new Row("b", T, false, true, 409),
            new Row("c", T, true, false, 409),
            new Row("d", T, true, true, 409),
            new Row("e", T, false, false, 409),
            new Row("f", T.plusMillis(1), true, false, 409),
            new Row("g", T.plusMillis(1), false, true, 409));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      // Given
      Performance p = saved(o1Id, row.published(), false, T, d(29), d(30), 100);
      clock.setInstant(row.now());

      // When
      MvcResult result = row.publish() ? publish(o1, p.getId()) : unpublish(o1, p.getId());

      // Then
      soft.assertThat(status(result)).as("%s 상태", row.label()).isEqualTo(row.expected());
      if (row.expected() == 409) {
        soft.assertThat(code(result)).as("%s code", row.label()).isEqualTo("VISIBILITY_LOCKED");
        // And 409 인 요청 뒤 published 가 요청 전 값 그대로다
        soft.assertThat(reload(p.getId()).getPublished())
            .as("%s published", row.label())
            .isEqualTo(row.published());
      }
    }
    soft.assertAll();
  }

  // ---------- SC-14 ----------

  /**
   * SC-14 (error)
   *
   * <p>Given 취소된 O1 소유 공연(표의 공개 여부·시각) When publish·unpublish·PUT(U0) Then 모두 409
   * PERFORMANCE_CANCELLED, 행이 그대로, 주최자 단건은 200·CANCELLED·editable·visibilityChangeable false
   */
  @Test
  void test_sc14_취소된_공연은_공개_전환_수정_모두_409_PERFORMANCE_CANCELLED_이고_DB_는_그대로다() throws Exception {
    record Row(String label, boolean published, boolean beforeOpen, String op) {}
    List<Row> rows =
        List.of(
            new Row("a", false, true, "publish"),
            new Row("b", true, true, "unpublish"),
            new Row("c", false, true, "put"),
            new Row("d", true, true, "put"),
            new Row("e", false, false, "put"),
            new Row("f", true, false, "publish"),
            new Row("g", true, false, "put"));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      // Given
      Instant openAt = row.beforeOpen() ? d(7) : d(-1);
      Instant closeAt = row.beforeOpen() ? d(29) : d(5);
      Performance p = saved(o1Id, row.published(), true, openAt, closeAt, d(30), 100);

      // When
      MvcResult result =
          switch (row.op()) {
            case "publish" -> publish(o1, p.getId());
            case "unpublish" -> unpublish(o1, p.getId());
            default -> update(o1, p.getId(), u0(p));
          };

      // Then
      soft.assertThat(status(result)).as("%s 상태", row.label()).isEqualTo(409);
      soft.assertThat(code(result)).as("%s code", row.label()).isEqualTo("PERFORMANCE_CANCELLED");
      // And 요청 뒤 행이 그대로다
      Performance after = reload(p.getId());
      soft.assertThat(after.getPublished())
          .as("%s published", row.label())
          .isEqualTo(row.published());
      soft.assertThat(after.getTitle()).as("%s title", row.label()).isEqualTo(TITLE);
      soft.assertThat(after.getCancelled()).as("%s cancelled", row.label()).isTrue();
      // And 같은 공연의 주최자 단건은 200·CANCELLED·플래그 모두 false 다
      MvcResult detail = organizerGet(o1, p.getId());
      soft.assertThat(status(detail)).as("%s 단건", row.label()).isEqualTo(200);
      soft.assertThat(json(detail).get("status"))
          .as("%s status", row.label())
          .isEqualTo("CANCELLED");
      soft.assertThat(json(detail).get("editable")).as("%s editable", row.label()).isEqualTo(false);
      soft.assertThat(json(detail).get("visibilityChangeable"))
          .as("%s visibilityChangeable", row.label())
          .isEqualTo(false);
    }
    soft.assertAll();
  }

  // ---------- SC-15 ----------

  /**
   * SC-15 (happy)
   *
   * <p>Given 공연의 openAt 이 T, 시계를 표의 시각으로 놓는다 When O1 이 U0 로 PUT Then a~d 는 200·title 반영, e·f 는 409
   * REGISTRATION_ALREADY_OPEN·title 그대로, 비공개 + now>openAt 에서 시간 순서가 어긋난 본문은 400 INVALID_TIME_ORDER,
   * O2 의 PUT 은 404
   */
  @Test
  void test_sc15_수정은_비공개면_시각_제한이_없고_공개면_오픈_전에만_된다() throws Exception {
    record Row(String label, boolean published, Instant now, int expected) {}
    List<Row> rows =
        List.of(
            new Row("a", false, T.minusMillis(1), 200),
            new Row("b", false, T, 200),
            new Row("c", false, T.plusMillis(1), 200),
            new Row("d", true, T.minusMillis(1), 200),
            new Row("e", true, T, 409),
            new Row("f", true, T.plusMillis(1), 409));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      // Given
      Performance p = saved(o1Id, row.published(), false, T, d(29), d(30), 100);
      clock.setInstant(row.now());

      // When
      MvcResult result = update(o1, p.getId(), u0(p));

      // Then
      soft.assertThat(status(result)).as("%s 상태", row.label()).isEqualTo(row.expected());
      Performance after = reload(p.getId());
      if (row.expected() == 200) {
        // And 200 이면 저장된 title 이 바뀌어 있다
        soft.assertThat(after.getTitle()).as("%s title", row.label()).isEqualTo(ENCORE);
      } else {
        soft.assertThat(code(result))
            .as("%s code", row.label())
            .isEqualTo("REGISTRATION_ALREADY_OPEN");
        soft.assertThat(after.getTitle()).as("%s title", row.label()).isEqualTo(TITLE);
      }
    }

    // And 비공개이고 now > openAt 인 공연에 시간 순서가 어긋난 본문은 400 INVALID_TIME_ORDER 이고 행은 그대로다
    clock.setInstant(T.plusMillis(1));
    Performance draft = saved(o1Id, false, false, T, d(29), d(30), 100);
    Map<String, Object> bad = u0(draft);
    bad.put("closeAt", d(31).toString());
    MvcResult badOrder = update(o1, draft.getId(), bad);
    soft.assertThat(status(badOrder)).as("시간 순서").isEqualTo(400);
    soft.assertThat(code(badOrder)).as("시간 순서 code").isEqualTo("INVALID_TIME_ORDER");
    soft.assertThat(reload(draft.getId()).getTitle()).isEqualTo(TITLE);
    // And 같은 공연에 O2 가 PUT 하면 404 다
    soft.assertThat(status(update(o2, draft.getId(), u0(draft)))).as("남의 공연").isEqualTo(404);
    soft.assertAll();
  }

  // ---------- SC-16 ----------

  /**
   * SC-16 (happy)
   *
   * <p>Given 공연의 openAt 이 T When 표의 (상태 × 시각) 조합에서 단건·목록 플래그를 읽고, 같은 시각에 새로 만든 같은 상태의 공연에 실제
   * PUT·전환을 보낸다 Then 플래그가 표와 같고 editable 은 PUT 200 여부와, visibilityChangeable 은 전환 200 여부와 일치, 목록
   * 항목의 플래그도 단건과 같다
   */
  @Test
  void test_sc16_editable_visibilityChangeable_은_같은_시각의_실제_요청_결과와_일치한다() throws Exception {
    record Row(
        String label,
        Instant now,
        boolean published,
        boolean cancelled,
        boolean editable,
        boolean changeable) {}
    List<Row> rows =
        List.of(
            new Row("비공개 T-1ms", T.minusMillis(1), false, false, true, true),
            new Row("비공개 T", T, false, false, true, false),
            new Row("비공개 T+1ms", T.plusMillis(1), false, false, true, false),
            new Row("공개 T-1ms", T.minusMillis(1), true, false, true, true),
            new Row("공개 T", T, true, false, false, false),
            new Row("공개 T+1ms", T.plusMillis(1), true, false, false, false),
            new Row("취소(비공개) T-1ms", T.minusMillis(1), false, true, false, false),
            new Row("취소(공개) T+1ms", T.plusMillis(1), true, true, false, false));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      // Given
      clock.setInstant(row.now());
      Performance inspected = saved(o1Id, row.published(), row.cancelled(), T, d(29), d(30), 100);
      Performance acted = saved(o1Id, row.published(), row.cancelled(), T, d(29), d(30), 100);

      // When 단건과 목록의 플래그를 읽는다
      Map<String, Object> detail = json(organizerGet(o1, inspected.getId()));
      Map<String, Object> listed = null;
      for (Map<String, Object> item : rows(organizerList(o1, ""), "items")) {
        if (((Number) item.get("id")).longValue() == inspected.getId()) {
          listed = item;
        }
      }

      // Then 플래그가 표와 같다
      soft.assertThat(detail.get("editable"))
          .as("%s editable", row.label())
          .isEqualTo(row.editable());
      soft.assertThat(detail.get("visibilityChangeable"))
          .as("%s visibilityChangeable", row.label())
          .isEqualTo(row.changeable());
      // And 목록 항목의 플래그도 단건과 같다
      soft.assertThat(listed).as("%s 목록 항목", row.label()).isNotNull();
      if (listed != null) {
        soft.assertThat(listed.get("editable"))
            .as("%s 목록 editable", row.label())
            .isEqualTo(detail.get("editable"));
        soft.assertThat(listed.get("visibilityChangeable"))
            .as("%s 목록 visibilityChangeable", row.label())
            .isEqualTo(detail.get("visibilityChangeable"));
      }
      // And editable 은 PUT 이 200 인지와, visibilityChangeable 은 전환이 200 인지와 일치한다
      soft.assertThat(status(update(o1, acted.getId(), u0(acted))))
          .as("%s PUT", row.label())
          .isEqualTo(row.editable() ? 200 : 409);
      MvcResult toggle =
          row.published() ? unpublish(o1, acted.getId()) : publish(o1, acted.getId());
      soft.assertThat(status(toggle))
          .as("%s 전환", row.label())
          .isEqualTo(row.changeable() ? 200 : 409);
    }
    soft.assertAll();
  }

  // ---------- SC-17 ----------

  /**
   * SC-17 (error)
   *
   * <p>Given O1 소유 공연 하나 When 표의 {id} 로 단건·publish·unpublish·PUT Then a~c(0·-1·long 최댓값)는 404 규약
   * 본문, d~f(long 초과·1.5·abc)는 400, 어느 요청도 공연 행을 바꾸지 않는다
   */
  @Test
  void test_sc17_경로의_id_가_이상하면_404_또는_400_이다() throws Exception {
    // Given
    Performance p = savedDefault(o1Id, false);
    Map<String, Integer> ids = new LinkedHashMap<>();
    ids.put("0", 404);
    ids.put("-1", 404);
    ids.put("9223372036854775807", 404);
    ids.put("9223372036854775808", 400);
    ids.put("1.5", 400);
    ids.put("abc", 400);
    SoftAssertions soft = new SoftAssertions();

    for (Map.Entry<String, Integer> row : ids.entrySet()) {
      String id = row.getKey();
      // When
      Map<String, MvcResult> results = new LinkedHashMap<>();
      results.put("GET 단건", organizerGet(o1, id));
      results.put("publish", publish(o1, id));
      results.put("unpublish", unpublish(o1, id));
      results.put("PUT", update(o1, id, u0(p)));

      // Then
      for (Map.Entry<String, MvcResult> result : results.entrySet()) {
        String label = id + " " + result.getKey();
        soft.assertThat(status(result.getValue())).as(label).isEqualTo(row.getValue());
        if (row.getValue() == 404) {
          soft.assertThat(code(result.getValue())).as(label + " code").isEqualTo(NOT_FOUND);
          soft.assertThat(json(result.getValue()).get("message"))
              .as(label + " message")
              .isEqualTo("Performance not found: " + id);
        }
      }
    }
    // And 어느 요청도 공연 행을 바꾸지 않는다
    Performance after = reload(p.getId());
    soft.assertThat(after.getPublished()).isFalse();
    soft.assertThat(after.getTitle()).isEqualTo(TITLE);
    soft.assertThat(performanceRepository.count()).isEqualTo(1L);
    soft.assertAll();
  }

  // ---------- SC-18 ----------

  /**
   * SC-18 (regression)
   *
   * <p>Given 공개 공연 V(소유자 O1)와 취소된 공개 공연 When 토큰 없이 목록·단건·next-opening Then 키 집합은 기존과 같고, 응답 본문에
   * published·editable· visibilityChangeable·ownerId 가 없으며 목록 최상위 키는
   * content·page·size·totalElements
   */
  @Test
  void test_sc18_관객_응답에는_주최자_필드가_없다() throws Exception {
    // Given
    Performance v = saved(o1Id, true, false, d(7));
    saved(o1Id, true, true, d(8));

    // When
    MvcResult list = getAs(null, "/api/performances");
    MvcResult detail = getAs(null, "/api/performances/" + v.getId());
    MvcResult next = getAs(null, "/api/performances/next-opening");

    // Then 목록 항목의 키 집합은 기존과 같고 단건은 sections 를 더한 집합이다
    for (Map<String, Object> item : rows(list, "content")) {
      assertThat(item.keySet()).isEqualTo(AUDIENCE_KEYS);
    }
    Set<String> detailKeys = new LinkedHashSet<>(AUDIENCE_KEYS);
    detailKeys.add("sections");
    assertThat(json(detail).keySet()).isEqualTo(detailKeys);
    assertThat(json(next).keySet()).isEqualTo(AUDIENCE_KEYS);
    // And 어느 응답 본문에도 주최자 필드 문자열이 없다
    for (MvcResult result : List.of(list, detail, next)) {
      String body = result.getResponse().getContentAsString();
      assertThat(body).doesNotContain("published", "editable", "visibilityChangeable", "ownerId");
    }
    // And 목록 응답의 최상위 키는 content·page·size·totalElements 다
    assertThat(json(list).keySet())
        .containsExactlyInAnyOrder("content", "page", "size", "totalElements");
  }

  // ---------- SC-19 ----------

  /**
   * SC-19 (happy)
   *
   * <p>Given O1 소유 비공개 P1(openAt T+7d) When PUT(U0) Then 200·주최자 응답 키·published false·editable true
   * When cancel Then 200·CANCELLED· editable·visibilityChangeable false·published false, 저장된
   * cancelled true, ownerId 키 없음
   */
  @Test
  void test_sc19_수정_취소_응답도_주최자_응답_형태이고_취소_뒤_플래그가_바뀐다() throws Exception {
    // Given
    Long p1 = registeredBy(o1);
    Performance fixture = reload(p1);

    // When PUT
    MvcResult updated = update(o1, p1, u0(fixture));

    // Then 200 이고 주최자 응답 키·published false·editable true 다
    assertThat(status(updated)).isEqualTo(200);
    Map<String, Object> updatedBody = json(updated);
    assertThat(updatedBody.keySet()).containsAll(ORGANIZER_KEYS);
    assertThat(updatedBody.get("published")).isEqualTo(false);
    assertThat(updatedBody.get("editable")).isEqualTo(true);

    // When cancel
    MvcResult cancelled = postAs(o1, "/api/performances/" + p1 + "/cancel");

    // Then 200 이고 status CANCELLED, editable·visibilityChangeable false, published false 다
    assertThat(status(cancelled)).isEqualTo(200);
    Map<String, Object> cancelledBody = json(cancelled);
    assertThat(cancelledBody.get("status")).isEqualTo("CANCELLED");
    assertThat(cancelledBody.get("editable")).isEqualTo(false);
    assertThat(cancelledBody.get("visibilityChangeable")).isEqualTo(false);
    assertThat(cancelledBody.get("published")).isEqualTo(false);
    // And 저장된 행의 cancelled 는 true 다
    assertThat(reload(p1).getCancelled()).isTrue();
    // And 응답에 ownerId 키가 없다
    assertThat(updatedBody).doesNotContainKey("ownerId");
    assertThat(cancelledBody).doesNotContainKey("ownerId");
  }
}
