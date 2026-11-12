package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.OBJECT_MAPPER;
import static com.example.ticket_booking.support.AuthTestHelper.accessTokenOf;
import static com.example.ticket_booking.support.AuthTestHelper.code;
import static com.example.ticket_booking.support.AuthTestHelper.json;
import static com.example.ticket_booking.support.AuthTestHelper.loginBody;
import static com.example.ticket_booking.support.AuthTestHelper.signUpBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.repository.PerformanceZoneRepository;
import com.example.ticket_booking.repository.SeatRepository;
import com.example.ticket_booking.repository.SocialAccountRepository;
import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import com.example.ticket_booking.support.PendingTokenTestHelper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * TASK-022 시나리오 SC-09, SC-10, SC-15 — 쓰기 가드 제거.
 *
 * <p>가드 환경변수를 실제로 켠다(클래스 properties: WRITE_GUARD_ENABLED=true, WRITE_API_KEY=guard-key-0123456789).
 * SC-09 는 그 값이 들어갔는지 Environment 로 먼저 확인한다 — 설정 없이 이름만 "ON" 인 테스트가 되지 않게(CLAUDE.md 리뷰 지시). SC-15 의
 * preflight 를 위해 CORS_ALLOWED_ORIGINS 도 준다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-022.md
 */
@SpringBootTest(
    properties = {
      "WRITE_GUARD_ENABLED=true",
      "WRITE_API_KEY=guard-key-0123456789",
      "CORS_ALLOWED_ORIGINS=https://app.example.com"
    })
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class WriteGuardRemovalApiTest {

  private static final Instant T = Instant.parse("2026-11-11T08:00:00Z");
  private static final String PASSWORD = "password123";
  private static final String ORIGIN = "https://app.example.com";

  @Autowired private MockMvc mockMvc;
  @Autowired private Environment environment;
  @Autowired private ApplicationContext context;
  @Autowired private PerformanceRepository performanceRepository;
  @Autowired private SeatRepository seatRepository;
  @Autowired private PerformanceZoneRepository zoneRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private SocialAccountRepository socialAccountRepository;
  @Autowired private MutableClock clock;

  @Value("${JWT_SECRET}")
  private String secret;

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
    socialAccountRepository.deleteAll();
    userRepository.deleteAll();
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
    body.put("title", "소리풍경 밴드 나이트");
    body.put("venue", "롤링홀");
    body.put("startAt", T.plus(Duration.ofDays(30)).toString());
    body.put("openAt", T.plus(Duration.ofDays(7)).toString());
    body.put("closeAt", T.plus(Duration.ofDays(29)).toString());
    body.put("sections", List.of(section));
    return body;
  }

  private MvcResult postJson(String path, String body, String authorization) throws Exception {
    var request = post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    if (authorization != null) {
      request = request.header("Authorization", authorization);
    }
    return mockMvc.perform(request).andReturn();
  }

  /**
   * SC-09 (happy)
   *
   * <p>Given WRITE_GUARD_ENABLED=true·WRITE_API_KEY 를 실제로 주었고 Environment 로 확인했다 When X-Write-Key
   * 없이 가입·로그인·소셜 가입 완료·공연 등록을 차례로 보낸다 Then 201·200(accessToken)·200·201 이고 어느 응답 code 도
   * WRITE_KEY_INVALID 가 아니다
   */
  @Test
  void test_sc09_쓰기_키_없이_가입_로그인_소셜_가입_완료가_되고_가드_환경변수를_켜도_막히지_않는다() throws Exception {
    // Given 그 값이 실제로 들어갔다
    assertThat(environment.getProperty("WRITE_GUARD_ENABLED")).isEqualTo("true");
    assertThat(environment.getProperty("WRITE_API_KEY")).isEqualTo("guard-key-0123456789");
    SoftAssertions soft = new SoftAssertions();

    // When a 가입(ORGANIZER)
    MvcResult signedUp =
        postJson(
            "/api/auth/signup",
            json(signUpBody("o1@example.com", PASSWORD, PASSWORD, "한도윤", "ORGANIZER")),
            null);
    // When b 로그인
    MvcResult loggedIn =
        postJson("/api/auth/login", json(loginBody("o1@example.com", PASSWORD)), null);
    // When c 소셜 가입 완료
    Map<String, Object> completeBody = new LinkedHashMap<>();
    completeBody.put("pendingToken", PendingTokenTestHelper.valid(secret, T));
    completeBody.put("role", "AUDIENCE");
    MvcResult completed =
        postJson("/api/auth/social/complete", OBJECT_MAPPER.writeValueAsString(completeBody), null);

    // Then 상태가 표와 같다
    soft.assertThat(signedUp.getResponse().getStatus()).as("a 가입").isEqualTo(201);
    soft.assertThat(loggedIn.getResponse().getStatus()).as("b 로그인").isEqualTo(200);
    String token = loggedIn.getResponse().getStatus() == 200 ? accessTokenOf(loggedIn) : null;
    soft.assertThat(token).as("b accessToken").isNotBlank();
    soft.assertThat(completed.getResponse().getStatus()).as("c 소셜 가입 완료").isEqualTo(200);
    // When d 공연 등록(b 의 토큰)
    MvcResult registered =
        postJson("/api/performances", json(r0()), token == null ? null : "Bearer " + token);
    soft.assertThat(registered.getResponse().getStatus()).as("d 공연 등록").isEqualTo(201);

    // And 어느 응답 본문의 code 도 WRITE_KEY_INVALID 가 아니다
    for (MvcResult result : List.of(signedUp, loggedIn, completed, registered)) {
      String body = result.getResponse().getContentAsString();
      soft.assertThat(body.isEmpty() ? null : code(result))
          .as(result.getRequest().getRequestURI() + " code")
          .isNotEqualTo("WRITE_KEY_INVALID");
    }
    soft.assertAll();
  }

  /**
   * SC-10 (regression)
   *
   * <p>When 쓰기 가드 필터·설정 클래스를 이름으로 불러온다 Then 둘 다 ClassNotFoundException 이고 writeGuardFilter 빈이 없다
   */
  @Test
  void test_sc10_쓰기_가드_코드가_없다() {
    // When / Then 둘 다 ClassNotFoundException 이다
    assertThatThrownBy(() -> Class.forName("com.example.ticket_booking.config.WriteGuardFilter"))
        .isInstanceOf(ClassNotFoundException.class);
    assertThatThrownBy(() -> Class.forName("com.example.ticket_booking.config.WriteGuardConfig"))
        .isInstanceOf(ClassNotFoundException.class);
    // And 애플리케이션 컨텍스트에 writeGuardFilter 라는 이름의 빈이 없다
    assertThat(context.containsBean("writeGuardFilter")).isFalse();
  }

  /**
   * SC-15 (regression)
   *
   * <p>Given 허용된 Origin When 그 Origin 으로 OPTIONS /api/performances preflight Then Allow-Headers 가
   * 정확히 "Content-Type, Authorization" 이고 X-Write-Key 가 없다
   */
  @Test
  void test_sc15_CORS_preflight_가_키_헤더를_허용하지_않는다() throws Exception {
    // When
    MvcResult result =
        mockMvc
            .perform(
                options("/api/performances")
                    .header("Origin", ORIGIN)
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "authorization, content-type"))
            .andReturn();

    // Then Access-Control-Allow-Headers 가 정확히 Content-Type, Authorization 이다
    String allowHeaders = result.getResponse().getHeader("Access-Control-Allow-Headers");
    assertThat(allowHeaders).isEqualTo("Content-Type, Authorization");
    // And 그 값에 X-Write-Key 가 없다
    assertThat(allowHeaders).doesNotContainIgnoringCase("X-Write-Key");
  }
}
