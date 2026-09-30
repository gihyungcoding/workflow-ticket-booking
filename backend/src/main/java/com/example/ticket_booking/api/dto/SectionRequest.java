package com.example.ticket_booking.api.dto;

import java.math.BigDecimal;

public record SectionRequest(
    String grade, BigDecimal price, String rowStart, String rowEnd, BigDecimal seatsPerRow) {}
