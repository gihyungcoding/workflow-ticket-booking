package com.example.ticket_booking.config;

import static com.example.ticket_booking.support.PendingTokenTestHelper.queryOf;

import com.example.ticket_booking.TicketBookingApplication;
import com.example.ticket_booking.support.FakeOAuth2User;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

/**
 * TASK-011 시나리오 SC-16, SC-19.
 *
 * <p>제공자 환경변수와 FRONTEND_BASE_URL 의 값은 실제 애플리케이션을 그 값으로 띄워(SpringApplicationBuilder, 서블릿, 임의 포트)
 * 확인한다. 환경변수는 기본 프로퍼티로 넣는다 — {@code @Value("${KAKAO_CLIENT_ID:}")} 가 읽는 경로와 같다. 공백만·앞뒤 공백 값이 명령행 인자
 * 파싱에서 사라지지 않게 인자가 아니라 프로퍼티 맵으로 넘긴다. 기동에 성공한 서버에 SC-01 의 콜백이 들어왔을 때의 {@code Location} 은 컨텍스트의
 * {@link OAuth2LoginSuccessHandler} 빈으로 확인한다(구현은 이 핸들러를 빈으로 등록해야 한다).
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-011.md
 */
class SocialProviderConfigTest {

  private static final String BASE = "https://app.example.test";
  private static final AtomicInteger DB_SEQUENCE = new AtomicInteger();
  private static final String[] PROVIDERS = {"KAKAO", "NAVER", "GOOGLE"};
  private static final Map<String, String> AUTHORIZE_HOST =
      Map.of("kakao", "kauth.kakao.com", "naver", "nid.naver.com", "google", "accounts.google.com");

  private final HttpClient client =
      HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

  // ---------- 기동 헬퍼 ----------

  private record Started(ConfigurableApplicationContext context, int port, Throwable failure) {
    boolean ok() {
      return failure == null;
    }

    String failureMessages() {
      List<String> messages = new ArrayList<>();
      for (Throwable t = failure; t != null; t = t.getCause()) {
        messages.add(String.valueOf(t.getMessage()));
      }
      return String.join(" | ", messages);
    }
  }

  private Started start(Map<String, Object> environment) {
    String url =
        "jdbc:h2:mem:social-config-"
            + DB_SEQUENCE.incrementAndGet()
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
    try {
      ConfigurableApplicationContext context =
          new SpringApplicationBuilder(TicketBookingApplication.class)
              .web(WebApplicationType.SERVLET)
              .properties(environment)
              .run("--server.port=0", "--spring.datasource.url=" + url);
      int port = Integer.parseInt(context.getEnvironment().getProperty("local.server.port"));
      return new Started(context, port, null);
    } catch (Throwable thrown) {
      return new Started(null, 0, thrown);
    }
  }

  private static void stop(Started started) {
    if (started.context() != null) {
      started.context().close();
    }
  }

  private HttpResponse<String> send(
      String method, int port, String path, String body, Map<String, String> headers)
      throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    for (Map.Entry<String, String> header : headers.entrySet()) {
      builder.header(header.getKey(), header.getValue());
    }
    if (body != null) {
      builder.header("Content-Type", "application/json");
      builder.method(method, HttpRequest.BodyPublishers.ofString(body));
    } else {
      builder.method(method, HttpRequest.BodyPublishers.noBody());
    }
    return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  /** 서버는 어느 경우에도 기동에 성공하고 이메일 가입은 201, 올바른 자격 증명 로그인은 200 이다. */
  private void assertEmailAuthWorks(SoftAssertions soft, String label, int port) throws Exception {
    String email = "user-" + DB_SEQUENCE.incrementAndGet() + "@example.com";
    String signUp =
        "{\"email\":\""
            + email
            + "\",\"password\":\"password123\",\"passwordConfirm\":\"password123\",\"name\":\"홍길동\",\"role\":\"AUDIENCE\"}";
    soft.assertThat(send("POST", port, "/api/auth/signup", signUp, Map.of()).statusCode())
        .as(label + " — 가입 상태")
        .isEqualTo(201);
    String login = "{\"email\":\"" + email + "\",\"password\":\"password123\"}";
    soft.assertThat(send("POST", port, "/api/auth/login", login, Map.of()).statusCode())
        .as(label + " — 로그인 상태")
        .isEqualTo(200);
  }

  // ---------- SC-16 ----------

  private enum Style {
    SET,
    UNSET,
    EMPTY,
    BLANK,
    ID_ONLY,
    SECRET_ONLY,
    PADDED
  }

