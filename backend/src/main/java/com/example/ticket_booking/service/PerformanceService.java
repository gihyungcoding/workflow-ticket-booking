package com.example.ticket_booking.service;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.domain.PerformanceStatus;
import com.example.ticket_booking.domain.PerformanceStatusRules;
import com.example.ticket_booking.domain.Seat;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.repository.SeatRepository;
import java.math.BigDecimal;
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
  private static final BigDecimal MAX_INT_VALUE = BigDecimal.valueOf(Integer.MAX_VALUE);

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
    return toResponse(performance, clock.instant(), sectionsOf(id));
  }

  private List<SectionSummaryResponse> sectionsOf(Long performanceId) {
    return seatRepository.findSectionCounts(performanceId).stream()
        .map(
            count ->
                new SectionSummaryResponse(
                    count.getGrade(), count.getPrice(), count.getSeatCount()))
        .toList();
  }

  @Transactional
  public PerformanceResponse registerPerformance(
      String title,
      String venue,
      Instant startAt,
      Instant openAt,
      Instant closeAt,
      List<SectionSpec> sections) {
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
      totalSeatsLong += (long) rowCount(section) * section.seatsPerRow().intValueExact();
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

  /**
   * 필드 형태(필수·길이·형식)는 api.dto 의 Bean Validation 이 Controller 경계에서 걸러낸다 (ADR-0010). price/seatsPerRow
   * 의 범위·정수성은 @DecimalMax·@Digits 로도 표현할 수 있지만, @Digits 구현의 precision()-scale() 계산이 극단적 지수 표기에서 int
   * 오버플로로 무력화될 수 있어 여기서 signum·compareTo 를 isIntegral() 보다 먼저 거치는 순서로 직접 판정한다. rowStart>rowEnd 는 두
   * 필드를 함께 봐야 하는 도메인 규칙이라 애노테이션으로 표현할 수 없다. null 원소는 @Valid cascade 대상이 아니라 여기까지 내려온다.
   */
  private void validateSection(SectionSpec section) {
    if (section == null) {
      throw new InvalidSectionException("구역 정보는 비어 있을 수 없습니다");
    }
    // price/seatsPerRow: 범위 확인(signum·compareTo)이 isIntegral()보다 먼저다 — isIntegral()의
    // stripTrailingZeros()는 scale이 Integer.MIN_VALUE 아래로 내려가면 ArithmeticException을 던진다
    // (극단적 지수 표기, 예: 100E+2147483647). signum<0/compareTo(ONE)<0 을 통과한 0이 아닌 값은
    // scale 이 그렇게까지 내려갈 수 없고, 0인 값은 stripTrailingZeros() 가 항상 scale 0으로 정규화하는
    // fast path를 타 안전하다(예: 0E+2147483647 도 isIntegral() 에서 true 로 판정됨).
    if (section.price().signum() < 0) {
      throw new InvalidSectionException("price는 0 이상이어야 합니다");
    }
    if (section.price().compareTo(MAX_INT_VALUE) > 0) {
      throw new InvalidSectionException("price는 " + Integer.MAX_VALUE + " 이하여야 합니다");
    }
    if (!isIntegral(section.price())) {
      throw new InvalidSectionException("price는 정수여야 합니다");
    }
    if (section.rowStart().charAt(0) > section.rowEnd().charAt(0)) {
      throw new InvalidSectionException("rowStart가 rowEnd보다 뒤일 수 없습니다");
    }
    if (section.seatsPerRow().compareTo(BigDecimal.ONE) < 0) {
      throw new InvalidSectionException("seatsPerRow는 1 이상이어야 합니다");
    }
    if (section.seatsPerRow().compareTo(MAX_INT_VALUE) > 0) {
      throw new InvalidSectionException("seatsPerRow는 " + Integer.MAX_VALUE + " 이하여야 합니다");
    }
    if (!isIntegral(section.seatsPerRow())) {
      throw new InvalidSectionException("seatsPerRow는 정수여야 합니다");
    }
  }

  private static boolean isIntegral(BigDecimal value) {
    return value.stripTrailingZeros().scale() <= 0;
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
      int seatsPerRow = section.seatsPerRow().intValueExact();
      for (char row = rowStart; row <= rowEnd; row++) {
        for (int number = 1; number <= seatsPerRow; number++) {
          String seatLabel = section.grade() + "-" + row + number;
          seats.add(
              new Seat(
                  performance,
                  section.grade(),
                  String.valueOf(row),
                  number,
                  seatLabel,
                  section.price().intValueExact()));
        }
      }
    }
    return seats;
  }

  private PerformanceResponse toResponse(Performance performance, Instant now) {
    return toResponse(performance, now, null);
  }

  private PerformanceResponse toResponse(
      Performance performance, Instant now, List<SectionSummaryResponse> sections) {
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
        status,
        sections);
  }
}
