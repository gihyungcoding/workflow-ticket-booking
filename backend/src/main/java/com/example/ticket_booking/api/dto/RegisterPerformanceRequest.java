package com.example.ticket_booking.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * sections 에는 @NotNull 을 붙이지 않는다 — 누락·빈 배열 판정은 PerformanceService 가 각각
 * INVALID_REQUEST·EMPTY_SECTIONS 로 구분해 응답하므로, 여기서 잡으면 그 구분이 사라진다. @Valid 는 배열 원소의 필드 형태만 검증하며 null
 * 원소는 건너뛴다.
 *
 * <p>posterUrl·zones 는 선택이다. posterUrl 의 형식·길이는 INVALID_POSTER_URL 로 따로 응답해야 해서 애노테이션이 아니라
 * PerformanceService 가 판정한다 — 여기서는 문자열이 아닌 JSON 타입만 거부한다. zones 의 null 원소는 @NotNull 로 거부한다.
 */
public record RegisterPerformanceRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 200) String venue,
    @NotNull Instant startAt,
    @NotNull Instant openAt,
    @NotNull Instant closeAt,
    @Valid List<SectionRequest> sections,
    @JsonDeserialize(using = StrictStringDeserializer.class) String posterUrl,
    List<@NotNull @Valid ZoneRequest> zones) {}
