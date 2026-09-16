package com.example.ticket_booking.api.dto;

import java.time.Instant;

public record UpdatePerformanceRequest(
    String title, String venue, Instant startAt, Instant openAt, Instant closeAt) {}
