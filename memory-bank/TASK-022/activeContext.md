---
task_id: TASK-022
title: "공연 소유자·역할 인가, 쓰기 가드 제거"
phase: "5"
phase_name: "Phase 5 - Reflect"
status: DONE
track: standard
created_at: 2026-11-11
last_updated: 2026-11-12

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-022-owner-role-auth"

last_checkpoint: "checkpoints/phase5/CP-5.2_context-reflect.md"
source: "docs/product/features/organizer-flow.md (Task A)"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-022.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-022.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-022.json"
  test: "workflow_design/05_scenario/TEST_TASK-022.json"
  dev: "workflow_design/06_dev/DEV_TASK-022.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-022.json"
  reflect: "workflow_design/08_reflect/REFLECT_TASK-022.json"
---

## 지금 무엇을 하고 있나

DONE (2026-11-12). Phase 4 예외 승인(verified_commit 8ea4ac9), 회고 작성 — 승인 대기(/wf-retro).

## 다음 한 걸음

`/wf-ship` 으로 PR. TASK-024 와 같은 배포로(체크리스트 §5-1). 회고 승인은 `/wf-retro`.

## 알아둬야 할 것

- 근거: ADR-0029(쓰기 가드 제거, 역할·소유권, 소유권 위반 404), ADR-0013(SecurityConfig 단일 지점), ADR-0017(대체됨)
- backend 는 `JAVA_HOME` 을 JDK 21 로 (`/usr/libexec/java_home -v 21`)
- 제약 AUTH-001 은 warn 이고 `promote_after: TASK-024` — 이 태스크로는 백엔드 위반만 사라진다. 프론트 `VITE_WRITE_KEY` 는 TASK-024 범위
- 거부 경로(401·403·404)는 "DB 변경 없음"을 비트랜잭션으로 확인한다 (CLAUDE.md 리뷰 지시)
- 범위 외: 비공개 상태·주최자 조회·공개 전환(TASK-023), 프론트 토큰 전송(TASK-024), 입금 확인 API 보호 방식
- develop 의 프론트 테스트 1건(tokens.ts `--c-draft`)은 PR #43 으로 따로 고친다 — 이 브랜치 범위 아님
- 미반영 회고 제안 3건은 대상이 모두 달라(각 1건) 시작 전 경고 대상 아님
