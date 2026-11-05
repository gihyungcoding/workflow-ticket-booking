package com.example.ticket_booking.support;

/** 테스트가 users 행을 직접 저장할 때 쓰는 값 (TASK-016 에서 handle 이 NOT NULL·UNIQUE 가 되었다). */
public final class UserFixtures {

  private UserFixtures() {}

  /** 이메일마다 다른 아이디 — 아이디 규칙을 검증하는 값이 아니라 저장에 필요한 자리 채움이다. */
  public static String fixtureHandle(String email) {
    return "t" + Integer.toHexString(email.hashCode());
  }
}
