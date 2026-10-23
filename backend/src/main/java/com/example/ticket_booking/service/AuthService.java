package com.example.ticket_booking.service;

import com.example.ticket_booking.config.JwtTokenProvider;
import com.example.ticket_booking.domain.User;
import com.example.ticket_booking.domain.UserRole;
import com.example.ticket_booking.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Locale;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  /** BCrypt 가 해시하는 입력의 상한(바이트). 글자 수가 아니다. */
  private static final int MAX_PASSWORD_BYTES = 72;

  /** users.email 컬럼 길이. 소문자화로 길이가 늘어나는 문자가 있어 정규화 후에 판정한다. */
  private static final int MAX_EMAIL_LENGTH = 255;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Transactional
  public UserResponse signUp(
      String email, String password, String passwordConfirm, String name, UserRole role) {
    if (!password.equals(passwordConfirm)) {
      throw new InvalidRequestException("passwordConfirm: 비밀번호 확인이 일치하지 않습니다");
    }
    if (exceedsBcryptLimit(password)) {
      throw new InvalidRequestException("password: UTF-8 기준 72바이트를 넘을 수 없습니다");
    }
    String normalizedEmail = normalize(email);
    if (normalizedEmail.length() > MAX_EMAIL_LENGTH) {
      throw new InvalidRequestException("email: 정규화(소문자) 후 " + MAX_EMAIL_LENGTH + "자를 넘을 수 없습니다");
    }
    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new EmailAlreadyExistsException();
    }
    try {
      User saved =
          userRepository.saveAndFlush(
              new User(normalizedEmail, passwordEncoder.encode(password), name, role));
      return toResponse(saved);
    } catch (DataIntegrityViolationException exception) {
      // existsByEmail 확인과 INSERT 사이에 같은 이메일이 먼저 들어온 경우(UNIQUE 제약, SQLState 23505).
      // 그 외 무결성 위반은 이메일 중복이 아니므로 409 로 오보하지 않고 그대로 던진다
      if (!isUniqueViolation(exception)) {
        throw exception;
      }
      throw new EmailAlreadyExistsException();
    }
  }

  @Transactional(readOnly = true)
  public LoginResponse login(String email, String password) {
    // 72바이트를 넘는 비밀번호로 가입한 사용자는 있을 수 없다 — 해시 비교로 가지 않고 같은 실패로 처리한다
    if (exceedsBcryptLimit(password)) {
      throw new InvalidCredentialsException();
    }
    User user =
        userRepository
            .findByEmail(normalize(email))
            .filter(found -> passwordEncoder.matches(password, found.getPasswordHash()))
            .orElseThrow(InvalidCredentialsException::new);
    String accessToken =
        jwtTokenProvider.issueAccessToken(user.getId(), user.getEmail(), user.getRole());
    return new LoginResponse(
        accessToken, "Bearer", JwtTokenProvider.ACCESS_TOKEN_SECONDS, toResponse(user));
  }

  @Transactional(readOnly = true)
  public UserResponse getMe(Long userId) {
    return userRepository
        .findById(userId)
        .map(AuthService::toResponse)
        .orElseThrow(UnauthenticatedException::new);
  }

  /** 제공자 로그인 판정 — 재로그인·자동 연결이면 accessToken, 아니면 가입 대기(pendingToken). */
  public SocialLoginResult loginWithSocial(SocialProfile profile) {
    throw new UnsupportedOperationException();
  }

  /** pendingToken 과 역할로 users·social_accounts 를 만들고 로그인시킨다. */
  public LoginResponse completeSocialSignUp(String pendingToken, UserRole role) {
    throw new UnsupportedOperationException();
  }

  /** 예외의 근본 원인이 UNIQUE 위반(SQLState 23505 — PostgreSQL·H2 공통)인지 본다. */
  private static boolean isUniqueViolation(Throwable exception) {
    return NestedExceptionUtils.getMostSpecificCause(exception) instanceof SQLException sql
        && "23505".equals(sql.getSQLState());
  }

  private static String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private static boolean exceedsBcryptLimit(String password) {
    return password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES;
  }

  private static UserResponse toResponse(User user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
  }
}
