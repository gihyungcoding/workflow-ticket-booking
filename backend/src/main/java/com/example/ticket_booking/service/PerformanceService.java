package com.example.ticket_booking.service;

import com.example.ticket_booking.api.dto.PerformanceListResponse;
import com.example.ticket_booking.api.dto.PerformanceResponse;
import com.example.ticket_booking.repository.PerformanceRepository;

import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final Clock clock;

    public PerformanceService(PerformanceRepository performanceRepository, Clock clock) {
        this.performanceRepository = performanceRepository;
        this.clock = clock;
    }

    public PerformanceListResponse getPerformances(PerformanceStatus status, int page, int size) {
        throw new UnsupportedOperationException();
    }

    public PerformanceResponse getPerformance(Long id) {
        throw new UnsupportedOperationException();
    }
}
