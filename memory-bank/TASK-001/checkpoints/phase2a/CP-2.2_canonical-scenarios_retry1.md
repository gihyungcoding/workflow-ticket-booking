---
checkpoint_id: CP-2.2
checkpoint_name: "Canonical 시나리오 재작성 (retry1)"
task_id: TASK-001
phase: "2a"
phase_name: "Phase 2a - Scenario Design (RETRY)"
saved_at: 2026-09-04T06:33:28Z
status: ARCHIVED

work_summary: "Phase 4 REJECT(RETRY_SCENARIO, COVERAGE_INSUFFICIENT) attempt 1에 대응해 시나리오를 11건→18건으로 보강했다. status 필터 UPCOMING/SOLD_OUT/CANCELLED 분기(SC-12~14), 근본 원인이던 openAt<=closeAt 불변조건(SC-15, F8 신설), title/venue 컬럼 길이(SC-16), 두 제약의 양성 대조군(SC-17/18)을 추가했다. Plan에도 F8/AC9/AC10/AC11을 추가했다."

progress:
  completed:
    - "사용자 확인: CLOSED 버그는 openAt<=closeAt 불변조건 추가로 근본 해결(코드 패치 아님), title/venue 길이도 같이 수정"
    - "Plan에 F8(openAt<=closeAt 불변조건), AC9(F8 대응), AC10(컬럼 길이), AC11(UPCOMING — 원래 §4에 누락됐던 것을 발견해 추가) 반영"
    - "SC-12(UPCOMING 필터), SC-13(SOLD_OUT 필터), SC-14(CANCELLED 필터) 작성 — 기존에 필터 경로가 전혀 실행되지 않던 3개 분기"
    - "SC-15(openAt>closeAt 저장 거부), SC-16(title 200자 초과 저장 거부) 작성"
    - "SC-10/SC-11의 Then을 Exception.class에서 DataIntegrityViolationException으로 좁힘 (Phase 4 code_review 지적)"
    - "1차 독립검증 → warn 5건(V1 MD 잔재, V3 AC11 값 단언 부재, V7 예외타입 노출, V8 Plan constraints 목록 누락, V10 양성 대조군 부재)"
    - "V1/V3/V4/V8(입력) 수정, SC-17/SC-18(양성 대조군) 추가, SC-06 픽스처가 F8을 위반하지 않음을 확인해 MD에 기록"
    - "2차 독립검증 → warn 4건(V5/V8 SC-17/18 단언 약함, V7 유지, V10 구조적 한계)"
    - "V5/V8 대응: SC-17/18에 재조회 값 보존 단언 추가, Plan에 '제약을 만족하는 저장 성공' 출력 추가"
  in_progress: "HITL#1 재승인 요청 (V7/V10 잔여 경고를 투명하게 보고)"
  blocked: false

next_steps:
  - priority: 1
    task: "AskUserQuestion으로 HITL#1 재승인 요청 — V7(예외타입 노출, 의도적 유지)과 V10(CLOSED/UPCOMING 상호배타성을 API 경계에서 직접 재현하는 시나리오 없음, F8 불변조건 논증에 의존)을 명시적으로 보고"

decisions:
  - decision: "V10(상호배타성 미검증)은 순수 로직 분리(F9, PerformanceStatusRules) + 단위 테스트로 처리하기로 사용자가 승인"
    rationale: "F8이 성립하면 상호배타성을 깨는 데이터 자체가 API로 만들어질 수 없어 HTTP 경계의 단일 GWT 시나리오로는 구조적으로 재현 불가능하다는 것을 사용자에게 설명했다. 대안으로 순수 함수(of/matches)를 뽑아 Spring 컨텍스트 없이 5개 상태의 상호배타성 자체를 단위 테스트로 보장하는 방법을 제시했고 사용자가 이를 선택했다. 단, 이 방식도 JPA Specification과 순수 로직 사이의 런타임 동치까지는 보장하지 못한다는 한계를 Plan(F9)에 명시했다"
    alternatives_considered: ["현재 수준에서 멈추고 잔여 위험으로 남김 (사용자가 선택하지 않음)", "두 번의 GET 호출을 하나의 시나리오에 넣기(When 단일성 위반이라 기각)"]
    impact: "PerformanceStatusRules.java를 Plan target_files/F9로 추가. 이 단위 테스트는 승인된 SC-01~18 어디에도 대응하지 않는 예외로 Phase 2b에서만 기록 — SCENARIO_TASK-001.md '인벤토리 예외' 섹션 참고"

recovery_prerequisites:
  - CP-2.1

execution_context:
  test_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew clean test"
  build_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew build"
  env_required: ["JAVA_HOME을 JDK 21 이상으로 설정"]
  main_files:
    - "workflow_design/05_scenario/SCENARIO_TASK-001.md"
    - "workflow_design/05_scenario/SCENARIO_TASK-001.json"
    - "workflow_design/04_plan/PLAN_TASK-001.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/SCENARIO_TASK-001.md"
    - path: "workflow_design/05_scenario/SCENARIO_TASK-001.json"
---

## 무엇을 했나

Phase 4에서 REJECT된 커버리지 공백을 시나리오 18건으로 보강했다. 근본
원인이었던 "openAt<=closeAt 불변조건 부재"는 코드를 고치는 대신 새 제약으로
막기로 사용자와 합의했고, 이 결정에 따라 Plan에 F8/AC9~11을 추가했다. 독립
검증을 2라운드 거치며 5건 → 4건으로 경고를 줄였고, 남은 2건(예외 타입 노출은
의도적 유지, 상호배타성 미검증은 GWT 구조적 한계)은 고치지 않고 사용자에게
투명하게 보고하기로 했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/SCENARIO_TASK-001.md` | 시나리오 SoT (18건) |
| `workflow_design/05_scenario/SCENARIO_TASK-001.json` | 파생 JSON |
| `workflow_design/04_plan/PLAN_TASK-001.json` | F8/AC9~11 반영 |

## 재개 방법

1. `AskUserQuestion` 으로 HITL#1 재승인을 받는다 (V7/V10 잔여 경고 보고 포함)
2. 승인 시 `generate_red_trigger=true`, `CP-2.4_hitl1-approved_retry1.md` 저장
