package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.OBJECT_MAPPER;
import static com.example.ticket_booking.support.AuthTestHelper.bodyAsMap;
import static com.example.ticket_booking.support.AuthTestHelper.postEmpty;
import static com.example.ticket_booking.support.AuthTestHelper.postJson;
import static com.example.ticket_booking.support.PendingTokenTestHelper.ABSENT;

import com.example.ticket_booking.repository.SocialAccountRepository;
import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.HandleRandomTestConfig;
import com.example.ticket_booking.support.MutableClock;
import com.example.ticket_booking.support.PendingTokenTestHelper;
import com.example.ticket_booking.support.SequenceRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * TASK-016 시나리오 SC-08 ~ SC-10, SC-14 — 소셜 가입 대기 정보 조회와 pendingToken 클레임 규칙(PLAN
 * design.claim_schema).
 *
 * <p>pendingToken 은 구현과 무관하게 직접 서명한다(PendingTokenTestHelper.signedRaw — 클레임 값을 변환하지 않는다). 클래스 레벨
 * {@code @Transactional} 을 두지 않고 행 수는 리포지토리로 새로 센다. 표의 각 행은 {@link SoftAssertions} 로 독립 단언한다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-016.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ClockTestConfig.class, HandleRandomTestConfig.class})
class PendingSignupApiTest {

  private static final String PENDING = "/api/auth/social/pending";
  private static final String COMPLETE = "/api/auth/social/complete";
  private static final Instant T = Instant.parse("2026-11-11T08:35:00Z");
  private static final String EXPIRES_AT = "2026-11-11T08:40:00Z";
  private static final String EMAIL = "harin.kim@kakao.com";
  private static final String NAME = "김하린";
  private static final String OTHER_SECRET = "another-secret-for-forged-tokens-0123456789";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private SocialAccountRepository socialAccountRepository;
  @Autowired private MutableClock clock;
  @Autowired private SequenceRandom random;

  @Value("${JWT_SECRET}")
  private String secret;

  @BeforeEach
  void reset() {
    cleanUp();
    clock.setInstant(T);
  }

  @AfterEach
  void cleanUp() {
    random.clear();
    socialAccountRepository.deleteAll();
    userRepository.deleteAll();
  }

  // ---------- 헬퍼 ----------

  private record Counts(long users, long socialAccounts) {}

  private Counts counts() {
    return new Counts(userRepository.count(), socialAccountRepository.count());
  }

  /** 시각 T 에 발급한 유효한 클레임 (KAKAO, harin.kim@kakao.com, 김하린). */
  private static Map<String, Object> claims(String provider) {
    Map<String, Object> claims = PendingTokenTestHelper.validClaims(T);
    claims.put("provider", provider);
    claims.put("email", EMAIL);
    claims.put("name", NAME);
    return claims;
  }

  private String token(Map<String, Object> claims) {
    return PendingTokenTestHelper.signedRaw(secret, claims);
  }

  private String claimRow(String claim, Object value) {
    Map<String, Object> claims = claims("KAKAO");
    claims.put(claim, value);
    return token(claims);
  }

