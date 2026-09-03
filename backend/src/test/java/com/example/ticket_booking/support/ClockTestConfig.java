package com.example.ticket_booking.support;

import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** 프로덕션 Clock 빈을 MutableClock으로 대체한다 — 기본값은 실제 시각이고, 경계값 테스트(SC-07/08)에서만 setInstant로 고정한다. */
@TestConfiguration
public class ClockTestConfig {

  @Bean
  @Primary
  public MutableClock mutableClock() {
    return new MutableClock(Instant.now(), ZoneOffset.UTC);
  }
}
