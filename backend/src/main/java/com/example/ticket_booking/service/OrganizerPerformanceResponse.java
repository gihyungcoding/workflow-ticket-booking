package com.example.ticket_booking.service;

import com.example.ticket_booking.domain.PerformanceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * 주최자 응답. 관객 응답(PerformanceResponse)과 타입을 나눠 주최자 전용 필드(published·editable·visibilityChangeable)가 관객
 * 응답에 새지 않게 한다. sections 는 단건 조회에서만 채워지고 posterUrl 은 없으면 키가 생략된다. 소유자 id 는 어느 응답에도 싣지
 * 않는다(ADR-0029).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrganizerPerformanceResponse(
    Long id,
    String title,
    String venue,
    Instant startAt,
    Instant openAt,
    Instant closeAt,
    Integer totalSeats,
    Integer availableSeats,
    PerformanceStatus status,
    String posterUrl,
    boolean published,
    boolean editable,
    boolean visibilityChangeable,
    List<SectionSummaryResponse> sections) {}
