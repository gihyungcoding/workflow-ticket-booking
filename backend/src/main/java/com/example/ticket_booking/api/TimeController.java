package com.example.ticket_booking.api;

import com.example.ticket_booking.api.dto.ServerTimeResponse;
import java.time.Clock;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 화면 카운트다운의 기준 시각(ADR-0024). 판정은 주입된 Clock 을 쓰는 서버 쪽이 하므로 여기는 그 Clock 을 그대로 보여 준다. 캐시된 시각은 오프셋을 틀리게
 * 만들어 no-store 를 직접 단다 — Spring Security 기본 헤더(no-cache, no-store, …)에 기대지 않는다.
 */
@RestController
public class TimeController {

  private final Clock clock;

  public TimeController(Clock clock) {
    this.clock = clock;
  }

  @GetMapping("/api/time")
  public ResponseEntity<ServerTimeResponse> now() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(new ServerTimeResponse(clock.instant()));
  }
}
