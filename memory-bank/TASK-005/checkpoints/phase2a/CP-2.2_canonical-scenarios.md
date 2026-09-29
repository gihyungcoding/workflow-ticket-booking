---
checkpoint_id: CP-2.2
checkpoint_name: "Canonical 시나리오 생성 완료"
task_id: TASK-005
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-17T00:20:00Z
status: ARCHIVED

work_summary: "TASK-005 시나리오 9건(happy 5 / error 4) 작성, PLAN F1~F7 전부와 acceptance_criteria 8/8 매핑"

progress:
  completed:
    - "SCENARIO_TASK-005.md 작성 — SC-01~SC-09"
    - "SCENARIO_TASK-005.json 파생 — coverage 8/8, uncovered 없음"
  in_progress: "scenario-validator 호출 준비"
  blocked: []

next_steps:
  - priority: 1
    task: "scenario-validator 서브에이전트 호출"
    file: "workflow_design/05_scenario/SCENARIO_TASK-005.md"
  - priority: 2
    task: "검증 결과를 validator/VALIDATION_TASK-005.json 에 원본 그대로 저장"

decisions:
  - decision: "AC8(취소 버튼→다이얼로그→확인→반영)을 SC-08/SC-09 두 시나리오로 분리"
    rationale: "GWT canonical 규칙(When은 하나) — 다이얼로그 표시와 확인 클릭 반영은 서로 다른 When"
    alternatives_considered: ["하나의 시나리오에 두 When 포함"]
    impact: "시나리오 수 8 → 9, 두 시나리오 모두 같은 AC8을 covers"
  - decision: "EMPTY_SECTIONS/INVALID_SECTION/INVALID_REQUEST/DUPLICATE_SEAT_RANGE, 수정화면 404 전용 시나리오를 두지 않음"
    rationale: "PLAN.unresolved에서 이미 AC5의 일반 실패 배너 경로로 흡수하기로 결정했고(CP-1.3), SC-05가 그 코드 경로를 이미 검증한다"
    alternatives_considered: ["코드별 전용 시나리오 4~5건 추가"]
    impact: "시나리오 수를 9건으로 유지 — complexity level '보통' 기준선(4~7건)보다 다소 많으나 AC 8개에 자연스럽게 대응, 중복 없음"

recovery_prerequisites:
  - CP-1.3
  - CP-2.1

execution_context:
  test_command: "cd frontend && npm test"
  build_command: "cd frontend && npm run build"
  env_required: []
  main_files:
    - "workflow_design/05_scenario/SCENARIO_TASK-005.md"
    - "workflow_design/05_scenario/SCENARIO_TASK-005.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/SCENARIO_TASK-005.md"
    - path: "workflow_design/05_scenario/SCENARIO_TASK-005.json"
---

## 무엇을 했나

PLAN의 F1~F7을 9개의 GWT 시나리오로 옮겼다. AC8이 두 단계 행위(다이얼로그 표시,
확인 후 반영)를 하나로 묶고 있어 canonical 규칙에 따라 SC-08/SC-09로 나눴다.
AC 범위 밖으로 판단한 에러 코드들(EMPTY_SECTIONS 등)은 전용 시나리오 없이
SC-05(일반 실패 배너 경로)가 이미 검증하는 코드 경로로 남겨뒀다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/SCENARIO_TASK-005.md` | 시나리오 SoT |
| `workflow_design/05_scenario/SCENARIO_TASK-005.json` | 파생 JSON, coverage 8/8 |

## 재개 방법

1. 위 산출물 파일을 읽는다
2. `scenario-validator` 서브에이전트를 호출한다
3. 결과를 `validator/VALIDATION_TASK-005.json` 에 원본 그대로 저장한다
