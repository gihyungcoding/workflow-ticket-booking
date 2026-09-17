---
checkpoint_id: CP-2.3
checkpoint_name: "독립검증 통과"
task_id: TASK-005
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-17T01:00:00Z
status: ACTIVE

work_summary: "scenario-validator 3회 호출 — 매회 overall.pass=true, 경고를 반영해 시나리오/PLAN을 보완했다"

progress:
  completed:
    - "1차 검증: pass, warn 3(V5 Then 관찰불가 3건, V6 When 복합 1건, V10 회귀시나리오 없음) → SC-03/04/05 문구 추가, SC-02 When 분리로 반영"
    - "2차 검증: pass, warn 3(V1 MD/JSON 문구 불일치, V5 잔여 SC-06/SC-09, V10 회귀 재확인) → MD/JSON 정합, SC-06/SC-09 재작성. V10은 사용자에게 AskUserQuestion으로 확인 → '회귀 시나리오 추가' 승인받아 PLAN에 F8 추가 + SC-10 신설"
    - "3차 검증: pass, warn 4(V5/V7 SC-10 컴포넌트명 노출+관찰불가, V8 PLAN outputs 미정의 2건, V10 문서 자기모순) → SC-10 Then 재작성, PLAN outputs에 문구/형식 명시, MD 독립검증 반영 절 전체 재작성"
    - "VALIDATION_TASK-005.json에 3차 결과를 원본 그대로 저장 + post_validation_fixes로 이후 조치 기록"
  in_progress: "HITL#1 승인 요청 준비"
  blocked: []

next_steps:
  - priority: 1
    task: "AskUserQuestion으로 HITL#1 승인 요청"
  - priority: 2
    task: "승인되면 SCENARIO_TASK-005.json human_input.generate_red_trigger = true, CP-2.4 저장"

decisions:
  - decision: "3차 검증까지만 돌리고 남은 warn 4건은 실제 반영했으므로 4차 재검증 없이 HITL#1로 진행"
    rationale: "fail은 한 번도 없었다(전부 pass) — 재시도 3회 상한은 fail 대응 규칙이라 무한 루프를 피하는 것이 목적에 맞다. 남은 지적은 이미 코드/문서로 반영 완료됨"
    alternatives_considered: ["4차 검증 1회 더"]
    impact: "HITL#1 요약에 V8/V10 반영 내역을 명시해 사람이 직접 확인할 수 있게 함"

recovery_prerequisites:
  - CP-2.2

execution_context:
  test_command: "cd frontend && npm test"
  build_command: "cd frontend && npm run build"
  env_required: []
  main_files:
    - "workflow_design/05_scenario/SCENARIO_TASK-005.md"
    - "workflow_design/05_scenario/SCENARIO_TASK-005.json"
    - "workflow_design/05_scenario/validator/VALIDATION_TASK-005.json"
    - "workflow_design/04_plan/PLAN_TASK-005.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/validator/VALIDATION_TASK-005.json"
---

## 무엇을 했나

scenario-validator를 3회 호출했다. 매회 fail 없이 pass였지만 경고를 실제로
반영하며 시나리오와 PLAN을 함께 다듬었다. 가장 중요한 반영은 V10(회귀
누락)이었다 — App.tsx에 정적 라우트(/performances/new)와 기존 동적
라우트(/performances/:id)가 공존하게 되는데 이를 검증하는 시나리오가
없었다. 사용자에게 직접 확인(AskUserQuestion)해 "회귀 시나리오 추가"를
승인받았고, PLAN에 F8을 새로 추가한 뒤 SC-10(regression)으로 반영했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/SCENARIO_TASK-005.md` | 시나리오 SoT (10건) |
| `workflow_design/05_scenario/SCENARIO_TASK-005.json` | 파생 JSON |
| `workflow_design/05_scenario/validator/VALIDATION_TASK-005.json` | 3차 검증 원본 + 이후 조치 기록 |
| `workflow_design/04_plan/PLAN_TASK-005.json` | F8 flow, outputs 문구 보강 (갱신) |

## 재개 방법

1. `SCENARIO_TASK-005.md`와 `VALIDATION_TASK-005.json`을 읽는다
2. HITL#1(AskUserQuestion)로 승인받는다
3. 승인되면 `generate_red_trigger = true`, CP-2.4 저장, 커밋
