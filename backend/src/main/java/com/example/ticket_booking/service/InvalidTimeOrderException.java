package com.example.ticket_booking.service;

public class InvalidTimeOrderException extends RuntimeException {

  public InvalidTimeOrderException() {
    super("openAt <= closeAt <= startAt 를 만족해야 합니다");
  }
}
