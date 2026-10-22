package com.example.ticket_booking.api.dto;

/**
 * 짝 없는 서로게이트(U+D800~U+DFFF 단독)를 거부하는 정규식 — PostgreSQL 은 이런 값을 다른 문자로 바꿔 저장해 입력과 저장값이 달라진다. 짝이 맞는
 * 쌍(이모지 등)은 한 코드포인트라 걸리지 않는다.
 */
final class NoLoneSurrogate {

  static final String REGEX = "^[^\\p{Cs}]*$";
  static final String MESSAGE = "짝 없는 서로게이트를 포함할 수 없습니다";

  private NoLoneSurrogate() {}
}
