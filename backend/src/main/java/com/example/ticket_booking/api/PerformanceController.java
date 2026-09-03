package com.example.ticket_booking.api;

import com.example.ticket_booking.api.dto.PerformanceListResponse;
import com.example.ticket_booking.api.dto.PerformanceResponse;
import com.example.ticket_booking.service.PerformanceService;
import com.example.ticket_booking.service.PerformanceStatus;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/performances")
public class PerformanceController {

    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @GetMapping
    public PerformanceListResponse list(
            @RequestParam(required = false) PerformanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return performanceService.getPerformances(status, page, size);
    }

    @GetMapping("/{id}")
    public PerformanceResponse detail(@PathVariable Long id) {
        return performanceService.getPerformance(id);
    }
}
