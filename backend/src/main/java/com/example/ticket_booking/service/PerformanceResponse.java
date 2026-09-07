package com.example.ticket_booking.service;

import com.example.ticket_booking.domain.PerformanceStatus;
import java.time.Instant;

public record PerformanceResponse(
    Long id,
    String title,
    String venue,
    Instant startAt,
    Instant openAt,
    Instant closeAt,
    Integer totalSeats,
    Integer availableSeats,
    PerformanceStatus status) {}
