package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.accessTokenOf;
import static com.example.ticket_booking.support.AuthTestHelper.code;
import static com.example.ticket_booking.support.AuthTestHelper.json;
import static com.example.ticket_booking.support.AuthTestHelper.loginBody;
import static com.example.ticket_booking.support.AuthTestHelper.signUpBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * TASK-009 시나리오 SC-19 (regression).
 *
 * <p>쓰기 가드를 실제로 켜고(WRITE_GUARD_ENABLED=true, 키 secret123 — 클래스 properties) ADR-0017 의 결정을 확인한다: 가드는
 * 건드리지 않았고 인증 API 의 쓰기도 면제하지 않는다. 가드는 WriteGuardFilter 가 이미 전역으로 걸고 있어 구현 전에도
 * 통과한다(already_passing).
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-009.md
 */
@SpringBootTest(properties = {"WRITE_GUARD_ENABLED=true", "WRITE_API_KEY=secret123"})
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class WriteGuardAuthApiTest {

  private static final String KEY = "secret123";
  private static final String EMAIL = "user@example.com";
  private static final String PASSWORD = "password123";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  void reset() {
    userRepository.deleteAll();
  }

  @AfterEach
  void cleanUp() {
    userRepository.deleteAll();
  }

  private MvcResult postJson(String path, String body, String writeKey) throws Exception {
    MockHttpServletRequestBuilder request =
        post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    if (writeKey != null) {
      request = request.header("X-Write-Key", writeKey);
    }
    return mockMvc.perform(request).andReturn();
  }

  private String validSignUp(String email) {
    return json(signUpBody(email, PASSWORD, PASSWORD, "홍길동", "AUDIENCE"));
  }

  @Test
  void test_sc19_쓰기_가드가_켜진_환경에서는_인증_API의_쓰기도_기존_가드_규칙을_따른다() throws Exception {
    // Given 쓰기 가드가 켜져 있고 키는 secret123 이다 (클래스 properties)
    // And user@example.com 이 가입돼 있어 유효한 accessToken 이 있다
    MvcResult signedUp = postJson("/api/auth/signup", validSignUp(EMAIL), KEY);
    assertThat(signedUp.getResponse().getStatus()).as("Given: 가입 201").isEqualTo(201);
    MvcResult loggedIn = postJson("/api/auth/login", json(loginBody(EMAIL, PASSWORD)), KEY);
    assertThat(loggedIn.getResponse().getStatus()).as("Given: 로그인 200").isEqualTo(200);
    String token = accessTokenOf(loggedIn);
    String performanceBody =
        """
        {"title":"공연","venue":"홀","startAt":"2030-01-01T10:00:00Z","openAt":"2029-01-01T10:00:00Z",
         "closeAt":"2029-12-01T10:00:00Z",
         "sections":[{"grade":"VIP","price":1000,"rowStart":"A","rowEnd":"A","seatsPerRow":1}]}
        """;
    record Row(String label, MvcResult result, int expected) {}
    SoftAssertions soft = new SoftAssertions();

    // When 아래 표의 각 요청을 보낸다
    List<Row> rows =
        List.of(
            new Row(
                "signup 키 없음",
                postJson("/api/auth/signup", validSignUp("a@example.com"), null),
                401),
            new Row(
                "signup 키 있음",
                postJson("/api/auth/signup", validSignUp("b@example.com"), KEY),
                201),
            new Row(
                "login 키 없음",
                postJson("/api/auth/login", json(loginBody(EMAIL, PASSWORD)), null),
                401),
            new Row(
                "login 키 있음",
                postJson("/api/auth/login", json(loginBody(EMAIL, PASSWORD)), KEY),
                200),
            new Row("공연 등록 키 없음", postJson("/api/performances", performanceBody, null), 401),
            new Row(
                "me 키 없이 조회",
                mockMvc
                    .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andReturn(),
                200));

    for (Row row : rows) {
      // Then 모든 행의 상태가 기대와 같다
      soft.assertThat(row.result().getResponse().getStatus())
          .as(row.label())
          .isEqualTo(row.expected());
      // And 401 행의 code 는 WRITE_KEY_INVALID 이다 (UNAUTHORIZED 가 아니다)
      if (row.expected() == 401 && row.result().getResponse().getStatus() == 401) {
        soft.assertThat(code(row.result()))
            .as(row.label() + " code")
            .isEqualTo("WRITE_KEY_INVALID");
      }
    }
    soft.assertAll();
  }
}
