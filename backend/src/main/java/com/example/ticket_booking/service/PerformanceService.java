package com.example.ticket_booking.service;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.domain.PerformanceStatus;
import com.example.ticket_booking.domain.PerformanceStatusRules;
import com.example.ticket_booking.repository.PerformanceRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PerformanceService {

  private final PerformanceRepository performanceRepository;
  private final Clock clock;

  public PerformanceService(PerformanceRepository performanceRepository, Clock clock) {
    this.performanceRepository = performanceRepository;
    this.clock = clock;
  }

  public PerformanceListResponse getPerformances(PerformanceStatus status, int page, int size) {
    Instant now = clock.instant();

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by("startAt").ascending());
    Page<Performance> result =
        performanceRepository.findVisiblePerformances(status, now, pageRequest);

    List<PerformanceResponse> content =
        result.getContent().stream().map(performance -> toResponse(performance, now)).toList();

    return new PerformanceListResponse(
        content, result.getNumber(), result.getSize(), result.getTotalElements());
  }

  public PerformanceResponse getPerformance(Long id) {
    Performance performance =
        performanceRepository.findById(id).orElseThrow(() -> new PerformanceNotFoundException(id));
    return toResponse(performance, clock.instant());
  }

  private PerformanceResponse toResponse(Performance performance, Instant now) {
    PerformanceStatus status =
        PerformanceStatusRules.of(
            Boolean.TRUE.equals(performance.getCancelled()),
            now,
            performance.getOpenAt(),
            performance.getCloseAt(),
            performance.getAvailableSeats());
    return new PerformanceResponse(
        performance.getId(),
        performance.getTitle(),
        performance.getVenue(),
        performance.getStartAt(),
        performance.getOpenAt(),
        performance.getCloseAt(),
        performance.getTotalSeats(),
        performance.getAvailableSeats(),
        status);
  }
}
