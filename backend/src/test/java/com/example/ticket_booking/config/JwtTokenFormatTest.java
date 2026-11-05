package com.example.ticket_booking.config;

import com.example.ticket_booking.domain.SocialProvider;
import com.example.ticket_booking.domain.UserRole;
import com.example.ticket_booking.support.PendingTokenTestHelper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

/**
 * TASK-016 시나리오 SC-16 — 토큰 검증은 형식 밖 문자가 붙거나 섞인 토큰을 받지 않는다(Phase 4 RC-2). 헤더 형식 검사가 먼저 막는 문자도 검증 자체가
 * 받지 않아야 다른 입구가 생겨도 같은 결과다. 관찰 대상은 검증 결과(확인된 사용자 id / 대기 정보가 있는가)다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-016.md
 */
class JwtTokenFormatTest {

  private static final String SECRET = "test-only-jwt-secret-0123456789abcdef";
  private static final Instant NOW = Instant.parse("2026-11-11T08:35:00Z");

  private final JwtTokenProvider provider =
      JwtTokenProvider.fromEnv(SECRET, Clock.fixed(NOW, ZoneOffset.UTC));

  /** 표의 행 — 정상 토큰을 받아 바꾼 토큰을 돌려준다. */
  private static Map<String, UnaryOperator<String>> rows() {
    Map<String, UnaryOperator<String>> rows = new LinkedHashMap<>();
    for (String suffix : new String[] {"=", "==", "~", "!", " ", "\n", "\u0000", "é", "\ud800"}) {
      String label = "끝에 " + suffix.replace("\n", "LF").replace("\u0000", "NUL").replace(" ", "공백");
      rows.put(label, token -> token + suffix);
    }
    rows.put(
        "서명 마디 가운데 NUL", token -> PendingTokenTestHelper.insertIntoSegment(token, 2, "\u0000"));
    rows.put(
        "페이로드 마디 가운데 NUL", token -> PendingTokenTestHelper.insertIntoSegment(token, 1, "\u0000"));
    rows.put("마디가 넷 (.abc)", token -> token + ".abc");
    return rows;
  }

  /** 정상 토큰의 페이로드 앞에 같은 키를 하나 더 넣어 다시 서명한다. */
  private static String withDuplicateKey(String token, String firstEntry) {
    String payload =
        new String(
            java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]),
            java.nio.charset.StandardCharsets.UTF_8);
    return PendingTokenTestHelper.signedJson(SECRET, "{" + firstEntry + "," + payload.substring(1));
  }

  @Test
  void test_sc16_토큰_검증은_형식_밖_문자가_붙거나_섞인_토큰을_받지_않는다() {
    // Given 정상 accessToken a(사용자 id 확인됨)와 정상 pendingToken p(대기 정보 확인됨)가 있다
    String a = provider.issueAccessToken(7L, "member@example.com", UserRole.AUDIENCE);
    String p =
        provider.issuePendingToken("harin.kim@kakao.com", "김하린", SocialProvider.KAKAO, "k-1", true);
    SoftAssertions soft = new SoftAssertions();

    // When 그대로 넣으면 Then 검증 결과가 있다
    soft.assertThat(provider.verifyAccessToken(a)).as("access 그대로").contains(7L);
    soft.assertThat(provider.verifyPendingToken(p)).as("pending 그대로").isPresent();

    // When 표의 값을 넣으면 Then 검증 결과가 비어 있다
    for (Map.Entry<String, UnaryOperator<String>> row : rows().entrySet()) {
      soft.assertThat(provider.verifyAccessToken(row.getValue().apply(a)))
          .as("access " + row.getKey())
          .isEmpty();
      soft.assertThat(provider.verifyPendingToken(row.getValue().apply(p)))
          .as("pending " + row.getKey())
          .isEmpty();
    }
    // 서명은 맞지만 페이로드에 같은 키가 두 번
    long exp = NOW.plusSeconds(100).getEpochSecond();
    soft.assertThat(provider.verifyAccessToken(withDuplicateKey(a, "\"exp\":" + exp)))
        .as("access 중복 exp")
        .isEmpty();
    soft.assertThat(provider.verifyAccessToken(withDuplicateKey(a, "\"typ\":\"access\"")))
        .as("access 중복 typ")
        .isEmpty();
    soft.assertThat(provider.verifyPendingToken(withDuplicateKey(p, "\"exp\":" + exp)))
        .as("pending 중복 exp")
        .isEmpty();
    soft.assertThat(provider.verifyPendingToken(withDuplicateKey(p, "\"typ\":\"pending\"")))
        .as("pending 중복 typ")
        .isEmpty();
    soft.assertAll();
  }
}
