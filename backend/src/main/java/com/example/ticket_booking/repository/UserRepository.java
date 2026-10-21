package com.example.ticket_booking.repository;

import com.example.ticket_booking.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}
