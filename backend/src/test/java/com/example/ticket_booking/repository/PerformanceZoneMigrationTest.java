package com.example.ticket_booking.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * TASK-019 시나리오 SC-14 · SC-16 — V6 마이그레이션(포스터·구역 위치)을 운영과 같은 엔진(PostgreSQL)에서 확인한다.
 *
 * <p>나머지 테스트는 H2 + ddl-auto 로 돌아 Flyway 를 거치지 않는다(ADR-0006). 연결은 자동 커밋이라 INSERT 마다 따로 커밋·실패한다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-019.md
 */
@Testcontainers
class PerformanceZoneMigrationTest {

  private static final Instant T = Instant.parse("2026-11-11T08:00:00Z");

  @Container
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  @BeforeEach
  @AfterEach
  void resetSchema() throws SQLException {
    try (Connection connection = connect();
        Statement statement = connection.createStatement()) {
      statement.execute("DROP SCHEMA public CASCADE");
      statement.execute("CREATE SCHEMA public");
    }
  }

  private static Connection connect() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private static Flyway flyway(String target) {
    return Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .target(target == null ? MigrationVersion.LATEST : MigrationVersion.fromVersion(target))
        .load();
  }

  /** V5 스키마에 기존 공연 1행 + 좌석 20행(R 80000 10, 그다음 VIP 120000 10)을 넣고 공연 id 를 돌려준다. */
  private static long seedV5Performance(Connection connection) throws SQLException {
    long performanceId;
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO performance (title, venue, start_at, open_at, close_at, total_seats,"
                + " available_seats, cancelled) VALUES (?, ?, ?, ?, ?, 20, 20, false) RETURNING id")) {
      insert.setString(1, "루미너스 콘서트");
      insert.setString(2, "KSPO DOME");
      insert.setTimestamp(3, Timestamp.from(T.plus(Duration.ofDays(30))));
      insert.setTimestamp(4, Timestamp.from(T.plus(Duration.ofDays(7))));
      insert.setTimestamp(5, Timestamp.from(T.plus(Duration.ofDays(29))));
      try (ResultSet rows = insert.executeQuery()) {
        rows.next();
        performanceId = rows.getLong(1);
      }
    }
    try (PreparedStatement seat =
        connection.prepareStatement(
            "INSERT INTO seat (performance_id, grade, seat_row, seat_number, seat_label, price)"
                + " VALUES (?, ?, ?, ?, ?, ?)")) {
      for (String[] grade : new String[][] {{"R", "A", "80000"}, {"VIP", "B", "120000"}}) {
        for (int number = 1; number <= 10; number++) {
          seat.setLong(1, performanceId);
          seat.setString(2, grade[0]);
          seat.setString(3, grade[1]);
          seat.setInt(4, number);
          seat.setString(5, grade[0] + "-" + grade[1] + number);
          seat.setInt(6, Integer.parseInt(grade[2]));
          seat.executeUpdate();
        }
      }
    }
    return performanceId;
  }

  private static long count(Connection connection, String table) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet rows = statement.executeQuery("SELECT count(*) FROM " + table)) {
      rows.next();
      return rows.getLong(1);
    }
  }

  // ---------- SC-14 ----------

  /**
   * SC-14 (regression)
   *
   * <p>Given V5 까지 적용, 기존 공연 1행 + 좌석 20행 When V6 까지 적용한다 Then 성공·현재 버전 6, poster_url NULL·다른 열 그대로,
   * performance_zone 0행
   */
  @Test
  void test_sc14_V6_는_기존_데이터가_있는_PostgreSQL_에_적용되고_기존_행이_그대로다() throws SQLException {
    // Given
    flyway("5").migrate();
    long performanceId;
    try (Connection connection = connect()) {
      performanceId = seedV5Performance(connection);
    }

    // When — V6 로 대상을 고정한다(TASK-022 에서 V7 이 더해져 "끝까지" 는 V6 가 아니다)
    flyway("6").migrate();

    // Then
    Flyway latest = flyway("6");
    assertThat(latest.info().current().getVersion().getVersion()).isEqualTo("6");
    try (Connection connection = connect();
        PreparedStatement select =
            connection.prepareStatement(
                "SELECT poster_url, title, venue, start_at, open_at, close_at, total_seats,"
                    + " available_seats, cancelled FROM performance WHERE id = ?")) {
      select.setLong(1, performanceId);
      try (ResultSet row = select.executeQuery()) {
        assertThat(row.next()).isTrue();
        assertThat(row.getString("poster_url")).isNull();
        assertThat(row.getString("title")).isEqualTo("루미너스 콘서트");
        assertThat(row.getString("venue")).isEqualTo("KSPO DOME");
        assertThat(row.getTimestamp("start_at").toInstant()).isEqualTo(T.plus(Duration.ofDays(30)));
        assertThat(row.getTimestamp("open_at").toInstant()).isEqualTo(T.plus(Duration.ofDays(7)));
        assertThat(row.getTimestamp("close_at").toInstant()).isEqualTo(T.plus(Duration.ofDays(29)));
        assertThat(row.getInt("total_seats")).isEqualTo(20);
        assertThat(row.getInt("available_seats")).isEqualTo(20);
        assertThat(row.getBoolean("cancelled")).isFalse();
      }
      assertThat(count(connection, "seat")).isEqualTo(20);
      assertThat(count(connection, "performance_zone")).isZero();
    }
  }

  // ---------- SC-16 ----------

  /**
   * SC-16 (regression)
   *
   * <p>Given V6 까지 적용, 그 공연에 (R, R, 1) 한 행 When 표의 행을 넣는다 Then 각각 제약 위반으로 실패하고 performance_zone 은
   * 1행 그대로
   */
  @Test
  void test_sc16_V6_가_만든_구역_테이블은_잘못된_행을_받지_않는다() throws SQLException {
    // Given
    flyway("5").migrate();
    long performanceId;
    try (Connection connection = connect()) {
      performanceId = seedV5Performance(connection);
    }
    flyway(null).migrate();
    String insertZone =
        "INSERT INTO performance_zone (performance_id, grade, area, area_note, stage_order)"
            + " VALUES (%s, %s, %s, %s, %s)";
    try (Connection connection = connect();
        Statement statement = connection.createStatement()) {
      statement.executeUpdate(String.format(insertZone, performanceId, "'R'", "'R'", "NULL", 1));
    }

    Map<String, String> rows = new LinkedHashMap<>();
    rows.put("a UNIQUE grade", String.format(insertZone, performanceId, "'R'", "'x'", "NULL", 2));
    rows.put(
        "b UNIQUE stage_order",
        String.format(insertZone, performanceId, "'VIP'", "'x'", "NULL", 1));
    rows.put(
        "c CHECK stage_order", String.format(insertZone, performanceId, "'VIP'", "'x'", "NULL", 0));
    rows.put(
        "d area NOT NULL", String.format(insertZone, performanceId, "'VIP'", "NULL", "NULL", 2));
    rows.put("e FK", String.format(insertZone, 999, "'VIP'", "'x'", "NULL", 1));
    rows.put(
        "f area 31자",
        String.format(insertZone, performanceId, "'VIP'", "'" + "가".repeat(31) + "'", "NULL", 2));
    rows.put(
        "g area_note 61자",
        String.format(insertZone, performanceId, "'VIP'", "'x'", "'" + "나".repeat(61) + "'", 2));
    rows.put(
        "h poster_url 501자",
        "UPDATE performance SET poster_url = '"
            + "a".repeat(501)
            + "' WHERE id = "
            + performanceId);

    // When · Then
    SoftAssertions softly = new SoftAssertions();
    try (Connection connection = connect()) {
      for (Map.Entry<String, String> row : rows.entrySet()) {
        try (Statement statement = connection.createStatement()) {
          softly
              .assertThatThrownBy(() -> statement.executeUpdate(row.getValue()))
              .as(row.getKey())
              .isInstanceOf(SQLException.class);
        }
      }
      softly.assertThat(count(connection, "performance_zone")).as("구역 행").isEqualTo(1);
    }
    softly.assertAll();
  }
}
