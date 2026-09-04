package com.example.ticket_booking.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * F9 — PerformanceStatusRules 단위 테스트 (Spring 컨텍스트 없음).
 *
 * <p>승인된 GWT 시나리오(SCENARIO_TASK-001.md의 SC-01~18) 중 어느 것에도 1:1 대응하지
 * 않는 예외적 테스트다. HITL#1 재승인 시 사용자가 승인한 방식 — Phase 4에서 발견된
 * CLOSED/UPCOMING 상호배타성 결함이 F8(openAt&lt;=closeAt) 도입 이후에는 API로 재현할
 * 수 없어(그 데이터 자체가 저장 거부됨), 순수 규칙 자체의 자기 일관성을 이 테스트로
 * 대신 보장한다. JPA Specification과의 런타임 동치까지는 보장하지 않는다는 한계가
 * PLAN_TASK-001.json의 F9 항목에 명시되어 있다.
 *
 * <p>SoT: workflow_design/04_plan/PLAN_TASK-001.json (F9), 이 클래스 자체가 근거다.
 */
class PerformanceStatusRulesTest {

    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    private record Fixture(PerformanceStatus expected, boolean cancelled, Instant openAt, Instant closeAt,
            int availableSeats) {
    }

    private static final List<Fixture> FIXTURES = List.of(
            new Fixture(PerformanceStatus.CANCELLED, true,
                    NOW.minus(1, ChronoUnit.DAYS), NOW.plus(1, ChronoUnit.DAYS), 5),
            new Fixture(PerformanceStatus.UPCOMING, false,
                    NOW.plus(1, ChronoUnit.DAYS), NOW.plus(10, ChronoUnit.DAYS), 5),
            new Fixture(PerformanceStatus.OPEN, false,
                    NOW.minus(1, ChronoUnit.DAYS), NOW.plus(1, ChronoUnit.DAYS), 5),
            new Fixture(PerformanceStatus.SOLD_OUT, false,
                    NOW.minus(1, ChronoUnit.DAYS), NOW.plus(1, ChronoUnit.DAYS), 0),
            new Fixture(PerformanceStatus.CLOSED, false,
                    NOW.minus(10, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.DAYS), 5));

    @Test
    void 다섯_상태는_서로_배타적이다() {
        for (Fixture fixture : FIXTURES) {
            PerformanceStatus computed = PerformanceStatusRules.of(
                    fixture.cancelled(), NOW, fixture.openAt(), fixture.closeAt(), fixture.availableSeats());
            assertThat(computed).as("fixture=%s", fixture).isEqualTo(fixture.expected());

            for (PerformanceStatus candidate : PerformanceStatus.values()) {
                boolean matches = PerformanceStatusRules.matches(
                        candidate, fixture.cancelled(), NOW, fixture.openAt(), fixture.closeAt(),
                        fixture.availableSeats());
                assertThat(matches)
                        .as("fixture=%s candidate=%s", fixture, candidate)
                        .isEqualTo(candidate == fixture.expected());
            }
        }
    }
}
