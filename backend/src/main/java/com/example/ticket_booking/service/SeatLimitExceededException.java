package com.example.ticket_booking.service;

public class SeatLimitExceededException extends RuntimeException {

  public SeatLimitExceededException(long totalSeats) {
    super("좌석 총수는 5,000을 넘을 수 없습니다: " + totalSeats);
  }
}
