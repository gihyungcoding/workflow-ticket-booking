package com.example.ticket_booking.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

/**
 * TASK-014 시나리오 SC-08 (통합) — 가드 설정이 없는 기본 상태.
 *
 * <p>WRITE_GUARD_ENABLED 를 지정하지 않는다. 가드가 켜진 SC-04/06 과 설정이 달라 별도 클래스로 둔다.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-014.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
@Transactional
class WriteGuardOffIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private MutableClock clock;

  @BeforeEach
  void resetClock() {
    clock.setInstant(Instant.now());
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
            .perform(
                post("/api/performances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerJson))
            .andReturn();

    // Then 모든 기존 테스트가 통과한다 — 이 대표 요청이 가드 없이 그대로 성공한다
    assertThat(result.getResponse().getStatus()).isEqualTo(201);
  }
}
