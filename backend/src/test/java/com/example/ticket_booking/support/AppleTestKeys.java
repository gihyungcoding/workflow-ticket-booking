package com.example.ticket_booking.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;

/**
 * 애플 시나리오(TASK-012)가 쓰는 키와 Client Secret 단언. 키는 구현과 무관하게 JCA 로 직접 만든다 — 구현이 어떤 형태의 키를 받아들이는지가 시나리오
 * 대상이므로 테스트가 형태별 PEM 을 손으로 만든다.
 */
public final class AppleTestKeys {

  public static final String TEAM_ID = "TEAM123456";
  public static final String CLIENT_ID = "com.example.tickets.web";
  public static final String KEY_ID = "KEY1234567";
  public static final String AUDIENCE = "https://appleid.apple.com";
  public static final long TTL_SECONDS = 300;

  /** 설정에 넣는 서명 키. 공개 키로 Client Secret 서명을 검증한다. */
  public static final KeyPair EC = ecPair("secp256r1");

  /** 서명 검증이 실패해야 하는 다른 키 쌍. */
  public static final KeyPair OTHER_EC = ecPair("secp256r1");

  private AppleTestKeys() {}

  private static KeyPair ecPair(String curve) {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
      generator.initialize(new ECGenParameterSpec(curve));
      return generator.generateKeyPair();
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException(exception);
    }
  }

  public static ECPrivateKey privateKey() {
    return (ECPrivateKey) EC.getPrivate();
  }

  // ---------- PEM 형태 ----------

  private static String pem(String label, byte[] der) {
    String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
    return "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----";
  }

  /** `.p8` 와 같은 형태 — PKCS#8 PEM, 실제 개행. */
  public static String pkcs8Pem(PrivateKey key) {
    return pem("PRIVATE KEY", key.getEncoded());
  }

  public static String validPem() {
    return pkcs8Pem(EC.getPrivate());
  }

  /** 환경변수에 한 줄로 넣느라 개행이 두 글자 `\n` 이 된 PEM. */
  public static String escapedNewlinesPem() {
    return validPem().replace("\n", "\\n");
  }

  /** 헤더·푸터가 없는 PKCS#8 DER 의 base64 본문. */
  public static String bareBase64() {
    return Base64.getEncoder().encodeToString(EC.getPrivate().getEncoded());
  }

  public static String publicKeyPem() {
    return pem("PUBLIC KEY", EC.getPublic().getEncoded());
  }

  public static String headerOnlyPem() {
    return "-----BEGIN PRIVATE KEY-----\n-----END PRIVATE KEY-----";
  }

  public static String rsaPkcs8Pem() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return pkcs8Pem(generator.generateKeyPair().getPrivate());
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException(exception);
    }
  }

  public static String p384Pem() {
    return pkcs8Pem(ecPair("secp384r1").getPrivate());
  }

  /** SEC1(RFC 5915) 형식의 `BEGIN EC PRIVATE KEY` — PKCS#8 이 아니다. */
  public static String sec1Pem() {
    ECPrivateKey privateKey = privateKey();
    ECPublicKey publicKey = (ECPublicKey) EC.getPublic();
    byte[] d = fixed(privateKey.getS(), 32);
    byte[] point = new byte[65];
    point[0] = 4;
    System.arraycopy(fixed(publicKey.getW().getAffineX(), 32), 0, point, 1, 32);
    System.arraycopy(fixed(publicKey.getW().getAffineY(), 32), 0, point, 33, 32);
    byte[] oidPrime256v1 = {
      0x06, 0x08, 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x03, 0x01, 0x07
    };
    byte[] bitString = concat(new byte[] {0}, point);
    byte[] sequence =
        concat(
            der(0x02, new byte[] {1}),
            der(0x04, d),
            der(0xA0, oidPrime256v1),
            der(0xA1, der(0x03, bitString)));
    return pem("EC PRIVATE KEY", der(0x30, sequence));
  }

  private static byte[] fixed(BigInteger value, int length) {
    byte[] raw = value.toByteArray();
    byte[] out = new byte[length];
    int copy = Math.min(raw.length, length);
    System.arraycopy(raw, raw.length - copy, out, length - copy, copy);
    return out;
  }

  private static byte[] concat(byte[]... parts) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (byte[] part : parts) {
      out.writeBytes(part);
    }
    return out.toByteArray();
  }

  private static byte[] der(int tag, byte[] content) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(tag);
    if (content.length < 128) {
      out.write(content.length);
    } else {
      out.write(0x81);
      out.write(content.length);
    }
    out.writeBytes(content);
    return out.toByteArray();
  }

  // ---------- Client Secret 단언 ----------

  /** SC-01 규격: ES256 헤더, 클레임, 64바이트 JOSE 서명, 공개 키로 검증됨 / 다른 키로는 실패. */
  public static void assertClientSecret(
      SoftAssertions soft, String label, String jwt, long expectedIat) {
    soft.assertThat(jwt).as(label + " — Client Secret").isNotBlank();
    if (jwt == null || jwt.isBlank()) {
      return;
    }
    soft.assertThat(jwt.split("\\.", -1)).as(label + " — 점으로 구분된 세 부분").hasSize(3);
    try {
      SignedJWT parsed = SignedJWT.parse(jwt);
      soft.assertThat(parsed.getHeader().getAlgorithm())
          .as(label + " — alg")
          .isEqualTo(JWSAlgorithm.ES256);
      soft.assertThat(parsed.getHeader().getKeyID()).as(label + " — kid").isEqualTo(KEY_ID);
      Map<String, Object> claims = AuthTestHelper.payloadOf(jwt);
      soft.assertThat(claims.get("iss")).as(label + " — iss").isEqualTo(TEAM_ID);
      soft.assertThat(claims.get("sub")).as(label + " — sub").isEqualTo(CLIENT_ID);
      soft.assertThat(claims.get("aud")).as(label + " — aud 는 문자열").isEqualTo(AUDIENCE);
      soft.assertThat(number(claims.get("iat"))).as(label + " — iat").isEqualTo(expectedIat);
      soft.assertThat(number(claims.get("exp")))
          .as(label + " — exp")
          .isEqualTo(expectedIat + TTL_SECONDS);
      soft.assertThat(parsed.getSignature().decode()).as(label + " — JOSE 형식 서명 64바이트").hasSize(64);
      soft.assertThat(parsed.verify(new ECDSAVerifier((ECPublicKey) EC.getPublic())))
          .as(label + " — 짝이 되는 공개 키로 검증")
          .isTrue();
      soft.assertThat(parsed.verify(new ECDSAVerifier((ECPublicKey) OTHER_EC.getPublic())))
          .as(label + " — 다른 키 쌍의 공개 키로는 검증 실패")
          .isFalse();
    } catch (ParseException | JOSEException | RuntimeException exception) {
      soft.fail(label + " — Client Secret 을 해석할 수 없다: " + exception);
    }
  }

  private static Long number(Object value) {
    return value instanceof Number number ? number.longValue() : null;
  }

  /** 키 본문의 일부를 오류 메시지에 싣지 않았는지 볼 때 쓰는 조각. */
  public static String bodyFragment(String pem) {
    String body = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
    return body.length() > 24 ? body.substring(8, 24) : body;
  }
}
