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
 * Red 단계 컴파일용 스켈레톤 — 스키마 제약(NOT NULL·UNIQUE(provider, provider_user_id)·길이)은 Phase 3 에서 추가한다. 접근자
 * 본문은 던지기만 한다.
 */
@Entity
@Table(name = "social_accounts")
public class SocialAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id")
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider")
  private SocialProvider provider;

  @Column(name = "provider_user_id")
  private String providerUserId;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;

  protected SocialAccount() {}

  public SocialAccount(Long userId, SocialProvider provider, String providerUserId) {
    this.userId = userId;
    this.provider = provider;
    this.providerUserId = providerUserId;
  }

  public Long getId() {
    throw new UnsupportedOperationException();
  }

  public Long getUserId() {
    throw new UnsupportedOperationException();
  }

  public SocialProvider getProvider() {
    throw new UnsupportedOperationException();
  }

  public String getProviderUserId() {
    throw new UnsupportedOperationException();
  }

  public Instant getCreatedAt() {
    throw new UnsupportedOperationException();
  }
}
