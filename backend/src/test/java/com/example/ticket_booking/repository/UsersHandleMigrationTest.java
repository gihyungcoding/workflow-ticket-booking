package com.example.ticket_booking.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * TASK-016 시나리오 SC-07 · SC-13 — V5 마이그레이션(기존 회원 아이디 채우기)을 운영과 같은 엔진(PostgreSQL)에서 확인한다.
 *
 * <p>나머지 테스트는 H2 + ddl-auto 로 돌아 Flyway 를 거치지 않는다(ADR-0006). V5 는 PostgreSQL 문법이라 여기서만 검증한다. 단어 목록은
 * 시나리오 규약 표를 그대로 옮긴 것이다 — 구현의 목록과 대조하려는 것이므로 구현에서 가져오지 않는다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-016.md
 */
@Testcontainers
class UsersHandleMigrationTest {

  private static final String HANDLE_PATTERN = "^[a-z]{3,6}[0-9]{4}$";
  private static final Set<String> WORDS =
      Set.of(
          "wave", "star", "stage", "encore", "aria", "beat", "chord", "drum", "echo", "flute",
          "harp", "jazz", "lyric", "melody", "note", "opera", "piano", "rhythm", "solo", "tempo",
          "tune", "verse", "viola", "waltz", "cello", "choir", "disco", "folk", "hymn", "oboe",
          "lute", "gig");

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

  private static void insertV4User(
      Connection connection, String email, String hash, boolean verified) throws SQLException {
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO users (email, password_hash, name, role, email_verified) VALUES (?, ?, ?, ?, ?)")) {
      insert.setString(1, email);
      insert.setString(2, hash);
      insert.setString(3, "기존");
      insert.setString(4, "AUDIENCE");
      insert.setBoolean(5, verified);
      insert.executeUpdate();
    }
  }

  private static void insertWithoutHandle(Connection connection, String email) throws SQLException {
    insertV4User(connection, email, "hash", false);
  }

  private static void insertWithHandle(Connection connection, String email, String handle)
      throws SQLException {
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO users (email, password_hash, name, role, email_verified, handle)"
                + " VALUES (?, 'hash', '기존', 'AUDIENCE', false, ?)")) {
      insert.setString(1, email);
      insert.setString(2, handle);
      insert.executeUpdate();
    }
  }

  // ---------- SC-07 ----------

  @Test
  void test_sc07_마이그레이션_뒤_기존_회원_전원이_겹치지_않는_아이디를_갖는다() throws SQLException {
    // Given V4 까지 적용된 users 에 표의 회원이 있다
    flyway("4").migrate();
    Map<String, Object[]> seeds =
        Map.of(
            "harin.kim@kakao.com", new Object[] {null, true},
            "harin.kim@naver.com", new Object[] {"hash", false},
            "wave7302@example.com", new Object[] {"hash", false},
            "ab@example.com", new Object[] {"hash", false},
            "김하린@example.com", new Object[] {"hash", false});
    try (Connection connection = connect()) {
      for (Map.Entry<String, Object[]> seed : seeds.entrySet()) {
        insertV4User(
            connection, seed.getKey(), (String) seed.getValue()[0], (Boolean) seed.getValue()[1]);
      }
    }

    // When V5 를 적용한다
    flyway(null).migrate();

    try (Connection connection = connect()) {
      List<String> handles = new ArrayList<>();
      try (Statement statement = connection.createStatement();
          ResultSet rows = statement.executeQuery("SELECT email, handle FROM users")) {
        while (rows.next()) {
          handles.add(rows.getString("handle"));
        }
      }
      // Then 표의 모든 행의 handle 이 NULL 이 아니고 형식에 맞는다
      assertThat(handles).hasSize(seeds.size());
      assertThat(handles)
          .allSatisfy(handle -> assertThat(handle).isNotNull().matches(HANDLE_PATTERN));
      // And 숫자를 뗀 단어가 규약 표의 단어 목록 안에 있다
      assertThat(handles)
          .allSatisfy(handle -> assertThat(WORDS).contains(handle.replaceAll("[0-9]{4}$", "")));
      // And handle 값이 서로 모두 다르다
      assertThat(new HashSet<>(handles)).hasSize(handles.size());
      // And 이어서 handle 없이 회원을 저장하면 제약 위반으로 실패한다
      assertThatThrownBy(() -> insertWithoutHandle(connection, "no-handle@example.com"))
          .isInstanceOf(SQLException.class);
      // And 이미 있는 handle 로 회원을 저장하면 제약 위반으로 실패한다
      assertThatThrownBy(() -> insertWithHandle(connection, "dup@example.com", handles.get(0)))
          .isInstanceOf(SQLException.class);
    }
  }

  // ---------- SC-13 ----------

  @Test
  void test_sc13_회원이_없을_때도_마이그레이션이_성공한다() throws SQLException {
    // Given V4 까지 적용된 users 가 비어 있다
    flyway("4").migrate();

    // When V5 를 적용한다
    var result = flyway(null).migrate();

    // Then 적용이 성공한다
    assertThat(result.success).isTrue();
    assertThat(result.targetSchemaVersion).isEqualTo("5");
    // And 이어서 handle 없이 회원을 저장하면 제약 위반으로 실패한다
    try (Connection connection = connect()) {
      assertThatThrownBy(() -> insertWithoutHandle(connection, "no-handle@example.com"))
          .isInstanceOf(SQLException.class);
    }
  }
}
