package com.example.ticket_booking.service;

import com.example.ticket_booking.domain.PerformanceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * sections는 단일 공연 상세 조회(getPerformance)에서만 채워진다 — 목록/등록/수정/취소 응답은 null로 두어 JSON에서 키 자체가
 * 생략된다(TASK-008, acceptance_criteria 3).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PerformanceResponse(
    Long id,
    String title,
    String venue,
    Instant startAt,
    Instant openAt,
    Instant closeAt,
    Integer totalSeats,
    Integer availableSeats,
    PerformanceStatus status,
    List<SectionSummaryResponse> sections) {}
