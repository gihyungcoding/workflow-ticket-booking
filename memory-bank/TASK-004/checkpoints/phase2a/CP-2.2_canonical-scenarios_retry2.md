---
checkpoint_id: CP-2.2
checkpoint_name: "Canonical 시나리오 생성 완료 (attempt 3)"
task_id: TASK-004
phase: "2a"
phase_name: "Phase 2a - Scenario Design (attempt 3)"
saved_at: 2026-09-15T15:00:00Z
status: ARCHIVED

work_summary: "Phase 4 재검증(attempt 2) FAIL에 따라 SC-22(title 길이, POST)·SC-23(sections null 원소)·SC-24(title 길이, PUT — 신규 추가) 3건을 확정했다. SC-22/23은 이전 세션이 작업 트리에 초안으로 남긴 것을 code-reviewer로 재확인했고, SC-24는 이번 code-reviewer 리뷰에서 PUT 경로도 같은 결함을 공유한다는 사실이 드러나 추가했다."

progress:
  completed:
    - "이전 세션이 남긴 SC-22(title 200자 초과, POST)·SC-23(sections null 원소) 초안을 code-reviewer 재현 결과로 검증"
    - "SC-24(title 200자 초과, PUT) 신규 작성 — coverage-policy §3에 따라 별도 엔드포인트라 SC-22와 병합하지 않음"
    - "SCENARIO_TASK-004.md/.json 갱신, coverage.extra_scenarios_not_ac_mapped에 SC-22/23/24 사유 기록"
  in_progress: "scenario-validator 독립검증 호출"
  blocked: []

next_steps:
  - priority: 1
    task: "scenario-validator 서브에이전트 호출"
    file: "workflow_design/05_scenario/SCENARIO_TASK-004.md"
  - priority: 2
    task: "검증 결과를 validator/VALIDATION_TASK-004.json 에 원본 그대로 저장"

decisions:
  - decision: "SC-22(POST)와 SC-24(PUT)를 병합하지 않고 별도 시나리오로 둔다"
    rationale: "registerPerformance/updatePerformance가 validateRequired를 공유하지만 서로 다른 Controller 엔드포인트다. 길이 검사를 등록 경로에만 붙이는 잘못된 수정이 들어가면 SC-22는 통과하고 PUT만 500을 내는 결함이 실제로 가능하다 — coverage-policy §3 병합 금지 기준(같은 코드 결함이면 한쪽만 실패할 수 있는가)에 해당"
    alternatives_considered: ["SC-22 하나만 두고 PUT은 구현 시 자연히 해결된다고 가정 — code-reviewer가 명시적으로 PUT 경로를 지적했고, 이전 F10(SC-16) 때도 공유 헬퍼라는 가정만으로 검증 없이 넘어갔다가 이번에 실제로 길이 검사가 통째로 빠지는 결함이 났으므로 기각"]
    impact: "시나리오 총 24건(attempt 3 기준), acceptance_criteria 9/9 커버는 변화 없음(SC-24는 AC 미대응, code-reviewer 재현 결함 수정용)"
  - decision: "sections null 원소(SC-23)는 PUT 대응 시나리오를 만들지 않는다"
    rationale: "UpdatePerformanceRequest DTO에 sections 필드 자체가 없음을 코드로 직접 확인함(title/venue/startAt/openAt/closeAt만 존재) — PUT 경로에 해당 결함이 원천적으로 존재할 수 없다"
    alternatives_considered: []
    impact: "SC-23은 POST 단일 시나리오로 충분"

recovery_prerequisites:
  - CP-4.2_verification_retry1

execution_context:
  test_command: "cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test"
  build_command: "cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew build"
  env_required: ["JAVA_HOME을 JDK21로 설정"]
  main_files:
    - "workflow_design/05_scenario/SCENARIO_TASK-004.md"
    - "workflow_design/05_scenario/SCENARIO_TASK-004.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/SCENARIO_TASK-004.md"
    - path: "workflow_design/05_scenario/SCENARIO_TASK-004.json"
---

## 무엇을 했나

Phase 4 재검증(attempt 2) FAIL의 롤백으로 Phase 2a(attempt 3)를 진행했다. 이전
세션이 작업 트리에 남긴 SC-22(title 길이, POST)·SC-23(sections null 원소) 초안을
code-reviewer 실행 재현으로 검증하고, PUT 경로도 같은 title/venue 길이 결함을
공유한다는 새 발견에 따라 SC-24(title 길이, PUT)를 추가로 확정했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/SCENARIO_TASK-004.md` | 시나리오 SoT (attempt 3, 24건) |
| `workflow_design/05_scenario/SCENARIO_TASK-004.json` | 파생 JSON |

## 재개 방법

1. 위 산출물 파일을 읽는다
2. `scenario-validator` 서브에이전트를 호출한다
3. 결과를 `validator/VALIDATION_TASK-004.json` 에 저장한다
