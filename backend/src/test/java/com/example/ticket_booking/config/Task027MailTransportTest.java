package com.example.ticket_booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticket_booking.service.AccountMails;
import com.example.ticket_booking.service.MailGateway;
import com.example.ticket_booking.service.MailMessage;
import com.example.ticket_booking.support.BareBoot;
import com.example.ticket_booking.support.BareBoot.Started;
import com.example.ticket_booking.support.FakeSmtpServer;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * TASK-027 SC-01~SC-05·SC-20 — 메일 전송 방식 선택과 기동 검증. 실제 애플리케이션을 설정 파일 없이 띄운다({@link BareBoot}, 머리말
 * 「기동」).
 *
 * <p>환경변수 {@code SPRING_MAIL_HOST}·{@code SPRING_MAIL_PORT} 는 운영에서 Spring Boot 완화 바인딩으로 {@code
 * spring.mail.host}·{@code spring.mail.port} 가 된다. 명령행 인자에는 완화 바인딩이 없어 정식 이름으로 넣는다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-027.md
 */
@ExtendWith(OutputCaptureExtension.class)
class Task027MailTransportTest {

  private static final String FROM = "no-reply@ticket.on";
  private static final String TO = "e@ticket.on";
  private static final String SUBJECT = "이메일 인증을 해 주세요";
  private static final String LINK = "https://app.example/verify-email#token=abc";

  private static final MailMessage MESSAGE = new MailMessage(TO, SUBJECT, "링크 " + LINK);

  private static final Object UNSET = new Object();

  private final HttpClient client =
      HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

