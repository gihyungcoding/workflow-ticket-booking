package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.OBJECT_MAPPER;
import static com.example.ticket_booking.support.AuthTestHelper.accessTokenOf;
import static com.example.ticket_booking.support.AuthTestHelper.bodyAsMap;
import static com.example.ticket_booking.support.AuthTestHelper.code;
import static com.example.ticket_booking.support.AuthTestHelper.getWithAuthorization;
import static com.example.ticket_booking.support.AuthTestHelper.login;
import static com.example.ticket_booking.support.AuthTestHelper.postJson;
import static com.example.ticket_booking.support.AuthTestHelper.signUp;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticket_booking.repository.SocialAccountRepository;
import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.HandleRandomTestConfig;
import com.example.ticket_booking.support.MutableClock;
import com.example.ticket_booking.support.PendingTokenTestHelper;
import com.example.ticket_booking.support.SequenceRandom;
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
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * TASK-016 시나리오 SC-01 ~ SC-06, SC-11, SC-12 — 티켓온 아이디(handle).
 *
 * <p>난수원은 {@link SequenceRandom} 으로 고정한다(시나리오의 「뽑힌 값」). 회원·아이디는 API 로만 만들고, 저장된 handle 은
 * JdbcTemplate 으로 새로 읽는다 — 클래스 레벨 {@code @Transactional} 을 두지 않는다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-016.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ClockTestConfig.class, HandleRandomTestConfig.class})
class UserHandleApiTest {

  private static final String HANDLE_PATTERN = "^[a-z]{3,6}[0-9]{4}$";
  private static final String PASSWORD = "password123";
  private static final Instant FIXED_NOW = Instant.parse("2026-11-11T08:35:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private SocialAccountRepository socialAccountRepository;
  @Autowired private SequenceRandom random;
  @Autowired private MutableClock clock;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Value("${JWT_SECRET}")
  private String secret;

  @BeforeEach
  void reset() {
    cleanUp();
    clock.setInstant(FIXED_NOW);
  }

  @AfterEach
  void cleanUp() {
    random.clear();
    socialAccountRepository.deleteAll();
    userRepository.deleteAll();
  }

  // ---------- 헬퍼 ----------

  private MvcResult emailSignUp(String email, String name) throws Exception {
    return signUp(mockMvc, email, PASSWORD, name, "AUDIENCE");
  }

  private static String handleOf(MvcResult result) throws Exception {
    return (String) bodyAsMap(result).get("handle");
  }

  @SuppressWarnings("unchecked")
  private static String userHandleOf(MvcResult result) throws Exception {
    Map<String, Object> user = (Map<String, Object>) bodyAsMap(result).get("user");
    return user == null ? null : (String) user.get("handle");
  }

  private String storedHandle(String email) {
    return jdbcTemplate.queryForObject(
        "SELECT handle FROM users WHERE email = ?", String.class, email);
  }

  private long countByHandle(String handle) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM users WHERE handle = ?", Long.class, handle);
  }

  /** 아이디 wave7302 인 회원을 API 로 만든다. */
  private void seedWave7302() throws Exception {
    random.enqueue(0, 7302);
    MvcResult seeded = emailSignUp("seed@example.com", "시드");
    assertThat(seeded.getResponse().getStatus()).as("시드 회원 가입").isEqualTo(201);
    assertThat(handleOf(seeded)).as("시드 회원 아이디").isEqualTo("wave7302");
  }

  private String kakaoPendingToken() {
    Map<String, Object> claims = PendingTokenTestHelper.validClaims(FIXED_NOW);
    claims.put("provider", "KAKAO");
    claims.put("email", "harin.kim@kakao.com");
    claims.put("name", "김하린");
    return PendingTokenTestHelper.signedClaims(secret, claims);
  }

