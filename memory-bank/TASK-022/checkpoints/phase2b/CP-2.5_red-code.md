---
status: ACTIVE
saved_at: 2026-11-12T00:00:00Z
summary: "Red 15건 — 새 시나리오 Red 11 · already_passing 4(SC-02·11·13·14). 전체 184 중 20 실패(새 11 + 바뀐 승인 테스트 9), 손대지 않은 기존 테스트 전부 통과. 스켈레톤: Performance getOwnerId·소유자 생성자 UOE. 통합 HITL 승인(2026-11-12)"

next_steps:
  - "통합 HITL #1·2 재승인(2회차 RETRY) → Phase 3 sub 정규형 검사 → 자체 리뷰 2R"

decisions:
  - decision: "테스트 4파일 — OwnerMigration(SC-01, PostgreSQL) · Ownership(SC-02~08·11·13·14, MockMvc) · ErrorResponseRegression(SC-12, 실제 서버) · WriteGuardRemoval(SC-09·10·15, 가드 properties ON)"
    rationale: "SC-09 는 컨텍스트 properties 가 달라 클래스를 나눔. SC-10·15 는 같은 컨텍스트에서 확인 가능"
  - decision: "SC-12 를 MockMvc(Ownership) 에서 PerformanceErrorResponseRegressionTest(RANDOM_PORT) 로 옮김"
    rationale: "MockMvc 는 ERROR 디스패치를 재현하지 못한다(TASK-009 주석). 토큰 실린 405·415 가 /error 에서 다시 인증될 위험이 바로 이 표의 관심사"
  - decision: "Performance 소유자 생성자는 ownerId 를 첫 인자로(10인자)"
    rationale: "기존 9인자(…, String posterUrl) 와 null 인자 호출이 모호해지지 않게"
  - decision: "SC-14 의 '응답 cancelled true' 를 status == CANCELLED 로 관찰"
    rationale: "PerformanceResponse 에 cancelled 필드가 없다. 시나리오 문구 결함 — 2b 에서 SCENARIO 를 고치지 않고 HITL 에 올림"

  - decision: "RETRY(1/3) 2b 재진입 — SC-06 k·l 단언 400, SC-05 정리 순서·SC-13 재로그인 하네스 수정"
    rationale: "Phase 3 Green 실행 결과. 구현이 이미 있어 변이 검증 2건으로 가짜 통과가 아님을 확인(TEST_TASK-022.json red_retry)"

  - decision: "RETRY(2/3) 2b 재진입 — SC-05 에 w~ad 8행. 값은 O1 의 실제 id 로 만든다(리터럴 7 이면 표기만 다른 '같은 사용자' 라는 의도가 사라진다)"
    rationale: "Red 관찰 — 6행 201, 2행은 이미 401"

attempts:
  - outcome: "Phase 3 Green 에서 3건 실패 — SC-06 k·l 400, SC-05 FK 위반(정리 순서), SC-13 401(토큰 만료)"
    back_to: "2a (RETRY_SCENARIO 1/3) → 2b"

  - outcome: "Phase 3 자체 리뷰 1R RC-1 — sub 비정규 표기"
    back_to: "2a (RETRY_SCENARIO 2/3) → 2b"
---

# CP-2.5 Red — TASK-022

- `workflow_design/05_scenario/TEST_TASK-022.json`
