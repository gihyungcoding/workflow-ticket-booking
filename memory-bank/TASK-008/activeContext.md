---
task_id: TASK-008
title: "공연 상세 API에 구역별 좌석 구성 요약 추가"
phase: "3"
phase_name: "Phase 3 - Green"
status: ACTIVE
created_at: 2026-09-17
last_updated: 2026-09-17

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-008-performance-section-summary-api"

last_checkpoint: CP-2.6
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-008.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-008.md"
  test: "workflow_design/05_scenario/TEST_TASK-008.json"
---

## 지금 무엇을 하고 있나

Phase 2b를 마쳤다. `PerformanceSectionSummaryApiTest.java`에 시나리오 4건을
1:1로 옮겼다 — SC-01/SC-04는 sections가 없어 진짜로 실패(Red), SC-02/SC-03은
이 태스크가 건드리지 않는 기존 동작(404, 목록 응답 형태)의 회귀 가드라 구현
전에도 이미 통과한다(already_passing, TASK-001 선례와 동일 패턴). HITL#2 승인
완료. 이제 `wf-develop` 스킬로 Phase 3(Green)을 시작한다.

## 다음 한 걸음

`wf-develop` 스킬을 호출해 SC-01/SC-04를 통과시키는 최소 구현을 한다 —
`SectionSummaryResponse`(service, 신규), `SeatSectionCount`(repository 프로젝션,
신규), `SeatRepository.findSectionCounts`(신규 @Query), `PerformanceResponse.
sections` 필드(+@JsonInclude NON_NULL), `PerformanceService.getPerformance`만
수정(목록/등록/수정/취소는 건드리지 않는다 — CP-2.3 범위 축소 결정).

## 알아둬야 할 것

- SC-02/SC-03은 Green 단계에서도 계속 통과해야 하는 회귀 가드다 — 구현이
  실수로 목록 경로에도 sections를 채우면 SC-03이 즉시 깨진다
- PerformanceResponse를 직접 생성하는 곳은 PerformanceService.toResponse()
  한 곳뿐이라 필드 추가 파급은 작지만, getPerformance만 별도 분기가 필요하다

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
