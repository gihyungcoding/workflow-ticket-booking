package com.example.ticket_booking.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

/**
 * 제공자(애플·카카오·네이버·구글)의 토큰·사용자 정보·JWKS 엔드포인트를 흉내 내는 로컬 서버. JDK 내장 {@link HttpServer} 라 새 의존성이 없다. 받은
 * 요청을 기록하고, 경로별로 다음 응답을 정한다.
 */
public final class FakeProviderServer {

  public static final String KEY_ID = "test-key";
  public static final String ISSUER = "https://appleid.apple.com";

  /** 응답. body 가 null 이면 본문 없이 보낸다. */
  public record Response(int status, String contentType, String body) {}

  /** 받은 요청. 헤더 이름은 소문자, 폼은 application/x-www-form-urlencoded 일 때만 푼다. */
  public record Recorded(
      String method, String path, Map<String, String> headers, Map<String, String> form) {}

  private final HttpServer server;
  private final RSAKey signingKey;
  private final RSAKey otherKey;
  private final Map<String, Response> responses = new ConcurrentHashMap<>();
  private final List<Recorded> recorded = new CopyOnWriteArrayList<>();

  public FakeProviderServer() {
    try {
      signingKey =
          new RSAKeyGenerator(2048)
              .keyID(KEY_ID)
              .keyUse(KeyUse.SIGNATURE)
              .algorithm(JWSAlgorithm.RS256)
              .generate();
      otherKey = new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
      server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    } catch (IOException | JOSEException exception) {
      throw new IllegalStateException(exception);
    }
    server.createContext("/", this::handle);
    server.start();
  }

  public void stop() {
    server.stop(0);
  }

  public String url(String path) {
    return "http://localhost:" + server.getAddress().getPort() + path;
  }

  /** 등록된 토큰·사용자 정보·JWKS 주소를 이 서버로 바꾼다. */
  public ClientRegistration redirect(ClientRegistration registration) {
    String id = registration.getRegistrationId();
    ClientRegistration.Builder builder =
        ClientRegistration.withClientRegistration(registration).tokenUri(url("/" + id + "/token"));
    if ("apple".equals(id)) {
      builder.jwkSetUri(url("/apple/jwks"));
    } else {
      builder.userInfoUri(url("/" + id + "/userinfo"));
    }
    return builder.build();
  }

  public void respond(String path, int status, String contentType, String body) {
    responses.put(path, new Response(status, contentType, body));
  }

  public void respondJson(String path, String body) {
    respond(path, 200, "application/json", body);
  }

  /** 응답 설정과 받은 요청 기록을 지운다. */
  public void reset() {
    responses.clear();
    recorded.clear();
  }

  public void clearRequests() {
    recorded.clear();
  }

  public List<Recorded> requests(String path) {
    return recorded.stream().filter(request -> request.path().equals(path)).toList();
  }

  // ---------- id_token ----------

  /** JWKS 의 키로 서명한 id_token. iat·exp 는 Instant 로 넘긴다. */
  public String idToken(Map<String, Object> claims) {
    return sign(claims, signingKey);
  }

  /** JWKS 에 없는 키(같은 kid)로 서명한 id_token. */
  public String idTokenSignedByOtherKey(Map<String, Object> claims) {
    return sign(claims, otherKey);
  }

  private static String sign(Map<String, Object> claims, RSAKey key) {
    JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
    for (Map.Entry<String, Object> entry : claims.entrySet()) {
      Object value = entry.getValue();
      switch (entry.getKey()) {
        case "iat" -> builder.issueTime(Date.from((Instant) value));
        case "exp" -> builder.expirationTime(Date.from((Instant) value));
        case "aud" -> builder.audience((String) value);
        default -> builder.claim(entry.getKey(), value);
      }
    }
    try {
      SignedJWT jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), builder.build());
      jwt.sign(new RSASSASigner(key));
      return jwt.serialize();
    } catch (JOSEException exception) {
      throw new IllegalStateException(exception);
    }
  }

  // ---------- 서버 ----------

  private void handle(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    Map<String, String> headers = new LinkedHashMap<>();
    exchange
        .getRequestHeaders()
        .forEach((name, values) -> headers.put(name.toLowerCase(), values.get(0)));
    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    Map<String, String> form = new LinkedHashMap<>();
    String type = headers.getOrDefault("content-type", "");
    if (type.startsWith("application/x-www-form-urlencoded") && !body.isEmpty()) {
      for (String pair : body.split("&")) {
        int index = pair.indexOf('=');
        String key = index < 0 ? pair : pair.substring(0, index);
        String value = index < 0 ? "" : pair.substring(index + 1);
        form.put(
            URLDecoder.decode(key, StandardCharsets.UTF_8),
            URLDecoder.decode(value, StandardCharsets.UTF_8));
      }
    }
    recorded.add(new Recorded(exchange.getRequestMethod(), path, headers, form));

    Response response;
    if ("/apple/jwks".equals(path)) {
      response =
          new Response(200, "application/json", new JWKSet(signingKey.toPublicJWK()).toString());
    } else {
      response = responses.getOrDefault(path, new Response(404, "text/plain", "not found"));
    }
    exchange.getResponseHeaders().add("Content-Type", response.contentType());
    byte[] bytes =
        response.body() == null ? new byte[0] : response.body().getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(response.status(), bytes.length == 0 ? -1 : bytes.length);
    if (bytes.length > 0) {
      exchange.getResponseBody().write(bytes);
    }
    exchange.close();
  }
}
