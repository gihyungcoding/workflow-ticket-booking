package com.example.ticket_booking.config;

import com.example.ticket_booking.domain.UserRole;

/** 검증을 마친 accessToken 이 담는 값 — 보안 체인은 role 로 권한을 정하고, principal 은 userId 다. */
public record AccessClaims(Long userId, UserRole role) {}
