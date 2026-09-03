package com.example.ticket_booking.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** 테스트에서 "지금"을 임의로 고정하기 위한 Clock (ADR-0005). */
public class MutableClock extends Clock {

  private volatile Instant instant;
  private final ZoneId zone;

  public MutableClock(Instant instant, ZoneId zone) {
    this.instant = instant;
    this.zone = zone;
  }

  public void setInstant(Instant instant) {
    this.instant = instant;
  }

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return new MutableClock(instant, zone);
  }

  @Override
  public Instant instant() {
    return instant;
  }
}
