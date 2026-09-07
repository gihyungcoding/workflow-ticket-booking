package com.example.ticket_booking.service;

import java.time.Instant;

/**
 * 예매 상태 판정 규칙(F2)의 단일 출처. Spring 컨텍스트 없이 상호배타성을 단위 테스트할 수 있도록 순수 정적 메서드로 분리한다 (F9). JPA
 * Specification(PerformanceRepository 조회 조건)과 런타임 코드를 공유하지는 않는다 — 그 동치는 SC-02/06/12/13/14 같은 HTTP
 * 레벨 시나리오가 담당한다.
 */
public final class PerformanceStatusRules {

  private PerformanceStatusRules() {}

  public static PerformanceStatus of(
      boolean cancelled, Instant now, Instant openAt, Instant closeAt, int availableSeats) {
    if (cancelled) {
      return PerformanceStatus.CANCELLED;
    }
    if (now.isBefore(openAt)) {
      return PerformanceStatus.UPCOMING;
    }
    if (now.isAfter(closeAt)) {
      return PerformanceStatus.CLOSED;
    }
    return availableSeats > 0 ? PerformanceStatus.OPEN : PerformanceStatus.SOLD_OUT;
  }

  /**
   * of() 를 호출하지 않고 상태별 조건을 독립적으로 다시 표현한다 — of()의 우선순위 분기 (CANCELLED > UPCOMING > CLOSED >
   * OPEN/SOLD_OUT)를 각 분기에 "이전 우선순위가 아님" 조건으로 풀어 썼다. CLOSED 분기가 openAt<=now 를 포함하는 것이 핵심이다 — F8
   * (openAt<=closeAt) 불변조건이 없어도(malformed 데이터라도) of() 와 항상 일치한다. 두 메서드가 서로 다른 방식으로 같은 규칙을 표현하므로, 이
   * 클래스의 단위 테스트가 실제로 두 구현을 교차 검증한다.
   */
  public static boolean matches(
      PerformanceStatus status,
      boolean cancelled,
      Instant now,
      Instant openAt,
      Instant closeAt,
      int availableSeats) {
    return switch (status) {
      case CANCELLED -> cancelled;
      case UPCOMING -> !cancelled && now.isBefore(openAt);
      case OPEN ->
          !cancelled && !now.isBefore(openAt) && !now.isAfter(closeAt) && availableSeats > 0;
      case SOLD_OUT ->
          !cancelled && !now.isBefore(openAt) && !now.isAfter(closeAt) && availableSeats == 0;
      case CLOSED -> !cancelled && !now.isBefore(openAt) && now.isAfter(closeAt);
    };
  }
}
