package com.example.ticket_booking.api;

import com.example.ticket_booking.domain.PerformanceFilter;
import com.example.ticket_booking.service.OrganizerPerformanceListResponse;
import com.example.ticket_booking.service.OrganizerPerformanceResponse;
import com.example.ticket_booking.service.PerformanceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 주최자 전용 조회. 보안 체인이 ROLE_ORGANIZER 를 확인한 뒤에만 여기 온다(SecurityConfig). principal 은 토큰의 사용자 id 다. */
@RestController
@RequestMapping("/api/organizer/performances")
public class OrganizerPerformanceController {

  private final PerformanceService performanceService;

  public OrganizerPerformanceController(PerformanceService performanceService) {
    this.performanceService = performanceService;
  }

  @GetMapping
  public OrganizerPerformanceListResponse list(
      @RequestParam(defaultValue = "ALL") PerformanceFilter filter,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      Authentication authentication) {
    return performanceService.getMyPerformances(
        (Long) authentication.getPrincipal(), filter, page, size);
  }

  @GetMapping("/{id}")
  public OrganizerPerformanceResponse detail(@PathVariable Long id, Authentication authentication) {
    return performanceService.getMyPerformance((Long) authentication.getPrincipal(), id);
  }
}
