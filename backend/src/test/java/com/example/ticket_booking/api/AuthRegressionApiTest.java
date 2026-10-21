package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.bodyAsMap;
import static com.example.ticket_booking.support.AuthTestHelper.getWithAuthorization;
import static com.example.ticket_booking.support.AuthTestHelper.postJson;
import static com.example.ticket_booking.support.AuthTestHelper.signedToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.repository.SeatRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * TASK-009 시나리오 SC-15 (regression).
 *
 * <p>Security 가 들어와도 공연 API 가 인증 없이 그대로 동작하는지, 인증 API 경로의 사전 요청이 보안 필터에서 거부되지 않는지 본다. CORS 허용 출처는
 * 클래스 properties 로 주입한다. 이 시나리오는 Security 구현 전에도 통과한다(already_passing) — 기존 동작이 계속된다는 부정형 회귀다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-009.md
 */
@SpringBootTest(properties = {"CORS_ALLOWED_ORIGINS=https://app.example.com"})
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class AuthRegressionApiTest {

  private static final String ORIGIN = "https://app.example.com";

  @Autowired private MockMvc mockMvc;
  @Autowired private PerformanceRepository performanceRepository;
  @Autowired private SeatRepository seatRepository;
  @Autowired private MutableClock clock;

  @Value("${JWT_SECRET}")
  private String jwtSecret;

  @BeforeEach
  void reset() {
    deleteAllPerformances();
    clock.setInstant(Instant.now());
  }

  @AfterEach
  void cleanUp() {
    deleteAllPerformances();
  }

  /** 등록 API 가 만든 좌석이 공연을 참조하므로 좌석부터 지운다 — 남기면 다른 테스트의 목록 단언을 오염시킨다. */
  private void deleteAllPerformances() {
    seatRepository.deleteAll();
    performanceRepository.deleteAll();
  }

  private String registerJson() {
    Instant now = clock.instant();
    return """
        {
          "title": "새 공연",
          "venue": "테스트홀",
          "startAt": "%s",
          "openAt": "%s",
          "closeAt": "%s",
          "sections": [{"grade":"VIP","price":120000,"rowStart":"A","rowEnd":"B","seatsPerRow":10}]
        }
        """
        .formatted(
            now.plus(30, ChronoUnit.DAYS),
            now.minus(1, ChronoUnit.DAYS),
            now.plus(20, ChronoUnit.DAYS));
  }

  private String updateJson() {
    Instant now = clock.instant();
    return """
        {
          "title": "공연 P",
          "venue": "부산 BEXCO",
          "startAt": "%s",
          "openAt": "%s",
          "closeAt": "%s"
        }
        """
        .formatted(
            now.plus(30, ChronoUnit.DAYS),
            now.minus(1, ChronoUnit.DAYS),
            now.plus(10, ChronoUnit.DAYS));
  }

  @Test
  void test_sc15_Security_도입_뒤에도_공연_API는_인증_없이_그대로_동작한다() throws Exception {
    // Given 아직 오픈 전인 공연이 하나 등록돼 있다 (id 는 P — 이미 오픈된 공연은 수정할 수 없다)
    Instant now = clock.instant();
    Performance p =
        performanceRepository.save(
            new Performance(
                "공연 P",
                "서울올림픽공원",
                now.plus(30, ChronoUnit.DAYS),
                now.plus(5, ChronoUnit.DAYS),
                now.plus(20, ChronoUnit.DAYS),
                100,
                50,
                false));
    Long id = p.getId();
    SoftAssertions soft = new SoftAssertions();

    // When Authorization 없이, CSRF 토큰 없이 각 요청을 보낸다
    MvcResult list = getWithAuthorization(mockMvc, "/api/performances", null);
    MvcResult detail = getWithAuthorization(mockMvc, "/api/performances/" + id, null);
    MvcResult register = postJson(mockMvc, "/api/performances", registerJson());
    MvcResult update =
        mockMvc
            .perform(
                put("/api/performances/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateJson()))
            .andReturn();
    MvcResult cancel = mockMvc.perform(post("/api/performances/" + id + "/cancel")).andReturn();

    // Then 모든 행의 상태가 기대와 같다 (401·403 이 아니다)
    soft.assertThat(list.getResponse().getStatus()).as("GET 목록").isEqualTo(200);
    soft.assertThat(detail.getResponse().getStatus()).as("GET 단건").isEqualTo(200);
    soft.assertThat(register.getResponse().getStatus()).as("POST 등록").isEqualTo(201);
    soft.assertThat(update.getResponse().getStatus()).as("PUT 수정").isEqualTo(200);
    soft.assertThat(cancel.getResponse().getStatus()).as("POST 취소").isEqualTo(200);
    // And 응답 본문은 기존과 같은 필드를 갖는다
    if (list.getResponse().getStatus() == 200) {
      soft.assertThat(bodyAsMap(list).keySet())
          .as("목록 키")
          .containsExactlyInAnyOrder("content", "page", "size", "totalElements");
    }
    for (Map.Entry<String, MvcResult> entry :
        Map.of("단건", detail, "등록", register, "수정", update).entrySet()) {
      int status = entry.getValue().getResponse().getStatus();
      if (status == 200 || status == 201) {
        soft.assertThat(bodyAsMap(entry.getValue()).keySet())
            .as(entry.getKey() + " 키")
            .contains("id", "title", "venue", "status");
      }
    }
    if (cancel.getResponse().getStatus() == 200) {
      soft.assertThat(bodyAsMap(cancel).get("status")).as("취소 status").isEqualTo("CANCELLED");
    }

    // When 같은 조회에 만료된 토큰 / 위조된 토큰이 실려 온다
    Instant past = clock.instant().minus(2, ChronoUnit.HOURS);
    String expired =
        signedToken(
            jwtSecret, "1", "user@example.com", "AUDIENCE", "access", past, past.plusSeconds(60));
    String forged =
        signedToken(
            "another-secret-0123456789abcdefghij",
            "1",
            "user@example.com",
            "AUDIENCE",
            "access",
            clock.instant(),
            clock.instant().plusSeconds(3600));
    MvcResult withExpired = getWithAuthorization(mockMvc, "/api/performances", "Bearer " + expired);
    MvcResult withForged = getWithAuthorization(mockMvc, "/api/performances", "Bearer " + forged);

    // Then 공연 조회는 200 이다 (인증 실패가 공연 조회를 막지 않는다)
    soft.assertThat(withExpired.getResponse().getStatus()).as("만료 토큰 + 목록").isEqualTo(200);
    soft.assertThat(withForged.getResponse().getStatus()).as("위조 토큰 + 목록").isEqualTo(200);

    // When 허용된 Origin 이 인증 API 경로에 사전 요청을 보낸다
    MvcResult preflight =
        mockMvc
            .perform(
                options("/api/auth/login")
                    .header("Origin", ORIGIN)
                    .header("Access-Control-Request-Method", "POST")
                    .header(
                        "Access-Control-Request-Headers", "authorization,content-type,x-write-key"))
            .andReturn();

    // Then 2xx 이고 Access-Control-Allow-Origin 이 그 Origin 이다 (401·403 이 아니다)
    soft.assertThat(preflight.getResponse().getStatus()).as("사전 요청 상태").isBetween(200, 299);
    soft.assertThat(preflight.getResponse().getHeader("Access-Control-Allow-Origin"))
        .as("사전 요청 Allow-Origin")
        .isEqualTo(ORIGIN);
    soft.assertAll();
  }
}
