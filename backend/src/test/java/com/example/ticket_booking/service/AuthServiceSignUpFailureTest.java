package com.example.ticket_booking.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.ticket_booking.config.JwtTokenProvider;
import com.example.ticket_booking.domain.User;
import com.example.ticket_booking.domain.UserRole;
import com.example.ticket_booking.repository.UserRepository;
import java.sql.SQLException;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * TASK-009 시나리오 SC-22 — 가입 중 저장 실패는 UNIQUE 위반일 때만 이메일 중복 오류다.
 *
 * <p>H2 로는 UNIQUE 외 무결성 위반을 만들 수 없어 저장소가 던지는 예외를 정해 두고 서비스 계층 동작을 본다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-009.md
 */
class AuthServiceSignUpFailureTest {

  @Test
  void test_sc22_가입_중_저장_실패는_이메일_중복_UNIQUE_위반일_때만_409다() {
    // Given 가입하려는 이메일은 users 에 없다(선검사는 "없음"을 돌려준다)
    record Row(String label, String sqlState, boolean expectEmailAlreadyExists) {}
    List<Row> rows =
        List.of(
            new Row("UNIQUE 위반 23505", "23505", true),
            new Row("NOT NULL 위반 23502", "23502", false),
            new Row("잘못된 문자 인코딩 22021", "22021", false));
    SoftAssertions soft = new SoftAssertions();

    for (Row row : rows) {
      UserRepository userRepository = mock(UserRepository.class);
      PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
      when(userRepository.existsByEmail(anyString())).thenReturn(false);
      when(passwordEncoder.encode(anyString())).thenReturn("hash");
      DataIntegrityViolationException failure =
          new DataIntegrityViolationException(
              "저장 실패", new SQLException("저장소가 던진 실패", row.sqlState()));
      when(userRepository.saveAndFlush(any(User.class))).thenThrow(failure);
      AuthService service =
          new AuthService(userRepository, passwordEncoder, mock(JwtTokenProvider.class));

      // When 가입을 요청했는데 저장 단계가 해당 실패로 끝난다
      Throwable thrown = null;
      try {
        service.signUp("new@example.com", "password123", "password123", "홍길동", UserRole.AUDIENCE);
      } catch (RuntimeException exception) {
        thrown = exception;
      }

      // Then 각 행의 결과가 기대와 같다
      soft.assertThat(thrown).as(row.label() + " 예외").isNotNull();
      if (row.expectEmailAlreadyExists()) {
        soft.assertThat(thrown)
            .as(row.label() + " → 이메일 중복 오류")
            .isInstanceOf(EmailAlreadyExistsException.class);
      } else {
        // And 이메일 중복 오류가 아니다 — 저장소가 던진 DataIntegrityViolationException 이 그대로 전달된다
        soft.assertThat(thrown)
            .as(row.label() + " → 이메일 중복 오류가 아님")
            .isNotInstanceOf(EmailAlreadyExistsException.class);
        soft.assertThat(thrown).as(row.label() + " → 원래 실패").isSameAs(failure);
      }
    }
    soft.assertAll();
  }
}
