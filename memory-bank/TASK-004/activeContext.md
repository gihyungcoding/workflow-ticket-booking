---
task_id: TASK-004
title: "공연 등록/수정/취소 API"
phase: "2b"
phase_name: "Phase 2b - Red"
status: ACTIVE
created_at: 2026-09-11
last_updated: 2026-09-14

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-004-performance-registration-api"

last_checkpoint: CP-2.4
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-004.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-004.md"
---

## 지금 무엇을 하고 있나

Phase 2a(Scenario)를 완료했다. 시나리오 15건(happy 3 / error 9 / boundary 2 /
regression 1)이 acceptance_criteria 9/9을 전부 커버하고, 독립검증 3회 끝에
overall.pass=true(경고 2건, 경미)로 수렴해 HITL#1 승인을 받았다.

## 다음 한 걸음

`wf-red` 스킬로 Phase 2b를 시작한다 — SC-01~SC-15를 실패하는 테스트 코드로 옮긴다.
PerformanceApiTest.java에 이어서 작성하고, MutableClock/ClockTestConfig 패턴을
재사용한다.

## 알아둬야 할 것

- ADR-0009: 좌석은 구역(등급)×행×열 입력으로 서버가 개별 `seat` 행을 생성한다 — 이 태스크의
  핵심 데이터 모델 결정
- 주최자 인증/소유권 확인은 이번 태스크 범위 밖이다 (`performance-registration.md` §7)
- 시각 순서(`openAt <= closeAt <= startAt`)와 좌석 총수(≤5,000) 검증은 DB 제약이 아니라
  서비스 계층 검증이다 (`performance-registration.md` §1-3)
- 취소 응답은 `cancelled` 필드를 새로 노출하지 않는다 — `PerformanceResponse` 에는
  `status` 만 있고, 취소되면 `PerformanceStatusRules` 가 이를 최우선으로 `CANCELLED` 로
  계산한다. Red 테스트는 `status == CANCELLED` 를 단언한다 (Phase 2a 검증에서 발견,
  CP-2.3 참고)
- 행 범위 겹침 판정은 `[rowStart, rowEnd]` 알파벳 구간의 교집합만으로 본다 (좌석 번호
  조합은 보지 않음). `rowStart`/`rowEnd` 는 A~Z 단일 대문자만 지원한다
- 기존 `performance` 테이블 스키마는 바뀌지 않는다 — `seat` 테이블만 신설
