package com.example.ticket_booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;

/**
 * TASK-014 시나리오 SC-03.
 *
 * <p>CorsFilter 구현이 Spring 의 CORS 설정 메서드(allowedOrigins 등)에 와일드카드를 쓰지 않으므로
 * check_architecture.py 는 구현 전인 지금도 위반 0건을 보고한다 — already_passing
 * (TEST_TASK-014.json 참고).
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-014.md
 */
class ArchitectureCheckTest {

  @Test
  void test_sc03_CORS_구현에_와일드카드_패턴이_없다() throws Exception {
    // Given CorsConfig 구현이 작성되어 있다
    // When python3 scripts/check_architecture.py --id DEPLOY-002 를 실행한다
    Process process =
        new ProcessBuilder("python3", "scripts/check_architecture.py", "--id", "DEPLOY-002")
            .directory(new File(".."))
            .redirectErrorStream(true)
            .start();
    int exitCode = process.waitFor();

    // Then 위반이 0건으로 보고된다 (스크립트는 위반이 있으면 0이 아닌 코드로 종료한다)
    assertThat(exitCode).isEqualTo(0);
  }
}
