package com.example.ticket_booking.config;

import com.example.ticket_booking.service.AccountMails;
import com.example.ticket_booking.service.MailGateway;
import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * 계정 메일 설정(ADR-0031). {@code MAIL_TRANSPORT}(smtp | log, 앞뒤 공백·대소문자 무시)로 MailGateway 구현을 고른다 — 항상
 * 필수다(사용자 결정 2026-11-19). 비었거나 정의 밖이면, smtp 인데 SMTP 호스트({@code SPRING_MAIL_HOST} →
 * spring.mail.host)나 보내는 사람({@code MAIL_FROM})이 없으면, SMTP 타임아웃 값이 틀리면 기동이 실패한다 — 「운영에서 메일이 조용히 안
 * 나가는」 일을 막는다. SMTP 타임아웃이 없으면 10초를 건다({@link #applyTimeouts}). 링크의 기준 주소도 여기서 검증한다({@link
 * FrontendBaseUrl}).
 */
@Configuration
public class MailConfig {

  private static final String[] SMTP_TIMEOUT_KEYS = {
    "connectiontimeout", "timeout", "writetimeout"
  };
  private static final long DEFAULT_SMTP_TIMEOUT_MILLIS = 10_000;

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
        if (!(sender instanceof JavaMailSenderImpl impl)) {
          throw new IllegalStateException("SPRING_MAIL_HOST 로 SMTP 발송기를 만들지 못했다");
        }
        applyTimeouts(environment, impl);
        yield new SmtpMailGateway(impl, from.trim());
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

  /**
   * SMTP 연결·읽기·쓰기 타임아웃(ms) — JavaMail 기본(과 0)은 무한 대기라 메일 서비스가 응답하지 않으면 발송 스레드가 영영 멈춘다(자체 리뷰 RC-2).
   * 키마다 없거나 빈 값·공백만이면 10초, 값이 있으면 앞뒤 공백을 정리한 양의 정수여야 한다 — 0·음수·정수 아님은 기동 실패(메시지에 환경변수 이름).
   */
  static void applyTimeouts(Environment environment, JavaMailSenderImpl sender) {
    for (String key : SMTP_TIMEOUT_KEYS) {
      String property = "spring.mail.properties.mail.smtp." + key;
      String raw = environment.getProperty(property);
      String value;
      if (raw == null || raw.isBlank()) {
        value = String.valueOf(DEFAULT_SMTP_TIMEOUT_MILLIS);
      } else {
        String trimmed = raw.trim();
        long millis;
        try {
          millis = Long.parseLong(trimmed);
        } catch (NumberFormatException exception) {
          throw invalidTimeout(key, raw);
        }
        if (millis <= 0 || millis > Integer.MAX_VALUE) {
          throw invalidTimeout(key, raw);
        }
        value = String.valueOf(millis);
      }
      sender.getJavaMailProperties().setProperty("mail.smtp." + key, value);
    }
  }

  private static IllegalStateException invalidTimeout(String key, String raw) {
    return new IllegalStateException(
        "SPRING_MAIL_PROPERTIES_MAIL_SMTP_"
            + key.toUpperCase(Locale.ROOT)
            + " 은 양의 정수(ms)여야 한다: \""
            + raw
            + "\"");
  }

  private static String requireText(Environment environment, String key, String name) {
    String value = environment.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(name + " 이 필요하다 (MAIL_TRANSPORT=smtp)");
    }
    return value;
  }
}
