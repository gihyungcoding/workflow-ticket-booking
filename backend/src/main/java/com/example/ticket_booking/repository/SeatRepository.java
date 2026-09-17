package com.example.ticket_booking.repository;

import com.example.ticket_booking.domain.Seat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {

  @Query(
      "SELECT s.grade AS grade, s.price AS price, COUNT(s) AS seatCount "
          + "FROM Seat s WHERE s.performance.id = :performanceId "
          + "GROUP BY s.grade, s.price "
          + "ORDER BY s.price DESC, s.grade ASC")
  List<SeatSectionCount> findSectionCounts(@Param("performanceId") Long performanceId);
}
