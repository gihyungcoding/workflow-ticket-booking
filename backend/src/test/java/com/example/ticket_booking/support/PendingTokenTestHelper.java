package com.example.ticket_booking.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * pendingToken 을 구현과 무관하게 Nimbus 로 직접 만든다 — 검증 로직이 형식 깨짐 때문에 우연히 401 을 내는 가짜 통과를 막는다. null 인 클레임은
 * 아예 넣지 않는다.
 */
public final class PendingTokenTestHelper {

  public static final String EMAIL = "new@example.com";
  public static final String NAME = "신규";
  public static final String PROVIDER = "GOOGLE";
  public static final String PROVIDER_USER_ID = "g-100";
  public static final long PENDING_SECONDS = 300;

  /** claims 맵에서 해당 클레임을 아예 넣지 않는다는 표시. */
  public static final Object ABSENT = new Object();

  private PendingTokenTestHelper() {}

  /** 정상 토큰의 클레임(구글, new@example.com, typ=pending, emailVerified=true). 행마다 하나만 바꿔 쓴다. */
  public static Map<String, Object> validClaims(Instant issuedAt) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("email", EMAIL);
    map.put("name", NAME);
    map.put("provider", PROVIDER);
    map.put("providerUserId", PROVIDER_USER_ID);
    map.put("emailVerified", true);
    map.put("typ", "pending");
    map.put("iat", issuedAt);
    map.put("exp", issuedAt.plusSeconds(PENDING_SECONDS));
    return map;
  }

  /** {@link #validClaims} 를 고친 클레임 맵으로 서명한다. 값이 {@link #ABSENT} 면 그 클레임을 넣지 않는다. */
  public static String signedClaims(String secret, Map<String, Object> claims) {
    JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
    for (Map.Entry<String, Object> entry : claims.entrySet()) {
      Object value = entry.getValue();
      if (value == ABSENT) continue;
      switch (entry.getKey()) {
        case "iat" -> builder.issueTime(Date.from((Instant) value));
        case "exp" -> builder.expirationTime(Date.from((Instant) value));
        default -> builder.claim(entry.getKey(), value);
      }
    }
    try {
      // 클레임 JSON 의 짝 없는 서로게이트는 UTF-8 로 인코딩될 때 '?' 로 바뀌어 서버에 닿지 않는다(TASK-009 SC-20 의 테스트 결함과
      // 같은 부류). \\uXXXX 이스케이프로 바꿔 시나리오가 정한 입력이 그대로 서명되게 한다
      String json = escapeLoneSurrogates(builder.build().toString());
      JWSObject jws = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(json));
      jws.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
      return jws.serialize();
    } catch (JOSEException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static String escapeLoneSurrogates(String json) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < json.length(); i++) {
      char c = json.charAt(i);
      boolean paired =
          (Character.isHighSurrogate(c)
                  && i + 1 < json.length()
                  && Character.isLowSurrogate(json.charAt(i + 1)))
              || (Character.isLowSurrogate(c)
                  && i > 0
                  && Character.isHighSurrogate(json.charAt(i - 1)));
      if (Character.isSurrogate(c) && !paired) {
        out.append(String.format("\\u%04x", (int) c));
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  /** emailVerified 만 달리한 정상 토큰. */
  public static String valid(String secret, Instant issuedAt, boolean emailVerified) {
    Map<String, Object> claims = validClaims(issuedAt);
    claims.put("emailVerified", emailVerified);
    return signedClaims(secret, claims);
  }

  private static JWTClaimsSet claims(
      String email,
      String name,
      String provider,
      String providerUserId,
      String typ,
      Instant issuedAt,
      Instant expiresAt) {
    JWTClaimsSet.Builder builder =
        new JWTClaimsSet.Builder()
            .issueTime(Date.from(issuedAt))
            .expirationTime(Date.from(expiresAt));
    if (email != null) builder.claim("email", email);
    if (name != null) builder.claim("name", name);
    if (provider != null) builder.claim("provider", provider);
    if (providerUserId != null) builder.claim("providerUserId", providerUserId);
    if (typ != null) builder.claim("typ", typ);
    builder.claim("emailVerified", true);
    return builder.build();
  }

  /** 기본값(구글, new@example.com, typ=pending)으로 서명한 정상 토큰. exp 는 발급 + 5분. */
  public static String valid(String secret, Instant issuedAt) {
    return signed(
        secret,
        EMAIL,
        NAME,
        PROVIDER,
        PROVIDER_USER_ID,
        "pending",
        issuedAt,
        issuedAt.plusSeconds(PENDING_SECONDS));
  }

  public static String signed(
      String secret,
      String email,
      String name,
      String provider,
      String providerUserId,
      String typ,
      Instant issuedAt,
      Instant expiresAt) {
    try {
      SignedJWT jwt =
          new SignedJWT(
              new JWSHeader(JWSAlgorithm.HS256),
              claims(email, name, provider, providerUserId, typ, issuedAt, expiresAt));
      jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
      return jwt.serialize();
    } catch (JOSEException exception) {
      throw new IllegalStateException(exception);
    }
  }

  /** 서명 알고리즘이 none 인 토큰. */
  public static String unsigned(Instant issuedAt, Instant expiresAt) {
    return new PlainJWT(
            claims(EMAIL, NAME, PROVIDER, PROVIDER_USER_ID, "pending", issuedAt, expiresAt))
        .serialize();
  }

  /** 서명 부분만 지운 토큰(header.payload.). */
  public static String withoutSignature(String token) {
    String[] parts = token.split("\\.");
    return parts[0] + "." + parts[1] + ".";
  }

  /** payload 의 email 만 바꾸고 header·서명은 그대로 둔 변조 토큰. */
  public static String tamperEmail(String token, String to) {
    String[] parts = token.split("\\.");
    String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    String changed = payload.replace("\"email\":\"" + EMAIL + "\"", "\"email\":\"" + to + "\"");
    String encoded =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(changed.getBytes(StandardCharsets.UTF_8));
    return parts[0] + "." + encoded + "." + parts[2];
  }

  /** 리다이렉트 주소의 쿼리를 풀어 Map 으로. 같은 키가 두 번 나오면 마지막 값이 남는다. */
  public static Map<String, String> queryOf(String url) {
    Map<String, String> result = new LinkedHashMap<>();
    String rawQuery = URI.create(url).getRawQuery();
    if (rawQuery == null) return result;
    for (String pair : rawQuery.split("&")) {
      int index = pair.indexOf('=');
      String key = index < 0 ? pair : pair.substring(0, index);
      String value = index < 0 ? "" : pair.substring(index + 1);
      result.put(
          URLDecoder.decode(key, StandardCharsets.UTF_8),
          URLDecoder.decode(value, StandardCharsets.UTF_8));
    }
    return result;
  }
}
