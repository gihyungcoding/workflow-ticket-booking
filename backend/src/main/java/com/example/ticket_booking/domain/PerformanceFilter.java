package com.example.ticket_booking.domain;

/** 주최자 목록의 필터. DRAFT 는 취소되지 않은 비공개 공연이고, 나머지 상태는 공개 여부와 무관하게 {@link PerformanceStatus} 와 같다. */
public enum PerformanceFilter {
  ALL,
  DRAFT,
  UPCOMING,
  OPEN,
  SOLD_OUT,
  CLOSED,
  CANCELLED
}
