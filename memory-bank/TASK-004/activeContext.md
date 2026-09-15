---
task_id: TASK-004
title: "공연 등록/수정/취소 API"
phase: "4"
phase_name: "Phase 4 - Verify"
status: ACTIVE
created_at: 2026-09-11
last_updated: 2026-09-15

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-004-performance-registration-api"

last_checkpoint: CP-3.4
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-004.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-004.md"
  test: "backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java"
  dev: "workflow_design/06_dev/DEV_TASK-004.json"
---

## 지금 무엇을 하고 있나

Phase 3(Green)을 완료했다. `PerformanceService.registerPerformance/updatePerformance/
cancelPerformance` 와 `Performance.updateSchedule/cancel` 을 구현해 전체 테스트
34/34 통과(신규 14 + 기존 20), 린트(spotlessCheck) error 0. 리팩토링은 필요 없다고
판단했다.

## 다음 한 걸음

`wf-verify` 스킬로 Phase 4를 시작한다 — acceptance_criteria 9건 충족 여부,
아키텍처 제약(check_architecture.py), 범위 이탈(scope_deviations 1건: SeatRepository
생성자 주입)을 확인하고 HITL#3 승인을 받는다.

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
