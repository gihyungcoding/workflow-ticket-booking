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
  private static final int MAX_GRADE_LENGTH = 20;
  private static final int MAX_TITLE_LENGTH = 200;
  private static final int MAX_VENUE_LENGTH = 200;

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
    validateRequired(title, venue, startAt, openAt, closeAt);
    if (sections == null) {
      throw new InvalidRequestException("sections가 필요합니다");
    }
    if (sections.isEmpty()) {
      throw new EmptySectionsException();
    }
    for (SectionSpec section : sections) {
      validateSection(section);
    }
    validateTimeOrder(openAt, closeAt, startAt);

    long totalSeatsLong = 0;
    for (SectionSpec section : sections) {
      totalSeatsLong += (long) rowCount(section) * section.seatsPerRow();
    }
    if (totalSeatsLong > MAX_SEATS) {
      throw new SeatLimitExceededException(totalSeatsLong);
    }
    int totalSeats = (int) totalSeatsLong;
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
    seatRepository.saveAll(generateSeats(performance, sections));

    return toResponse(performance, clock.instant());
  }

  @Transactional
  public PerformanceResponse updatePerformance(
      Long id, String title, String venue, Instant startAt, Instant openAt, Instant closeAt) {
    validateRequired(title, venue, startAt, openAt, closeAt);
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

  private void validateRequired(
      String title, String venue, Instant startAt, Instant openAt, Instant closeAt) {
    if (isBlank(title) || isBlank(venue) || startAt == null || openAt == null || closeAt == null) {
      throw new InvalidRequestException("title/venue/startAt/openAt/closeAt는 필수입니다");
    }
    if (title.length() > MAX_TITLE_LENGTH || venue.length() > MAX_VENUE_LENGTH) {
      throw new InvalidRequestException("title/venue는 " + MAX_TITLE_LENGTH + "자를 넘을 수 없습니다");
    }
  }

  private void validateSection(SectionSpec section) {
    if (section == null) {
      throw new InvalidSectionException("구역 정보는 비어 있을 수 없습니다");
    }
    if (isBlank(section.grade()) || section.grade().length() > MAX_GRADE_LENGTH) {
      throw new InvalidSectionException("grade는 1~" + MAX_GRADE_LENGTH + "자여야 합니다");
    }
    if (section.price() == null || section.price() < 0) {
      throw new InvalidSectionException("price는 0 이상이어야 합니다");
    }
    if (!isValidRow(section.rowStart()) || !isValidRow(section.rowEnd())) {
      throw new InvalidSectionException("rowStart/rowEnd는 A~Z 단일 대문자여야 합니다");
    }
    if (section.rowStart().charAt(0) > section.rowEnd().charAt(0)) {
      throw new InvalidSectionException("rowStart가 rowEnd보다 뒤일 수 없습니다");
    }
    if (section.seatsPerRow() == null || section.seatsPerRow() < 1) {
      throw new InvalidSectionException("seatsPerRow는 1 이상이어야 합니다");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static boolean isValidRow(String row) {
    return row != null && row.length() == 1 && row.charAt(0) >= 'A' && row.charAt(0) <= 'Z';
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

  private List<Seat> generateSeats(Performance performance, List<SectionSpec> sections) {
    List<Seat> seats = new ArrayList<>();
    for (SectionSpec section : sections) {
      char rowStart = section.rowStart().charAt(0);
      char rowEnd = section.rowEnd().charAt(0);
      for (char row = rowStart; row <= rowEnd; row++) {
        for (int number = 1; number <= section.seatsPerRow(); number++) {
          String seatLabel = section.grade() + "-" + row + number;
          seats.add(
              new Seat(
                  performance,
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
