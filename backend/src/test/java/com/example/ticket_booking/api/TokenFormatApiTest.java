package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.accessTokenOf;
import static com.example.ticket_booking.support.AuthTestHelper.bodyAsMap;
import static com.example.ticket_booking.support.AuthTestHelper.getWithAuthorization;
import static com.example.ticket_booking.support.AuthTestHelper.login;
import static com.example.ticket_booking.support.AuthTestHelper.signUp;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.HandleRandomTestConfig;
import com.example.ticket_booking.support.SequenceRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * TASK-016 시나리오 SC-15 — 정상 accessToken 의 다른 문자열 표기로는 인증되지 않는다(Phase 4 RC-2). 아래 표기는 Authorization
 * 헤더 형식 검사를 통과해 토큰 검증까지 닿는다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-016.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(HandleRandomTestConfig.class)
class TokenFormatApiTest {

  private static final String EMAIL = "member@example.com";
  private static final String PASSWORD = "password123";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private SequenceRandom random;

  @BeforeEach
  @AfterEach
  void cleanUp() {
    random.clear();
    userRepository.deleteAll();
  }

  private static String codeOf(MvcResult result) {
    try {
      Object code = bodyAsMap(result).get("code");
      return code == null ? null : String.valueOf(code);
    } catch (Exception notJson) {
      return null;
    }
  }

  @Test
  void test_sc15_정상_accessToken_의_다른_문자열_표기로는_인증되지_않는다() throws Exception {
    // Given 이메일 가입한 회원의 정상 accessToken t 가 있다
    random.enqueue(0, 1);
    assertThat(signUp(mockMvc, EMAIL, PASSWORD, "회원", "AUDIENCE").getResponse().getStatus())
        .as("가입")
        .isEqualTo(201);
    String t = accessTokenOf(login(mockMvc, EMAIL, PASSWORD));
    Long memberId = userRepository.findByEmail(EMAIL).orElseThrow().getId();
    Map<String, String> rows = new LinkedHashMap<>();
    rows.put("t=", t + "=");
    rows.put("t==", t + "==");
    rows.put("t~", t + "~");
    rows.put("t.abc (마디가 넷)", t + ".abc");
    SoftAssertions soft = new SoftAssertions();

    // When t 그대로 GET /api/auth/me 를 호출하면 Then 200, 응답 id·email 이 그 회원
    MvcResult asIs = getWithAuthorization(mockMvc, "/api/auth/me", "Bearer " + t);
    soft.assertThat(asIs.getResponse().getStatus()).as("그대로 상태").isEqualTo(200);
    if (asIs.getResponse().getStatus() == 200) {
      soft.assertThat(((Number) bodyAsMap(asIs).get("id")).longValue())
          .as("그대로 id")
          .isEqualTo(memberId);
      soft.assertThat(bodyAsMap(asIs).get("email")).as("그대로 email").isEqualTo(EMAIL);
    }
    for (Map.Entry<String, String> row : rows.entrySet()) {
      // When 표의 값으로 GET /api/auth/me 를 호출한다
      MvcResult result = getWithAuthorization(mockMvc, "/api/auth/me", "Bearer " + row.getValue());
      // Then 401 UNAUTHORIZED 다
      soft.assertThat(result.getResponse().getStatus()).as(row.getKey() + " 상태").isEqualTo(401);
      soft.assertThat(codeOf(result)).as(row.getKey() + " code").isEqualTo("UNAUTHORIZED");
    }
    soft.assertAll();
  }
}
