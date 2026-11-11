package com.example.ticket_booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** 공연 등급 하나의 구역 위치 — V6__add_poster_and_zones.sql 의 performance_zone. */
@Entity
@Table(name = "performance_zone")
public class PerformanceZone {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "performance_id")
  private Performance performance;

  private String grade;

  private String area;

  @Column(name = "area_note")
  private String areaNote;

  @Column(name = "stage_order")
  private Integer stageOrder;

  protected PerformanceZone() {}
}
