package com.example.ticket_booking.api.dto;

import com.example.ticket_booking.domain.UserRole;

/** 검증 애노테이션(@NotBlank·@NotNull)은 Phase 3 에서 붙인다. */
public record CompleteSocialSignUpRequest(String pendingToken, UserRole role) {}
