package com.example.ticket_booking.config;

import com.example.ticket_booking.service.AccountMails;
import com.example.ticket_booking.service.MailGateway;
import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 계정 메일 설정(ADR-0031). {@code MAIL_TRANSPORT}(smtp | log, 앞뒤 공백·대소문자 무시)로 MailGateway 구현을 고른다 — 항상
 * 필수다(사용자 결정 2026-11-19). 비었거나 정의 밖이면, smtp 인데 SMTP 호스트({@code SPRING_MAIL_HOST} →
 * spring.mail.host)나 보내는 사람({@code MAIL_FROM})이 없으면 기동이 실패한다 — 「운영에서 메일이 조용히 안 나가는」 일을 막는다. 링크의 기준
 * 주소도 여기서 검증한다({@link FrontendBaseUrl}).
 */
@Configuration
public class MailConfig {

  @Bean
  public MailGateway mailGateway(Environment environment, ObjectProvider<JavaMailSender> senders) {
    String transport = environment.getProperty("MAIL_TRANSPORT");
    String normalized = transport == null ? "" : transport.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "log" -> new LoggingMailGateway();
      case "smtp" -> {
        requireText(environment, "spring.mail.host", "SPRING_MAIL_HOST");
        String from = requireText(environment, "MAIL_FROM", "MAIL_FROM");
        JavaMailSender sender = senders.getIfAvailable();
        if (sender == null) {
          throw new IllegalStateException("SPRING_MAIL_HOST 로 SMTP 발송기를 만들지 못했다");
        }
        yield new SmtpMailGateway(sender, from.trim());
      }
      default ->
          throw new IllegalStateException(
              "MAIL_TRANSPORT 는 smtp 또는 log 여야 한다(운영은 smtp): "
                  + (transport == null ? "미설정" : "\"" + transport + "\""));
    };
  }

  @Bean
  public AccountMails accountMails(Environment environment) {
    return new AccountMails(FrontendBaseUrl.from(environment));
  }

  private static String requireText(Environment environment, String key, String name) {
    String value = environment.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(name + " 이 필요하다 (MAIL_TRANSPORT=smtp)");
    }
    return value;
  }
}
