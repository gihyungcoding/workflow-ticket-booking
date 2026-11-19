package com.example.ticket_booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * TASK-027 SC-19 — architecture.md §5 외부 의존 표의 메일 발송 행이 ADR-0031 과 같은 내용인가.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-027.md
 */
class Task027ArchitectureDocTest {

  /**
   * SC-19 (happy) architecture.md §5 에 ADR-0031 과 같은 메일 발송 행이 있다.
   *
   * <p>Given 저장소의 docs/architecture/architecture.md When §5 외부 의존 표를 읽는다 Then 대상이 메일 발송(SMTP) 인 행이
   * 정확히 1개이고 그 행에 MailGateway·ADR-0031 이 있으며 실패 열에 가입·소셜 로그인·이미 인증된 계정의 로그인은 동작한다는 내용이 있다
   */
  @Test
  void test_sc19_architecture_md_5_에_ADR_0031_과_같은_메일_발송_행이_있다() throws IOException {
    // Given
    Path doc = Path.of("../docs/architecture/architecture.md");
    String text = Files.readString(doc, StandardCharsets.UTF_8);

    // When
    int start = text.indexOf("## 5. 외부 의존");
    int end = text.indexOf("\n## ", start + 1);
    String section = text.substring(start, end < 0 ? text.length() : end);
    List<String> mailRows =
        section.lines().filter(line -> line.startsWith("| 메일 발송(SMTP) |")).toList();

    // Then
    assertThat(start).as("§5 제목").isNotNegative();
    assertThat(mailRows).hasSize(1);
    String row = mailRows.get(0);
    assertThat(row).contains("MailGateway").contains("ADR-0031");
    String[] cells = row.split("\\|");
    // cells[0] 은 빈 칸, [1] 대상, [2] 용도, [3] 실패하면, [4] 대안
    assertThat(cells[3])
        .as("실패하면 열")
        .contains("가입")
        .contains("소셜 로그인")
        .contains("이미 인증된 계정의 로그인은 동작");
  }
}
