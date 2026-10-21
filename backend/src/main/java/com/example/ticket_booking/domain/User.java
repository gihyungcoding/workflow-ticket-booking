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

/** TASK-009 Phase 2b 스켈레톤 — 필드와 구조적 애노테이션만 있다. 제약(NOT NULL·길이·UNIQUE)은 Phase 3 에서 추가한다. */
@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "email")
  private String email;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "name")
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "role")
  private UserRole role;

  @Column(name = "created_at")
  private Instant createdAt;

  protected User() {}

  public Long getId() {
    throw new UnsupportedOperationException();
  }

  public String getEmail() {
    throw new UnsupportedOperationException();
  }

  public String getPasswordHash() {
    throw new UnsupportedOperationException();
  }

  public String getName() {
    throw new UnsupportedOperationException();
  }

  public UserRole getRole() {
    throw new UnsupportedOperationException();
  }

  public Instant getCreatedAt() {
    throw new UnsupportedOperationException();
  }
}
