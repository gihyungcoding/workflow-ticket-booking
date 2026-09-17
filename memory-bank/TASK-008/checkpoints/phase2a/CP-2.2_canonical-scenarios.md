---
checkpoint_id: CP-2.2
checkpoint_name: "Canonical 시나리오 생성 완료"
task_id: TASK-008
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-17T01:40:00Z
status: ACTIVE

work_summary: "시나리오 4건 작성(happy 1 / error 1 / regression 1 / boundary 1), acceptance_criteria 3건 전부 커버"

progress:
  completed:
    - "SC-01~SC-04 작성, 각각 covers/flow 메타데이터 부착"
    - "SCENARIO_TASK-008.json 파생 및 coverage 검증(3/3, uncovered 없음)"
  in_progress: "scenario-validator 서브에이전트 호출 준비"
  blocked: []

next_steps:
  - priority: 1
    task: "scenario-validator 서브에이전트 호출"
    file: "workflow_design/05_scenario/SCENARIO_TASK-008.md"
  - priority: 2
    task: "검증 결과를 validator/VALIDATION_TASK-008.json 에 원본 그대로 저장"

decisions:
  - decision: "SC-04(boundary)를 추가해 PLAN의 unresolved 항목(grade+price 동일한 두 구역의 합산)을 명시적으로 검증한다"
    rationale: "SC-01만으로는 그룹핑 키가 실제로 (grade,price)인지 확인할 수 없다 — 구역 순서나 개수로 잘못 나뉘어도 SC-01은 통과할 수 있다. 이 시나리오가 실패했을 때 SC-01은 통과하면서 이것만 실패하는 결함(예: 구역 인덱스로 그룹핑)이 실제로 가능하다"
    alternatives_considered: ["boundary 생략 — PLAN unresolved로만 남김"]
    impact: "시나리오 수 3 → 4, SC-01/SC-04가 같은 acceptance_criteria를 covers로 공유"

recovery_prerequisites:
  - CP-2.1

execution_context:
  test_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew -p backend test"
  build_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew -p backend build"
  env_required: []
  main_files:
    - "workflow_design/05_scenario/SCENARIO_TASK-008.md"
    - "workflow_design/05_scenario/SCENARIO_TASK-008.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/SCENARIO_TASK-008.md"
    - path: "workflow_design/05_scenario/SCENARIO_TASK-008.json"
---

## 무엇을 했나

PLAN_TASK-008.json의 flow 3개(F1~F3)를 Given/When/Then 시나리오 4건으로 전개했다.
PLAN의 unresolved 항목(같은 grade+price 구역의 합산 동작)을 boundary 시나리오로
승격해 명시적으로 검증하기로 했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/SCENARIO_TASK-008.md` | 시나리오 SoT |
| `workflow_design/05_scenario/SCENARIO_TASK-008.json` | 파생 JSON |

## 재개 방법

1. 위 산출물 파일을 읽는다
2. `scenario-validator` 서브에이전트를 호출한다
3. 결과를 `validator/VALIDATION_TASK-008.json` 에 원본 그대로 저장한다
