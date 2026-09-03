---
task_id: TASK-001
title: "공연 목록/상세 조회 API"
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
status: ACTIVE
created_at: 2026-09-03
last_updated: 2026-09-03

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-001-performance-list-detail-api"

last_checkpoint: CP-2.4
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-001.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-001.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-001.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-001.json"
---

## 지금 무엇을 하고 있나

Phase 2a(시나리오 설계)를 완료하고 HITL#1 승인을 받았다. 시나리오 11건(happy 4 /
boundary 4 / error 3)이 acceptance_criteria 8/8(최초 7건 + 사용자 요청으로 추가한
NOT NULL/CHECK 제약 검증 1건)을 커버한다. 독립검증자를 4회 호출해 통과시켰다.
EXIT GATE 통과, 다음은 Phase 2b(Red 테스트 작성)다.

## 다음 한 걸음

`wf-red` 스킬로 `SCENARIO_TASK-001.md`의 11개 시나리오를 실패하는 테스트 코드로
옮기고 HITL#2 승인을 받는다.

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
- **Plan이 Phase 2a 중 개정됨**: F7(엔티티 제약-마이그레이션 제약 일치)과 AC8을
  사용자 요청으로 추가했다 — `PLAN_TASK-001.json.amendments` 참고. Performance
  엔티티는 V1 마이그레이션의 NOT NULL/CHECK를 애노테이션으로도 표현해야 하고,
  Phase 2b는 SC-10/SC-11(저장이 "DB에 실제로 반영되는 시점까지" 거부되는지)을
  `@DataJpaTest`로 옮겨야 한다
- SC-07/SC-08은 Clock 고정 시각을 **실제 시스템 시각과 다르게**(예: 몇 년 뒤) 잡아야
  한다 — 그래야 구현이 Clock 대신 SQL now()/시스템 시각을 쓰는 결함을 status 값
  자체로 잡아낼 수 있다
