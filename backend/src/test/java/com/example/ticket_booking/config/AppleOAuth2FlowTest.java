package com.example.ticket_booking.config;

import static com.example.ticket_booking.support.AuthTestHelper.json;
import static com.example.ticket_booking.support.AuthTestHelper.payloadOf;
import static com.example.ticket_booking.support.PendingTokenTestHelper.queryOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.ticket_booking.repository.SocialAccountRepository;
import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.AppleTestKeys;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.FakeProviderServer;
import com.example.ticket_booking.support.MutableClock;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * TASK-012 시나리오 SC-02, SC-09, SC-10, SC-12.
 *
 * <p>인가 요청 → 콜백 → 토큰 교환 → id_token 검증 → 성공/실패 핸들러가 <b>실제 보안 필터 체인</b>을 탄다. 애플·카카오·네이버·구글 서버는 로컬 가짜
 * 서버({@link FakeProviderServer})로 대체한다 — 테스트 컨텍스트의 {@code ClientRegistrationRepository} 가 만들어질 때
 * 토큰·사용자 정보·JWKS 주소만 가짜 서버로 바꾼다(그 밖의 등록 내용은 구현이 정한 그대로). 브라우저의 쿠키 동작은 증명 대상이 아니다 — MockMvc 가 같은 세션을
 * 그대로 넘긴다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-012.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class AppleOAuth2FlowTest {

  private static final String BASE = "https://app.example.test";
  private static final String FAILURE_URL = BASE + "/login?error=social";
  private static final String PENDING_PREFIX = BASE + "/oauth2/redirect?pendingToken=";
  private static final FakeProviderServer FAKE = new FakeProviderServer();

  @DynamicPropertySource
  static void providerEnvironment(DynamicPropertyRegistry registry) {
    registry.add("FRONTEND_BASE_URL", () -> BASE);
    registry.add("APPLE_CLIENT_ID", () -> AppleTestKeys.CLIENT_ID);
    registry.add("APPLE_TEAM_ID", () -> AppleTestKeys.TEAM_ID);
    registry.add("APPLE_KEY_ID", () -> AppleTestKeys.KEY_ID);
    registry.add("APPLE_PRIVATE_KEY", AppleTestKeys::validPem);
    for (String provider : new String[] {"KAKAO", "NAVER", "GOOGLE"}) {
      registry.add(provider + "_CLIENT_ID", () -> "id-" + provider);
      registry.add(provider + "_CLIENT_SECRET", () -> "secret-" + provider);
    }
  }

  /** 등록된 제공자의 토큰·사용자 정보·JWKS 주소를 가짜 서버로 바꾼다. */
  @TestConfiguration
  static class FakeEndpoints {
    @Bean
    static BeanPostProcessor fakeProviderEndpoints() {
      return new BeanPostProcessor() {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
          if (!(bean instanceof InMemoryClientRegistrationRepository repository)) {
            return bean;
          }
          List<ClientRegistration> mapped = new ArrayList<>();
          for (ClientRegistration registration : repository) {
            mapped.add(FAKE.redirect(registration));
          }
          return new InMemoryClientRegistrationRepository(mapped);
        }
      };
    }
  }

  @AfterAll
  static void stopFake() {
    FAKE.stop();
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private MutableClock clock;
  @Autowired private UserRepository userRepository;
  @Autowired private SocialAccountRepository socialAccountRepository;

  @BeforeEach
  void reset() {
    socialAccountRepository.deleteAll();
    userRepository.deleteAll();
    FAKE.reset();
    clock.setInstant(Instant.parse("2026-10-27T00:00:00Z"));
  }

  // ---------- 헬퍼 ----------

  private record Started(MockHttpSession session, String state, String nonce) {}

  private record Counts(long users, long socialAccounts) {}

  private Counts counts() {
    return new Counts(userRepository.count(), socialAccountRepository.count());
  }

  /** GET /oauth2/authorization/<제공자> — 302 이어야 한다. 같은 세션과 발급된 state·nonce 를 돌려준다. */
  private Started start(String provider) throws Exception {
    MvcResult result = mockMvc.perform(get("/oauth2/authorization/" + provider)).andReturn();
    assertThat(result.getResponse().getStatus()).as(provider + " 인가 요청 상태").isEqualTo(302);
    Map<String, String> query = queryOf(result.getResponse().getRedirectedUrl());
    return new Started(
        (MockHttpSession) result.getRequest().getSession(), query.get("state"), query.get("nonce"));
  }

  /** 애플이 브라우저를 통해 보내는 form_post 콜백. */
  private String postCallback(Started started, Map<String, String> form) throws Exception {
    MockHttpServletRequestBuilder request =
        post("/login/oauth2/code/apple")
            .session(started.session())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED);
    form.forEach(request::param);
    MvcResult result = mockMvc.perform(request).andReturn();
    assertThat(result.getResponse().getStatus()).as("콜백 응답 상태").isEqualTo(302);
    return result.getResponse().getRedirectedUrl();
  }

  private static Map<String, String> form(String code, Started started, String user) {
    Map<String, String> form = new LinkedHashMap<>();
    form.put("code", code);
    form.put("state", started.state());
    if (user != null) {
      form.put("user", user);
    }
    return form;
  }

  private static Map<String, Object> idClaims(String nonce) {
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("iss", FakeProviderServer.ISSUER);
    claims.put("aud", AppleTestKeys.CLIENT_ID);
    claims.put("sub", "a-200");
    claims.put("email", "full@example.com");
    claims.put("email_verified", "true");
    claims.put("nonce", nonce);
    claims.put("iat", Instant.now().minusSeconds(60));
    claims.put("exp", Instant.now().plusSeconds(3600));
    return claims;
  }

  private static String tokenResponse(String idToken) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("access_token", "apple-access");
    body.put("token_type", "Bearer");
    body.put("expires_in", 3600);
    if (idToken != null) {
      body.put("id_token", idToken);
    }
    return json(body);
  }

  private static void respondValidToken(Started started) {
    FAKE.respondJson("/apple/token", tokenResponse(FAKE.idToken(idClaims(started.nonce()))));
  }

  private static List<FakeProviderServer.Recorded> appleTokenRequests() {
    return FAKE.requests("/apple/token");
  }

  // ---------- SC-02 ----------

  @Test
  void test_sc02_토큰_교환_요청에는_요청마다_새로_만든_client_secret_이_실린다() throws Exception {
    // Given 애플 등록이 있고 토큰 엔드포인트는 가짜 서버다 (요청을 기록한다)
    SoftAssertions soft = new SoftAssertions();
    String[] times = {"2026-10-27T00:00:00Z", "2026-10-27T00:10:00Z"};
    String[] codes = {"code-1", "code-2"};
    long[] iats = {1793059200L, 1793059800L};
    List<FakeProviderServer.Recorded> exchanges = new ArrayList<>();

    // When 00:00 에 code-1 로, 이어서 00:10 에 code-2 로 토큰 교환을 두 번 요청한다
    for (int i = 0; i < 2; i++) {
      clock.setInstant(Instant.parse(times[i]));
      FAKE.clearRequests();
      Started started = start("apple");
      respondValidToken(started);
      postCallback(started, form(codes[i], started, null));
      List<FakeProviderServer.Recorded> requests = appleTokenRequests();
      soft.assertThat(requests).as(times[i] + " — 토큰 엔드포인트 요청 수").hasSize(1);
      if (requests.size() == 1) {
        exchanges.add(requests.get(0));
      }
    }

    // Then 첫 요청은 POST 폼이고 grant_type·code·client_id 가 맞다
    if (exchanges.size() == 2) {
      FakeProviderServer.Recorded first = exchanges.get(0);
      FakeProviderServer.Recorded second = exchanges.get(1);
      soft.assertThat(first.method()).isEqualTo("POST");
      soft.assertThat(first.form().get("grant_type")).isEqualTo("authorization_code");
      soft.assertThat(first.form().get("code")).isEqualTo("code-1");
      soft.assertThat(first.form().get("client_id")).isEqualTo(AppleTestKeys.CLIENT_ID);
      // And 첫 요청의 client_secret 은 SC-01 규격의 JWT 다 (iat=1793059200)
      AppleTestKeys.assertClientSecret(soft, "첫 요청", first.form().get("client_secret"), iats[0]);
      // And 두 요청 모두 Authorization 헤더가 없다
      soft.assertThat(first.headers()).as("첫 요청 헤더").doesNotContainKey("authorization");
      soft.assertThat(second.headers()).as("두 번째 요청 헤더").doesNotContainKey("authorization");
      // And 두 번째 요청은 code-2 이고 client_secret 은 iat=1793059800 이다 (첫 번째와 다른 값)
      soft.assertThat(second.form().get("code")).isEqualTo("code-2");
      AppleTestKeys.assertClientSecret(
          soft, "두 번째 요청", second.form().get("client_secret"), iats[1]);
      soft.assertThat(second.form().get("client_secret"))
          .as("두 번째 client_secret 은 첫 번째와 다르다")
          .isNotEqualTo(first.form().get("client_secret"));
    }
    soft.assertAll();
  }

  // ---------- SC-09 ----------

  private record FailureRow(
      String label,
      boolean noExchange,
      Consumer<Started> fake,
      Function<Started, Map<String, String>> form) {}

  private static FailureRow row(
      String label,
      boolean noExchange,
      Consumer<Started> fake,
      Function<Started, Map<String, String>> form) {
    return new FailureRow(label, noExchange, fake, form);
  }

  private static Function<Started, Map<String, String>> normalForm() {
    return started -> form("code-x", started, null);
  }

  private static Consumer<Started> tokenRespondsWith(int status, String contentType, String body) {
    return started -> FAKE.respond("/apple/token", status, contentType, body);
  }

  private static Consumer<Started> idTokenWith(String claim, Object value) {
    return started -> {
      Map<String, Object> claims = idClaims(started.nonce());
      claims.put(claim, value);
      FAKE.respondJson("/apple/token", tokenResponse(FAKE.idToken(claims)));
    };
  }

  @Test
  void test_sc09_애플_인증이_실패하거나_id_token_이_검증을_통과하지_못하면_실패_화면으로_돌려보낸다() throws Exception {
    // Given 애플 등록이 있고 토큰 엔드포인트와 JWKS 는 가짜 서버다
    // And 사용자가 GET /oauth2/authorization/apple 로 시작해 애플로 보내졌다 (state·nonce 발급, 같은 세션)
    List<FailureRow> rows = new ArrayList<>();
    rows.add(
        row(
            "1 사용자가 취소했다",
            true,
            started -> {},
            started -> {
              Map<String, String> form = new LinkedHashMap<>();
              form.put("error", "user_cancelled_authorize");
              form.put("state", started.state());
              return form;
            }));
    rows.add(
        row(
            "2 state 가 발급한 값과 다르다",
            true,
            AppleOAuth2FlowTest::respondValidToken,
            started -> {
              Map<String, String> form = new LinkedHashMap<>();
              form.put("code", "code-x");
              form.put("state", "not-the-issued-state");
              return form;
            }));
    rows.add(
        row(
            "3 state 가 없다",
            true,
            AppleOAuth2FlowTest::respondValidToken,
            started -> {
              Map<String, String> form = new LinkedHashMap<>();
              form.put("code", "code-x");
              return form;
            }));
    rows.add(
        row("4 이미 쓴 state 를 다시 쓴다", true, AppleOAuth2FlowTest::respondValidToken, normalForm()));
    rows.add(
        row(
            "5 토큰 엔드포인트가 400 invalid_client",
            false,
            tokenRespondsWith(400, "application/json", "{\"error\":\"invalid_client\"}"),
            normalForm()));
    rows.add(
        row(
            "6 토큰 응답에 id_token 이 없다",
            false,
            started -> FAKE.respondJson("/apple/token", tokenResponse((String) null)),
            normalForm()));
    rows.add(row("7 iss 가 다르다", false, idTokenWith("iss", "https://evil.example"), normalForm()));
    rows.add(row("8 aud 가 다르다", false, idTokenWith("aud", "com.other.app"), normalForm()));
    rows.add(row("9 nonce 가 다르다", false, idTokenWith("nonce", "other-nonce"), normalForm()));
    rows.add(
        row(
            "10 exp 가 과거다",
            false,
            started -> {
              Map<String, Object> claims = idClaims(started.nonce());
              claims.put("iat", Instant.now().minusSeconds(7200));
              claims.put("exp", Instant.now().minusSeconds(3600));
              FAKE.respondJson("/apple/token", tokenResponse(FAKE.idToken(claims)));
            },
            normalForm()));
    rows.add(
        row(
            "11 JWKS 에 없는 키로 서명됐다",
            false,
            started ->
                FAKE.respondJson(
                    "/apple/token",
                    tokenResponse(FAKE.idTokenSignedByOtherKey(idClaims(started.nonce())))),
            normalForm()));
    rows.add(
        row(
            "12 code 가 없다",
            true,
            AppleOAuth2FlowTest::respondValidToken,
            started -> {
              Map<String, String> form = new LinkedHashMap<>();
              form.put("state", started.state());
              return form;
            }));
    rows.add(
        row(
            "13 code 가 빈 문자열이다",
            true,
            AppleOAuth2FlowTest::respondValidToken,
            started -> {
              Map<String, String> form = new LinkedHashMap<>();
              form.put("code", "");
              form.put("state", started.state());
              return form;
            }));
    rows.add(
        row(
            "14a 400 invalid_grant",
            false,
            tokenRespondsWith(400, "application/json", "{\"error\":\"invalid_grant\"}"),
            normalForm()));
    rows.add(
        row(
            "14b 400 invalid_request",
            false,
            tokenRespondsWith(400, "application/json", "{\"error\":\"invalid_request\"}"),
            normalForm()));
    rows.add(row("15 500 빈 본문", false, tokenRespondsWith(500, "text/plain", null), normalForm()));
    rows.add(
        row(
            "16 400 비 JSON 본문",
            false,
            tokenRespondsWith(400, "text/html", "<html>error</html>"),
            normalForm()));
    rows.add(
        row("17 200 빈 본문", false, tokenRespondsWith(200, "application/json", null), normalForm()));
    rows.add(
        row(
            "18 200 비 JSON 본문",
            false,
            tokenRespondsWith(200, "application/json", "not json"),
            normalForm()));
    rows.add(
        row(
            "19 200 JSON 이지만 access_token 없음",
            false,
            tokenRespondsWith(
                200, "application/json", "{\"token_type\":\"Bearer\",\"id_token\":\"x\"}"),
            normalForm()));
    SoftAssertions soft = new SoftAssertions();

    for (FailureRow row : rows) {
      FAKE.reset();
      Counts before = counts();
      Started started = start("apple");
      row.fake().accept(started);
      if (row.label().startsWith("4 ")) {
        // 한 번 성공한 콜백의 state 를 다시 쓴다
        postCallback(started, form("code-first", started, null));
        FAKE.clearRequests();
      }

      // When 애플이 브라우저를 통해 POST /login/oauth2/code/apple 로 표의 상황을 돌려준다
      String location;
      try {
        location = postCallback(started, row.form().apply(started));
      } catch (Exception | AssertionError exception) {
        soft.fail(row.label() + " — 실패 화면 리다이렉트가 아니라 예외/비정상 응답이 났다: " + exception);
        continue;
      }

      // Then 실패 화면으로 이동하고 쿼리에 token·pendingToken 이 없다
      soft.assertThat(location).as(row.label() + " — 이동 주소").isEqualTo(FAILURE_URL);
      soft.assertThat(queryOf(location)).as(row.label() + " — 쿼리").containsOnlyKeys("error");
      // And users·social_accounts 행 수가 그대로다
      soft.assertThat(counts()).as(row.label() + " — 행 수").isEqualTo(before);
      // And 토큰 엔드포인트로 요청이 나가지 않는 상황이다
      if (row.noExchange()) {
        soft.assertThat(appleTokenRequests()).as(row.label() + " — 토큰 엔드포인트 요청").isEmpty();
      }
    }
    soft.assertAll();
  }

  // ---------- SC-10 ----------

  @Test
  void test_sc10_인가_요청부터_form_post_콜백까지_이어지면_이름이_담긴_가입_대기로_끝난다() throws Exception {
    // Given 애플 등록이 있고 토큰 엔드포인트와 JWKS 는 가짜 서버다 (users·social_accounts 는 비어 있다)
    Counts before = counts();
    // And 사용자가 GET /oauth2/authorization/apple 로 시작해 애플로 보내졌다
    Started started = start("apple");
    // And 가짜 토큰 엔드포인트는 JWKS 의 키로 서명한 id_token 을 돌려준다
    respondValidToken(started);
    String user = json(Map.of("name", Map.of("firstName", "길동", "lastName", "홍")));

    // When 애플이 POST /login/oauth2/code/apple 로 code=code-9, state, user 를 보낸다
    String location = postCallback(started, form("code-9", started, user));

    // Then pendingToken 주소로 이동하고 쿼리에 token 은 없다
    SoftAssertions soft = new SoftAssertions();
    soft.assertThat(location).as("가입 대기 주소").startsWith(PENDING_PREFIX);
    soft.assertThat(queryOf(location)).as("쿼리 키").containsOnlyKeys("pendingToken");
    // And pendingToken 의 값이 맞다
    String pendingToken = queryOf(location).get("pendingToken");
    if (pendingToken != null) {
      Map<String, Object> payload = payloadOf(pendingToken);
      soft.assertThat(payload.get("email")).isEqualTo("full@example.com");
      soft.assertThat(payload.get("name")).isEqualTo("길동 홍");
      soft.assertThat(payload.get("provider")).isEqualTo("APPLE");
      soft.assertThat(payload.get("providerUserId")).isEqualTo("a-200");
      soft.assertThat(payload.get("emailVerified")).isEqualTo(true);
    }
    // And 토큰 엔드포인트가 받은 요청의 client_secret 은 SC-01 규격의 JWT 이고 code=code-9 였다
    List<FakeProviderServer.Recorded> requests = appleTokenRequests();
    soft.assertThat(requests).as("토큰 엔드포인트 요청 수").hasSize(1);
    if (requests.size() == 1) {
      soft.assertThat(requests.get(0).form().get("code")).isEqualTo("code-9");
      AppleTestKeys.assertClientSecret(
          soft, "토큰 교환", requests.get(0).form().get("client_secret"), 1793059200L);
    }
    // And users·social_accounts 행 수가 그대로다
    soft.assertThat(counts()).as("행 수").isEqualTo(before);
    soft.assertAll();
  }

  // ---------- SC-12 ----------

  @Test
  void test_sc12_애플을_추가해도_다른_제공자의_토큰_교환은_설정된_정적_client_secret_을_그대로_쓴다() throws Exception {
    // Given 카카오·네이버·구글이 id-<P>/secret-<P> 로 설정돼 있고 애플도 설정돼 있다
    // And 각 제공자의 토큰 엔드포인트와 사용자 정보 엔드포인트는 가짜 서버다
    String tokenJson = "{\"access_token\":\"at\",\"token_type\":\"Bearer\",\"expires_in\":3600}";
    FAKE.respondJson("/kakao/token", tokenJson);
    FAKE.respondJson("/naver/token", tokenJson);
    FAKE.respondJson("/google/token", tokenJson);
    FAKE.respondJson(
        "/kakao/userinfo",
        "{\"id\":123,\"kakao_account\":{\"email\":\"k@example.com\",\"is_email_verified\":true,"
            + "\"is_email_valid\":true,\"profile\":{\"nickname\":\"카카오\"}}}");
    FAKE.respondJson(
        "/naver/userinfo",
        "{\"resultcode\":\"00\",\"response\":{\"id\":\"n-1\",\"email\":\"n@example.com\",\"name\":\"네이버\"}}");
    FAKE.respondJson(
        "/google/userinfo",
        "{\"sub\":\"g-1\",\"email\":\"g@example.com\",\"email_verified\":true,\"name\":\"구글\"}");
    SoftAssertions soft = new SoftAssertions();

    for (String provider : new String[] {"kakao", "naver", "google"}) {
      FAKE.clearRequests();
      Started started = start(provider);

      // When 표의 제공자로 인가 코드 code-2 의 토큰 교환을 요청한다
      MvcResult result =
          mockMvc
              .perform(
                  get("/login/oauth2/code/" + provider)
                      .session(started.session())
                      .param("code", "code-2")
                      .param("state", started.state()))
              .andReturn();
      soft.assertThat(result.getResponse().getStatus()).as(provider + " — 콜백 상태").isEqualTo(302);

      // Then 클라이언트 인증 정보는 표의 전달 방식으로 표의 값 그대로다
      List<FakeProviderServer.Recorded> requests = FAKE.requests("/" + provider + "/token");
      soft.assertThat(requests).as(provider + " — 토큰 엔드포인트 요청 수").hasSize(1);
      if (requests.size() != 1) {
        continue;
      }
      FakeProviderServer.Recorded exchange = requests.get(0);
      String id = "id-" + provider.toUpperCase();
      String secret = "secret-" + provider.toUpperCase();
      if (provider.equals("google")) {
        String basic =
            "Basic "
                + Base64.getEncoder()
                    .encodeToString((id + ":" + secret).getBytes(StandardCharsets.UTF_8));
        soft.assertThat(exchange.headers().get("authorization"))
            .as("구글 — Authorization 헤더")
            .isEqualTo(basic);
        soft.assertThat(exchange.form())
            .as("구글 — 폼에 client_secret 없음")
            .doesNotContainKey("client_secret");
      } else {
        soft.assertThat(exchange.form().get("client_id"))
            .as(provider + " — client_id")
            .isEqualTo(id);
        soft.assertThat(exchange.form().get("client_secret"))
            .as(provider + " — client_secret")
            .isEqualTo(secret);
        soft.assertThat(exchange.headers())
            .as(provider + " — 헤더")
            .doesNotContainKey("authorization");
      }
    }
    soft.assertAll();
  }
}
