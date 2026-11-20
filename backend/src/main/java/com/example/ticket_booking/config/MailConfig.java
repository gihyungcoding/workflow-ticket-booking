package com.example.ticket_booking.config;

import com.example.ticket_booking.service.AccountMails;
import com.example.ticket_booking.service.MailGateway;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * 계정 메일 설정(ADR-0031). {@code MAIL_TRANSPORT}(smtp | log, 앞뒤 공백·대소문자 무시)로 MailGateway 구현을 고른다 — 항상
 * 필수다(사용자 결정 2026-11-19). 아래 경우 기동이 실패한다 — 「운영에서 메일이 조용히 안 나가는」 일을 막는다.
 *
 * <ul>
 *   <li>MAIL_TRANSPORT 가 비었거나 정의 밖
 *   <li>JNDI 메일 세션 이름({@code SPRING_MAIL_JNDI_NAME})이 있다 — 전송 방식과 무관. 세션이 먼저 만들어지면 타임아웃을 걸 수 없다
 *   <li>smtp 인데 SMTP 호스트({@code SPRING_MAIL_HOST})·보내는 사람({@code MAIL_FROM})이 없다
 *   <li>smtp 인데 프로토콜({@code SPRING_MAIL_PROTOCOL})이 smtp·smtps 가 아니다(없거나 빈 값이면 smtp)
 *   <li>smtp 인데 그 프로토콜의 타임아웃 값이 ASCII 양의 정수(int 범위)가 아니다
 * </ul>
 *
 * <p>SMTP 연결·읽기·쓰기 타임아웃이 없으면 10초를 건다({@link #applyTimeouts}). log 이면 프로토콜·타임아웃을 읽지 않는다. 링크의 기준 주소도
 * 여기서 검증한다({@link FrontendBaseUrl}).
 */
@Configuration
public class MailConfig {

  private static final String[] TIMEOUT_KEYS = {"connectiontimeout", "timeout", "writetimeout"};
  private static final long DEFAULT_TIMEOUT_MILLIS = 10_000;
  private static final Pattern ASCII_DIGITS = Pattern.compile("[0-9]+");

  @Bean
  public MailGateway mailGateway(Environment environment, ObjectProvider<JavaMailSender> senders) {
    if (environment.containsProperty("spring.mail.jndi-name")) {
      throw new IllegalStateException(
          "SPRING_MAIL_JNDI_NAME(JNDI 메일 세션)은 지원하지 않는다 — SMTP 타임아웃을 걸 수 없다. SPRING_MAIL_HOST 로 설정한다");
    }
    String transport = environment.getProperty("MAIL_TRANSPORT");
    String normalized = transport == null ? "" : transport.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "log" -> new LoggingMailGateway();
      case "smtp" -> {
        String host = singleToken(environment, "spring.mail.host", "SPRING_MAIL_HOST");
        String from = singleToken(environment, "MAIL_FROM", "MAIL_FROM");
        JavaMailSender sender = senders.getIfAvailable();
        if (!(sender instanceof JavaMailSenderImpl impl)) {
          throw new IllegalStateException("SPRING_MAIL_HOST 로 SMTP 발송기를 만들지 못했다");
        }
        // 검증한 값과 발송에 쓰는 값이 같아야 한다 — Boot 가 넣은 호스트 원문(앞뒤 공백·개행 포함)을 정리한 값으로 바꾼다(Phase 4 RC-2)
        impl.setHost(host);
        String protocol = protocol(environment);
        impl.setProtocol(protocol);
        applyTimeouts(environment, impl, protocol);
        yield new SmtpMailGateway(impl, from);
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

  /** 없거나 빈 값·공백이면 smtp, 아니면 앞뒤 공백·대소문자를 정리해 smtp | smtps 만 받는다. */
  static String protocol(Environment environment) {
    String raw = environment.getProperty("spring.mail.protocol");
    if (raw == null || raw.isBlank()) {
      return "smtp";
    }
    String normalized = raw.trim().toLowerCase(Locale.ROOT);
    if (!normalized.equals("smtp") && !normalized.equals("smtps")) {
      throw new IllegalStateException(
          "SPRING_MAIL_PROTOCOL 은 smtp 또는 smtps 여야 한다: \"" + raw + "\"");
    }
    return normalized;
  }

  /**
   * SMTP 연결·읽기·쓰기 타임아웃(ms) — JavaMail 기본(과 0)은 무한 대기라 메일 서비스가 응답하지 않으면 발송 스레드가 영영 멈춘다(자체 리뷰 RC-2).
   * JavaMail 은 프로토콜 접두사 키만 읽으므로({@code mail.smtps.timeout}) 고른 프로토콜의 키만 보고 그 키에 건다 — 다른 프로토콜의 키는
   * 무시한다. 키마다 없거나 빈 값·공백만이면 10초, 값이 있으면 앞뒤 공백을 정리한 ASCII 숫자이고 1 이상 int 최대 이하여야 한다 — 아니면 기동 실패(메시지에
   * 환경변수 이름).
   */
  static void applyTimeouts(Environment environment, JavaMailSenderImpl sender, String protocol) {
    for (String key : TIMEOUT_KEYS) {
      String raw = environment.getProperty("spring.mail.properties.mail." + protocol + "." + key);
      String value;
      if (raw == null || raw.isBlank()) {
        value = String.valueOf(DEFAULT_TIMEOUT_MILLIS);
      } else {
        String trimmed = raw.trim();
        if (!ASCII_DIGITS.matcher(trimmed).matches()) {
          throw invalidTimeout(protocol, key, raw);
        }
        // 앞에 붙은 0 은 값에 영향이 없다 — 떼고 나서 자릿수로 long 넘침을 먼저 막는다
        String digits = trimmed.replaceFirst("^0+(?=.)", "");
        if (digits.length() > 10) {
          throw invalidTimeout(protocol, key, raw);
        }
        long millis = Long.parseLong(digits);
        if (millis <= 0 || millis > Integer.MAX_VALUE) {
          throw invalidTimeout(protocol, key, raw);
        }
        value = String.valueOf(millis);
      }
      sender.getJavaMailProperties().setProperty("mail." + protocol + "." + key, value);
    }
  }

  private static IllegalStateException invalidTimeout(String protocol, String key, String raw) {
    return new IllegalStateException(
        "SPRING_MAIL_PROPERTIES_MAIL_"
            + protocol.toUpperCase(Locale.ROOT)
            + "_"
            + key.toUpperCase(Locale.ROOT)
            + " 은 1 이상 2147483647 이하의 정수(ms)여야 한다: \""
            + raw
            + "\"");
  }

  /**
   * 앞뒤 공백·개행을 정리한 한 덩어리 값 — 비었으면 「필요하다」, 정리한 뒤 안에 공백·탭·CR·LF 가 있으면 「한 덩어리여야 한다」로 기동 실패(메시지에 환경변수
   * 이름). 호스트 안의 공백은 연결 실패가, 보내는 사람 안의 개행은 메일 헤더 주입이 된다.
   */
  static String singleToken(Environment environment, String key, String name) {
    String trimmed = requireText(environment, key, name).trim();
    if (trimmed.isEmpty()) {
      throw new IllegalStateException(name + " 이 필요하다 (MAIL_TRANSPORT=smtp)");
    }
    if (trimmed.chars().anyMatch(Character::isWhitespace)) {
      throw new IllegalStateException(name + " 안에 공백·개행이 있다(앞뒤 공백은 정리한다)");
    }
    return trimmed;
  }

  private static String requireText(Environment environment, String key, String name) {
    String value = environment.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(name + " 이 필요하다 (MAIL_TRANSPORT=smtp)");
    }
    return value;
  }
}
