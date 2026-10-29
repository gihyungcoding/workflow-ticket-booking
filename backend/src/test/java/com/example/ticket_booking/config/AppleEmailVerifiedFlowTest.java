package com.example.ticket_booking.config;

import static com.example.ticket_booking.support.AuthTestHelper.payloadOf;
import static com.example.ticket_booking.support.PendingTokenTestHelper.queryOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.ticket_booking.domain.SocialProvider;
import com.example.ticket_booking.domain.User;
import com.example.ticket_booking.domain.UserRole;
import com.example.ticket_booking.repository.SocialAccountRepository;
import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.AppleTestKeys;
import com.example.ticket_booking.support.FakeProviderServer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Phase 4 1라운드 RC-1 의 회귀 테스트 — id_token 의 {@code email_verified} 가 <b>운영 경로(Spring 의 OIDC 디코더)를 거친
 * 뒤에도</b> 불리언 true 또는 정확히 문자열 "true" 일 때만 검증됨으로 읽히는지.
 *
 * <p>SC-06 은 attributes 를 직접 주입하는 핸들러 테스트라 디코더의 클레임 변환(문자열 → Boolean, 대소문자 무시)을 거치지 않았다. 이 테스트는 가짜
 * 애플 서버가 서명한 id_token 을 실제 필터 체인에 통과시켜, 검증된 기존 계정이 있을 때 <b>자동 연결(검증됨)</b>이 되는지 <b>가입 대기(미검증)</b>인지를
 * 본다. 승인된 테스트 파일은 고치지 않고 새 파일로 추가했다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-012.md 규약 「이메일 검증됨」,
 * workflow_design/07_verify/VERIFY_TASK-012.json RC-1
 */
@SpringBootTest
@AutoConfigureMockMvc
class AppleEmailVerifiedFlowTest {

  private static final String BASE = "https://app.example.test";
  private static final FakeProviderServer FAKE = new FakeProviderServer();

  @DynamicPropertySource
  static void providerEnvironment(DynamicPropertyRegistry registry) {
    registry.add("FRONTEND_BASE_URL", () -> BASE);
    registry.add("APPLE_CLIENT_ID", () -> AppleTestKeys.CLIENT_ID);
    registry.add("APPLE_TEAM_ID", () -> AppleTestKeys.TEAM_ID);
    registry.add("APPLE_KEY_ID", () -> AppleTestKeys.KEY_ID);
    registry.add("APPLE_PRIVATE_KEY", AppleTestKeys::validPem);
  }

