package com.example.ticket_booking.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** TASK-009 인증 테스트 공용 도구 — 요청 전송, 응답 해석, 토큰 직접 제작(구현과 독립적으로 Nimbus 로 만든다). */
public final class AuthTestHelper {

  public static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

  private AuthTestHelper() {}

  public static String json(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

  public static Map<String, Object> signUpBody(
      String email, String password, String passwordConfirm, String name, String role) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("email", email);
    body.put("password", password);
    body.put("passwordConfirm", passwordConfirm);
    body.put("name", name);
    body.put("role", role);
    return body;
  }

  public static Map<String, Object> loginBody(String email, String password) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("email", email);
    body.put("password", password);
    return body;
  }

  public static MvcResult postJson(MockMvc mockMvc, String path, String rawBody) throws Exception {
    return mockMvc
        .perform(post(path).contentType(MediaType.APPLICATION_JSON).content(rawBody))
        .andReturn();
  }

  /** 본문 없이 JSON 컨텐츠 타입으로만 보낸다. */
  public static MvcResult postEmpty(MockMvc mockMvc, String path) throws Exception {
    return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)).andReturn();
  }

  public static MvcResult signUp(
      MockMvc mockMvc, String email, String password, String name, String role) throws Exception {
    return postJson(
        mockMvc, "/api/auth/signup", json(signUpBody(email, password, password, name, role)));
  }

  public static MvcResult login(MockMvc mockMvc, String email, String password) throws Exception {
    return postJson(mockMvc, "/api/auth/login", json(loginBody(email, password)));
  }

  /** authorization 이 null 이면 헤더를 아예 보내지 않는다. */
  public static MvcResult getWithAuthorization(MockMvc mockMvc, String path, String authorization)
      throws Exception {
    MockHttpServletRequestBuilder request = get(path);
    if (authorization != null) {
      request = request.header("Authorization", authorization);
    }
    return mockMvc.perform(request).andReturn();
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> bodyAsMap(MvcResult result) throws Exception {
    return OBJECT_MAPPER.readValue(result.getResponse().getContentAsString(), Map.class);
  }

  public static String code(MvcResult result) throws Exception {
    return String.valueOf(bodyAsMap(result).get("code"));
  }

  /** 로그인 응답에서 accessToken 만 꺼낸다. 로그인이 실패하면 null. */
  public static String accessTokenOf(MvcResult loginResult) throws Exception {
    if (loginResult.getResponse().getStatus() != 200) {
      return null;
    }
    return (String) bodyAsMap(loginResult).get("accessToken");
  }

  /** JWT 의 payload 를 Map 으로 푼다(서명 검증 없음 — 클레임 확인용). */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> payloadOf(String token) {
    String[] parts = token.split("\\.");
    String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    return OBJECT_MAPPER.readValue(payload, Map.class);
  }

  /** payload 의 role 만 바꾸고 header·서명은 그대로 둔 변조 토큰. */
  public static String tamperRole(String token, String from, String to) {
    String[] parts = token.split("\\.");
    String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    String changed = payload.replace("\"role\":\"" + from + "\"", "\"role\":\"" + to + "\"");
    String encoded =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(changed.getBytes(StandardCharsets.UTF_8));
    return parts[0] + "." + encoded + "." + parts[2];
  }

  /** HS256 으로 서명한 토큰. secret 은 UTF-8 32바이트 이상이어야 한다. */
  public static String signedToken(
      String secret,
      String subject,
      String email,
      String role,
      String typ,
      Instant issuedAt,
      Instant expiresAt) {
    try {
      JWTClaimsSet claims = claims(subject, email, role, typ, issuedAt, expiresAt);
      SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
      return jwt.serialize();
    } catch (JOSEException exception) {
      throw new IllegalStateException(exception);
    }
  }

  /** 서명 알고리즘이 none 인 토큰. */
  public static String unsignedToken(
      String subject, String email, String role, String typ, Instant issuedAt, Instant expiresAt) {
    return new PlainJWT(claims(subject, email, role, typ, issuedAt, expiresAt)).serialize();
  }

  private static JWTClaimsSet claims(
      String subject, String email, String role, String typ, Instant issuedAt, Instant expiresAt) {
    return new JWTClaimsSet.Builder()
        .subject(subject)
        .claim("email", email)
        .claim("role", role)
        .claim("typ", typ)
        .issueTime(Date.from(issuedAt))
        .expirationTime(Date.from(expiresAt))
        .build();
  }
}
