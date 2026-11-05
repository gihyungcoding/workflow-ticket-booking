package com.example.ticket_booking.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.ticket_booking.config.JwtTokenProvider;
import com.example.ticket_booking.domain.SocialProvider;
import com.example.ticket_booking.domain.User;
import com.example.ticket_booking.domain.UserRole;
import com.example.ticket_booking.repository.SocialAccountRepository;
import com.example.ticket_booking.repository.UserRepository;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * TASK-016 Phase 3 자체 리뷰 RC-1 — users INSERT 의 UNIQUE 위반을 이메일 중복(409)과 아이디 경합(503)으로 가를 때, 메시지에 섞여
 * 들어오는 사용자 입력값(이메일)이 판정을 흔들지 않는다.
 *
 * <p>경쟁 구간(존재 확인과 INSERT 사이)이라 API 로는 결정적으로 만들 수 없어, 저장소가 던지는 예외를 정해 두고 서비스 동작을 본다. 메시지는 실제 DB 가 낸
 * 문자열을 옮긴 것이다 — PostgreSQL 16(Testcontainers 프로브, 세부 줄은 클라이언트 로캘로 번역된다), H2 2.4(MODE=PostgreSQL).
 */
class AuthServiceUniqueViolationTest {

  private static final String H2_EMAIL =
      "Unique index or primary key violation: \"public.CONSTRAINT_6 INDEX public.CONSTRAINT_INDEX_6"
          + " ON public.users(email NULLS FIRST) VALUES ( /* 1 */ 'ux_users_handle@x.com' )\";"
          + " SQL statement:\ninsert into users (created_at,email,email_verified,handle,name,password_hash,role)"
          + " values (?,?,?,?,?,?,?) [23505-240]";
  private static final String H2_HANDLE =
      "Unique index or primary key violation: \"public.UK_HANDLE INDEX public.UK_HANDLE_INDEX_6"
          + " ON public.users(handle NULLS FIRST) VALUES ( /* 2 */ 'wave0001' )\";"
          + " SQL statement:\ninsert into users (created_at,email,email_verified,handle,name,password_hash,role)"
          + " values (?,?,?,?,?,?,?) [23505-240]";
  private static final String PG_EMAIL =
      "ERROR: duplicate key value violates unique constraint \"users_email_key\"\n"
          + "  세부 정보: Key (email)=(ux_users_handle@x.com) already exists.";
  private static final String PG_EMAIL_DETAIL_EN =
      "ERROR: duplicate key value violates unique constraint \"users_email_key\"\n"
          + "  Detail: Key (email)=(a@users(handle.com) already exists.";
  private static final String PG_HANDLE =
      "ERROR: duplicate key value violates unique constraint \"ux_users_handle\"\n"
          + "  세부 정보: Key (handle)=(wave0001) already exists.";

  private record Row(String label, String message, Class<? extends RuntimeException> expected) {}

  private static final List<Row> ROWS =
      List.of(
          new Row("H2 이메일 위반 (값에 ux_users_handle)", H2_EMAIL, EmailAlreadyExistsException.class),
          new Row("H2 아이디 위반", H2_HANDLE, HandleGenerationFailedException.class),
          new Row(
              "PG 이메일 위반 (세부 줄 값에 ux_users_handle)", PG_EMAIL, EmailAlreadyExistsException.class),
          new Row(
              "PG 이메일 위반 (세부 줄 값에 users(handle)",
              PG_EMAIL_DETAIL_EN,
              EmailAlreadyExistsException.class),
          new Row("PG 아이디 위반", PG_HANDLE, HandleGenerationFailedException.class));

  private static DataIntegrityViolationException violation(String message) {
    return new DataIntegrityViolationException("저장 실패", new SQLException(message, "23505"));
  }

  @Test
  void 이메일_가입_INSERT_의_UNIQUE_위반은_위반한_제약으로만_가른다() {
    SoftAssertions soft = new SoftAssertions();
    for (Row row : ROWS) {
      UserRepository userRepository = mock(UserRepository.class);
      PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
      when(userRepository.existsByEmail(anyString())).thenReturn(false);
      when(passwordEncoder.encode(anyString())).thenReturn("hash");
      when(userRepository.saveAndFlush(any(User.class))).thenThrow(violation(row.message()));
      AuthService service =
          new AuthService(userRepository, passwordEncoder, mock(JwtTokenProvider.class));

      Throwable thrown =
          catchAny(
              () ->
                  service.signUp(
                      "ux_users_handle@x.com",
                      "password123",
                      "password123",
                      "이름",
                      UserRole.AUDIENCE));

      soft.assertThat(thrown).as(row.label()).isInstanceOf(row.expected());
    }
    soft.assertAll();
  }

  @Test
  void 소셜_가입_완료_INSERT_의_UNIQUE_위반은_위반한_제약으로만_가른다() {
    SoftAssertions soft = new SoftAssertions();
    for (Row row : ROWS) {
      UserRepository userRepository = mock(UserRepository.class);
      SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
      JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
      when(jwtTokenProvider.verifyPendingToken("token"))
          .thenReturn(
              Optional.of(
                  new JwtTokenProvider.PendingSignup(
                      "ux_users_handle@x.com",
                      "이름",
                      SocialProvider.KAKAO,
                      "k-1",
                      true,
                      Instant.parse("2026-11-11T08:40:00Z"))));
      when(userRepository.existsByEmail(anyString())).thenReturn(false);
      when(socialAccountRepository.findByProviderAndProviderUserId(any(), anyString()))
          .thenReturn(Optional.empty());
      when(userRepository.saveAndFlush(any(User.class))).thenThrow(violation(row.message()));
      AuthService service =
          new AuthService(
              userRepository,
              mock(PasswordEncoder.class),
              jwtTokenProvider,
              socialAccountRepository,
              new HandleGenerator(userRepository, new SecureRandom()));

      Throwable thrown = catchAny(() -> service.completeSocialSignUp("token", UserRole.AUDIENCE));

      soft.assertThat(thrown).as(row.label()).isInstanceOf(row.expected());
    }
    soft.assertAll();
  }

  private static Throwable catchAny(Runnable action) {
    try {
      action.run();
      return null;
    } catch (RuntimeException exception) {
      return exception;
    }
  }
}
