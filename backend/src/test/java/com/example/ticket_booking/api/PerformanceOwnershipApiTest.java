package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.accessTokenOf;
import static com.example.ticket_booking.support.AuthTestHelper.login;
import static com.example.ticket_booking.support.AuthTestHelper.signUp;
import static com.example.ticket_booking.support.AuthTestHelper.tamperRole;
import static com.example.ticket_booking.support.PendingTokenTestHelper.ABSENT;
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
import com.example.ticket_booking.support.PendingTokenTestHelper;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.beans.factory.annotation.Value;
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
 * TASK-022 시나리오 SC-02~SC-08, SC-11, SC-13, SC-14 — 공연 쓰기의 역할 인가와 소유권.
 *
 * <p>SC-12(오류 응답 회귀 표)는 실제 서버가 필요해 PerformanceErrorResponseRegressionTest 에 둔다 — MockMvc 는 ERROR
 * 디스패치를 재현하지 못한다.
 *
 * <p>클래스 레벨 {@code @Transactional} 을 두지 않는다 — "DB 가 바뀌지 않는다" 와 "DB 에 반영된다" 는 리포지토리 count·findById 로
 * 새로 읽는다(CLAUDE.md 리뷰 지시). 주최자·관객 토큰은 실제 가입·로그인으로 얻고, 클레임 변형 토큰은 테스트 비밀로 직접 서명한다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-022.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class PerformanceOwnershipApiTest {

  private static final Instant T = Instant.parse("2026-11-11T08:00:00Z");
  private static final String PASSWORD = "password123";
  private static final String TITLE = "소리풍경 밴드 나이트";
  private static final String ENCORE = "소리풍경 밴드 나이트 앙코르";
  private static final String FORBIDDEN_MESSAGE = "주최자만 이용할 수 있습니다";

  @Autowired private MockMvc mockMvc;
  @Autowired private PerformanceRepository performanceRepository;
  @Autowired private SeatRepository seatRepository;
  @Autowired private PerformanceZoneRepository zoneRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private SocialAccountRepository socialAccountRepository;
  @Autowired private MutableClock clock;

  @Value("${JWT_SECRET}")
  private String secret;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  private String o1;
  private String o2;
  private String a1;
  private Long o1Id;

  @BeforeEach
  void setUp() throws Exception {
    clock.setInstant(T);
    clearTables();
    o1 = tokenOf("o1@example.com", "ORGANIZER");
    o2 = tokenOf("o2@example.com", "ORGANIZER");
    a1 = tokenOf("a1@example.com", "AUDIENCE");
    o1Id = userRepository.findByEmail("o1@example.com").orElseThrow().getId();
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

  // ---------- 규약 ----------

  private String tokenOf(String email, String role) throws Exception {
    signUp(mockMvc, email, PASSWORD, "한도윤", role);
    return accessTokenOf(login(mockMvc, email, PASSWORD));
  }

  /** 규약 R0 — R 등급 A~B열 열당 10석 55000. */
  private static Map<String, Object> r0() {
    Map<String, Object> section = new LinkedHashMap<>();
    section.put("grade", "R");
    section.put("price", 55000);
    section.put("rowStart", "A");
    section.put("rowEnd", "B");
    section.put("seatsPerRow", 10);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("title", TITLE);
    body.put("venue", "롤링홀");
    body.put("startAt", T.plus(Duration.ofDays(30)).toString());
    body.put("openAt", T.plus(Duration.ofDays(7)).toString());
    body.put("closeAt", T.plus(Duration.ofDays(29)).toString());
    body.put("sections", List.of(section));
    return body;
  }

  /** 규약 U0 — title 만 바꾼 수정 본문. */
  private static Map<String, Object> u0(String title) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("title", title);
    body.put("venue", "롤링홀");
    body.put("startAt", T.plus(Duration.ofDays(30)).toString());
    body.put("openAt", T.plus(Duration.ofDays(7)).toString());
    body.put("closeAt", T.plus(Duration.ofDays(29)).toString());
    return body;
  }

  /** 소유자 NULL 인 공연(기존 생성자). */
  private Performance savedWithoutOwner() {
    return performanceRepository.save(
        new Performance(
            TITLE,
            "롤링홀",
            T.plus(Duration.ofDays(30)),
            T.plus(Duration.ofDays(7)),
            T.plus(Duration.ofDays(29)),
            20,
            20,
            false));
  }

  /** 토큰 주인이 등록한 공연의 id. */
  private Long registeredBy(String token) throws Exception {
    MvcResult result = register(token, r0());
    assertThat(result.getResponse().getStatus()).as("픽스처 등록").isEqualTo(201);
    return ((Number) json(result).get("id")).longValue();
  }

  /** 테스트 비밀로 서명한 access 토큰. 값이 ABSENT 면 그 클레임을 넣지 않는다. */
  private String accessToken(Consumer<Map<String, Object>> edit) {
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("sub", String.valueOf(o1Id));
    claims.put("email", "o1@example.com");
    claims.put("role", "ORGANIZER");
    claims.put("typ", "access");
    claims.put("iat", T);
    claims.put("exp", T.plus(Duration.ofHours(1)));
    edit.accept(claims);
    return PendingTokenTestHelper.signedRaw(secret, claims);
  }

  // ---------- 호출 ----------

  private static MockHttpServletRequestBuilder withAuthorization(
      MockHttpServletRequestBuilder builder, String authorization) {
    return authorization == null ? builder : builder.header("Authorization", authorization);
  }

  private static String bearer(String token) {
    return token == null ? null : "Bearer " + token;
  }

  private MvcResult send(MockHttpServletRequestBuilder builder, String authorization)
      throws Exception {
    return mockMvc.perform(withAuthorization(builder, authorization)).andReturn();
  }

  private MvcResult register(String token, Map<String, Object> body) throws Exception {
    return registerWithHeader(bearer(token), body);
  }

  private MvcResult registerWithHeader(String authorization, Map<String, Object> body)
      throws Exception {
    return send(
        post("/api/performances")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonText.of(body)),
        authorization);
  }

  private MvcResult update(String authorization, Object id, Map<String, Object> body)
      throws Exception {
    return send(
        put("/api/performances/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonText.of(body)),
        authorization);
  }

  private MvcResult cancel(String authorization, Object id) throws Exception {
    return send(post("/api/performances/" + id + "/cancel"), authorization);
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

  private static int status(MvcResult result) {
    return result.getResponse().getStatus();
  }

  private Performance reload(Long id) {
    return performanceRepository.findById(id).orElseThrow();
  }

  // ---------- SC-02 ----------

  /**
   * SC-02 (regression)
   *
   * <p>Given 기존 생성자로 저장한 소유자 NULL 공연 P0 When 토큰 없이 목록·상세를 조회한다 Then 둘 다 200, 키 집합이 이 태스크 전과 같고
   * ownerId 키가 없다
   */
  @Test
  void test_sc02_소유자_없는_기존_공연의_관객_응답은_그대로다() throws Exception {
    // Given
    Performance p0 = savedWithoutOwner();

    // When
    MvcResult list = send(get("/api/performances"), null);
    MvcResult detail = send(get("/api/performances/" + p0.getId()), null);

    // Then 둘 다 200 이다
    assertThat(status(list)).isEqualTo(200);
    assertThat(status(detail)).isEqualTo(200);
    // And 목록 항목과 상세의 키 집합이 이 태스크 전과 같다
    @SuppressWarnings("unchecked")
    Map<String, Object> item =
        ((List<Map<String, Object>>) json(list).get("content"))
            .stream()
                .filter(row -> ((Number) row.get("id")).longValue() == p0.getId())
                .findFirst()
                .orElseThrow();
    Set<String> listKeys =
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
    assertThat(item.keySet()).isEqualTo(listKeys);
    Set<String> detailKeys = new java.util.HashSet<>(listKeys);
    detailKeys.add("sections");
    assertThat(json(detail).keySet()).isEqualTo(detailKeys);
    // And 어느 응답에도 ownerId 키가 없다
    assertThat(list.getResponse().getContentAsString()).doesNotContain("ownerId");
    assertThat(detail.getResponse().getContentAsString()).doesNotContain("ownerId");
  }

  // ---------- SC-03 ----------

  /**
   * SC-03 (happy)
   *
   * <p>Given 공연이 없다 When O1 토큰으로 R0 을 등록한다 Then 201, 응답에 ownerId 없음, 다시 읽은 ownerId 가 O1, 좌석 20행
   */
  @Test
  void test_sc03_주최자가_등록하면_그_사람이_소유자다() throws Exception {
    // When
    MvcResult result = register(o1, r0());

    // Then 201 이다
    assertThat(status(result)).isEqualTo(201);
    // And 응답 본문에 ownerId 키가 없다
    Map<String, Object> body = json(result);
    assertThat(body).doesNotContainKey("ownerId");
    // And 트랜잭션 밖에서 다시 읽은 공연의 ownerId 가 O1 의 id 다
    Long id = ((Number) body.get("id")).longValue();
    assertThat(reload(id).getOwnerId()).isEqualTo(o1Id);
    // And 그 공연의 좌석은 20행이다
    assertThat(seatRepository.count()).isEqualTo(20);
  }

  // ---------- SC-04 ----------

  /**
   * SC-04 (error)
   *
   * <p>Given O1 소유의 P1 When 쓰기 경로 3개에 표의 Authorization 을 싣는다 Then 상태·code 가 표와 같고, 공연·좌석 수와 P1 의
   * title·cancelled 가 그대로다
   */
  @Test
  void test_sc04_토큰이_없거나_무효면_401_관객이면_403_이고_DB_는_그대로다() throws Exception {
    // Given
    Long p1 = registeredBy(o1);
    long performances = performanceRepository.count();
    long seats = seatRepository.count();
    String brokenSignature = o1.substring(0, o1.length() - 1) + (o1.endsWith("A") ? "B" : "A");
    Map<String, String[]> rows = new LinkedHashMap<>();
    rows.put("a 헤더 없음", new String[] {null, "401", "UNAUTHORIZED"});
    rows.put("b 빈 문자열", new String[] {"", "401", "UNAUTHORIZED"});
    rows.put("c Bearer 만", new String[] {"Bearer ", "401", "UNAUTHORIZED"});
    rows.put("d 서명 변조", new String[] {bearer(brokenSignature), "401", "UNAUTHORIZED"});
    rows.put("e 관객", new String[] {bearer(a1), "403", "FORBIDDEN_ROLE"});
    rows.put(
        "f 관객 role 변조",
        new String[] {bearer(tamperRole(a1, "AUDIENCE", "ORGANIZER")), "401", "UNAUTHORIZED"});
    SoftAssertions soft = new SoftAssertions();

    for (Map.Entry<String, String[]> row : rows.entrySet()) {
      String authorization = row.getValue()[0];
      int expected = Integer.parseInt(row.getValue()[1]);
      String expectedCode = row.getValue()[2];
      Map<String, MvcResult> calls = new LinkedHashMap<>();
      // When
      calls.put("등록", registerWithHeader(authorization, r0()));
      calls.put("수정", update(authorization, p1, u0(ENCORE)));
      calls.put("취소", cancel(authorization, p1));

      for (Map.Entry<String, MvcResult> call : calls.entrySet()) {
        String label = row.getKey() + " · " + call.getKey();
        // Then 상태와 code 가 표와 같다
        soft.assertThat(status(call.getValue())).as(label + " 상태").isEqualTo(expected);
        soft.assertThat(code(call.getValue())).as(label + " code").isEqualTo(expectedCode);
        if (expected == 403) {
          soft.assertThat(json(call.getValue()).get("message"))
              .as(label + " message")
              .isEqualTo(FORBIDDEN_MESSAGE);
          soft.assertThat(call.getValue().getResponse().getContentType())
              .as(label + " Content-Type")
              .startsWith(MediaType.APPLICATION_JSON_VALUE);
        }
      }
      // And 요청마다 공연 수·좌석 수가 그대로다
      soft.assertThat(performanceRepository.count())
          .as(row.getKey() + " 공연 수")
          .isEqualTo(performances);
      soft.assertThat(seatRepository.count()).as(row.getKey() + " 좌석 수").isEqualTo(seats);
      // And P1 의 title·cancelled 가 그대로다
      Performance reloaded = reload(p1);
      soft.assertThat(reloaded.getTitle()).as(row.getKey() + " title").isEqualTo(TITLE);
      soft.assertThat(reloaded.getCancelled()).as(row.getKey() + " cancelled").isFalse();
    }
    soft.assertAll();
  }

  // ---------- SC-05 ----------

  /**
   * SC-05 (error)
   *
   * <p>Given 공연이 없다, 표의 클레임으로 서명한 access 토큰 When R0 을 등록한다 Then 상태·code 가 표와 같고, 201 이 아닌 행은 공연 수
   * 0, a 행의 ownerId 는 O1
   */
  @Test
  void test_sc05_역할_클레임이_정확한_값이_아니면_토큰이_무효다() throws Exception {
    // Given
    Map<String, Object[]> rows = new LinkedHashMap<>();
    rows.put("a ORGANIZER", new Object[] {"role", "ORGANIZER", 201, null});
    rows.put("b AUDIENCE", new Object[] {"role", "AUDIENCE", 403, "FORBIDDEN_ROLE"});
    rows.put("c 클레임 없음", new Object[] {"role", ABSENT, 401, "UNAUTHORIZED"});
    rows.put("d null", new Object[] {"role", null, 401, "UNAUTHORIZED"});
    rows.put("e 숫자", new Object[] {"role", 1, 401, "UNAUTHORIZED"});
    rows.put("f 배열", new Object[] {"role", List.of("ORGANIZER"), 401, "UNAUTHORIZED"});
    rows.put("g 객체", new Object[] {"role", Map.of("name", "ORGANIZER"), 401, "UNAUTHORIZED"});
    rows.put("h 불리언", new Object[] {"role", true, 401, "UNAUTHORIZED"});
    rows.put("i ADMIN", new Object[] {"role", "ADMIN", 401, "UNAUTHORIZED"});
    rows.put("j 소문자", new Object[] {"role", "organizer", 401, "UNAUTHORIZED"});
    rows.put("k 앞 공백", new Object[] {"role", " ORGANIZER", 401, "UNAUTHORIZED"});
    rows.put("l 빈 문자열", new Object[] {"role", "", 401, "UNAUTHORIZED"});
    rows.put("m 공백만", new Object[] {"role", "   ", 401, "UNAUTHORIZED"});
    rows.put("n 전각 공백", new Object[] {"role", "　", 401, "UNAUTHORIZED"});
    rows.put("o 제로폭 꼬리", new Object[] {"role", "ORGANIZER​", 401, "UNAUTHORIZED"});
    rows.put("p sub 없음", new Object[] {"sub", ABSENT, 401, "UNAUTHORIZED"});
    rows.put("q sub 숫자", new Object[] {"sub", 7, 401, "UNAUTHORIZED"});
    rows.put("r sub 7.0", new Object[] {"sub", "7.0", 401, "UNAUTHORIZED"});
    rows.put("s sub 7e0", new Object[] {"sub", "7e0", 401, "UNAUTHORIZED"});
    rows.put("t sub long 초과", new Object[] {"sub", "9223372036854775808", 401, "UNAUTHORIZED"});
    rows.put("u typ pending", new Object[] {"typ", "pending", 401, "UNAUTHORIZED"});
    rows.put("v 만료", new Object[] {"exp", T.minusSeconds(1), 401, "UNAUTHORIZED"});
    SoftAssertions soft = new SoftAssertions();

    for (Map.Entry<String, Object[]> row : rows.entrySet()) {
      String claim = (String) row.getValue()[0];
      Object value = row.getValue()[1];
      int expected = (Integer) row.getValue()[2];
      String token = accessToken(claims -> claims.put(claim, value));
      long before = performanceRepository.count();

      // When
      MvcResult result = register(token, r0());

      // Then 상태와 code 가 표와 같다
      soft.assertThat(status(result)).as(row.getKey() + " 상태").isEqualTo(expected);
      if (row.getValue()[3] != null) {
        soft.assertThat(code(result)).as(row.getKey() + " code").isEqualTo(row.getValue()[3]);
      }
      if (expected == 201) {
        // And a 행에서 저장된 공연의 ownerId 는 O1 의 id 다
        Long id = ((Number) json(result).get("id")).longValue();
        soft.assertThat(reload(id).getOwnerId()).as(row.getKey() + " ownerId").isEqualTo(o1Id);
        zoneRepository.deleteAll();
        seatRepository.deleteAll();
        performanceRepository.deleteAll();
      } else {
        // And 201 이 아닌 행에서는 공연 수가 그대로다
        soft.assertThat(performanceRepository.count()).as(row.getKey() + " 공연 수").isEqualTo(before);
      }
    }
    soft.assertAll();
  }

  // ---------- SC-06 ----------

  /**
   * SC-06 (error)
   *
   * <p>Given O1 의 정상 토큰 t When 표의 헤더로 R0 을 등록한다 Then 상태가 표와 같고 201 이 아닌 행은 공연 수가 직전 값 그대로 — k·l(제어
   * 문자 꼬리)은 요청 자체가 거부되는 400 이다
   */
  @Test
  void test_sc06_Authorization_헤더_앞뒤에_붙은_글자는_정상_토큰을_무효로_만든다() throws Exception {
    // Given
    String t = o1;
    Map<String, Object[]> rows = new LinkedHashMap<>();
    rows.put("a Bearer", new Object[] {"Bearer " + t, 201});
    rows.put("b bearer 소문자", new Object[] {"bearer " + t, 201});
    rows.put("c 공백 두 개", new Object[] {"Bearer  " + t, 401});
    rows.put("d 뒤 공백", new Object[] {"Bearer " + t + " ", 401});
    rows.put("e 앞 공백", new Object[] {" Bearer " + t, 401});
    rows.put("f 스킴 없음", new Object[] {t, 401});
    rows.put("g ~ 꼬리", new Object[] {"Bearer " + t + "~", 401});
    rows.put("h 비 ASCII 꼬리", new Object[] {"Bearer " + t + "é", 401});
    rows.put("i Basic", new Object[] {"Basic " + t, 401});
    rows.put("j = 꼬리", new Object[] {"Bearer " + t + "=", 401});
    rows.put("k LF 꼬리", new Object[] {"Bearer " + t + "\n", 400});
    rows.put("l NUL 꼬리", new Object[] {"Bearer " + t + "\u0000", 400});
    rows.put("m 서로게이트 꼬리", new Object[] {"Bearer " + t + "\uD800", 401});
    SoftAssertions soft = new SoftAssertions();

    for (Map.Entry<String, Object[]> row : rows.entrySet()) {
      int expected = (Integer) row.getValue()[1];
      long before = performanceRepository.count();

      // When
      MvcResult result = registerWithHeader((String) row.getValue()[0], r0());

      // Then 상태가 표와 같다
      soft.assertThat(status(result)).as(row.getKey() + " 상태").isEqualTo(expected);
      // And 201 이 아닌 행에서는 공연 수가 그대로다
      if (expected != 201) {
        soft.assertThat(performanceRepository.count()).as(row.getKey() + " 공연 수").isEqualTo(before);
      } else {
        soft.assertThat(performanceRepository.count())
            .as(row.getKey() + " 공연 수")
            .isEqualTo(before + 1);
      }
    }
    soft.assertAll();
  }

  // ---------- SC-07 ----------

  /**
   * SC-07 (error)
   *
   * <p>Given O2 소유 P2, 소유자 NULL P0 When O1 토큰으로 표의 id 에 수정·취소 Then 상태·본문이 표와 같고, a~c 는 message 의 id
   * 만 다르다, P2·P0 과 공연 수가 그대로다, 빈 title 본문은 세 경우 모두 400 INVALID_REQUEST
   */
  @Test
  void test_sc07_남의_공연_소유자_없는_공연_없는_공연은_같은_404_이고_DB_는_그대로다() throws Exception {
    // Given
    Long p2 = registeredBy(o2);
    Long p0 = savedWithoutOwner().getId();
    long performances = performanceRepository.count();
    Map<String, Object[]> rows = new LinkedHashMap<>();
    rows.put("a 남의 공연", new Object[] {p2, 404, "Performance not found: " + p2});
    rows.put("b 소유자 NULL", new Object[] {p0, 404, "Performance not found: " + p0});
    rows.put("c 없음", new Object[] {999999L, 404, "Performance not found: 999999"});
    rows.put("d 0", new Object[] {0L, 404, null});
    rows.put("e -1", new Object[] {-1L, 404, null});
    rows.put("f long 초과", new Object[] {"9223372036854775808", 400, null});
    rows.put("g 1.9", new Object[] {"1.9", 400, null});
    SoftAssertions soft = new SoftAssertions();
    Map<String, Set<String>> keySets = new LinkedHashMap<>();

    for (Map.Entry<String, Object[]> row : rows.entrySet()) {
      Object id = row.getValue()[0];
      int expected = (Integer) row.getValue()[1];
      Map<String, MvcResult> calls = new LinkedHashMap<>();
      // When
      calls.put("수정", update(bearer(o1), id, u0(ENCORE)));
      calls.put("취소", cancel(bearer(o1), id));

      for (Map.Entry<String, MvcResult> call : calls.entrySet()) {
        String label = row.getKey() + " · " + call.getKey();
        // Then 상태·본문이 표와 같다
        soft.assertThat(status(call.getValue())).as(label + " 상태").isEqualTo(expected);
        if (expected == 404) {
          soft.assertThat(code(call.getValue()))
              .as(label + " code")
              .isEqualTo("PERFORMANCE_NOT_FOUND");
          if (row.getValue()[2] != null) {
            soft.assertThat(json(call.getValue()).get("message"))
                .as(label + " message")
                .isEqualTo(row.getValue()[2]);
            keySets.put(label, json(call.getValue()).keySet());
          }
        }
      }
    }

    // And a·b·c 의 본문은 키 집합과 code 가 같다
    soft.assertThat(Set.copyOf(keySets.values())).as("404 본문 키 집합").hasSize(1);
    // And P2·P0 의 title·cancelled·ownerId 와 공연 수가 그대로다
    soft.assertThat(performanceRepository.count()).as("공연 수").isEqualTo(performances);
    Performance reloadedP2 = reload(p2);
    soft.assertThat(reloadedP2.getTitle()).as("P2 title").isEqualTo(TITLE);
    soft.assertThat(reloadedP2.getCancelled()).as("P2 cancelled").isFalse();
    soft.assertThat(reloadedP2.getOwnerId())
        .as("P2 ownerId")
        .isEqualTo(userRepository.findByEmail("o2@example.com").orElseThrow().getId());
    Performance reloadedP0 = reload(p0);
    soft.assertThat(reloadedP0.getTitle()).as("P0 title").isEqualTo(TITLE);
    soft.assertThat(reloadedP0.getCancelled()).as("P0 cancelled").isFalse();
    soft.assertThat(reloadedP0.getOwnerId()).as("P0 ownerId").isNull();

    // And 본문 검증은 소유권보다 먼저다 — 빈 title 수정 본문은 세 경우 모두 400 INVALID_REQUEST
    for (Object id : List.of(p2, p0, 999999L)) {
      MvcResult invalid = update(bearer(o1), id, u0(""));
      soft.assertThat(status(invalid)).as("빈 title · " + id + " 상태").isEqualTo(400);
      soft.assertThat(code(invalid)).as("빈 title · " + id + " code").isEqualTo("INVALID_REQUEST");
    }
    soft.assertAll();
  }

  // ---------- SC-08 ----------

  /**
   * SC-08 (happy)
   *
   * <p>Given 시계 T, O1 이 등록한 P1 When O1 토큰으로 U0 수정 Then 200·응답 title 앙코르, 다시 읽은 title 같고 ownerId O1
   */
  @Test
  void test_sc08_자기_공연을_오픈_전에_수정하면_반영된다() throws Exception {
    // Given
    Long p1 = registeredBy(o1);

    // When
    MvcResult result = update(bearer(o1), p1, u0(ENCORE));

    // Then 200 이고 응답 title 이 앙코르다
    assertThat(status(result)).isEqualTo(200);
    assertThat(json(result).get("title")).isEqualTo(ENCORE);
    // And 트랜잭션 밖에서 다시 읽은 P1 의 title 도 같고 ownerId 는 O1 그대로다
    Performance reloaded = reload(p1);
    assertThat(reloaded.getTitle()).isEqualTo(ENCORE);
    assertThat(reloaded.getOwnerId()).isEqualTo(o1Id);
  }

  // ---------- SC-11 ----------

  /**
   * SC-11 (happy)
   *
   * <p>Given 시계 T, 공연 P1(openAt T+7d) When 세 조회를 토큰 없이·무효 토큰으로 Then 모두 200, 상세·next-opening id 가 P1
   */
  @Test
  void test_sc11_공연_조회는_토큰_없이도_무효_토큰이_실려도_열린다() throws Exception {
    // Given
    Long p1 = registeredBy(o1);
    SoftAssertions soft = new SoftAssertions();

    for (String authorization : new String[] {null, "Bearer invalid.token.value"}) {
      String label = authorization == null ? "토큰 없음" : "무효 토큰";
      // When
      MvcResult list = send(get("/api/performances"), authorization);
      MvcResult detail = send(get("/api/performances/" + p1), authorization);
      MvcResult next = send(get("/api/performances/next-opening"), authorization);

      // Then 모두 200 이다
      soft.assertThat(status(list)).as(label + " 목록").isEqualTo(200);
      soft.assertThat(status(detail)).as(label + " 상세").isEqualTo(200);
      soft.assertThat(status(next)).as(label + " next-opening").isEqualTo(200);
      // And 상세·next-opening 응답 id 가 P1 이다
      soft.assertThat(((Number) json(detail).get("id")).longValue())
          .as(label + " 상세 id")
          .isEqualTo(p1);
      soft.assertThat(((Number) json(next).get("id")).longValue())
          .as(label + " next-opening id")
          .isEqualTo(p1);
    }
    soft.assertAll();
  }

  // ---------- SC-13 ----------

  /**
   * SC-13 (error)
   *
   * <p>Given O1 소유 P1(openAt T+7d), 시계 T+7d When O1 토큰으로 title '다른 제목' 수정 Then 409
   * REGISTRATION_ALREADY_OPEN, 다시 읽은 title 그대로
   */
  @Test
  void test_sc13_자기_공연이라도_오픈_시각이_되면_수정할_수_없다() throws Exception {
    // Given
    Long p1 = registeredBy(o1);
    clock.setInstant(T.plus(Duration.ofDays(7)));
    // 시계를 옮기면 T 에 받은 토큰(유효 24시간)은 만료된다 — 같은 O1 로 다시 로그인한다
    String o1Now = accessTokenOf(login(mockMvc, "o1@example.com", PASSWORD));

    // When
    MvcResult result = update(bearer(o1Now), p1, u0("다른 제목"));

    // Then 409 REGISTRATION_ALREADY_OPEN 이다
    assertThat(status(result)).isEqualTo(409);
    assertThat(code(result)).isEqualTo("REGISTRATION_ALREADY_OPEN");
    // And 트랜잭션 밖에서 다시 읽은 title 은 그대로다
    assertThat(reload(p1).getTitle()).isEqualTo(TITLE);
  }

  // ---------- SC-14 ----------

  /**
   * SC-14 (happy)
   *
   * <p>Given O1 소유 P1(취소 안 됨) When O1 토큰으로 취소 Then 200·응답 cancelled true, 다시 읽은 cancelled true
   */
  @Test
  void test_sc14_자기_공연을_취소하면_반영된다() throws Exception {
    // Given
    Long p1 = registeredBy(o1);

    // When
    MvcResult result = cancel(bearer(o1), p1);

    // Then 200 이고 응답 cancelled 가 true 다
    assertThat(status(result)).isEqualTo(200);
    assertThat(json(result).get("status")).isEqualTo("CANCELLED");
    // And 트랜잭션 밖에서 다시 읽은 P1 의 cancelled 가 true 다
    assertThat(reload(p1).getCancelled()).isTrue();
  }
}
