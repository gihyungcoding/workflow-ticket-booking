package com.example.ticket_booking.repository;

import com.example.ticket_booking.domain.PerformanceZone;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceZoneRepository extends JpaRepository<PerformanceZone, Long> {

  List<PerformanceZone> findByPerformanceIdOrderByStageOrder(Long performanceId);
}
