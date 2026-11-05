package com.example.ticket_booking.api.dto;

import jakarta.validation.constraints.NotBlank;
import tools.jackson.databind.annotation.JsonDeserialize;

/** pendingToken 의 형식(서명·만료·용도·클레임)은 서비스가 판정한다 — 여기서는 비어 있지 않은 문자열인지만 본다. */
public record PendingSignupRequest(
    @NotBlank @JsonDeserialize(using = StrictStringDeserializer.class) String pendingToken) {}
