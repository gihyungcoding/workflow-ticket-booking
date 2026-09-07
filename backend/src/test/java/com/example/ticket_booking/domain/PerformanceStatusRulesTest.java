package com.example.ticket_booking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * F9 — PerformanceStatusRules 단위 테스트 (Spring 컨텍스트 없음).
 *
 * <p>승인된 GWT 시나리오(SCENARIO_TASK-001.md의 SC-01~18) 중 어느 것에도 1:1 대응하지 않는 예외적 테스트다. HITL#1 재승인 시 사용자가
 * 승인한 방식 — Phase 4에서 발견된 CLOSED/UPCOMING 상호배타성 결함은 F8(openAt&lt;=closeAt) 도입 이후 API로는 재현할 수 없지만(그
 * 데이터 자체가 저장 거부됨), 이 클래스는 순수 함수라 DB 제약과 무관하게 malformed 데이터(openAt&gt;closeAt)로도 호출할 수 있다 — 마지막 픽스처가
 * 정확히 원래 버그의 모양을 재현해, F8에 기대지 않고 of()/matches()가 그 경우에도 일치함을 직접 증명한다. 다만 실제 JPA
 * Specification(PerformanceRepositoryImpl)과의 런타임 동치까지는 보장하지 않는다는 한계는 PLAN_TASK-001.json의 F9 항목에 남아
 * 있다.
 *
 * <p>SoT: workflow_design/04_plan/PLAN_TASK-001.json (F9), 이 클래스 자체가 근거다.
 */
class PerformanceStatusRulesTest {

  private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

  private record Fixture(
      PerformanceStatus expected,
      boolean cancelled,
      Instant openAt,
      Instant closeAt,
      int availableSeats) {}

  private static final List<Fixture> FIXTURES =
      List.of(
          new Fixture(
              PerformanceStatus.CANCELLED,
              true,
              NOW.minus(1, ChronoUnit.DAYS),
              NOW.plus(1, ChronoUnit.DAYS),
              5),
          new Fixture(
              PerformanceStatus.UPCOMING,
              false,
              NOW.plus(1, ChronoUnit.DAYS),
              NOW.plus(10, ChronoUnit.DAYS),
              5),
          new Fixture(
              PerformanceStatus.OPEN,
              false,
              NOW.minus(1, ChronoUnit.DAYS),
              NOW.plus(1, ChronoUnit.DAYS),
              5),
          new Fixture(
              PerformanceStatus.SOLD_OUT,
              false,
              NOW.minus(1, ChronoUnit.DAYS),
              NOW.plus(1, ChronoUnit.DAYS),
              0),
          new Fixture(
              PerformanceStatus.CLOSED,
              false,
              NOW.minus(10, ChronoUnit.DAYS),
              NOW.minus(1, ChronoUnit.DAYS),
              5),
          // openAt > closeAt — DB에서는 F8(CHECK openAt<=closeAt)이 막아 존재할 수 없는
          // 데이터지만, 순수 함수는 DB 제약과 무관하게 호출될 수 있다. Phase 4에서 발견된
          // 버그가 정확히 이 모양(now < openAt 이면서 now > closeAt)이었다 — of()는
          // UPCOMING을 우선하므로 UPCOMING이 맞고, matches()도 CLOSED에서 false를
          // 내야 한다(openAt<=now 조건 때문). F8에 기대지 않고 이 자리에서 직접 증명한다.
          new Fixture(
              PerformanceStatus.UPCOMING,
              false,
              NOW.plus(10, ChronoUnit.DAYS),
              NOW.minus(10, ChronoUnit.DAYS),
              5));

  @Test
  void 다섯_상태는_서로_배타적이다() {
    for (Fixture fixture : FIXTURES) {
      PerformanceStatus computed =
          PerformanceStatusRules.of(
              fixture.cancelled(),
              NOW,
              fixture.openAt(),
              fixture.closeAt(),
              fixture.availableSeats());
      assertThat(computed).as("fixture=%s", fixture).isEqualTo(fixture.expected());

      for (PerformanceStatus candidate : PerformanceStatus.values()) {
        boolean matches =
            PerformanceStatusRules.matches(
                candidate,
                fixture.cancelled(),
                NOW,
                fixture.openAt(),
                fixture.closeAt(),
                fixture.availableSeats());
        assertThat(matches)
            .as("fixture=%s candidate=%s", fixture, candidate)
            .isEqualTo(candidate == fixture.expected());
      }
    }
  }
}
