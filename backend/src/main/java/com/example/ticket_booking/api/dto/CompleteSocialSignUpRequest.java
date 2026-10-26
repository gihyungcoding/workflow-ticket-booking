package com.example.ticket_booking.api.dto;

import com.example.ticket_booking.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

/** pendingToken 의 형식(서명·만료·용도)은 서비스가 판정한다 — 여기서는 비어 있지 않은 문자열인지만 본다. */
public record CompleteSocialSignUpRequest(
    @NotBlank @JsonDeserialize(using = StrictStringDeserializer.class) String pendingToken,
    @NotNull UserRole role) {}
