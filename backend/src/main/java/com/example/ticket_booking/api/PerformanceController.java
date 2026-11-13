package com.example.ticket_booking.api;

import com.example.ticket_booking.api.dto.RegisterPerformanceRequest;
import com.example.ticket_booking.api.dto.SectionRequest;
import com.example.ticket_booking.api.dto.UpdatePerformanceRequest;
import com.example.ticket_booking.domain.PerformanceStatus;
import com.example.ticket_booking.service.OrganizerPerformanceResponse;
import com.example.ticket_booking.service.PerformanceListResponse;
import com.example.ticket_booking.service.PerformanceResponse;
import com.example.ticket_booking.service.PerformanceService;
import com.example.ticket_booking.service.SectionSpec;
import com.example.ticket_booking.service.ZoneSpec;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

  /** 홈 머리글 후보. 리터럴 경로라 /{id} 보다 먼저 매칭된다. */
  @GetMapping("/next-opening")
  public ResponseEntity<PerformanceResponse> nextOpening() {
    return performanceService
        .getNextOpening()
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @GetMapping("/{id}")
  public PerformanceResponse detail(@PathVariable Long id) {
    return performanceService.getPerformance(id);
  }

  /**
   * 쓰기는 보안 체인이 ROLE_ORGANIZER 를 확인한 뒤에만 여기 온다(SecurityConfig). principal 은 토큰의 사용자 id 다 — 등록하면 그
   * 사람이 소유자다.
   */
  @PostMapping
  public ResponseEntity<OrganizerPerformanceResponse> register(
      @Valid @RequestBody RegisterPerformanceRequest request, Authentication authentication) {
    List<SectionSpec> sections =
        request.sections() == null
            ? null
            : request.sections().stream().map(this::toSectionSpec).toList();
    List<ZoneSpec> zones =
        request.zones() == null
            ? List.of()
            : request.zones().stream()
                .map(zone -> new ZoneSpec(zone.area(), zone.note(), zone.grades()))
                .toList();
    OrganizerPerformanceResponse response =
        performanceService.registerPerformance(
            (Long) authentication.getPrincipal(),
            request.title(),
            request.venue(),
            request.startAt(),
            request.openAt(),
            request.closeAt(),
            sections,
            request.posterUrl(),
            zones);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PutMapping("/{id}")
  public OrganizerPerformanceResponse update(
      @PathVariable Long id,
      @Valid @RequestBody UpdatePerformanceRequest request,
      Authentication authentication) {
    return performanceService.updatePerformance(
        (Long) authentication.getPrincipal(),
        id,
        request.title(),
        request.venue(),
        request.startAt(),
        request.openAt(),
        request.closeAt());
  }

  @PostMapping("/{id}/cancel")
  public OrganizerPerformanceResponse cancel(@PathVariable Long id, Authentication authentication) {
    return performanceService.cancelPerformance((Long) authentication.getPrincipal(), id);
  }

  @PostMapping("/{id}/publish")
  public OrganizerPerformanceResponse publish(
      @PathVariable Long id, Authentication authentication) {
    return performanceService.publish((Long) authentication.getPrincipal(), id);
  }

  @PostMapping("/{id}/unpublish")
  public OrganizerPerformanceResponse unpublish(
      @PathVariable Long id, Authentication authentication) {
    return performanceService.unpublish((Long) authentication.getPrincipal(), id);
  }

  private SectionSpec toSectionSpec(SectionRequest request) {
    if (request == null) {
      return null;
    }
    return new SectionSpec(
        request.grade(),
        request.price(),
        request.rowStart(),
        request.rowEnd(),
        request.seatsPerRow());
  }
}
