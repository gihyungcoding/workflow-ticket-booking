package com.example.ticket_booking.config;

import java.time.Clock;

/** TASK-009 Phase 2b 스켈레톤 — 시그니처만 있다. 구현과 빈 등록은 Phase 3. */
public class JwtTokenProvider {

  private JwtTokenProvider() {}

  /**
   * JWT_SECRET 환경변수 원문과 Clock 으로 만든다. 비었거나 공백이거나 UTF-8 32바이트 미만이면 IllegalStateException(메시지에
   * JWT_SECRET) 을 던진다.
   */
  public static JwtTokenProvider fromEnv(String secret, Clock clock) {
    throw new UnsupportedOperationException();
  }
}
