package com.example.ticket_booking.service;

public class RegistrationAlreadyOpenException extends RuntimeException {

  public RegistrationAlreadyOpenException(Long id) {
    super("이미 오픈된 공연은 수정할 수 없습니다: " + id);
  }
}
