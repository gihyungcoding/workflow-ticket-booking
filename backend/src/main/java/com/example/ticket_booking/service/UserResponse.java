package com.example.ticket_booking.service;

import com.example.ticket_booking.domain.UserRole;

public record UserResponse(Long id, String email, String name, UserRole role) {}
