package com.example.ticket_booking.domain;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/**
 * V2__create_seat.sql 의 NOT NULL/CHECK/UNIQUE 제약을 애노테이션으로도 표현한다 (ADR-0009). 등록 시 구역(등급)×행×열
 * 조합마다 하나씩 생성되며, seatLabel 은 Service 가 계산해 넘긴다 — 이 엔티티 자체는 계산하지 않는다.
 */
@Entity
@Table(
    name = "seat",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"performance_id", "seat_row", "seat_number"}),
    check =
        @CheckConstraint(
            name = "seat_seat_number_check",
            constraint = "seat_number > 0 AND price >= 0"))
public class Seat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "performance_id", nullable = false)
  private Long performanceId;

  @Column(nullable = false, length = 50)
  private String grade;

  @Column(name = "seat_row", nullable = false, length = 10)
  private String seatRow;

  @Column(name = "seat_number", nullable = false)
  private Integer seatNumber;

  @Column(name = "seat_label", nullable = false, length = 30)
  private String seatLabel;

  @Column(nullable = false)
  private Integer price;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Seat() {}

  public Seat(
      Long performanceId,
      String grade,
      String seatRow,
      Integer seatNumber,
      String seatLabel,
      Integer price) {
    this.performanceId = performanceId;
    this.grade = grade;
    this.seatRow = seatRow;
    this.seatNumber = seatNumber;
    this.seatLabel = seatLabel;
    this.price = price;
  }

  public Long getId() {
    return id;
  }

  public Long getPerformanceId() {
    return performanceId;
  }

  public String getGrade() {
    return grade;
  }

  public String getSeatRow() {
    return seatRow;
  }

  public Integer getSeatNumber() {
    return seatNumber;
  }

  public String getSeatLabel() {
    return seatLabel;
  }

  public Integer getPrice() {
    return price;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
