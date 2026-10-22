package com.example.ticket_booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/**
 * V3__create_users.sql 의 NOT NULL/UNIQUE/길이 제약을 애노테이션으로도 표현한다. 테스트는 이 애노테이션에서 스키마를 만들므로(ADR-0006)
 * 마이그레이션과 어긋나지 않게 유지한다. email 은 저장 전에 소문자로 정규화된 값이다(AuthService).
 */
@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "email", nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private UserRole role;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected User() {}

  public User(String email, String passwordHash, String name, UserRole role) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.name = name;
    this.role = role;
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getName() {
    return name;
  }

  public UserRole getRole() {
    return role;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
