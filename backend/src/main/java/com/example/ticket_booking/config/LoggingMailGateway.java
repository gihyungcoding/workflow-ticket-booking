package com.example.ticket_booking.config;

import com.example.ticket_booking.service.MailGateway;
import com.example.ticket_booking.service.MailMessage;

/** 개발·테스트용 — 메일을 보내지 않고 받는 사람·제목·본문을 로그로 남긴다({@code MAIL_TRANSPORT=log}). */
public class LoggingMailGateway implements MailGateway {

  @Override
  public void send(MailMessage message) {
    throw new UnsupportedOperationException();
  }
}
