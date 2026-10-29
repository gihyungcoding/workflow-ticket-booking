package com.example.ticket_booking.config;

import static com.example.ticket_booking.support.AuthTestHelper.OBJECT_MAPPER;
import static com.example.ticket_booking.support.AuthTestHelper.json;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * TASK-012 시나리오 SC-03 ~ SC-08.
 *
 * <p>애플 서버는 두지 않는다 — id_token 검증을 통과한 뒤의 클레임(attributes)을 가짜 OAuth2 인증 정보로 넣어 성공 핸들러를 직접 실행하고,
 * 리다이렉트 주소와 DB 행을 본다(TASK-011 {@code OAuth2SocialLoginCallbackTest} 와 같은 방식). 클래스 레벨
 * {@code @Transactional} 을 두지 않는다 — DB 확인은 리포지토리로 새로 읽는다(불변·반영 단언이 1차 캐시에 가려지지 않게). 필터 체인을 실제로 타는
 * 확인은 {@link AppleOAuth2FlowTest}.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-012.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class AppleLoginCallbackTest {

  private static final String BASE = "https://app.example.test";
  private static final String PENDING_PREFIX = BASE + "/oauth2/redirect?pendingToken=";
  private static final String TOKEN_PREFIX = BASE + "/oauth2/redirect?token=";
  private static final String FAILURE_URL = BASE + "/login?error=social";
  private static final Instant FIXED_NOW = Instant.parse("2026-10-27T00:00:00Z");
  private static final String PASSWORD = "password123";
  private static final String NO_NAME = "Apple 사용자";

  /** 맵·JSON 에서 해당 키를 아예 넣지 않는다는 표시. */
  private static final Object ABSENT = new Object();

  private static final String NUL = String.valueOf((char) 0);
  private static final String LONE_SURROGATE = String.valueOf((char) 0xD800);

  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;
  @Autowired private SocialAccountRepository socialAccountRepository;
  @Autowired private MutableClock clock;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private MockMvc mockMvc;

  private OAuth2LoginSuccessHandler successHandler;

  @BeforeEach
  void reset() {
    cleanUp();
    clock.setInstant(FIXED_NOW);
    successHandler = new OAuth2LoginSuccessHandler(authService, BASE);
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

  /** 제공자가 검증한 이메일로 소셜 가입을 끝낸 사용자 — 비밀번호 해시가 없고 email_verified=true (자동 연결 대상). */
  private User saveVerifiedSocialUser(String email, UserRole role, String name) {
    return userRepository.save(new User(email, null, name, role, true));
  }

  private String callback(
      String registrationId, Map<String, Object> attributes, String method, String user)
      throws Exception {
    MockHttpServletRequest request =
        new MockHttpServletRequest(method, "/login/oauth2/code/" + registrationId);
    if (user != null) {
      request.setParameter("user", user);
    }
    MockHttpServletResponse response = new MockHttpServletResponse();
    OAuth2AuthenticationToken authentication =
        new OAuth2AuthenticationToken(new FakeOAuth2User(attributes), List.of(), registrationId);
    successHandler.onAuthenticationSuccess(request, response, authentication);
    assertThat(response.getStatus()).as("리다이렉트 상태").isEqualTo(302);
    return response.getRedirectedUrl();
  }

  /** 애플의 POST 콜백. */
  private String appleCallback(Map<String, Object> claims, String user) throws Exception {
    return callback("apple", claims, "POST", user);
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

  private static Map<String, Object> apple(Object sub, Object email, Object verified) {
    return mapOf("sub", sub, "email", email, "email_verified", verified);
  }

  /** 새 가입 대기를 만드는 기본 클레임 — email_verified 는 애플이 실제로 보내는 문자열 "true". */
  private static Map<String, Object> appleNew() {
    return apple("a-001", "new@example.com", "true");
  }

  /** user 파라미터 JSON. ABSENT 인 이름 부분은 넣지 않는다. user.email 은 항상 다른 값 — 무시돼야 한다. */
  private static String userJson(Object firstName, Object lastName) {
    Map<String, Object> name = new LinkedHashMap<>();
    if (firstName != ABSENT) {
      name.put("firstName", firstName);
    }
    if (lastName != ABSENT) {
      name.put("lastName", lastName);
    }
    Map<String, Object> user = new LinkedHashMap<>();
    user.put("name", name);
    user.put("email", "other@example.com");
    return json(user);
  }

  private void assertPending(SoftAssertions soft, String label, String location) {
    soft.assertThat(location).as(label + " — 가입 대기 주소").startsWith(PENDING_PREFIX);
    soft.assertThat(queryOf(location)).as(label + " — 쿼리 키").containsOnlyKeys("pendingToken");
  }

  private void assertFailure(SoftAssertions soft, String label, String location) {
    soft.assertThat(location).as(label + " — 실패 주소").isEqualTo(FAILURE_URL);
    soft.assertThat(queryOf(location)).as(label + " — 실패 쿼리").containsOnlyKeys("error");
  }

  // ---------- SC-03 ----------

  private record NameRow(
      String label,
      String registrationId,
      Map<String, Object> claims,
      String method,
      String user,
      String expectedName,
      String expectedProvider,
      String expectedProviderUserId) {}

  private static NameRow appleRow(String label, String user, String expectedName) {
    return new NameRow(label, "apple", appleNew(), "POST", user, expectedName, "APPLE", "a-001");
  }

  @Test
  void test_sc03_최초_로그인의_이름은_가입_대기_토큰에_실린다() throws Exception {
    // Given users 에 new@example.com 이 없고 social_accounts 가 비어 있다
    // And 애플이 {sub:a-001, email:new@example.com, email_verified:"true"} 를 돌려줬다
    String first49 = "가".repeat(49);
    String last50 = "나".repeat(50);
    String hundred = "가".repeat(100);
    String emoji98 = "가".repeat(98) + "😀";
    String valid = userJson("길동", "홍");
    List<NameRow> rows = new ArrayList<>();
    rows.add(appleRow("1 정상 — user.email 은 무시", valid, "길동 홍"));
    rows.add(appleRow("2 firstName 만", userJson("길동", ABSENT), "길동"));
    rows.add(appleRow("3 lastName 만", userJson(ABSENT, "홍"), "홍"));
    rows.add(appleRow("4 firstName 공백만", userJson("   ", "홍"), "홍"));
    rows.add(appleRow("5 firstName JSON null", userJson(null, "홍"), "홍"));
    rows.add(appleRow("6 둘 다 빈 문자열", userJson("", ""), NO_NAME));
    rows.add(appleRow("7 name 이 빈 객체", "{\"name\":{}}", NO_NAME));
    rows.add(appleRow("8a name 키 없음 — 빈 객체", "{}", NO_NAME));
    rows.add(appleRow("8b name 키 없음 — email 만", "{\"email\":\"other@example.com\"}", NO_NAME));
    rows.add(appleRow("9 user 파라미터 없음", null, NO_NAME));
    rows.add(appleRow("10a user 빈 문자열", "", NO_NAME));
    rows.add(appleRow("10b user 공백만", "   ", NO_NAME));
    rows.add(appleRow("11 합성 정확히 100자", userJson(first49, last50), first49 + " " + last50));
    rows.add(appleRow("12 앞 공백은 변환하지 않는다", userJson(" 길동", "홍"), " 길동 홍"));
    rows.add(appleRow("13 짝이 맞는 서로게이트", userJson("민😀", "김"), "민😀 김"));
    rows.add(
        new NameRow(
            "14 GET 이면 읽지 않는다", "apple", appleNew(), "GET", valid, NO_NAME, "APPLE", "a-001"));
    rows.add(appleRow("15a firstName 단독 100자", userJson(hundred, ABSENT), hundred));
    rows.add(appleRow("15b lastName 단독 100자", userJson(ABSENT, hundred), hundred));
    rows.add(appleRow("16 UTF-16 코드 유닛 합 정확히 100", userJson(emoji98, ABSENT), emoji98));
    rows.add(
        new NameRow(
            "17 apple 이 아니면 읽지 않는다",
            "google",
            mapOf("sub", "g-1", "email", "new@example.com", "email_verified", true, "name", "구글이름"),
            "POST",
            valid,
            "구글이름",
            "GOOGLE",
            "g-1"));
    SoftAssertions soft = new SoftAssertions();

    for (NameRow row : rows) {
      Counts before = counts();

      // When 해당 제공자 콜백을 처리한다
      String location = callback(row.registrationId(), row.claims(), row.method(), row.user());

      // Then pendingToken 주소로 이동하고 쿼리에 token 은 없다
      assertPending(soft, row.label(), location);
      // And users·social_accounts 행 수가 그대로다
      soft.assertThat(counts()).as(row.label() + " — 행 수").isEqualTo(before);
      // And pendingToken 의 값이 표와 같다
      String pendingToken = queryOf(location).get("pendingToken");
      if (pendingToken != null) {
        Map<String, Object> payload = payloadOf(pendingToken);
        soft.assertThat(payload.get("email"))
            .as(row.label() + " — email")
            .isEqualTo("new@example.com");
        soft.assertThat(payload.get("provider"))
            .as(row.label() + " — provider")
            .isEqualTo(row.expectedProvider());
        soft.assertThat(payload.get("providerUserId"))
            .as(row.label() + " — providerUserId")
            .isEqualTo(row.expectedProviderUserId());
        soft.assertThat(payload.get("emailVerified"))
            .as(row.label() + " — emailVerified")
            .isEqualTo(true);
        soft.assertThat(payload.get("name"))
            .as(row.label() + " — name")
            .isEqualTo(row.expectedName());
      }
    }
    soft.assertAll();
  }

  // ---------- SC-04 ----------

  private record InputRow(
      String label,
      Map<String, Object> claims,
      String user,
      boolean accepted,
      String acceptedEmail,
      String acceptedProviderUserId) {}

  private static Map<String, Object> claimsWith(String key, Object value) {
    Map<String, Object> claims = apple("a-100", "new@example.com", "true");
    if (value == ABSENT) {
      claims.remove(key);
    } else {
      claims.put(key, value);
    }
    return claims;
  }

  private static InputRow rejectEmail(String label, Object email) {
    return new InputRow("email " + label, claimsWith("email", email), null, false, null, null);
  }

  private static InputRow rejectSub(String label, Object sub) {
    return new InputRow("sub " + label, claimsWith("sub", sub), null, false, null, null);
  }

  private static InputRow rejectUser(String label, String user) {
    return new InputRow("user " + label, claimsWith("sub", "a-100"), user, false, null, null);
  }

  @Test
  void test_sc04_규칙_밖의_외부_문자열은_아무것도_만들지_않고_거부하고_경계_안의_값은_받아들인다() throws Exception {
    // Given users 에 new@example.com 이 없고 social_accounts 가 비어 있다
    // And 애플이 기본 클레임에서 표의 한 필드만 바꾼 값을 돌려줬다 (user 행은 POST 콜백에 user 를 싣는다)
    String email255 = "a".repeat(255 - "@example.com".length()) + "@example.com";
    String email256 = "a".repeat(256 - "@example.com".length()) + "@example.com";
    String dotted256 = "İ".repeat(122) + "@example.com";
    List<InputRow> rows = new ArrayList<>();
    // email — 거부
    rows.add(rejectEmail("누락", ABSENT));
    rows.add(rejectEmail("null", null));
    rows.add(rejectEmail("숫자 123", 123));
    rows.add(rejectEmail("빈 문자열", ""));
    rows.add(rejectEmail("공백만", "   "));
    rows.add(rejectEmail("@ 없음", "no-at-sign"));
    rows.add(rejectEmail("@@", "a@@b.com"));
    rows.add(rejectEmail("로컬 파트 없음", "@b.com"));
    rows.add(rejectEmail("도메인 없음", "a@"));
    rows.add(rejectEmail("NUL 포함", "a" + NUL + "@b.com"));
    rows.add(rejectEmail("앞 NUL", NUL + "new@example.com"));
    rows.add(rejectEmail("뒤 NUL", "new@example.com" + NUL));
    rows.add(rejectEmail("짝 없는 서로게이트", "a" + LONE_SURROGATE + "@b.com"));
    rows.add(rejectEmail("탭 포함", "a\t@b.com"));
    rows.add(rejectEmail("개행 포함", "a\n@b.com"));
    rows.add(rejectEmail("NBSP", "a b@c.com"));
    rows.add(rejectEmail("제로폭 공백", "a​b@c.com"));
    rows.add(rejectEmail("방향 제어", "a‮b@c.com"));
    rows.add(rejectEmail("소문자화 뒤 256자", email256));
    rows.add(rejectEmail("소문자화로 길어져 256자", dotted256));
    // email — 허용
    rows.add(
        new InputRow("email 255자", claimsWith("email", email255), null, true, email255, "a-100"));
    rows.add(
        new InputRow(
            "email 대문자·앞뒤 공백",
            claimsWith("email", "  New@Example.COM "),
            null,
            true,
            "new@example.com",
            "a-100"));
    String relay = "abc123@privaterelay.appleid.com";
    for (Object isPrivate : new Object[] {"true", "false", ABSENT}) {
      Map<String, Object> claims = claimsWith("email", relay);
      if (isPrivate != ABSENT) {
        claims.put("is_private_email", isPrivate);
      }
      rows.add(
          new InputRow("릴레이 주소 is_private_email=" + isPrivate, claims, null, true, relay, "a-100"));
    }
    // sub
    rows.add(rejectSub("누락", ABSENT));
    rows.add(rejectSub("null", null));
    rows.add(rejectSub("숫자 12345", 12345));
    rows.add(rejectSub("불리언", true));
    rows.add(rejectSub("빈 문자열", ""));
    rows.add(rejectSub("공백만", "   "));
    rows.add(rejectSub("NUL 포함", "a" + NUL + "b"));
    rows.add(rejectSub("짝 없는 서로게이트", "a" + LONE_SURROGATE));
    rows.add(rejectSub("탭 포함", "a\tb"));
    rows.add(rejectSub("256자", "a".repeat(256)));
    String sub255 = "a".repeat(255);
    rows.add(
        new InputRow("sub 255자", claimsWith("sub", sub255), null, true, "new@example.com", sub255));
    // user
    for (String raw : new String[] {"not-json", "[]", "\"홍길동\"", "123", "null", "true"}) {
      rows.add(rejectUser("JSON 객체가 아님 " + raw, raw));
    }
    for (String raw : new String[] {"{\"name\":\"홍길동\"}", "{\"name\":[]}", "{\"name\":123}"}) {
      rows.add(rejectUser("name 이 객체가 아님 " + raw, raw));
    }
    for (String type : new String[] {"123", "true", "[]", "{}"}) {
      rows.add(
          rejectUser(
              "firstName 타입 " + type,
              "{\"name\":{\"firstName\":" + type + ",\"lastName\":\"홍\"}}"));
    }
    for (String escape : new String[] {"\\u0000", "\\t", "\\n", "\\ud800"}) {
      rows.add(
          rejectUser(
              "firstName 에 " + escape,
              "{\"name\":{\"firstName\":\"길" + escape + "동\",\"lastName\":\"홍\"}}"));
    }
    for (String escape : new String[] {"\\u0000", "\\ud800"}) {
      rows.add(
          rejectUser(
              "lastName 에 " + escape,
              "{\"name\":{\"firstName\":\"길동\",\"lastName\":\"홍" + escape + "\"}}"));
    }
    rows.add(rejectUser("firstName 101자", userJson("가".repeat(101), ABSENT)));
    rows.add(rejectUser("lastName 101자", userJson(ABSENT, "가".repeat(101))));
    rows.add(rejectUser("합성 101자 (50+50)", userJson("가".repeat(50), "나".repeat(50))));
    rows.add(rejectUser("UTF-16 코드 유닛 합 101", userJson("가".repeat(99) + "😀", ABSENT)));
    SoftAssertions soft = new SoftAssertions();

    for (InputRow row : rows) {
      Counts before = counts();

      // When 애플 콜백을 처리한다
      String location = appleCallback(row.claims(), row.user());

      // Then 거부면 실패 주소, 허용이면 가입 대기 주소와 값
      soft.assertThat(counts()).as(row.label() + " — 행 수").isEqualTo(before);
      if (row.accepted()) {
        assertPending(soft, row.label(), location);
        String pendingToken = queryOf(location).get("pendingToken");
        if (pendingToken != null) {
          Map<String, Object> payload = payloadOf(pendingToken);
          soft.assertThat(payload.get("email"))
              .as(row.label() + " — email")
              .isEqualTo(row.acceptedEmail());
          soft.assertThat(payload.get("providerUserId"))
              .as(row.label() + " — providerUserId")
              .isEqualTo(row.acceptedProviderUserId());
          if (row.label().startsWith("릴레이")) {
            soft.assertThat(payload.get("emailVerified"))
                .as(row.label() + " — emailVerified")
                .isEqualTo(true);
          }
        }
      } else {
        assertFailure(soft, row.label(), location);
      }
    }

    // 이미 연결된 계정의 재로그인 콜백에 not-json 이 실려도 거부한다
    User b = saveVerifiedSocialUser("b@example.com", UserRole.AUDIENCE, "원래이름");
    socialAccountRepository.save(new SocialAccount(b.getId(), SocialProvider.APPLE, "a-100"));
    Counts linkedBefore = counts();
    String linkedLocation = appleCallback(apple("a-100", "new@example.com", "true"), "not-json");
    assertFailure(soft, "이미 연결된 계정 + not-json", linkedLocation);
    soft.assertThat(counts()).as("이미 연결된 계정 — 행 수").isEqualTo(linkedBefore);
    soft.assertAll();
  }

  // ---------- SC-05 ----------

  @Test
  void test_sc05_이메일이_일치하고_애플이_검증했으면_기존_계정에_연결하고_바로_로그인한다() throws Exception {
    // Given 제공자가 검증한 이메일로 소셜 가입을 끝낸 사용자 A(hong@example.com, ORGANIZER, 홍길동)가 있다
    User a = saveVerifiedSocialUser("hong@example.com", UserRole.ORGANIZER, "홍길동");
    String hashBefore = a.getPasswordHash();
    record Row(String label, Map<String, Object> claims) {}
    List<Row> rows =
        List.of(
            new Row("불리언 true", apple("a-1", "hong@example.com", true)),
            new Row("문자열 \"true\"", apple("a-1", "hong@example.com", "true")),
            new Row("문자열 \"true\" + 대문자·앞뒤 공백", apple("a-1", "  Hong@Example.COM  ", "true")));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      socialAccountRepository.deleteAll();

      // When 애플 콜백을 처리한다 (user 파라미터 없음)
      String location = appleCallback(row.claims(), null);

      // Then 로그인 주소로 이동하고 쿼리에 pendingToken 은 없다
      soft.assertThat(location).as(row.label() + " — 로그인 주소").startsWith(TOKEN_PREFIX);
      soft.assertThat(queryOf(location)).as(row.label() + " — 쿼리 키").containsOnlyKeys("token");
      String token = queryOf(location).get("token");
      if (token != null) {
        // And 그 accessToken 으로 GET /api/auth/me 를 호출하면 A 의 정보(ORGANIZER)가 나온다
        MvcResult me =
            mockMvc
                .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andReturn();
        soft.assertThat(me.getResponse().getStatus()).as(row.label() + " — /me 상태").isEqualTo(200);
        Map<?, ?> body = OBJECT_MAPPER.readValue(me.getResponse().getContentAsString(), Map.class);
        soft.assertThat(body.get("role")).as(row.label() + " — /me 역할").isEqualTo("ORGANIZER");
      }
      // And users 행 수와 A 의 정보는 그대로다
      soft.assertThat(userRepository.count()).as(row.label() + " — users 행 수").isEqualTo(1);
      User reread = userRepository.findById(a.getId()).orElseThrow();
      soft.assertThat(reread.getEmail()).isEqualTo("hong@example.com");
      soft.assertThat(reread.getName()).isEqualTo("홍길동");
      soft.assertThat(reread.getRole()).isEqualTo(UserRole.ORGANIZER);
      soft.assertThat(reread.getPasswordHash()).isEqualTo(hashBefore);
      soft.assertThat(reread.isEmailVerified()).as(row.label() + " — email_verified").isTrue();
      // And social_accounts 에 (APPLE, a-1, A) 가 정확히 1건 생긴다
      List<SocialAccount> accounts = socialAccountRepository.findAll();
      soft.assertThat(accounts).as(row.label() + " — social_accounts 행 수").hasSize(1);
      if (accounts.size() == 1) {
        soft.assertThat(accounts.get(0).getProvider()).isEqualTo(SocialProvider.APPLE);
        soft.assertThat(accounts.get(0).getProviderUserId()).isEqualTo("a-1");
        soft.assertThat(accounts.get(0).getUserId()).isEqualTo(a.getId());
      }
    }
    soft.assertAll();
  }

  // ---------- SC-06 ----------

  @Test
  void test_sc06_애플의_검증_플래그가_true_로_읽히지_않으면_연결하지_않고_가입_대기가_된다() throws Exception {
    // Given 제공자가 검증한 이메일로 소셜 가입을 끝낸 사용자 A(hong@example.com, email_verified=true)가 있다
    User a = saveVerifiedSocialUser("hong@example.com", UserRole.AUDIENCE, "홍길동");
    record Row(String label, Object value) {}
    List<Row> rows =
        List.of(
            new Row("불리언 false", false),
            new Row("문자열 \"false\"", "false"),
            new Row("필드 누락", ABSENT),
            new Row("null", null),
            new Row("\"True\"", "True"),
            new Row("\"TRUE\"", "TRUE"),
            new Row("\"tRue\"", "tRue"),
            new Row("\" true\"", " true"),
            new Row("\"true \"", "true "),
            new Row("\"1\"", "1"),
            new Row("숫자 1", 1),
            new Row("숫자 0", 0),
            new Row("\"yes\"", "yes"),
            new Row("\"t\"", "t"),
            new Row("빈 문자열", ""),
            new Row("전각 \"ｔｒｕｅ\"", "ｔｒｕｅ"),
            new Row("뒤에 NUL", "true" + NUL),
            new Row("배열 [true]", List.of(true)),
            new Row("객체 {v:true}", Map.of("v", true)));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      Counts before = counts();

      // When 애플 콜백을 처리한다
      String location = appleCallback(apple("a-1", "hong@example.com", row.value()), null);

      // Then 가입 대기 주소로 이동하고 쿼리에 token 은 없다
      assertPending(soft, row.label(), location);
      // And users·social_accounts 행 수가 그대로이고 A 의 정보는 바뀌지 않는다
      soft.assertThat(counts()).as(row.label() + " — 행 수").isEqualTo(before);
      User reread = userRepository.findById(a.getId()).orElseThrow();
      soft.assertThat(reread.getName()).as(row.label() + " — A 이름").isEqualTo("홍길동");
      // And pendingToken 의 email 은 hong@example.com, emailVerified 는 false 다
      String pendingToken = queryOf(location).get("pendingToken");
      if (pendingToken != null) {
        Map<String, Object> payload = payloadOf(pendingToken);
        soft.assertThat(payload.get("email"))
            .as(row.label() + " — email")
            .isEqualTo("hong@example.com");
        soft.assertThat(payload.get("emailVerified"))
            .as(row.label() + " — emailVerified")
            .isEqualTo(false);
      }
    }
    soft.assertAll();
  }

  // ---------- SC-07 ----------

  @Test
  void test_sc07_이미_연결된_애플_계정은_이메일이_바뀌거나_미검증이어도_같은_사용자로_로그인하고_이름은_바뀌지_않는다() throws Exception {
    // Given 사용자 B(b@example.com, 원래이름)가 있고 social_accounts 에 (APPLE, a-9, B) 가 있다
    User b = userRepository.save(new User("b@example.com", null, "원래이름", UserRole.AUDIENCE, true));
    socialAccountRepository.save(new SocialAccount(b.getId(), SocialProvider.APPLE, "a-9"));
    Counts before = counts();
    // And 애플이 {sub:a-9, email:other@example.com, email_verified:"false"} 를 돌려줬다
    Map<String, Object> claims = apple("a-9", "other@example.com", "false");
    List<String> users = new ArrayList<>();
    users.add(null);
    users.add(userJson("새", "이름"));
    SoftAssertions soft = new SoftAssertions();

    for (String user : users) {
      String label = user == null ? "user 없음" : "user 에 다른 이름";

      // When 애플 POST 콜백을 처리한다
      String location = appleCallback(claims, user);

      // Then 로그인 주소로 이동하고 그 토큰의 사용자는 B 다
      soft.assertThat(location).as(label + " — 로그인 주소").startsWith(TOKEN_PREFIX);
      String token = queryOf(location).get("token");
      if (token != null) {
        MvcResult me =
            mockMvc
                .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andReturn();
        Map<?, ?> body = OBJECT_MAPPER.readValue(me.getResponse().getContentAsString(), Map.class);
        soft.assertThat(body.get("email")).as(label + " — 토큰의 사용자").isEqualTo("b@example.com");
      }
      // And social_accounts 는 (APPLE, a-9, B) 1건뿐이고 users 행 수·B 의 이메일·이름은 그대로다
      soft.assertThat(counts()).as(label + " — 행 수").isEqualTo(before);
      User reread = userRepository.findById(b.getId()).orElseThrow();
      soft.assertThat(reread.getEmail()).as(label + " — B 이메일").isEqualTo("b@example.com");
      soft.assertThat(reread.getName()).as(label + " — B 이름").isEqualTo("원래이름");
    }
    soft.assertAll();
  }

  // ---------- SC-08 ----------

  @Test
  void test_sc08_역할을_고르면_애플_이름으로_가입이_끝나고_로그인된다() throws Exception {
    record Row(
        String label,
        Object verified,
        String user,
        String expectedName,
        boolean expectedVerified) {}
    String hundred = "가".repeat(49) + " " + "나".repeat(50);
    List<Row> rows =
        List.of(
            new Row("1 이름 있음", "true", userJson("길동", "홍"), "길동 홍", true),
            new Row("2 이름 없음", "true", null, NO_NAME, true),
            new Row("3 합성 100자", "true", userJson("가".repeat(49), "나".repeat(50)), hundred, true),
            new Row("4 미검증 + 이름 있음", "false", userJson("길동", "홍"), "길동 홍", false));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      cleanUp();
      // Given 애플 콜백을 처리해 가입 대기(pendingToken)를 받았다
      String location =
          appleCallback(apple("a-001", "new@example.com", row.verified()), row.user());
      String pendingToken = queryOf(location).get("pendingToken");
      soft.assertThat(pendingToken).as(row.label() + " — 가입 대기 토큰").isNotBlank();
      if (pendingToken == null) {
        continue;
      }

      // When POST /api/auth/social/complete 에 pendingToken 과 role=ORGANIZER 를 보낸다
      MvcResult result =
          postJson(
              mockMvc,
              "/api/auth/social/complete",
              json(Map.of("pendingToken", pendingToken, "role", "ORGANIZER")));

      // Then 200 LoginResponse 이고 user 의 이메일·역할·이름이 표와 같다
      soft.assertThat(result.getResponse().getStatus()).as(row.label() + " — 상태").isEqualTo(200);
      Map<?, ?> body =
          OBJECT_MAPPER.readValue(result.getResponse().getContentAsString(), Map.class);
      Map<?, ?> user = body.get("user") instanceof Map<?, ?> map ? map : Map.of();
      soft.assertThat(user.get("email"))
          .as(row.label() + " — user.email")
          .isEqualTo("new@example.com");
      soft.assertThat(user.get("role")).as(row.label() + " — user.role").isEqualTo("ORGANIZER");
      soft.assertThat(user.get("name"))
          .as(row.label() + " — user.name")
          .isEqualTo(row.expectedName());
      // And 그 accessToken 으로 GET /api/auth/me 를 호출하면 같은 이메일·이름·역할이 나온다
      Object accessToken = body.get("accessToken");
      if (accessToken instanceof String token) {
        MvcResult me =
            mockMvc
                .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andReturn();
        Map<?, ?> meBody =
            OBJECT_MAPPER.readValue(me.getResponse().getContentAsString(), Map.class);
        soft.assertThat(meBody.get("name"))
            .as(row.label() + " — /me 이름")
            .isEqualTo(row.expectedName());
        soft.assertThat(meBody.get("email"))
            .as(row.label() + " — /me 이메일")
            .isEqualTo("new@example.com");
        soft.assertThat(meBody.get("role")).as(row.label() + " — /me 역할").isEqualTo("ORGANIZER");
      }
      // And users 에 새 행이 정확히 1건이고 social_accounts 에 (APPLE, a-001, 그 사용자) 가 정확히 1건이다
      List<User> users = userRepository.findAll();
      soft.assertThat(users).as(row.label() + " — users 행 수").hasSize(1);
      if (users.size() == 1) {
        User created = users.get(0);
        soft.assertThat(created.getEmail()).isEqualTo("new@example.com");
        soft.assertThat(created.getName())
            .as(row.label() + " — users.name")
            .isEqualTo(row.expectedName());
        soft.assertThat(created.getRole()).isEqualTo(UserRole.ORGANIZER);
        soft.assertThat(created.getPasswordHash()).isNull();
        soft.assertThat(created.isEmailVerified())
            .as(row.label() + " — users.email_verified")
            .isEqualTo(row.expectedVerified());
        List<SocialAccount> accounts = socialAccountRepository.findAll();
        soft.assertThat(accounts).as(row.label() + " — social_accounts 행 수").hasSize(1);
        if (accounts.size() == 1) {
          soft.assertThat(accounts.get(0).getProvider()).isEqualTo(SocialProvider.APPLE);
          soft.assertThat(accounts.get(0).getProviderUserId()).isEqualTo("a-001");
          soft.assertThat(accounts.get(0).getUserId()).isEqualTo(created.getId());
        }
      }
    }
    soft.assertAll();
  }
}
