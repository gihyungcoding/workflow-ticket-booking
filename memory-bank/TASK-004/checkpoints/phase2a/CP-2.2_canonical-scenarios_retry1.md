---
checkpoint_id: CP-2.2
checkpoint_name: "Canonical 시나리오 생성 완료 (재시도)"
task_id: TASK-004
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-15T02:00:00Z
status: ACTIVE

work_summary: "Phase 4 FAIL(RETRY_SCENARIO)에 따라 SC-16~SC-21 6건을 추가(20건 총계)하고 PLAN_TASK-004.json에 F10/F11 흐름·INVALID_REQUEST/INVALID_SECTION 출력을 신설. 기존 SC-01~13/15는 변경 없음"

progress:
  completed:
    - "VERIFY_TASK-004.json code_review.findings를 근거로 필요한 오류 경로 식별 — 필수 필드 누락, 역방향 행 범위, seatsPerRow 범위, 정수 오버플로, grade 길이"
    - "PLAN_TASK-004.json 갱신 — F10/F11 flow, design.outputs에 INVALID_REQUEST/INVALID_SECTION, F1 steps에 전체 검증 순서 명시, F3을 long 산술로 정정, amendments에 attempt 2 기록"
    - "SCENARIO_TASK-004.md/.json에 SC-16~SC-21 추가, SC-15 flow 오류(F3→F1) 정정, SC-13/16 null-vs-빈배열 구분 명시"
    - "generate_red_trigger를 false로 되돌림(재승인 전)"
    - "1차 검증: fail(V4, Plan에 흐름 없음) — Plan 보강"
    - "2차 검증: fail(V4, 시나리오 flow가 여전히 null) — SC-16→F10, SC-17/18/19→F11 배선"
    - "3차 검증: pass(경고 2건 — grade 길이 미검증, regression 시나리오 없음) — SC-21(grade 20자 초과) 추가로 경고 1건 자체 해소"
  in_progress: "HITL#1 재승인 대기"
  blocked: []

next_steps:
  - priority: 1
    task: "AskUserQuestion으로 HITL#1 재승인 요청"
  - priority: 2
    task: "승인 시 generate_red_trigger=true, CP-2.4 retry1 저장"

decisions:
  - decision: "price null/음수에 대한 전용 시나리오는 추가하지 않는다"
    rationale: "SC-17/18/19/21이 이미 INVALID_SECTION(F11)의 서로 다른 분기를 검증하고 있고, price 검증은 같은 코드 경로(같은 검증 메서드 내 한 줄 추가)라 별도 시나리오 없이도 구현이 누락될 위험이 낮다고 판단 (coverage-policy §3)"
    alternatives_considered: ["price 전용 시나리오 추가 — 22건으로 늘어남"]
    impact: "시나리오 수를 20건으로 유지"
  - decision: "검증 순서 변경(F10→F9→F11→F2→F3→F4)에 대한 전용 regression 시나리오는 추가하지 않고, Phase 3에서 기존 스위트 전체 실행으로 회귀를 확인한다"
    rationale: "이미 승인된 SC-02~05/13(기존 14건)가 전부 유효한 형식의 단일/이중 구역을 전제로 하므로 새 검사(F10/F11)에 걸리지 않고 그대로 통과해야 한다 — 이는 회귀 시나리오를 새로 쓰는 것보다 기존 테스트 스위트 전체 실행이 더 직접적인 증거다"
    alternatives_considered: ["검증 순서를 명시적으로 확인하는 regression 시나리오 추가"]
    impact: "Phase 3에서 전체 스위트(기존 14 + 신규 6~7) 실행 결과를 CP-3.2에 반드시 기록해야 한다"
  - decision: "3차 검증 통과 후 SC-21(grade 20자 초과)을 추가로 자체 반영하고 4차 검증은 요청하지 않았다"
    rationale: "이미 pass=true였고, 추가한 SC-21은 기존 3개 검사(F11)와 동일한 패턴을 그대로 따르는 저위험 추가라 재검증 비용 대비 이득이 낮다고 판단"
    alternatives_considered: ["4차 검증 요청"]
    impact: "HITL#1에서 이 추가를 투명하게 보고한다"

recovery_prerequisites:
  - CP-4.2

execution_context:
  test_command: "cd backend && ./gradlew test"
  build_command: "cd backend && ./gradlew build"
  env_required: ["JAVA_HOME을 JDK21로 설정"]
  main_files:
    - "workflow_design/05_scenario/SCENARIO_TASK-004.md"
    - "workflow_design/04_plan/PLAN_TASK-004.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/07_verify/VERIFY_TASK-004.json"
---

## 무엇을 했나

Phase 4 FAIL(RETRY_SCENARIO)에 따라 Phase 2a로 되돌아와 code-reviewer가 재현한
결함을 막는 오류 시나리오 6건(SC-16~SC-21)을 추가했다. Plan도 함께 갱신해 새 흐름
(F10 필수 필드, F11 구역 형식)과 출력(INVALID_REQUEST, INVALID_SECTION)을 명시했다.
독립검증 3회 끝에 pass로 수렴했고, 남은 경고 하나(grade 길이 미검증)는 SC-21 추가로
직접 해소했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/SCENARIO_TASK-004.md` | 시나리오 SoT (20건) |
| `workflow_design/04_plan/PLAN_TASK-004.json` | F10/F11 흐름 추가된 Plan (amendments 2건) |

## 재개 방법

1. HITL#1 재승인을 받는다
2. 승인되면 `SCENARIO_TASK-004.json` 의 `generate_red_trigger=true`, CP-2.4 retry1 저장
3. Phase 2b로 재진입 — SC-16~SC-21용 Red 테스트 6건 추가 작성
