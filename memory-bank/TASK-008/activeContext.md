---
task_id: TASK-008
title: "공연 상세 API에 구역별 좌석 구성 요약 추가"
phase: "5"
phase_name: "Phase 5 - Reflect"
status: ACTIVE
created_at: 2026-09-17
last_updated: 2026-09-17

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-008-performance-section-summary-api"

last_checkpoint: CP-4.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-008.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-008.md"
  test: "workflow_design/05_scenario/TEST_TASK-008.json"
  dev: "workflow_design/06_dev/DEV_TASK-008.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-008.json"
---

## 지금 무엇을 하고 있나

Phase 4(Verify)를 마쳤다 — status: PASS. code-reviewer가 GROUP BY 결과 순서
비결정성 결함을 찾아 Phase 3으로 되돌려 즉시 수정(ORDER BY 추가, 커밋 460533b)
했고, 재검증 후 HITL#3 승인을 받았다. verified_commit = 460533b. 이제
`wf-reflect` 스킬로 Phase 5를 시작한다.

## 다음 한 걸음

`wf-reflect` 스킬을 호출해 KPT 회고를 하고 HITL#4 승인을 받아 태스크를 닫는다.
Keep/Try 후보: (1) Repository 프로젝션/Service DTO 분리 패턴 — 재사용 가치 있음,
(2) code-reviewer의 test-coverage 소견(POST→GET 연계 테스트 부재)을 Try로 남긴다,
(3) Phase 2a에서 발견한 백엔드 데이터 공백(TASK-005 차단) 같은 Plan-Scenario
교차검증 패턴을 규칙으로 승격할지 검토.

## 알아둬야 할 것

- 이 태스크가 끝나면 TASK-005(BLOCKED)가 재개 가능해진다 — unblock_condition:
  실제 확정된 sections 응답 스키마(grade/price/seatCount, ORDER BY로 정렬 보장됨)를
  TASK-005의 Plan/시나리오에 반영

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
