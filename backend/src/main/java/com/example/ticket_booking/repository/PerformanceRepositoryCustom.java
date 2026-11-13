package com.example.ticket_booking.repository;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.domain.PerformanceFilter;
import com.example.ticket_booking.domain.PerformanceStatus;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service가 도메인 언어(상태, 시각, 페이지 정보)로만 호출할 수 있도록 쿼리 조립을 Repository 계층에 가둔다 (architecture.md §2 — "쿼리"는
 * Repository 책임, ARCH-003).
 */
public interface PerformanceRepositoryCustom {

  Page<Performance> findVisiblePerformances(
      PerformanceStatus status, Instant now, Pageable pageable);

  /**
   * 요청자가 소유한 공연을 공개 여부와 무관하게, 지난 공연도 포함해 돌려준다. filter 의 상태 조건은 관객 조회와 같은 statusSpecification 을 쓴다.
   */
  Page<Performance> findOwnedPerformances(
      Long ownerId, PerformanceFilter filter, Instant now, Pageable pageable);

  /** 아직 열리지 않았고 지나지 않은 공연 중 openAt 이 가장 이른 것(같으면 id 가 작은 것). */
  Optional<Performance> findNextOpening(Instant now);
}
