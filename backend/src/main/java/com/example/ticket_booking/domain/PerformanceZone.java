package com.example.ticket_booking.domain;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 공연 등급 하나의 구역 위치 — 묶음 이름(area)과 무대에서 가까운 순서(stageOrder). V6__add_poster_and_zones.sql 의
 * UNIQUE·CHECK·길이를 애노테이션으로도 표현한다 — 테스트는 이 애노테이션에서 스키마를 만든다(ADR-0006).
 */
@Entity
@Table(
    name = "performance_zone",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"performance_id", "grade"}),
      @UniqueConstraint(columnNames = {"performance_id", "stage_order"})
    },
    check =
        @CheckConstraint(
            name = "performance_zone_stage_order_check",
            constraint = "stage_order > 0"))
public class PerformanceZone {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "performance_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Performance performance;

  @Column(nullable = false, length = 20)
  private String grade;

  @Column(nullable = false, length = 30)
  private String area;

  @Column(name = "area_note", length = 60)
  private String areaNote;

  @Column(name = "stage_order", nullable = false)
  private Integer stageOrder;

  protected PerformanceZone() {}

  public PerformanceZone(
      Performance performance, String grade, String area, String areaNote, Integer stageOrder) {
    this.performance = performance;
    this.grade = grade;
    this.area = area;
    this.areaNote = areaNote;
    this.stageOrder = stageOrder;
  }

  public String getGrade() {
    return grade;
  }

  public String getArea() {
    return area;
  }

  public String getAreaNote() {
    return areaNote;
  }

  public Integer getStageOrder() {
    return stageOrder;
  }
}