  private static Map<String, Object> providerEnvironment(Style style) {
    Map<String, Object> environment = new LinkedHashMap<>();
    environment.put("FRONTEND_BASE_URL", BASE);
    for (String provider : PROVIDERS) {
      String id = "id-" + provider;
      String secret = "secret-" + provider;
      switch (style) {
        case SET -> {
          environment.put(provider + "_CLIENT_ID", id);
          environment.put(provider + "_CLIENT_SECRET", secret);
        }
        case UNSET -> {}
        case EMPTY -> {
          environment.put(provider + "_CLIENT_ID", "");
          environment.put(provider + "_CLIENT_SECRET", "");
        }
        case BLANK -> {
          environment.put(provider + "_CLIENT_ID", "   ");
          environment.put(provider + "_CLIENT_SECRET", "   ");
        }
        case ID_ONLY -> environment.put(provider + "_CLIENT_ID", id);
        case SECRET_ONLY -> environment.put(provider + "_CLIENT_SECRET", secret);
        case PADDED -> {
          environment.put(provider + "_CLIENT_ID", "  " + id + "  ");
          environment.put(provider + "_CLIENT_SECRET", "  " + secret + "  ");
        }
      }
    }
    return environment;
  }

  @Test
  void test_sc16_제공자_설정이_있는_제공자만_인가_요청을_받는다() throws Exception {
    SoftAssertions soft = new SoftAssertions();

    for (Style style : Style.values()) {
      // Given 서버가 FRONTEND_BASE_URL 과 표의 제공자 환경변수 값으로 기동했다
      Started started = start(providerEnvironment(style));
      soft.assertThat(started.ok())
          .as(style + " — 기동 성공 (" + started.failureMessages() + ")")
          .isTrue();
      if (!started.ok()) {
        continue;
      }
      try {
        boolean configured = style == Style.SET || style == Style.PADDED;
        for (Map.Entry<String, String> provider : AUTHORIZE_HOST.entrySet()) {
          // When GET /oauth2/authorization/<제공자> 를 호출한다
          HttpResponse<String> response =
              send(
                  "GET",
                  started.port(),
                  "/oauth2/authorization/" + provider.getKey(),
                  null,
                  Map.of());
          String label = style + " / " + provider.getKey();
          if (configured) {
            // Then 제공자의 인가 주소로 302 하고 state 파라미터가 있다
            soft.assertThat(response.statusCode()).as(label + " — 상태").isEqualTo(302);
            String location = response.headers().firstValue("location").orElse("");
            soft.assertThat(location)
                .as(label + " — Location")
                .startsWith("https://" + provider.getValue());
            soft.assertThat(queryOf(location)).as(label + " — 쿼리").containsKey("state");
          } else {
            // Then 제공자로 302 하지 않고 404 다
            soft.assertThat(response.statusCode()).as(label + " — 상태").isEqualTo(404);
          }
        }
        if (style == Style.SET) {
          // 요청 조작: redirect_uri 쿼리와 X-Forwarded-Host 헤더는 제공자로 보내는 콜백 주소에 영향을 주지 않는다
          HttpResponse<String> withQuery =
              send(
                  "GET",
                  started.port(),
                  "/oauth2/authorization/google?redirect_uri=https://evil.example/cb",
                  null,
                  Map.of());
          soft.assertThat(withQuery.statusCode()).as("redirect_uri 쿼리 — 상태").isEqualTo(302);
          soft.assertThat(
                  queryOf(withQuery.headers().firstValue("location").orElse(""))
                      .getOrDefault("redirect_uri", ""))
              .as("redirect_uri 쿼리 — Location 의 redirect_uri")
              .doesNotContain("evil.example");
          HttpResponse<String> withHeader =
              send(
                  "GET",
                  started.port(),
                  "/oauth2/authorization/google",
                  null,
                  Map.of("X-Forwarded-Host", "evil.example"));
          soft.assertThat(withHeader.statusCode()).as("X-Forwarded-Host — 상태").isEqualTo(302);
          soft.assertThat(
                  queryOf(withHeader.headers().firstValue("location").orElse(""))
                      .getOrDefault("redirect_uri", ""))
              .as("X-Forwarded-Host — Location 의 redirect_uri")
              .doesNotContain("evil.example");
          // 경로 변수: 알 수 없는 제공자·미등록 apple·대소문자 변형은 모두 404
          for (String path : List.of("foo", "apple", "GOOGLE", "Google")) {
            soft.assertThat(
                    send("GET", started.port(), "/oauth2/authorization/" + path, null, Map.of())
                        .statusCode())
                .as("경로 변수 " + path)
                .isEqualTo(404);
          }
        }
        // And 서버는 기동에 성공하고 가입·로그인이 동작한다
        assertEmailAuthWorks(soft, style.toString(), started.port());
      } finally {
        stop(started);
      }
    }
    soft.assertAll();
  }

  // ---------- SC-19 ----------

  private record BaseRow(String label, String value, String expectedPrefix) {
    boolean accepted() {
      return expectedPrefix != null;
    }
  }

