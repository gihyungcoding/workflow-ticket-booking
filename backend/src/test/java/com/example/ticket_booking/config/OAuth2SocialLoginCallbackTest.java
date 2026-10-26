package com.example.ticket_booking.config;

import static com.example.ticket_booking.support.AuthTestHelper.OBJECT_MAPPER;
import static com.example.ticket_booking.support.AuthTestHelper.code;
import static com.example.ticket_booking.support.AuthTestHelper.login;
import static com.example.ticket_booking.support.AuthTestHelper.payloadOf;
import static com.example.ticket_booking.support.AuthTestHelper.postJson;
import static com.example.ticket_booking.support.PendingTokenTestHelper.queryOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.example.ticket_booking.domain.SocialAccount;
import com.example.ticket_booking.domain.SocialProvider;
import com.example.ticket_booking.domain.User;
import com.example.ticket_booking.domain.UserRole;
import com.example.ticket_booking.repository.SocialAccountRepository;
import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.service.AuthService;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.FakeOAuth2User;
import com.example.ticket_booking.support.MutableClock;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * TASK-011 시나리오 SC-01 ~ SC-08, SC-20.
 *
 * <p>제공자 서버는 두지 않는다 — 제공자가 돌려준 사용자 정보(attributes)를 가짜 OAuth2 인증 정보로 넣어 성공·실패 핸들러를 실제로 실행하고, 리다이렉트
 * 주소와 DB 행을 본다. 클래스 레벨 {@code @Transactional} 을 두지 않는다 — DB 확인은 리포지토리로 새로 읽는다(불변·반영 단언이 1차 캐시에 가려지지
 * 않게). 표의 각 행은 {@link SoftAssertions} 로 독립 단언한다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-011.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class OAuth2SocialLoginCallbackTest {

  private static final String BASE = "https://app.example.test";
  private static final String PENDING_PREFIX = BASE + "/oauth2/redirect?pendingToken=";
  private static final String TOKEN_PREFIX = BASE + "/oauth2/redirect?token=";
  private static final String FAILURE_URL = BASE + "/login?error=social";
  private static final Instant FIXED_NOW = Instant.parse("2026-10-23T00:00:00Z");
  private static final String PASSWORD = "password123";

  /** attributes 에서 해당 키를 아예 넣지 않는다는 표시. */
  private static final Object ABSENT = new Object();

  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;
  @Autowired private SocialAccountRepository socialAccountRepository;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private MutableClock clock;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private MockMvc mockMvc;

  private OAuth2LoginSuccessHandler successHandler;
  private OAuth2LoginFailureHandler failureHandler;

  @BeforeEach
  void reset() {
    cleanUp();
    clock.setInstant(FIXED_NOW);
    successHandler = new OAuth2LoginSuccessHandler(authService, BASE);
    failureHandler = new OAuth2LoginFailureHandler(BASE);
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

  private User saveEmailUser(String email, UserRole role, String name) {
    return userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), name, role));
  }

  /** 제공자가 검증한 이메일로 소셜 가입을 끝낸 사용자 — 비밀번호 해시가 없고 email_verified=true (자동 연결 대상). */
  private User saveVerifiedSocialUser(String email, UserRole role, String name) {
    return userRepository.save(new User(email, null, name, role, true));
  }

  private static String emailOf(int length) {
    return "a".repeat(length - "@example.com".length()) + "@example.com";
  }

  private String callback(String registrationId, Map<String, Object> attributes) throws Exception {
    return callback(registrationId, attributes, new MockHttpServletRequest());
  }

  private String callback(
      String registrationId, Map<String, Object> attributes, MockHttpServletRequest request)
      throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    OAuth2AuthenticationToken authentication =
        new OAuth2AuthenticationToken(new FakeOAuth2User(attributes), List.of(), registrationId);
    successHandler.onAuthenticationSuccess(request, response, authentication);
    assertThat(response.getStatus()).as("리다이렉트 상태").isEqualTo(302);
    return response.getRedirectedUrl();
  }

  private String failureCallback(
      OAuth2AuthenticationException exception, HttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    failureHandler.onAuthenticationFailure(request, response, exception);
    assertThat(response.getStatus()).as("리다이렉트 상태").isEqualTo(302);
    return response.getRedirectedUrl();
  }

  private static Map<String, Object> mapOf(Object... keyValues) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      if (keyValues[i + 1] != ABSENT) {
        map.put((String) keyValues[i], keyValues[i + 1]);
      }
    }
    return map;
  }

  private static Map<String, Object> google(
      Object sub, Object email, Object verified, Object name) {
    return mapOf("sub", sub, "email", email, "email_verified", verified, "name", name);
  }

  private static Map<String, Object> googleNew() {
    return google("g-new", "new@example.com", true, "신규");
  }

  private static Map<String, Object> kakao(
      Object id, Object email, Object verified, Object valid, Object nickname) {
    Map<String, Object> profile = mapOf("nickname", nickname);
    Map<String, Object> account =
        mapOf(
            "email",
            email,
            "is_email_verified",
            verified,
            "is_email_valid",
            valid,
            "profile",
            profile);
    return mapOf("id", id, "kakao_account", account);
  }

  private static Map<String, Object> kakaoNew() {
    return kakao(777, "new@example.com", true, true, "신규");
  }

  private static Map<String, Object> naver(Object id, Object email, Object name) {
    return mapOf("resultcode", "00", "response", mapOf("id", id, "email", email, "name", name));
  }

  private static Map<String, Object> naverNew() {
    return naver("n-new", "new@example.com", "신규");
  }

  private void assertPending(SoftAssertions soft, String label, String location) {
    soft.assertThat(location).as(label + " — 가입 대기 주소").startsWith(PENDING_PREFIX);
    soft.assertThat(queryOf(location)).as(label + " — 쿼리 키").containsOnlyKeys("pendingToken");
  }

  private void assertLoggedIn(SoftAssertions soft, String label, String location, Long userId) {
    soft.assertThat(location).as(label + " — 로그인 주소").startsWith(TOKEN_PREFIX);
    soft.assertThat(queryOf(location)).as(label + " — 쿼리 키").containsOnlyKeys("token");
    String token = queryOf(location).get("token");
    soft.assertThat(token).as(label + " — accessToken").isNotBlank();
    if (token != null && !token.isBlank()) {
      soft.assertThat(jwtTokenProvider.verifyAccessToken(token))
          .as(label + " — 토큰의 사용자")
          .hasValue(userId);
    }
  }

  // ---------- SC-01 ----------

  @Test
  void test_sc01_처음_로그인하는_소셜_사용자는_가입_대기가_된다() throws Exception {
    // Given users 에 new@example.com 이 없고 social_accounts 가 비어 있다
    // And 구글이 {sub:g-100, email:new@example.com, email_verified:true, name:신규} 를 돌려줬다
    Counts before = counts();

    // When 구글 콜백을 처리한다
    String location = callback("google", google("g-100", "new@example.com", true, "신규"));

    // Then pendingToken 주소로 이동하고 쿼리에 token 은 없다
    SoftAssertions soft = new SoftAssertions();
    assertPending(soft, "SC-01", location);
    // And users·social_accounts 행 수가 그대로다
    soft.assertThat(counts()).as("행 수").isEqualTo(before);
    // And pendingToken 클레임
    String pendingToken = queryOf(location).get("pendingToken");
    if (pendingToken != null) {
      Map<String, Object> payload = payloadOf(pendingToken);
      soft.assertThat(payload.get("email")).isEqualTo("new@example.com");
      soft.assertThat(payload.get("name")).isEqualTo("신규");
      soft.assertThat(payload.get("provider")).isEqualTo("GOOGLE");
      soft.assertThat(payload.get("providerUserId")).isEqualTo("g-100");
      soft.assertThat(payload.get("typ")).isEqualTo("pending");
      soft.assertThat(payload.get("emailVerified")).as("emailVerified 클레임").isEqualTo(true);
      soft.assertThat(((Number) payload.get("exp")).longValue())
          .as("만료는 발급 시각 + 5분")
          .isEqualTo(FIXED_NOW.plusSeconds(300).getEpochSecond());
    }
    soft.assertAll();
  }

  // ---------- SC-02 ----------

  private record ProviderRow(
      String label,
      String registrationId,
      Map<String, Object> attributes,
      SocialProvider provider,
      String providerUserId) {}

  @Test
  void test_sc02_이메일이_일치하고_검증됐으면_기존_계정에_연결하고_바로_로그인한다() throws Exception {
    // Given 제공자가 검증한 이메일로 소셜 가입을 끝낸 사용자 A(hong@example.com, ORGANIZER, 홍길동, email_verified=true)가
    // 있다
    User a = saveVerifiedSocialUser("hong@example.com", UserRole.ORGANIZER, "홍길동");
    String hashBefore = a.getPasswordHash();
    List<ProviderRow> rows =
        List.of(
            new ProviderRow(
                "카카오",
                "kakao",
                kakao(777, "hong@example.com", true, true, "길동"),
                SocialProvider.KAKAO,
                "777"),
            new ProviderRow(
                "구글",
                "google",
                google("g-1", "hong@example.com", true, "길동"),
                SocialProvider.GOOGLE,
                "g-1"),
            new ProviderRow(
                "구글(대문자·앞뒤 공백)",
                "google",
                google("g-2", "  Hong@Example.COM  ", true, "길동"),
                SocialProvider.GOOGLE,
                "g-2"));
    SoftAssertions soft = new SoftAssertions();

    for (ProviderRow row : rows) {
      socialAccountRepository.deleteAll();

      // When 해당 제공자 콜백을 처리한다
      String location = callback(row.registrationId(), row.attributes());

      // Then 로그인 주소로 이동하고 토큰의 사용자는 A 다
      assertLoggedIn(soft, row.label(), location, a.getId());
      String token = queryOf(location).get("token");
      if (token != null) {
        // And 그 토큰으로 GET /api/auth/me 를 호출하면 A 의 정보(ORGANIZER)가 나온다
        MvcResult me =
            mockMvc
                .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andReturn();
        soft.assertThat(me.getResponse().getStatus()).as(row.label() + " — /me 상태").isEqualTo(200);
        Map<?, ?> body = OBJECT_MAPPER.readValue(me.getResponse().getContentAsString(), Map.class);
        soft.assertThat(body.get("role")).as(row.label() + " — /me 역할").isEqualTo("ORGANIZER");
        soft.assertThat(body.get("email"))
            .as(row.label() + " — /me 이메일")
            .isEqualTo("hong@example.com");
      }
      // And users 행 수와 A 의 정보는 그대로다
      soft.assertThat(userRepository.count()).as(row.label() + " — users 행 수").isEqualTo(1);
      User reread = userRepository.findById(a.getId()).orElseThrow();
      soft.assertThat(reread.getEmail()).isEqualTo("hong@example.com");
      soft.assertThat(reread.getName()).isEqualTo("홍길동");
      soft.assertThat(reread.getRole()).isEqualTo(UserRole.ORGANIZER);
      soft.assertThat(reread.getPasswordHash()).isEqualTo(hashBefore);
      soft.assertThat(reread.isEmailVerified()).as(row.label() + " — email_verified").isTrue();
      // And social_accounts 에 (provider, providerUserId, A) 가 정확히 1건 생긴다
      List<SocialAccount> accounts = socialAccountRepository.findAll();
      soft.assertThat(accounts).as(row.label() + " — social_accounts 행 수").hasSize(1);
      if (accounts.size() == 1) {
        soft.assertThat(accounts.get(0).getProvider()).isEqualTo(row.provider());
        soft.assertThat(accounts.get(0).getProviderUserId()).isEqualTo(row.providerUserId());
        soft.assertThat(accounts.get(0).getUserId()).isEqualTo(a.getId());
      }
    }
    soft.assertAll();
  }

  // ---------- SC-03 ----------

  @Test
  void test_sc03_이메일이_같아도_검증되지_않았으면_연결하지_않고_가입_대기가_된다() throws Exception {
    // Given 소셜 가입을 끝낸 검증된 사용자 A(hong@example.com, email_verified=true)가 있고 social_accounts 는 비어 있다
    // (제공자 플래그가 연결을 막는 유일한 이유가 되도록 A 는 연결 대상이다)
    User a = saveVerifiedSocialUser("hong@example.com", UserRole.AUDIENCE, "홍길동");
    String hongEmail = "hong@example.com";
    record Row(String label, String registrationId, Map<String, Object> attributes) {}
    List<Row> rows =
        List.of(
            new Row("구글 false", "google", google("g-1", hongEmail, false, "길동")),
            new Row("구글 누락", "google", google("g-1", hongEmail, ABSENT, "길동")),
            new Row("구글 문자열 true", "google", google("g-1", hongEmail, "true", "길동")),
            new Row("구글 문자열 TRUE", "google", google("g-1", hongEmail, "TRUE", "길동")),
            new Row("구글 숫자 1", "google", google("g-1", hongEmail, 1, "길동")),
            new Row("구글 null", "google", google("g-1", hongEmail, null, "길동")),
            new Row("구글 배열 [true]", "google", google("g-1", hongEmail, List.of(true), "길동")),
            new Row("구글 객체 {v:true}", "google", google("g-1", hongEmail, Map.of("v", true), "길동")),
            new Row("카카오 verified false", "kakao", kakao(777, hongEmail, false, true, "길동")),
            new Row("카카오 verified 누락", "kakao", kakao(777, hongEmail, ABSENT, true, "길동")),
            new Row("카카오 verified 문자열", "kakao", kakao(777, hongEmail, "true", true, "길동")),
            new Row("카카오 verified 숫자 1", "kakao", kakao(777, hongEmail, 1, true, "길동")),
            new Row("카카오 verified null", "kakao", kakao(777, hongEmail, null, true, "길동")),
            new Row("카카오 valid 문자열", "kakao", kakao(777, hongEmail, true, "true", "길동")),
            new Row("카카오 valid 숫자 1", "kakao", kakao(777, hongEmail, true, 1, "길동")),
            new Row("카카오 valid null", "kakao", kakao(777, hongEmail, true, null, "길동")),
            new Row("카카오 valid false", "kakao", kakao(777, hongEmail, true, false, "길동")),
            new Row("카카오 valid 누락", "kakao", kakao(777, hongEmail, true, ABSENT, "길동")),
            new Row("네이버(플래그 없음)", "naver", naver("n-1", hongEmail, "길동")));
    Counts before = counts();
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      // When 해당 제공자 콜백을 처리한다
      String location = callback(row.registrationId(), row.attributes());

      // Then 가입 대기로 이동하고 쿼리에 token 은 없다
      assertPending(soft, row.label(), location);
      // And users·social_accounts 행 수가 그대로이고 A 의 정보는 바뀌지 않는다
      soft.assertThat(counts()).as(row.label() + " — 행 수").isEqualTo(before);
      soft.assertThat(userRepository.findById(a.getId()).orElseThrow().getName())
          .as(row.label() + " — A 의 이름")
          .isEqualTo("홍길동");
      // And pendingToken 의 email 은 hong@example.com 이다
      String pendingToken = queryOf(location).get("pendingToken");
      if (pendingToken != null) {
        soft.assertThat(payloadOf(pendingToken).get("email"))
            .as(row.label() + " — pendingToken email")
            .isEqualTo(hongEmail);
      }
    }
    soft.assertAll();
  }

  // ---------- SC-04 ----------

  @Test
  void test_sc04_이미_연결된_제공자_계정은_이메일이_바뀌거나_미검증이어도_같은_사용자로_로그인한다() throws Exception {
    // Given 사용자 B(b@example.com)가 있고 social_accounts 에 (GOOGLE, g-9, B) 가 있다
    User b = saveEmailUser("b@example.com", UserRole.AUDIENCE, "비");
    socialAccountRepository.save(new SocialAccount(b.getId(), SocialProvider.GOOGLE, "g-9"));
    Counts before = counts();

    // And 구글이 이메일이 다르고 검증도 되지 않은 사용자 정보를 돌려줬다
    // When 구글 콜백을 처리한다
    String location = callback("google", google("g-9", "other@example.com", false, "바뀐이름"));

    // Then 로그인 주소로 이동하고 토큰의 사용자는 B 다
    SoftAssertions soft = new SoftAssertions();
    assertLoggedIn(soft, "SC-04", location, b.getId());
    // And social_accounts 는 여전히 1건이고 users 행 수·B 의 이메일·이름은 그대로다
    soft.assertThat(counts()).as("행 수").isEqualTo(before);
    User reread = userRepository.findById(b.getId()).orElseThrow();
    soft.assertThat(reread.getEmail()).isEqualTo("b@example.com");
    soft.assertThat(reread.getName()).isEqualTo("비");
    List<SocialAccount> accounts = socialAccountRepository.findAll();
    soft.assertThat(accounts).hasSize(1);
    if (accounts.size() == 1) {
      soft.assertThat(accounts.get(0).getProvider()).isEqualTo(SocialProvider.GOOGLE);
      soft.assertThat(accounts.get(0).getProviderUserId()).isEqualTo("g-9");
      soft.assertThat(accounts.get(0).getUserId()).isEqualTo(b.getId());
    }
    soft.assertAll();
  }

  // ---------- SC-05 ----------

  @Test
  void test_sc05_같은_제공자_계정의_콜백이_동시에_와도_연결_행은_하나다() throws Exception {
    // Given 이메일로 가입한 사용자 A 가 있고 social_accounts 는 비어 있다
    User a = saveEmailUser("hong@example.com", UserRole.AUDIENCE, "홍길동");
    Map<String, Object> attributes = google("g-5", "hong@example.com", true, "길동");
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    // When 같은 제공자 계정의 콜백 두 건을 같은 시점에 시작시켜 동시에 처리한다
    List<Future<String>> futures = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      futures.add(
          executor.submit(
              () -> {
                ready.countDown();
                start.await();
                return callback("google", attributes);
              }));
    }
    ready.await();
    start.countDown();
    List<String> locations = new ArrayList<>();
    for (Future<String> future : futures) {
      locations.add(future.get());
    }
    executor.shutdown();

    // Then 두 응답 모두 로그인 주소이고 두 토큰의 사용자는 모두 A 다
    SoftAssertions soft = new SoftAssertions();
    for (int i = 0; i < locations.size(); i++) {
      assertLoggedIn(soft, "SC-05 응답 " + i, locations.get(i), a.getId());
    }
    // And social_accounts 에는 (GOOGLE, g-5, A) 가 정확히 1건이다
    List<SocialAccount> accounts = socialAccountRepository.findAll();
    soft.assertThat(accounts).as("social_accounts 행 수").hasSize(1);
    if (accounts.size() == 1) {
      soft.assertThat(accounts.get(0).getProviderUserId()).isEqualTo("g-5");
      soft.assertThat(accounts.get(0).getUserId()).isEqualTo(a.getId());
    }
    soft.assertAll();
  }

  // ---------- SC-06 ----------

  @Test
  void test_sc06_제공자마다_다른_응답_구조에서_같은_필드를_뽑는다() throws Exception {
    // Given users 에 해당 이메일이 없고 social_accounts 가 비어 있다
    record Row(
        String label,
        String registrationId,
        Map<String, Object> attributes,
        String provider,
        String providerUserId,
        String email,
        String name) {}
    List<Row> rows =
        List.of(
            new Row(
                "카카오(중첩, 숫자 id)",
                "kakao",
                kakao(123456, "k@example.com", true, true, "카카오닉"),
                "KAKAO",
                "123456",
                "k@example.com",
                "카카오닉"),
            new Row(
                "네이버(response 래퍼)",
                "naver",
                naver("n-abc", "n@example.com", "네이버이름"),
                "NAVER",
                "n-abc",
                "n@example.com",
                "네이버이름"),
            new Row(
                "구글(평면)",
                "google",
                google("g-abc", "g@example.com", true, "구글이름"),
                "GOOGLE",
                "g-abc",
                "g@example.com",
                "구글이름"),
            new Row(
                "구글(이메일 대문자·앞뒤 공백)",
                "google",
                google("g-abd", "  G@Example.COM ", true, "구글이름"),
                "GOOGLE",
                "g-abd",
                "g@example.com",
                "구글이름"));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      // When 해당 제공자 콜백을 처리한다
      String location = callback(row.registrationId(), row.attributes());

      // Then 가입 대기로 이동하고 pendingToken 의 값이 표와 같다
      assertPending(soft, row.label(), location);
      String pendingToken = queryOf(location).get("pendingToken");
      if (pendingToken != null) {
        Map<String, Object> payload = payloadOf(pendingToken);
        soft.assertThat(payload.get("provider"))
            .as(row.label() + " provider")
            .isEqualTo(row.provider());
        soft.assertThat(payload.get("providerUserId"))
            .as(row.label() + " providerUserId")
            .isEqualTo(row.providerUserId());
        soft.assertThat(payload.get("email")).as(row.label() + " email").isEqualTo(row.email());
        soft.assertThat(payload.get("name")).as(row.label() + " name").isEqualTo(row.name());
      }
    }
    soft.assertAll();
  }

  // ---------- SC-07 ----------

  private record AttributeRow(
      String label, String registrationId, Map<String, Object> attributes, boolean accepted) {}

  private static void fail(
      List<AttributeRow> rows, String label, String registrationId, Map<String, Object> attrs) {
    rows.add(new AttributeRow(label, registrationId, attrs, false));
  }

  private static void pass(
      List<AttributeRow> rows, String label, String registrationId, Map<String, Object> attrs) {
    rows.add(new AttributeRow(label, registrationId, attrs, true));
  }

  private static List<AttributeRow> invalidAttributeRows() {
    List<AttributeRow> rows = new ArrayList<>();
    String emoji = "\uD83D\uDE00";
    Object[] notString = {123, true, Map.of("a", 1), List.of("a")};

    // email — 구글 기준
    Object[] badEmails = {
      ABSENT,
      null,
      "",
      "   ",
      emailOf(256),
      "a\u0000b@example.com",
      "\u0000a@example.com",
      "a\ud800b@example.com",
      "a\tb@example.com",
      "a\u0001b@example.com",
      "\u0130".repeat(124) + "@example.com",
      "no-at-sign",
      "a@b@example.com",
      "@example.com",
      "a@",
      "a b@example.com",
      "a\u00a0b@example.com",
      "a\u200bb@example.com",
      "a\u202eb@example.com"
    };
    for (Object email : badEmails) {
      fail(rows, "email=" + describe(email), "google", google("g-new", email, true, "신규"));
    }
    for (Object email : notString) {
      fail(rows, "email 비문자열=" + describe(email), "google", google("g-new", email, true, "신규"));
    }
    pass(rows, "email 255자", "google", google("g-new", emailOf(255), true, "신규"));

    // name
    Object[] badNames = {
      ABSENT,
      null,
      "",
      "   ",
      "가".repeat(101),
      "홍\u0000길동",
      "홍\ud800길동",
      "홍\t길동",
      "홍\u0001길동",
      emoji.repeat(51)
    };
    for (Object name : badNames) {
      fail(
          rows, "name=" + describe(name), "google", google("g-new", "new@example.com", true, name));
    }
    for (Object name : notString) {
      fail(
          rows,
          "name 비문자열=" + describe(name),
          "google",
          google("g-new", "new@example.com", true, name));
    }
    pass(rows, "name 100자", "google", google("g-new", "new@example.com", true, "가".repeat(100)));
    pass(
        rows, "name 이모지 50개", "google", google("g-new", "new@example.com", true, emoji.repeat(50)));

    // providerUserId — 구글 sub
    Object[] badIds = {
      ABSENT, null, "", "   ", "g".repeat(256), "g\u0000x", "g\ud800x", "g\tx", "g\u0001x"
    };
    for (Object id : badIds) {
      fail(rows, "sub=" + describe(id), "google", google(id, "new@example.com", true, "신규"));
    }
    for (Object id : notString) {
      fail(rows, "sub 비문자열=" + describe(id), "google", google(id, "new@example.com", true, "신규"));
    }
    fail(rows, "네이버 response.id 숫자", "naver", naver(123, "new@example.com", "신규"));
    fail(rows, "네이버 response.id 불리언", "naver", naver(true, "new@example.com", "신규"));
    pass(rows, "sub 255자", "google", google("g".repeat(255), "new@example.com", true, "신규"));

    // providerUserId — 카카오 id (정수)
    Object[] badKakaoIds = {
      1.5, "123", true, Map.of("a", 1), List.of(1), -1, 0, new BigInteger("99999999999999999999")
    };
    for (Object id : badKakaoIds) {
      fail(rows, "카카오 id=" + describe(id), "kakao", kakao(id, "new@example.com", true, true, "신규"));
    }
    pass(rows, "카카오 id=1", "kakao", kakao(1, "new@example.com", true, true, "신규"));
    pass(
        rows,
        "카카오 id=Long.MAX",
        "kakao",
        kakao(Long.MAX_VALUE, "new@example.com", true, true, "신규"));

    // 구조
    Map<String, Object> noAccount = kakaoNew();
    noAccount.remove("kakao_account");
    fail(rows, "카카오 kakao_account 없음", "kakao", noAccount);
    Map<String, Object> noResponse = naverNew();
    noResponse.remove("response");
    fail(rows, "네이버 response 없음", "naver", noResponse);
    fail(rows, "카카오 profile 없음", "kakao", kakao(777, "new@example.com", true, true, ABSENT));
    Map<String, Object> noProfile =
        mapOf(
            "id",
            777,
            "kakao_account",
            mapOf("email", "new@example.com", "is_email_verified", true, "is_email_valid", true));
    fail(rows, "카카오 profile 키 없음", "kakao", noProfile);
    for (Object notObject : new Object[] {"text", List.of("a")}) {
      fail(
          rows,
          "카카오 kakao_account=" + describe(notObject),
          "kakao",
          mapOf("id", 777, "kakao_account", notObject));
      fail(
          rows,
          "카카오 profile=" + describe(notObject),
          "kakao",
          mapOf(
              "id",
              777,
              "kakao_account",
              mapOf(
                  "email",
                  "new@example.com",
                  "is_email_verified",
                  true,
                  "is_email_valid",
                  true,
                  "profile",
                  notObject)));
      fail(rows, "네이버 response=" + describe(notObject), "naver", mapOf("response", notObject));
    }
    return rows;
  }

  private static String describe(Object value) {
    if (value == ABSENT) return "누락";
    if (value == null) return "null";
    String text = String.valueOf(value);
    String shown = text.length() > 24 ? text.substring(0, 24) + "…(" + text.length() + ")" : text;
    return value.getClass().getSimpleName()
        + ":"
        + shown.replace("\u0000", "\\0").replace("\t", "\\t");
  }

  @Test
  void test_sc07_필수_값이_없거나_이상한_제공자_응답은_아무것도_만들지_않고_실패로_돌려보낸다() throws Exception {
    // Given users·social_accounts 의 현재 행 수를 기록했다
    Counts before = counts();
    SoftAssertions soft = new SoftAssertions();

    for (AttributeRow row : invalidAttributeRows()) {
      // When 해당 제공자 콜백을 처리한다
      String location = callback(row.registrationId(), row.attributes());

      if (row.accepted()) {
        // Then 통과 행은 가입 대기로 이동한다
        soft.assertThat(location).as(row.label()).startsWith(PENDING_PREFIX);
      } else {
        // Then 실패 주소로 이동하고 쿼리에 token·pendingToken 이 없다
        soft.assertThat(location).as(row.label()).isEqualTo(FAILURE_URL);
        soft.assertThat(queryOf(location))
            .as(row.label() + " — 쿼리")
            .doesNotContainKeys("token", "pendingToken");
      }
      // And users·social_accounts 행 수가 그대로다
      soft.assertThat(counts()).as(row.label() + " — 행 수").isEqualTo(before);
    }
    soft.assertAll();
  }

  // ---------- SC-08 ----------

  @Test
  void test_sc08_제공자_인증이_실패하거나_취소되면_실패_화면으로_돌려보낸다() throws Exception {
    // Given users·social_accounts 의 현재 행 수를 기록했다
    Counts before = counts();
    Map<String, String> rows = new LinkedHashMap<>();
    rows.put("사용자가 취소", "access_denied");
    rows.put("토큰 교환 실패", "invalid_token_response");
    rows.put("사용자 정보 조회 실패", "invalid_user_info_response");
    SoftAssertions soft = new SoftAssertions();

    for (Map.Entry<String, String> row : rows.entrySet()) {
      // When 콜백 실패를 처리한다
      String location =
          failureCallback(
              new OAuth2AuthenticationException(new OAuth2Error(row.getValue())),
              new MockHttpServletRequest());

      // Then 실패 주소로 이동하고 쿼리에 token·pendingToken 이 없다
      soft.assertThat(location).as(row.getKey()).isEqualTo(FAILURE_URL);
      soft.assertThat(queryOf(location))
          .as(row.getKey() + " — 쿼리")
          .doesNotContainKeys("token", "pendingToken");
      // And users·social_accounts 행 수가 그대로다
      soft.assertThat(counts()).as(row.getKey() + " — 행 수").isEqualTo(before);
    }
    soft.assertAll();
  }

  // ---------- SC-20 ----------

  @Test
  void test_sc20_콜백_요청을_조작해도_리다이렉트_주소는_설정값에서만_나온다() throws Exception {
    // Given 상황 표의 하나가 만들어져 있다 — 조작 × 상황 전부를 본다
    Map<String, Consumer<MockHttpServletRequest>> manipulations = new LinkedHashMap<>();
    manipulations.put("쿼리 returnUrl", r -> r.addParameter("returnUrl", "https://evil.example"));
    manipulations.put("쿼리 redirect", r -> r.addParameter("redirect", "//evil.example/x"));
    manipulations.put("쿼리 next", r -> r.addParameter("next", "https://evil.example"));
    manipulations.put(
        "쿼리 redirect_uri", r -> r.addParameter("redirect_uri", "https://evil.example"));
    manipulations.put("헤더 Referer", r -> r.addHeader("Referer", "https://evil.example/"));
    manipulations.put("헤더 Origin", r -> r.addHeader("Origin", "https://evil.example"));
    manipulations.put(
        "헤더 Host",
        r -> {
          r.addHeader("Host", "evil.example");
          r.setServerName("evil.example");
        });
    manipulations.put("헤더 X-Forwarded-Host", r -> r.addHeader("X-Forwarded-Host", "evil.example"));
    manipulations.put("헤더 X-Forwarded-Proto", r -> r.addHeader("X-Forwarded-Proto", "http"));
    SoftAssertions soft = new SoftAssertions();

    for (Map.Entry<String, Consumer<MockHttpServletRequest>> manipulation :
        manipulations.entrySet()) {
      // 상황 1 — 신규 사용자
      cleanUp();
      MockHttpServletRequest request = new MockHttpServletRequest();
      manipulation.getValue().accept(request);
      // When 조작이 붙은 콜백을 처리한다
      String pending = callback("google", googleNew(), request);
      soft.assertThat(pending).as("신규 / " + manipulation.getKey()).startsWith(PENDING_PREFIX);
      soft.assertThat(pending).as("신규 / " + manipulation.getKey()).doesNotContain("evil.example");

      // 상황 2 — 재로그인
      cleanUp();
      User b = saveEmailUser("b@example.com", UserRole.AUDIENCE, "비");
      socialAccountRepository.save(new SocialAccount(b.getId(), SocialProvider.GOOGLE, "g-new"));
      request = new MockHttpServletRequest();
      manipulation.getValue().accept(request);
      String relogin = callback("google", googleNew(), request);
      soft.assertThat(relogin).as("재로그인 / " + manipulation.getKey()).startsWith(TOKEN_PREFIX);
      soft.assertThat(relogin).as("재로그인 / " + manipulation.getKey()).doesNotContain("evil.example");

      // 상황 3 — 제공자 인증 취소
      request = new MockHttpServletRequest();
      manipulation.getValue().accept(request);
      String cancelled =
          failureCallback(
              new OAuth2AuthenticationException(new OAuth2Error("access_denied")), request);
      soft.assertThat(cancelled).as("취소 / " + manipulation.getKey()).startsWith(FAILURE_URL);
      soft.assertThat(cancelled).as("취소 / " + manipulation.getKey()).doesNotContain("evil.example");
    }
    soft.assertAll();
  }

  // ---------- SC-21 ----------

  private enum Origin {
    EMAIL_SIGNUP,
    KAKAO_UNVERIFIED,
    NAVER
  }

  @Test
  void test_sc21_이메일_소유가_확인되지_않은_계정은_자동_연결_대상이_아니다() throws Exception {
    SoftAssertions soft = new SoftAssertions();

    for (Origin origin : Origin.values()) {
      // Given 표의 방식으로 hong@example.com 계정 A 가 만들어져 있고 social_accounts 는 비어 있다
      cleanUp();
      Map<String, Object> ownerCallback = null;
      String ownerRegistration = null;
      switch (origin) {
        case EMAIL_SIGNUP ->
            authService.signUp("hong@example.com", PASSWORD, PASSWORD, "홍길동", UserRole.AUDIENCE);
        case KAKAO_UNVERIFIED -> {
          ownerRegistration = "kakao";
          ownerCallback = kakao(777, "hong@example.com", false, true, "길동");
        }
        case NAVER -> {
          ownerRegistration = "naver";
          ownerCallback = naver("n-1", "hong@example.com", "길동");
        }
      }
      if (ownerCallback != null) {
        String pending = queryOf(callback(ownerRegistration, ownerCallback)).get("pendingToken");
        authService.completeSocialSignUp(pending, UserRole.AUDIENCE);
      }
      User a = userRepository.findByEmail("hong@example.com").orElseThrow();
      Counts before = counts();

      // And 구글이 검증된 이메일로 사용자 정보를 돌려줬다
      // When 구글 콜백을 처리한다
      String location = callback("google", google("g-1", "hong@example.com", true, "길동"));

      // Then 연결하지 않고 가입 대기로 이동하며 쿼리에 token 은 없다
      assertPending(soft, origin.toString(), location);
      // And users·social_accounts 행 수가 그대로이고 A 의 정보는 바뀌지 않는다
      soft.assertThat(counts()).as(origin + " — 행 수").isEqualTo(before);
      User reread = userRepository.findById(a.getId()).orElseThrow();
      soft.assertThat(reread.getName()).as(origin + " — A 의 이름").isEqualTo(a.getName());
      soft.assertThat(reread.getPasswordHash())
          .as(origin + " — A 의 비밀번호 해시")
          .isEqualTo(a.getPasswordHash());
      // And 그 pendingToken 으로 가입 완료를 호출하면 409 EMAIL_ALREADY_EXISTS 이고 행 수가 그대로다
      String pendingToken = queryOf(location).get("pendingToken");
      if (pendingToken != null) {
        MvcResult complete =
            postJson(
                mockMvc,
                "/api/auth/social/complete",
                OBJECT_MAPPER.writeValueAsString(
                    Map.of("pendingToken", pendingToken, "role", "AUDIENCE")));
        soft.assertThat(complete.getResponse().getStatus())
            .as(origin + " — 가입 완료 상태")
            .isEqualTo(409);
        soft.assertThat(code(complete))
            .as(origin + " — 가입 완료 code")
            .isEqualTo("EMAIL_ALREADY_EXISTS");
        soft.assertThat(counts()).as(origin + " — 가입 완료 뒤 행 수").isEqualTo(before);
      }
      // And A 를 만든 쪽의 로그인은 계속 된다
      switch (origin) {
        case EMAIL_SIGNUP -> {
          MvcResult emailLogin = login(mockMvc, "hong@example.com", PASSWORD);
          soft.assertThat(emailLogin.getResponse().getStatus())
              .as(origin + " — A 의 로그인")
              .isEqualTo(200);
        }
        case KAKAO_UNVERIFIED ->
            assertLoggedIn(
                soft,
                origin + " — A 의 로그인",
                callback("kakao", kakao(777, "hong@example.com", false, true, "길동")),
                a.getId());
        case NAVER ->
            assertLoggedIn(
                soft,
                origin + " — A 의 로그인",
                callback("naver", naver("n-1", "hong@example.com", "길동")),
                a.getId());
      }
      // And A 의 email_verified 는 false 다
      soft.assertThat(userRepository.findById(a.getId()).orElseThrow().isEmailVerified())
          .as(origin + " — email_verified")
          .isFalse();
    }
    soft.assertAll();
  }
}