  /** 값 표기: {@link #UNSET} 이면 그 키를 뺀다(「키 없음」). */
  private static Map<String, Object> env(Object... pairs) {
    Map<String, Object> values = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      values.put((String) pairs[i], pairs[i + 1] == UNSET ? null : pairs[i + 1]);
    }
    return values;
  }

  // ---------- SC-01 ----------

  /**
   * SC-01 (happy) log 전송이면 메일이 로그로 남는다.
   *
   * <p>Given 애플리케이션이 MAIL_TRANSPORT=log 로 기동했다(테스트 수집 구현으로 바꾸지 않는다), MAIL_FROM 은 설정하지 않았다 When
   * MailGateway.send(메일) 를 부른다 Then 예외 없이 끝나고, 출력 캡처에 받는 사람·제목·링크가 있고, 선택된 빈이 LoggingMailGateway 다
   */
  @Test
  void test_sc01_log_전송이면_메일이_로그로_남는다(CapturedOutput output) {
    // Given
    ConfigurableApplicationContext context =
        BareBoot.run(env("MAIL_TRANSPORT", "log", "MAIL_FROM", UNSET));
    try {
      MailGateway gateway = context.getBean(MailGateway.class);

      // When
      gateway.send(MESSAGE);

      // Then
      assertThat(output.getOut()).contains(TO).contains(SUBJECT).contains(LINK);
      assertThat(gateway).isInstanceOf(LoggingMailGateway.class);
    } finally {
      context.close();
    }
  }

  // ---------- SC-02 ----------

  /**
   * SC-02 (happy) smtp 전송이면 SMTP 서버가 메일을 받는다.
   *
   * <p>Given 테스트용 SMTP 서버가 임의 포트에서 떠 있고, 애플리케이션이 smtp·호스트·포트·MAIL_FROM 으로 기동했다 When SC-01 과 같은 메일을
   * 보낸다 Then SMTP 서버가 1통을 받고 From·To·제목·본문 링크가 그대로이며, 선택된 빈이 SmtpMailGateway 다
   */
  @Test
  void test_sc02_smtp_전송이면_SMTP_서버가_메일을_받는다() throws Exception {
    try (FakeSmtpServer smtp = new FakeSmtpServer()) {
      // Given
      ConfigurableApplicationContext context =
          BareBoot.run(
              env(
                  "MAIL_TRANSPORT",
                  "smtp",
                  "spring.mail.host",
                  "localhost",
                  "spring.mail.port",
                  String.valueOf(smtp.port()),
                  "MAIL_FROM",
                  FROM));
      try {
        MailGateway gateway = context.getBean(MailGateway.class);

        // When
        gateway.send(MESSAGE);

        // Then
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> !smtp.received().isEmpty());
        assertThat(smtp.received()).hasSize(1);
        MimeMessage received = smtp.received().get(0);
        assertThat(((InternetAddress) received.getFrom()[0]).getAddress()).isEqualTo(FROM);
        assertThat(((InternetAddress) received.getAllRecipients()[0]).getAddress()).isEqualTo(TO);
        assertThat(received.getSubject()).isEqualTo(SUBJECT);
        assertThat(String.valueOf(received.getContent())).contains(LINK);
        assertThat(gateway).isInstanceOf(SmtpMailGateway.class);
      } finally {
        context.close();
      }
    }
  }

  // ---------- SC-03 ----------

  private record TransportRow(String row, Object value, Class<?> gateway) {}

  /**
   * SC-03 (error) MAIL_TRANSPORT 가 정의 밖이면 기동이 실패하고 정의된 값이면 그 구현을 쓴다.
   *
   * <p>Given 설정 파일을 읽지 않는 기동에 기본 값을 넣었다(smtp 행은 호스트·MAIL_FROM 도) When 표의 MAIL_TRANSPORT 로 기동한다 Then
   * 결과 열대로 — 실패 행은 예외 체인에 MAIL_TRANSPORT, 성공 행은 구현 열의 MailGateway 빈
   */
  @Test
  void test_sc03_MAIL_TRANSPORT_가_정의_밖이면_기동이_실패하고_정의된_값이면_그_구현을_쓴다() {
    TransportRow[] rows = {
      new TransportRow("a", UNSET, null),
      new TransportRow("b", "", null),
      new TransportRow("c", "   ", null),
      new TransportRow("d", "log", LoggingMailGateway.class),
      new TransportRow("e", "smtp", SmtpMailGateway.class),
      new TransportRow("f", "LOG", LoggingMailGateway.class),
      new TransportRow("g", "Smtp", SmtpMailGateway.class),
      new TransportRow("h", "  log  ", LoggingMailGateway.class),
      new TransportRow("i", "1", null),
      new TransportRow("j", "yes", null),
      new TransportRow("k", "true", null),
      new TransportRow("l", "smtp,log", null),
      new TransportRow("m", "log,", null),
      new TransportRow("n", "lo", null),
      new TransportRow("o", "logging", null),
      new TransportRow("p", "smtps", null),
      new TransportRow("q", "ses", null),
    };
    SoftAssertions soft = new SoftAssertions();
    for (TransportRow row : rows) {
      // Given
      Map<String, Object> values = env("MAIL_TRANSPORT", row.value());
      values.put("spring.mail.host", "localhost");
      values.put("MAIL_FROM", FROM);

      // When
      Started started = BareBoot.start(values);
      try {
        // Then
        String label = "SC-03 " + row.row() + " MAIL_TRANSPORT=" + describe(row.value());
        if (row.gateway() == null) {
          soft.assertThat(started.ok()).as(label + " — 기동 실패여야 한다").isFalse();
          if (!started.ok()) {
            soft.assertThat(started.failureMessages()).as(label).contains("MAIL_TRANSPORT");
          }
        } else {
          soft.assertThat(started.ok())
              .as(label + " — 기동 성공 (" + started.failureMessages() + ")")
              .isTrue();
          if (started.ok()) {
            soft.assertThat(gatewayOf(started)).as(label).isInstanceOf(row.gateway());
          }
        }
      } finally {
        started.close();
      }
    }
    soft.assertAll();
  }

  // ---------- SC-04 ----------

  private record SmtpRow(String row, Object transport, Object host, Object from, String name) {}

  /**
   * SC-04 (error) smtp 인데 SMTP 호스트나 보내는 사람이 없으면 기동이 실패한다.
   *
   * <p>Given 기본 값에 MAIL_TRANSPORT=smtp When 표의 SPRING_MAIL_HOST·MAIL_FROM 으로 기동한다 Then 결과 열대로 — 실패
   * 행은 예외 체인에 이름 열의 변수 이름
   */
  @Test
  void test_sc04_smtp_인데_SMTP_호스트나_보내는_사람이_없으면_기동이_실패한다() {
    SmtpRow[] rows = {
      new SmtpRow("a", "smtp", UNSET, FROM, "SPRING_MAIL_HOST"),
      new SmtpRow("b", "smtp", "", FROM, "SPRING_MAIL_HOST"),
      new SmtpRow("c", "smtp", "   ", FROM, "SPRING_MAIL_HOST"),
      new SmtpRow("d", "smtp", "localhost", UNSET, "MAIL_FROM"),
      new SmtpRow("e", "smtp", "localhost", "", "MAIL_FROM"),
      new SmtpRow("f", "smtp", "localhost", "   ", "MAIL_FROM"),
      new SmtpRow("g", "smtp", "localhost", FROM, null),
      new SmtpRow("h", "log", UNSET, UNSET, null),
    };
    SoftAssertions soft = new SoftAssertions();
    for (SmtpRow row : rows) {
      // Given
      Map<String, Object> values =
          env(
              "MAIL_TRANSPORT",
              row.transport(),
              "spring.mail.host",
              row.host(),
              "MAIL_FROM",
              row.from());

      // When
      Started started = BareBoot.start(values);
      try {
        // Then
        String label =
            "SC-04 "
                + row.row()
                + " host="
                + describe(row.host())
                + " from="
                + describe(row.from());
        if (row.name() != null) {
          soft.assertThat(started.ok()).as(label + " — 기동 실패여야 한다").isFalse();
          if (!started.ok()) {
            soft.assertThat(started.failureMessages()).as(label).contains(row.name());
          }
        } else {
          soft.assertThat(started.ok())
              .as(label + " — 기동 성공 (" + started.failureMessages() + ")")
              .isTrue();
          if (started.ok()) {
            // 성공은 메일 구현이 실제로 골라진 상태여야 한다 — 빈이 없는 채로 뜬 것은 성공이 아니다
            soft.assertThat(gatewayOf(started)).as(label).isNotNull();
          }
        }
      } finally {
        started.close();
      }
    }
    soft.assertAll();
  }

  // ---------- SC-05 ----------

  private record BaseUrlRow(String row, Object value, String linkPrefix) {}

  /**
   * SC-05 (error) 소셜 제공자가 없어도 FRONTEND_BASE_URL 이 없거나 틀리면 기동이 실패한다.
   *
   * <p>Given 소셜 제공자 환경변수는 하나도 없고 나머지는 기본 값 When 표의 FRONTEND_BASE_URL 로 기동한다 Then 실패 행은 예외 체인에
   * FRONTEND_BASE_URL, 성공 행은 인증 메일 링크가 링크 앞부분 열로 시작한다
   */
  @Test
  void test_sc05_소셜_제공자가_없어도_FRONTEND_BASE_URL_이_없거나_틀리면_기동이_실패한다() {
    BaseUrlRow[] rows = {
      new BaseUrlRow("a", UNSET, null),
      new BaseUrlRow("b", "", null),
      new BaseUrlRow("c", "   ", null),
      new BaseUrlRow("d", "ftp://app.example", null),
      new BaseUrlRow("e", "https://user@app.example", null),
      new BaseUrlRow("f", "https://app.example?x=1", null),
      new BaseUrlRow("g", "https://app.example#", null),
      new BaseUrlRow("h", "https://", null),
      new BaseUrlRow("i", "app.example", null),
      new BaseUrlRow("j", "https://app.example", "https://app.example/verify-email#token="),
      new BaseUrlRow("k", "  https://app.example/  ", "https://app.example/verify-email#token="),
      new BaseUrlRow("l", "https://app.example///", "https://app.example/verify-email#token="),
      new BaseUrlRow("m", "HTTPS://App.Example", "HTTPS://App.Example/verify-email#token="),
      new BaseUrlRow("n", "http://localhost:5173", "http://localhost:5173/verify-email#token="),
    };
    SoftAssertions soft = new SoftAssertions();
    for (BaseUrlRow row : rows) {
      // Given · When
      Started started = BareBoot.start(env("FRONTEND_BASE_URL", row.value()));
      try {
        // Then
        String label = "SC-05 " + row.row() + " FRONTEND_BASE_URL=" + describe(row.value());
        if (row.linkPrefix() == null) {
          soft.assertThat(started.ok()).as(label + " — 기동 실패여야 한다").isFalse();
          if (!started.ok()) {
            soft.assertThat(started.failureMessages()).as(label).contains("FRONTEND_BASE_URL");
          }
        } else {
          soft.assertThat(started.ok())
              .as(label + " — 기동 성공 (" + started.failureMessages() + ")")
              .isTrue();
          if (started.ok()) {
            soft.assertThat(verificationBody(started, "r"))
                .as(label)
                .contains(row.linkPrefix() + "r");
          }
        }
      } finally {
        started.close();
      }
    }
    soft.assertAll();
  }

  // ---------- SC-20 ----------

  private record SocialRow(String row, Object value, String location) {}

  /**
   * SC-20 (regression) 소셜 제공자가 설정된 기동은 지금과 같고 소셜 실패 이동도 같은 기준 주소를 쓴다.
   *
   * <p>Given 기본 값에 KAKAO 제공자 값을 넣었다 When 표의 FRONTEND_BASE_URL 로 기동하고 성공하면 소셜 실패 콜백을 보낸다 Then 실패 행은
   * 예외 체인에 FRONTEND_BASE_URL, 성공 행은 302·Location 이 이동 열이고 같은 기동의 인증 메일 링크가 https://app.example 기준이다
   */
  @Test
  void test_sc20_소셜_제공자가_설정된_기동은_지금과_같고_소셜_실패_이동도_같은_기준_주소를_쓴다() throws Exception {
    SocialRow[] rows = {
      new SocialRow("a", "https://app.example/", "https://app.example/login?error=social"),
      new SocialRow("b", UNSET, null),
      new SocialRow("c", "https://app.example?x=1", null),
    };
    SoftAssertions soft = new SoftAssertions();
    for (SocialRow row : rows) {
      // Given
      Map<String, Object> values = new HashMap<>(env("FRONTEND_BASE_URL", row.value()));
      values.put("KAKAO_CLIENT_ID", "id-KAKAO");
      values.put("KAKAO_CLIENT_SECRET", "secret-KAKAO");

      // When
      Started started = BareBoot.start(values);
      try {
        // Then
        String label = "SC-20 " + row.row() + " FRONTEND_BASE_URL=" + describe(row.value());
        if (row.location() == null) {
          soft.assertThat(started.ok()).as(label + " — 기동 실패여야 한다").isFalse();
          if (!started.ok()) {
            soft.assertThat(started.failureMessages()).as(label).contains("FRONTEND_BASE_URL");
          }
          continue;
        }
        soft.assertThat(started.ok())
            .as(label + " — 기동 성공 (" + started.failureMessages() + ")")
            .isTrue();
        if (!started.ok()) {
          continue;
        }
        HttpResponse<String> response =
            client.send(
                HttpRequest.newBuilder(
                        URI.create(
                            "http://localhost:"
                                + started.port()
                                + "/login/oauth2/code/kakao?error=access_denied&state=x"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());
        soft.assertThat(response.statusCode()).as(label + " — 상태").isEqualTo(302);
        soft.assertThat(response.headers().firstValue("location").orElse(""))
            .as(label + " — Location")
            .isEqualTo(row.location());
        soft.assertThat(verificationBody(started, "r"))
            .as(label + " — 메일 링크")
            .contains("https://app.example/verify-email#token=r");
      } finally {
        started.close();
      }
    }
    soft.assertAll();
  }

  // ---------- SC-22 ----------

  private record TimeoutRow(
      String row,
      Object connection,
      Object read,
      Object write,
      Duration atLeast,
      Duration within,
      String failName) {}

  /**
   * SC-22 (boundary) SMTP 서버가 응답하지 않아도 발송은 타임아웃으로 끝나고 타임아웃 설정이 틀리면 기동이 실패한다.
   *
   * <p>Given 블랙홀 SMTP 서버, smtp·호스트·포트·MAIL_FROM 과 표의 타임아웃 설정으로 기동한다 When 기동이 성공하면 send 를 부른다 Then
   * 결과가 시간이면 그 범위 안에 예외로 끝나고, 기동 실패면 예외 체인에 이름 열의 환경변수 이름이 있다
   */
  @Test
  void test_sc22_SMTP_서버가_응답하지_않아도_발송은_타임아웃으로_끝나고_타임아웃_설정이_틀리면_기동이_실패한다() throws Exception {
    Duration s1 = Duration.ofSeconds(1);
    Duration s5 = Duration.ofSeconds(5);
    Duration s9 = Duration.ofSeconds(9);
    Duration s20 = Duration.ofSeconds(20);
    TimeoutRow[] rows = {
      new TimeoutRow("a", UNSET, UNSET, UNSET, s9, s20, null),
      new TimeoutRow("b", "1000", "1000", "1000", s1, s5, null),
      new TimeoutRow("c", "1000", "", "1000", s9, s20, null),
      new TimeoutRow("d", "1000", "   ", "1000", s9, s20, null),
      new TimeoutRow("e", UNSET, " 1000 ", UNSET, s1, s5, null),
      new TimeoutRow(
          "f", "1000", "0", "1000", null, null, "SPRING_MAIL_PROPERTIES_MAIL_SMTP_TIMEOUT"),
      new TimeoutRow(
          "g", "1000", "-5", "1000", null, null, "SPRING_MAIL_PROPERTIES_MAIL_SMTP_TIMEOUT"),
      new TimeoutRow(
          "h",
          "abc",
          "1000",
          "1000",
          null,
          null,
          "SPRING_MAIL_PROPERTIES_MAIL_SMTP_CONNECTIONTIMEOUT"),
      new TimeoutRow(
          "i", "1000", "1000", "1e3", null, null, "SPRING_MAIL_PROPERTIES_MAIL_SMTP_WRITETIMEOUT"),
      new TimeoutRow(
          "j", "1000", "1.5", "1000", null, null, "SPRING_MAIL_PROPERTIES_MAIL_SMTP_TIMEOUT"),
    };
    SoftAssertions soft = new SoftAssertions();
    try (com.example.ticket_booking.support.BlackholeSmtpServer blackhole =
        new com.example.ticket_booking.support.BlackholeSmtpServer()) {
      for (TimeoutRow row : rows) {
        String label = "SC-22 " + row.row();
        // Given
        Map<String, Object> values =
            env(
                "MAIL_TRANSPORT", "smtp",
                "spring.mail.host", "localhost",
                "spring.mail.port", String.valueOf(blackhole.port()),
                "MAIL_FROM", FROM,
                "spring.mail.properties.mail.smtp.connectiontimeout", row.connection(),
                "spring.mail.properties.mail.smtp.timeout", row.read(),
                "spring.mail.properties.mail.smtp.writetimeout", row.write());
        Started started = BareBoot.start(values);
        try {
          if (row.failName() != null) {
            // Then 기동 실패
            soft.assertThat(started.ok()).as(label + " — 기동 실패여야 한다").isFalse();
            if (!started.ok()) {
              soft.assertThat(started.failureMessages()).as(label).contains(row.failName());
            }
            continue;
          }
          soft.assertThat(started.ok())
              .as(label + " — 기동 성공 (" + started.failureMessages() + ")")
              .isTrue();
          if (!started.ok()) {
            continue;
          }
          MailGateway gateway = started.context().getBean(MailGateway.class);

          // When
          // 행마다 새 데몬 스레드 — 무한 대기한 행의 스레드가 다음 행을 막지 않게
          java.util.concurrent.ExecutorService runner =
              java.util.concurrent.Executors.newSingleThreadExecutor(
                  task -> {
                    Thread thread = new Thread(task, "sc22-send-" + row.row());
                    thread.setDaemon(true);
                    return thread;
                  });
          long begin = System.nanoTime();
          java.util.concurrent.Future<Throwable> sending =
              runner.submit(
                  () -> {
                    try {
                      gateway.send(MESSAGE);
                      return null;
                    } catch (Throwable thrown) {
                      return thrown;
                    }
                  });
          Throwable thrown;
          try {
            thrown =
                sending.get(
                    row.within().toMillis() + 2_000, java.util.concurrent.TimeUnit.MILLISECONDS);
          } catch (java.util.concurrent.TimeoutException hanging) {
            soft.fail(label + " — " + row.within() + " 안에 끝나지 않았다(무한 대기)");
            runner.shutdownNow();
            continue;
          }
          runner.shutdownNow();
          Duration elapsed = Duration.ofNanos(System.nanoTime() - begin);

          // Then
          soft.assertThat(thrown).as(label + " — 예외로 끝난다").isNotNull();
          soft.assertThat(elapsed).as(label + " — 걸린 시간 하한").isGreaterThanOrEqualTo(row.atLeast());
          soft.assertThat(elapsed).as(label + " — 걸린 시간 상한").isLessThanOrEqualTo(row.within());
        } finally {
          started.close();
        }
      }
    }
    soft.assertAll();
  }

  // ---------- 도우미 ----------

  private static MailGateway gatewayOf(Started started) {
    Map<String, MailGateway> gateways = started.context().getBeansOfType(MailGateway.class);
    return gateways.size() == 1 ? gateways.values().iterator().next() : null;
  }

  /** 기동한 컨텍스트의 AccountMails 로 인증 메일 본문을 만든다. 만들지 못하면 그 예외를 문자열로 돌려준다(단언 메시지에 남게). */
  private static String verificationBody(Started started, String rawToken) {
    try {
      return started.context().getBean(AccountMails.class).verification(TO, rawToken).body();
    } catch (RuntimeException exception) {
      return "<메일을 만들지 못했다: " + exception + ">";
    }
  }

  private static String describe(Object value) {
    return value == UNSET || value == null ? "(키 없음)" : "\"" + value + "\"";
  }
}
