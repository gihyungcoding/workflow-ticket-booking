---
checkpoint_id: CP-2.1
checkpoint_name: "시나리오 경로 확정"
task_id: TASK-008
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-17T01:30:00Z
status: ARCHIVED

work_summary: "SCENARIO_TASK-008.md/.json 신규 작성 — 기존 시나리오 확장 아님"

progress:
  completed:
    - "PLAN_TASK-008.json 의 flows(F1~F3)를 시나리오 재료로 확인"
    - "TASK-005 Phase 2a 검증에서 나온 경고(V5 문구 미특정, V6 When 이중동작, V7 구현 누출)를 이번 시나리오 작성 시 미리 피함 — 응답 필드 값을 구체적으로 명시, When을 단일 HTTP 호출로 유지"
  in_progress: "SC-01~04 작성"
  blocked: []

next_steps:
  - priority: 1
    task: "SCENARIO_TASK-008.md 작성"

decisions: []

recovery_prerequisites:
  - CP-1.3

execution_context:
  test_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew -p backend test"
  build_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew -p backend build"
  env_required: []
  main_files: []

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/04_plan/PLAN_TASK-008.json"
---

## 무엇을 했나

TASK-008은 신규 태스크라 기존 시나리오가 없다. PLAN의 flows 3개를 시나리오 재료로
삼아 새 SCENARIO 문서를 작성하기로 했다. 같은 세션에서 방금 겪은 TASK-005의
scenario-validator 경고 패턴(문구 미특정, When 이중동작)을 이번에는 처음부터
피하기로 했다.

## 산출물

| 파일 | 역할 |
|---|---|
| (다음 CP에서 생성) `workflow_design/05_scenario/SCENARIO_TASK-008.md` | 시나리오 SoT |

## 재개 방법

1. PLAN_TASK-008.json의 flows를 하나씩 GWT로 전개한다
