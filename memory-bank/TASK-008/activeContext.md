---
task_id: TASK-008
title: "공연 상세 API에 구역별 좌석 구성 요약 추가"
phase: "5"
phase_name: "Phase 5 - Reflect"
status: DONE
created_at: 2026-09-17
last_updated: 2026-09-17

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-008-performance-section-summary-api"

last_checkpoint: CP-5.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-008.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-008.md"
  test: "workflow_design/05_scenario/TEST_TASK-008.json"
  dev: "workflow_design/06_dev/DEV_TASK-008.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-008.json"
  reflect: "workflow_design/08_reflect/REFLECT_TASK-008.json"

pull_request:
  number: 6
  url: "https://github.com/gihyungcoding/workflow-ticket-booking/pull/6"
  base: develop
  opened_at: 2026-09-17
---

## 지금 무엇을 하고 있나

완료됐다. Phase 1~5를 모두 거쳤고 HITL 4건 전부 승인받았다. status: PASS
(Phase 4), 회고 승인(Phase 5). verified_commit = 460533b.

## 다음 한 걸음

`/wf-ship` 으로 머지를 준비한다. 규칙 개선안 3건(REFLECT_TASK-008.json의
rule_proposals)은 사용자 승인에 따라 이 세션에서 이어서 wf-plan/wf-red
SKILL.md에 직접 반영한다(별도 커밋).

## 알아둬야 할 것

- 이 태스크가 끝나 TASK-005(BLOCKED)가 재개 가능해졌다 — unblock_condition:
  실제 확정된 sections 응답 스키마(grade/price/seatCount, ORDER BY로 정렬 보장됨)를
  TASK-005의 Plan/시나리오에 반영
- `PerformanceResponse`를 직접 생성하는 곳은 `PerformanceService.java` 한 곳뿐이다
- GET /api/performances(목록) 응답에는 sections가 없다(acceptance_criteria 3)
- backend 명령은 JAVA_HOME을 JDK 21로 지정해야 한다 (기본 java는 8)
