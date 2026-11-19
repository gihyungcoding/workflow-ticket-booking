package com.example.ticket_booking.service;

import static com.example.ticket_booking.domain.EmailTokenPurpose.VERIFY_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.CapturingMailConfig;
import com.example.ticket_booking.support.CapturingMailGateway;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import com.example.ticket_booking.support.PostgresTestContainer;
import com.example.ticket_booking.support.Task027Db;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.assertj.core.api.SoftAssertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * TASK-027 SC-12~SC-17 — 간격 제한 발송, 메일 문구·링크, 커밋 뒤 비동기 발송. PostgreSQL(Flyway 스키마) 위에서 돌고 클래스 레벨
 * {@code @Transactional} 없이 JDBC 로 새로 읽는다. 메일은 {@link CapturingMailGateway} 가 모은다(머리말 「메일」).
 *
 * <p>「보내지 않는다」는 같은 테스트의 모든 호출이 끝난 뒤 5초를 기다려도 0통인지로 본다 — 행마다 다른 사용자(이메일)를 써서 한 번만 기다린다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-027.md
 */
@SpringBootTest(properties = "FRONTEND_BASE_URL=https://app.example/")
@Import({ClockTestConfig.class, CapturingMailConfig.class})
@ExtendWith(OutputCaptureExtension.class)
class Task027AccountMailTest {

