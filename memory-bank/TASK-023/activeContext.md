---
task_id: TASK-023
title: "비공개 상태·주최자 조회·공개 전환 API"
phase: "1"
phase_name: "Phase 1 - Plan"
status: ACTIVE
track: standard
created_at: 2026-11-13
last_updated: 2026-11-13

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-023-organizer-visibility-api"

last_checkpoint: none
source: "docs/product/features/organizer-flow.md (Task B)"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-023.json"
---

## 지금 무엇을 하고 있나

워크플로우를 시작했다. Phase 1(Plan)에 진입한다.

## 다음 한 걸음

`wf-plan` 으로 organizer-flow.md §3·§4·§6 과 현재 코드(V7, PerformanceService 등)를 조사해 PLAN_TASK-023.json 을 만든다.

## 알아둬야 할 것

- 선행 TASK-022(owner_id·역할 인가)는 done. 기존 행 owner NULL 은 주최자 목록에 나오지 않는다
- 추정 12h → standard 트랙, risk high
- JPA 리뷰 지시(CLAUDE.md): UPDATE 경로(publish/unpublish/수정)는 비트랜잭션 프로브로 DB 반영·거부 시 변경 없음을 확인
