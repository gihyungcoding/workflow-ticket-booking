package com.example.ticket_booking.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 필드 형태 제약만 애노테이션으로 선언한다 (ADR-0010). price/seatsPerRow 의 범위·정수성은 @DecimalMax·@Digits 로도 표현할 수
 * 있지만, @Digits 구현의 precision()-scale() 계산이 극단적 지수 표기에서 int 오버플로로 무력화될 수 있어 PerformanceService 가
 * signum·compareTo 를 isIntegral() 보다 먼저 거치는 순서로 직접 판정한다. rowStart 가 rowEnd 보다 앞인지는 두 필드를 함께 봐야 하는
 * 도메인 규칙이라 애노테이션으로 표현할 수 없다.
 */
public record SectionRequest(
    @NotBlank @Size(max = 20) String grade,
    @NotNull BigDecimal price,
    @NotBlank @Pattern(regexp = "^[A-Z]$") String rowStart,
    @NotBlank @Pattern(regexp = "^[A-Z]$") String rowEnd,
    @NotNull BigDecimal seatsPerRow) {}
