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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
 * TASK-027 SC-26 — 종료를 기다리는 중 인터럽트되면 남은 메일을 버리고 인터럽트 상태를 지킨 채 예외 없이 돌아온다. 발송기를 멈추므로 이 클래스의 컨텍스트는 다른
 * 테스트와 공유하지 않는다({@code @DirtiesContext}).
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-027.md
 */
@SpringBootTest(
    properties = {"FRONTEND_BASE_URL=https://app.example/", "task027.context=interrupt"})
@Import({ClockTestConfig.class, CapturingMailConfig.class})
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class Task027MailShutdownInterruptTest {

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
   * SC-26 (error) 종료를 기다리는 중 인터럽트되면 남은 메일을 버리고 인터럽트 상태를 지킨 채 예외 없이 돌아온다.
   *
   * <p>Given 이 시나리오만의 컨텍스트, 메일 한 통에 2초, U1…U5(i1…i5@ticket.on)에게 보냈다 When 스레드 X 가 종료 처리를 부르고 300ms
   * 뒤 인터럽트 Then 인터럽트 뒤 3초 안에 예외 없이 돌아오고, X 의 인터럽트 상태 true, 5초 뒤 보낸 메일 5통 미만, 「인터럽트」가 든 WARN 줄
   */
  @Test
  void test_sc26_종료를_기다리는_중_인터럽트되면_남은_메일을_버리고_인터럽트_상태를_지킨_채_예외_없이_돌아온다(CapturedOutput output)
      throws Exception {
    // Given
    Task027Db db = new Task027Db(jdbc, userRepository);
    db.reset();
    gateway.reset();
    clock.setInstant(T);
    gateway.delay(2_000);
    for (int i = 1; i <= 5; i++) {
      accountMail.sendVerificationMail(db.emailUser("i" + i + "@ticket.on"));
    }

    // When
    AtomicReference<Throwable> thrown = new AtomicReference<>();
    AtomicBoolean interruptedAfter = new AtomicBoolean();
    Thread x =
        new Thread(
            () -> {
              try {
                accountMail.shutdown();
              } catch (Throwable throwable) {
                thrown.set(throwable);
              }
              interruptedAfter.set(Thread.currentThread().isInterrupted());
            },
            "sc26-shutdown");
    x.start();
    Thread.sleep(300);
    long interruptedAt = System.nanoTime();
    x.interrupt();
    x.join(10_000);
    long returnedWithinMillis = (System.nanoTime() - interruptedAt) / 1_000_000;
    Thread.sleep(5_000);

    // Then
    assertThat(x.isAlive()).as("X 가 돌아왔다").isFalse();
    assertThat(returnedWithinMillis).as("인터럽트 뒤 돌아오기까지(ms)").isLessThanOrEqualTo(3_000);
    assertThat(thrown.get()).as("예외 없음").isNull();
    assertThat(interruptedAfter.get()).as("인터럽트 상태").isTrue();
    assertThat(gateway.sent().size()).as("5초 뒤 보낸 메일 수").isLessThan(5);
    assertThat(output.getAll().lines().filter(l -> l.contains("WARN") && l.contains("인터럽트")))
        .as("「인터럽트」가 든 WARN 줄")
        .isNotEmpty();
  }
}
