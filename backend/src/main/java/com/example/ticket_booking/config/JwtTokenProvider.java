package com.example.ticket_booking.config;

import com.example.ticket_booking.domain.SocialProvider;
import com.example.ticket_booking.domain.UserRole;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
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
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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

  /**
   * JWS 압축 직렬화 — base64url 세 마디. 디코더는 마디 안의 base64url 밖 문자(NUL·짝 없는 서로게이트·= 등)를 건너뛰고 읽어서, 정상 토큰 뒤에
   * 문자를 붙여도 통과했다(TASK-016 SC-14·RC-2). 디코더에 넘기기 전에 형식을 먼저 본다 — accessToken·pendingToken 공통.
   */
  private static final Pattern COMPACT_JWS =
      Pattern.compile("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

  /** 페이로드에 같은 키가 두 번 있으면 읽기에서 실패한다 — 파서마다 어느 쪽 값을 쓰는지가 달라 판정이 갈리는 것을 막는다. */
  private static final ObjectMapper PAYLOAD_READER =
      JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();

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
   * JWT_SECRET) 을 던져 기동을 실패시킨다 — 서명 키가 약한 채로 뜨지 않는다.
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

  /** 서명·만료·용도(typ=access)·역할이 모두 유효하면 사용자 id 를, 하나라도 어긋나면 비어 있는 Optional 을 돌려준다. */
  public Optional<Long> verifyAccessToken(String token) {
    return verifyAccessClaims(token).map(AccessClaims::userId);
  }

  /**
   * 서명·만료·용도(typ=access)가 유효하고, sub 가 long 범위의 정수 문자열이며, role 이 UserRole 이름과 정확히 같은 문자열이면 그 값을
   * 돌려준다. role 이 없거나 정의 밖이면 토큰 전체를 무효로 본다 — 역할 없는 인증 주체를 만들지 않는다(ADR-0029).
   */
  public Optional<AccessClaims> verifyAccessClaims(String token) {
    JsonNode payload = readPayload(token);
    // 디코더는 숫자 sub 를 문자열로 바꿔 읽는다 — 발급하는 sub 는 문자열이므로 원문에서 본다
    if (payload == null || !payload.path("sub").isString()) {
      return Optional.empty();
    }
    try {
      Jwt jwt = decoder.decode(token);
      Optional<UserRole> role = roleOf(jwt.getClaims().get("role"));
      if (role.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(new AccessClaims(Long.parseLong(jwt.getSubject()), role.get()));
    } catch (JwtException | IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  /** 대소문자·공백·보이지 않는 문자까지 그대로 비교한다 — 이름이 정확히 같은 문자열만 역할이다. */
  private static Optional<UserRole> roleOf(Object claim) {
    if (!(claim instanceof String name)) {
      return Optional.empty();
    }
    return Arrays.stream(UserRole.values()).filter(role -> role.name().equals(name)).findFirst();
  }

  /** pendingToken 이 담는 값. expiresAt 은 역할 선택 화면이 남은 시간을 보여 주는 데 쓴다. */
  public record PendingSignup(
      String email,
      String name,
      SocialProvider provider,
      String providerUserId,
      boolean emailVerified,
      Instant expiresAt) {}

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
    JsonNode payload = readPayload(token);
    if (payload == null || !hasWholeSecondsExp(payload)) {
      return Optional.empty();
    }
    try {
      Jwt jwt = pendingDecoder.decode(token);
      // 이 서버가 발급하는 pendingToken 의 만료는 발급 + 5분이다. 그보다 먼 만료는 발급 쪽 결함이나 위조로 보고 거부한다(claim_schema.exp)
      Instant expiresAt = jwt.getExpiresAt();
      if (expiresAt.isAfter(clock.instant().plusSeconds(PENDING_TOKEN_SECONDS))) {
        return Optional.empty();
      }
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
              email,
              name,
              SocialProvider.valueOf(provider),
              providerUserId,
              emailVerified,
              expiresAt));
    } catch (JwtException | IllegalArgumentException exception) {
      return Optional.empty();
    }
  }

  /**
   * 디코더에 넘기기 전의 공통 형식 검사 — 압축 JWS 세 마디이고 페이로드가 중복 키 없는 JSON 객체면 그 페이로드를, 아니면 null 을 돌려준다.
   * accessToken 과 pendingToken 이 같은 지점을 지나야 한쪽에만 구멍이 남지 않는다(TASK-016 RC-2).
   */
  private static JsonNode readPayload(String token) {
    if (token == null || !COMPACT_JWS.matcher(token).matches()) {
      return null;
    }
    try {
      String payload = token.substring(token.indexOf('.') + 1, token.lastIndexOf('.'));
      JsonNode node = PAYLOAD_READER.readTree(Base64.getUrlDecoder().decode(payload));
      return node != null && node.isObject() ? node : null;
    } catch (RuntimeException unreadable) {
      return null;
    }
  }

  /**
   * exp 가 정수 초인지 페이로드 원문에서 본다. 디코더는 소수(120.5)를 정수로 잘라 읽고, long 범위를 넘는 값도 다른 값으로 바꿔 읽어서 변환 뒤에는 구별할 수
   * 없다.
   */
  private static boolean hasWholeSecondsExp(JsonNode payload) {
    JsonNode exp = payload.get("exp");
    return exp != null && exp.isIntegralNumber() && exp.canConvertToLong();
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
