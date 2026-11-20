package com.example.ticket_booking.domain;

import java.time.Duration;

/** 일회용 이메일 링크 토큰의 용도(ADR-0032). 용도마다 유효 기간이 다르다 — 인증 24시간, 재설정 30분. */
public enum EmailTokenPurpose {
  VERIFY_EMAIL(Duration.ofHours(24)),
  RESET_PASSWORD(Duration.ofMinutes(30));

  private final Duration ttl;

  EmailTokenPurpose(Duration ttl) {
    this.ttl = ttl;
  }

  /** 발급 시각부터 이 기간이 지나면(정각 포함) 만료다. */
  public Duration ttl() {
    return ttl;
  }
}
