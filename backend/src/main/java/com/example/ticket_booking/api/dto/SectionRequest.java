package com.example.ticket_booking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 필드 형태 제약만 애노테이션으로 선언한다 (ADR-0010). price 가 0 이상인지, seatsPerRow 가 1 이상인지, rowStart 가 rowEnd 보다
 * 앞인지는 PerformanceService 가 판정한다 — 여러 필드나 좌석 총수를 함께 봐야 하는 도메인 규칙이다.
 */
public record SectionRequest(
    @NotBlank @Size(max = 20) String grade,
    @NotNull Integer price,
    @NotBlank @Pattern(regexp = "^[A-Z]$") String rowStart,
    @NotBlank @Pattern(regexp = "^[A-Z]$") String rowEnd,
    @NotNull Integer seatsPerRow) {}
