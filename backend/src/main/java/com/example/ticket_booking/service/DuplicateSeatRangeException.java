package com.example.ticket_booking.service;

public class DuplicateSeatRangeException extends RuntimeException {

  public DuplicateSeatRangeException() {
    super("두 구역의 행 범위가 겹칩니다");
  }
}
