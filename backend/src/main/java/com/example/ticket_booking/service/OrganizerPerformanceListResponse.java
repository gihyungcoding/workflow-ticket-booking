package com.example.ticket_booking.service;

import java.util.List;

public record OrganizerPerformanceListResponse(
    List<OrganizerPerformanceResponse> items, int page, int size, long totalElements) {}
