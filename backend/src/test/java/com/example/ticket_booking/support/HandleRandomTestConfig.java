package com.example.ticket_booking.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 아이디 생성 난수원을 {@link SequenceRandom} 으로 대체한다 (TASK-016). SequenceRandom 은 {@code RandomGenerator}
 * 이므로 프로덕션 코드가 그 타입으로 주입받으면 이 빈을 받는다.
 */
@TestConfiguration
public class HandleRandomTestConfig {

  @Bean
  @Primary
  public SequenceRandom sequenceRandom() {
    return new SequenceRandom();
  }
}
