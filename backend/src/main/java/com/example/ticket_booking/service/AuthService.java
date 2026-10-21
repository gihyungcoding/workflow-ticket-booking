package com.example.ticket_booking.service;

import com.example.ticket_booking.config.JwtTokenProvider;
import com.example.ticket_booking.domain.User;
import com.example.ticket_booking.domain.UserRole;
import com.example.ticket_booking.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  /** BCrypt 가 해시하는 입력의 상한(바이트). 글자 수가 아니다. */
  private static final int MAX_PASSWORD_BYTES = 72;

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
    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new EmailAlreadyExistsException();
    }
    try {
      User saved =
          userRepository.saveAndFlush(
              new User(normalizedEmail, passwordEncoder.encode(password), name, role));
      return toResponse(saved);
    } catch (DataIntegrityViolationException exception) {
      // existsByEmail 확인과 INSERT 사이에 같은 이메일이 먼저 들어온 경우 (UNIQUE 제약)
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
