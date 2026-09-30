---
checkpoint_id: CP-2.1
checkpoint_name: "Phase 2a 진입 — 신규 시나리오 작성"
task_id: TASK-006
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-18T00:10:00Z
status: ARCHIVED

work_summary: "PLAN_TASK-006.json의 flows 3건(F1/F2/F3)을 시나리오로 옮기는 작업 시작 — 신규 작성, 기존 시나리오 확장 아님"

progress:
  completed:
    - "PLAN_TASK-006.json 로드, flows 3건 확인"
  in_progress: "GWT 시나리오 작성"
  blocked: []

next_steps:
  - priority: 1
    task: "SCENARIO_TASK-006.md 작성"

decisions: []

recovery_prerequisites:
  - CP-1.3

execution_context:
  test_command: "./gradlew test --tests \"*PerformanceRegistrationApiTest*\""
  build_command: "./gradlew build"
  env_required: []
  main_files: []

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/04_plan/PLAN_TASK-006.json"
---

## 무엇을 했나

Phase 1의 flows 3건을 그대로 시나리오로 옮기기로 했다 — 기존 PerformanceRegistrationApiTest.java에
이미 유사한 error 시나리오(rowStart 역순, seatsPerRow=0 등)가 있어 같은 관찰 패턴(400/INVALID_SECTION/
seat 개수 불변)을 재사용한다.

## 산출물

(다음 체크포인트에서)

## 다음 단계

SCENARIO_TASK-006.md 작성.
