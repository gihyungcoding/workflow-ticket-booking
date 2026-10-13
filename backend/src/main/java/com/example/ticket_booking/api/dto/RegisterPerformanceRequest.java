package com.example.ticket_booking.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * sections 에는 @NotNull 을 붙이지 않는다 — 누락·빈 배열 판정은 PerformanceService 가 각각
 * INVALID_REQUEST·EMPTY_SECTIONS 로 구분해 응답하므로, 여기서 잡으면 그 구분이 사라진다. @Valid 는 배열 원소의 필드 형태만 검증하며 null
 * 원소는 건너뛴다.
 */
public record RegisterPerformanceRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 200) String venue,
    @NotNull Instant startAt,
    @NotNull Instant openAt,
    @NotNull Instant closeAt,
    @Valid List<SectionRequest> sections) {}
