package com.example.ticket_booking.domain;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/**
 * V1__create_performance.sql 의 NOT NULL/CHECK/길이 제약을 애노테이션으로도 표현한다 (F7/F8, AC8/AC9/AC10). 테스트는 이
 * 애노테이션에서 스키마를 생성하므로(ADR-0006) 마이그레이션과 어긋나지 않게 유지한다.
 *
 * <p>open_at &lt;= close_at 제약(F8)은 Phase 4에서 발견된 결함(CLOSED 상태 판정이 openAt&gt;closeAt 데이터에서 모순되는
 * 문제)의 근본 해결책이다 — 이 제약이 성립하면 PerformanceService의 statusSpecification() CLOSED 분기는 코드를 고치지 않아도 올바르다
 * (해당 클래스의 주석 참고).
 */
@Entity
@Table(
    name = "performance",
    check =
        @CheckConstraint(
            name = "performance_seats_check",
            constraint =
                "total_seats > 0 AND available_seats >= 0 AND available_seats <= total_seats"
                    + " AND open_at <= close_at"))
public class Performance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, length = 200)
  private String venue;

  @Column(name = "start_at", nullable = false)
  private Instant startAt;

  @Column(name = "open_at", nullable = false)
  private Instant openAt;

  @Column(name = "close_at", nullable = false)
  private Instant closeAt;

  @Column(name = "total_seats", nullable = false)
  private Integer totalSeats;

  @Column(name = "available_seats", nullable = false)
  private Integer availableSeats;

  @Column(nullable = false)
  private Boolean cancelled;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Performance() {}

  public Performance(
      String title,
      String venue,
      Instant startAt,
      Instant openAt,
      Instant closeAt,
      Integer totalSeats,
      Integer availableSeats,
      Boolean cancelled) {
    this.title = title;
    this.venue = venue;
    this.startAt = startAt;
    this.openAt = openAt;
    this.closeAt = closeAt;
    this.totalSeats = totalSeats;
    this.availableSeats = availableSeats;
    this.cancelled = cancelled;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getVenue() {
    return venue;
  }

  public Instant getStartAt() {
    return startAt;
  }

  public Instant getOpenAt() {
    return openAt;
  }

  public Instant getCloseAt() {
    return closeAt;
  }

  public Integer getTotalSeats() {
    return totalSeats;
  }

  public Integer getAvailableSeats() {
    return availableSeats;
  }

  public Boolean getCancelled() {
    return cancelled;
  }
}
