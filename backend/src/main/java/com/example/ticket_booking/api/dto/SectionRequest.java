package com.example.ticket_booking.api.dto;

public record SectionRequest(
    String grade, Integer price, String rowStart, String rowEnd, Integer seatsPerRow) {}
