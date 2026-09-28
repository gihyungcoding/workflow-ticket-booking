---
checkpoint_id: CP-2.2
checkpoint_name: "Canonical 시나리오 작성 완료"
task_id: TASK-006
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-18T00:15:00Z
status: ACTIVE

work_summary: "시나리오 3건 작성 (error 2 / happy 1), AC 3/3 커버 — 1차 독립검증 FAIL(V2: happy 0건) 반영해 보완 완료"

progress:
  completed:
    - "SC-01 price 비정수 거부 작성 (covers AC1, flow F1)"
    - "SC-02 seatsPerRow 비정수 거부 작성 (covers AC2, flow F2)"
    - "SC-03 정수 요청 성공 작성 (covers AC3, flow F3)"
    - "SCENARIO_TASK-006.md → SCENARIO_TASK-006.json 파생"
    - "1차 독립검증 FAIL(V2) 수신 → SC-03을 regression에서 happy로 재분류"
    - "V5 반영: SC-03 Given에 rowStart=\"A\"/rowEnd=\"A\" 고정, Then을 '좌석 수 10건'으로 확정값화"
    - "V3 부수 지적 반영: SC-01의 price 예시값을 -0.5 → 100.5로 변경 (기존 '0 이상' 규칙과 분리)"
    - "V8 반영: PLAN_TASK-006.json design.inputs에 rowStart/rowEnd를 컨텍스트 값으로 명시"
    - "V9 반영: SC-03이 기존 테스트와 겹치는 이유(타입이 Integer→BigDecimal로 바뀌어 같은 입력도 새 경로를 검증해야 함)를 시나리오 note로 명문화"
  in_progress: "2차 독립검증 대기"
  blocked: []

next_steps:
  - priority: 1
    task: "scenario-validator 서브에이전트 재호출 (2차)"
    file: "workflow_design/05_scenario/SCENARIO_TASK-006.md"
  - priority: 2
    task: "검증 결과를 VALIDATION_TASK-006.json에 원본 그대로 저장 (기존 파일 덮어씀)"

decisions:
  - decision: "SC-03을 regression에서 happy로 재분류하고, 별도 happy 시나리오를 새로 추가하지 않는다"
    rationale: "1차 독립검증(V2)이 이 판단을 명시적으로 반려했다 — 타입은 수치 하한 기준이라 'regression이 happy를 겸한다'는 설명으로 대체되지 않는다. 재분류가 시나리오 내용을 바꾸지 않고 가장 단순하게 게이트를 통과시키는 방법"
    alternatives_considered: ["별도 happy 시나리오 신규 추가(SC-04)", "SC-03 유지하고 이의 제기"]
    impact: "시나리오 3건 유지 (happy 1 / error 2), 개수 하한 충족"
  - decision: "SC-01의 price 예시값을 AC 원문의 -0.5 대신 100.5로 변경"
    rationale: "1차 독립검증 V3가 지적한 대로 -0.5는 '정수 아님'과 '0 미만'을 동시에 위반해 어느 검사가 걸렸는지 구분되지 않는다. AC는 '예시'일 뿐 리터럴 값을 강제하지 않으므로 covers 매핑은 그대로 유지된다"
    alternatives_considered: ["-0.5 유지하고 그대로 진행"]
    impact: "SC-01의 변별력 향상, AC1 커버리지 문구는 변경 없음"

recovery_prerequisites:
  - CP-2.1

execution_context:
  test_command: "./gradlew test --tests \"*PerformanceRegistrationApiTest*\""
  build_command: "./gradlew build"
  env_required: []
  main_files:
    - "backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/SCENARIO_TASK-006.md"
    - path: "workflow_design/05_scenario/SCENARIO_TASK-006.json"
---

## 무엇을 했나

PLAN의 flow 3건을 각 1개씩 시나리오로 옮겼다. 기존 PerformanceRegistrationApiTest.java의
관찰 패턴(상태 코드 + code 필드 + seat 개수 불변)을 그대로 재사용해 새 관례를 만들지 않았다.
SC-03을 happy와 regression을 겸하는 시나리오로 설계한 것이 유일한 판단 지점이라 결정으로
남겼다 — 검증자가 "happy 없음"으로 지적하면 타입만 재분류하거나 근거를 보완한다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/SCENARIO_TASK-006.md` | 시나리오 3건 (SoT) |
| `workflow_design/05_scenario/SCENARIO_TASK-006.json` | MD에서 파생, AC 3/3 커버 |

## 다음 단계

`scenario-validator` 서브에이전트 호출.
