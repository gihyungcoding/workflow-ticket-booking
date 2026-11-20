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

/**
 * 일회용 이메일 링크 토큰(ADR-0032, V9). 원문은 없고 SHA-256 해시만 있다. 사용자와는 id 로만 잇는다 — JPA 연관을 두지 않아 사용자 삭제는 DB 의
 * ON DELETE CASCADE 가 처리한다. 소모(used_at)는 리포지토리의 조건부 UPDATE 로만 바꾼다.
 */
@Entity
@Table(name = "email_token")
public class EmailToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "purpose", nullable = false, length = 20)
  private EmailTokenPurpose purpose;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected EmailToken() {}

  public EmailToken(Long userId, EmailTokenPurpose purpose, String tokenHash, Instant issuedAt) {
    this.userId = userId;
    this.purpose = purpose;
    this.tokenHash = tokenHash;
    this.expiresAt = issuedAt.plus(purpose.ttl());
    this.createdAt = issuedAt;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public EmailTokenPurpose getPurpose() {
    return purpose;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }
}
