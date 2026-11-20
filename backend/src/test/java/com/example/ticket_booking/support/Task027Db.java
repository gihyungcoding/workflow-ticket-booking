package com.example.ticket_booking.support;

import com.example.ticket_booking.domain.User;
import com.example.ticket_booking.domain.UserRole;
import com.example.ticket_booking.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * TASK-027 PostgreSQL 테스트의 DB 도우미 — 사용자 행 준비와 JDBC 로 새로 읽기. 테스트 클래스는 클래스 레벨 {@code @Transactional}
 * 을 쓰지 않는다(요구사항 §5, CLAUDE.md JPA 지시) — 여기서 읽는 값은 커밋된 값이다.
 */
public final class Task027Db {

  private final JdbcTemplate jdbc;
  private final UserRepository users;

  public Task027Db(JdbcTemplate jdbc, UserRepository users) {
    this.jdbc = jdbc;
    this.users = users;
  }

  /** 사용자와 그 사용자를 참조하는 모든 행(토큰·소셜 연결·공연…)을 지운다. */
  public void reset() {
    jdbc.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
  }

  /** 규약의 사용자 E — 이메일 가입(비밀번호 있음, 미인증). */
  public Long emailUser(String email) {
    return users
        .save(
            new User(
                email,
                "$2a$10$abcdefghijklmnopqrstuuabcdefghijklmnopqrstuvwxyzABCDE",
                "이메일사용자",
                UserRole.AUDIENCE,
                false,
                UserFixtures.fixtureHandle(email)))
        .getId();
  }

  /** 규약의 사용자 S — 소셜 가입(비밀번호 없음, 인증됨). */
  public Long socialUser(String email) {
    return users
        .save(
            new User(
                email, null, "소셜사용자", UserRole.AUDIENCE, true, UserFixtures.fixtureHandle(email)))
        .getId();
  }

  public static String sha256Hex(String raw) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  /** token_hash 로 찾은 행의 used_at. 행이 없으면 예외다 — 「바뀌지 않는다」 단언은 행이 있어야 뜻이 있다. */
  public Instant usedAt(String raw) {
    Timestamp value =
        jdbc.queryForObject(
            "SELECT used_at FROM email_token WHERE token_hash = ?",
            Timestamp.class,
            sha256Hex(raw));
    return value == null ? null : value.toInstant();
  }

  public int countByHash(String raw) {
    Integer count =
        jdbc.queryForObject(
            "SELECT count(*) FROM email_token WHERE token_hash = ?", Integer.class, sha256Hex(raw));
    return count == null ? 0 : count;
  }

  public List<Map<String, Object>> tokenRows(Long userId, String purpose) {
    return jdbc.queryForList(
        "SELECT * FROM email_token WHERE user_id = ? AND purpose = ?", userId, purpose);
  }

  public List<Map<String, Object>> allTokenRows() {
    return jdbc.queryForList("SELECT * FROM email_token");
  }

  public int tokenCount() {
    Integer count = jdbc.queryForObject("SELECT count(*) FROM email_token", Integer.class);
    return count == null ? 0 : count;
  }

  public Instant accountMailSentAt(Long userId) {
    Timestamp value =
        jdbc.queryForObject(
            "SELECT account_mail_sent_at FROM users WHERE id = ?", Timestamp.class, userId);
    return value == null ? null : value.toInstant();
  }

  public void setAccountMailSentAt(Long userId, Instant value) {
    jdbc.update(
        "UPDATE users SET account_mail_sent_at = ? WHERE id = ?",
        value == null ? null : Timestamp.from(value),
        userId);
  }
}
