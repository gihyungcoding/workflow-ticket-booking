---
task_id: TASK-008
title: "공연 상세 API에 구역별 좌석 구성 요약 추가"
phase: "2b"
phase_name: "Phase 2b - Red"
status: ACTIVE
created_at: 2026-09-17
last_updated: 2026-09-17

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-008-performance-section-summary-api"

last_checkpoint: CP-2.4
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-008.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-008.md"
---

## 지금 무엇을 하고 있나

Phase 2a를 마쳤다. 시나리오 4건(SC-01~04), 독립검증 PASS(경고 2건은 Plan 범위를
좁혀 해소), HITL#1 승인 완료. Plan을 acceptance_criteria 3건 범위로 좁혀
등록/수정/취소 응답은 건드리지 않기로 했다 — sections는 GET /api/performances/{id}
(상세) 한 곳에서만 채운다. 이제 `wf-red` 스킬로 Phase 2b를 시작한다.

## 다음 한 걸음

`wf-red` 스킬을 호출해 SC-01~04를 실패하는 테스트로 옮긴다. 기존
PerformanceApiTest.java/PerformanceRegistrationApiTest.java 의 SpringBootTest+
MockMvc+H2 패턴을 따른다.

## 알아둬야 할 것

- TASK-005가 이 태스크의 완료를 기다리고 있다 (`memory-bank/TASK-005/activeContext.md`
  status=BLOCKED, unblock_condition 참고)
- 좌석은 이미 `generateSeats()`가 grade/price를 담아 개별 행으로 생성해뒀다 —
  새 마이그레이션 없이 seat 테이블을 (performance_id, grade, price)로 그룹핑해
  집계하면 된다
- `PerformanceResponse`를 직접 생성(`new PerformanceResponse(...)`)하는 곳은
  `PerformanceService.java` 한 곳뿐이다 — 필드 추가로 인한 파급이 작다
- GET /api/performances(목록) 응답에는 sections를 넣지 않는다(acceptance_criteria 3)
- backend 명령은 JAVA_HOME을 JDK 21로 지정해야 한다 (기본 java는 8)
