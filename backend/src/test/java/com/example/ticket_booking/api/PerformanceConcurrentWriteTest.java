package com.example.ticket_booking.api;

import static com.example.ticket_booking.support.AuthTestHelper.accessTokenOf;
import static com.example.ticket_booking.support.AuthTestHelper.login;
import static com.example.ticket_booking.support.AuthTestHelper.signUp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.repository.PerformanceZoneRepository;
import com.example.ticket_booking.repository.SeatRepository;
import com.example.ticket_booking.repository.SocialAccountRepository;
import com.example.ticket_booking.repository.UserRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.JsonText;
import com.example.ticket_booking.support.MutableClock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * TASK-023 시나리오 SC-20 — 같은 공연의 쓰기가 겹치면 하나씩 처리된다(Phase 4 RC-1).
 *
 * <p>경쟁을 우연에 맡기지 않는다. 별도 트랜잭션이 JPA 표준 잠금으로 공연 행을 쥔 채 값을 바꾸고 끝내지 않으면(진행 중인 쓰기를 흉내), 별도 스레드의 HTTP 요청
 * B 가 그 사이에 들어간다. 잠금 없이 옛 행을 읽어 전 컬럼을 저장하는 구현은 진행 중이던 변경(취소·title·공개)을 되돌린다. 결과는 구현(잠금·버전)과 무관하게 최종
 * DB 값으로 단언한다.
 *
 * <p>클래스 레벨 {@code @Transactional} 을 두지 않는다 — 별도 트랜잭션이 실제로 커밋돼야 하고, 최종 값은 리포지토리 재조회로 읽는다(CLAUDE.md
 * 리뷰 지시). 시각은 MutableClock 으로 고정한다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-023.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class PerformanceConcurrentWriteTest {

  private static final Instant T = Instant.parse("2026-11-13T09:00:00Z");
  private static final String PASSWORD = "password123";
  private static final String TITLE = "소리풍경 밴드 나이트";
  private static final String ENCORE = "소리풍경 밴드 나이트 앙코르";
  private static final String OTHER = "다른 제목";
  private static final long HOLD_MILLIS = 300;

  @Autowired private MockMvc mockMvc;
  @Autowired private PerformanceRepository performanceRepository;
  @Autowired private SeatRepository seatRepository;
  @Autowired private PerformanceZoneRepository zoneRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private SocialAccountRepository socialAccountRepository;
  @Autowired private MutableClock clock;
  @Autowired private PlatformTransactionManager transactionManager;

  @PersistenceContext private EntityManager entityManager;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();
  private final ExecutorService executor = Executors.newFixedThreadPool(2);

  private String o1;
  private Long o1Id;

  @BeforeEach
  void setUp() throws Exception {
    // 토큰 발급 시각이 가장 이른 시각(T-1ms)보다 늦지 않게, 먼저 그 시각으로 가입·로그인한다
    clock.setInstant(T.minusMillis(1));
    clearTables();
    signUp(mockMvc, "o1@example.com", PASSWORD, "한도윤", "ORGANIZER");
    o1 = accessTokenOf(login(mockMvc, "o1@example.com", PASSWORD));
    o1Id = userRepository.findByEmail("o1@example.com").orElseThrow().getId();
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
    clearTables();
  }

  private void clearTables() {
    zoneRepository.deleteAll();
    seatRepository.deleteAll();
    performanceRepository.deleteAll();
    socialAccountRepository.deleteAll();
    userRepository.deleteAll();
  }

  // ---------- 픽스처·호출 ----------

  private Performance saved(boolean published, Instant openAt) {
    return performanceRepository.save(
        new Performance(
            o1Id,
            TITLE,
            "롤링홀",
            T.plus(Duration.ofDays(30)),
            openAt,
            T.plus(Duration.ofDays(29)),
            100,
            100,
            false,
            null,
            published));
  }

  private static Map<String, Object> u0(Performance performance) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("title", ENCORE);
    body.put("venue", "롤링홀");
    body.put("startAt", performance.getStartAt().toString());
    body.put("openAt", performance.getOpenAt().toString());
    body.put("closeAt", performance.getCloseAt().toString());
    return body;
  }

  private MvcResult send(MockHttpServletRequestBuilder builder) throws Exception {
    return mockMvc.perform(builder.header("Authorization", "Bearer " + o1)).andReturn();
  }

  private MvcResult publish(Long id) throws Exception {
    return send(post("/api/performances/" + id + "/publish"));
  }

  private MvcResult unpublish(Long id) throws Exception {
    return send(post("/api/performances/" + id + "/unpublish"));
  }

  private MvcResult cancel(Long id) throws Exception {
    return send(post("/api/performances/" + id + "/cancel"));
  }

  private MvcResult putU0(Performance performance) throws Exception {
    return send(
        put("/api/performances/" + performance.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonText.of(u0(performance))));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> json(MvcResult result) throws Exception {
    String body = result.getResponse().getContentAsString();
    return body.isEmpty() ? Map.of() : objectMapper.readValue(body, Map.class);
  }

  private interface Request {
    MvcResult send() throws Exception;
  }

  /**
   * 행을 쥔 별도 트랜잭션이 change 를 반영한 채 끝나지 않은 동안 요청 B 를 보낸다. B 를 보낸 직후 afterSent 를 실행하고, B 가 끝나지 않았음을 확인한
   * 뒤 트랜잭션을 끝내고 B 의 결과를 돌려준다.
   */
  private MvcResult race(
      Long id,
      Consumer<Performance> change,
      Request request,
      Runnable afterSent,
      SoftAssertions soft,
      String label)
      throws Exception {
    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    Future<?> holder =
        executor.submit(
            () ->
                new TransactionTemplate(transactionManager)
                    .execute(
                        status -> {
                          Performance row =
                              entityManager.find(
                                  Performance.class, id, LockModeType.PESSIMISTIC_WRITE);
                          change.accept(row);
                          entityManager.flush();
                          locked.countDown();
                          try {
                            release.await(10, TimeUnit.SECONDS);
                          } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                          }
                          return null;
                        }));
    assertThat(locked.await(10, TimeUnit.SECONDS)).as("%s 행 잠금", label).isTrue();

    Future<MvcResult> b = executor.submit(request::send);
    Thread.sleep(HOLD_MILLIS);
    afterSent.run();
    // Then B 는 그 트랜잭션이 끝나기 전에는 응답하지 않는다
    soft.assertThat(b.isDone()).as("%s: 진행 중에는 B 가 끝나지 않는다", label).isFalse();

    release.countDown();
    holder.get(10, TimeUnit.SECONDS);
    return b.get(10, TimeUnit.SECONDS);
  }

  private void expect(
      SoftAssertions soft,
      String label,
      MvcResult result,
      int status,
      String code,
      Long id,
      boolean cancelled,
      boolean published,
      String title)
      throws Exception {
    soft.assertThat(result.getResponse().getStatus()).as("%s 상태", label).isEqualTo(status);
    if (code != null) {
      soft.assertThat(json(result).get("code")).as("%s code", label).isEqualTo(code);
    }
    Performance after = performanceRepository.findById(id).orElseThrow();
    soft.assertThat(after.getCancelled()).as("%s cancelled", label).isEqualTo(cancelled);
    soft.assertThat(after.getPublished()).as("%s published", label).isEqualTo(published);
    soft.assertThat(after.getTitle()).as("%s title", label).isEqualTo(title);
  }

  private static void retitle(Performance row) {
    row.updateSchedule(OTHER, row.getVenue(), row.getStartAt(), row.getOpenAt(), row.getCloseAt());
  }

  // ---------- SC-20 ----------

  /**
   * SC-20 (error)
   *
   * <p>Given O1 소유 공연 P(표의 공개 여부) 와, P 의 행을 잠근 채 표의 변경을 반영했지만 끝나지 않은 별도 트랜잭션 When 그 트랜잭션이 끝나기 전에 O1
   * 이 요청 B 를 보낸다 Then B 는 끝나기 전에는 응답하지 않고, 끝나면 표의 결과로 응답하며, 최종 DB(cancelled·published·title)가 표와 같다
   */
  @Test
  void test_sc20_같은_공연의_쓰기가_겹치면_하나씩_처리돼_서로의_변경을_덮어쓰지_않는다() throws Exception {
    SoftAssertions soft = new SoftAssertions();
    Runnable none = () -> {};
    Runnable toOpenAt = () -> clock.setInstant(T);

    // a — 비공개 + 진행 중 취소, B publish → 409 PERFORMANCE_CANCELLED, 취소 그대로·비공개 그대로
    clock.setInstant(T);
    Performance a = saved(false, T.plus(Duration.ofDays(7)));
    MvcResult ra = race(a.getId(), Performance::cancel, () -> publish(a.getId()), none, soft, "a");
    expect(soft, "a", ra, 409, "PERFORMANCE_CANCELLED", a.getId(), true, false, TITLE);

    // b — 공개 + 진행 중 취소, B unpublish → 409 PERFORMANCE_CANCELLED, 취소 그대로·공개 그대로
    Performance b = saved(true, T.plus(Duration.ofDays(7)));
    MvcResult rb =
        race(b.getId(), Performance::cancel, () -> unpublish(b.getId()), none, soft, "b");
    expect(soft, "b", rb, 409, "PERFORMANCE_CANCELLED", b.getId(), true, true, TITLE);

    // c — 비공개 + 진행 중 취소, B PUT → 409 PERFORMANCE_CANCELLED, title 그대로
    Performance c = saved(false, T.plus(Duration.ofDays(7)));
    MvcResult rc = race(c.getId(), Performance::cancel, () -> putU0(c), none, soft, "c");
    expect(soft, "c", rc, 409, "PERFORMANCE_CANCELLED", c.getId(), true, false, TITLE);

    // d — 비공개 + 진행 중 title 변경, B publish → 200, 둘 다 반영
    Performance d = saved(false, T.plus(Duration.ofDays(7)));
    MvcResult rd =
        race(
            d.getId(),
            PerformanceConcurrentWriteTest::retitle,
            () -> publish(d.getId()),
            none,
            soft,
            "d");
    expect(soft, "d", rd, 200, null, d.getId(), false, true, OTHER);

    // e — 비공개 + 진행 중 공개 전환, B PUT → 200, 둘 다 반영
    Performance e = saved(false, T.plus(Duration.ofDays(7)));
    MvcResult re =
        race(e.getId(), row -> row.changePublished(true), () -> putU0(e), none, soft, "e");
    expect(soft, "e", re, 200, null, e.getId(), false, true, ENCORE);

    // f — 공개, openAt T, 시각 T-1ms 에 B unpublish 를 보낸 뒤 대기 중 T 로 옮김 → 409 VISIBILITY_LOCKED
    clock.setInstant(T.minusMillis(1));
    Performance f = saved(true, T);
    MvcResult rf =
        race(
            f.getId(),
            PerformanceConcurrentWriteTest::retitle,
            () -> unpublish(f.getId()),
            toOpenAt,
            soft,
            "f");
    expect(soft, "f", rf, 409, "VISIBILITY_LOCKED", f.getId(), false, true, OTHER);

    // g — 비공개 + 진행 중 title 변경, B cancel → 200, 둘 다 반영
    clock.setInstant(T);
    Performance g = saved(false, T.plus(Duration.ofDays(7)));
    MvcResult rg =
        race(
            g.getId(),
            PerformanceConcurrentWriteTest::retitle,
            () -> cancel(g.getId()),
            none,
            soft,
            "g");
    expect(soft, "g", rg, 200, null, g.getId(), true, false, OTHER);

    // h — 비공개, openAt T, 시각 T-1ms 에 B publish 를 보낸 뒤 대기 중 T 로 옮김 → 409 VISIBILITY_LOCKED
    clock.setInstant(T.minusMillis(1));
    Performance h = saved(false, T);
    MvcResult rh =
        race(
            h.getId(),
            PerformanceConcurrentWriteTest::retitle,
            () -> publish(h.getId()),
            toOpenAt,
            soft,
            "h");
    expect(soft, "h", rh, 409, "VISIBILITY_LOCKED", h.getId(), false, false, OTHER);

    // i — 공개, openAt T, 시각 T-1ms 에 B PUT 을 보낸 뒤 대기 중 T 로 옮김 → 409 REGISTRATION_ALREADY_OPEN
    clock.setInstant(T.minusMillis(1));
    Performance i = saved(true, T);
    MvcResult ri =
        race(
            i.getId(),
            PerformanceConcurrentWriteTest::retitle,
            () -> putU0(i),
            toOpenAt,
            soft,
            "i");
    expect(soft, "i", ri, 409, "REGISTRATION_ALREADY_OPEN", i.getId(), false, true, OTHER);

    soft.assertAll();
  }
}
