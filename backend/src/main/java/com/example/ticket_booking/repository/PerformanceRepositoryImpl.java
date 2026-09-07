package com.example.ticket_booking.repository;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.domain.PerformanceStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

/**
 * PerformanceRepositoryCustom 구현. Spring Data JPA의 "{리포지토리명}Impl" 관례로 자동 인식되어
 * PerformanceRepository에 합성된다.
 *
 * <p>Specification/CriteriaBuilder 조립은 이 클래스(Repository 계층) 안에만 있다 — Service는 이 타입들을 알 필요가 없다
 * (ARCH-003).
 */
public class PerformanceRepositoryImpl extends SimpleJpaRepository<Performance, Long>
    implements PerformanceRepositoryCustom {

  public PerformanceRepositoryImpl(EntityManager entityManager) {
    super(Performance.class, entityManager);
  }

  @Override
  public Page<Performance> findVisiblePerformances(
      PerformanceStatus status, Instant now, Pageable pageable) {
    Specification<Performance> spec = notPastSpecification(now);
    if (status != null) {
      spec = spec.and(statusSpecification(status, now));
    }
    return findAll(spec, pageable);
  }

  /** 목록은 status 필터와 무관하게 항상 지난 공연(startAt < now)을 제외한다 (F4). */
  private Specification<Performance> notPastSpecification(Instant now) {
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startAt"), now);
  }

  /**
   * status 는 저장 컬럼이 아니라 계산값이라, {@link com.example.ticket_booking.domain.PerformanceStatusRules#of}
   * 의 5개 규칙을 WHERE 절 조건으로 옮긴다 (F3). CLOSED 분기는 openAt<=now 조건을 명시하지 않지만, F8(엔티티/마이그레이션의 CHECK
   * open_at<=close_at)이 항상 성립하는 한 "closeAt<now" 만으로 "openAt<=now AND
   * closeAt<now"(PerformanceStatusRules의 CLOSED 조건)와 동치다 — closeAt<now 이고 openAt<=closeAt 이면
   * openAt<now 가 산술적으로 따라온다. F8이 깨지면(제약을 제거하면) 이 동치도 깨지므로 함께 고려한다.
   */
  private Specification<Performance> statusSpecification(PerformanceStatus status, Instant now) {
    return switch (status) {
      case CANCELLED -> (root, query, cb) -> cb.isTrue(root.get("cancelled"));
      case UPCOMING ->
          (root, query, cb) ->
              cb.and(cb.isFalse(root.get("cancelled")), cb.greaterThan(root.get("openAt"), now));
      case OPEN ->
          (root, query, cb) ->
              cb.and(
                  cb.isFalse(root.get("cancelled")),
                  cb.lessThanOrEqualTo(root.get("openAt"), now),
                  cb.greaterThanOrEqualTo(root.get("closeAt"), now),
                  cb.greaterThan(root.get("availableSeats"), 0));
      case SOLD_OUT ->
          (root, query, cb) ->
              cb.and(
                  cb.isFalse(root.get("cancelled")),
                  cb.lessThanOrEqualTo(root.get("openAt"), now),
                  cb.greaterThanOrEqualTo(root.get("closeAt"), now),
                  cb.equal(root.get("availableSeats"), 0));
      case CLOSED ->
          (root, query, cb) ->
              cb.and(cb.isFalse(root.get("cancelled")), cb.lessThan(root.get("closeAt"), now));
    };
  }
}
