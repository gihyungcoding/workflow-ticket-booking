package com.example.ticket_booking.config;

import com.example.ticket_booking.service.MailGateway;
import com.example.ticket_booking.service.MailMessage;

/** 운영용 — Spring Mail 로 SMTP 텍스트 메일을 보낸다({@code MAIL_TRANSPORT=smtp}). */
public class SmtpMailGateway implements MailGateway {

  @Override
  public void send(MailMessage message) {
    throw new UnsupportedOperationException();
  }
}
