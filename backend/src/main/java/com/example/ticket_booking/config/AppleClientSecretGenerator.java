package com.example.ticket_booking.config;

import java.security.interfaces.ECPrivateKey;
import java.time.Clock;

/** 애플 토큰 교환에 쓰는 Client Secret(ES256 서명 JWT)을 만든다. Red 단계 스켈레톤 — 구조만 있고 동작은 Phase 3 에서 만든다. */
public class AppleClientSecretGenerator {

  public AppleClientSecretGenerator(
      String teamId, String clientId, String keyId, ECPrivateKey privateKey, Clock clock) {
    throw new UnsupportedOperationException();
  }

  /** 토큰 교환 요청마다 새로 만든 Client Secret(compact JWS). */
  public String generate() {
    throw new UnsupportedOperationException();
  }
}
