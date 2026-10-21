package com.example.ticket_booking.service;

public record LoginResponse(
    String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {}
