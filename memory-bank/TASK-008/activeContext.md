---
task_id: TASK-008
title: "공연 상세 API에 구역별 좌석 구성 요약 추가"
phase: "1"
phase_name: "Phase 1 - Plan"
status: ACTIVE
created_at: 2026-09-17
last_updated: 2026-09-17

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-008-performance-section-summary-api"

last_checkpoint: null
artifacts: {}
---

## 지금 무엇을 하고 있나

워크플로우를 막 시작했다. TASK-005(공연 등록/수정 화면) Phase 2a 시나리오 검증
중 발견된 백엔드 데이터 공백(구역별 좌석 구성 조회 API 부재)을 메우는 선행
태스크다. memory-bank 생성과 브랜치 분기까지 마쳤고, 이제 `wf-plan` 스킬로
Phase 1을 시작한다.

## 다음 한 걸음

`wf-plan` 스킬을 호출해 `PerformanceResponse.java`, `PerformanceService.java`,
`SeatRepository.java` 를 조사하고, GET /api/performances/{id} 응답에 추가할
`sections` 필드(구역별 grade·price·좌석수)의 설계를 `PLAN_TASK-008.json` 으로
정규화한다.

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
