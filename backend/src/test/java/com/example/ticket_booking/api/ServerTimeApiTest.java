package com.example.ticket_booking.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.example.ticket_booking.support.ClockTestConfig;
import com.example.ticket_booking.support.MutableClock;
import java.time.Instant;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * TASK-019 시나리오 SC-01 — 서버 기준 시각 API.
 *
 * <p>SoT: workflow_design/05_scenario/SCENARIO_TASK-019.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ClockTestConfig.class)
class ServerTimeApiTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private MutableClock clock;

  private final ObjectMapper objectMapper = JsonMapper.builder().build();

  /**
   * SC-01 (happy)
   *
   * <p>Given 시계가 표의 시각을 가리킨다 When Authorization 헤더 없이 GET /api/time 을 호출한다 Then 200, now 가 Z 로 끝나고
   * 표의 시각과 같다, 본문 키는 now 하나, Cache-Control 은 정확히 no-store
   */
  @Test
  @SuppressWarnings("unchecked")
  void test_sc01_서버_시각은_인증_없이_주입된_시각을_캐시_금지로_돌려준다() throws Exception {
    SoftAssertions softly = new SoftAssertions();
    for (String row : new String[] {"2026-11-11T08:55:28.123Z", "2026-11-11T09:00:00Z"}) {
      // Given
      Instant expected = Instant.parse(row);
      clock.setInstant(expected);

      // When
      MvcResult result = mockMvc.perform(get("/api/time")).andReturn();

      // Then
      softly.assertThat(result.getResponse().getStatus()).as("%s 상태", row).isEqualTo(200);
      if (result.getResponse().getStatus() != 200) {
        continue;
      }
      Map<String, Object> body =
          objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
      String now = (String) body.get("now");
      softly.assertThat(now).as("%s now 형식", row).endsWith("Z");
      softly.assertThat(Instant.parse(now)).as("%s now 값", row).isEqualTo(expected);
      softly.assertThat(body.keySet()).as("%s 본문 키", row).containsExactly("now");
      softly
          .assertThat(result.getResponse().getHeaders("Cache-Control"))
          .as("%s Cache-Control", row)
          .containsExactly("no-store");
    }
    softly.assertAll();
  }
}
