package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.OBJECT_MAPPER;
import static com.example.ticket_booking.support.AuthTestHelper.code;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.ticket_booking.repository.SocialAccountRepository;
import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import com.example.ticket_booking.support.PendingTokenTestHelper;
import java.time.Instant;
import java.util.LinkedHashMap;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * TASK-011 시나리오 SC-18.
 *
 * <p>쓰기 가드를 실제로 켠다(클래스 properties: WRITE_GUARD_ENABLED=true, WRITE_API_KEY=server-key). 가드는
 * WriteGuardFilter 가 이미 모든 쓰기 메서드에 걸고 있어 엔드포인트가 없어도 키가 틀린 행은 구현 전에 통과할 수 있다(already_passing). 키가 맞는
 * 행은 가입 완료가 구현돼야 200 이 된다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-011.md
 */
@SpringBootTest(properties = {"WRITE_GUARD_ENABLED=true", "WRITE_API_KEY=server-key"})
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class WriteGuardSocialCompleteTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-10-23T00:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private SocialAccountRepository socialAccountRepository;
  @Autowired private MutableClock clock;

  @Value("${JWT_SECRET}")
  private String secret;

  @BeforeEach
  void reset() {
    cleanUp();
    clock.setInstant(FIXED_NOW);
  }

  @AfterEach
  void cleanUp() {
    socialAccountRepository.deleteAll();
    userRepository.deleteAll();
  }

  private MvcResult complete(String writeKey) throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("pendingToken", PendingTokenTestHelper.valid(secret, FIXED_NOW));
    body.put("role", "AUDIENCE");
    MockHttpServletRequestBuilder request =
        post("/api/auth/social/complete")
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(body));
    if (writeKey != null) {
      request = request.header("X-Write-Key", writeKey);
    }
    return mockMvc.perform(request).andReturn();
  }

  @Test
  void test_sc18_쓰기_가드가_켜져_있으면_가입_완료도_키가_있어야_한다() throws Exception {
    // Given 서버가 WRITE_GUARD_ENABLED=true 와 WRITE_API_KEY=server-key 로 기동했다
    // And 유효한 pendingToken 이 있고 users·social_accounts 의 현재 행 수를 기록했다
    long usersBefore = userRepository.count();
    long socialBefore = socialAccountRepository.count();
    Map<String, String> wrongKeys = new LinkedHashMap<>();
    wrongKeys.put("헤더 없음", null);
    wrongKeys.put("빈 문자열", "");
    wrongKeys.put("다른 값", "other-key");
    SoftAssertions soft = new SoftAssertions();

    for (Map.Entry<String, String> row : wrongKeys.entrySet()) {
      // When POST /api/auth/social/complete 를 표의 X-Write-Key 로 호출한다
      MvcResult result = complete(row.getValue());

      // Then 키가 틀리면 pendingToken 이 유효해도 401 WRITE_KEY_INVALID 이고 행 수가 그대로다
      soft.assertThat(result.getResponse().getStatus()).as(row.getKey() + " — 상태").isEqualTo(401);
      soft.assertThat(code(result)).as(row.getKey() + " — code").isEqualTo("WRITE_KEY_INVALID");
      soft.assertThat(userRepository.count()).as(row.getKey() + " — users").isEqualTo(usersBefore);
      soft.assertThat(socialAccountRepository.count())
          .as(row.getKey() + " — social_accounts")
          .isEqualTo(socialBefore);
    }

    // And 키가 맞으면 SC-09 와 같은 결과(200, 가입 완료)다
    MvcResult ok = complete("server-key");
    soft.assertThat(ok.getResponse().getStatus()).as("server-key — 상태").isEqualTo(200);
    soft.assertThat(userRepository.count()).as("server-key — users").isEqualTo(usersBefore + 1);
    soft.assertThat(socialAccountRepository.count())
        .as("server-key — social_accounts")
        .isEqualTo(socialBefore + 1);
    soft.assertAll();
  }
}
