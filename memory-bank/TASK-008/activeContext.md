---
task_id: TASK-008
title: "공연 상세 API에 구역별 좌석 구성 요약 추가"
phase: "4"
phase_name: "Phase 4 - Verify"
status: ACTIVE
created_at: 2026-09-17
last_updated: 2026-09-17

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-008-performance-section-summary-api"

last_checkpoint: CP-3.4
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-008.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-008.md"
  test: "workflow_design/05_scenario/TEST_TASK-008.json"
  dev: "workflow_design/06_dev/DEV_TASK-008.json"
---

## 지금 무엇을 하고 있나

Phase 3(Green)을 마쳤다. SectionSummaryResponse/SeatSectionCount 신규 작성,
SeatRepository.findSectionCounts 추가, PerformanceResponse.sections 필드 추가,
PerformanceService.getPerformance만 수정(목록/등록/수정/취소는 그대로). 전체
테스트 47/47 통과, spotlessCheck error 0, 아키텍처 제약 위반 0. 이제 `wf-verify`
스킬로 Phase 4를 시작한다.

## 다음 한 걸음

`wf-verify` 스킬을 호출해 acceptance_criteria 3건 + 회귀(SC-02/SC-03 여전히
통과) + code-reviewer 소견을 근거로 완료를 판정하고 HITL#3을 받는다.

## 알아둬야 할 것

- 이 태스크가 끝나면 TASK-005(BLOCKED)가 재개 가능해진다 — unblock_condition:
  실제 확정된 sections 응답 스키마(grade/price/seatCount)를 TASK-005의 Plan/시나리오에
  반영
- SC-02/SC-03(already_passing)이 Phase 4에서도 계속 통과하는지 다시 확인 필요
  (회귀 가드의 실효성 검증)

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
