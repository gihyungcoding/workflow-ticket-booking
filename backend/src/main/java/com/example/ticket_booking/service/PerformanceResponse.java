package com.example.ticket_booking.service;

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
