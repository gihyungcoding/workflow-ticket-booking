package com.example.ticket_booking.service;

import java.util.List;

public record PerformanceListResponse(
        List<PerformanceResponse> content,
        int page,
        int size,
        long totalElements) {
}
