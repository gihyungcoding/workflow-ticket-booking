package com.example.ticket_booking.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ticket_booking.domain.Performance;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * TASK-001 시나리오 SC-10~SC-11, SC-15~SC-18.
 *
 * <p>Performance 엔티티가 V1 마이그레이션의 NOT NULL/CHECK/길이 제약을 애노테이션으로도
 * 표현하는지 검증한다 (F7/F8, AC8/AC9/AC10). SC-15/16은 attempt 2에서 발견된 결함(F8
 * 불변조건, 컬럼 길이) 대응이라 아직 애노테이션이 없어 Red다. SC-17/18은 그 제약의 양성
 * 대조군으로, 제약이 아직 없는 현재 코드에서도 이미 저장에 성공하므로 처음부터 Green이다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-001.md
 */
@DataJpaTest
class PerformanceConstraintTest {

  @Autowired private PerformanceRepository performanceRepository;

  @Autowired private TestEntityManager entityManager;

  private Performance performance(
      String title, Instant openAt, Instant closeAt, int totalSeats, int availableSeats) {
    Instant now = Instant.now();
    return new Performance(
        title, "테스트홀", now.plus(1, ChronoUnit.DAYS), openAt, closeAt, totalSeats,
        availableSeats, false);
  }

  @Test
  void test_sc10_필수_필드가_없는_공연은_저장이_거부된다() {
    Instant now = Instant.now();
    // Given 공연 데이터에서 title 이 null 이고, 나머지 필드는 모두 유효하다
    Performance performance =
        new Performance(
            null,
            "테스트홀",
            now.plus(1, ChronoUnit.DAYS),
            now.minus(1, ChronoUnit.DAYS),
            now.plus(10, ChronoUnit.DAYS),
            100,
            50,
            false);

    // When 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
    // Then DataIntegrityViolationException(또는 그 하위 타입)이 발생해 저장이 거부된다
    assertThatThrownBy(() -> performanceRepository.saveAndFlush(performance))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void test_sc11_availableSeats가_totalSeats보다_크면_저장이_거부된다() {
    Instant now = Instant.now();
    // Given 공연 데이터의 totalSeats = 10, availableSeats = 11 이다 (그 외 필드는 유효)
    Performance performance =
        new Performance(
            "공연",
            "테스트홀",
            now.plus(1, ChronoUnit.DAYS),
            now.minus(1, ChronoUnit.DAYS),
            now.plus(10, ChronoUnit.DAYS),
            10,
            11,
            false);

    // When 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
    // Then DataIntegrityViolationException(또는 그 하위 타입)이 발생해 저장이 거부된다
    assertThatThrownBy(() -> performanceRepository.saveAndFlush(performance))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void test_sc15_openAt가_closeAt보다_늦으면_저장이_거부된다() {
    Instant now = Instant.now();
    // Given 공연 데이터의 openAt 이 closeAt 보다 1일 늦다 (그 외 필드는 유효)
    Performance performance = performance("공연", now.plus(11, ChronoUnit.DAYS), now.plus(10, ChronoUnit.DAYS), 100, 50);

    // When 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
    // Then DataIntegrityViolationException(또는 그 하위 타입)이 발생해 저장이 거부된다
    assertThatThrownBy(() -> performanceRepository.saveAndFlush(performance))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void test_sc16_title이_200자를_초과하면_저장이_거부된다() {
    Instant now = Instant.now();
    // Given 공연 데이터의 title 이 201자이다 (그 외 필드는 유효)
    String title = "가".repeat(201);
    Performance performance = performance(title, now.minus(1, ChronoUnit.DAYS), now.plus(10, ChronoUnit.DAYS), 100, 50);

    // When 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
    // Then DataIntegrityViolationException(또는 그 하위 타입)이 발생해 저장이 거부된다
    assertThatThrownBy(() -> performanceRepository.saveAndFlush(performance))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void test_sc17_title이_정확히_200자면_저장된다() {
    Instant now = Instant.now();
    // Given 공연 데이터의 title 이 정확히 200자이다 (그 외 필드는 유효)
    String title = "가".repeat(200);
    Performance performance = performance(title, now.minus(1, ChronoUnit.DAYS), now.plus(10, ChronoUnit.DAYS), 100, 50);

    // When 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
    Long id = performanceRepository.saveAndFlush(performance).getId();
    entityManager.clear();

    // Then 예외 없이 저장된다
    // Then 저장된 데이터를 다시 조회하면 title 이 200자 그대로 보존되어 있다
    Performance reloaded = performanceRepository.findById(id).orElseThrow();
    assertThat(reloaded.getTitle()).hasSize(200).isEqualTo(title);
  }

  @Test
  void test_sc18_openAt이_closeAt과_같으면_저장된다() {
    Instant now = Instant.now();
    // Given 공연 데이터의 openAt 과 closeAt 이 정확히 같은 시각이다 (그 외 필드는 유효)
    Instant sameInstant = now.plus(5, ChronoUnit.DAYS);
    Performance performance = performance("공연", sameInstant, sameInstant, 100, 50);

    // When 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
    Long id = performanceRepository.saveAndFlush(performance).getId();
    entityManager.clear();

    // Then 예외 없이 저장된다
    // Then 저장된 데이터를 다시 조회하면 openAt 과 closeAt 이 입력한 값 그대로 보존되어 있다
    Performance reloaded = performanceRepository.findById(id).orElseThrow();
    assertThat(reloaded.getOpenAt()).isEqualTo(sameInstant);
    assertThat(reloaded.getCloseAt()).isEqualTo(sameInstant);
  }
}
