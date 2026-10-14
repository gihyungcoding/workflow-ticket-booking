package com.example.ticket_booking.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * TASK-014 시나리오 SC-12, SC-22.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-014.md
 */
class DockerfileTest {

  private static final Path DOCKERFILE = Path.of("Dockerfile");

  @Test
  void test_sc12_Dockerfile이_bootJar_산출물을_JVM_힙_상한과_함께_실행하도록_작성되어_있다() throws Exception {
    // Given backend/Dockerfile 이 작성되어 있다
    assertThat(Files.exists(DOCKERFILE)).as("Dockerfile 이 존재해야 한다").isTrue();

    // When 파일 내용을 확인한다
    String content = Files.readString(DOCKERFILE);

    // Then ENTRYPOINT 또는 CMD 라인이 bootJar 산출물을 java -jar 로 실행하며 -Xmx 옵션을 포함한다
    assertThat(content).containsPattern("(ENTRYPOINT|CMD).*java.*-jar");
    assertThat(content).contains("-Xmx");
  }

  @Test
  void test_sc22_Dockerfile로_이미지가_빌드된다() throws Exception {
    // Docker 데몬이 없으면 검증 불가로 WARN 처리하고 첫 배포에서 확인한다 (AC 원문에 명시됨)
    boolean dockerAvailable = isDockerAvailable();
    assumeTrue(dockerAvailable, "Docker 데몬을 사용할 수 없어 이 시나리오는 검증하지 못했다 (WARN)");

    // Given backend/Dockerfile 이 작성되어 있다 (SC-12 에서 내용 확인됨)
    // When docker build 로 이미지를 빌드한다
    Process process =
        new ProcessBuilder("docker", "build", "-t", "ticket-booking-backend-test", ".")
            .directory(new File("."))
            .redirectErrorStream(true)
            .start();
    boolean finished = process.waitFor(300, TimeUnit.SECONDS);
    int exitCode = finished ? process.exitValue() : -1;

    // Then 빌드가 성공한다
    assertThat(exitCode).isEqualTo(0);
  }

  private boolean isDockerAvailable() {
    try {
      Process process = new ProcessBuilder("docker", "info").start();
      return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
    } catch (Exception e) {
      return false;
    }
  }
}
