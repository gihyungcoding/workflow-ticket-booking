package com.example.ticket_booking.service;

import java.time.Instant;

/**
 * 예매 상태 판정 규칙(F2)의 단일 출처. Spring 컨텍스트 없이 상호배타성을 단위 테스트할 수
 * 있도록 순수 정적 메서드로 분리한다 (F9). JPA Specification(PerformanceRepository 조회
 * 조건)과 런타임 코드를 공유하지는 않는다 — 그 동치는 SC-02/06/12/13/14 같은 HTTP 레벨
 * 시나리오가 담당한다.
 */
public final class PerformanceStatusRules {

    private PerformanceStatusRules() {
    }

    public static PerformanceStatus of(boolean cancelled, Instant now, Instant openAt, Instant closeAt,
            int availableSeats) {
        throw new UnsupportedOperationException();
    }

    public static boolean matches(PerformanceStatus status, boolean cancelled, Instant now, Instant openAt,
            Instant closeAt, int availableSeats) {
        throw new UnsupportedOperationException();
    }
}
