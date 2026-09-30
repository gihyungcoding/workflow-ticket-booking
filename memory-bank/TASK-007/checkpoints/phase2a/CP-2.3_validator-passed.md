---
checkpoint_id: CP-2.3
checkpoint_name: "독립검증 통과"
task_id: TASK-007
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-30T01:00:00Z
status: ACTIVE

work_summary: "scenario-validator 3회 호출 — 매회 overall.pass=true, 경고를 실제로 반영해 PLAN/시나리오를 보완했다"

progress:
  completed:
    - "1차 검증: pass, warn 3(V7 SC-07 클래스명 노출, V8 PLAN inputs/outputs 누락, V9 PUT 경로 0건) → SC-07 재작성, PLAN 보강, SC-09(PUT) 추가"
    - "2차 검증: pass, warn 3(V8 PUT 경로변수 여전히 누락, V9 SC-03/04/05/08/09가 기존 테스트와 동일 입력인데 실제로는 이미 통과해 Red로 쓸 수 없음, V10 sections=[null] cascade 최대 위험 누락) → PLAN에 id/선행상태 추가, SC-02/03/04/05/06/08/09를 error→regression으로 재분류(실제 현재 실패하는 건 SC-07뿐임을 명시), SC-10 추가"
    - "3차 검증: pass, warn 2(V9 여전히 6건이 기존 테스트와 동일 — Phase 2b가 신규작성/기존재실행 중 무엇으로 처리할지 명시 필요, V10 신규 발견: 최상위+section 동시 위반 시 에러 코드가 마이그레이션으로 INVALID_REQUEST→INVALID_SECTION으로 뒤집히는데 이를 검증하는 시나리오가 없고 기존 23개도 전부 단일 위반이라 무방비)"
    - "VALIDATION_TASK-007.json에 3회 요약 + 3차 원본을 그대로 저장"
  in_progress: "V9/V10에 대한 사람 판단(AskUserQuestion) 대기"
  blocked: []

next_steps:
  - priority: 1
    task: "AskUserQuestion으로 V10(동시 위반 에러 코드 뒤집힘)에 대한 처리 방향을 확인한다"
  - priority: 2
    task: "결정에 따라 시나리오/PLAN을 반영한 뒤 HITL#1(AskUserQuestion) 승인 요청"

decisions:
  - decision: "3차 검증까지만 돌리고 남은 warn 2건은 사람에게 직접 확인한다 — 4차 재검증 없이 진행"
    rationale: "fail은 한 번도 없었다(전부 pass) — 재시도 3회 상한은 fail 대응 규칙이라 무한 루프를 피하는 것이 목적에 맞다. 남은 지적(V9 중복 처리 방식, V10 에러 코드 우선순위)은 Phase 2a 시나리오 문구를 더 다듬어서 해결할 사안이 아니라 Phase 2b 구현 방침·태스크 범위에 대한 판단이 필요한 사안이다"
    alternatives_considered: ["4차 검증 1회 더", "V10을 임의로 해석해 시나리오 추가 후 진행"]
    impact: "HITL#1 전에 AskUserQuestion으로 V10을 먼저 확인한다 — TASK-005 회고에서도 이런 패턴(V10류 발견을 사용자 확인 후 반영)을 썼다"

recovery_prerequisites:
  - CP-2.2

execution_context:
  test_command: "cd backend && ./gradlew test --tests '*PerformanceRegistrationApiTest*'"
  build_command: "cd backend && ./gradlew build"
  env_required: []
  main_files:
    - "workflow_design/05_scenario/SCENARIO_TASK-007.md"
    - "workflow_design/05_scenario/SCENARIO_TASK-007.json"
    - "workflow_design/05_scenario/validator/VALIDATION_TASK-007.json"
    - "workflow_design/04_plan/PLAN_TASK-007.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/validator/VALIDATION_TASK-007.json"
---

## 무엇을 했나

scenario-validator를 3회 호출했다. 매회 fail 없이 pass였지만 경고를 실제로
반영하며 시나리오와 PLAN을 함께 다듬었다. 가장 중요했던 반영은 2차 V9였다 —
"이 태스크는 외부에서 관찰되는 동작을 바꾸지 않는 순수 리팩토링에 가깝다"는
사실을 발견하고, SC-02~06/08/09를 `error`에서 `regression`으로 재분류했다.
이 태스크에서 실제로 지금(마이그레이션 전) 실패하는 시나리오는 SC-07
(HttpMessageNotReadableException 핸들러 부재) 하나뿐이다 — Phase 2b가 이
사실을 명시적으로 다뤄야 한다.

3차 V10에서는 더 중요한 발견이 나왔다: title이 비어있으면서 동시에 grade가
너무 긴 것처럼 **최상위 필드와 section 필드를 동시에 위반**하는 요청의
에러 코드가 마이그레이션으로 실제로 바뀐다(현재 INVALID_REQUEST → Plan의
FieldError 경로 판정으로는 INVALID_SECTION). 기존 23개 테스트도 전부 단일
위반만 다뤄 이 뒤집힘을 잡는 자동 테스트가 전혀 없다. 이건 Phase 2a
시나리오 문구를 다듬어서 해결할 문제가 아니라, "이 뒤집힘을 허용할지,
막을지(예: title 우선순위 유지)"를 정하는 설계 판단이 필요해 시나리오를
더 늘리기 전에 사람에게 먼저 확인하기로 했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/SCENARIO_TASK-007.md` | 시나리오 SoT (10건) |
| `workflow_design/05_scenario/SCENARIO_TASK-007.json` | 파생 JSON |
| `workflow_design/05_scenario/validator/VALIDATION_TASK-007.json` | 3회 검증 요약 + 3차 원본 |
| `workflow_design/04_plan/PLAN_TASK-007.json` | inputs 보강(PUT 경로변수·malformed body) (갱신) |

## 재개 방법

1. `VALIDATION_TASK-007.json`의 V10을 읽는다
2. AskUserQuestion으로 처리 방향을 확인한다
3. 결정을 반영한 뒤 HITL#1(AskUserQuestion) 승인 요청, CP-2.4 저장, 커밋
