---
task_id: TASK-023
title: "비공개 상태·주최자 조회·공개 전환 API"
phase: "3"
phase_name: "Phase 3 - Green (완료)"
status: ACTIVE
track: standard
created_at: 2026-11-13
last_updated: 2026-11-13

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-023-organizer-visibility-api"

last_checkpoint: "checkpoints/phase3/CP-3.4_context-dev.md"
source: "docs/product/features/organizer-flow.md (Task B)"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-023.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-023.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-023.json"
  test: "workflow_design/05_scenario/TEST_TASK-023.json"
  dev: "workflow_design/06_dev/DEV_TASK-023.json"
---

## 지금 무엇을 하고 있나

Phase 3 Green 완료 — 203/203, spotless 0, 자체 리뷰 1라운드.

## 다음 한 걸음

Phase 4 — `wf-verify`.

## 알아둬야 할 것

- 선행 TASK-022(owner_id·역할 인가)는 done. 기존 행 owner NULL 은 주최자 목록에 나오지 않는다
- 추정 12h → standard 트랙, risk high
- JPA 리뷰 지시(CLAUDE.md): UPDATE 경로(publish/unpublish/수정)는 비트랜잭션 프로브로 DB 반영·거부 시 변경 없음을 확인
