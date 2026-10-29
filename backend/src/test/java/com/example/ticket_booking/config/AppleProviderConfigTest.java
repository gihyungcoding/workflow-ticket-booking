package com.example.ticket_booking.config;

import static com.example.ticket_booking.support.PendingTokenTestHelper.queryOf;

import com.example.ticket_booking.TicketBookingApplication;
import com.example.ticket_booking.support.AppleTestKeys;
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

/**
 * TASK-012 시나리오 SC-11.
 *
 * <p>환경변수와 개인 키는 실제 애플리케이션을 그 값으로 띄워(SpringApplicationBuilder, 서블릿, 임의 포트) 확인한다. 환경변수는 기본 프로퍼티로 넣는다
 * — 공백만·앞뒤 공백·역슬래시 값이 명령행 인자 파싱에서 바뀌지 않게 인자가 아니라 프로퍼티 맵으로 넘긴다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-012.md
 */
class AppleProviderConfigTest {

  private static final String BASE = "https://app.example.test";
  private static final String APPLE_AUTHORIZE = "https://appleid.apple.com/auth/authorize";
  private static final AtomicInteger DB_SEQUENCE = new AtomicInteger();

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
        "jdbc:h2:mem:apple-config-"
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

  private HttpResponse<String> get(int port, String path) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  // ---------- 환경변수 헬퍼 ----------

  private static Map<String, Object> appleEnvironment(String privateKey) {
    Map<String, Object> environment = new LinkedHashMap<>();
    environment.put("FRONTEND_BASE_URL", BASE);
    environment.put("APPLE_CLIENT_ID", AppleTestKeys.CLIENT_ID);
    environment.put("APPLE_TEAM_ID", AppleTestKeys.TEAM_ID);
    environment.put("APPLE_KEY_ID", AppleTestKeys.KEY_ID);
    environment.put("APPLE_PRIVATE_KEY", privateKey);
    return environment;
  }

  private static Map<String, Object> valid() {
    return appleEnvironment(AppleTestKeys.validPem());
  }

  private static Map<String, Object> without(Map<String, Object> environment, String... keys) {
    for (String key : keys) {
      environment.remove(key);
    }
    return environment;
  }

  private static Map<String, Object> with(
      Map<String, Object> environment, String key, Object value) {
    environment.put(key, value);
    return environment;
  }

  // ---------- 표 ----------

  private enum Expect {
    /** 302, 규약의 애플 인가 요청 전체. */
    APPLE_AUTHORIZATION,
    /** 302, 애플 인가 주소로 시작한다. */
    APPLE_REDIRECT,
    /** 302, 애플 인가 주소이고 client_id 는 공백을 제거한 값. */
    APPLE_REDIRECT_TRIMMED,
    /** 404. */
    NOT_FOUND,
    /** 302, 구글 인가 주소이고 response_mode 가 없다. */
    GOOGLE_REDIRECT,
    /** 기동 실패. */
    START_FAILS
  }

  private record Row(
      int no,
      String label,
      Map<String, Object> environment,
      String path,
      Expect expect,
      String messageContains,
      String messageMustNotContain) {}

  private static final String APPLE_PATH = "/oauth2/authorization/apple";

  private static Row ok(int no, String label, Map<String, Object> env, Expect expect) {
    return new Row(no, label, env, APPLE_PATH, expect, null, null);
  }

  private static Row fails(
      int no, String label, Map<String, Object> env, String contains, String mustNotContain) {
    return new Row(no, label, env, APPLE_PATH, Expect.START_FAILS, contains, mustNotContain);
  }

  private static Map<String, Object> padded(Map<String, Object> environment) {
    Map<String, Object> result = new LinkedHashMap<>(environment);
    for (String key :
        List.of("APPLE_CLIENT_ID", "APPLE_TEAM_ID", "APPLE_KEY_ID", "APPLE_PRIVATE_KEY")) {
      result.put(key, "  " + environment.get(key) + "  ");
    }
    return result;
  }