  /** 애플 등록의 토큰·JWKS 주소만 가짜 서버로 바꾼다. */
  @TestConfiguration
  static class FakeEndpoints {
    @Bean
    static BeanPostProcessor fakeAppleEndpoints() {
      return new BeanPostProcessor() {
        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
          if (!(bean instanceof InMemoryClientRegistrationRepository repository)) {
            return bean;
          }
          List<ClientRegistration> mapped = new ArrayList<>();
          for (ClientRegistration registration : repository) {
            mapped.add(FAKE.redirect(registration));
          }
          return new InMemoryClientRegistrationRepository(mapped);
        }
      };
    }
  }

  @AfterAll
  static void stopFake() {
    FAKE.stop();
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private SocialAccountRepository socialAccountRepository;

  /** claims 에서 해당 클레임을 아예 넣지 않는다는 표시. */
  private static final Object ABSENT = new Object();

  private record Row(String label, Object value, boolean verified) {}

  @BeforeEach
  void reset() {
    socialAccountRepository.deleteAll();
    userRepository.deleteAll();
    FAKE.reset();
  }

  @Test
  void test_rc1_운영_경로를_거친_뒤에도_email_verified_는_불리언_true_또는_정확히_문자열_true_일_때만_검증됨이다()
      throws Exception {
    // Given 제공자가 검증한 이메일로 소셜 가입을 끝낸 사용자 A(full@example.com, email_verified=true)가 있다
    User a = userRepository.save(new User("full@example.com", null, "기존", UserRole.AUDIENCE, true));
    List<Row> rows =
        List.of(
            new Row("불리언 true", true, true),
            new Row("문자열 \"true\"", "true", true),
            new Row("문자열 \"TRUE\"", "TRUE", false),
            new Row("문자열 \"True\"", "True", false),
            new Row("문자열 \"tRue\"", "tRue", false),
            new Row("문자열 \" true\"", " true", false),
            new Row("문자열 \"true \"", "true ", false),
            new Row("문자열 \"yes\"", "yes", false),
            new Row("문자열 \"1\"", "1", false),
            new Row("문자열 \"on\"", "on", false),
            new Row("정수 1", 1, false),
            new Row("불리언 false", false, false),
            new Row("문자열 \"false\"", "false", false),
            new Row("클레임 누락", ABSENT, false));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      socialAccountRepository.deleteAll();
      FAKE.reset();
      // And 사용자가 GET /oauth2/authorization/apple 로 시작해 애플로 보내졌다
      MvcResult start = mockMvc.perform(get("/oauth2/authorization/apple")).andReturn();
      assertThat(start.getResponse().getStatus()).as("인가 요청 상태").isEqualTo(302);
      Map<String, String> query = queryOf(start.getResponse().getRedirectedUrl());
      // And 애플이 서명한 id_token 의 email_verified 가 표의 값이다 (이메일은 A 와 같다)
      Map<String, Object> claims = new LinkedHashMap<>();
      claims.put("iss", FakeProviderServer.ISSUER);
      claims.put("aud", AppleTestKeys.CLIENT_ID);
      claims.put("sub", "a-rc1");
      claims.put("email", "full@example.com");
      claims.put("nonce", query.get("nonce"));
      claims.put("iat", Instant.now().minusSeconds(60));
      claims.put("exp", Instant.now().plusSeconds(3600));
      if (row.value() != ABSENT) {
        claims.put("email_verified", row.value());
      }
      FAKE.respondJson(
          "/apple/token",
          "{\"access_token\":\"x\",\"token_type\":\"Bearer\",\"expires_in\":3600,\"id_token\":\""
              + FAKE.idToken(claims)
              + "\"}");

      // When 애플이 브라우저를 통해 POST /login/oauth2/code/apple 로 콜백한다
      MvcResult callback =
          mockMvc
              .perform(
                  post("/login/oauth2/code/apple")
                      .session((MockHttpSession) start.getRequest().getSession())
                      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                      .param("code", "code-rc1")
                      .param("state", query.get("state")))
              .andReturn();
      String location = callback.getResponse().getRedirectedUrl();

      if (row.verified()) {
        // Then 검증됨 — 기존 계정에 연결되고 로그인 주소로 이동한다
        soft.assertThat(location)
            .as(row.label() + " — 로그인 주소")
            .startsWith(BASE + "/oauth2/redirect?token=");
        soft.assertThat(socialAccountRepository.findAll())
            .as(row.label() + " — social_accounts")
            .hasSize(1);
        if (!socialAccountRepository.findAll().isEmpty()) {
          soft.assertThat(socialAccountRepository.findAll().get(0).getProvider())
              .isEqualTo(SocialProvider.APPLE);
          soft.assertThat(socialAccountRepository.findAll().get(0).getUserId())
              .isEqualTo(a.getId());
        }
      } else {
        // Then 미검증 — 연결하지 않고 가입 대기가 된다 (emailVerified 클레임은 false)
        soft.assertThat(location)
            .as(row.label() + " — 가입 대기 주소")
            .startsWith(BASE + "/oauth2/redirect?pendingToken=");
        soft.assertThat(socialAccountRepository.count()).as(row.label() + " — 연결 없음").isZero();
        String pendingToken = queryOf(location).get("pendingToken");
        if (pendingToken != null) {
          soft.assertThat(payloadOf(pendingToken).get("emailVerified"))
              .as(row.label() + " — pendingToken.emailVerified")
              .isEqualTo(false);
        }
      }
    }
    soft.assertAll();
  }
}
