package com.example.ticket_booking.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.repository.SeatRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * TASK-009 시나리오 SC-17 (regression).
 *
 * <p>MockMvc 는 서블릿 컨테이너의 ERROR 디스패치(sendError → /error)를 재현하지 못한다 — 그래서 이 오류 응답의 회귀는 MockMvc 로는 가짜
 * 통과가 된다. 실제 서버를 띄워(RANDOM_PORT) HTTP 로 요청한다. 기대 상태는 Security 도입 이전(develop)에서 같은 요청으로 확인한
 * 값이다(Phase 4 프로브).
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-009.md
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ClockTestConfig.class)
class PerformanceErrorResponseRegressionTest {

  @LocalServerPort private int port;

  @Autowired private PerformanceRepository performanceRepository;
  @Autowired private SeatRepository seatRepository;
  @Autowired private MutableClock clock;

  private final HttpClient client = HttpClient.newHttpClient();

  @BeforeEach
  void reset() {
    deleteAll();
    clock.setInstant(Instant.now());
  }

  @AfterEach
  void cleanUp() {
    deleteAll();
  }

  private void deleteAll() {
    seatRepository.deleteAll();
    performanceRepository.deleteAll();
  }

  private record Row(
      String label, String method, String path, String contentType, String body, int expected) {}

  private int send(Row row) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + row.path()));
    if (row.contentType() != null) {
      builder.header("Content-Type", row.contentType());
    }
    builder.method(
        row.method(),
        row.body() == null ? BodyPublishers.noBody() : BodyPublishers.ofString(row.body()));
    HttpResponse<String> response = client.send(builder.build(), BodyHandlers.ofString());
    return response.statusCode();
  }

  @Test
  void test_sc17_공연_API의_프레임워크_오류_응답은_Security_도입_뒤에도_그대로다() throws Exception {
    // Given 공연이 하나 등록돼 있다 (id 는 P)
    Instant now = clock.instant();
    Long id =
        performanceRepository
            .save(
                new Performance(
                    "공연 P",
                    "서울올림픽공원",
                    now.plus(30, ChronoUnit.DAYS),
                    now.plus(5, ChronoUnit.DAYS),
                    now.plus(20, ChronoUnit.DAYS),
                    100,
                    50,
                    false))
            .getId();
    List<Row> rows =
        List.of(
            new Row("경로 id 타입 불일치", "GET", "/api/performances/abc", null, null, 400),
            new Row("쿼리 page 타입 불일치", "GET", "/api/performances?page=abc", null, null, 400),
            new Row("쿼리 status enum 값 오류", "GET", "/api/performances?status=NOPE", null, null, 400),
            new Row("DELETE 미지원 메서드", "DELETE", "/api/performances/" + id, null, null, 405),
            new Row("PATCH 미지원 메서드", "PATCH", "/api/performances/" + id, null, null, 405),
            new Row("text/plain 본문 POST", "POST", "/api/performances", "text/plain", "x", 415),
            new Row("취소 경로 id 타입 불일치", "POST", "/api/performances/abc/cancel", null, null, 400),
            new Row("존재하지 않는 공연", "GET", "/api/performances/99999", null, null, 404));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      // When 각 요청을 Authorization 없이 보낸다
      int status = send(row);

      // Then 모든 행의 상태가 표의 기대 숫자와 같다 (Security 도입 이전과 같은 값이며, 인증을 요구하는 401 이 아니다)
      soft.assertThat(status)
          .as(row.label() + " (" + row.method() + " " + row.path() + ")")
          .isEqualTo(row.expected());
    }
    assertThat(rows).as("표의 행 수").hasSize(8);
    soft.assertAll();
  }
}
