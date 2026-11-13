package com.example.ticket_booking.service;

public class VisibilityLockedException extends RuntimeException {

  public VisibilityLockedException(Long id) {
    super("예매가 열린 뒤에는 공개 여부를 바꿀 수 없습니다: " + id);
  }
}
