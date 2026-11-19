package com.example.ticket_booking.support;

import com.example.ticket_booking.service.MailGateway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** 애플리케이션의 MailGateway 를 {@link CapturingMailGateway} 로 바꾼다(TASK-027 머리말 「메일」). */
@TestConfiguration
public class CapturingMailConfig {

  @Bean
  @Primary
  public CapturingMailGateway capturingMailGateway() {
    return new CapturingMailGateway();
  }

  /** MailGateway 타입으로 주입받는 곳도 같은 수집 구현을 받는다. */
  @Bean
  @Primary
  public MailGateway primaryMailGateway(CapturingMailGateway capturing) {
    return capturing;
  }
}
