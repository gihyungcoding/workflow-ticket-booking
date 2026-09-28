---
task_id: TASK-006
title: "구역 price/seatsPerRow 정수 강제 검증"
phase: "2a"
phase_name: "Phase 2a - Scenario Design (완료, HITL#1 승인)"
status: ACTIVE
created_at: 2026-09-18
last_updated: 2026-09-28

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-006-section-numeric-validation"

last_checkpoint: CP-2.4
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-006.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-006.md"
---

## 지금 무엇을 하고 있나

Phase 2a 완료. 시나리오 3건(happy 1 SC-03 / error 2 SC-01·SC-02), 독립검증
2차 PASS(warn 3건 — V7 seat 표현, V9 SC-03 기존 테스트와 중복, V10 regression
타입 0건), HITL#1 승인 받음(warn 3건 인지 상태로 보완 없이 승인). Phase 2a
게이트 7/7 통과. 다음은 Phase 2b(Red 테스트 작성)다.

## 다음 한 걸음

`wf-red` 스킬로 넘겨 Phase 2b를 진행한다.

## 알아둬야 할 것

- 원인은 TASK-004 Phase 4 검증(attempt 3)에서 code-reviewer가 이미 재현·확인함
  (Jackson `ACCEPT_FLOAT_AS_INT` 기본 설정으로 소수가 0 방향 절삭됨). 재조사 불필요
  (`absorbed_steps` 참고)
- price=-0.5 → 0으로 절삭되어 `PerformanceService.validateSection` 의 "price는 0 이상"
  검사를 우회, 무료 좌석 생성
- seatsPerRow=1.9 → 1로 절삭되어 클라이언트 좌석수 미리보기와 서버 실제 생성 좌석수가 어긋남
- 대상 파일: `backend/src/main/java/com/example/ticket_booking/api/dto/SectionRequest.java`,
  `backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java`
- AC 3건: 비정수 price → 400/INVALID_SECTION·좌석 미생성 / 비정수 seatsPerRow →
  400/INVALID_SECTION·좌석 미생성 / 기존 정수 요청은 그대로 201 성공(회귀)
- 위험도 low, 규모 6h(정상 WRU 범위)
- 결제 기능이 아직 없어 크래시·DoS는 아니라는 이유로 TASK-004에서는 WARN 예외 승인하고
  이 태스크로 분리됨 — 이번 태스크에서는 예외 없이 정식으로 막아야 함
- ADR-0010(2026-09-16 채택)이 이 태스크와 직결됨 — DTO shape 검증은 궁극적으로 Bean
  Validation으로 가지만 마이그레이션은 TASK-007(별도 태스크)이 담당하기로 명시적으로
  미뤄져 있다. 그래서 이 태스크는 Bean Validation을 선점 도입하지 않고, 기존 수동
  검증(Service 계층) 관례를 따르되 SectionRequest/SectionSpec의 price/seatsPerRow
  타입을 Integer → BigDecimal로 바꿔 Jackson 절삭 문제 자체를 없앤다 (CP-1.3 결정)
- target_files는 SectionRequest.java + PerformanceService.java뿐 아니라
  SectionSpec.java도 포함 — task의 implementation_spec.paths에는 없었지만 타입을
  맞추려면 필요함을 Phase 1 조사에서 확인함
- PerformanceController.java(toSectionSpec)는 타입만 맞으면 로직 변경 없이
  그대로 컴파일될 것으로 예상 — Phase 3에서 실제 컴파일로 확인 필요
