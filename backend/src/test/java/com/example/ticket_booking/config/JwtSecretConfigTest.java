package com.example.ticket_booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticket_booking.TicketBookingApplication;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * TASK-009 시나리오 SC-16.
 *
 * <p>JWT_SECRET 값 경계는 JwtTokenProvider.fromEnv 로, "기동 자체가 실패한다" 는 실제 애플리케이션을 빈 값으로 띄워 확인한다. 환경변수가
 * "미설정"인 행은 테스트 리소스가 값을 채워 두므로 null 로 팩토리에 직접 넘겨 대신한다 — @Value 기본값(빈 문자열)과 같은 경로다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-009.md
 */
class JwtSecretConfigTest {

  private record Row(String label, String secret, boolean accepted) {}

  @Test
  void test_sc16_JWT_비밀키는_UTF8_32바이트_이상이_아니면_기동하지_않는다() {
    // Given 애플리케이션을 JWT_SECRET 값을 바꿔 가며 기동한다
    List<Row> rows =
        List.of(
            new Row("미설정(null)", null, false),
            new Row("빈 문자열", "", false),
            new Row("공백만 32칸", " ".repeat(32), false),
            new Row("ASCII 31자(31바이트)", "a".repeat(31), false),
            new Row("ASCII 32자(32바이트)", "a".repeat(32), true),
            new Row("한글 11자(33바이트)", "가".repeat(11), true));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      // When 해당 값으로 기동(비밀키 검증)을 시도한다
      if (row.accepted()) {
        // Then 32바이트 이상이면 기동 성공이다
        soft.assertThatCode(() -> JwtTokenProvider.fromEnv(row.secret(), Clock.systemUTC()))
            .as(row.label())
            .doesNotThrowAnyException();
      } else {
        // Then 기동 실패이고 오류 메시지에 JWT_SECRET 이 들어 있다
        soft.assertThatThrownBy(() -> JwtTokenProvider.fromEnv(row.secret(), Clock.systemUTC()))
            .as(row.label())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET");
      }
    }

    // And 실제 애플리케이션을 빈 JWT_SECRET 으로 띄우면 기동이 실패하고 예외 체인에 JWT_SECRET 이 들어 있다
    ConfigurableApplicationContext context = null;
    Throwable failure = null;
    try {
      context =
          new SpringApplicationBuilder(TicketBookingApplication.class)
              .web(WebApplicationType.NONE)
              .run(
                  "--JWT_SECRET=",
                  "--spring.datasource.url=jdbc:h2:mem:jwt-secret-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
    } catch (Throwable thrown) {
      failure = thrown;
    } finally {
      if (context != null) {
        context.close();
      }
    }
    soft.assertThat(failure).as("빈 JWT_SECRET 으로 기동하면 실패해야 한다").isNotNull();
    if (failure != null) {
      List<String> messages = new ArrayList<>();
      for (Throwable t = failure; t != null; t = t.getCause()) {
        messages.add(String.valueOf(t.getMessage()));
      }
      assertThat(String.join(" | ", messages)).as("예외 체인 메시지").contains("JWT_SECRET");
    }
    soft.assertAll();
  }
}
