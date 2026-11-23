package com.example.ticket_booking.support;

import com.example.ticket_booking.TicketBookingApplication;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

/**
 * 설정 파일(테스트 application.properties)을 읽지 않고 실제 애플리케이션을 띄운다(TASK-027 머리말 「기동」). 테스트 설정 파일의 값이 기본
 * 프로퍼티({@code SpringApplicationBuilder.properties})보다 우선해 「키 없음」 행을 만들 수 없기 때문이다(프로브 2026-11-19).
 * 값은 가장 우선하는 프로퍼티 소스 하나로 넣는다. 기본 값은 {@link #defaults()} 이고, 넘긴 맵의 값이 {@code null} 이면 그 키를 뺀다(「키
 * 없음」).
 */
public final class BareBoot {

  private static final AtomicInteger DB_SEQUENCE = new AtomicInteger();

  private BareBoot() {}

  /**
   * H2(PostgreSQL 모드)·JWT 테스트 키·{@code MAIL_TRANSPORT=log}·{@code
   * FRONTEND_BASE_URL=https://app.example}.
   */
  public static Map<String, Object> defaults() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put(
        "spring.datasource.url",
        "jdbc:h2:mem:bare-boot-"
            + DB_SEQUENCE.incrementAndGet()
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
    values.put("spring.datasource.driver-class-name", "org.h2.Driver");
    values.put("spring.jpa.hibernate.ddl-auto", "create-drop");
    values.put("spring.flyway.enabled", "false");
    values.put("JWT_SECRET", "test-only-jwt-secret-0123456789abcdef");
    values.put("MAIL_TRANSPORT", "log");
    values.put("FRONTEND_BASE_URL", "https://app.example");
    return values;
  }

  /** 기본 값 위에 {@code overrides} 를 얹어 서블릿(임의 포트)으로 띄운다. 실패하면 예외를 그대로 던진다. */
  public static ConfigurableApplicationContext run(Map<String, Object> overrides) {
    Map<String, Object> values = defaults();
    overrides.forEach(
        (key, value) -> {
          if (value == null) {
            values.remove(key);
          } else {
            values.put(key, value);
          }
        });
    // 값은 명령행 인자가 아니라 가장 우선하는 프로퍼티 소스로 넣는다 — 공백만·앞뒤 공백·역슬래시·개행(PEM) 값이 인자 파싱을 거치지 않게
    // (AppleProviderConfigTest 의 기존 이유). 설정 파일을 끄는 위치 값과 포트만 인자로 준다.
    Map<String, Object> source = new LinkedHashMap<>(values);
    return new SpringApplicationBuilder(TicketBookingApplication.class)
        .web(WebApplicationType.SERVLET)
        .initializers(
            context -> {
              MutablePropertySources sources = context.getEnvironment().getPropertySources();
              sources.addFirst(new MapPropertySource("bare-boot", source));
              // 「키 없음」은 OS 환경변수에서도 없다 — 개발자 머신의 FRONTEND_BASE_URL·MAIL_TRANSPORT 가 새어 들지 않게(RC-4)
              sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
            })
        // 기본 위치(classpath:/application.properties 등)를 존재하지 않는 파일 하나로 바꿔 설정 파일을 읽지 않는다
        .run(
            "--spring.config.location=optional:classpath:/bare-boot-no-such-file.properties",
            "--server.port=0");
  }

  /** 기동 결과 — 성공이면 컨텍스트, 실패면 예외. */
  public record Started(ConfigurableApplicationContext context, Throwable failure) {
    public boolean ok() {
      return failure == null;
    }

    public int port() {
      return Integer.parseInt(context.getEnvironment().getProperty("local.server.port"));
    }

    /** 예외 체인의 메시지를 " | " 로 잇는다. */
    public String failureMessages() {
      List<String> messages = new ArrayList<>();
      for (Throwable t = failure; t != null; t = t.getCause()) {
        messages.add(String.valueOf(t.getMessage()));
      }
      return String.join(" | ", messages);
    }

    public void close() {
      if (context != null) {
        context.close();
      }
    }
  }

  /** {@link #run} 을 부르고 예외를 {@link Started} 로 받는다. */
  public static Started start(Map<String, Object> overrides) {
    try {
      return new Started(run(overrides), null);
    } catch (Throwable thrown) {
      return new Started(null, thrown);
    }
  }
}
