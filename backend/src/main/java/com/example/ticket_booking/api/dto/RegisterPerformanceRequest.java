package com.example.ticket_booking.api.dto;

import java.time.Instant;
import java.util.List;

public record RegisterPerformanceRequest(
    String title,
    String venue,
    Instant startAt,
    Instant openAt,
    Instant closeAt,
    List<SectionRequest> sections) {}