  private MvcResult complete(String pendingToken) throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("pendingToken", pendingToken);
    body.put("role", "AUDIENCE");
    return postJson(mockMvc, "/api/auth/social/complete", OBJECT_MAPPER.writeValueAsString(body));
  }

  // ---------- SC-01 ----------

  @Test
  void test_sc01_이메일_가입_응답과_내_정보에_형식에_맞는_아이디가_있다() throws Exception {
    // Given users 가 비어 있고 뽑힌 값이 (0, 7302) 이다
    random.enqueue(0, 7302);

    // When harin.kim@kakao.com / 김하린 / AUDIENCE 로 가입한다
    MvcResult signUp = emailSignUp("harin.kim@kakao.com", "김하린");

    // Then 201 이고 응답의 handle 은 wave7302 다
    assertThat(signUp.getResponse().getStatus()).isEqualTo(201);
    assertThat(handleOf(signUp)).isEqualTo("wave7302");
    // And 같은 이메일·비밀번호로 로그인하면 응답 user.handle 이 wave7302 다
    MvcResult login = login(mockMvc, "harin.kim@kakao.com", PASSWORD);
    assertThat(userHandleOf(login)).isEqualTo("wave7302");
    // And 그 응답의 accessToken 으로 GET /api/auth/me 를 호출하면 handle 이 wave7302 다
    MvcResult me = getWithAuthorization(mockMvc, "/api/auth/me", "Bearer " + accessTokenOf(login));
    assertThat(handleOf(me)).isEqualTo("wave7302");
    // And users 를 새로 읽으면 그 행의 handle 이 wave7302 다
    assertThat(storedHandle("harin.kim@kakao.com")).isEqualTo("wave7302");
  }

  // ---------- SC-02 ----------

  @Test
  void test_sc02_단어_번호와_숫자의_경계에서도_형식이_맞다() throws Exception {
    // Given 뽑힌 값이 표의 쌍이다 (행마다 새로 가입한다)
    record Row(int word, int number, String handle) {}
    List<Row> rows =
        List.of(
            new Row(0, 0, "wave0000"),
            new Row(0, 42, "wave0042"),
            new Row(7, 5, "drum0005"),
            new Row(3, 1000, "encore1000"),
            new Row(31, 9999, "gig9999"));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      cleanUp();
      random.enqueue(row.word(), row.number());

      // When 이메일 가입을 한다
      MvcResult result = emailSignUp("user@example.com", "사용자");

      // Then 응답의 handle 이 표와 같고 형식에 맞는다
      String label = "(" + row.word() + ", " + row.number() + ")";
      soft.assertThat(result.getResponse().getStatus()).as(label + " 상태").isEqualTo(201);
      String handle = result.getResponse().getStatus() == 201 ? handleOf(result) : null;
      soft.assertThat(handle).as(label + " handle").isEqualTo(row.handle());
      soft.assertThat(handle).as(label + " 형식").isNotNull().matches(HANDLE_PATTERN);
    }
    soft.assertAll();
  }

  // ---------- SC-03 ----------

  @Test
  void test_sc03_아이디는_이메일_이름과_무관하다() throws Exception {
    // Given users 가 비어 있다
    record Row(String email, String name) {}
    List<Row> rows =
        List.of(
            new Row("harin.kim@kakao.com", "김하린"),
            new Row("minji.jung@gmail.com", "정민지"),
            new Row("wave9999@example.com", "wave"));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      cleanUp();
      // When 표의 이메일·이름으로, 뽑힌 값 (1, 123) 일 때 가입한다
      random.enqueue(1, 123);
      MvcResult result = emailSignUp(row.email(), row.name());

      // Then handle 이 star0123 이다
      soft.assertThat(result.getResponse().getStatus()).as(row.email() + " 상태").isEqualTo(201);
      String handle = result.getResponse().getStatus() == 201 ? handleOf(result) : null;
      soft.assertThat(handle).as(row.email() + " handle").isEqualTo("star0123");
    }
    soft.assertAll();
  }

  // ---------- SC-04 ----------

  @Test
  void test_sc04_이미_있는_아이디가_뽑히면_다시_뽑는다() throws Exception {
    // Given 아이디 wave7302 인 회원이 이미 있다
    seedWave7302();
    // And 다음 가입에서 뽑힌 값이 차례로 (0, 7302), (0, 7302), (1, 1) 이다
    random.enqueue(0, 7302, 2);
    random.enqueue(1, 1);

    // When new@example.com 으로 이메일 가입한다
    MvcResult result = emailSignUp("new@example.com", "신규");

    // Then 201 이고 handle 은 star0001 이다
    assertThat(result.getResponse().getStatus()).isEqualTo(201);
    assertThat(handleOf(result)).isEqualTo("star0001");
    // And users 에 wave7302 인 행은 여전히 하나다
    assertThat(countByHandle("wave7302")).isEqualTo(1);
  }

  // ---------- SC-05 ----------

  @Test
  void test_sc05_10번_연속_겹치면_가입이_503이고_아무것도_만들지_않는다() throws Exception {
    SoftAssertions soft = new SoftAssertions();

    // 행 1: (0, 7302) 9번, 그다음 (1, 1) → 201, star0001
    seedWave7302();
    random.enqueue(0, 7302, 9);
    random.enqueue(1, 1);
    MvcResult ninth = emailSignUp("new@example.com", "신규");
    soft.assertThat(ninth.getResponse().getStatus()).as("9번 겹친 뒤 상태").isEqualTo(201);
    soft.assertThat(ninth.getResponse().getStatus() == 201 ? handleOf(ninth) : null)
        .as("9번 겹친 뒤 handle")
        .isEqualTo("star0001");

    // 행 2: (0, 7302) 10번 → 503 HANDLE_GENERATION_FAILED, users 행 수 그대로
    cleanUp();
    seedWave7302();
    long usersBefore = userRepository.count();
    random.enqueue(0, 7302, 10);
    MvcResult tenth = emailSignUp("new@example.com", "신규");
    soft.assertThat(tenth.getResponse().getStatus()).as("10번 겹침 상태").isEqualTo(503);
    soft.assertThat(code(tenth)).as("10번 겹침 code").isEqualTo("HANDLE_GENERATION_FAILED");
    soft.assertThat(userRepository.count()).as("users 행 수").isEqualTo(usersBefore);
    soft.assertThat(userRepository.findByEmail("new@example.com"))
        .as("new@example.com 행")
        .isEmpty();
    soft.assertAll();
  }

  // ---------- SC-06 ----------

  @Test
  void test_sc06_소셜_최초_가입으로_만든_회원도_아이디를_받는다() throws Exception {
    // Given 유효한 pendingToken(KAKAO, harin.kim@kakao.com, 김하린)이 있다
    String token = kakaoPendingToken();
    // And 아이디 wave7302 인 회원이 있고 뽑힌 값이 차례로 (0, 7302), (2, 77) 이다
    seedWave7302();
    random.enqueue(0, 7302);
    random.enqueue(2, 77);

    // When 역할 AUDIENCE 로 POST /api/auth/social/complete 를 호출한다
    MvcResult result = complete(token);

    // Then 200 이고 응답 user.handle 은 stage0077 이다
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(userHandleOf(result)).isEqualTo("stage0077");
    // And 응답의 accessToken 으로 GET /api/auth/me 를 호출하면 handle 이 stage0077 이다
    MvcResult me = getWithAuthorization(mockMvc, "/api/auth/me", "Bearer " + accessTokenOf(result));
    assertThat(handleOf(me)).isEqualTo("stage0077");
  }

  // ---------- SC-11 ----------

  @Test
  void test_sc11_이메일_중복_가입은_여전히_409다() throws Exception {
    // Given harin.kim@kakao.com 회원이 이미 있다
    random.enqueue(0, 7302);
    assertThat(emailSignUp("harin.kim@kakao.com", "김하린").getResponse().getStatus())
        .as("기존 회원 가입")
        .isEqualTo(201);
    long usersBefore = userRepository.count();
    String handleBefore = storedHandle("harin.kim@kakao.com");
    SoftAssertions soft = new SoftAssertions();

    for (String email : List.of("harin.kim@kakao.com", "HARIN.KIM@KAKAO.COM")) {
      // 구현이 이메일 확인 전에 아이디를 뽑아도 하네스가 멈추지 않게 값을 넉넉히 준비한다
      random.clear();
      random.enqueue(1, 1);

      // When 같은 이메일로 이메일 가입한다
      MvcResult result = emailSignUp(email, "김하린");

      // Then 409 EMAIL_ALREADY_EXISTS 이고 users 행 수가 그대로다
      soft.assertThat(result.getResponse().getStatus()).as(email + " 상태").isEqualTo(409);
      soft.assertThat(code(result)).as(email + " code").isEqualTo("EMAIL_ALREADY_EXISTS");
      soft.assertThat(userRepository.count()).as(email + " users 행 수").isEqualTo(usersBefore);
    }
    // And 기존 회원의 handle 이 바뀌지 않는다
    soft.assertThat(handleBefore).as("기존 handle").isEqualTo("wave7302");
    soft.assertThat(storedHandle("harin.kim@kakao.com")).as("409 뒤 handle").isEqualTo(handleBefore);
    soft.assertAll();
  }

  // ---------- SC-12 ----------

  @Test
  void test_sc12_소셜_가입_완료에서도_10번_연속_겹치면_503이고_아무것도_만들지_않는다() throws Exception {
    SoftAssertions soft = new SoftAssertions();

    // 행 1: (0, 7302) 9번, 그다음 (1, 1) → 200, user.handle = star0001
    seedWave7302();
    random.enqueue(0, 7302, 9);
    random.enqueue(1, 1);
    MvcResult ninth = complete(kakaoPendingToken());
    soft.assertThat(ninth.getResponse().getStatus()).as("9번 겹친 뒤 상태").isEqualTo(200);
    soft.assertThat(ninth.getResponse().getStatus() == 200 ? userHandleOf(ninth) : null)
        .as("9번 겹친 뒤 user.handle")
        .isEqualTo("star0001");

    // 행 2: (0, 7302) 10번 → 503, users·social_accounts 행 수 그대로
    cleanUp();
    seedWave7302();
    long usersBefore = userRepository.count();
    long socialBefore = socialAccountRepository.count();
    random.enqueue(0, 7302, 10);
    MvcResult tenth = complete(kakaoPendingToken());
    soft.assertThat(tenth.getResponse().getStatus()).as("10번 겹침 상태").isEqualTo(503);
    soft.assertThat(code(tenth)).as("10번 겹침 code").isEqualTo("HANDLE_GENERATION_FAILED");
    soft.assertThat(userRepository.count()).as("users 행 수").isEqualTo(usersBefore);
    soft.assertThat(socialAccountRepository.count())
        .as("social_accounts 행 수")
        .isEqualTo(socialBefore);
    soft.assertAll();
  }
}
