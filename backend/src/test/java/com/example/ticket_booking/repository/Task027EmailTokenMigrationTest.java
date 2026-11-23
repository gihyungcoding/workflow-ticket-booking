package com.example.ticket_booking.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
 * TASK-027 SC-18 — V9 마이그레이션(email_token·users.account_mail_sent_at)을 운영과 같은 엔진(PostgreSQL)에서 확인한다.
 * 연결은 자동 커밋이다(PerformancePublishedMigrationTest 관례).
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-027.md
 */
@Testcontainers
class Task027EmailTokenMigrationTest {

  private static final String HASH_A = "a".repeat(64);

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

  private static long insertUser(
      Connection connection, String email, String passwordHash, boolean verified, String handle)
      throws SQLException {
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO users (email, password_hash, name, role, email_verified, handle)"
                + " VALUES (?, ?, ?, 'AUDIENCE', ?, ?) RETURNING id")) {
      insert.setString(1, email);
      insert.setString(2, passwordHash);
      insert.setString(3, "기존사용자");
      insert.setBoolean(4, verified);
      insert.setString(5, handle);
      try (ResultSet result = insert.executeQuery()) {
        result.next();
        return result.getLong(1);
      }
    }
  }

  /** 사용자 행을 열 이름 → 값 으로 읽는다(새 열 account_mail_sent_at 은 뺀다 — 기존 열 비교용). */
  private static Map<String, Object> readUser(Connection connection, long id) throws SQLException {
    try (PreparedStatement select =
        connection.prepareStatement("SELECT * FROM users WHERE id = ?")) {
      select.setLong(1, id);
      try (ResultSet result = select.executeQuery()) {
        result.next();
        ResultSetMetaData meta = result.getMetaData();
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
          String name = meta.getColumnName(i);
          if (!name.equals("account_mail_sent_at")) {
            row.put(name, result.getObject(i));
          }
        }
        return row;
      }
    }
  }

  private static List<String> columns(Connection connection, String table) throws SQLException {
    List<String> names = new ArrayList<>();
    try (PreparedStatement select =
        connection.prepareStatement(
            "SELECT column_name FROM information_schema.columns WHERE table_name = ? ORDER BY ordinal_position")) {
      select.setString(1, table);
      try (ResultSet result = select.executeQuery()) {
        while (result.next()) {
          names.add(result.getString(1));
        }
      }
    }
    return names;
  }

  private static void insertToken(Connection connection, long userId, String hash)
      throws SQLException {
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO email_token (user_id, purpose, token_hash, expires_at)"
                + " VALUES (?, 'VERIFY_EMAIL', ?, now() + interval '1 day')")) {
      insert.setLong(1, userId);
      insert.setString(2, hash);
      insert.executeUpdate();
    }
  }

  /**
   * SC-18 (happy) V9 는 기존 사용자 행을 그대로 두고 적용된다.
   *
   * <p>Given V8 까지 적용, 이메일 가입 행(false)·소셜 가입 행(true)+소셜 연결 1행, 모든 열 값을 읽어 뒀다 When 끝까지 적용한다 Then 버전
   * 9, 기존 열 값 같음·account_mail_sent_at NULL, email_token 열 7개, token_hash 고유, 없는 user_id 는 FK 위반,
   * 사용자 삭제 시 토큰도 사라진다
   */
  @Test
  void test_sc18_V9_는_기존_사용자_행을_그대로_두고_적용된다() throws SQLException {
    // Given
    flyway("8").migrate();
    long emailUser;
    long socialUser;
    Map<String, Object> emailBefore;
    Map<String, Object> socialBefore;
    try (Connection connection = connect()) {
      emailUser = insertUser(connection, "old@ticket.on", "$2a$10$hash", false, "olduser1");
      socialUser = insertUser(connection, "social@ticket.on", null, true, "olduser2");
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate(
            "INSERT INTO social_accounts (user_id, provider, provider_user_id) VALUES ("
                + socialUser
                + ", 'KAKAO', 'k-1')");
      }
      emailBefore = readUser(connection, emailUser);
      socialBefore = readUser(connection, socialUser);
    }

    // When
    flyway(null).migrate();

    // Then
    assertThat(flyway(null).info().current().getVersion().getVersion()).isEqualTo("9");
    try (Connection connection = connect()) {
      assertThat(readUser(connection, emailUser)).isEqualTo(emailBefore);
      assertThat(readUser(connection, socialUser)).isEqualTo(socialBefore);
      assertThat(emailBefore.get("email_verified")).isEqualTo(false);
      assertThat(socialBefore.get("email_verified")).isEqualTo(true);
      try (Statement statement = connection.createStatement();
          ResultSet result =
              statement.executeQuery(
                  "SELECT count(*) FROM users WHERE account_mail_sent_at IS NOT NULL")) {
        result.next();
        assertThat(result.getInt(1)).isZero();
      }
      assertThat(columns(connection, "email_token"))
          .containsExactlyInAnyOrder(
              "id", "user_id", "purpose", "token_hash", "expires_at", "used_at", "created_at");

      insertToken(connection, emailUser, HASH_A);
      assertThatThrownBy(() -> insertToken(connection, socialUser, HASH_A))
          .as("같은 token_hash 두 번")
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("token_hash");
      assertThatThrownBy(() -> insertToken(connection, emailUser + 1000, "b".repeat(64)))
          .as("없는 user_id")
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("foreign key");

      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate("DELETE FROM users WHERE id = " + emailUser);
        try (ResultSet result =
            statement.executeQuery(
                "SELECT count(*) FROM email_token WHERE token_hash = '" + HASH_A + "'")) {
          result.next();
          assertThat(result.getInt(1)).as("사용자 삭제 뒤 토큰").isZero();
        }
      }
    }
  }
}
