package com.example.ticket_booking.config;

import com.example.ticket_booking.support.AppleTestKeys;
import com.example.ticket_booking.support.MutableClock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

/**
 * TASK-012 시나리오 SC-01.
 *
 * <p>테스트가 만든 EC P-256 키 쌍으로 Client Secret 을 만들어 구현과 무관하게 Nimbus 로 해석·검증한다. 시각은 MutableClock 으로
 * 고정한다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-012.md
 */
class AppleClientSecretGeneratorTest {

  private record Row(String now, long iat) {}

  @Test
  void test_sc01_client_secret_은_es256_으로_서명된_jwt_다() {
    // Given 테스트가 만든 EC P-256 개인 키로 팀 ID·클라이언트 ID·키 ID 를 설정했다
    MutableClock clock = new MutableClock(Instant.parse("2026-10-27T00:00:00Z"), ZoneOffset.UTC);
    AppleClientSecretGenerator generator =
        new AppleClientSecretGenerator(
            AppleTestKeys.TEAM_ID,
            AppleTestKeys.CLIENT_ID,
            AppleTestKeys.KEY_ID,
            AppleTestKeys.privateKey(),
            clock);
    Row[] rows = {
      new Row("2026-10-27T00:00:00Z", 1793059200L), new Row("2026-10-27T00:10:00Z", 1793059800L)
    };
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      // And 현재 시각은 표의 시각이다
      clock.setInstant(Instant.parse(row.now()));

      // When Client Secret 을 생성한다
      String secret = generator.generate();

      // Then 세 부분·alg·kid·iss·sub·aud(문자열)·iat·exp·64바이트 서명·공개 키 검증(다른 키는 실패)
      AppleTestKeys.assertClientSecret(soft, row.now(), secret, row.iat());
    }
    soft.assertAll();
  }
}
