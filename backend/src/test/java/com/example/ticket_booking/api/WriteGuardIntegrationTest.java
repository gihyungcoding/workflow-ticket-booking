package com.example.ticket_booking.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.example.ticket_booking.domain.Performance;
import com.example.ticket_booking.repository.PerformanceRepository;
import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * TASK-014 시나리오 SC-04, SC-08.
 *
 * <p>WriteGuardFilter 가 아직 Spring 컨텍스트에 등록되지 않은 상태(Phase 2b)라, 이 두 시나리오는
 * 가드 유무와 무관하게 기존 쓰기 경로가 정상 동작하는지를 검증한다 — 구현 전에도 이미 참이라
 * already_passing 으로 분류된다(TEST_TASK-014.json 참고).
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-014.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
@Transactional
class WriteGuardIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private PerformanceRepository performanceRepository;

  @Autowired private MutableClock clock;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  @BeforeEach
  void resetClock() {
    clock.setInstant(Instant.now());
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

  @SuppressWarnings("unchecked")
  private Map<String, Object> bodyAsMap(MvcResult result) throws Exception {
    return objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
  }

  @Test
  void test_sc04_가드ON_키일치_쓰기_요청이_정상처리되고_변경이_반영된다() throws Exception {
    // Given WRITE_GUARD_ENABLED=true, WRITE_API_KEY=secret123 로 기동했고, venue 가 "서울올림픽공원"인
    // 공연이 등록되어 있다 (가드가 아직 컨텍스트에 등록되지 않아 이 속성은 지금은 아무 영향이 없다)
    Instant now = clock.instant();
    Performance p = performance("공연 P", "서울올림픽공원", now.plus(30, ChronoUnit.DAYS));

    String updateJson =
        """
        {
          "title": "공연 P",
          "venue": "부산 BEXCO",
          "startAt": "%s",
          "openAt": "%s",
          "closeAt": "%s"
        }
        """
            .formatted(
                now.plus(30, ChronoUnit.DAYS).toString(),
                now.minus(1, ChronoUnit.DAYS).toString(),
                now.plus(10, ChronoUnit.DAYS).toString());

    // When X-Write-Key: secret123 헤더와 함께 PUT /api/performances/{id} 로 venue 를 "부산 BEXCO" 로
    // 바꿔 호출한다
    MvcResult putResult =
        mockMvc
            .perform(
                put("/api/performances/" + p.getId())
                    .header("X-Write-Key", "secret123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateJson))
            .andReturn();

    // Then 200이 반환되고 응답 바디의 venue 가 "부산 BEXCO" 다
    assertThat(putResult.getResponse().getStatus()).isEqualTo(200);
    assertThat(bodyAsMap(putResult).get("venue")).isEqualTo("부산 BEXCO");

    // Then 같은 공연을 다시 조회(GET)하면 venue 가 "부산 BEXCO" 로 반영되어 있다
    MvcResult getResult = mockMvc.perform(get("/api/performances/" + p.getId())).andReturn();
    assertThat(bodyAsMap(getResult).get("venue")).isEqualTo("부산 BEXCO");
  }

  @Test
  void test_sc08_가드_OFF_기본값_에서_기존_쓰기_요청이_정상_처리된다() throws Exception {
    // Given WRITE_GUARD_ENABLED 를 설정하지 않았다(기본값)
    Instant now = clock.instant();
    String sections =
        """
        [
          {"grade":"VIP","price":120000,"rowStart":"A","rowEnd":"A","seatsPerRow":10}
        ]
        """;
    String registerJson =
        """
        {
          "title": "가을 재즈 콘서트",
          "venue": "OO홀",
          "startAt": "%s",
          "openAt": "%s",
          "closeAt": "%s",
          "sections": %s
        }
        """
            .formatted(
                now.plus(30, ChronoUnit.DAYS).toString(),
                now.minus(1, ChronoUnit.DAYS).toString(),
                now.plus(20, ChronoUnit.DAYS).toString(),
                sections);

    // When X-Write-Key 헤더 없이 POST /api/performances 로 등록한다 (이 시나리오는 "기존 백엔드 테스트
    // 스위트 전체가 통과한다"는 회귀 보증을 대표하는 단일 요청으로 축약한 것이다)
    MvcResult result =
        mockMvc
            .perform(post("/api/performances").contentType(MediaType.APPLICATION_JSON).content(registerJson))
            .andReturn();

    // Then 모든 기존 테스트가 통과한다 — 이 대표 요청이 가드 없이 그대로 성공한다
    assertThat(result.getResponse().getStatus()).isEqualTo(201);
  }
}
