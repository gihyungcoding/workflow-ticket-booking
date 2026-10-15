package com.example.ticket_booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * TASK-014 시나리오 SC-10.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-014.md
 */
@SpringBootTest
class ApplicationDefaultPortPropertyTest {

  @Autowired private Environment environment;

  @Test
  void test_sc10_PORT_가_없으면_기본_포트_8080_을_쓴다() {
    // Given PORT 환경변수가 설정되어 있지 않다
    // When 애플리케이션 설정을 로딩한다
    // Then server.port 값이 8080이다
    assertThat(environment.getProperty("server.port")).isEqualTo("8080");
  }
}
