package com.example.ticket_booking.service;

public class PerformanceCancelledException extends RuntimeException {

  public PerformanceCancelledException(Long id) {
    super("취소된 공연은 변경할 수 없습니다: " + id);
  }
}