  private static final Instant T = Instant.parse("2026-11-20T09:00:00Z");
  private static final Duration QUIET = Duration.ofSeconds(5);
  private static final Pattern TOKEN = Pattern.compile("#token=([A-Za-z0-9_-]+)");

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registry) {
    PostgresTestContainer.register(registry);
  }

  @Autowired private AccountMailService accountMail;
  @Autowired private AccountMails mails;
  @Autowired private EmailTokenService tokens;
  @Autowired private CapturingMailGateway gateway;
  @Autowired private MutableClock clock;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private UserRepository userRepository;
  @Autowired private PlatformTransactionManager transactionManager;

  private Task027Db db;

  @BeforeEach
  void setUp() {
    db = new Task027Db(jdbc, userRepository);
    db.reset();
    gateway.reset();
    clock.setInstant(T);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
    gateway.reset();
  }

  private static void quiet() throws InterruptedException {
    Thread.sleep(QUIET.toMillis());
  }

  // ---------- SC-12 ----------

  private record IntervalRow(
      String row,
      Function<AccountMailService, Function<Long, Boolean>> mail,
      Instant lastSent,
      boolean expected,
      int mails,
      Instant after) {}

  /**
   * SC-12 (boundary) 마지막 발송 60초 안에는 보내지 않고 지나면 보낸다.
   *
   * <p>Given 사용자의 account_mail_sent_at 을 마지막 발송 열로 맞췄다, 시각 T When AccountMailService 로 간격 제한 대상 메일을
   * 보낸다 Then 결과·5초 뒤 메일 수·account_mail_sent_at 이 표대로다
   */
  @Test
  void test_sc12_마지막_발송_60초_안에는_보내지_않고_지나면_보낸다() throws Exception {
    Function<AccountMailService, Function<Long, Boolean>> verify = s -> s::sendVerificationMail;
    Function<AccountMailService, Function<Long, Boolean>> reset = s -> s::sendPasswordResetMail;
    Function<AccountMailService, Function<Long, Boolean>> social = s -> s::sendSocialAccountNotice;
    IntervalRow[] rows = {
      new IntervalRow("a", verify, null, true, 1, T),
      new IntervalRow("b", verify, T.minusSeconds(1), false, 0, T.minusSeconds(1)),
      new IntervalRow("c", verify, T.minusMillis(59_999), false, 0, T.minusMillis(59_999)),
      new IntervalRow("d", verify, T.minusSeconds(60), false, 0, T.minusSeconds(60)),
      new IntervalRow("e", verify, T.minusMillis(60_001), true, 1, T),
      new IntervalRow("f", verify, T.minusSeconds(3600), true, 1, T),
      new IntervalRow("g", verify, T.plusSeconds(1), false, 0, T.plusSeconds(1)),
      new IntervalRow("h", reset, T.minusSeconds(10), false, 0, T.minusSeconds(10)),
      new IntervalRow("i", social, T.minusSeconds(10), false, 0, T.minusSeconds(10)),
      new IntervalRow("j", reset, T.minusSeconds(61), true, 1, T),
    };
    Map<IntervalRow, Long> users = new LinkedHashMap<>();
    Map<IntervalRow, Object> results = new LinkedHashMap<>();
    for (IntervalRow row : rows) {
      // Given
      Long user =
          row.mail() == social
              ? db.socialUser("sc12" + row.row() + "@ticket.on")
              : db.emailUser("sc12" + row.row() + "@ticket.on");
      db.setAccountMailSentAt(user, row.lastSent());
      users.put(row, user);

      // When
      Object result;
      try {
        result = row.mail().apply(accountMail).apply(user);
      } catch (RuntimeException exception) {
        result = exception;
      }
      results.put(row, result);
    }
    quiet();

    // Then
    SoftAssertions soft = new SoftAssertions();
    for (IntervalRow row : rows) {
      String label = "SC-12 " + row.row();
      soft.assertThat(results.get(row)).as(label + " — 결과").isEqualTo(row.expected());
      soft.assertThat(gateway.sentTo("sc12" + row.row() + "@ticket.on"))
          .as(label + " — 메일 수")
          .hasSize(row.mails());
      soft.assertThat(db.accountMailSentAt(users.get(row)))
          .as(label + " — account_mail_sent_at")
          .isEqualTo(row.after());
    }
    soft.assertAll();
  }

  // ---------- SC-13 ----------

  /**
   * SC-13 (boundary) 간격 제한 대상 메일을 동시에 요청해도 한 통만 나간다.
   *
   * <p>Given 사용자 E 의 account_mail_sent_at NULL When 별도 스레드 10개가 각자 트랜잭션에서 동시에 인증 메일을 요청한다 Then true
   * 1·false 9, 5초 뒤 메일 1통, account_mail_sent_at = T
   */
  @Test
  void test_sc13_간격_제한_대상_메일을_동시에_요청해도_한_통만_나간다() throws Exception {
    // Given
    Long e = db.emailUser("e@ticket.on");
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    ExecutorService pool = Executors.newFixedThreadPool(10);
    CountDownLatch go = new CountDownLatch(1);
    List<Future<Boolean>> futures = new ArrayList<>();
    try {
      // When
      for (int i = 0; i < 10; i++) {
        futures.add(
            pool.submit(
                () -> {
                  go.await();
                  return transaction.execute(status -> accountMail.sendVerificationMail(e));
                }));
      }
      go.countDown();
      int trues = 0;
      int falses = 0;
      for (Future<Boolean> future : futures) {
        Boolean result = future.get(30, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(result)) {
          trues++;
        } else {
          falses++;
        }
      }
      quiet();

      // Then
      assertThat(trues).isEqualTo(1);
      assertThat(falses).isEqualTo(9);
      assertThat(gateway.sentTo("e@ticket.on")).hasSize(1);
      assertThat(db.accountMailSentAt(e)).isEqualTo(T);
    } finally {
      pool.shutdownNow();
    }
  }

  // ---------- SC-14 ----------

  /**
   * SC-14 (error) 간격 제한 발송의 대상 사용자가 없으면 아무것도 하지 않는다 · 호출자 인자 오류.
   *
   * <p>Given 사용자 E 만 있다 When 표의 인자로 부른다 Then 결과 열대로, 5초 뒤 메일 0통, email_token 행 수와 E 의
   * account_mail_sent_at 이 호출 전과 같다
   */
  @Test
  void test_sc14_간격_제한_발송의_대상_사용자가_없으면_아무것도_하지_않는다_호출자_인자_오류() throws Exception {
    // Given
    Long e = db.emailUser("e@ticket.on");
    String r = tokens.issue(e, VERIFY_EMAIL);
    int tokensBefore = db.tokenCount();
    Instant sentBefore = db.accountMailSentAt(e);
    SoftAssertions soft = new SoftAssertions();

    // When · Then (행별 결과)
    soft.assertThat(call(() -> accountMail.sendVerificationMail(e + 1000)))
        .as("SC-14 a")
        .isEqualTo(false);
    soft.assertThat(call(() -> accountMail.sendVerificationMail(0L)))
        .as("SC-14 b")
        .isEqualTo(false);
    soft.assertThat(call(() -> accountMail.sendVerificationMail(-1L)))
        .as("SC-14 c")
        .isEqualTo(false);
    soft.assertThat(call(() -> accountMail.sendVerificationMail(null)))
        .as("SC-14 d")
        .isInstanceOf(IllegalArgumentException.class);
    soft.assertThat(call(() -> tokens.issue(null, VERIFY_EMAIL)))
        .as("SC-14 e")
        .isInstanceOf(IllegalArgumentException.class);
    soft.assertThat(call(() -> tokens.issue(0L, VERIFY_EMAIL)))
        .as("SC-14 f(0)")
        .isInstanceOf(IllegalArgumentException.class);
    soft.assertThat(call(() -> tokens.issue(-1L, VERIFY_EMAIL)))
        .as("SC-14 f(-1)")
        .isInstanceOf(IllegalArgumentException.class);
    soft.assertThat(call(() -> tokens.issue(e + 1000, VERIFY_EMAIL)))
        .as("SC-14 g")
        .isInstanceOf(DataIntegrityViolationException.class);
    soft.assertThat(call(() -> tokens.issue(e, null)))
        .as("SC-14 h")
        .isInstanceOf(IllegalArgumentException.class);
    soft.assertThat(call(() -> tokens.consume(r, null)))
        .as("SC-14 i")
        .isInstanceOf(IllegalArgumentException.class);
    soft.assertThat(db.usedAt(r)).as("SC-14 i — used_at").isNull();
    quiet();

    // Then (공통)
    soft.assertThat(gateway.sent()).as("SC-14 — 메일 수").isEmpty();
    soft.assertThat(db.tokenCount()).as("SC-14 — email_token 행 수").isEqualTo(tokensBefore);
    soft.assertThat(db.accountMailSentAt(e))
        .as("SC-14 — E 의 account_mail_sent_at")
        .isEqualTo(sentBefore);
    soft.assertAll();
  }

  private interface Call {
    Object run();
  }

  private static Object call(Call call) {
    try {
      return call.run();
    } catch (Throwable thrown) {
      return thrown;
    }
  }

  // ---------- SC-15 ----------

  /**
   * SC-15 (happy) 메일 네 종류의 제목·본문·링크.
   *
   * <p>Given FRONTEND_BASE_URL=https://app.example/ 로 기동했다, 원문 r When AccountMails 로 표의 메일을
   * e@ticket.on 으로 만든다 Then 받는 사람·제목·본문이 규약대로이고 겹친 / 가 없으며, 변경 알림은 받는 사람 하나로만 만들고 값 칸·http 가 없다
   */
  @Test
  void test_sc15_메일_네_종류의_제목_본문_링크() {
    String to = "e@ticket.on";
    String r = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    String b = "https://app.example";
    SoftAssertions soft = new SoftAssertions();

    // When · Then — a 인증
    MailMessage verification = mails.verification(to, r);
    soft.assertThat(verification.to()).as("a 받는 사람").isEqualTo(to);
    soft.assertThat(verification.subject()).as("a 제목").isEqualTo("이메일 인증을 해 주세요");
    soft.assertThat(verification.body())
        .as("a 본문")
        .contains(b + "/verify-email#token=" + r)
        .contains("24시간")
        .contains("직접 가입하지 않았다면 무시해 주세요")
        .doesNotContain("//verify-email");

    // b 재설정
    MailMessage reset = mails.passwordReset(to, r);
    soft.assertThat(reset.to()).as("b 받는 사람").isEqualTo(to);
    soft.assertThat(reset.subject()).as("b 제목").isEqualTo("비밀번호를 다시 정해 주세요");
    soft.assertThat(reset.body())
        .as("b 본문")
        .contains(b + "/reset-password#token=" + r)
        .contains("30분")
        .contains("요청하지 않았다면 무시해 주세요")
        .doesNotContain("//reset-password");

    // c 변경 알림
    MailMessage changed = mails.passwordChanged(to);
    soft.assertThat(changed.to()).as("c 받는 사람").isEqualTo(to);
    soft.assertThat(changed.subject()).as("c 제목").isEqualTo("비밀번호를 바꿨어요");
    soft.assertThat(changed.body())
        .as("c 본문")
        .contains("비밀번호 찾기")
        .doesNotContain("http")
        .doesNotContain("비밀번호:")
        .doesNotContainIgnoringCase("password");

    // d 소셜 안내
    MailMessage social = mails.socialAccountNotice(to);
    soft.assertThat(social.to()).as("d 받는 사람").isEqualTo(to);
    soft.assertThat(social.subject()).as("d 제목").isEqualTo("소셜 계정으로 로그인해 주세요");
    soft.assertThat(social.body())
        .as("d 본문")
        .contains(b + "/login")
        .doesNotContain("#token=")
        .doesNotContain("//login");
    soft.assertAll();
  }

  // ---------- SC-16 ----------

  /**
   * SC-16 (error) 요청의 Host 를 위조해도 메일 링크는 FRONTEND_BASE_URL 이다.
   *
   * <p>Given 현재 스레드의 요청 컨텍스트에 위조 Host·X-Forwarded-Host·X-Forwarded-Proto·Origin 요청을 넣었다 When 그
   * 스레드에서 E 에게 인증 메일을 보낸다 Then 모인 인증 메일 1통의 링크가 https://app.example/verify-email#token= 로 시작하고
   * evil.example 이 없다
   */
  @Test
  void test_sc16_요청의_Host_를_위조해도_메일_링크는_FRONTEND_BASE_URL_이다() {
    // Given
    Long e = db.emailUser("e@ticket.on");
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/auth/email-verification/resend");
    request.setServerName("evil.example");
    request.addHeader("Host", "evil.example");
    request.addHeader("X-Forwarded-Host", "evil.example");
    request.addHeader("X-Forwarded-Proto", "http");
    request.addHeader("Origin", "https://evil.example");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    // When
    accountMail.sendVerificationMail(e);

    // Then
    Awaitility.await().atMost(QUIET).until(() -> !gateway.sentTo("e@ticket.on").isEmpty());
    List<MailMessage> sent = gateway.sentTo("e@ticket.on");
    assertThat(sent).hasSize(1);
    assertThat(sent.get(0).body()).contains("https://app.example/verify-email#token=");
    assertThat(sent.get(0).body()).doesNotContain("evil.example");
  }

  // ---------- SC-17 ----------

  /**
   * SC-17 (error) 메일은 커밋 뒤 비동기로 나가고 롤백이면 나가지 않으며 발송 실패는 호출자에 영향이 없다.
   *
   * <p>Given 사용자(account_mail_sent_at NULL) When 표의 방식으로 호출한다 Then 메일·뒤 값·호출자 열대로다
   */
  @Test
  void test_sc17_메일은_커밋_뒤_비동기로_나가고_롤백이면_나가지_않으며_발송_실패는_호출자에_영향이_없다(CapturedOutput output)
      throws Exception {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    SoftAssertions soft = new SoftAssertions();

    // a — 바깥 트랜잭션 안에서 발송 → 커밋 전 0통 → 커밋 뒤 1통
    Long a = db.emailUser("sc17a@ticket.on");
    int[] beforeCommit = {-1};
    Object resultA =
        call(
            () ->
                transaction.execute(
                    status -> {
                      boolean sent = accountMail.sendVerificationMail(a);
                      sleep(500);
                      beforeCommit[0] = gateway.sentTo("sc17a@ticket.on").size();
                      return sent;
                    }));
    soft.assertThat(resultA).as("a 호출자").isEqualTo(true);
    soft.assertThat(beforeCommit[0]).as("a 커밋 전").isZero();
    awaitMail(soft, "sc17a@ticket.on", 1, "a 커밋 뒤");
    soft.assertThat(db.accountMailSentAt(a)).as("a 뒤 값").isEqualTo(T);

    // b — 바깥 트랜잭션 롤백
    Long b = db.emailUser("sc17b@ticket.on");
    Object resultB =
        call(
            () ->
                transaction.execute(
                    status -> {
                      boolean sent = accountMail.sendVerificationMail(b);
                      status.setRollbackOnly();
                      return sent;
                    }));
    soft.assertThat(resultB).as("b 호출자").isInstanceOf(Boolean.class);

    // c — 트랜잭션 없이
    Long c = db.emailUser("sc17c@ticket.on");
    soft.assertThat(call(() -> accountMail.sendVerificationMail(c))).as("c 호출자").isEqualTo(true);
    awaitMail(soft, "sc17c@ticket.on", 1, "c");
    soft.assertThat(db.accountMailSentAt(c)).as("c 뒤 값").isEqualTo(T);

    // d — 발송이 예외를 던진다
    Long d = db.emailUser("sc17d@ticket.on");
    gateway.failWith(new MailSendException("SMTP 연결 실패(테스트)"));
    soft.assertThat(call(() -> accountMail.sendVerificationMail(d))).as("d 호출자").isEqualTo(true);
    Awaitility.await()
        .atMost(QUIET)
        .until(() -> gateway.attempts().stream().anyMatch(m -> m.to().equals("sc17d@ticket.on")));
    soft.assertThat(db.accountMailSentAt(d)).as("d 뒤 값").isEqualTo(T);

    // e — d 와 같은 실패, e@ticket.on 에게 — 로그를 본다
    Long e = db.emailUser("e@ticket.on");
    soft.assertThat(call(() -> accountMail.sendVerificationMail(e))).as("e 호출자").isEqualTo(true);
    Awaitility.await()
        .atMost(QUIET)
        .until(() -> gateway.attempts().stream().anyMatch(m -> m.to().equals("e@ticket.on")));
    gateway.failWith(null);
    String rawE =
        gateway.attempts().stream()
            .filter(m -> m.to().equals("e@ticket.on"))
            .map(m -> tokenOf(m.body()))
            .findFirst()
            .orElse("<토큰 없음>");
    soft.assertThat(db.accountMailSentAt(e)).as("e 뒤 값").isEqualTo(T);

    // f — 변경 알림은 간격 없이, account_mail_sent_at 을 바꾸지 않는다
    Long f = db.emailUser("sc17f@ticket.on");
    db.setAccountMailSentAt(f, T.minusSeconds(10));
    soft.assertThat(call(() -> runVoid(() -> accountMail.sendPasswordChangedNotice(f))))
        .as("f 호출자")
        .isNull();
    awaitMail(soft, "sc17f@ticket.on", 1, "f");
    soft.assertThat(db.accountMailSentAt(f)).as("f 뒤 값").isEqualTo(T.minusSeconds(10));

    // g — 변경 알림을 바깥 트랜잭션에서 하고 롤백
    Long g = db.emailUser("sc17g@ticket.on");
    soft.assertThat(
            call(
                () ->
                    transaction.execute(
                        status -> {
                          accountMail.sendPasswordChangedNotice(g);
                          status.setRollbackOnly();
                          return null;
                        })))
        .as("g 호출자")
        .isNull();

    quiet();

    // b·d·g 의 「0통」과 b 의 뒤 값
    soft.assertThat(gateway.sentTo("sc17b@ticket.on")).as("b 메일").isEmpty();
    soft.assertThat(db.accountMailSentAt(b)).as("b 뒤 값").isNull();
    soft.assertThat(gateway.sentTo("sc17d@ticket.on")).as("d 메일(성공)").isEmpty();
    soft.assertThat(
            gateway.attempts().stream().filter(m -> m.to().equals("sc17d@ticket.on")).count())
        .as("d 보내기 시도")
        .isEqualTo(1L);
    soft.assertThat(gateway.sentTo("sc17g@ticket.on")).as("g 메일").isEmpty();
    soft.assertThat(gateway.sentTo("e@ticket.on")).as("e 메일(성공)").isEmpty();

    // e 의 로그
    String out = output.getAll();
    List<String> warnLines =
        out.lines()
            .filter(line -> line.contains("WARN") && line.contains("e***@ticket.on"))
            .toList();
    soft.assertThat(warnLines).as("e — WARN 줄(가린 받는 사람)").hasSize(1);
    soft.assertThat(out).as("e — 원래 주소").doesNotContain("e@ticket.on");
    soft.assertThat(out).as("e — 토큰 원문").doesNotContain(rawE);
    soft.assertThat(out).as("e — 본문 문구").doesNotContain("24시간");
    soft.assertAll();
  }

  private void awaitMail(SoftAssertions soft, String to, int count, String label) {
    try {
      Awaitility.await().atMost(QUIET).until(() -> gateway.sentTo(to).size() >= count);
    } catch (RuntimeException timeout) {
      // 아래 단언이 실제 수를 남긴다
    }
    soft.assertThat(gateway.sentTo(to)).as(label + " 메일").hasSize(count);
  }

  private static String tokenOf(String body) {
    Matcher matcher = TOKEN.matcher(body == null ? "" : body);
    return matcher.find() ? matcher.group(1) : "<토큰 없음>";
  }

  private static Object runVoid(Runnable runnable) {
    runnable.run();
    return null;
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }
}
