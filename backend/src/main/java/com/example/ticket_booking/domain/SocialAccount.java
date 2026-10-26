package com.example.ticket_booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/**
 * V4__create_social_accounts.sql 의 NOT NULL/UNIQUE/길이 제약을 애노테이션으로도 표현한다. 테스트는 이 애노테이션에서 스키마를
 * 만들므로(ADR-0006) 마이그레이션과 어긋나지 않게 유지한다. (provider, provider_user_id) 는 제공자 계정 하나가 사용자 하나에만 연결되게 하는
 * 유일 제약이다.
 */
@Entity
@Table(
    name = "social_accounts",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_social_accounts_provider_user",
            columnNames = {"provider", "provider_user_id"}))
public class SocialAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 20)
  private SocialProvider provider;

  @Column(name = "provider_user_id", nullable = false, length = 255)
  private String providerUserId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected SocialAccount() {}

  public SocialAccount(Long userId, SocialProvider provider, String providerUserId) {
    this.userId = userId;
    this.provider = provider;
    this.providerUserId = providerUserId;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public SocialProvider getProvider() {
    return provider;
  }

  public String getProviderUserId() {
    return providerUserId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
