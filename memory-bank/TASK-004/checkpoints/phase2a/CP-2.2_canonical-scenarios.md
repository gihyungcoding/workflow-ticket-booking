---
checkpoint_id: CP-2.2
checkpoint_name: "Canonical 시나리오 생성 완료"
task_id: TASK-004
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-14T00:00:00Z
status: ACTIVE

work_summary: "PLAN_TASK-004.json의 flow 9건(F1~F9)을 시나리오 13건(happy 3 / error 9 / boundary 1)으로 옮김. acceptance_criteria 9/9 커버"

progress:
  completed:
    - "SCENARIO_TASK-004.md 작성 — SC-01~SC-13"
    - "SCENARIO_TASK-004.json 파생 — coverage.uncovered 빈 배열 확인"
  in_progress: "scenario-validator 서브에이전트 호출 준비"
  blocked: []

next_steps:
  - priority: 1
    task: "scenario-validator 서브에이전트 호출"
    file: "workflow_design/05_scenario/SCENARIO_TASK-004.md"
  - priority: 2
    task: "검증 결과를 VALIDATION_TASK-004.json 에 원본 그대로 저장"

decisions:
  - decision: "AC3(openAt>closeAt 또는 closeAt>startAt)를 SC-02/SC-03 두 시나리오로 분리했다"
    rationale: "두 비교가 코드에서 별개의 조건문으로 구현될 가능성이 높고(AND/OR 혼동 같은 독립적 버그가 가능), TASK-001에서 openAt<=closeAt 불변조건 관련 결함이 실제로 Phase 4에서 발견된 전례가 있어 이 영역은 보수적으로 갈랐다"
    alternatives_considered: ["하나의 시나리오로 통합"]
    impact: "시나리오 수 +1"
  - decision: "시나리오가 13건으로 '복잡' 등급 기준선(8~12건)을 1건 초과한다"
    rationale: "3개 엔드포인트(등록/수정/취소) × 각각의 정상/오류 분기 + 좌석 겹침/상한 + 멱등성 + 빈 구역까지, AC 9건 자체가 이미 분기가 많다. SC-08(수정 시 시각순서 재검증)과 SC-13(빈 구역)은 AC에는 없지만 Plan에 이미 명시된 동작이라 생략하지 않았다"
    alternatives_considered: ["SC-08/SC-13 생략(11건으로 축소)", "SC-02/SC-03 병합(12건으로 축소)"]
    impact: "HITL#1 보고 시 사용자에게 이 초과분을 투명하게 알리고 트리밍 여부를 확인한다"

recovery_prerequisites:
  - CP-1.3

execution_context:
  test_command: "cd backend && ./gradlew test"
  build_command: "cd backend && ./gradlew build"
  env_required: []
  main_files:
    - "workflow_design/05_scenario/SCENARIO_TASK-004.md"
    - "workflow_design/05_scenario/SCENARIO_TASK-004.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/04_plan/PLAN_TASK-004.json"
---

## 무엇을 했나

PLAN_TASK-004.json의 9개 flow를 Given/When/Then 시나리오 13건으로 옮겼다. acceptance_criteria
9건을 전부 커버했고, Plan에 명시됐지만 AC 텍스트엔 없는 동작 2건(수정 시 시각순서 재검증,
빈 구역 거부)도 시나리오로 남겼다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/SCENARIO_TASK-004.md` | 시나리오 SoT |
| `workflow_design/05_scenario/SCENARIO_TASK-004.json` | 파생 JSON |

## 재개 방법

1. 위 산출물 파일을 읽는다
2. `scenario-validator` 서브에이전트를 호출한다
3. 결과를 `workflow_design/05_scenario/validator/VALIDATION_TASK-004.json` 에 저장한다
