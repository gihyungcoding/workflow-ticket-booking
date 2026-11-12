package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.OBJECT_MAPPER;
import static com.example.ticket_booking.support.AuthTestHelper.json;
import static com.example.ticket_booking.support.AuthTestHelper.loginBody;
import static com.example.ticket_booking.support.AuthTestHelper.signUpBody;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.repository.SeatRepository;
import com.example.ticket_booking.repository.UserRepository;
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
import java.util.Map;
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
 * <p>TASK-022 부터 공연 쓰기는 주최자 토큰이 있어야 한다(ADR-0029). SC-17 은 기대 숫자를 그대로 두고 쓰기 행에만 주최자 토큰을 싣는다.
 * TASK-022 SC-12 는 같은 8행에 토큰 없음·관객 토큰 행을 더한 표다 — ERROR 디스패치를 거치는 응답이라 같은 실제 서버에서 본다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-009.md · SCENARIO_TASK-022.md
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ClockTestConfig.class)
class PerformanceErrorResponseRegressionTest {

  @LocalServerPort private int port;

  @Autowired private PerformanceRepository performanceRepository;
  @Autowired private SeatRepository seatRepository;
  @Autowired private UserRepository userRepository;
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
    userRepository.deleteAll();
  }

  private record Row(
      String label,
      String method,
      String path,
      String contentType,
      String body,
      String token,
      int expected) {}

  private HttpResponse<String> postJson(String path, String body) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(body))
            .build(),
        BodyHandlers.ofString());
  }

  /** 실제 서버에 가입·로그인해 access 토큰을 얻는다. */
  @SuppressWarnings("unchecked")
  private String tokenOf(String email, String role) throws Exception {
    HttpResponse<String> signedUp =
        postJson(
            "/api/auth/signup", json(signUpBody(email, "password123", "password123", "한도윤", role)));
    assertThat(signedUp.statusCode()).as(email + " 가입").isEqualTo(201);
    HttpResponse<String> loggedIn =
        postJson("/api/auth/login", json(loginBody(email, "password123")));
    assertThat(loggedIn.statusCode()).as(email + " 로그인").isEqualTo(200);
    return (String) OBJECT_MAPPER.readValue(loggedIn.body(), Map.class).get("accessToken");
  }

  private int send(Row row) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + row.path()));
    if (row.contentType() != null) {
      builder.header("Content-Type", row.contentType());
    }
    if (row.token() != null) {
      builder.header("Authorization", "Bearer " + row.token());
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
    // And (TASK-022) 쓰기 행에 실을 주최자 토큰이 있다
    String organizer = tokenOf("o1@example.com", "ORGANIZER");
    List<Row> rows =
        List.of(
            new Row("경로 id 타입 불일치", "GET", "/api/performances/abc", null, null, null, 400),
            new Row("쿼리 page 타입 불일치", "GET", "/api/performances?page=abc", null, null, null, 400),
            new Row(
                "쿼리 status enum 값 오류",
                "GET",
                "/api/performances?status=NOPE",
                null,
                null,
                null,
                400),
            new Row(
                "DELETE 미지원 메서드", "DELETE", "/api/performances/" + id, null, null, organizer, 405),
            new Row(
                "PATCH 미지원 메서드", "PATCH", "/api/performances/" + id, null, null, organizer, 405),
            new Row(
                "text/plain 본문 POST",
                "POST",
                "/api/performances",
                "text/plain",
                "x",
                organizer,
                415),
            new Row(
                "취소 경로 id 타입 불일치",
                "POST",
                "/api/performances/abc/cancel",
                null,
                null,
                organizer,
                400),
            new Row("존재하지 않는 공연", "GET", "/api/performances/99999", null, null, null, 404));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      // When 각 요청을 보낸다 (조회 행은 Authorization 없이, 쓰기 행은 주최자 토큰으로 — TASK-022)
      int status = send(row);

      // Then 모든 행의 상태가 표의 기대 숫자와 같다 (Security 도입 이전과 같은 값이며, 인증을 요구하는 401 이 아니다)
      soft.assertThat(status)
          .as(row.label() + " (" + row.method() + " " + row.path() + ")")
          .isEqualTo(row.expected());
    }
    assertThat(rows).as("표의 행 수").hasSize(8);
    soft.assertAll();
  }

  /**
   * TASK-022 SC-12 (regression)
   *
   * <p>Given 공연 P0(소유자 NULL) When 표의 요청을 표의 토큰으로 보낸다 Then 모든 행의 상태가 표와 같다 — a~h 는 SC-17 의 8행(쓰기 행에
   * 주최자 토큰), i~l 은 토큰 없음·관객 토큰 행
   */
  @Test
  void test_sc12_기존_오류_응답_회귀_표는_쓰기_행에_주최자_토큰을_실으면_같은_숫자다() throws Exception {
    // Given 공연 P0(소유자 NULL)이 있다
    Instant now = clock.instant();
    Long p0 =
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
    String o1 = tokenOf("o1@example.com", "ORGANIZER");
    String a1 = tokenOf("a1@example.com", "AUDIENCE");
    List<Row> rows =
        List.of(
            new Row("a", "GET", "/api/performances/abc", null, null, null, 400),
            new Row("b", "GET", "/api/performances?page=abc", null, null, null, 400),
            new Row("c", "GET", "/api/performances?status=NOPE", null, null, null, 400),
            new Row("d", "DELETE", "/api/performances/" + p0, null, null, o1, 405),
            new Row("e", "PATCH", "/api/performances/" + p0, null, null, o1, 405),
            new Row("f", "POST", "/api/performances", "text/plain", "x", o1, 415),
            new Row("g", "POST", "/api/performances/abc/cancel", null, null, o1, 400),
            new Row("h", "GET", "/api/performances/99999", null, null, null, 404),
            new Row("i", "DELETE", "/api/performances/" + p0, null, null, null, 401),
            new Row("j", "POST", "/api/performances", "text/plain", "x", null, 401),
            new Row("k", "DELETE", "/api/performances/" + p0, null, null, a1, 403),
            new Row("l", "PATCH", "/api/performances/" + p0, null, null, a1, 403));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      // When
      int status = send(row);

      // Then 모든 행의 상태가 표와 같다
      soft.assertThat(status)
          .as(row.label() + " (" + row.method() + " " + row.path() + ")")
          .isEqualTo(row.expected());
    }
    soft.assertAll();
  }
}
