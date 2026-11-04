package com.example.ticket_booking.config;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 티켓온 아이디 생성의 난수원. 아이디는 추측되지 않아야 하므로 SecureRandom 을 쓴다. */
@Configuration
public class HandleRandomConfig {

  @Bean
  public RandomGenerator handleRandom() {
    return new SecureRandom();
  }
}
