package com.example.ticket_booking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdatePerformanceRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 200) String venue,
    @NotNull Instant startAt,
    @NotNull Instant openAt,
    @NotNull Instant closeAt) {}
