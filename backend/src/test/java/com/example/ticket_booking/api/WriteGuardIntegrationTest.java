package com.example.ticket_booking.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * TASK-014 시나리오 SC-04, SC-06 (통합).
 *
 * <p>가드를 실제로 켜고(WRITE_GUARD_ENABLED=true) 필터 체인 전체를 태운다. 클래스 레벨 {@code @Transactional} 을 두지 않는다 —
 * 트랜잭션 안에서는 PUT 후 GET 이 1차 캐시를 읽고 롤백되어 "DB 에 실제로 반영됐는가/변경이 없는가" 를 증명하지 못한다(Phase 4 R1). 요청은 각자
 * 커밋하고, 확인은 리포지토리로 새로 읽는다.
 *
 * <p>SC-06 의 필터 단위 검증(WriteGuardFilterTest)은 이 통합 테스트로 옮겼다 — "DB 변경 없음" 은 chain 호출 여부가 아니라 실제 DB
 * 상태로 확인해야 한다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-014.md
 */
@SpringBootTest(
    properties = {
      "WRITE_GUARD_ENABLED=true",
      "WRITE_API_KEY=secret123",
      "CORS_ALLOWED_ORIGINS=https://app.example.com"
    })
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class WriteGuardIntegrationTest {

  private static final String ORIGIN = "https://app.example.com";

  @Autowired private MockMvc mockMvc;

  @Autowired private PerformanceRepository performanceRepository;

  @Autowired private MutableClock clock;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  @BeforeEach
  void resetClock() {
    performanceRepository.deleteAll();
    clock.setInstant(Instant.now());
  }

  @AfterEach
  void cleanUp() {
    performanceRepository.deleteAll();
  }

  private Performance performance(String title, String venue, Instant startAt) {
    Instant now = clock.instant();
    return performanceRepository.save(
        new Performance(
            title,
            venue,
            startAt,
            now.plus(5, ChronoUnit.DAYS),
            now.plus(20, ChronoUnit.DAYS),
            100,
            50,
            false));
  }

  private String updateJson(String venue) {
    Instant now = clock.instant();
    return """
        {
          "title": "공연 P",
          "venue": "%s",
          "startAt": "%s",
          "openAt": "%s",
          "closeAt": "%s"
        }
        """
        .formatted(
            venue,
            now.plus(30, ChronoUnit.DAYS).toString(),
            now.minus(1, ChronoUnit.DAYS).toString(),
            now.plus(10, ChronoUnit.DAYS).toString());
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> bodyAsMap(MvcResult result) throws Exception {
    return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
  }

  @Test
  void test_sc04_가드ON_키일치_쓰기_요청이_정상처리되고_변경이_반영된다() throws Exception {
    // Given WRITE_GUARD_ENABLED=true, WRITE_API_KEY=secret123 로 기동했고(클래스 properties),
    // venue 가 "서울올림픽공원"인 공연이 등록되어 있다
    Performance p = performance("공연 P", "서울올림픽공원", clock.instant().plus(30, ChronoUnit.DAYS));

    // When X-Write-Key: secret123 헤더와 함께 PUT /api/performances/{id} 로 venue 를 "부산 BEXCO" 로
    // 바꿔 호출한다
    MvcResult putResult =
        mockMvc
            .perform(
                put("/api/performances/" + p.getId())
                    .header("X-Write-Key", "secret123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateJson("부산 BEXCO")))
            .andReturn();

    // Then 200이 반환되고 응답 바디의 venue 가 "부산 BEXCO" 다
    assertThat(putResult.getResponse().getStatus()).isEqualTo(200);
    assertThat(bodyAsMap(putResult).get("venue")).isEqualTo("부산 BEXCO");

    // Then 같은 공연을 다시 조회(GET)하면 venue 가 "부산 BEXCO" 로 반영되어 있다
    MvcResult getResult = mockMvc.perform(get("/api/performances/" + p.getId())).andReturn();
    assertThat(bodyAsMap(getResult).get("venue")).isEqualTo("부산 BEXCO");
    // 트랜잭션 밖이므로 위 PUT 은 이미 커밋됐다 — 리포지토리로 새로 읽어도 같아야 한다
    assertThat(performanceRepository.findById(p.getId()).orElseThrow().getVenue())
        .isEqualTo("부산 BEXCO");
  }

  @Test
  void test_sc06_쓰기_메서드는_키가_없거나_틀리면_401로_거부되고_DB가_변경되지_않는다() throws Exception {
    // Given WRITE_GUARD_ENABLED=true, WRITE_API_KEY=secret123 로 기동했고 공연 1건이 있다
    Performance p = performance("공연 P", "서울올림픽공원", clock.instant().plus(30, ChronoUnit.DAYS));
    long countBefore = performanceRepository.count();

    // When 메서드(POST/PUT/PATCH/DELETE) × 키상태(헤더없음/틀림) 8개 조합으로 요청한다
    String url = "/api/performances/" + p.getId();
    for (String method : List.of("POST", "PUT", "PATCH", "DELETE")) {
      for (String key : new String[] {null, "wrong"}) {
        var builder =
            switch (method) {
              case "POST" -> post("/api/performances");
              case "PUT" -> put(url);
              case "PATCH" -> patch(url);
              default -> delete(url);
            };
        builder
            .header("Origin", ORIGIN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(updateJson("해킹된 장소"));
        if (key != null) {
          builder.header("X-Write-Key", key);
        }

        var response = mockMvc.perform(builder).andReturn().getResponse();

        // Then 표의 모든 조합에서 401이 반환된다
        assertThat(response.getStatus()).as("%s key=%s", method, key).isEqualTo(401);
        // Then 401 응답에도 CORS 헤더가 붙어 브라우저가 오류 본문을 읽을 수 있다(필터 순서)
        assertThat(response.getHeader("Access-Control-Allow-Origin"))
            .as("%s key=%s ACAO", method, key)
            .isEqualTo(ORIGIN);
      }
    }

    // Then 각 요청 모두 DB에 어떤 변경도 생기지 않는다 — 트랜잭션 밖에서 새로 읽어 확인한다
    assertThat(performanceRepository.count()).isEqualTo(countBefore);
    Performance after = performanceRepository.findById(p.getId()).orElseThrow();
    assertThat(after.getVenue()).isEqualTo("서울올림픽공원");
  }
}
