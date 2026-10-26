package com.example.ticket_booking.config;

import com.example.ticket_booking.domain.SocialProvider;
import com.example.ticket_booking.domain.UserRole;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * accessToken 을 HS256 으로 발급·검증한다 (ADR-0012). 시각은 주입된 Clock 으로만 판단한다(ADR-0005) — 만료 허용 오차(skew)는 0
 * 이고 만료 시각과 같은 순간부터 만료로 본다(RFC 7519 §4.1.4: 현재 시각이 exp 보다 이전일 때만 유효).
 */
public class JwtTokenProvider {

  public static final long ACCESS_TOKEN_SECONDS = 86400;
  public static final long PENDING_TOKEN_SECONDS = 300;
  private static final String TYPE_ACCESS = "access";
  private static final String TYPE_PENDING = "pending";
  private static final int MIN_SECRET_BYTES = 32;

  private final JwtEncoder encoder;
  private final JwtDecoder decoder;
  private final JwtDecoder pendingDecoder;
  private final Clock clock;

  private JwtTokenProvider(SecretKey key, Clock clock) {
    this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    this.decoder = decoderFor(key, clock, TYPE_ACCESS);
    this.pendingDecoder = decoderFor(key, clock, TYPE_PENDING);
    this.clock = clock;
  }

  /** 같은 키·같은 만료 판단으로 용도(typ)만 달리 검증하는 디코더 — 한 용도의 토큰이 다른 용도로 통과하지 못한다. */
  private static JwtDecoder decoderFor(SecretKey key, Clock clock, String type) {
    NimbusJwtDecoder nimbusDecoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    nimbusDecoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(notExpiredValidator(clock), typeValidator(type)));
    return nimbusDecoder;
  }

  /**
   * JWT_SECRET 환경변수 원문과 Clock 으로 만든다. 비었거나 공백이거나 UTF-8 32바이트 미만이면 IllegalStateException(메시지에
   * JWT_SECRET) 을 던져 기동을 실패시킨다 — 서명 키가 약한 채로 뜨지 않는다(WriteGuardFilter.fromEnv 와 같은 관례).
   */
  public static JwtTokenProvider fromEnv(String secret, Clock clock) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("JWT_SECRET 이 필요하다 (비어 있으면 토큰을 서명할 수 없다)");
    }
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "JWT_SECRET 은 UTF-8 기준 "
              + MIN_SECRET_BYTES
              + "바이트 이상이어야 한다 (현재 "
              + bytes.length
              + "바이트)");
    }
    return new JwtTokenProvider(new SecretKeySpec(bytes, "HmacSHA256"), clock);
  }

  public String issueAccessToken(Long userId, String email, UserRole role) {
    Instant issuedAt = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(String.valueOf(userId))
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(ACCESS_TOKEN_SECONDS))
            .claim("email", email)
            .claim("role", role.name())
            .claim("typ", TYPE_ACCESS)
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  /** 서명·만료·용도(typ=access)가 모두 유효하면 사용자 id 를, 하나라도 어긋나면 비어 있는 Optional 을 돌려준다. */
  public Optional<Long> verifyAccessToken(String token) {
    try {
      Jwt jwt = decoder.decode(token);
      return Optional.of(Long.parseLong(jwt.getSubject()));
    } catch (JwtException | IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  /** pendingToken 이 담는 값. */
  public record PendingSignup(
      String email,
      String name,
      SocialProvider provider,
      String providerUserId,
      boolean emailVerified) {}

  /** typ=pending, 발급 시각 + 5분 만료의 토큰을 발급한다. sub 가 없어 사용자 토큰으로 오인될 여지가 없다. */
  public String issuePendingToken(
      String email,
      String name,
      SocialProvider provider,
      String providerUserId,
      boolean emailVerified) {
    Instant issuedAt = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(PENDING_TOKEN_SECONDS))
            .claim("email", email)
            .claim("name", name)
            .claim("provider", provider.name())
            .claim("providerUserId", providerUserId)
            .claim("emailVerified", emailVerified)
            .claim("typ", TYPE_PENDING)
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  /**
   * 서명·만료·용도(typ=pending)·필수 클레임이 모두 유효하면 값을, 하나라도 어긋나면 비어 있는 Optional 을 돌려준다. 서명이 맞아도 문자열은 제공자 응답과
   * 같은 규칙(SocialInputRules)을 다시 거친다 — 키가 새거나 발급 쪽에 결함이 있어도 규칙 밖의 값이 DB 에 닿지 않는다.
   */
  public Optional<PendingSignup> verifyPendingToken(String token) {
    try {
      Jwt jwt = pendingDecoder.decode(token);
      Map<String, Object> claims = jwt.getClaims();
      String email = SocialInputRules.email(claims.get("email"));
      String name = SocialInputRules.name(claims.get("name"));
      String providerUserId = SocialInputRules.providerUserId(claims.get("providerUserId"));
      // 검증 여부는 Boolean 이어야 한다 — 없거나 "true"·1 같은 값은 거부한다(참으로 읽히면 자동 연결 근거가 되기 때문)
      if (email == null
          || name == null
          || providerUserId == null
          || !(claims.get("provider") instanceof String provider)
          || !(claims.get("emailVerified") instanceof Boolean emailVerified)) {
        return Optional.empty();
      }
      return Optional.of(
          new PendingSignup(
              email, name, SocialProvider.valueOf(provider), providerUserId, emailVerified));
    } catch (JwtException | IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  /**
   * Spring 의 JwtTimestampValidator 는 exp 와 같은 순간을 아직 유효로 본다 — RFC 7519 는 현재 시각이 exp 보다 이전이어야 한다고
   * 하므로 직접 비교한다. exp 가 없는 토큰은 만료를 알 수 없어 거부한다.
   */
  private static OAuth2TokenValidator<Jwt> notExpiredValidator(Clock clock) {
    return jwt -> {
      Instant expiresAt = jwt.getExpiresAt();
      return expiresAt != null && clock.instant().isBefore(expiresAt)
          ? OAuth2TokenValidatorResult.success()
          : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "만료된 토큰이다", null));
    };
  }

  private static OAuth2TokenValidator<Jwt> typeValidator(String type) {
    return jwt ->
        type.equals(jwt.getClaims().get("typ"))
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", type + " 토큰이 아니다", null));
  }
}
