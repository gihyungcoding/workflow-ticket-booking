package com.example.ticket_booking.service;

import com.example.ticket_booking.domain.SocialProvider;
import java.time.Instant;

/** 소셜 가입 대기(pendingToken)의 내용 — 역할 선택 화면이 어느 계정으로 가입 중인지와 남은 시간을 보여 준다. */
public record PendingSignupResponse(
    SocialProvider provider, String email, String name, Instant expiresAt) {}
