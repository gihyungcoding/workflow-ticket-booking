package com.example.ticket_booking.repository;

import com.example.ticket_booking.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {}
