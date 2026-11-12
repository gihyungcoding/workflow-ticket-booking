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
 * TASK-022 시나리오 SC-01 — V7 마이그레이션(공연 소유자)을 운영과 같은 엔진(PostgreSQL)에서 확인한다.
 *
 * <p>나머지 테스트는 H2 + ddl-auto 로 돌아 Flyway 를 거치지 않는다(ADR-0006). 외래 키 동작(존재하지 않는 사용자 거부, 사용자 삭제 시
 * NULL)은 여기서만 확인된다. 연결은 자동 커밋이다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-022.md
 */
@Testcontainers
class PerformanceOwnerMigrationTest {

  private static final Instant T = Instant.parse("2026-11-11T08:00:00Z");
  private static final String[] KEPT_COLUMNS = {
    "title",
    "venue",
    "start_at",
    "open_at",
    "close_at",
    "total_seats",
    "available_seats",
    "cancelled",
    "poster_url"
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

  private static long seedUser(Connection connection) throws SQLException {
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO users (email, password_hash, name, role, handle)"
                + " VALUES ('organizer@example.com', 'x', '한도윤', 'ORGANIZER', 'wave0001')"
                + " RETURNING id")) {
      try (ResultSet rows = insert.executeQuery()) {
        rows.next();
        return rows.getLong(1);
      }
    }
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

  private static Object ownerId(Connection connection, long performanceId) throws SQLException {
    try (PreparedStatement select =
        connection.prepareStatement("SELECT owner_id FROM performance WHERE id = ?")) {
      select.setLong(1, performanceId);
      try (ResultSet rows = select.executeQuery()) {
        rows.next();
        return rows.getObject("owner_id");
      }
    }
  }

  private static void setOwner(Connection connection, long performanceId, long userId)
      throws SQLException {
    try (PreparedStatement update =
        connection.prepareStatement("UPDATE performance SET owner_id = ? WHERE id = ?")) {
      update.setLong(1, userId);
      update.setLong(2, performanceId);
      update.executeUpdate();
    }
  }

  /**
   * SC-01 (regression)
   *
   * <p>Given PostgreSQL 에 V6 까지 적용, 기존 공연 1행 + 좌석 20행, 사용자 1행, 기존 공연 행의 열 값을 읽어 두었다 When Flyway 를
   * 끝까지 적용한다 Then 성공·현재 버전 7, owner_id NULL, 다른 열은 읽어 둔 값과 같다, 없는 사용자 id 로 갱신하면 외래 키 위반, 있는 사용자로
   * 갱신한 뒤 그 사용자를 지우면 owner_id 가 NULL(공연 행은 남는다)
   */
  @Test
  void test_sc01_V7_은_기존_공연이_있는_PostgreSQL_에_적용되고_소유자를_비워_둔다() throws SQLException {
    // Given
    flyway("6").migrate();
    long performanceId;
    long userId;
    Map<String, Object> before;
    try (Connection connection = connect()) {
      performanceId = seedPerformance(connection);
      userId = seedUser(connection);
      before = keptColumns(connection, performanceId);
    }

    // When
    flyway(null).migrate();

    // Then 적용에 성공하고 현재 버전은 7 이다
    assertThat(flyway(null).info().current().getVersion().getVersion()).isEqualTo("7");
    try (Connection connection = connect()) {
      // And 기존 공연의 owner_id 는 NULL 이다
      assertThat(ownerId(connection, performanceId)).isNull();
      // And 다른 열은 읽어 둔 값과 같다
      assertThat(keptColumns(connection, performanceId)).isEqualTo(before);
      // And 없는 사용자 id 로 owner_id 를 갱신하면 외래 키 위반으로 실패한다
      assertThatThrownBy(() -> setOwner(connection, performanceId, userId + 1000))
          .isInstanceOf(SQLException.class)
          .satisfies(e -> assertThat(((SQLException) e).getSQLState()).isEqualTo("23503"));
      // And 있는 사용자 id 로 갱신한 뒤 그 사용자 행을 지우면 owner_id 가 NULL 이 된다(공연 행은 남는다)
      setOwner(connection, performanceId, userId);
      assertThat(ownerId(connection, performanceId)).isEqualTo(userId);
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate("DELETE FROM users WHERE id = " + userId);
      }
      assertThat(ownerId(connection, performanceId)).isNull();
      assertThat(keptColumns(connection, performanceId)).isEqualTo(before);
    }
  }
}
