package com.example.ticket_booking.service;

/** 소셜 로그인 판정 결과 — accessToken 이 있으면 로그인, pendingToken 이 있으면 가입 대기다. 둘 중 하나만 채워진다. */
public record SocialLoginResult(String accessToken, String pendingToken) {}
