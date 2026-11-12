---
status: ACTIVE
saved_at: 2026-11-12T00:00:00Z
summary: "Red 15건 — 새 시나리오 Red 11 · already_passing 4(SC-02·11·13·14). 전체 184 중 20 실패(새 11 + 바뀐 승인 테스트 9), 손대지 않은 기존 테스트 전부 통과. 스켈레톤: Performance getOwnerId·소유자 생성자 UOE. 통합 HITL 대기"

next_steps:
  - "HITL #1·2 통합 승인 — 승인 시 generate_red_trigger·human_review.approved 기록 후 Phase 3(wf-develop)"

decisions:
  - decision: "테스트 4파일 — OwnerMigration(SC-01, PostgreSQL) · Ownership(SC-02~08·11·13·14, MockMvc) · ErrorResponseRegression(SC-12, 실제 서버) · WriteGuardRemoval(SC-09·10·15, 가드 properties ON)"
    rationale: "SC-09 는 컨텍스트 properties 가 달라 클래스를 나눔. SC-10·15 는 같은 컨텍스트에서 확인 가능"
  - decision: "SC-12 를 MockMvc(Ownership) 에서 PerformanceErrorResponseRegressionTest(RANDOM_PORT) 로 옮김"
    rationale: "MockMvc 는 ERROR 디스패치를 재현하지 못한다(TASK-009 주석). 토큰 실린 405·415 가 /error 에서 다시 인증될 위험이 바로 이 표의 관심사"
  - decision: "Performance 소유자 생성자는 ownerId 를 첫 인자로(10인자)"
    rationale: "기존 9인자(…, String posterUrl) 와 null 인자 호출이 모호해지지 않게"
  - decision: "SC-14 의 '응답 cancelled true' 를 status == CANCELLED 로 관찰"
    rationale: "PerformanceResponse 에 cancelled 필드가 없다. 시나리오 문구 결함 — 2b 에서 SCENARIO 를 고치지 않고 HITL 에 올림"

attempts: []
---

# CP-2.5 Red — TASK-022

- `workflow_design/05_scenario/TEST_TASK-022.json`
