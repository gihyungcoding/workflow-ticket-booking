package com.example.ticket_booking.api.dto;

import java.util.List;

public record PerformanceListResponse(
        List<PerformanceResponse> content,
        int page,
        int size,
        long totalElements) {
}
