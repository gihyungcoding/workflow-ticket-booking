---
status: ACTIVE
saved_at: 2026-10-21T00:00:00Z
summary: "PLAN_TASK-009.json 작성 완료 — route Backend, flows 9건이 AC 8건을 전부 덮음(F9 는 입력 경계). EXIT GATE 통과."

next_steps:
  - "Phase 2a — wf-scenario 로 시나리오 설계 (경계값: 비밀번호 72바이트, 이메일 정규화, 만료 토큰, 존재하지 않는 경로의 401/404)"
  - "사용자 확인 요청 3건은 PLAN.unresolved 의 [결정·확인 요청] — JWT 라이브러리, 인증 API 의 쓰기 가드 적용, 이메일 정규화"

decisions:
  - decision: "쓰기 가드(WriteGuardFilter)는 건드리지 않고 인증 API 도 면제하지 않는다"
    rationale: "ADR-0017. 소유권 검사 전까지 공연 쓰기의 유일한 방어. 프론트는 쓰기 요청 전부에 키를 붙이므로 TASK-010 에 문제 없다"
  - decision: "JWT 는 spring-security-oauth2-jose(Nimbus)로 발급·검증하고 JwtTokenProvider 가 감싼다"
    rationale: "TASK-011 의 OAuth2 Client 와 같은 계열이라 의존성이 늘지 않는다. ADR-0012·0013 은 라이브러리를 정하지 않았다 — 사용자 확인 요청"
  - decision: "JWT 시각은 주입된 Clock 으로 처리한다"
    rationale: "ADR-0005. 테스트에서 MutableClock 으로 만료 토큰을 재현"
  - decision: "V3 는 기존 V1·V2 관례(BIGSERIAL, TIMESTAMPTZ)를 따른다"
    rationale: "출처 문서 §3 의 IDENTITY·TIMESTAMP 와 다르지만 엔티티(Instant)·기존 마이그레이션과 정합"
  - decision: "검증 오류(400)는 기존 PerformanceExceptionHandler 를 재사용하고 AuthExceptionHandler 는 409·401 만 담당"
    rationale: "@RestControllerAdvice 전역 — 같은 예외를 두 핸들러가 처리하면 모호해진다"

attempts: []
---
