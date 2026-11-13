---
task_id: TASK-023
title: "비공개 상태·주최자 조회·공개 전환 API"
phase: "4"
phase_name: "Phase 4 - Verify (FAIL — 결정 대기)"
status: ACTIVE
track: standard
created_at: 2026-11-13
last_updated: 2026-11-13

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-023-organizer-visibility-api"

last_checkpoint: "checkpoints/phase4/CP-4.2_verification.md"
source: "docs/product/features/organizer-flow.md (Task B)"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-023.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-023.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-023.json"
  test: "workflow_design/05_scenario/TEST_TASK-023.json"
  dev: "workflow_design/06_dev/DEV_TASK-023.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-023.json"
---

## 지금 무엇을 하고 있나

Phase 4 1회차 FAIL. 코드 리뷰 [correctness] 2건(RC-1 동시 변경 덮어쓰기, RC-2 관객 목록 page·size).

## 다음 한 걸음

사용자 결정 후 롤백 지점(2a/3) 확정.

## 알아둬야 할 것

- 선행 TASK-022(owner_id·역할 인가)는 done. 기존 행 owner NULL 은 주최자 목록에 나오지 않는다
- 추정 12h → standard 트랙, risk high
- JPA 리뷰 지시(CLAUDE.md): UPDATE 경로(publish/unpublish/수정)는 비트랜잭션 프로브로 DB 반영·거부 시 변경 없음을 확인