  @Test
  void test_sc11_애플_설정이_갖춰진_경우에만_인가_요청을_받고_개인_키가_올바르지_않으면_기동하지_않는다() throws Exception {
    // Given 서버가 FRONTEND_BASE_URL 과 표의 환경변수로 기동을 시도한다 ("유효" 는 테스트가 만든 EC P-256 PKCS#8 PEM)
    List<Row> rows = new ArrayList<>();
    rows.add(ok(1, "네 값 모두 유효", valid(), Expect.APPLE_AUTHORIZATION));
    Map<String, Object> withGoogle =
        with(
            with(valid(), "GOOGLE_CLIENT_ID", "id-GOOGLE"),
            "GOOGLE_CLIENT_SECRET",
            "secret-GOOGLE");
    rows.add(
        new Row(
            2,
            "애플 + 구글 설정, 구글 인가 요청",
            withGoogle,
            "/oauth2/authorization/google",
            Expect.GOOGLE_REDIRECT,
            null,
            null));
    rows.add(
        ok(
            3,
            "애플 네 값 모두 미설정",
            without(
                valid(), "APPLE_CLIENT_ID", "APPLE_TEAM_ID", "APPLE_KEY_ID", "APPLE_PRIVATE_KEY"),
            Expect.NOT_FOUND));
    rows.add(ok(4, "APPLE_CLIENT_ID 만 없다", without(valid(), "APPLE_CLIENT_ID"), Expect.NOT_FOUND));
    rows.add(ok(5, "APPLE_TEAM_ID 만 없다", without(valid(), "APPLE_TEAM_ID"), Expect.NOT_FOUND));
    rows.add(ok(6, "APPLE_KEY_ID 만 없다", without(valid(), "APPLE_KEY_ID"), Expect.NOT_FOUND));
    rows.add(
        ok(7, "APPLE_PRIVATE_KEY 만 없다", without(valid(), "APPLE_PRIVATE_KEY"), Expect.NOT_FOUND));
    Map<String, Object> empty = valid();
    Map<String, Object> blank = valid();
    for (String key :
        List.of("APPLE_CLIENT_ID", "APPLE_TEAM_ID", "APPLE_KEY_ID", "APPLE_PRIVATE_KEY")) {
      empty.put(key, "");
      blank.put(key, "   ");
    }
    rows.add(ok(8, "네 값이 모두 빈 문자열", empty, Expect.NOT_FOUND));
    rows.add(ok(9, "네 값이 모두 공백만", blank, Expect.NOT_FOUND));
    for (String key :
        List.of("APPLE_CLIENT_ID", "APPLE_TEAM_ID", "APPLE_KEY_ID", "APPLE_PRIVATE_KEY")) {
      rows.add(ok(10, key + " 만 공백만", with(valid(), key, "   "), Expect.NOT_FOUND));
    }
    rows.add(ok(11, "네 값 모두 앞뒤 공백", padded(valid()), Expect.APPLE_REDIRECT_TRIMMED));
    rows.add(
        fails(
            12,
            "애플만 유효하고 FRONTEND_BASE_URL 없음",
            without(valid(), "FRONTEND_BASE_URL"),
            "FRONTEND_BASE_URL",
            null));
    rows.add(
        ok(
            13,
            "개인 키 PEM 실제 개행",
            appleEnvironment(AppleTestKeys.validPem()),
            Expect.APPLE_REDIRECT));
    rows.add(
        ok(
            14,
            "개인 키 개행이 두 글자 \\n",
            appleEnvironment(AppleTestKeys.escapedNewlinesPem()),
            Expect.APPLE_REDIRECT));
    rows.add(
        ok(
            15,
            "개인 키 헤더·푸터 없는 base64",
            appleEnvironment(AppleTestKeys.bareBase64()),
            Expect.APPLE_REDIRECT));
    rows.add(
        ok(
            16,
            "개인 키 PEM 앞뒤 공백·개행",
            appleEnvironment("\n  " + AppleTestKeys.validPem() + "  \n"),
            Expect.APPLE_REDIRECT));
    rows.add(
        fails(
            17, "개인 키 not-a-key", appleEnvironment("not-a-key"), "APPLE_PRIVATE_KEY", "not-a-key"));
    String rsa = AppleTestKeys.rsaPkcs8Pem();
    rows.add(
        fails(
            18,
            "개인 키 RSA PKCS#8",
            appleEnvironment(rsa),
            "APPLE_PRIVATE_KEY",
            AppleTestKeys.bodyFragment(rsa)));
    String p384 = AppleTestKeys.p384Pem();
    rows.add(
        fails(
            19,
            "개인 키 EC P-384",
            appleEnvironment(p384),
            "APPLE_PRIVATE_KEY",
            AppleTestKeys.bodyFragment(p384)));
    String sec1 = AppleTestKeys.sec1Pem();
    rows.add(
        fails(
            20,
            "개인 키 SEC1 BEGIN EC PRIVATE KEY",
            appleEnvironment(sec1),
            "APPLE_PRIVATE_KEY",
            AppleTestKeys.bodyFragment(sec1)));
    String publicPem = AppleTestKeys.publicKeyPem();
    rows.add(
        fails(
            21,
            "개인 키 자리에 공개 키",
            appleEnvironment(publicPem),
            "APPLE_PRIVATE_KEY",
            AppleTestKeys.bodyFragment(publicPem)));
    rows.add(
        fails(
            22,
            "개인 키 PEM 헤더·푸터만",
            appleEnvironment(AppleTestKeys.headerOnlyPem()),
            "APPLE_PRIVATE_KEY",
            null));
    rows.add(
        ok(
            23,
            "키는 깨졌지만 APPLE_TEAM_ID 가 없다",
            without(appleEnvironment("not-a-key"), "APPLE_TEAM_ID"),
            Expect.NOT_FOUND));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      String label = "#" + row.no() + " " + row.label();

      // When 기동에 성공하면 표의 요청을 보낸다
      Started started = start(row.environment());
      try {
        // Then 표의 결과가 된다
        if (row.expect() == Expect.START_FAILS) {
          soft.assertThat(started.ok()).as(label + " — 기동 실패해야 한다").isFalse();
          if (!started.ok()) {
            soft.assertThat(started.failureMessages())
                .as(label + " — 오류 메시지")
                .contains(row.messageContains());
            if (row.messageMustNotContain() != null) {
              soft.assertThat(started.failureMessages())
                  .as(label + " — 오류 메시지에 키 본문이 없다")
                  .doesNotContain(row.messageMustNotContain());
            }
          }
          continue;
        }
        soft.assertThat(started.ok())
            .as(label + " — 기동 성공 (" + started.failureMessages() + ")")
            .isTrue();
        if (!started.ok()) {
          continue;
        }
        HttpResponse<String> response = get(started.port(), row.path());
        String location = response.headers().firstValue("location").orElse("");
        switch (row.expect()) {
          case NOT_FOUND ->
              soft.assertThat(response.statusCode()).as(label + " — 상태").isEqualTo(404);
          case GOOGLE_REDIRECT -> {
            soft.assertThat(response.statusCode()).as(label + " — 상태").isEqualTo(302);
            soft.assertThat(location)
                .as(label + " — Location")
                .startsWith("https://accounts.google.com");
            soft.assertThat(queryOf(location))
                .as(label + " — 구글 인가 요청에는 response_mode 가 없다")
                .doesNotContainKey("response_mode");
          }
          case APPLE_REDIRECT, APPLE_REDIRECT_TRIMMED, APPLE_AUTHORIZATION -> {
            soft.assertThat(response.statusCode()).as(label + " — 상태").isEqualTo(302);
            soft.assertThat(location).as(label + " — Location").startsWith(APPLE_AUTHORIZE);
            Map<String, String> query = queryOf(location);
            if (row.expect() == Expect.APPLE_REDIRECT_TRIMMED) {
              soft.assertThat(query.get("client_id"))
                  .as(label + " — 공백을 제거한 client_id")
                  .isEqualTo(AppleTestKeys.CLIENT_ID);
            }
            if (row.expect() == Expect.APPLE_AUTHORIZATION) {
              soft.assertThat(query.get("client_id"))
                  .as(label + " — client_id")
                  .isEqualTo(AppleTestKeys.CLIENT_ID);
              soft.assertThat(query.get("response_type"))
                  .as(label + " — response_type")
                  .isEqualTo("code");
              soft.assertThat(query.get("scope"))
                  .as(label + " — scope")
                  .isEqualTo("openid name email");
              soft.assertThat(query.get("response_mode"))
                  .as(label + " — response_mode")
                  .isEqualTo("form_post");
              soft.assertThat(query.getOrDefault("redirect_uri", ""))
                  .as(label + " — redirect_uri")
                  .endsWith("/login/oauth2/code/apple");
              soft.assertThat(query.get("state")).as(label + " — state").isNotBlank();
              soft.assertThat(query.get("nonce")).as(label + " — nonce").isNotBlank();
            }
          }
          default -> throw new IllegalStateException(row.expect().name());
        }
      } finally {
        stop(started);
      }
    }
    soft.assertAll();
  }
}
