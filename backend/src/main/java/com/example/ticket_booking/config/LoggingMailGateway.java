package com.example.ticket_booking.config;

import com.example.ticket_booking.service.MailGateway;
import com.example.ticket_booking.service.MailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 개발·테스트용 — 메일을 보내지 않고 받는 사람·제목·본문을 INFO 로그로 남긴다({@code MAIL_TRANSPORT=log}). 본문의 링크(토큰 원문)가 로그에
 * 남으므로 운영에서는 쓰지 않는다(운영은 smtp — 배포 체크리스트).
 */
public class LoggingMailGateway implements MailGateway {

  private static final Logger log = LoggerFactory.getLogger(LoggingMailGateway.class);

  @Override
  public void send(MailMessage message) {
    log.info("메일(log 전송) to={} subject={}\n{}", message.to(), message.subject(), message.body());
  }
}
