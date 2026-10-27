package com.example.ticket_booking.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * 애플 토큰 교환에 쓰는 Client Secret 을 만든다. 애플은 고정 비밀번호가 아니라 개발자 포털에서 받은 {@code .p8} 키(EC P-256)로 서명한 짧은
 * 수명의 JWT(ES256)를 요구한다. 토큰 교환 요청마다 새로 만든다. 시각은 주입된 Clock 으로만 얻는다(ADR-0005).
 */
public class AppleClientSecretGenerator {

  static final String AUDIENCE = "https://appleid.apple.com";
  static final long TTL_SECONDS = 300;

  private final String teamId;
  private final String clientId;
  private final String keyId;
  private final ECPrivateKey privateKey;
  private final Clock clock;

  public AppleClientSecretGenerator(
      String teamId, String clientId, String keyId, ECPrivateKey privateKey, Clock clock) {
    this.teamId = teamId;
    this.clientId = clientId;
    this.keyId = keyId;
    this.privateKey = privateKey;
    this.clock = clock;
  }

  /** 토큰 교환 요청마다 새로 만든 Client Secret(compact JWS). */
  public String generate() {
    Instant issuedAt = clock.instant();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer(teamId)
            .subject(clientId)
            .audience(AUDIENCE)
            .issueTime(Date.from(issuedAt))
            .expirationTime(Date.from(issuedAt.plusSeconds(TTL_SECONDS)))
            .build();
    try {
      SignedJWT jwt =
          new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).build(), claims);
      jwt.sign(new ECDSASigner(privateKey));
      return jwt.serialize();
    } catch (JOSEException exception) {
      throw new IllegalStateException("애플 Client Secret 을 서명할 수 없다", exception);
    }
  }

  /**
   * APPLE_PRIVATE_KEY 환경변수 원문을 EC P-256 개인 키로 읽는다. PKCS#8 DER 의 base64 본문을 받는다 — {@code
   * -----BEGIN/END-----} 줄, 줄바꿈 대신 쓴 두 글자 {@code \n}, 앞뒤 공백은 있어도 된다. 그 밖(다른 곡선·RSA·SEC1·공개 키·깨진
   * 본문)은 본문을 PKCS#8 개인 키로 읽지 못해 IllegalStateException — 메시지에 APPLE_PRIVATE_KEY 가 있고 키 본문은 싣지 않는다(원인
   * 예외도 잇지 않는다).
   */
  static ECPrivateKey parsePrivateKey(String raw) {
    String body =
        raw.strip()
            .replace("\\n", "\n")
            .lines()
            .map(String::strip)
            .filter(line -> !line.startsWith("-----"))
            .collect(Collectors.joining());
    try {
      byte[] der = Base64.getDecoder().decode(body);
      var key = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
      if (key instanceof ECPrivateKey ecKey && isP256(ecKey.getParams())) {
        return ecKey;
      }
    } catch (IllegalArgumentException | GeneralSecurityException exception) {
      // 아래에서 원인 없이 같은 오류로 바꾼다
    }
    throw invalidKey();
  }

  private static boolean isP256(ECParameterSpec parameters) throws GeneralSecurityException {
    AlgorithmParameters algorithm = AlgorithmParameters.getInstance("EC");
    algorithm.init(new ECGenParameterSpec("secp256r1"));
    ECParameterSpec p256 = algorithm.getParameterSpec(ECParameterSpec.class);
    return p256.getOrder().equals(parameters.getOrder())
        && p256.getCurve().equals(parameters.getCurve());
  }

  private static IllegalStateException invalidKey() {
    return new IllegalStateException("APPLE_PRIVATE_KEY 가 올바르지 않다 (PKCS#8 형식의 EC P-256 개인 키여야 한다)");
  }
}
