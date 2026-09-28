---
task_id: TASK-006
title: "구역 price/seatsPerRow 정수 강제 검증"
phase: "3"
phase_name: "Phase 3 - Green (완료)"
status: ACTIVE
created_at: 2026-09-18
last_updated: 2026-09-28

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-006-section-numeric-validation"

last_checkpoint: CP-3.4
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-006.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-006.md"
  test: "workflow_design/05_scenario/TEST_TASK-006.json"
  dev: "workflow_design/06_dev/DEV_TASK-006.json"
---

## 지금 무엇을 하고 있나

Phase 3 완료. PerformanceService.validateSection에 isIntegral() 정수 확인 추가
(F1/F2 구현), 검증 통과 후 변환은 intValueExact()로 교체. 전체 테스트 50/50
통과(대상 3건 포함), spotlessCheck error 0, 리팩토링 불필요 판단. 다음은
Phase 4(Verify)다.

## 다음 한 걸음

`wf-verify` 스킬로 넘겨 Phase 4를 진행한다.

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
- Phase 2b에서 실제로 확인: PerformanceController.java는 컴파일 시점에 변경 불필요했음(예상 적중)
- Phase 3에서 할 일: PerformanceService.validateSection에 "정수인지" 확인을
  null/범위 확인보다 먼저 추가(stripTrailingZeros 후 scale>0이면 InvalidSectionException).
  price는 F1(SC-01), seatsPerRow는 F2(SC-02)가 검증. 기존 .intValue() 절삭 코드는
  검증 통과 후의 안전한 변환으로 남겨도 되고, intValueExact()로 바꿔도 됨(검증 후라
  동일한 결과) — Phase 3 판단
- SC-03(already_passing)이 Phase 3 이후에도 계속 통과하는지가 회귀 확인 기준
