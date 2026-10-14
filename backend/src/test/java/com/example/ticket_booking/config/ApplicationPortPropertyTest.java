package com.example.ticket_booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * TASK-014 시나리오 SC-09.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-014.md
 */
@SpringBootTest(properties = "PORT=10000")
class ApplicationPortPropertyTest {

  @Autowired private Environment environment;

  @Test
  void test_sc09_PORT_환경변수가_server_port_로_반영된다() {
    // Given PORT=10000 환경변수가 설정되어 있다
    // When 애플리케이션 설정을 로딩한다 (@SpringBootTest 컨텍스트 기동 시점)
    // Then server.port 값이 10000이다
    assertThat(environment.getProperty("server.port")).isEqualTo("10000");
  }
}
