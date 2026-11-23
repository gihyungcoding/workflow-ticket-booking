package com.example.ticket_booking.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 애플리케이션의 MailGateway 를 {@link CapturingMailGateway} 로 바꾼다(TASK-027 머리말 「메일」). 수집 구현이 곧 MailGateway
 * 라 빈 하나를 {@code @Primary} 로 두면 MailGateway 로 주입받는 곳도 같은 객체를 받는다.
 */
@TestConfiguration
public class CapturingMailConfig {

  @Bean
  @Primary
  public CapturingMailGateway capturingMailGateway() {
    return new CapturingMailGateway();
  }
}
