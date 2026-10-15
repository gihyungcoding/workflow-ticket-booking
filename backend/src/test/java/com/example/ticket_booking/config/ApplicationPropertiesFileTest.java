package com.example.ticket_booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * TASK-014 시나리오 SC-11.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-014.md
 */
class ApplicationPropertiesFileTest {

  @Test
  void test_sc11_설정_파일에_DB_접속_정보가_하드코딩돼_있지_않다() throws Exception {
    // Given application.properties 파일이 작성되어 있다
    Path path = Path.of("src/main/resources/application.properties");

    // When 파일 내용을 확인한다
    String content = Files.readString(path);

    // Then spring.datasource.url/username/password 가 파일에 리터럴 값으로 없다
    assertThat(content)
        .doesNotContain("spring.datasource.url=")
        .doesNotContain("spring.datasource.username=")
        .doesNotContain("spring.datasource.password=");
  }
}
