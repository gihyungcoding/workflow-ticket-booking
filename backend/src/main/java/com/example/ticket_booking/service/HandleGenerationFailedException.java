package com.example.ticket_booking.service;

/** 아이디를 정해진 횟수 안에 겹치지 않게 뽑지 못했다 — 가입은 아무것도 만들지 않고 실패한다(503). */
public class HandleGenerationFailedException extends RuntimeException {

  public HandleGenerationFailedException() {
    super("아이디를 만들지 못했습니다. 잠시 후 다시 시도해 주세요");
  }
}
