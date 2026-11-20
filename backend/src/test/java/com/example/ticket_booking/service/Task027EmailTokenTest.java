package com.example.ticket_booking.service;

import static com.example.ticket_booking.domain.EmailTokenPurpose.RESET_PASSWORD;
import static com.example.ticket_booking.domain.EmailTokenPurpose.VERIFY_EMAIL;
import static com.example.ticket_booking.support.Task027Db.sha256Hex;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticket_booking.domain.EmailTokenPurpose;
import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.CapturingMailConfig;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import com.example.ticket_booking.support.PostgresTestContainer;
import com.example.ticket_booking.support.Task027Db;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * TASK-027 SC-06~SC-11 — 일회용 이메일 토큰의 발급·소모. PostgreSQL(Flyway 스키마) 위에서 돌고, 클래스 레벨
 * {@code @Transactional} 없이 JDBC 로 새로 읽어 확인한다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-027.md
 */
@SpringBootTest
@Import({ClockTestConfig.class, CapturingMailConfig.class})
class Task027EmailTokenTest {

  private static final Instant T = Instant.parse("2026-11-20T09:00:00Z");
  private static final String RAW_PATTERN = "[A-Za-z0-9_-]{43}";

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registry) {
    PostgresTestContainer.register(registry);
  }

  @Autowired private EmailTokenService tokens;
  @Autowired private MutableClock clock;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private UserRepository userRepository;
  @Autowired private PlatformTransactionManager transactionManager;

  private Task027Db db;

  @BeforeEach
  void setUp() {
    db = new Task027Db(jdbc, userRepository);
    db.reset();
    clock.setInstant(T);
  }

  // ---------- SC-06 ----------

  /**
   * SC-06 (happy) 발급한 토큰의 원문은 43자이고 DB 에는 해시만 남는다.
   *
   * <p>Given 사용자 E, 시각 T When issue(E, VERIFY_EMAIL) 를 두 번 부른다(r1·r2) Then 둘 다 43자·문자 집합, r1 ≠ r2,
   * E·VERIFY 행 1개이고 token_hash = sha256hex(r2), 어느 행·열에도 원문이 없다, expires_at = T+24h, used_at NULL
   */
  @Test
  void test_sc06_발급한_토큰의_원문은_43자이고_DB_에는_해시만_남는다() {
    // Given
    Long e = db.emailUser("e@ticket.on");

    // When
    String r1 = tokens.issue(e, VERIFY_EMAIL);
    String r2 = tokens.issue(e, VERIFY_EMAIL);

    // Then
    assertThat(r1).matches(RAW_PATTERN);
    assertThat(r2).matches(RAW_PATTERN);
    assertThat(r1).isNotEqualTo(r2);
    List<Map<String, Object>> rows = db.tokenRows(e, "VERIFY_EMAIL");
    assertThat(rows).hasSize(1);
    assertThat(String.valueOf(rows.get(0).get("token_hash")).trim()).isEqualTo(sha256Hex(r2));
    for (Map<String, Object> row : db.allTokenRows()) {
      for (Map.Entry<String, Object> column : row.entrySet()) {
        assertThat(String.valueOf(column.getValue()))
            .as("email_token." + column.getKey())
            .doesNotContain(r1)
            .doesNotContain(r2);
      }
    }
    assertThat(((Timestamp) rows.get(0).get("expires_at")).toInstant())
        .isEqualTo(T.plus(Duration.ofHours(24)));
    assertThat(rows.get(0).get("used_at")).isNull();
  }

  // ---------- SC-07 ----------

  private record ExpiryRow(
      String row, EmailTokenPurpose purpose, Duration stored, Duration at, boolean success) {}

  /**
   * SC-07 (boundary) 만료는 인증 24시간·재설정 30분이고 정각에는 이미 만료다.
   *
   * <p>Given 시각 T 에 표의 용도로 토큰을 발급했다 When 시각을 소모 시각으로 옮겨 consume 한다 Then 성공이면 E.id·used_at = 소모 시각,
   * 만료면 빈 결과·used_at NULL
   */
  @Test
  void test_sc07_만료는_인증_24시간_재설정_30분이고_정각에는_이미_만료다() {
    Duration day = Duration.ofHours(24);
    Duration half = Duration.ofMinutes(30);
    Duration ms = Duration.ofMillis(1);
    ExpiryRow[] rows = {
      new ExpiryRow("a", VERIFY_EMAIL, day, day.minus(ms), true),
      new ExpiryRow("b", VERIFY_EMAIL, day, day, false),
      new ExpiryRow("c", VERIFY_EMAIL, day, day.plus(ms), false),
      new ExpiryRow("d", RESET_PASSWORD, half, half.minus(ms), true),
      new ExpiryRow("e", RESET_PASSWORD, half, half, false),
      new ExpiryRow("f", RESET_PASSWORD, half, half.plus(ms), false),
    };
    Long e = db.emailUser("e@ticket.on");
    SoftAssertions soft = new SoftAssertions();
    for (ExpiryRow row : rows) {
      String label = "SC-07 " + row.row();
      // Given
      clock.setInstant(T);
      String raw = tokens.issue(e, row.purpose());
      List<Map<String, Object>> stored = db.tokenRows(e, row.purpose().name());
      soft.assertThat(((Timestamp) stored.get(0).get("expires_at")).toInstant())
          .as(label + " — 저장된 expires_at")
          .isEqualTo(T.plus(row.stored()));

      // When
      Instant at = T.plus(row.at());
      clock.setInstant(at);
      Optional<Long> result = tokens.consume(raw, row.purpose());

      // Then
      if (row.success()) {
        soft.assertThat(result).as(label).contains(e);
        soft.assertThat(db.usedAt(raw)).as(label + " — used_at").isEqualTo(at);
      } else {
        soft.assertThat(result).as(label).isEmpty();
        soft.assertThat(db.usedAt(raw)).as(label + " — used_at").isNull();
      }
    }
    soft.assertAll();
  }

  // ---------- SC-08 ----------

  private interface Variant {
    List<String> of(String raw);
  }

  private record InvalidRow(
      String row, Variant inputs, EmailTokenPurpose purpose, boolean consumeFirst) {}

  /**
   * SC-08 (error) 무효한 토큰은 빈 결과이고 DB 가 바뀌지 않는다.
   *
   * <p>Given 시각 T 에 VERIFY_EMAIL 토큰 r 을 발급했다(행마다 새로), 준비 열이 있으면 먼저 한다 When 시각 T+1m 에 입력·용도로 consume
   * 한다 Then 예외 없이 빈 결과이고 r 행의 used_at 이 준비 뒤 값 그대로다
   */
  @Test
  void test_sc08_무효한_토큰은_빈_결과이고_DB_가_바뀌지_않는다() {
    InvalidRow[] rows = {
      new InvalidRow("a", raw -> listOfNull(), VERIFY_EMAIL, false),
      new InvalidRow("b", raw -> List.of(""), VERIFY_EMAIL, false),
      new InvalidRow("c", raw -> List.of("   "), VERIFY_EMAIL, false),
      new InvalidRow("d", raw -> List.of("　", " ", "​", " "), VERIFY_EMAIL, false),
      new InvalidRow("e", raw -> List.of(raw.substring(0, 42)), VERIFY_EMAIL, false),
      new InvalidRow("f", raw -> List.of(raw.substring(0, 20)), VERIFY_EMAIL, false),
      new InvalidRow("g", raw -> List.of(raw + "x"), VERIFY_EMAIL, false),
      new InvalidRow("h", raw -> List.of(changeLast(raw)), VERIFY_EMAIL, false),
      new InvalidRow("i", raw -> upperIfDifferent(raw), VERIFY_EMAIL, false),
      new InvalidRow(
          "j",
          raw -> List.of(" " + raw, raw + " ", raw + "\n", raw + "=", "=" + raw, "~" + raw),
          VERIFY_EMAIL,
          false),
      new InvalidRow(
          "k",
          raw -> List.of(raw.substring(0, 21) + "\u0000" + raw.substring(21), raw + "\u0000"),
          VERIFY_EMAIL,
          false),
      new InvalidRow("l", raw -> List.of(raw + "\uD800", raw + "é"), VERIFY_EMAIL, false),
      new InvalidRow("m", raw -> List.of(changeLast(raw.substring(1) + "Q")), VERIFY_EMAIL, false),
      new InvalidRow("n", List::of, RESET_PASSWORD, false),
      new InvalidRow("o", List::of, VERIFY_EMAIL, true),
      new InvalidRow("p", raw -> List.of("a".repeat(10_000)), VERIFY_EMAIL, false),
      new InvalidRow("q", Task027EmailTokenTest::normalizationVariants, VERIFY_EMAIL, false),
    };
    Long e = db.emailUser("e@ticket.on");
    SoftAssertions soft = new SoftAssertions();
    for (InvalidRow row : rows) {
      // Given
      clock.setInstant(T);
      String raw = tokens.issue(e, VERIFY_EMAIL);
      Instant afterPrepare = null;
      clock.setInstant(T.plus(Duration.ofMinutes(1)));
      if (row.consumeFirst()) {
        tokens.consume(raw, VERIFY_EMAIL);
        afterPrepare = T.plus(Duration.ofMinutes(1));
        soft.assertThat(db.usedAt(raw)).as("SC-08 o — 준비(첫 소모)").isEqualTo(afterPrepare);
      }
      for (String input : row.inputs().of(raw)) {
        String label = "SC-08 " + row.row() + " 입력=" + printable(input);
        // When
        Optional<Long> result = null;
        Throwable thrown = null;
        try {
          result = tokens.consume(input, row.purpose());
        } catch (Throwable throwable) {
          thrown = throwable;
        }

        // Then
        soft.assertThat(thrown).as(label + " — 예외 없음").isNull();
        soft.assertThat(result).as(label).isEqualTo(Optional.empty());
        soft.assertThat(db.usedAt(raw)).as(label + " — used_at").isEqualTo(afterPrepare);
      }
    }
    soft.assertAll();
  }

  private static List<String> listOfNull() {
    List<String> values = new ArrayList<>();
    values.add(null);
    return values;
  }

  private static String changeLast(String raw) {
    char last = raw.charAt(raw.length() - 1);
    return raw.substring(0, raw.length() - 1) + (last == 'A' ? 'B' : 'A');
  }

  private static List<String> upperIfDifferent(String raw) {
    String upper = raw.toUpperCase();
    // 원문에 소문자가 하나도 없으면(확률 극히 낮음) 소문자로 바꿔 '원문과 다른 대소문자 변형'을 만든다
    return List.of(upper.equals(raw) ? raw.toLowerCase() : upper);
  }

  /** 전각·결합 문자·비슷해 보이는 기호 — 어느 것도 원문으로 정규화해 받아들이지 않는다. */
  private static List<String> normalizationVariants(String raw) {
    List<String> values = new ArrayList<>();
    values.add(raw.substring(0, raw.length() - 1) + fullWidth(raw.charAt(raw.length() - 1)));
    values.add(raw + "́");
    values.add(fullWidth('A') + raw.substring(1));
    values.add(fullWidth('0') + raw.substring(1));
    values.add(
        raw.replace('-', '‐').replace('_', '＿')
            + (raw.contains("-") || raw.contains("_") ? "" : "‐"));
    return values;
  }

  private static String fullWidth(char c) {
    return (c >= '!' && c <= '~') ? String.valueOf((char) (c - '!' + '！')) : c + "́";
  }

  private static String printable(String value) {
    if (value == null) {
      return "null";
    }
    StringBuilder out = new StringBuilder("\"");
    String shown =
        value.length() > 60 ? value.substring(0, 60) + "…(" + value.length() + "자)" : value;
    for (char c : shown.toCharArray()) {
      out.append(c < 0x20 || c > 0x7e ? String.format("\\u%04X", (int) c) : String.valueOf(c));
    }
    return out.append('"').toString();
  }

  // ---------- SC-09 ----------

  /**
   * SC-09 (happy) 유효한 토큰은 한 번 소모되고 두 번째는 빈 결과다.
   *
   * <p>Given 시각 T 에 RESET_PASSWORD 토큰 r When 시각 T+10m 에 consume 을 두 번 부른다 Then 첫 호출 E.id, used_at =
   * T+10m, 두 번째 빈 결과·used_at 그대로
   */
  @Test
  void test_sc09_유효한_토큰은_한_번_소모되고_두_번째는_빈_결과다() {
    // Given
    Long e = db.emailUser("e@ticket.on");
    String raw = tokens.issue(e, RESET_PASSWORD);
    Instant at = T.plus(Duration.ofMinutes(10));
    clock.setInstant(at);

    // When
    Optional<Long> first = tokens.consume(raw, RESET_PASSWORD);
    Instant usedAfterFirst = db.usedAt(raw);
    Optional<Long> second = tokens.consume(raw, RESET_PASSWORD);

    // Then
    assertThat(first).contains(e);
    assertThat(usedAfterFirst).isEqualTo(at);
    assertThat(second).isEmpty();
    assertThat(db.usedAt(raw)).isEqualTo(at);
  }

  // ---------- SC-10 ----------

  /**
   * SC-10 (boundary) 같은 토큰을 동시에 두 번 소모하면 한 쪽만 성공한다.
   *
   * <p>Given VERIFY_EMAIL 토큰 r When 별도 스레드 두 개가 각자 트랜잭션에서 동시에 consume 한다 — 20회(회마다 새 토큰) Then 매 회
   * 성공 1·빈 결과 1, used_at 이 채워져 있다
   */
  @Test
  void test_sc10_같은_토큰을_동시에_두_번_소모하면_한_쪽만_성공한다() throws Exception {
    Long e = db.emailUser("e@ticket.on");
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    SoftAssertions soft = new SoftAssertions();
    try {
      for (int round = 1; round <= 20; round++) {
        // Given
        String raw = tokens.issue(e, VERIFY_EMAIL);
        CountDownLatch go = new CountDownLatch(1);

        // When
        List<Future<Optional<Long>>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
          futures.add(
              pool.submit(
                  () -> {
                    go.await();
                    return transaction.execute(status -> tokens.consume(raw, VERIFY_EMAIL));
                  }));
        }
        go.countDown();
        int success = 0;
        int empty = 0;
        for (Future<Optional<Long>> future : futures) {
          Optional<Long> result = future.get(30, TimeUnit.SECONDS);
          if (result != null && result.isPresent()) {
            success++;
          } else {
            empty++;
          }
        }

        // Then
        soft.assertThat(success).as("SC-10 " + round + "회 — 성공 수").isEqualTo(1);
        soft.assertThat(empty).as("SC-10 " + round + "회 — 빈 결과 수").isEqualTo(1);
        soft.assertThat(db.usedAt(raw)).as("SC-10 " + round + "회 — used_at").isEqualTo(T);
      }
    } finally {
      pool.shutdownNow();
    }
    soft.assertAll();
  }

  // ---------- SC-11 ----------

  /**
   * SC-11 (happy) 같은 사용자·용도로 새로 발급하면 이전 토큰은 사라진다.
   *
   * <p>Given E 의 VERIFY a·RESET p, S 의 VERIFY s When E 에게 VERIFY b 를 새로 발급한다 Then a 는 빈 결과·행 없음,
   * E·VERIFY 는 b 1개, p·s 는 그대로 소모되고, b 는 E.id
   */
  @Test
  void test_sc11_같은_사용자_용도로_새로_발급하면_이전_토큰은_사라진다() {
    // Given
    Long e = db.emailUser("e@ticket.on");
    Long s = db.socialUser("s@ticket.on");
    String a = tokens.issue(e, VERIFY_EMAIL);
    String p = tokens.issue(e, RESET_PASSWORD);
    String sToken = tokens.issue(s, VERIFY_EMAIL);

    // When
    String b = tokens.issue(e, VERIFY_EMAIL);

    // Then
    assertThat(db.countByHash(a)).isZero();
    List<Map<String, Object>> verifyRows = db.tokenRows(e, "VERIFY_EMAIL");
    assertThat(verifyRows).hasSize(1);
    assertThat(String.valueOf(verifyRows.get(0).get("token_hash")).trim()).isEqualTo(sha256Hex(b));
    assertThat(tokens.consume(a, VERIFY_EMAIL)).isEmpty();
    assertThat(tokens.consume(p, RESET_PASSWORD)).contains(e);
    assertThat(tokens.consume(sToken, VERIFY_EMAIL)).contains(s);
    assertThat(tokens.consume(b, VERIFY_EMAIL)).contains(e);
  }
}
