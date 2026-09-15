package com.example.ticket_booking.api;

import com.example.ticket_booking.api.dto.RegisterPerformanceRequest;
import com.example.ticket_booking.api.dto.SectionRequest;
import com.example.ticket_booking.api.dto.UpdatePerformanceRequest;
import com.example.ticket_booking.domain.PerformanceStatus;
import com.example.ticket_booking.service.PerformanceListResponse;
import com.example.ticket_booking.service.PerformanceResponse;
import com.example.ticket_booking.service.PerformanceService;
import com.example.ticket_booking.service.SectionSpec;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    return performanceService.getPerformances(status, page, Math.min(size, 100));
  }

  @GetMapping("/{id}")
  public PerformanceResponse detail(@PathVariable Long id) {
    return performanceService.getPerformance(id);
  }

  @PostMapping
  public ResponseEntity<PerformanceResponse> register(
      @RequestBody RegisterPerformanceRequest request) {
    List<SectionSpec> sections = request.sections().stream().map(this::toSectionSpec).toList();
    PerformanceResponse response =
        performanceService.registerPerformance(
            request.title(),
            request.venue(),
            request.startAt(),
            request.openAt(),
            request.closeAt(),
            sections);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/{id}")
  public PerformanceResponse update(
      @PathVariable Long id, @RequestBody UpdatePerformanceRequest request) {
    return performanceService.updatePerformance(
        id,
        request.title(),
        request.venue(),
        request.startAt(),
        request.openAt(),
        request.closeAt());
  }

  @PostMapping("/{id}/cancel")
  public PerformanceResponse cancel(@PathVariable Long id) {
    return performanceService.cancelPerformance(id);
  }

  private SectionSpec toSectionSpec(SectionRequest request) {
    return new SectionSpec(
        request.grade(),
        request.price(),
        request.rowStart(),
        request.rowEnd(),
        request.seatsPerRow());
  }
}
