---
task_id: TASK-001
title: "공연 목록/상세 조회 API"
phase: "1"
phase_name: "Phase 1 - Plan"
status: ACTIVE
created_at: 2026-09-03
last_updated: 2026-09-03

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-001-performance-list-detail-api"

last_checkpoint: —
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-001.json"
---

## 지금 무엇을 하고 있나

TASK-001 워크플로우를 시작했다. memory-bank 생성과 브랜치 분기까지 마쳤고,
Phase 1(`wf-plan`)로 진입하려는 참이다.

## 다음 한 걸음

`wf-plan` 스킬로 코드베이스를 조사해 `performance` 테이블/Repository/Service/
Controller 의 입력·출력·흐름을 확정하고 `PLAN_TASK-001.json` 을 작성한다.

## 알아둬야 할 것

- 예매 상태(UPCOMING/OPEN/SOLD_OUT/CLOSED/CANCELLED)는 DB 컬럼이 아니라
  `java.time.Clock` 주입 기반 시각과 `availableSeats` 로 계산한다 (ADR-0005,
  SQL `now()` 사용 금지)
- 목록은 항상 `start_at >= now` 조건이 상태 필터와 별개로 적용된다
- 이 태스크는 읽기 전용 API만 다룬다 — 공연 등록/수정(쓰기)과 좌석 단위 데이터는
  범위 밖 (`docs/product/features/performance-availability.md` §6)
- 마이그레이션 도구 미정 — Flyway 권장 (동 문서 §5)
- TASK-002(프론트 목록/상세 화면)가 이 태스크에 의존한다