  private static String tokenBody(String token) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("pendingToken", token);
    return OBJECT_MAPPER.writeValueAsString(body);
  }

  /** 본문이 JSON 이 아니거나 비어 있으면 null — 엔드포인트가 아직 없어 HTML·빈 본문이 와도 단언이 실패로 끝나게 한다. */
  private static String codeOf(MvcResult result) {
    try {
      Object code = bodyAsMap(result).get("code");
      return code == null ? null : String.valueOf(code);
    } catch (Exception notJson) {
      return null;
    }
  }

  private static Object messageOf(MvcResult result) {
    try {
      return bodyAsMap(result).get("message");
    } catch (Exception notJson) {
      return null;
    }
  }

  private MvcResult pending(String rawBody) throws Exception {
    return postJson(mockMvc, PENDING, rawBody);
  }

  private MvcResult complete(String rawBody) throws Exception {
    return postJson(mockMvc, COMPLETE, rawBody);
  }

  /** 거부 표의 한 행 — 요청 본문 문자열 그대로 (요청 문자열에 짝 없는 서로게이트를 넣는 행이 있어 본문을 직접 만든다). */
  private record RejectRow(String label, Supplier<String> tokenJson, Instant now) {}

  private static String quoted(String token) {
    return OBJECT_MAPPER.writeValueAsString(token);
  }

  /** SC-09 의 「토큰 자체」 표와 「클레임」 표 — SC-14 가 같은 값을 가입 완료 입구에서 다시 쓴다. */
  private List<RejectRow> rejectRows() {
    List<RejectRow> rows = new ArrayList<>();
    String valid = token(claims("KAKAO"));

    // 토큰 자체
    rows.add(new RejectRow("만료 정각 (T+300)", () -> quoted(valid), T.plusSeconds(300)));
    rows.add(new RejectRow("만료 후 (T+301)", () -> quoted(valid), T.plusSeconds(301)));
    rows.add(
        new RejectRow(
            "다른 키로 서명",
            () -> quoted(PendingTokenTestHelper.signedRaw(OTHER_SECRET, claims("KAKAO"))),
            T));
    rows.add(new RejectRow("페이로드 변조", () -> quoted(tamperEmail(valid)), T));
    rows.add(new RejectRow("형식이 깨짐 abc", () -> quoted("abc"), T));
    rows.add(new RejectRow("8,192자 임의 문자열", () -> quoted("a".repeat(8192)), T));
    rows.add(new RejectRow("요청 문자열에 NUL", () -> quoted(valid + "\u0000"), T));
    rows.add(new RejectRow("요청 문자열에 짝 없는 서로게이트", () -> "\"" + valid + "\\ud800\"", T));
    for (String suffix : List.of("=", "==", "~", "!", " ", "\n", "é")) {
      rows.add(
          new RejectRow(
              "끝에 " + suffix.replace("\n", "LF").replace(" ", "공백"),
              () -> quoted(valid + suffix),
              T));
    }
    rows.add(new RejectRow("앞에 공백", () -> quoted(" " + valid), T));
    rows.add(new RejectRow("마디가 넷 (.abc)", () -> quoted(valid + ".abc"), T));
    rows.add(
        new RejectRow(
            "페이로드 마디 가운데 NUL",
            () -> quoted(PendingTokenTestHelper.insertIntoSegment(valid, 1, "\u0000")),
            T));
    rows.add(
        new RejectRow(
            "서명 마디 가운데 NUL",
            () -> quoted(PendingTokenTestHelper.insertIntoSegment(valid, 2, "\u0000")),
            T));
    long expSeconds = T.plusSeconds(300).getEpochSecond();
    String body = PendingTokenTestHelper.rawJson(claims("KAKAO")).substring(1);
    rows.add(
        new RejectRow(
            "중복 키 exp (정수, 정수)",
            () ->
                quoted(
                    PendingTokenTestHelper.signedJson(
                        secret, "{\"exp\":" + (expSeconds - 10) + "," + body)),
            T));
    rows.add(
        new RejectRow(
            "중복 키 exp (null, 정수)",
            () -> quoted(PendingTokenTestHelper.signedJson(secret, "{\"exp\":null," + body)),
            T));
    rows.add(
        new RejectRow(
            "중복 키 typ (pending, pending)",
            () -> quoted(PendingTokenTestHelper.signedJson(secret, "{\"typ\":\"pending\"," + body)),
            T));

    // 클레임 × 이상값 (PLAN design.claim_schema)
    Object[] typeVariants = {1, Map.of(), List.of(), true};
    for (String claim : List.of("typ", "email", "name", "provider", "providerUserId")) {
      claimRows(rows, claim, "누락", ABSENT);
      claimRows(rows, claim, "null", null);
      claimRows(rows, claim, "빈 문자열", "");
      claimRows(rows, claim, "공백만", "   ");
      for (Object variant : typeVariants) {
        claimRows(rows, claim, "타입 " + describe(variant), variant);
      }
    }
    claimRows(rows, "typ", "NUL", "pend\u0000ing");
    claimRows(rows, "typ", "짝 없는 서로게이트", "pending\ud800");
    claimRows(rows, "typ", "access", "access");
    claimRows(rows, "typ", "PENDING", "PENDING");

    long t = T.getEpochSecond();
    claimRows(rows, "exp", "누락", ABSENT);
    claimRows(rows, "exp", "null", null);
    claimRows(rows, "exp", "빈 문자열", "");
    claimRows(rows, "exp", "공백만", "   ");
    claimRows(rows, "exp", "문자열 later", "later");
    claimRows(rows, "exp", "객체", Map.of());
    claimRows(rows, "exp", "배열", List.of());
    claimRows(rows, "exp", "불리언", true);
    claimRows(rows, "exp", "소수 T+120.5", t + 120.5);
    claimRows(rows, "exp", "먼 미래 T+301", t + 301);
    claimRows(rows, "exp", "Instant 범위 초과 (양)", 9.3e18);
    claimRows(rows, "exp", "0", 0L);
    claimRows(rows, "exp", "음수 -1", -1L);
    claimRows(rows, "exp", "Instant 범위 초과 (음)", -9.3e18);
    claimRows(rows, "exp", "숫자 문자열", String.valueOf(t + 120));
    // JSON 숫자 표기를 그대로 남기려고 BigDecimal 로 쓴다 — "1762850220.0" / "1.76285022E+9"
    claimRows(
        rows,
        "exp",
        "정수값 소수 표기 T+120.0",
        new java.math.BigDecimal(java.math.BigInteger.valueOf((t + 120) * 10), 1));
    claimRows(
        rows,
        "exp",
        "지수 표기 T+120",
        new java.math.BigDecimal(java.math.BigInteger.valueOf((t + 120) / 10), -1));
    claimRows(rows, "exp", "정수 Instant 최대+1", 31556889864403200L);
    claimRows(rows, "exp", "정수 Instant 최소-1", -31557014167219201L);
    claimRows(rows, "exp", "long 초과 양", new java.math.BigInteger("99999999999999999999"));
    claimRows(rows, "exp", "long 초과 음", new java.math.BigInteger("-99999999999999999999"));

    claimRows(rows, "email", "NUL", "harin\u0000kim@kakao.com");
    claimRows(rows, "email", "짝 없는 서로게이트", "harin\ud800kim@kakao.com");
    claimRows(rows, "email", "정규화 후 256자 (소문자)", "a".repeat(244) + "@example.com");
    claimRows(rows, "email", "정규화 후 256자 (대문자 섞음)", "A".repeat(244) + "@EXAMPLE.com");
    claimRows(rows, "email", "원문 255자 + İ 소문자 확장 256자", "İ" + "a".repeat(242) + "@example.com");
    claimRows(rows, "email", "@ 없음", "harin.kimkakao.com");
    claimRows(rows, "email", "@ 두 개", "harin@kim@kakao.com");
    claimRows(rows, "email", "@ 앞이 빔", "@a.com");
    claimRows(rows, "email", "@ 뒤가 빔", "a@");
    claimRows(rows, "email", "내부 공백", "a b@c.com");
    claimRows(rows, "email", "내부 NBSP", "a\u00a0b@c.com");
    claimRows(rows, "email", "서식 문자 u200b", "harin\u200bkim@kakao.com");

    claimRows(rows, "name", "NUL", "김\u0000하린");
    claimRows(rows, "name", "짝 없는 서로게이트", "김하린\ud800");
    claimRows(rows, "name", "101자", "가".repeat(101));

    claimRows(rows, "provider", "NUL", "KAKAO\u0000");
    claimRows(rows, "provider", "짝 없는 서로게이트", "KAKAO\ud800");
    claimRows(rows, "provider", "정의 밖 FOO", "FOO");
    claimRows(rows, "provider", "소문자 kakao", "kakao");

    claimRows(rows, "providerUserId", "NUL", "k-1\u0000");
    claimRows(rows, "providerUserId", "짝 없는 서로게이트", "k-1\ud800");
    claimRows(rows, "providerUserId", "256자", "k".repeat(256));

    claimRows(rows, "emailVerified", "누락", ABSENT);
    claimRows(rows, "emailVerified", "null", null);
    claimRows(rows, "emailVerified", "빈 문자열", "");
    claimRows(rows, "emailVerified", "공백만", "  ");
    for (Object variant : new Object[] {"true", "TRUE", 1, 0, Map.of(), List.of()}) {
      claimRows(rows, "emailVerified", "타입 " + describe(variant), variant);
    }
    return rows;
  }

  private void claimRows(List<RejectRow> rows, String claim, String label, Object value) {
    rows.add(new RejectRow(claim + " " + label, () -> quoted(claimRow(claim, value)), T));
  }

  private static String describe(Object value) {
    if (value instanceof String s) return "문자열 \"" + s + "\"";
    if (value instanceof Map<?, ?>) return "객체 {}";
    if (value instanceof List<?>) return "배열 []";
    return value.getClass().getSimpleName() + " " + value;
  }

  /** 서명은 그대로 두고 페이로드의 이메일만 바꾼다. */
  private static String tamperEmail(String token) {
    String[] parts = token.split("\\.");
    java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
    java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder().withoutPadding();
    String payload = new String(decoder.decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
    String tampered = payload.replace(EMAIL, "attacker@kakao.com");
    parts[1] = encoder.encodeToString(tampered.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return String.join(".", Arrays.asList(parts));
  }

  // ---------- SC-08 ----------

  @Test
  void test_sc08_소셜_가입_대기_정보를_돌려준다() throws Exception {
    // Given 시각 T 에 발급한 유효한 pendingToken 이 있고 행 수를 기록했다
    Counts before = counts();
    record Row(String provider, Instant now) {}
    List<Row> rows =
        List.of(
            new Row("KAKAO", T),
            new Row("KAKAO", T.plusSeconds(299)),
            new Row("NAVER", T),
            new Row("GOOGLE", T),
            new Row("APPLE", T));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      clock.setInstant(row.now());
      // When 표의 시각에 POST /api/auth/social/pending 을 호출한다
      MvcResult result = pending(tokenBody(token(claims(row.provider()))));

      // Then 200 이고 본문이 {provider, email, name, expiresAt} 다
      String label = row.provider() + " @" + row.now();
      soft.assertThat(result.getResponse().getStatus()).as(label + " 상태").isEqualTo(200);
      if (result.getResponse().getStatus() == 200) {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("provider", row.provider());
        expected.put("email", EMAIL);
        expected.put("name", NAME);
        expected.put("expiresAt", EXPIRES_AT);
        soft.assertThat(bodyAsMap(result)).as(label + " 본문").isEqualTo(expected);
      }
    }
    // And users·social_accounts 행 수가 그대로다
    soft.assertThat(counts()).as("행 수").isEqualTo(before);

    // And 이메일 클레임이 「정규화」 표의 값이면 200 이고 email 이 표의 값이다 (KAKAO, 시각 T)
    clock.setInstant(T);
    String max = "a".repeat(243) + "@example.com";
    Map<String, String> normalization = new LinkedHashMap<>();
    normalization.put("  Harin.Kim@KAKAO.com  ", "harin.kim@kakao.com");
    normalization.put(max, max);
    normalization.put(" " + "A".repeat(243) + "@Example.COM" + " ", max);
    for (Map.Entry<String, String> row : normalization.entrySet()) {
      Map<String, Object> claims = claims("KAKAO");
      claims.put("email", row.getKey());
      MvcResult result = pending(tokenBody(token(claims)));
      String label = "정규화 원문 " + row.getKey().length() + "자";
      soft.assertThat(result.getResponse().getStatus()).as(label + " 상태").isEqualTo(200);
      soft.assertThat(
              result.getResponse().getStatus() == 200 ? bodyAsMap(result).get("email") : null)
          .as(label + " email")
          .isEqualTo(row.getValue());
    }
    soft.assertAll();
  }

  // ---------- SC-09 ----------

  @Test
  void test_sc09_만료_위조된_pendingToken_으로_대기_정보를_조회하면_401이다() throws Exception {
    // Given 행 수를 기록했고 요청의 pendingToken 이 표의 상태다
    Counts before = counts();
    SoftAssertions soft = new SoftAssertions();
    String firstMessage = null;

    for (RejectRow row : rejectRows()) {
      clock.setInstant(row.now());
      // When POST /api/auth/social/pending 을 호출한다
      MvcResult result = pending("{\"pendingToken\":" + row.tokenJson().get() + "}");

      // Then 401 PENDING_SIGNUP_EXPIRED 이다
      String body = result.getResponse().getContentAsString();
      soft.assertThat(result.getResponse().getStatus()).as(row.label() + " 상태").isEqualTo(401);
      String rowCode = codeOf(result);
      soft.assertThat(rowCode).as(row.label() + " code").isEqualTo("PENDING_SIGNUP_EXPIRED");
      // And 모든 행에서 응답의 code 와 message 가 같다
      Object message = messageOf(result);
      if (firstMessage == null && message != null) firstMessage = String.valueOf(message);
      soft.assertThat(message).as(row.label() + " message").isEqualTo(firstMessage);
      // And 어느 행에서도 응답 본문에 토큰 안의 이메일·이름 값이 나오지 않는다
      soft.assertThat(body).as(row.label() + " 본문 노출").doesNotContain(EMAIL).doesNotContain(NAME);
    }
    // And users·social_accounts 행 수가 그대로다
    soft.assertThat(counts()).as("행 수").isEqualTo(before);
    soft.assertAll();
  }

  // ---------- SC-14 ----------

  @Test
  void test_sc14_가입_완료도_같은_클레임_규칙으로_거부하고_아무것도_만들지_않는다() throws Exception {
    // Given 요청의 pendingToken 이 SC-09 표의 각 칸 상태다
    SoftAssertions soft = new SoftAssertions();

    for (RejectRow row : rejectRows()) {
      // 행마다 데이터를 비우고 행 수를 기록한다 — 한 행이 잘못 가입시켜도 다른 행의 판정에 번지지 않게
      cleanUp();
      Counts before = counts();
      clock.setInstant(row.now());
      // 거부되지 않는 구현이라도 하네스가 멈추지 않게 뽑힌 값을 준비한다
      random.clear();
      random.enqueue(1, 1);

      // When 역할 AUDIENCE 로 POST /api/auth/social/complete 를 호출한다
      MvcResult result =
          complete("{\"pendingToken\":" + row.tokenJson().get() + ",\"role\":\"AUDIENCE\"}");

      // Then 모든 칸에서 401 PENDING_SIGNUP_EXPIRED 이다
      String body = result.getResponse().getContentAsString();
      soft.assertThat(result.getResponse().getStatus()).as(row.label() + " 상태").isEqualTo(401);
      soft.assertThat(codeOf(result)).as(row.label() + " code").isEqualTo("PENDING_SIGNUP_EXPIRED");
      // And users·social_accounts 행 수가 그대로다
      soft.assertThat(counts()).as(row.label() + " 행 수").isEqualTo(before);
    }
    soft.assertAll();
  }

  // ---------- SC-10 ----------

  @Test
  void test_sc10_대기_정보_요청의_형식이_틀리면_400이다() throws Exception {
    // Given 행 수를 기록했다
    Counts before = counts();
    Map<String, String> rows = new LinkedHashMap<>();
    rows.put("pendingToken 누락", "{}");
    rows.put("pendingToken null", "{\"pendingToken\":null}");
    rows.put("빈 문자열", "{\"pendingToken\":\"\"}");
    rows.put("공백만", "{\"pendingToken\":\"   \"}");
    rows.put("숫자", "{\"pendingToken\":123}");
    rows.put("객체", "{\"pendingToken\":{}}");
    rows.put("배열", "{\"pendingToken\":[]}");
    rows.put("불리언", "{\"pendingToken\":true}");
    rows.put("JSON 아님", "not json");
    SoftAssertions soft = new SoftAssertions();

    for (Map.Entry<String, String> row : rows.entrySet()) {
      // When 표의 본문으로 호출한다
      MvcResult result = pending(row.getValue());
      // Then 400 INVALID_REQUEST 이다
      soft.assertThat(result.getResponse().getStatus()).as(row.getKey() + " 상태").isEqualTo(400);
      soft.assertThat(codeOf(result)).as(row.getKey() + " code").isEqualTo("INVALID_REQUEST");
    }
    // 본문이 비어 있음
    MvcResult empty = postEmpty(mockMvc, PENDING);
    soft.assertThat(empty.getResponse().getStatus()).as("본문 없음 상태").isEqualTo(400);
    soft.assertThat(codeOf(empty)).as("본문 없음 code").isEqualTo("INVALID_REQUEST");
    // And users·social_accounts 행 수가 그대로다
    soft.assertThat(counts()).as("행 수").isEqualTo(before);
    soft.assertAll();
  }
}
