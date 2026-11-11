package com.example.ticket_booking.service;

public class ZoneGradeDuplicatedException extends RuntimeException {

  public ZoneGradeDuplicatedException(String grade) {
    super("zones 에 등급 '" + grade + "' 이 두 번 이상 있습니다");
  }
}
