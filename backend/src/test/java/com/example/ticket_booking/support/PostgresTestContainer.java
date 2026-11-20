package com.example.ticket_booking.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 스프링 컨텍스트를 운영과 같은 엔진(PostgreSQL)에서 Flyway 스키마로 띄울 때 쓰는 컨테이너(ADR-0006, TASK-027). 테스트 JVM 하나에 하나만
 * 띄우고 재사용한다. 나머지 테스트는 지금처럼 H2 + ddl-auto 다.
 */
public final class PostgresTestContainer {

  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

  static {
    POSTGRES.start();
  }

  private PostgresTestContainer() {}

  /** 데이터소스를 컨테이너로 바꾸고 Flyway 를 켠다 — 스키마는 마이그레이션이 정본이라 ddl-auto 는 끈다. */
  public static void register(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
  }
}
