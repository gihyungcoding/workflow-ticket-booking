package com.example.ticket_booking.repository;

import com.example.ticket_booking.domain.Performance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TASK-001 시나리오 SC-10~SC-11 (Red).
 *
 * Performance 엔티티가 V1 마이그레이션의 NOT NULL/CHECK 제약을 애노테이션으로도
 * 표현하는지 검증한다 (F7, AC8). 애노테이션이 아직 없으므로 저장이 그대로
 * 성공해버려 두 테스트 모두 "예외를 기대했으나 아무것도 던져지지 않음"으로 실패한다.
 *
 * SoT: workflow_design/05_scenario/SCENARIO_TASK-001.md
 */
@DataJpaTest
class PerformanceConstraintTest {

    @Autowired
    private PerformanceRepository performanceRepository;

    @Test
    void test_sc10_필수_필드가_없는_공연은_저장이_거부된다() {
        Instant now = Instant.now();
        // Given 공연 데이터에서 title 이 null 이고, 나머지 필드는 모두 유효하다
        Performance performance = new Performance(
                null, "테스트홀",
                now.plus(1, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), now.plus(10, ChronoUnit.DAYS),
                100, 50, false);

        // When 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
        // Then 예외가 발생해 저장이 거부된다
        assertThatThrownBy(() -> performanceRepository.saveAndFlush(performance))
                .isInstanceOf(Exception.class);
    }

    @Test
    void test_sc11_availableSeats가_totalSeats보다_크면_저장이_거부된다() {
        Instant now = Instant.now();
        // Given 공연 데이터의 totalSeats = 10, availableSeats = 11 이다 (그 외 필드는 유효)
        Performance performance = new Performance(
                "공연", "테스트홀",
                now.plus(1, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS), now.plus(10, ChronoUnit.DAYS),
                10, 11, false);

        // When 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
        // Then 예외가 발생해 저장이 거부된다
        assertThatThrownBy(() -> performanceRepository.saveAndFlush(performance))
                .isInstanceOf(Exception.class);
    }
}
