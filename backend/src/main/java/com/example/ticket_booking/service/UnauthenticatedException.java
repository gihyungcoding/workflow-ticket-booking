package com.example.ticket_booking.service;

/** 유효한 토큰이어도 그 사용자가 더 이상 없을 때 던진다. */
public class UnauthenticatedException extends RuntimeException {

  public UnauthenticatedException() {
    super("인증이 필요합니다");
  }
}
