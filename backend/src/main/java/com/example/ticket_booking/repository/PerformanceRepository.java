package com.example.ticket_booking.repository;

import com.example.ticket_booking.domain.Performance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {
}
