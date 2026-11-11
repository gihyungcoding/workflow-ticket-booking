---
status: ACTIVE
saved_at: 2026-11-11T00:00:00Z
summary: "Red 16건 — 16 실패 / 기존 164 통과. 스켈레톤 3파일(PerformanceZone·Repository·Performance 9인자 생성자 UOE). 통합 HITL 대기."

next_steps:
  - "HITL#1·2 통합 승인 → Phase 3"

decisions:
  - decision: "테스트 5파일로 나눔 — ServerTime · ZoneRegistration(SC-02~09) · BrowseResponse(SC-10·15) · NextOpening(SC-11~13) · ZoneMigration(SC-14·16, PostgreSQL)"
    rationale: "입구별 관례(기존 Performance*ApiTest · UsersHandleMigrationTest)"
  - decision: "요청 본문을 support/JsonText 로 손 직렬화"
    rationale: "짝 없는 서로게이트·NUL 을 \\uXXXX 로 서버에 그대로 보내고, 다른 JSON 타입을 Raw 로 끼우기 위해"
  - decision: "next-opening 테스트는 매번 performance·seat·zone 을 비운다"
    rationale: "다른 비트랜잭션 테스트가 남긴 공연이 후보로 끼면 결과가 흔들린다"

attempts: []
---
