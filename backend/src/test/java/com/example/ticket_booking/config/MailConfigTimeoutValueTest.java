package com.example.ticket_booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * TASK-027 Phase 3 자체 리뷰 3회차(RC-2 변종) — 타임아웃 값 규칙(ASCII 숫자, 1 이상 int 최대 이하)은 앞에 붙은 0 의 개수와 무관하다.
 * 시나리오 SC-22 의 규칙 안 경계라 승인 표를 고치지 않고 단위 테스트로 지킨다(pre_review.tests_added).
 */
class MailConfigTimeoutValueTest {

  @Test
  void 앞에_0이_붙어_10자를_넘어도_범위_안의_정수면_그_값을_쓴다() {
    // Given
    StandardEnvironment environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "test",
                Map.of(
                    "spring.mail.properties.mail.smtp.timeout", "00000001000",
                    "spring.mail.properties.mail.smtp.connectiontimeout", "0000000002147483647")));
    JavaMailSenderImpl sender = new JavaMailSenderImpl();

    // When
    MailConfig.applyTimeouts(environment, sender, "smtp");

    // Then
    assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.timeout")).isEqualTo("1000");
    assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.connectiontimeout"))
        .isEqualTo("2147483647");
    assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.writetimeout"))
        .isEqualTo("10000");
  }
}
