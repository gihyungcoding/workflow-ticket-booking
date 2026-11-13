package com.example.ticket_booking.service;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.domain.PerformanceFilter;
import com.example.ticket_booking.domain.PerformanceStatus;
import com.example.ticket_booking.domain.PerformanceStatusRules;
import com.example.ticket_booking.domain.PerformanceZone;
import com.example.ticket_booking.domain.Seat;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.repository.PerformanceZoneRepository;
import com.example.ticket_booking.repository.SeatRepository;
import com.example.ticket_booking.repository.SeatSectionCount;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerformanceService {

  private static final int MAX_SEATS = 5000;
  private static final int MAX_PAGE_SIZE = 100;
  private static final BigDecimal MAX_INT_VALUE = BigDecimal.valueOf(Integer.MAX_VALUE);
  private static final int MAX_POSTER_URL_LENGTH = 500;
  private static final Pattern CONTROL_OR_LONE_SURROGATE = Pattern.compile("[\\p{Cc}\\p{Cs}]");

  private final PerformanceRepository performanceRepository;
  private final SeatRepository seatRepository;
  private final PerformanceZoneRepository zoneRepository;
  private final Clock clock;

  public PerformanceService(
      PerformanceRepository performanceRepository,
      SeatRepository seatRepository,
      PerformanceZoneRepository zoneRepository,
      Clock clock) {
    this.performanceRepository = performanceRepository;
    this.seatRepository = seatRepository;
    this.zoneRepository = zoneRepository;
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
        performanceRepository
            .findById(id)
            .filter(found -> Boolean.TRUE.equals(found.getPublished()))
            .orElseThrow(() -> new PerformanceNotFoundException(id));
    return toResponse(performance, clock.instant(), sectionsOf(id));
  }

  /**
   * 요청자 소유 공연 목록. 공개 여부·지난 공연과 무관하게 startAt 내림차순(같으면 id 내림차순)이다. 잘못된 page·size 는 400 으로 거른다 — 관객
   * 목록처럼 처리되지 않은 예외로 새지 않게(page × size 가 int 범위를 넘으면 JPA 의 offset 이 넘친다).
   */
  public OrganizerPerformanceListResponse getMyPerformances(
      Long ownerId, PerformanceFilter filter, int page, int size) {
    if (page < 0 || size < 1 || (long) page * Math.min(size, MAX_PAGE_SIZE) > Integer.MAX_VALUE) {
      throw new InvalidRequestException("page 는 0 이상, size 는 1 이상이어야 합니다");
    }
    Instant now = clock.instant();
    PageRequest pageRequest =
        PageRequest.of(
            page,
            Math.min(size, MAX_PAGE_SIZE),
            Sort.by(Sort.Order.desc("startAt"), Sort.Order.desc("id")));
    Page<Performance> result =
        performanceRepository.findOwnedPerformances(ownerId, filter, now, pageRequest);
    List<OrganizerPerformanceResponse> items =
        result.getContent().stream()
            .map(performance -> toOrganizerResponse(performance, now, null))
            .toList();
    return new OrganizerPerformanceListResponse(
        items, result.getNumber(), result.getSize(), result.getTotalElements());
  }

  public OrganizerPerformanceResponse getMyPerformance(Long ownerId, Long id) {
    Performance performance = findOwned(ownerId, id);
    return toOrganizerResponse(performance, clock.instant(), sectionsOf(id));
  }

  @Transactional
  public OrganizerPerformanceResponse publish(Long ownerId, Long id) {
    return changeVisibility(ownerId, id, true);
  }

  @Transactional
  public OrganizerPerformanceResponse unpublish(Long ownerId, Long id) {
    return changeVisibility(ownerId, id, false);
  }

  /** 취소 → 시각 순으로 검사한다. 이미 그 상태면 값을 그대로 두고 응답만 돌려준다(오픈 전에 한해). */
  private OrganizerPerformanceResponse changeVisibility(Long ownerId, Long id, boolean published) {
    Performance performance = findOwned(ownerId, id);
    Instant now = clock.instant();
    if (Boolean.TRUE.equals(performance.getCancelled())) {
      throw new PerformanceCancelledException(id);
    }
    if (!PerformanceStatusRules.visibilityChangeable(false, now, performance.getOpenAt())) {
      throw new VisibilityLockedException(id);
    }
    if (!Boolean.valueOf(published).equals(performance.getPublished())) {
      performance.changePublished(published);
      performanceRepository.save(performance);
    }
    return toOrganizerResponse(performance, now, null);
  }

  /** 홈 머리글 후보 — 선택 규칙은 여기 한 곳에 둔다(스크랩이 생기면 이 정렬의 1순위만 바뀐다). */
  public Optional<PerformanceResponse> getNextOpening() {
    Instant now = clock.instant();
    return performanceRepository
        .findNextOpening(now)
        .map(performance -> toResponse(performance, now));
  }

  /**
   * 좌석 집계(price 내림차순 → grade 오름차순)에 구역 위치를 붙여 stageOrder 순으로 돌려준다. 구역 행이 없는 등급(V6 이전에 등록된 공연)은 집계
   * 순서대로 기존 구역 뒤에 등급 이름을 묶음 이름으로 붙인다 — 그래서 V6 이전 공연의 순서는 바뀌지 않는다. 같은 등급의 가격이 둘이면 같은 구역 안에서 집계 순서(가격
   * 내림차순)를 지킨다.
   */
  private List<SectionSummaryResponse> sectionsOf(Long performanceId) {
    Map<String, PerformanceZone> zones = new HashMap<>();
    int lastStageOrder = 0;
    for (PerformanceZone zone :
        zoneRepository.findByPerformanceIdOrderByStageOrder(performanceId)) {
      zones.put(zone.getGrade(), zone);
      lastStageOrder = Math.max(lastStageOrder, zone.getStageOrder());
    }
    Map<String, Integer> unzonedOrder = new HashMap<>();
    List<SectionSummaryResponse> sections = new ArrayList<>();
    for (SeatSectionCount count : seatRepository.findSectionCounts(performanceId)) {
      PerformanceZone zone = zones.get(count.getGrade());
      if (zone != null) {
        sections.add(
            new SectionSummaryResponse(
                count.getGrade(),
                count.getPrice(),
                count.getSeatCount(),
                zone.getArea(),
                zone.getAreaNote(),
                zone.getStageOrder()));
        continue;
      }
      if (!unzonedOrder.containsKey(count.getGrade())) {
        unzonedOrder.put(count.getGrade(), lastStageOrder + unzonedOrder.size() + 1);
      }
      sections.add(
          new SectionSummaryResponse(
              count.getGrade(),
              count.getPrice(),
              count.getSeatCount(),
              count.getGrade(),
              null,
              unzonedOrder.get(count.getGrade())));
    }
    sections.sort(Comparator.comparing(SectionSummaryResponse::stageOrder));
    return sections;
  }

  @Transactional
  public OrganizerPerformanceResponse registerPerformance(
      Long ownerId,
      String title,
      String venue,
      Instant startAt,
      Instant openAt,
      Instant closeAt,
      List<SectionSpec> sections,
      String posterUrl,
      List<ZoneSpec> zones) {
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

    validatePosterUrl(posterUrl);
    List<ZonePlacement> placements = placeZones(sections, zones);

    Performance performance =
        performanceRepository.save(
            new Performance(
                ownerId,
                title,
                venue,
                startAt,
                openAt,
                closeAt,
                totalSeats,
                totalSeats,
                false,
                posterUrl,
                false));
    seatRepository.saveAll(generateSeats(performance, sections));
    zoneRepository.saveAll(
        placements.stream()
            .map(
                placement ->
                    new PerformanceZone(
                        performance,
                        placement.grade(),
                        placement.area(),
                        placement.note(),
                        placement.stageOrder()))
            .toList());

    return toOrganizerResponse(performance, clock.instant(), null);
  }

  @Transactional
  public OrganizerPerformanceResponse updatePerformance(
      Long ownerId,
      Long id,
      String title,
      String venue,
      Instant startAt,
      Instant openAt,
      Instant closeAt) {
    Performance performance = findOwned(ownerId, id);
    Instant now = clock.instant();
    if (Boolean.TRUE.equals(performance.getCancelled())) {
      throw new PerformanceCancelledException(id);
    }
    if (!PerformanceStatusRules.editable(
        false, Boolean.TRUE.equals(performance.getPublished()), now, performance.getOpenAt())) {
      throw new RegistrationAlreadyOpenException(id);
    }
    validateTimeOrder(openAt, closeAt, startAt);

    performance.updateSchedule(title, venue, startAt, openAt, closeAt);
    performanceRepository.save(performance);
    return toOrganizerResponse(performance, now, null);
  }

  @Transactional
  public OrganizerPerformanceResponse cancelPerformance(Long ownerId, Long id) {
    Performance performance = findOwned(ownerId, id);
    performance.cancel();
    performanceRepository.save(performance);
    return toOrganizerResponse(performance, clock.instant(), null);
  }

  /**
   * 요청자가 소유한 공연만 돌려준다. 남의 공연·소유자를 모르는 공연(owner_id NULL)·없는 공연을 같은 404 로 묶는다 — 응답으로 공연의 존재나 소유자를 알 수
   * 없게(ADR-0029).
   */
  private Performance findOwned(Long ownerId, Long id) {
    return performanceRepository
        .findById(id)
        .filter(performance -> ownerId.equals(performance.getOwnerId()))
        .orElseThrow(() -> new PerformanceNotFoundException(id));
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

  /**
   * https 절대 URL 이고 500자(UTF-16 코드 유닛) 이하여야 한다. 원문 그대로 판정한다 — 앞뒤 공백을 걷어내지 않고(URI 파싱에서 거부된다), 스킴의
   * 대소문자는 RFC 3986 대로 구분하지 않되 저장값은 바꾸지 않는다. java.net.URI 는 짝 없는 서로게이트를 비 ASCII 글자로 받아 주므로 제어문자와 함께
   * 먼저 거부한다.
   *
   * <p>호스트는 URI.getHost() 로 보지 않는다 — 밑줄·비 ASCII 글자가 있는 호스트는 URI 가 등록 기반 권한으로 분류해 null 을 돌려준다. 대신 권한
   * 부분에서 userinfo(…@)와 포트(:…)를 직접 떼어 본다 (TASK-019 자체 리뷰 RC-1).
   */
  private void validatePosterUrl(String posterUrl) {
    if (posterUrl == null) {
      return;
    }
    if (posterUrl.length() > MAX_POSTER_URL_LENGTH
        || CONTROL_OR_LONE_SURROGATE.matcher(posterUrl).find()) {
      throw new InvalidPosterUrlException();
    }
    URI uri;
    try {
      uri = new URI(posterUrl);
    } catch (URISyntaxException exception) {
      throw new InvalidPosterUrlException();
    }
    if (!"https".equalsIgnoreCase(uri.getScheme()) || !hasValidAuthority(uri)) {
      throw new InvalidPosterUrlException();
    }
  }

  /**
   * userinfo 가 있으면 비어 있지 않고 '@' 를 담지 않으며, 포트가 있으면 ASCII 숫자이고, 호스트가 비어 있지 않고 %XX·':'(IPv6 리터럴 제외)를
   * 담지 않는다.
   */
  private static boolean hasValidAuthority(URI uri) {
    String authority = uri.getRawAuthority();
    if (authority == null) {
      return false;
    }
    // userinfo 는 비어 있지 않고 '@' 를 담지 않는다 — 첫 '@' 와 마지막 '@' 가 같아야 한다
    int userInfoEnd = authority.lastIndexOf('@');
    if (userInfoEnd == 0 || authority.indexOf('@') != userInfoEnd) {
      return false;
    }
    String hostAndPort = authority.substring(userInfoEnd + 1);
    String host;
    String port;
    if (hostAndPort.startsWith("[")) {
      // IPv6 리터럴은 URI 가 서버 기반으로 파싱했을 때만(getHost() != null) 올바르다
      int literalEnd = hostAndPort.indexOf(']');
      if (uri.getHost() == null || literalEnd < 0) {
        return false;
      }
      host = hostAndPort.substring(0, literalEnd + 1);
      String rest = hostAndPort.substring(literalEnd + 1);
      if (!rest.isEmpty() && !rest.startsWith(":")) {
        return false;
      }
      port = rest.isEmpty() ? null : rest.substring(1);
    } else {
      int portStart = hostAndPort.lastIndexOf(':');
      host = portStart < 0 ? hostAndPort : hostAndPort.substring(0, portStart);
      port = portStart < 0 ? null : hostAndPort.substring(portStart + 1);
    }
    // 포트는 ASCII 숫자만 — Character.isDigit 는 아랍-인도·전각 숫자도 받는다
    if (port != null && (port.isEmpty() || !port.chars().allMatch(c -> c >= '0' && c <= '9'))) {
      return false;
    }
    if (host.isEmpty() || host.indexOf('%') >= 0) {
      return false;
    }
    // IPv6 리터럴이 아닌 호스트에 ':' 가 남았으면 포트 구분자가 둘 이상이었다
    return host.startsWith("[") || host.indexOf(':') < 0;
  }

  private record ZonePlacement(String grade, String area, String note, int stageOrder) {}

  /**
   * 등급마다 구역 위치를 정한다. zones 를 펼친 순서(묶음 → 묶음 안 grades)로 stageOrder 를 매기고, zones 에 없는 등급은 sections 에
   * 처음 나온 순서대로 그 뒤에 등급 이름을 묶음 이름으로 붙인다. 펼친 grades 를 순서대로 보다가 처음 걸린 위반(sections 에 없음 / 이미 나옴)을 던진다.
   */
  private List<ZonePlacement> placeZones(List<SectionSpec> sections, List<ZoneSpec> zones) {
    Set<String> sectionGrades = new LinkedHashSet<>();
    for (SectionSpec section : sections) {
      sectionGrades.add(section.grade());
    }
    List<ZonePlacement> placements = new ArrayList<>();
    Set<String> placed = new LinkedHashSet<>();
    for (ZoneSpec zone : zones) {
      for (String grade : zone.grades()) {
        if (!sectionGrades.contains(grade)) {
          throw new ZoneGradeUnknownException(grade);
        }
        if (!placed.add(grade)) {
          throw new ZoneGradeDuplicatedException(grade);
        }
        placements.add(new ZonePlacement(grade, zone.area(), zone.note(), placements.size() + 1));
      }
    }
    for (String grade : sectionGrades) {
      if (placed.add(grade)) {
        placements.add(new ZonePlacement(grade, grade, null, placements.size() + 1));
      }
    }
    return placements;
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
        performance.getPosterUrl(),
        sections);
  }

  private OrganizerPerformanceResponse toOrganizerResponse(
      Performance performance, Instant now, List<SectionSummaryResponse> sections) {
    boolean cancelled = Boolean.TRUE.equals(performance.getCancelled());
    boolean published = Boolean.TRUE.equals(performance.getPublished());
    PerformanceStatus status =
        PerformanceStatusRules.of(
            cancelled,
            now,
            performance.getOpenAt(),
            performance.getCloseAt(),
            performance.getAvailableSeats());
    return new OrganizerPerformanceResponse(
        performance.getId(),
        performance.getTitle(),
        performance.getVenue(),
        performance.getStartAt(),
        performance.getOpenAt(),
        performance.getCloseAt(),
        performance.getTotalSeats(),
        performance.getAvailableSeats(),
        status,
        performance.getPosterUrl(),
        published,
        PerformanceStatusRules.editable(cancelled, published, now, performance.getOpenAt()),
        PerformanceStatusRules.visibilityChangeable(cancelled, now, performance.getOpenAt()),
        sections);
  }
}
