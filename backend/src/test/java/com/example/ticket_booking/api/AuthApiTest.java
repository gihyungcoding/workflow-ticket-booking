package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.accessTokenOf;
import static com.example.ticket_booking.support.AuthTestHelper.bodyAsMap;
import static com.example.ticket_booking.support.AuthTestHelper.code;
import static com.example.ticket_booking.support.AuthTestHelper.getWithAuthorization;
import static com.example.ticket_booking.support.AuthTestHelper.json;
import static com.example.ticket_booking.support.AuthTestHelper.login;
import static com.example.ticket_booking.support.AuthTestHelper.loginBody;
import static com.example.ticket_booking.support.AuthTestHelper.payloadOf;
import static com.example.ticket_booking.support.AuthTestHelper.postEmpty;
import static com.example.ticket_booking.support.AuthTestHelper.postJson;
import static com.example.ticket_booking.support.AuthTestHelper.signUp;
import static com.example.ticket_booking.support.AuthTestHelper.signUpBody;
import static com.example.ticket_booking.support.AuthTestHelper.signedToken;
import static com.example.ticket_booking.support.AuthTestHelper.tamperRole;
import static com.example.ticket_booking.support.AuthTestHelper.unsignedToken;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticket_booking.domain.User;
import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * TASK-009 시나리오 SC-01 ~ SC-14.
 *
 * <p>클래스 레벨 {@code @Transactional} 을 두지 않는다 — 요청은 각자 커밋하고 DB 확인은 리포지토리로 새로 읽는다(저장·삭제·불변 단언이 1차 캐시에
 * 가려지지 않게). 표의 각 행은 {@link SoftAssertions} 로 독립 단언한다 — 한 행이 실패해도 나머지 행의 결과가 함께 보인다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-009.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class AuthApiTest {

  private static final String EMAIL = "user@example.com";
  private static final String PASSWORD = "password123";
  private static final String NAME = "홍길동";
  private static final Instant FIXED_NOW = Instant.parse("2026-10-21T00:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private MutableClock clock;

  @Value("${JWT_SECRET}")
  private String jwtSecret;

  @BeforeEach
  void reset() {
    userRepository.deleteAll();
    clock.setInstant(Instant.now());
  }

  @AfterEach
  void cleanUp() {
    userRepository.deleteAll();
    clock.setInstant(Instant.now());
  }

  // ---- 공용 ----

  /** Given 용 가입. 실패하면 이후 단언이 무의미하므로 즉시 실패시킨다. */
  private Map<String, Object> givenSignedUp(String email, String password, String name, String role)
      throws Exception {
    MvcResult result = signUp(mockMvc, email, password, name, role);
    assertThat(result.getResponse().getStatus()).as("Given: 가입이 201 이어야 한다").isEqualTo(201);
    return bodyAsMap(result);
  }

  private String givenLoggedInToken() throws Exception {
    MvcResult result = login(mockMvc, EMAIL, PASSWORD);
    assertThat(result.getResponse().getStatus()).as("Given: 로그인이 200 이어야 한다").isEqualTo(200);
    return accessTokenOf(result);
  }

  private static boolean hasPasswordKey(Map<String, Object> body) {
    return body.keySet().stream().anyMatch(key -> key.toLowerCase().contains("password"));
  }

  private Map<String, Object> validSignUp(String email) {
    return signUpBody(email, PASSWORD, PASSWORD, NAME, "AUDIENCE");
  }

  private record Case(String label, String rawBody) {}

  /** 주어진 로컬 파트 + @ + 3개 레이블 + .com 으로 입력 길이가 total 인 형식이 유효한 이메일. */
  private static String emailWithLocal(String local, int total) {
    int domainLength = total - local.length() - 1;
    int lastLabel = domainLength - 63 - 1 - 63 - 1 - ".com".length();
    String email =
        local + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(lastLabel) + ".com";
    assertThat(email).hasSize(total);
    return email;
  }

  /** 로컬 64자 + @ + 3개 레이블 + .com 으로 총 길이가 total 인 형식이 유효한 이메일. */
  private static String emailOfLength(int total) {
    String local = "a".repeat(64);
    int domainLength = total - local.length() - 1;
    int lastLabel = domainLength - 63 - 1 - 63 - 1 - ".com".length();
    String email =
        local + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(lastLabel) + ".com";
    assertThat(email).hasSize(total);
    return email;
  }

  // ---- SC-01 ----

  @Test
  void test_sc01_올바른_정보로_가입하면_201과_비밀번호_없는_사용자_정보를_받는다() throws Exception {
    // Given users 가 비어 있다
    record Row(String requestEmail, String role, String expectedEmail) {}
    List<Row> rows =
        List.of(
            new Row("user@example.com", "AUDIENCE", "user@example.com"),
            new Row("organizer@example.com", "ORGANIZER", "organizer@example.com"),
            new Row("  User@Example.COM  ", "AUDIENCE", "user@example.com"));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      userRepository.deleteAll();

      // When POST /api/auth/signup 을 호출한다 (password·passwordConfirm 은 password123, name 은 홍길동)
      MvcResult result = signUp(mockMvc, row.requestEmail(), PASSWORD, NAME, row.role());

      // Then 모든 행이 201 이다
      String label = "[" + row.requestEmail() + " / " + row.role() + "]";
      soft.assertThat(result.getResponse().getStatus()).as(label + " 상태").isEqualTo(201);
      if (result.getResponse().getStatus() == 201) {
        Map<String, Object> body = bodyAsMap(result);
        // And 응답 본문의 키는 정확히 id, email, name, role 이다 (비밀번호 관련 키 없음)
        soft.assertThat(body.keySet())
            .as(label + " 키")
            .containsExactlyInAnyOrder("id", "email", "name", "role");
        // And email 은 기대값, role 은 요청값, id 는 숫자다
        soft.assertThat(body.get("email")).as(label + " email").isEqualTo(row.expectedEmail());
        soft.assertThat(body.get("role")).as(label + " role").isEqualTo(row.role());
        soft.assertThat(body.get("id")).as(label + " id").isInstanceOf(Number.class);
      }
    }
    soft.assertAll();
  }

  // ---- SC-02 ----

  @Test
  void test_sc02_저장된_비밀번호는_평문이_아니라_BCrypt_해시다() throws Exception {
    // Given user@example.com / password123 으로 가입이 끝났다
    givenSignedUp(EMAIL, PASSWORD, NAME, "AUDIENCE");

    // When users 의 해당 행을 요청과 별개로 새로 조회한다 (트랜잭션 밖)
    List<User> users = userRepository.findAll();
    assertThat(users).as("Given: 가입한 사용자 1명").hasSize(1);
    String stored = users.get(0).getPasswordHash();

    // Then 저장된 값(password_hash)은 password123 과 다르다
    assertThat(stored).isNotEqualTo(PASSWORD);
    // And BCrypt 형식($2a$·$2b$·$2y$ 로 시작하는 60자)이다
    assertThat(stored).matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    // And password123 의 BCrypt 해시로 검증되고 password124 로는 검증되지 않는다
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    assertThat(encoder.matches(PASSWORD, stored)).isTrue();
    assertThat(encoder.matches("password124", stored)).isFalse();
  }

  // ---- SC-03 ----

  @Test
  void test_sc03_이미_있는_이메일로_가입하면_409다() throws Exception {
    // Given user@example.com 이 이름 홍길동, 비밀번호 password123 으로 가입돼 있다
    givenSignedUp(EMAIL, PASSWORD, NAME, "AUDIENCE");
    List<User> before = userRepository.findAll();
    assertThat(before).as("Given: 가입한 사용자 1명").hasSize(1);
    String originalHash = before.get(0).getPasswordHash();
    List<String> emails = List.of("user@example.com", "USER@EXAMPLE.COM", "  user@example.com  ");
    SoftAssertions soft = new SoftAssertions();

    for (String email : emails) {
      // When 다른 이름(김철수)·다른 비밀번호(password999)로 가입을 요청한다
      MvcResult result =
          postJson(
              mockMvc,
              "/api/auth/signup",
              json(signUpBody(email, "password999", "password999", "김철수", "AUDIENCE")));

      // Then 409 이고 code 는 EMAIL_ALREADY_EXISTS 다
      String label = "[" + email + "]";
      soft.assertThat(result.getResponse().getStatus()).as(label + " 상태").isEqualTo(409);
      if (result.getResponse().getStatus() == 409) {
        soft.assertThat(code(result)).as(label + " code").isEqualTo("EMAIL_ALREADY_EXISTS");
      }
      // And users 의 행 수는 1 그대로이고, 기존 사용자의 이름·비밀번호 해시는 변하지 않았다
      List<User> after = userRepository.findAll();
      soft.assertThat(after).as(label + " 행 수").hasSize(1);
      if (after.size() == 1) {
        soft.assertThat(after.get(0).getName()).as(label + " 이름").isEqualTo(NAME);
        soft.assertThat(after.get(0).getPasswordHash()).as(label + " 해시").isEqualTo(originalHash);
      }
    }
    soft.assertAll();
  }

  // ---- SC-04 ----

  @Test
  void test_sc04_필수_값이_없거나_형식이_틀리면_400이다() throws Exception {
    // Given users 가 비어 있다
    List<Case> cases = new ArrayList<>();
    for (String mutation : new String[] {"missing", "null", "empty", "blank"}) {
      cases.add(mutated("email", mutation));
    }
    for (String badEmail : new String[] {"plainaddress", "a@", "@b.com", "a b@example.com"}) {
      cases.add(
          new Case(
              "email 형식 " + badEmail,
              json(signUpBody(badEmail, PASSWORD, PASSWORD, NAME, "AUDIENCE"))));
    }
    for (String mutation : new String[] {"missing", "null", "empty"}) {
      cases.add(mutated("password", mutation));
    }
    // 공백 8개 — 길이는 만족하므로 passwordConfirm 도 같은 값으로 맞춰 필수 검증만 걸리게 한다
    cases.add(
        new Case(
            "password 공백 8개",
            json(signUpBody("blank-pw@example.com", "        ", "        ", NAME, "AUDIENCE"))));
    for (String mutation : new String[] {"missing", "null", "empty"}) {
      cases.add(mutated("passwordConfirm", mutation));
    }
    for (String mutation : new String[] {"missing", "null", "empty", "blank"}) {
      cases.add(mutated("name", mutation));
    }
    for (String mutation : new String[] {"missing", "null"}) {
      cases.add(mutated("role", mutation));
    }
    for (Object badRole : new Object[] {"ADMIN", "audience", 1}) {
      Map<String, Object> body = validSignUp("bad-role@example.com");
      body.put("role", badRole);
      cases.add(new Case("role 정의 밖 값 " + badRole, json(body)));
    }
    cases.add(new Case("본문 깨진 JSON", "{"));
    SoftAssertions soft = new SoftAssertions();

    for (Case c : cases) {
      // When 유효한 요청에서 항목 하나만 바꿔 가입을 요청한다
      MvcResult result = postJson(mockMvc, "/api/auth/signup", c.rawBody());

      // Then 400 이고 code 는 INVALID_REQUEST 다
      soft.assertThat(result.getResponse().getStatus()).as(c.label() + " 상태").isEqualTo(400);
      if (result.getResponse().getStatus() == 400) {
        soft.assertThat(code(result)).as(c.label() + " code").isEqualTo("INVALID_REQUEST");
      }
    }
    // 본문 없음
    MvcResult emptyBody = postEmpty(mockMvc, "/api/auth/signup");
    soft.assertThat(emptyBody.getResponse().getStatus()).as("본문 없음 상태").isEqualTo(400);
    if (emptyBody.getResponse().getStatus() == 400) {
      soft.assertThat(code(emptyBody)).as("본문 없음 code").isEqualTo("INVALID_REQUEST");
    }
    // And users 에는 행이 생기지 않는다
    soft.assertThat(userRepository.count()).as("users 행 수").isZero();
    soft.assertAll();
  }

  /** 유효한 가입 요청에서 field 하나를 missing/null/empty/blank 로 바꾼 케이스. */
  private Case mutated(String field, String mutation) {
    Map<String, Object> body = new LinkedHashMap<>(validSignUp("mutated@example.com"));
    switch (mutation) {
      case "missing" -> body.remove(field);
      case "null" -> body.put(field, null);
      case "empty" -> body.put(field, "");
      case "blank" -> body.put(field, "   ");
      default -> throw new IllegalArgumentException(mutation);
    }
    return new Case(field + " " + mutation, json(body));
  }

  // ---- SC-05 ----

  @Test
  void test_sc05_필드_길이는_상한_플러스1에서_거부되고_상한_정확히는_통과한다() throws Exception {
    // Given users 가 비어 있다
    record Row(String label, String email, String password, String name, int expected) {}
    List<Row> rows =
        List.of(
            new Row("email 255자", emailOfLength(255), PASSWORD, NAME, 201),
            new Row("email 256자", emailOfLength(256), PASSWORD, NAME, 400),
            new Row("password 7자", "pw7@example.com", "a".repeat(7), NAME, 400),
            new Row("password 8자", "pw8@example.com", "a".repeat(8), NAME, 201),
            new Row("password 72자", "pw72@example.com", "a".repeat(72), NAME, 201),
            new Row("password 73자", "pw73@example.com", "a".repeat(73), NAME, 400),
            new Row("name 100자", "name100@example.com", PASSWORD, "가".repeat(100), 201),
            new Row("name 101자", "name101@example.com", PASSWORD, "가".repeat(101), 400));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      long before = userRepository.count();

      // When 유효한 요청에서 해당 항목 하나만 바꿔 가입을 요청한다
      MvcResult result =
          postJson(
              mockMvc,
              "/api/auth/signup",
              json(
                  signUpBody(row.email(), row.password(), row.password(), row.name(), "AUDIENCE")));

      // Then 각 행의 상태가 기대와 같다
      soft.assertThat(result.getResponse().getStatus())
          .as(row.label() + " 상태")
          .isEqualTo(row.expected());
      if (row.expected() == 400 && result.getResponse().getStatus() == 400) {
        // And 400 행에서는 code 가 INVALID_REQUEST 이고 users 에 행이 생기지 않는다
        soft.assertThat(code(result)).as(row.label() + " code").isEqualTo("INVALID_REQUEST");
      }
      if (row.expected() == 400) {
        soft.assertThat(userRepository.count()).as(row.label() + " 행 수").isEqualTo(before);
      }
    }
    soft.assertAll();
  }

  // ---- SC-06 ----

  @Test
  void test_sc06_비밀번호는_UTF8_72바이트를_넘지_않고_확인값과_같아야_한다() throws Exception {
    // Given users 가 비어 있다
    record Row(String label, String password, String confirm, int expected) {}
    String korean24 = "가".repeat(24);
    String korean25 = "가".repeat(25);
    String emoji19 = "😀".repeat(19);
    List<Row> rows =
        List.of(
            new Row("한글 24자(72바이트)", korean24, korean24, 201),
            new Row("한글 25자(75바이트)", korean25, korean25, 400),
            new Row("이모지 19개(76바이트)", emoji19, emoji19, 400),
            new Row("확인값 불일치", "password123", "password124", 400),
            new Row("대소문자만 다른 확인값", "password123", "Password123", 400));
    SoftAssertions soft = new SoftAssertions();
    int index = 0;

    for (Row row : rows) {
      long before = userRepository.count();
      String email = "bytes" + (index++) + "@example.com";

      // When 가입을 요청한다
      MvcResult result =
          postJson(
              mockMvc,
              "/api/auth/signup",
              json(signUpBody(email, row.password(), row.confirm(), NAME, "AUDIENCE")));

      // Then 각 행의 상태가 기대와 같다 (500 이 아니다)
      soft.assertThat(result.getResponse().getStatus())
          .as(row.label() + " 상태")
          .isEqualTo(row.expected());
      if (row.expected() == 400) {
        if (result.getResponse().getStatus() == 400) {
          // And 400 행: code 는 INVALID_REQUEST 이고 응답 본문에 입력한 비밀번호가 없다
          soft.assertThat(code(result)).as(row.label() + " code").isEqualTo("INVALID_REQUEST");
          soft.assertThat(result.getResponse().getContentAsString())
              .as(row.label() + " 본문에 비밀번호 없음")
              .doesNotContain(row.password());
        }
        // And users 에 행이 생기지 않는다
        soft.assertThat(userRepository.count()).as(row.label() + " 행 수").isEqualTo(before);
      }
    }
    soft.assertAll();
  }

  // ---- SC-07 ----

  @Test
  void test_sc07_올바른_이메일_비밀번호로_로그인하면_200과_유효한_accessToken을_받는다() throws Exception {
    // Given user@example.com / password123 으로 가입돼 있다 (홍길동, AUDIENCE)
    Map<String, Object> signedUp = givenSignedUp(EMAIL, PASSWORD, NAME, "AUDIENCE");
    Object id = signedUp.get("id");
    List<String> emails = List.of("user@example.com", "  USER@Example.com  ");
    SoftAssertions soft = new SoftAssertions();

    for (String email : emails) {
      // When POST /api/auth/login 을 호출한다
      MvcResult result = login(mockMvc, email, PASSWORD);

      // Then 모든 행이 200 이다
      String label = "[" + email + "]";
      soft.assertThat(result.getResponse().getStatus()).as(label + " 상태").isEqualTo(200);
      if (result.getResponse().getStatus() != 200) {
        continue;
      }
      Map<String, Object> body = bodyAsMap(result);
      // And accessToken 은 비어 있지 않은 점 두 개로 구분된 세 부분 문자열이다
      String token = (String) body.get("accessToken");
      soft.assertThat(token).as(label + " accessToken").isNotBlank();
      if (token != null) {
        soft.assertThat(token.split("\\.")).as(label + " 토큰 구조").hasSize(3).doesNotContain("");
      }
      // And tokenType 은 Bearer, expiresInSeconds 는 86400 이다
      soft.assertThat(body.get("tokenType")).as(label + " tokenType").isEqualTo("Bearer");
      soft.assertThat(((Number) body.get("expiresInSeconds")).longValue())
          .as(label + " expiresInSeconds")
          .isEqualTo(86400L);
      // And user 는 {id, email, name, role} 이고 비밀번호 관련 키가 없다
      @SuppressWarnings("unchecked")
      Map<String, Object> user = (Map<String, Object>) body.get("user");
      soft.assertThat(user)
          .as(label + " user")
          .containsEntry("id", id)
          .containsEntry("email", EMAIL)
          .containsEntry("name", NAME)
          .containsEntry("role", "AUDIENCE");
      soft.assertThat(user.keySet())
          .as(label + " user 키")
          .doesNotContain("password", "passwordHash");
      // And 토큰 클레임은 sub=N, email, role, typ=access 이고 exp − iat 은 86400초다
      if (token != null && token.split("\\.").length == 3) {
        Map<String, Object> claims = payloadOf(token);
        soft.assertThat(String.valueOf(claims.get("sub")))
            .as(label + " sub")
            .isEqualTo(String.valueOf(id));
        soft.assertThat(claims.get("email")).as(label + " claim email").isEqualTo(EMAIL);
        soft.assertThat(claims.get("role")).as(label + " claim role").isEqualTo("AUDIENCE");
        soft.assertThat(claims.get("typ")).as(label + " claim typ").isEqualTo("access");
        soft.assertThat(
                ((Number) claims.get("exp")).longValue() - ((Number) claims.get("iat")).longValue())
            .as(label + " exp - iat")
            .isEqualTo(86400L);
      }
    }
    soft.assertAll();
  }

  // ---- SC-08 ----

  @Test
  void test_sc08_틀린_비밀번호와_없는_이메일은_똑같은_401을_받는다() throws Exception {
    // Given user@example.com / password123 으로 가입돼 있다
    givenSignedUp(EMAIL, PASSWORD, NAME, "AUDIENCE");
    // And long@example.com 이 ASCII a 72개, long-ko@example.com 이 한글 가 24개(72바이트)를 비밀번호로 가입돼 있다
    givenSignedUp("long@example.com", "a".repeat(72), NAME, "AUDIENCE");
    givenSignedUp("long-ko@example.com", "가".repeat(24), NAME, "AUDIENCE");
    record Row(String label, String email, String password) {}
    List<Row> rows =
        List.of(
            new Row("틀린 비밀번호", EMAIL, "wrong-password"),
            new Row("존재하지 않는 이메일", "nobody@example.com", PASSWORD),
            new Row("72바이트 초과 비밀번호(ASCII 73자)", EMAIL, "a".repeat(73)),
            new Row("한글 25자(75바이트) 비밀번호", EMAIL, "가".repeat(25)),
            new Row("72바이트 비밀번호 + 1글자(ASCII)", "long@example.com", "a".repeat(72) + "b"),
            new Row("72바이트 비밀번호 + 1글자(한글)", "long-ko@example.com", "가".repeat(24) + "b"),
            new Row("256자 이메일", emailOfLength(256), PASSWORD));
    SoftAssertions soft = new SoftAssertions();
    String firstBody = null;

    for (Row row : rows) {
      // When 각 행으로 로그인을 요청한다
      MvcResult result = login(mockMvc, row.email(), row.password());
      String body = result.getResponse().getContentAsString();

      // Then 모든 행이 401 이고 code 는 INVALID_CREDENTIALS 다 (500 이 아니다)
      soft.assertThat(result.getResponse().getStatus()).as(row.label() + " 상태").isEqualTo(401);
      if (result.getResponse().getStatus() == 401) {
        soft.assertThat(code(result)).as(row.label() + " code").isEqualTo("INVALID_CREDENTIALS");
        // And 모든 행의 응답 본문 전체(code 와 message)가 글자 단위로 서로 같다
        if (firstBody == null) {
          firstBody = body;
        } else {
          soft.assertThat(body).as(row.label() + " 본문").isEqualTo(firstBody);
        }
      }
      // And 응답 본문에 accessToken 이 없다
      soft.assertThat(body).as(row.label() + " accessToken 없음").doesNotContain("accessToken");
    }
    soft.assertAll();
  }

  // ---- SC-09 ----

  @Test
  void test_sc09_로그인_요청의_필수_값_형식이_틀리면_401이_아니라_400이다() throws Exception {
    // Given user@example.com / password123 으로 가입돼 있다
    givenSignedUp(EMAIL, PASSWORD, NAME, "AUDIENCE");
    List<Case> cases = new ArrayList<>();
    for (String mutation : new String[] {"missing", "null", "empty", "blank"}) {
      cases.add(loginMutated("email", mutation));
    }
    cases.add(new Case("email 형식 plainaddress", json(loginBody("plainaddress", PASSWORD))));
    for (String mutation : new String[] {"missing", "null", "empty", "blank"}) {
      cases.add(loginMutated("password", mutation));
    }
    cases.add(new Case("본문 깨진 JSON", "{"));
    SoftAssertions soft = new SoftAssertions();

    for (Case c : cases) {
      // When 유효한 요청에서 항목 하나만 바꿔 로그인을 요청한다
      MvcResult result = postJson(mockMvc, "/api/auth/login", c.rawBody());

      // Then 400 이고 code 는 INVALID_REQUEST 다 (500 도 401 도 아니다)
      soft.assertThat(result.getResponse().getStatus()).as(c.label() + " 상태").isEqualTo(400);
      if (result.getResponse().getStatus() == 400) {
        soft.assertThat(code(result)).as(c.label() + " code").isEqualTo("INVALID_REQUEST");
      }
      // And 응답 본문에 accessToken 이 없다
      soft.assertThat(result.getResponse().getContentAsString())
          .as(c.label() + " 본문")
          .doesNotContain("accessToken");
    }
    MvcResult emptyBody = postEmpty(mockMvc, "/api/auth/login");
    soft.assertThat(emptyBody.getResponse().getStatus()).as("본문 없음 상태").isEqualTo(400);
    soft.assertThat(emptyBody.getResponse().getContentAsString())
        .as("본문 없음 본문")
        .doesNotContain("accessToken");
    soft.assertAll();
  }

  private Case loginMutated(String field, String mutation) {
    Map<String, Object> body = new LinkedHashMap<>(loginBody(EMAIL, PASSWORD));
    switch (mutation) {
      case "missing" -> body.remove(field);
      case "null" -> body.put(field, null);
      case "empty" -> body.put(field, "");
      case "blank" -> body.put(field, "   ");
      default -> throw new IllegalArgumentException(mutation);
    }
    return new Case(field + " " + mutation, json(body));
  }

  // ---- SC-10 ----

  @Test
  void test_sc10_유효한_토큰으로_me를_조회하면_현재_사용자_정보를_받는다() throws Exception {
    // Given user@example.com 으로 가입·로그인해 accessToken 을 갖고 있다
    givenSignedUp(EMAIL, PASSWORD, NAME, "AUDIENCE");
    MvcResult loggedIn = login(mockMvc, EMAIL, PASSWORD);
    assertThat(loggedIn.getResponse().getStatus()).as("Given: 로그인 200").isEqualTo(200);
    String token = accessTokenOf(loggedIn);
    Object loginUser = bodyAsMap(loggedIn).get("user");
    List<String> authorizations = List.of("Bearer " + token, "bearer " + token, "BEARER " + token);
    SoftAssertions soft = new SoftAssertions();

    for (String authorization : authorizations) {
      // When 각 Authorization 값으로 GET /api/auth/me 를 호출한다
      MvcResult result = getWithAuthorization(mockMvc, "/api/auth/me", authorization);

      // Then 모든 행이 200 이다
      String label = "[" + authorization.substring(0, 7) + "...]";
      soft.assertThat(result.getResponse().getStatus()).as(label + " 상태").isEqualTo(200);
      if (result.getResponse().getStatus() == 200) {
        Map<String, Object> body = bodyAsMap(result);
        // And 본문은 {id, email, name, role} 이고 로그인 응답의 user 와 같다
        soft.assertThat(body.keySet())
            .as(label + " 키")
            .containsExactlyInAnyOrder("id", "email", "name", "role");
        soft.assertThat(body).as(label + " 본문").isEqualTo(loginUser);
        // And 비밀번호 관련 키가 없다
        soft.assertThat(hasPasswordKey(body)).as(label + " 비밀번호 키").isFalse();
      }
    }
    soft.assertAll();
  }

  // ---- SC-11 ----

  @Test
  void test_sc11_인증_정보가_없으면_me와_허용_목록_밖_경로는_401이다() throws Exception {
    // Given 가입된 사용자가 있다
    givenSignedUp(EMAIL, PASSWORD, NAME, "AUDIENCE");
    String token = givenLoggedInToken();
    record Row(String label, String path, String authorization) {}
    List<Row> rows =
        List.of(
            new Row("Authorization 없음", "/api/auth/me", null),
            new Row("Basic 스킴", "/api/auth/me", "Basic dXNlcjpwYXNz"),
            new Row("빈 토큰", "/api/auth/me", "Bearer "),
            new Row("BearerXYZ", "/api/auth/me", "BearerXYZ"),
            new Row("공백 없는 Bearer<토큰>", "/api/auth/me", "Bearer" + token),
            new Row("공백 두 개", "/api/auth/me", "Bearer  " + token),
            new Row("토큰 뒤 추가 값", "/api/auth/me", "Bearer " + token + " extra"),
            new Row("허용 목록 밖 경로", "/api/not-registered", null));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      // When 각 요청을 보낸다
      MvcResult result = getWithAuthorization(mockMvc, row.path(), row.authorization());

      // Then 401 이고 code 는 UNAUTHORIZED 다
      soft.assertThat(result.getResponse().getStatus()).as(row.label() + " 상태").isEqualTo(401);
      if (result.getResponse().getStatus() == 401) {
        soft.assertThat(code(result)).as(row.label() + " code").isEqualTo("UNAUTHORIZED");
        // And Content-Type 은 application/json 이고 본문은 {code, message} 이며 사용자 정보가 없다
        soft.assertThat(result.getResponse().getContentType())
            .as(row.label() + " Content-Type")
            .startsWith("application/json");
        soft.assertThat(bodyAsMap(result).keySet())
            .as(row.label() + " 키")
            .containsExactlyInAnyOrder("code", "message");
        soft.assertThat(result.getResponse().getContentAsString())
            .as(row.label() + " 본문")
            .doesNotContain(EMAIL);
      }
    }
    soft.assertAll();
  }

  // ---- SC-12 ----

  @Test
  void test_sc12_위조되거나_용도가_다른_토큰은_me에서_401이다() throws Exception {
    // Given 가입된 사용자가 있고 유효한 accessToken 이 하나 있다
    Map<String, Object> signedUp = givenSignedUp(EMAIL, PASSWORD, NAME, "AUDIENCE");
    String id = String.valueOf(signedUp.get("id"));
    String valid = givenLoggedInToken();
    Instant now = clock.instant();
    Instant issuedAt = now;
    Instant expiresAt = now.plus(1, ChronoUnit.HOURS);
    String otherSecret = "another-secret-0123456789abcdefghij";
    record Row(String label, String token) {}
    List<Row> rows =
        List.of(
            new Row("형식 깨짐 not-a-jwt", "not-a-jwt"),
            new Row("형식 깨짐 a.b.c", "a.b.c"),
            new Row("payload 변조", tamperRole(valid, "AUDIENCE", "ORGANIZER")),
            new Row(
                "다른 키로 서명",
                signedToken(otherSecret, id, EMAIL, "AUDIENCE", "access", issuedAt, expiresAt)),
            new Row(
                "alg none", unsignedToken(id, EMAIL, "AUDIENCE", "access", issuedAt, expiresAt)),
            new Row(
                "typ pending",
                signedToken(jwtSecret, id, EMAIL, "AUDIENCE", "pending", issuedAt, expiresAt)));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      // When 각 토큰으로 GET /api/auth/me 를 호출한다
      MvcResult result = getWithAuthorization(mockMvc, "/api/auth/me", "Bearer " + row.token());

      // Then 401 이고 code 는 UNAUTHORIZED 다 (500 이 아니다)
      soft.assertThat(result.getResponse().getStatus()).as(row.label() + " 상태").isEqualTo(401);
      if (result.getResponse().getStatus() == 401) {
        soft.assertThat(code(result)).as(row.label() + " code").isEqualTo("UNAUTHORIZED");
      }
      // And 사용자 정보가 응답에 없다
      soft.assertThat(result.getResponse().getContentAsString())
          .as(row.label() + " 본문")
          .doesNotContain(EMAIL);
    }
    soft.assertAll();
  }

  // ---- SC-13 ----

  @Test
  void test_sc13_토큰은_발급_24시간_뒤에_만료된다() throws Exception {
    // Given 시각 T 에 로그인해 accessToken 을 받았다
    clock.setInstant(FIXED_NOW);
    givenSignedUp(EMAIL, PASSWORD, NAME, "AUDIENCE");
    String token = givenLoggedInToken();
    record Row(String label, Instant now, int expected) {}
    List<Row> rows =
        List.of(
            new Row("T+24h-1s", FIXED_NOW.plus(24, ChronoUnit.HOURS).minusSeconds(1), 200),
            new Row("T+24h 정각", FIXED_NOW.plus(24, ChronoUnit.HOURS), 401),
            new Row("T+24h+1s", FIXED_NOW.plus(24, ChronoUnit.HOURS).plusSeconds(1), 401));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      // And 현재 시각이 표의 값이다
      clock.setInstant(row.now());

      // When GET /api/auth/me 를 호출한다
      MvcResult result = getWithAuthorization(mockMvc, "/api/auth/me", "Bearer " + token);

      // Then 각 행의 상태가 기대와 같고, 401 행의 code 는 UNAUTHORIZED 다
      soft.assertThat(result.getResponse().getStatus())
          .as(row.label() + " 상태")
          .isEqualTo(row.expected());
      if (row.expected() == 401 && result.getResponse().getStatus() == 401) {
        soft.assertThat(code(result)).as(row.label() + " code").isEqualTo("UNAUTHORIZED");
      }
    }
    soft.assertAll();
  }

  // ---- SC-14 ----

  @Test
  void test_sc14_사용자가_사라진_뒤에는_유효한_토큰도_401이다() throws Exception {
    // Given user@example.com 으로 가입·로그인해 유효한 accessToken 을 갖고 있다
    givenSignedUp(EMAIL, PASSWORD, NAME, "AUDIENCE");
    String token = givenLoggedInToken();
    // And 그 뒤 해당 사용자의 users 행이 삭제됐다
    userRepository.deleteAll();

    // When GET /api/auth/me 를 호출한다
    MvcResult result = getWithAuthorization(mockMvc, "/api/auth/me", "Bearer " + token);

    // Then 401 이고 code 는 UNAUTHORIZED 다 (404·500 이 아니다)
    assertThat(result.getResponse().getStatus()).isEqualTo(401);
    assertThat(code(result)).isEqualTo("UNAUTHORIZED");
    // And 삭제된 사용자의 정보가 응답에 없다
    assertThat(result.getResponse().getContentAsString())
        .doesNotContain(EMAIL)
        .doesNotContain(NAME);
  }

  // ---- SC-18 ----

  @Test
  void test_sc18_이메일_길이는_정규화_후_기준으로_검증된다() throws Exception {
    // Given users 가 비어 있다 (İ 는 소문자로 바꾸면 두 글자가 되는 문자다)
    record Row(String label, String email, int expected) {}
    List<Row> rows =
        List.of(
            new Row("İ 64개, 입력 255자 → 정규화 후 319자", emailWithLocal("İ".repeat(64), 255), 400),
            new Row("İ 1개, 입력 254자 → 정규화 후 255자", emailWithLocal("İ" + "a".repeat(63), 254), 201),
            new Row("İ 1개, 입력 255자 → 정규화 후 256자", emailWithLocal("İ" + "a".repeat(63), 255), 400));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      userRepository.deleteAll();

      // When 각 이메일로 가입을 요청한다
      MvcResult result = signUp(mockMvc, row.email(), PASSWORD, NAME, "AUDIENCE");

      // Then 각 행의 상태가 기대와 같다
      soft.assertThat(result.getResponse().getStatus())
          .as(row.label() + " 상태")
          .isEqualTo(row.expected());
      if (row.expected() == 400) {
        // And 400 행에서는 code 가 INVALID_REQUEST 이다 (EMAIL_ALREADY_EXISTS 나 500 이 아니다)
        if (result.getResponse().getStatus() == 400) {
          soft.assertThat(code(result)).as(row.label() + " code").isEqualTo("INVALID_REQUEST");
        }
        // And 400 행에서는 users 에 행이 생기지 않는다
        soft.assertThat(userRepository.count()).as(row.label() + " 행 수").isZero();
      }
    }
    soft.assertAll();
  }
}
