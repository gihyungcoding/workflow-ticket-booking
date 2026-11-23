package com.example.ticket_booking.service;

/**
 * 메일 발송 경계(ADR-0031). 서비스는 이 인터페이스만 안다 — 운영은 SMTP, 개발·테스트는 로그 구현을 {@code MAIL_TRANSPORT} 로 고른다.
 * 실패하면 예외를 던진다. 예외를 삼키는 것은 호출하는 쪽(커밋 뒤 비동기 발송)의 몫이다.
 */
public interface MailGateway {

  void send(MailMessage message);
}
