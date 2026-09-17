---
checkpoint_id: CP-2.3
checkpoint_name: "독립검증 완료 — Plan 범위 축소로 경고 해소"
task_id: TASK-008
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-17T02:00:00Z
status: ARCHIVED

work_summary: "scenario-validator 독립검증 결과 overall.pass=true(경고 2건: V8/V10). 두 경고 모두 Phase 1에서 내가 임의로 넓힌 범위(등록/수정/취소 응답에도 sections 포함) 때문임을 확인하고, 시나리오를 늘리는 대신 PLAN_TASK-008.json을 원래 acceptance_criteria 범위(GET 상세/목록만)로 좁혀 해소했다"

progress:
  completed:
    - "scenario-validator 서브에이전트 호출, 응답을 workflow_design/05_scenario/validator/VALIDATION_TASK-008.json 에 가공 없이 저장"
    - "V8(입출력 대응)/V10(회귀 누락) 두 경고가 같은 근원(CP-1.1에서 내린 '등록/수정/취소도 sections 포함' 결정)에서 나온다는 것을 확인"
    - "PLAN_TASK-008.json 수정 — design.outputs[0].description, codebase_analysis.note, target_files(PerformanceService.java) change 설명에서 register/update/cancel 확장을 제거하고 getPerformance(상세) 한 곳에서만 sections를 채우도록 범위를 좁힘"
    - "acceptance_criteria 3건 커버리지가 여전히 100%임을 재확인 — 시나리오(SC-01~04)는 변경 불필요, Plan만 시나리오가 이미 그리고 있던 실제 범위에 맞춰 좁혔다"
  in_progress: null
  blocked: []

next_steps:
  - priority: 1
    task: "HITL#1 승인 요청 — 승인되면 human_input.generate_red_trigger=true 로 갱신 후 커밋"

decisions:
  - decision: "등록/수정/취소 응답에 sections를 포함하지 않는다 — CP-1.1의 해당 결정을 철회한다"
    rationale: "이 태스크의 acceptance_criteria 3건은 GET 상세/목록/404만 다룬다. '공연 상세와 동일한 구조'라는 feature 문서 문구를 근거로 범위를 넓힌 것은 이번 태스크가 요구하지 않은 확장이었다 — scenario-validator가 V8/V10에서 그 불일치(선언된 출력 범위와 실제 시나리오 커버리지의 괴리)를 정확히 짚어냈다. 필요해지면 그때 별도 태스크로 다룬다"
    alternatives_considered: ["SC-05~07을 추가해 등록/수정/취소 응답도 시나리오로 덮기"]
    impact: "PerformanceService.java 변경 범위가 getPerformance 한 곳으로 축소됨 — Phase 3 구현이 더 단순해진다. Green 단계에서 register/update/cancel 코드는 아예 건드리지 않는다"
  - decision: "SC-04의 covers 귀속이 형식적이라는 V3 지적은 수용하되 시나리오는 고치지 않는다"
    rationale: "SC-04는 실질적으로 Plan의 unresolved 항목(grade+price 동일 구역 합산)을 검증하는 boundary다. acceptance_criteria 1과 완전히 동일한 문장은 아니지만 같은 기능(sections의 grade·price·좌석수 정확성)의 다른 경계이므로 covers 공유가 부적절하지 않다고 판단"
    alternatives_considered: ["SC-04의 covers를 별도 문구로 분리하거나 acceptance_criteria에 boundary 조건을 추가"]
    impact: "없음 — SCENARIO_TASK-008.md/.json 원문 유지"

recovery_prerequisites:
  - CP-2.2

execution_context:
  test_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew -p backend test"
  build_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew -p backend build"
  env_required: []
  main_files:
    - "workflow_design/04_plan/PLAN_TASK-008.json"
    - "workflow_design/05_scenario/SCENARIO_TASK-008.md"
    - "workflow_design/05_scenario/validator/VALIDATION_TASK-008.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/validator/VALIDATION_TASK-008.json"
    - path: "workflow_design/04_plan/PLAN_TASK-008.json"
---

## 무엇을 했나

scenario-validator가 구조적 결함(fail) 없이 경고 2건을 보고했다. 둘 다 Phase 1에서
내가 acceptance_criteria보다 넓게 설계한 지점(등록/수정/취소 응답에도 sections 포함)을
가리켰다. 시나리오를 늘려 그 넓은 범위를 뒤늦게 정당화하는 대신, PLAN을 애초
acceptance_criteria가 요구하는 범위로 되돌렸다 — 요청받지 않은 확장을 만들지 않는다는
원칙에 따른 선택이다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/validator/VALIDATION_TASK-008.json` | 독립검증자 원본 응답 |
| `workflow_design/04_plan/PLAN_TASK-008.json` | 범위 축소 반영(outputs/codebase_analysis/target_files 수정) |

## 재개 방법

1. HITL#1 승인을 받는다 (범위가 좁아졌으므로 재검증 없이 진행 — 시나리오 자체는
   원래도 좁은 범위만 다루고 있었다)
2. 승인되면 SCENARIO_TASK-008.json의 human_input.generate_red_trigger를 true로 바꾼다
3. CP-2.4 저장 후 wf-red로 넘어간다
