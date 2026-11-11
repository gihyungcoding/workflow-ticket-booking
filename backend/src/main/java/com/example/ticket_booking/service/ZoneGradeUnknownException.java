package com.example.ticket_booking.service;

public class ZoneGradeUnknownException extends RuntimeException {

  public ZoneGradeUnknownException(String grade) {
    super("zones 의 등급 '" + grade + "' 이 sections 에 없습니다");
  }
}
