package com.example.ticket_booking.service;

import java.math.BigDecimal;

/**
 * 등록 요청의 구역 하나. Controller가 api.dto.SectionRequest 에서 변환해 넘긴다 — Service는 api 패키지를 import하지 않는다
 * (architecture.md §3, ARCH-002).
 */
public record SectionSpec(
    String grade, BigDecimal price, String rowStart, String rowEnd, BigDecimal seatsPerRow) {}
