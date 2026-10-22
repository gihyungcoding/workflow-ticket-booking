package com.example.ticket_booking.service;

/** 틀린 비밀번호와 존재하지 않는 이메일을 구분하지 않는다 — 메시지는 한 가지뿐이다. */
public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException() {
    super("이메일 또는 비밀번호가 올바르지 않습니다");
  }
}
