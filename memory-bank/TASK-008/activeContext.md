---
task_id: TASK-008
title: "공연 상세 API에 구역별 좌석 구성 요약 추가"
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
status: ACTIVE
created_at: 2026-09-17
last_updated: 2026-09-17

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-008-performance-section-summary-api"

last_checkpoint: CP-1.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-008.json"
---

## 지금 무엇을 하고 있나

Phase 1(Plan)을 마쳤다. route=Backend, flows 3개(F1~F3)가 acceptance_criteria
3건을 전부 커버한다. 이제 `wf-scenario` 스킬로 Phase 2a를 시작한다.

## 다음 한 걸음

`wf-scenario` 스킬을 호출해 PLAN_TASK-008.json의 F1~F3을 Given/When/Then
시나리오로 전개하고, `scenario-validator` 서브에이전트 검증 후 HITL#1을 받는다.

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
