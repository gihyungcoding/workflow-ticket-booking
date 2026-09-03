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

last_checkpoint: CP-1.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-001.json"
---

## 지금 무엇을 하고 있나

Phase 1(Plan)을 완료했다. `PLAN_TASK-001.json` 에 route(Backend), 4계층에 걸친
신규 파일 12건, flow 6개(acceptance_criteria 7건 전부 커버)를 정리했다. EXIT
GATE를 통과했으므로 다음은 Phase 2a(시나리오 설계)다.

## 다음 한 걸음

`wf-scenario` 스킬로 `PLAN_TASK-001.json` 의 flows를 Given/When/Then 시나리오로
옮기고 독립검증자 검증 후 HITL#1 승인을 받는다.

## 알아둬야 할 것

- 예매 상태(UPCOMING/OPEN/SOLD_OUT/CLOSED/CANCELLED)는 DB 컬럼이 아니라
  `java.time.Clock` 주입 기반 시각과 `availableSeats` 로 계산한다 (ADR-0005,
  SQL `now()` 사용 금지)
- 목록은 항상 `start_at >= now` 조건이 상태 필터와 별개로 적용된다
- 이 태스크는 읽기 전용 API만 다룬다 — 공연 등록/수정(쓰기)과 좌석 단위 데이터는
  범위 밖 (`docs/product/features/performance-availability.md` §6)
- 마이그레이션 도구 미정 — Flyway 권장 (동 문서 §5)
- TASK-002(프론트 목록/상세 화면)가 이 태스크에 의존한다
- **실제 패키지 루트는 `com.example.ticket_booking`** — 기능 문서·tasks.json의
  `com.ticketbooking` 은 실재하지 않는 경로다. Phase 2b/3에서 파일을 만들 때
  `PLAN_TASK-001.json` 의 `target_files` 경로(정정됨)를 따른다
- `status` 쿼리 필터는 DB WHERE 절로 구현해야 한다(F3) — 메모리 필터링은
  페이지네이션/totalElements를 깨뜨린다
- **후속 확인 필요**: `constraints.yaml` 의 ARCH-001/002 glob이 `**/controller/**`
  인데 실제 API 계층 위치는 `**/api/**` (architecture.md §2) — 이대로면 Phase 4에서
  이 태스크의 신규 컨트롤러가 검사 대상에서 빠진다. Phase 4 전에 사용자와 확인해
  glob을 정정할지 결정한다
