package com.example.ticket_booking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.CapturingMailConfig;
import com.example.ticket_booking.support.CapturingMailGateway;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import com.example.ticket_booking.support.PostgresTestContainer;
import com.example.ticket_booking.support.Task027Db;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * TASK-027 SC-24 — 종료할 때 대기 중인 메일을 보내고, 종료 뒤의 발송 요청은 호출자에게 예외를 전하지 않는다. 발송기를 멈추므로 이 클래스의 컨텍스트는 다른
 * 테스트와 공유하지 않는다({@code @DirtiesContext}). 「종료 절차」는 컨텍스트 닫기에서 불리는 {@code AccountMailService} 의
 * {@code @PreDestroy} 를 직접 부르는 것으로 만든다(같은 패키지).
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-027.md
 */
@SpringBootTest(properties = "FRONTEND_BASE_URL=https://app.example/")
@Import({ClockTestConfig.class, CapturingMailConfig.class})
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class Task027MailShutdownTest {

  private static final Instant T = Instant.parse("2026-11-20T09:00:00Z");

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registry) {
    PostgresTestContainer.register(registry);
  }

  @Autowired private AccountMailService accountMail;
  @Autowired private CapturingMailGateway gateway;
  @Autowired private MutableClock clock;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private UserRepository userRepository;

  /**
   * SC-24 (error) 종료할 때 대기 중인 메일을 보내고 종료 뒤의 발송 요청은 호출자에게 예외를 전하지 않는다.
   *
   * <p>Given 이 시나리오만의 컨텍스트, 메일 한 통에 200ms, 사용자 U1…U6(d1…d6@ticket.on) When U1…U5 발송 직후 종료 처리를 직접
   * 부르고, 돌아온 뒤 U6 발송 Then 종료 처리가 돌아온 시점에 5통, U6 호출 true·예외 없음, d6 메일은 5초 뒤에도 없음, WARN 1줄에
   * d***@ticket.on
   */
  @Test
  void test_sc24_종료할_때_대기_중인_메일을_보내고_종료_뒤의_발송_요청은_호출자에게_예외를_전하지_않는다(CapturedOutput output)
      throws Exception {
    // Given
    Task027Db db = new Task027Db(jdbc, userRepository);
    db.reset();
    gateway.reset();
    clock.setInstant(T);
    gateway.delay(200);
    List<Long> users = new ArrayList<>();
    for (int i = 1; i <= 6; i++) {
      users.add(db.emailUser("d" + i + "@ticket.on"));
    }

    // When
    for (int i = 0; i < 5; i++) {
      accountMail.sendVerificationMail(users.get(i));
    }
    accountMail.shutdown();
    int sentWhenShutdownReturned = gateway.sent().size();
    Object afterShutdown;
    try {
      afterShutdown = accountMail.sendVerificationMail(users.get(5));
    } catch (Throwable thrown) {
      afterShutdown = thrown;
    }
    Thread.sleep(5_000);

    // Then
    assertThat(sentWhenShutdownReturned).as("종료 처리가 돌아온 시점의 메일 수").isEqualTo(5);
    assertThat(afterShutdown).as("종료 뒤 호출").isEqualTo(true);
    assertThat(gateway.sentTo("d6@ticket.on")).as("d6 메일").isEmpty();
    List<String> warnLines =
        output
            .getAll()
            .lines()
            .filter(line -> line.contains("WARN") && line.contains("d***@ticket.on"))
            .toList();
    assertThat(warnLines).as("WARN 줄").hasSize(1);
  }
}
