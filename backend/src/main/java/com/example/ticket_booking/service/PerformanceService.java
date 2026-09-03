package com.example.ticket_booking.service;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.repository.PerformanceRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class PerformanceService {

  private final PerformanceRepository performanceRepository;
  private final Clock clock;

  public PerformanceService(PerformanceRepository performanceRepository, Clock clock) {
    this.performanceRepository = performanceRepository;
    this.clock = clock;
  }

  public PerformanceListResponse getPerformances(PerformanceStatus status, int page, int size) {
    Instant now = clock.instant();

    Specification<Performance> spec = notPastSpecification(now);
    if (status != null) {
      spec = spec.and(statusSpecification(status, now));
    }

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by("startAt").ascending());
    Page<Performance> result = performanceRepository.findAll(spec, pageRequest);

    List<PerformanceResponse> content =
        result.getContent().stream().map(performance -> toResponse(performance, now)).toList();

    return new PerformanceListResponse(
        content, result.getNumber(), result.getSize(), result.getTotalElements());
  }

  public PerformanceResponse getPerformance(Long id) {
    Performance performance =
        performanceRepository.findById(id).orElseThrow(() -> new PerformanceNotFoundException(id));
    return toResponse(performance, clock.instant());
  }

  private PerformanceResponse toResponse(Performance performance, Instant now) {
    return new PerformanceResponse(
        performance.getId(),
        performance.getTitle(),
        performance.getVenue(),
        performance.getStartAt(),
        performance.getOpenAt(),
        performance.getCloseAt(),
        performance.getTotalSeats(),
        performance.getAvailableSeats(),
        statusOf(performance, now));
  }

  /** 목록은 status 필터와 무관하게 항상 지난 공연(startAt < now)을 제외한다 (F4). */
  private Specification<Performance> notPastSpecification(Instant now) {
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startAt"), now);
  }

  /**
   * status 는 저장 컬럼이 아니라 계산값이라, {@link #statusOf} 의 5개 규칙을 그대로 WHERE 절 조건으로 옮긴다 (F3). 두 메서드는 같은 규칙을
   * 표현하므로 한쪽을 고치면 다른 쪽도 함께 고쳐야 한다.
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

  /** {@link #statusSpecification} 과 같은 규칙(§1-3)을 응답에 채울 값으로 계산한다. */
  private PerformanceStatus statusOf(Performance performance, Instant now) {
    if (Boolean.TRUE.equals(performance.getCancelled())) {
      return PerformanceStatus.CANCELLED;
    }
    if (now.isBefore(performance.getOpenAt())) {
      return PerformanceStatus.UPCOMING;
    }
    if (now.isAfter(performance.getCloseAt())) {
      return PerformanceStatus.CLOSED;
    }
    return performance.getAvailableSeats() > 0
        ? PerformanceStatus.OPEN
        : PerformanceStatus.SOLD_OUT;
  }
}
