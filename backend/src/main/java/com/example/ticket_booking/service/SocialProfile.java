package com.example.ticket_booking.service;

import com.example.ticket_booking.domain.SocialProvider;

/** 제공자 응답을 정규화한 값. email 은 users 와 같은 정규화(앞뒤 공백 제거·소문자) 뒤의 값이다. */
public record SocialProfile(
    SocialProvider provider,
    String providerUserId,
    String email,
    String name,
    boolean emailVerified) {}
