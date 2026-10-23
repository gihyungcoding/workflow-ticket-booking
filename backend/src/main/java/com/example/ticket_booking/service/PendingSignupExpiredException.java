package com.example.ticket_booking.service;

/** pendingToken 이 만료됐거나 위조됐거나 용도가 다르다. 구분해서 알리지 않는다. */
public class PendingSignupExpiredException extends RuntimeException {

  public PendingSignupExpiredException() {
    super("로그인 정보가 만료되었습니다. 다시 로그인해 주세요");
  }
}
