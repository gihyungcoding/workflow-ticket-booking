package com.example.ticket_booking.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * TASK-023 시나리오 SC-01 — V8 마이그레이션(공연 공개 여부)을 운영과 같은 엔진(PostgreSQL)에서 확인한다.
 *
 * <p>나머지 테스트는 H2 + ddl-auto 로 돌아 Flyway 를 거치지 않는다(ADR-0006). NOT NULL·컬럼 기본값·기존 행 채우기는 여기서만 확인된다.
 * 연결은 자동 커밋이다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-023.md
 */
@Testcontainers
class PerformancePublishedMigrationTest {

  private static final Instant T = Instant.parse("2026-11-13T09:00:00Z");

  private static final String[] KEPT_COLUMNS = {
    "title",
    "venue",
    "start_at",
    "open_at",
    "close_at",
    "total_seats",
    "available_seats",
    "cancelled",
    "poster_url",
    "owner_id"
  };

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

  /**
   * V6 스키마에 기존 공연 1행 + 좌석 20행을 넣고 공연 id 를 돌려준다(PerformanceZoneMigrationTest 의 seedV5Performance
   * 관례).
   */
  private static long seedPerformance(Connection connection) throws SQLException {
    long performanceId;
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO performance (title, venue, start_at, open_at, close_at, total_seats,"
                + " available_seats, cancelled, poster_url) VALUES (?, ?, ?, ?, ?, 20, 20, false, ?)"
                + " RETURNING id")) {
      insert.setString(1, "소리풍경 밴드 나이트");
      insert.setString(2, "롤링홀");
      insert.setTimestamp(3, Timestamp.from(T.plus(Duration.ofDays(30))));
      insert.setTimestamp(4, Timestamp.from(T.plus(Duration.ofDays(7))));
      insert.setTimestamp(5, Timestamp.from(T.plus(Duration.ofDays(29))));
      insert.setString(6, "https://example.com/poster.jpg");
      try (ResultSet rows = insert.executeQuery()) {
        rows.next();
        performanceId = rows.getLong(1);
      }
    }
    try (PreparedStatement seat =
        connection.prepareStatement(
            "INSERT INTO seat (performance_id, grade, seat_row, seat_number, seat_label, price)"
                + " VALUES (?, ?, ?, ?, ?, ?)")) {
      for (String row : new String[] {"A", "B"}) {
        for (int number = 1; number <= 10; number++) {
          seat.setLong(1, performanceId);
          seat.setString(2, "R");
          seat.setString(3, row);
          seat.setInt(4, number);
          seat.setString(5, "R-" + row + number);
          seat.setInt(6, 55000);
          seat.executeUpdate();
        }
      }
    }
    return performanceId;
  }

  private static Map<String, Object> keptColumns(Connection connection, long performanceId)
      throws SQLException {
    try (PreparedStatement select =
        connection.prepareStatement(
            "SELECT " + String.join(", ", KEPT_COLUMNS) + " FROM performance WHERE id = ?")) {
      select.setLong(1, performanceId);
      try (ResultSet rows = select.executeQuery()) {
        rows.next();
        Map<String, Object> values = new LinkedHashMap<>();
        for (String column : KEPT_COLUMNS) {
          values.put(column, rows.getObject(column));
        }
        return values;
      }
    }
  }

  private static Object published(Connection connection, long performanceId) throws SQLException {
    try (PreparedStatement select =
        connection.prepareStatement("SELECT published FROM performance WHERE id = ?")) {
      select.setLong(1, performanceId);
      try (ResultSet rows = select.executeQuery()) {
        rows.next();
        return rows.getObject("published");
      }
    }
  }

  /**
   * SC-01 (happy)
   *
   * <p>Given PostgreSQL 에 V7 까지 적용, 기존 공연 1행 + 좌석 20행, 열 값을 읽어 두었다 When Flyway 를 끝까지 적용한다 Then
   * 성공·현재 버전 8, 기존 공연 published TRUE, 다른 열은 읽어 둔 값과 같다, published 를 안 주고 INSERT 하면 FALSE, NULL 을
   * 넣으면 NOT NULL 위반
   */
  @Test
  void test_sc01_V8_은_기존_공연이_있는_PostgreSQL_에_적용되고_기존_행을_공개로_둔다() throws SQLException {
    // Given
    flyway("7").migrate();
    long performanceId;
    Map<String, Object> before;
    try (Connection connection = connect()) {
      performanceId = seedPerformance(connection);
      before = keptColumns(connection, performanceId);
    }

    // When
    flyway(null).migrate();

    // Then 적용에 성공하고 현재 버전은 8 이다
    assertThat(flyway(null).info().current().getVersion().getVersion()).isEqualTo("8");
    try (Connection connection = connect()) {
      // And 기존 공연의 published 는 TRUE 다
      assertThat(published(connection, performanceId)).isEqualTo(true);
      // And 다른 열은 읽어 둔 값과 같다
      assertThat(keptColumns(connection, performanceId)).isEqualTo(before);
      // And published 를 지정하지 않고 새 공연 행을 INSERT 하면 그 행의 published 는 FALSE 다
      long inserted;
      try (PreparedStatement insert =
          connection.prepareStatement(
              "INSERT INTO performance (title, venue, start_at, open_at, close_at, total_seats,"
                  + " available_seats, cancelled) VALUES ('새 공연', '롤링홀', ?, ?, ?, 20, 20, false)"
                  + " RETURNING id")) {
        insert.setTimestamp(1, Timestamp.from(T.plus(Duration.ofDays(30))));
        insert.setTimestamp(2, Timestamp.from(T.plus(Duration.ofDays(7))));
        insert.setTimestamp(3, Timestamp.from(T.plus(Duration.ofDays(29))));
        try (ResultSet rows = insert.executeQuery()) {
          rows.next();
          inserted = rows.getLong(1);
        }
      }
      assertThat(published(connection, inserted)).isEqualTo(false);
      // And published 에 NULL 을 넣으면 NOT NULL 위반으로 실패한다
      assertThatThrownBy(
              () -> {
                try (Statement statement = connection.createStatement()) {
                  statement.executeUpdate(
                      "UPDATE performance SET published = NULL WHERE id = " + performanceId);
                }
              })
          .isInstanceOf(SQLException.class)
          .satisfies(e -> assertThat(((SQLException) e).getSQLState()).isEqualTo("23502"));
    }
  }
}
