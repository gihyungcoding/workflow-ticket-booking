---
status: ACTIVE
saved_at: 2026-10-14T08:00:00Z
summary: "Green — 전체 89/89 통과(대상 23 + 기존 66), spotlessCheck 0, 범위 이탈 2건 기록. Phase 4 대기."

next_steps:
  - "Phase 4 — wf-verify (check_architecture DEPLOY-002, code-reviewer, JPA 지시 포함)"

decisions:
  - decision: "필터 와이어링은 FilterRegistrationBean 대신 @Bean + @Order 로 한다"
    rationale: "Boot 가 Filter 빈을 자동 등록하고 @Order 를 따른다 — 패키지 이동 위험 없이 순서를 보장"
  - decision: "server.port 를 classpath:/config/application.properties 에 둔다"
    rationale: "test resources 의 application.properties 가 main 것을 가려 SC-09/10 이 null. 테스트를 고치지 않기 위해 위치를 옮겼다 (scope_deviations 참고)"
  - decision: "401 코드는 WRITE_KEY_INVALID, 키 비교는 MessageDigest.isEqual"
    rationale: "기존 ErrorResponse 코드 관례(대문자 스네이크)를 따르고 타이밍 차이를 줄인다"
  - decision: "리팩토링 하지 않음"
    rationale: "중복·책임 혼재 없음"

attempts: []
---