  @Test
  void test_sc19_프론트_기준_주소_환경변수가_이상하면_기동하지_않거나_정리해서_쓴다() throws Exception {
    // Given 제공자 설정이 있다 (KAKAO_CLIENT_ID·KAKAO_CLIENT_SECRET 에 값이 있다)
    String pending = "/oauth2/redirect?pendingToken=";
    List<BaseRow> rows =
        List.of(
            new BaseRow("정상", "https://app.example.test", "https://app.example.test" + pending),
            new BaseRow(
                "끝 슬래시 1개", "https://app.example.test/", "https://app.example.test" + pending),
            new BaseRow(
                "끝 슬래시 2개", "https://app.example.test//", "https://app.example.test" + pending),
            new BaseRow(
                "앞뒤 공백", "  https://app.example.test  ", "https://app.example.test" + pending),
            new BaseRow("스킴 대문자", "HTTPS://app.example.test", "HTTPS://app.example.test" + pending),
            new BaseRow(
                "경로 포함", "https://app.example.test/app", "https://app.example.test/app" + pending),
            new BaseRow("쿼리", "https://app.example.test?x=1", null),
            new BaseRow("프래그먼트", "https://app.example.test#f", null),
            new BaseRow("사용자 정보", "https://user@evil.example", null),
            new BaseRow("호스트 없음", "https://", null),
            new BaseRow("접두사만 일치하는 스킴", "httpsfoo://x", null),
            new BaseRow("미설정", null, null),
            new BaseRow("빈 문자열", "", null),
            new BaseRow("공백만", "   ", null),
            new BaseRow("스킴 없음", "app.example.test", null),
            new BaseRow("javascript 스킴", "javascript:alert(1)", null),
            new BaseRow("ftp 스킴", "ftp://app.example.test", null));
    SoftAssertions soft = new SoftAssertions();

    for (BaseRow row : rows) {
      Map<String, Object> environment = new LinkedHashMap<>();
      environment.put("KAKAO_CLIENT_ID", "id-KAKAO");
      environment.put("KAKAO_CLIENT_SECRET", "secret-KAKAO");
      if (row.value() != null) {
        environment.put("FRONTEND_BASE_URL", row.value());
      }

      // When 서버를 기동한다
      Started started = start(environment);
      try {
        if (row.accepted()) {
          // Then 기동에 성공하고, SC-01 의 콜백이 들어오면 Location 이 표의 값으로 시작한다
          soft.assertThat(started.ok())
              .as(row.label() + " — 기동 성공 (" + started.failureMessages() + ")")
              .isTrue();
          if (started.ok()) {
            OAuth2LoginSuccessHandler handler =
                started.context().getBean(OAuth2LoginSuccessHandler.class);
            MockHttpServletResponse response = new MockHttpServletResponse();
            handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                new OAuth2AuthenticationToken(
                    new FakeOAuth2User(newKakaoUser()), List.of(), "kakao"));
            String location = response.getRedirectedUrl();
            soft.assertThat(location)
                .as(row.label() + " — Location")
                .startsWith(row.expectedPrefix());
            if (location != null) {
              soft.assertThat(location.substring(location.indexOf("://") + 3))
                  .as(row.label() + " — 스킴 뒤 //")
                  .doesNotContain("//");
            }
          }
        } else {
          // Then 기동 실패이고 오류 메시지에 FRONTEND_BASE_URL 이 있다
          soft.assertThat(started.ok()).as(row.label() + " — 기동 실패해야 한다").isFalse();
          if (!started.ok()) {
            soft.assertThat(started.failureMessages())
                .as(row.label() + " — 오류 메시지")
                .contains("FRONTEND_BASE_URL");
          }
        }
      } finally {
        stop(started);
      }
    }

    // 그리고 제공자 설정이 하나도 없으면 FRONTEND_BASE_URL 이 미설정이어도 기동에 성공하고 이메일 가입·로그인이 동작한다
    Started noProviders = start(new LinkedHashMap<>());
    soft.assertThat(noProviders.ok())
        .as("제공자 설정 없음 — 기동 성공 (" + noProviders.failureMessages() + ")")
        .isTrue();
    if (noProviders.ok()) {
      try {
        assertEmailAuthWorks(soft, "제공자 설정 없음", noProviders.port());
      } finally {
        stop(noProviders);
      }
    }
    soft.assertAll();
  }

  private static Map<String, Object> newKakaoUser() {
    Map<String, Object> profile = new LinkedHashMap<>();
    profile.put("nickname", "신규");
    Map<String, Object> account = new LinkedHashMap<>();
    account.put("email", "new@example.com");
    account.put("is_email_verified", true);
    account.put("is_email_valid", true);
    account.put("profile", profile);
    Map<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("id", 777);
    attributes.put("kakao_account", account);
    return attributes;
  }
}
