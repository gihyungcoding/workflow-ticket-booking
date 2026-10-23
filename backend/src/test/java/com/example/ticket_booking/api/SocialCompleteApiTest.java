package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.OBJECT_MAPPER;
import static com.example.ticket_booking.support.AuthTestHelper.accessTokenOf;
import static com.example.ticket_booking.support.AuthTestHelper.bodyAsMap;
import static com.example.ticket_booking.support.AuthTestHelper.code;
import static com.example.ticket_booking.support.AuthTestHelper.getWithAuthorization;
import static com.example.ticket_booking.support.AuthTestHelper.login;
import static com.example.ticket_booking.support.AuthTestHelper.postJson;
import static com.example.ticket_booking.support.AuthTestHelper.signUp;
import static com.example.ticket_booking.support.AuthTestHelper.signedToken;
import static com.example.ticket_booking.support.PendingTokenTestHelper.EMAIL;
import static com.example.ticket_booking.support.PendingTokenTestHelper.NAME;
import static com.example.ticket_booking.support.PendingTokenTestHelper.PENDING_SECONDS;
import static com.example.ticket_booking.support.PendingTokenTestHelper.PROVIDER;
import static com.example.ticket_booking.support.PendingTokenTestHelper.PROVIDER_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticket_booking.domain.SocialAccount;
import com.example.ticket_booking.domain.SocialProvider;
import com.example.ticket_booking.domain.User;
import com.example.ticket_booking.domain.UserRole;
import com.example.ticket_booking.repository.SocialAccountRepository;
import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import com.example.ticket_booking.support.PendingTokenTestHelper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * TASK-011 시나리오 SC-09 ~ SC-15, SC-17.
 *
 * <p>pendingToken 은 구현의 issuePendingToken 에 기대지 않고 Nimbus 로 직접 만든다(PendingTokenTestHelper) — 검증 로직이
 * 형식 깨짐 때문에 우연히 401 을 내는 가짜 통과를 막는다. 클래스 레벨 {@code @Transactional} 을 두지 않고 DB 확인은 리포지토리로 새로 읽는다. 표의
 * 각 행은 {@link SoftAssertions} 로 독립 단언한다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-011.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class SocialCompleteApiTest {

  private static final String COMPLETE = "/api/auth/social/complete";
  private static final Instant FIXED_NOW = Instant.parse("2026-10-23T00:00:00Z");
  private static final String PASSWORD = "password123";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private SocialAccountRepository socialAccountRepository;
  @Autowired private MutableClock clock;
  @Autowired private PasswordEncoder passwordEncoder;

  @Value("${JWT_SECRET}")
  private String secret;

  @BeforeEach
  void reset() {
    cleanUp();
    clock.setInstant(FIXED_NOW);
  }

  @AfterEach
  void cleanUp() {
    socialAccountRepository.deleteAll();
    userRepository.deleteAll();
  }

  // ---------- 헬퍼 ----------

  private record Counts(long users, long socialAccounts) {}

  private Counts counts() {
    return new Counts(userRepository.count(), socialAccountRepository.count());
  }

  private String validToken() {
    return PendingTokenTestHelper.valid(secret, FIXED_NOW);
  }

  private static String body(String token, Object role) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("pendingToken", token);
    map.put("role", role);
    return OBJECT_MAPPER.writeValueAsString(map);
  }

  private MvcResult complete(String rawBody) throws Exception {
    return postJson(mockMvc, COMPLETE, rawBody);
  }

  private User saveEmailUser(String email) {
    return userRepository.save(
        new User(email, passwordEncoder.encode(PASSWORD), "홍길동", UserRole.AUDIENCE));
  }

  // ---------- SC-09 ----------

  @Test
  void test_sc09_역할을_고르면_가입이_끝나고_로그인된다() throws Exception {
    // Given users 에 new@example.com 이 없고 유효한 pendingToken 이 있다
    Map<String, String> rows = new LinkedHashMap<>();
    rows.put("AUDIENCE", "AUDIENCE");
    rows.put("ORGANIZER", "ORGANIZER");
    SoftAssertions soft = new SoftAssertions();

    for (Map.Entry<String, String> row : rows.entrySet()) {
      cleanUp();

      // When POST /api/auth/social/complete 를 pendingToken 과 role 로 호출한다
      MvcResult result = complete(body(validToken(), row.getKey()));

      // Then 200 이고 응답은 tokenType·expiresInSeconds·user 를 담는다
      soft.assertThat(result.getResponse().getStatus()).as(row.getKey() + " — 상태").isEqualTo(200);
      Map<String, Object> response = bodyAsMap(result);
      soft.assertThat(response.get("tokenType")).isEqualTo("Bearer");
      soft.assertThat(String.valueOf(response.get("expiresInSeconds"))).isEqualTo("86400");
      Map<?, ?> user = (Map<?, ?>) response.get("user");
      soft.assertThat(user).as(row.getKey() + " — user").isNotNull();
      if (user != null) {
        soft.assertThat(user.get("id")).isNotNull();
        soft.assertThat(user.get("email")).isEqualTo(EMAIL);
        soft.assertThat(user.get("name")).isEqualTo(NAME);
        soft.assertThat(user.get("role")).isEqualTo(row.getValue());
      }
      // And users 에 행이 1건(비밀번호 해시 없음), social_accounts 에 행이 1건 생긴다
      List<User> users = userRepository.findAll();
      soft.assertThat(users).as(row.getKey() + " — users 행 수").hasSize(1);
      List<SocialAccount> accounts = socialAccountRepository.findAll();
      soft.assertThat(accounts).as(row.getKey() + " — social_accounts 행 수").hasSize(1);
      if (users.size() == 1 && accounts.size() == 1) {
        User saved = users.get(0);
        soft.assertThat(saved.getEmail()).isEqualTo(EMAIL);
        soft.assertThat(saved.getName()).isEqualTo(NAME);
        soft.assertThat(saved.getRole()).isEqualTo(UserRole.valueOf(row.getValue()));
        soft.assertThat(saved.getPasswordHash()).as(row.getKey() + " — 비밀번호 해시").isNull();
        soft.assertThat(accounts.get(0).getProvider()).isEqualTo(SocialProvider.valueOf(PROVIDER));
        soft.assertThat(accounts.get(0).getProviderUserId()).isEqualTo(PROVIDER_USER_ID);
        soft.assertThat(accounts.get(0).getUserId()).isEqualTo(saved.getId());
      }
      // And 응답의 accessToken 으로 GET /api/auth/me 를 호출하면 같은 사용자가 나온다
      Object accessToken = response.get("accessToken");
      soft.assertThat(accessToken).as(row.getKey() + " — accessToken").isNotNull();
      if (accessToken != null) {
        MvcResult me = getWithAuthorization(mockMvc, "/api/auth/me", "Bearer " + accessToken);
        soft.assertThat(me.getResponse().getStatus()).as(row.getKey() + " — /me 상태").isEqualTo(200);
        soft.assertThat(bodyAsMap(me).get("email")).isEqualTo(EMAIL);
      }
    }
    soft.assertAll();
  }

  // ---------- SC-10 ----------

  private record TokenRow(String label, Supplier<String> token, Instant now, boolean accepted) {}

  @Test
  void test_sc10_만료되거나_위조된_pendingToken_은_401_이고_아무것도_만들지_않는다() throws Exception {
    // Given 요청의 pendingToken 이 표의 상태다 (role 은 올바른 AUDIENCE)
    Instant exp = FIXED_NOW.plusSeconds(PENDING_SECONDS);
    Supplier<String> valid = this::validToken;
    String otherSecret = "another-secret-another-secret-0123456789";
    List<TokenRow> rows = new ArrayList<>();
    rows.add(new TokenRow("만료 1초 전", valid, exp.minusSeconds(1), true));
    rows.add(new TokenRow("만료 정각", valid, exp, false));
    rows.add(new TokenRow("만료 후(+1s)", valid, exp.plusSeconds(1), false));
    rows.add(
        new TokenRow(
            "다른 키로 서명",
            () -> PendingTokenTestHelper.valid(otherSecret, FIXED_NOW),
            FIXED_NOW,
            false));
    rows.add(
        new TokenRow(
            "서명 부분 제거",
            () -> PendingTokenTestHelper.withoutSignature(validToken()),
            FIXED_NOW,
            false));
    rows.add(
        new TokenRow(
            "alg=none", () -> PendingTokenTestHelper.unsigned(FIXED_NOW, exp), FIXED_NOW, false));
    rows.add(
        new TokenRow(
            "페이로드 변조(이메일)",
            () -> PendingTokenTestHelper.tamperEmail(validToken(), "evil@example.com"),
            FIXED_NOW,
            false));
    rows.add(new TokenRow("형식 깨짐(점 하나)", () -> "a.b", FIXED_NOW, false));
    rows.add(new TokenRow("형식 깨짐(임의 문자열)", () -> "garbage", FIXED_NOW, false));
    rows.add(new TokenRow("형식 깨짐(점 셋)", () -> "a.b.c.d", FIXED_NOW, false));
    rows.add(
        new TokenRow(
            "accessToken(typ=access)",
            () -> signedToken(secret, "1", EMAIL, "AUDIENCE", "access", FIXED_NOW, exp),
            FIXED_NOW,
            false));
    rows.add(
        new TokenRow(
            "typ 없음",
            () ->
                PendingTokenTestHelper.signed(
                    secret, EMAIL, NAME, PROVIDER, PROVIDER_USER_ID, null, FIXED_NOW, exp),
            FIXED_NOW,
            false));
    String[] missing = {"email", "name", "provider", "providerUserId"};
    for (String field : missing) {
      rows.add(
          new TokenRow(
              field + " 클레임 누락",
              () ->
                  PendingTokenTestHelper.signed(
                      secret,
                      field.equals("email") ? null : EMAIL,
                      field.equals("name") ? null : NAME,
                      field.equals("provider") ? null : PROVIDER,
                      field.equals("providerUserId") ? null : PROVIDER_USER_ID,
                      "pending",
                      FIXED_NOW,
                      exp),
              FIXED_NOW,
              false));
    }
    for (String provider : new String[] {"FOO", ""}) {
      rows.add(
          new TokenRow(
              "provider='" + provider + "'",
              () ->
                  PendingTokenTestHelper.signed(
                      secret, EMAIL, NAME, provider, PROVIDER_USER_ID, "pending", FIXED_NOW, exp),
              FIXED_NOW,
              false));
    }
    rows.add(
        new TokenRow(
            "providerUserId 빈 문자열",
            () ->
                PendingTokenTestHelper.signed(
                    secret, EMAIL, NAME, PROVIDER, "", "pending", FIXED_NOW, exp),
            FIXED_NOW,
            false));
    rows.add(new TokenRow("수 KB 길이의 임의 문자열", () -> "a".repeat(8192), FIXED_NOW, false));
    Set<String> messages = new HashSet<>();
    SoftAssertions soft = new SoftAssertions();

    for (TokenRow row : rows) {
      cleanUp();
      Counts before = counts();
      clock.setInstant(row.now());

      // When POST /api/auth/social/complete 를 호출한다
      MvcResult result = complete(body(row.token().get(), "AUDIENCE"));

      if (row.accepted()) {
        // Then 통과 행은 200 으로 가입이 끝난다
        soft.assertThat(result.getResponse().getStatus()).as(row.label()).isEqualTo(200);
      } else {
        // Then 401 PENDING_SIGNUP_EXPIRED 이고 행 수가 그대로다
        soft.assertThat(result.getResponse().getStatus()).as(row.label() + " — 상태").isEqualTo(401);
        soft.assertThat(code(result))
            .as(row.label() + " — code")
            .isEqualTo("PENDING_SIGNUP_EXPIRED");
        soft.assertThat(counts()).as(row.label() + " — 행 수").isEqualTo(before);
        messages.add(String.valueOf(bodyAsMap(result).get("message")));
      }
    }
    // And 모든 실패 행의 message 가 같다(만료와 위조를 구분해 알리지 않는다)
    soft.assertThat(messages).as("실패 행의 message 종류").hasSize(1);
    soft.assertAll();
  }

  // ---------- SC-11 ----------

  @Test
  void test_sc11_가입_완료_요청의_형식이_틀리면_400_이다() throws Exception {
    // Given 유효한 pendingToken 이 있고 users·social_accounts 의 현재 행 수를 기록했다
    String token = validToken();
    Counts before = counts();
    Map<String, String> rows = new LinkedHashMap<>();
    rows.put("role 누락", "{\"pendingToken\":\"" + token + "\"}");
    rows.put("role null", body(token, null));
    rows.put("role 정의 밖(ADMIN)", body(token, "ADMIN"));
    rows.put("role 소문자", body(token, "audience"));
    rows.put("role 숫자", body(token, 1));
    rows.put("pendingToken 누락", "{\"role\":\"AUDIENCE\"}");
    rows.put("pendingToken null", body(null, "AUDIENCE"));
    rows.put("pendingToken 빈 문자열", body("", "AUDIENCE"));
    rows.put("pendingToken 공백만", body("   ", "AUDIENCE"));
    rows.put("pendingToken 숫자", "{\"pendingToken\":123,\"role\":\"AUDIENCE\"}");
    rows.put("pendingToken 객체", "{\"pendingToken\":{},\"role\":\"AUDIENCE\"}");
    rows.put("pendingToken 배열", "{\"pendingToken\":[],\"role\":\"AUDIENCE\"}");
    rows.put("pendingToken 불리언", "{\"pendingToken\":true,\"role\":\"AUDIENCE\"}");
    rows.put("본문이 비어 있음", "");
    rows.put("JSON 이 아님", "not json");
    SoftAssertions soft = new SoftAssertions();

    for (Map.Entry<String, String> row : rows.entrySet()) {
      // When POST /api/auth/social/complete 를 해당 본문으로 호출한다
      MvcResult result = complete(row.getValue());

      // Then 400 INVALID_REQUEST 이고 행 수가 그대로다
      soft.assertThat(result.getResponse().getStatus()).as(row.getKey() + " — 상태").isEqualTo(400);
      soft.assertThat(code(result)).as(row.getKey() + " — code").isEqualTo("INVALID_REQUEST");
      soft.assertThat(counts()).as(row.getKey() + " — 행 수").isEqualTo(before);
    }
    soft.assertAll();
  }

  // ---------- SC-12 ----------

  @Test
  void test_sc12_가입_완료_시점에_이미_있는_이메일_연결은_409_이고_아무것도_만들지_않는다() throws Exception {
    SoftAssertions soft = new SoftAssertions();

    // 상태 1 — pendingToken 의 이메일이 users 에 이미 있다 (이메일 가입자)
    cleanUp();
    saveEmailUser(EMAIL);
    Counts before = counts();
    MvcResult emailTaken = complete(body(validToken(), "AUDIENCE"));
    soft.assertThat(emailTaken.getResponse().getStatus()).as("이메일 중복 — 상태").isEqualTo(409);
    soft.assertThat(code(emailTaken)).as("이메일 중복 — code").isEqualTo("EMAIL_ALREADY_EXISTS");
    soft.assertThat(counts()).as("이메일 중복 — 행 수").isEqualTo(before);

    // 상태 2 — 같은 pendingToken 으로 이미 가입을 끝냈다 (두 번째 호출)
    cleanUp();
    String token = validToken();
    MvcResult first = complete(body(token, "AUDIENCE"));
    soft.assertThat(first.getResponse().getStatus()).as("첫 호출 — 상태").isEqualTo(200);
    before = counts();
    MvcResult second = complete(body(token, "AUDIENCE"));
    soft.assertThat(second.getResponse().getStatus()).as("두 번째 호출 — 상태").isEqualTo(409);
    soft.assertThat(code(second)).as("두 번째 호출 — code").isEqualTo("EMAIL_ALREADY_EXISTS");
    soft.assertThat(counts()).as("두 번째 호출 — 행 수").isEqualTo(before);

    // 상태 3 — (provider, providerUserId) 가 이미 다른 이메일의 사용자에게 연결돼 있다
    cleanUp();
    User other = saveEmailUser("other@example.com");
    socialAccountRepository.save(
        new SocialAccount(other.getId(), SocialProvider.valueOf(PROVIDER), PROVIDER_USER_ID));
    before = counts();
    MvcResult linked = complete(body(validToken(), "AUDIENCE"));
    soft.assertThat(linked.getResponse().getStatus()).as("이미 연결 — 상태").isEqualTo(409);
    soft.assertThat(code(linked)).as("이미 연결 — code").isEqualTo("SOCIAL_ACCOUNT_ALREADY_LINKED");
    soft.assertThat(counts()).as("이미 연결 — 행 수").isEqualTo(before);
    soft.assertAll();
  }

  // ---------- SC-13 ----------

  @Test
  void test_sc13_같은_pendingToken_으로_가입_완료가_동시에_와도_사용자는_하나만_생긴다() throws Exception {
    // Given users 에 new@example.com 이 없고 유효한 pendingToken 이 있다
    String token = validToken();
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    // When 같은 토큰으로 두 요청을 같은 시점에 시작시켜 동시에 처리한다
    List<Future<MvcResult>> futures = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      Callable<MvcResult> call =
          () -> {
            ready.countDown();
            start.await();
            return complete(body(token, "AUDIENCE"));
          };
      futures.add(executor.submit(call));
    }
    ready.await();
    start.countDown();
    List<MvcResult> results = new ArrayList<>();
    for (Future<MvcResult> future : futures) {
      results.add(future.get());
    }
    executor.shutdown();

    // Then 한 요청은 200, 다른 요청은 409(EMAIL_ALREADY_EXISTS 또는 SOCIAL_ACCOUNT_ALREADY_LINKED), 500 은 없다
    SoftAssertions soft = new SoftAssertions();
    List<Integer> statuses =
        results.stream().map(r -> r.getResponse().getStatus()).sorted().toList();
    soft.assertThat(statuses).as("두 응답 상태").containsExactly(200, 409);
    for (MvcResult result : results) {
      if (result.getResponse().getStatus() == 409) {
        soft.assertThat(code(result)).isIn("EMAIL_ALREADY_EXISTS", "SOCIAL_ACCOUNT_ALREADY_LINKED");
      }
    }
    // And users 에 new@example.com 은 정확히 1건, social_accounts 에 (GOOGLE, g-100) 은 정확히 1건이다
    soft.assertThat(
            userRepository.findAll().stream().filter(u -> EMAIL.equals(u.getEmail())).count())
        .as("users 행 수")
        .isEqualTo(1);
    soft.assertThat(
            socialAccountRepository.findAll().stream()
                .filter(
                    a ->
                        SocialProvider.valueOf(PROVIDER) == a.getProvider()
                            && PROVIDER_USER_ID.equals(a.getProviderUserId()))
                .count())
        .as("social_accounts 행 수")
        .isEqualTo(1);
    soft.assertAll();
  }

  // ---------- SC-14 ----------

  @Test
  void test_sc14_pendingToken_은_일반_API_의_인증_토큰으로_쓸_수_없다() throws Exception {
    // Given 유효한 pendingToken 이 있다
    String valid = validToken();
    String expired =
        PendingTokenTestHelper.valid(secret, FIXED_NOW.minusSeconds(PENDING_SECONDS + 10));
    SoftAssertions soft = new SoftAssertions();

    // When·Then GET /api/auth/me (유효한 pendingToken) → 401 UNAUTHORIZED
    MvcResult meValid = getWithAuthorization(mockMvc, "/api/auth/me", "Bearer " + valid);
    soft.assertThat(meValid.getResponse().getStatus())
        .as("me/유효한 pendingToken — 상태")
        .isEqualTo(401);
    soft.assertThat(code(meValid)).as("me/유효한 pendingToken — code").isEqualTo("UNAUTHORIZED");

    // 같은 pendingToken 이 실제 사용자의 id 를 sub 로 갖고 있어도 typ 때문에 거부된다 — typ 검증만이 막는 경우를 따로 본다
    // (사용자가 없으면 /me 가 사용자 조회 실패로 401 을 내 typ 검증이 지워져도 통과해 버린다)
    User holder = saveEmailUser("holder@example.com");
    String withSubject =
        signedToken(
            secret,
            String.valueOf(holder.getId()),
            "holder@example.com",
            "AUDIENCE",
            "pending",
            FIXED_NOW,
            FIXED_NOW.plusSeconds(PENDING_SECONDS));
    MvcResult meWithSubject =
        getWithAuthorization(mockMvc, "/api/auth/me", "Bearer " + withSubject);
    soft.assertThat(meWithSubject.getResponse().getStatus())
        .as("me/sub 를 가진 pendingToken — 상태")
        .isEqualTo(401);
    soft.assertThat(code(meWithSubject))
        .as("me/sub 를 가진 pendingToken — code")
        .isEqualTo("UNAUTHORIZED");

    // GET /api/auth/me (만료된 pendingToken) → 401 UNAUTHORIZED
    MvcResult meExpired = getWithAuthorization(mockMvc, "/api/auth/me", "Bearer " + expired);
    soft.assertThat(meExpired.getResponse().getStatus())
        .as("me/만료된 pendingToken — 상태")
        .isEqualTo(401);
    soft.assertThat(code(meExpired)).as("me/만료된 pendingToken — code").isEqualTo("UNAUTHORIZED");

    // GET /api/performances (유효한 pendingToken) → 토큰 없이 부르는 것과 같은 결과
    MvcResult withToken = getWithAuthorization(mockMvc, "/api/performances", "Bearer " + valid);
    MvcResult withoutToken = getWithAuthorization(mockMvc, "/api/performances", null);
    soft.assertThat(withoutToken.getResponse().getStatus()).isEqualTo(200);
    soft.assertThat(withToken.getResponse().getStatus())
        .as("공연 목록/유효한 pendingToken — 상태")
        .isEqualTo(withoutToken.getResponse().getStatus());
    soft.assertAll();
  }

  // ---------- SC-15 ----------

  @Test
  void test_sc15_비밀번호가_없는_소셜_전용_계정으로_이메일_로그인하면_401_이다() throws Exception {
    // Given 소셜 전용 사용자(new@example.com, 비밀번호 해시 없음)가 있다
    userRepository.save(new User(EMAIL, null, NAME, UserRole.AUDIENCE));
    // And 비교용으로 이메일 가입 사용자의 틀린 비밀번호 로그인과 존재하지 않는 이메일 로그인의 응답을 받아 둔다
    saveEmailUser("e@example.com");
    MvcResult wrongPassword = login(mockMvc, "e@example.com", "wrong-password-1");
    MvcResult unknownEmail = login(mockMvc, "nobody@example.com", PASSWORD);
    Map<String, Object> wrongBody = bodyAsMap(wrongPassword);
    Map<String, String> rows = new LinkedHashMap<>();
    rows.put("password123", PASSWORD);
    rows.put("72바이트 초과(한글 25자)", "가".repeat(25));
    SoftAssertions soft = new SoftAssertions();
    soft.assertThat(wrongPassword.getResponse().getStatus()).isEqualTo(401);
    soft.assertThat(unknownEmail.getResponse().getStatus()).isEqualTo(401);

    for (Map.Entry<String, String> row : rows.entrySet()) {
      // When POST /api/auth/login 을 new@example.com 과 표의 비밀번호로 호출한다
      MvcResult result = login(mockMvc, EMAIL, row.getValue());

      // Then 401 INVALID_CREDENTIALS 이고 500 이 아니다
      soft.assertThat(result.getResponse().getStatus()).as(row.getKey() + " — 상태").isEqualTo(401);
      soft.assertThat(code(result)).as(row.getKey() + " — code").isEqualTo("INVALID_CREDENTIALS");
      // And 응답의 code·message 는 비교용 두 응답과 같다
      Map<String, Object> resultBody = bodyAsMap(result);
      soft.assertThat(resultBody.get("message"))
          .as(row.getKey() + " — 틀린 비밀번호 응답과 message")
          .isEqualTo(wrongBody.get("message"));
      soft.assertThat(resultBody.get("message"))
          .as(row.getKey() + " — 없는 이메일 응답과 message")
          .isEqualTo(bodyAsMap(unknownEmail).get("message"));
      soft.assertThat(code(unknownEmail)).isEqualTo(code(result));
    }
    soft.assertAll();
  }

  // ---------- SC-17 ----------

  @Test
  void test_sc17_기존_이메일_인증과_공연_API_는_그대로_동작한다() throws Exception {
    // Given 소셜 로그인이 도입된 서버이고 제공자 설정은 없다
    // And 사용자 U(user@example.com)가 이메일로 가입돼 있고 로그인으로 받은 accessToken 이 있다
    MvcResult signedUp = signUp(mockMvc, "user@example.com", PASSWORD, "홍길동", "AUDIENCE");
    assertThat(signedUp.getResponse().getStatus()).as("Given 가입").isEqualTo(201);
    String accessToken = accessTokenOf(login(mockMvc, "user@example.com", PASSWORD));
    assertThat(accessToken).as("Given 로그인").isNotBlank();
    SoftAssertions soft = new SoftAssertions();

    // When·Then 표의 요청을 보낸다
    MvcResult newSignUp = signUp(mockMvc, "fresh@example.com", PASSWORD, "새사용자", "ORGANIZER");
    soft.assertThat(newSignUp.getResponse().getStatus()).as("POST /api/auth/signup").isEqualTo(201);

    MvcResult loggedIn = login(mockMvc, "user@example.com", PASSWORD);
    soft.assertThat(loggedIn.getResponse().getStatus()).as("POST /api/auth/login").isEqualTo(200);
    soft.assertThat(bodyAsMap(loggedIn).get("accessToken")).isNotNull();

    MvcResult me = getWithAuthorization(mockMvc, "/api/auth/me", "Bearer " + accessToken);
    soft.assertThat(me.getResponse().getStatus()).as("GET /api/auth/me").isEqualTo(200);
    soft.assertThat(bodyAsMap(me).get("email")).isEqualTo("user@example.com");

    MvcResult performances = getWithAuthorization(mockMvc, "/api/performances", null);
    soft.assertThat(performances.getResponse().getStatus())
        .as("GET /api/performances")
        .isEqualTo(200);
    soft.assertAll();
  }
}
