package com.example.ticket_booking.repository;

import com.example.ticket_booking.domain.Performance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PerformanceRepository extends JpaRepository<Performance, Long>,
        JpaSpecificationExecutor<Performance> {
}
