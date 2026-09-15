package com.example.ticket_booking.service;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.domain.PerformanceStatus;
import com.example.ticket_booking.domain.PerformanceStatusRules;
import com.example.ticket_booking.domain.Seat;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.repository.SeatRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerformanceService {

  private static final int MAX_SEATS = 5000;

  private final PerformanceRepository performanceRepository;
  private final SeatRepository seatRepository;
  private final Clock clock;

  public PerformanceService(
      PerformanceRepository performanceRepository, SeatRepository seatRepository, Clock clock) {
    this.performanceRepository = performanceRepository;
    this.seatRepository = seatRepository;
    this.clock = clock;
  }

  public PerformanceListResponse getPerformances(PerformanceStatus status, int page, int size) {
    Instant now = clock.instant();

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by("startAt").ascending());
    Page<Performance> result =
        performanceRepository.findVisiblePerformances(status, now, pageRequest);

    List<PerformanceResponse> content =
        result.getContent().stream().map(performance -> toResponse(performance, now)).toList();

    return new PerformanceListResponse(
        content, result.getNumber(), result.getSize(), result.getTotalElements());
  }

  public PerformanceResponse getPerformance(Long id) {
    Performance performance =
        performanceRepository.findById(id).orElseThrow(() -> new PerformanceNotFoundException(id));
    return toResponse(performance, clock.instant());
  }

  @Transactional
  public PerformanceResponse registerPerformance(
      String title,
      String venue,
      Instant startAt,
      Instant openAt,
      Instant closeAt,
      List<SectionSpec> sections) {
    if (sections.isEmpty()) {
      throw new EmptySectionsException();
    }
    validateTimeOrder(openAt, closeAt, startAt);

    int totalSeats = 0;
    for (SectionSpec section : sections) {
      totalSeats += rowCount(section) * section.seatsPerRow();
    }
    if (totalSeats > MAX_SEATS) {
      throw new SeatLimitExceededException(totalSeats);
    }
    for (int i = 0; i < sections.size(); i++) {
      for (int j = i + 1; j < sections.size(); j++) {
        if (rowRangesOverlap(sections.get(i), sections.get(j))) {
          throw new DuplicateSeatRangeException();
        }
      }
    }

    Performance performance =
        performanceRepository.save(
            new Performance(title, venue, startAt, openAt, closeAt, totalSeats, totalSeats, false));
    seatRepository.saveAll(generateSeats(performance.getId(), sections));

    return toResponse(performance, clock.instant());
  }

  @Transactional
  public PerformanceResponse updatePerformance(
      Long id, String title, String venue, Instant startAt, Instant openAt, Instant closeAt) {
    Performance performance =
        performanceRepository.findById(id).orElseThrow(() -> new PerformanceNotFoundException(id));
    Instant now = clock.instant();
    if (!now.isBefore(performance.getOpenAt())) {
      throw new RegistrationAlreadyOpenException(id);
    }
    validateTimeOrder(openAt, closeAt, startAt);

    performance.updateSchedule(title, venue, startAt, openAt, closeAt);
    performanceRepository.save(performance);
    return toResponse(performance, now);
  }

  @Transactional
  public PerformanceResponse cancelPerformance(Long id) {
    Performance performance =
        performanceRepository.findById(id).orElseThrow(() -> new PerformanceNotFoundException(id));
    performance.cancel();
    performanceRepository.save(performance);
    return toResponse(performance, clock.instant());
  }

  private void validateTimeOrder(Instant openAt, Instant closeAt, Instant startAt) {
    if (openAt.isAfter(closeAt) || closeAt.isAfter(startAt)) {
      throw new InvalidTimeOrderException();
    }
  }

  private int rowCount(SectionSpec section) {
    return section.rowEnd().charAt(0) - section.rowStart().charAt(0) + 1;
  }

  private boolean rowRangesOverlap(SectionSpec a, SectionSpec b) {
    char aStart = a.rowStart().charAt(0);
    char aEnd = a.rowEnd().charAt(0);
    char bStart = b.rowStart().charAt(0);
    char bEnd = b.rowEnd().charAt(0);
    return aStart <= bEnd && bStart <= aEnd;
  }

  private List<Seat> generateSeats(Long performanceId, List<SectionSpec> sections) {
    List<Seat> seats = new ArrayList<>();
    for (SectionSpec section : sections) {
      char rowStart = section.rowStart().charAt(0);
      char rowEnd = section.rowEnd().charAt(0);
      for (char row = rowStart; row <= rowEnd; row++) {
        for (int number = 1; number <= section.seatsPerRow(); number++) {
          String seatLabel = section.grade() + "-" + row + number;
          seats.add(
              new Seat(
                  performanceId,
                  section.grade(),
                  String.valueOf(row),
                  number,
                  seatLabel,
                  section.price()));
        }
      }
    }
    return seats;
  }

  private PerformanceResponse toResponse(Performance performance, Instant now) {
    PerformanceStatus status =
        PerformanceStatusRules.of(
            Boolean.TRUE.equals(performance.getCancelled()),
            now,
            performance.getOpenAt(),
            performance.getCloseAt(),
            performance.getAvailableSeats());
    return new PerformanceResponse(
        performance.getId(),
        performance.getTitle(),
        performance.getVenue(),
        performance.getStartAt(),
        performance.getOpenAt(),
        performance.getCloseAt(),
        performance.getTotalSeats(),
        performance.getAvailableSeats(),
        status);
  }
}
