package com.example.ticket_booking.config;

import com.example.ticket_booking.domain.UserRole;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * accessToken 을 HS256 으로 발급·검증한다 (ADR-0012). 시각은 주입된 Clock 으로만 판단한다(ADR-0005) — 만료 허용 오차(skew)는 0
 * 이다.
 */
public class JwtTokenProvider {

  public static final long ACCESS_TOKEN_SECONDS = 86400;
  private static final String TYPE_ACCESS = "access";
  private static final int MIN_SECRET_BYTES = 32;

  private final JwtEncoder encoder;
  private final JwtDecoder decoder;
  private final Clock clock;

  private JwtTokenProvider(SecretKey key, Clock clock) {
    this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    NimbusJwtDecoder nimbusDecoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
    timestampValidator.setClock(clock);
    nimbusDecoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(timestampValidator, accessTypeValidator()));
    this.decoder = nimbusDecoder;
    this.clock = clock;
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

  private static OAuth2TokenValidator<Jwt> accessTypeValidator() {
    return jwt ->
        TYPE_ACCESS.equals(jwt.getClaimAsString("typ"))
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "access 토큰이 아니다", null));
  }
}
