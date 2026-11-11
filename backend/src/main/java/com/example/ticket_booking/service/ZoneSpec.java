package com.example.ticket_booking.service;

import java.util.List;

/**
 * 등록 요청의 구역 묶음 하나. Controller가 api.dto.ZoneRequest 에서 변환해 넘긴다 — Service는 api 패키지를 import하지 않는다
 * (architecture.md §3, ARCH-002).
 */
public record ZoneSpec(String area, String note, List<String> grades) {}
