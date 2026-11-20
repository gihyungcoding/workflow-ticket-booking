package com.example.ticket_booking.config;

import com.example.ticket_booking.service.MailGateway;
import com.example.ticket_booking.service.MailMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 운영용 — Spring Mail 로 SMTP 텍스트 메일을 보낸다({@code MAIL_TRANSPORT=smtp}). 실패는 Spring 의 MailException 으로
 * 던진다.
 */
public class SmtpMailGateway implements MailGateway {

  private final JavaMailSender sender;
  private final String from;

  public SmtpMailGateway(JavaMailSender sender, String from) {
    this.sender = sender;
    this.from = from;
  }

  @Override
  public void send(MailMessage message) {
    SimpleMailMessage mail = new SimpleMailMessage();
    mail.setFrom(from);
    mail.setTo(message.to());
    mail.setSubject(message.subject());
    mail.setText(message.body());
    sender.send(mail);
  }
}
