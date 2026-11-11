package com.example.ticket_booking.service;

import com.fasterxml.jackson.annotation.JsonInclude;

/** area·stageOrder 는 구역 위치(TASK-019). 설명이 없는 구역은 areaNote 키가 생략된다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SectionSummaryResponse(
    String grade,
    Integer price,
    Long seatCount,
    String area,
    String areaNote,
    Integer stageOrder) {}
