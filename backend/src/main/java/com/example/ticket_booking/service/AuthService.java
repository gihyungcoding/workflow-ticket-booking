package com.example.ticket_booking.service;

import com.example.ticket_booking.config.JwtTokenProvider;
import com.example.ticket_booking.domain.SocialAccount;
import com.example.ticket_booking.domain.User;
import com.example.ticket_booking.domain.UserRole;
import com.example.ticket_booking.repository.SocialAccountRepository;
import com.example.ticket_booking.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final SocialAccountRepository socialAccountRepository;

  @Autowired
  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider,
      SocialAccountRepository socialAccountRepository) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.socialAccountRepository = socialAccountRepository;
  }

  /** 이메일 가입·로그인만 쓰는 단위 테스트용 — 소셜 메서드는 socialAccountRepository 가 있어야 동작한다. */
  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider) {
    this(userRepository, passwordEncoder, jwtTokenProvider, null);
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
            // 소셜 전용 계정은 해시가 없다 — 비교하지 않고 다른 실패와 같은 응답으로 처리한다
            .filter(found -> found.getPasswordHash() != null)
            .filter(found -> passwordEncoder.matches(password, found.getPasswordHash()))
            .orElseThrow(InvalidCredentialsException::new);
    return toLoginResponse(user);
  }

  @Transactional(readOnly = true)
  public UserResponse getMe(Long userId) {
    return userRepository
        .findById(userId)
        .map(AuthService::toResponse)
        .orElseThrow(UnauthenticatedException::new);
  }

  /**
   * 제공자 로그인 판정 — 이미 연결된 계정이면 재로그인, 이메일이 같고 제공자가 검증했으면 기존 계정에 연결(ADR-0014), 아니면 가입 대기(pendingToken).
   * 가입 대기에서는 DB 에 아무것도 쓰지 않는다.
   *
   * <p>트랜잭션을 걸지 않는다 — 같은 제공자 계정의 콜백이 동시에 오면 연결 INSERT 하나가 UNIQUE 위반(23505)이 되는데, 한 트랜잭션 안에서는 그 실패가
   * 이후 재조회까지 롤백 전용으로 만든다. 리포지토리 호출마다 따로 커밋하고, 위반이면 먼저 들어간 행을 다시 읽는다.
   */
  public SocialLoginResult loginWithSocial(SocialProfile profile) {
    Optional<SocialAccount> linked =
        socialAccountRepository.findByProviderAndProviderUserId(
            profile.provider(), profile.providerUserId());
    if (linked.isPresent()) {
      return loggedIn(linked.get().getUserId());
    }
    Optional<User> sameEmail =
        profile.emailVerified() ? userRepository.findByEmail(profile.email()) : Optional.empty();
    if (sameEmail.isEmpty()) {
      return new SocialLoginResult(
          null,
          jwtTokenProvider.issuePendingToken(
              profile.email(), profile.name(), profile.provider(), profile.providerUserId()));
    }
    Long userId = sameEmail.get().getId();
    try {
      socialAccountRepository.saveAndFlush(
          new SocialAccount(userId, profile.provider(), profile.providerUserId()));
    } catch (DataIntegrityViolationException exception) {
      if (!isUniqueViolation(exception)) {
        throw exception;
      }
      // 동시에 온 같은 콜백이 먼저 연결했다 — 그 행의 사용자로 로그인시킨다
      userId =
          socialAccountRepository
              .findByProviderAndProviderUserId(profile.provider(), profile.providerUserId())
              .orElseThrow(() -> exception)
              .getUserId();
    }
    return loggedIn(userId);
  }

  /** pendingToken 과 역할로 users·social_accounts 를 만들고 로그인시킨다. 둘 중 하나라도 실패하면 둘 다 만들지 않는다. */
  @Transactional
  public LoginResponse completeSocialSignUp(String pendingToken, UserRole role) {
    JwtTokenProvider.PendingSignup pending =
        jwtTokenProvider
            .verifyPendingToken(pendingToken)
            .orElseThrow(PendingSignupExpiredException::new);
    if (userRepository.existsByEmail(pending.email())) {
      throw new EmailAlreadyExistsException();
    }
    if (socialAccountRepository
        .findByProviderAndProviderUserId(pending.provider(), pending.providerUserId())
        .isPresent()) {
      throw new SocialAccountAlreadyLinkedException();
    }
    User user;
    try {
      user = userRepository.saveAndFlush(new User(pending.email(), null, pending.name(), role));
    } catch (DataIntegrityViolationException exception) {
      // 존재 확인과 INSERT 사이에 같은 이메일이 먼저 들어왔다(동시 요청)
      if (!isUniqueViolation(exception)) {
        throw exception;
      }
      throw new EmailAlreadyExistsException();
    }
    try {
      socialAccountRepository.saveAndFlush(
          new SocialAccount(user.getId(), pending.provider(), pending.providerUserId()));
    } catch (DataIntegrityViolationException exception) {
      if (!isUniqueViolation(exception)) {
        throw exception;
      }
      throw new SocialAccountAlreadyLinkedException();
    }
    return toLoginResponse(user);
  }

  private SocialLoginResult loggedIn(Long userId) {
    User user = userRepository.findById(userId).orElseThrow(UnauthenticatedException::new);
    return new SocialLoginResult(
        jwtTokenProvider.issueAccessToken(user.getId(), user.getEmail(), user.getRole()), null);
  }

  private LoginResponse toLoginResponse(User user) {
    String accessToken =
        jwtTokenProvider.issueAccessToken(user.getId(), user.getEmail(), user.getRole());
    return new LoginResponse(
        accessToken, "Bearer", JwtTokenProvider.ACCESS_TOKEN_SECONDS, toResponse(user));
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
