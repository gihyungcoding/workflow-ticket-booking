---
checkpoint_id: CP-2.2
checkpoint_name: "Canonical 시나리오 생성 완료"
task_id: TASK-007
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-30T00:20:00Z
status: ARCHIVED

work_summary: "TASK-007의 GWT 시나리오 8건(happy 1 / error 6 / regression 1) 작성, AC 4/4 커버"

progress:
  completed:
    - "SCENARIO_TASK-007.md 작성 — F1~F4 전부 최소 1건씩 대응"
    - "SCENARIO_TASK-007.json 파생 — coverage.uncovered 없음(AC 4/4)"
  in_progress: "scenario-validator 서브에이전트 호출 준비"
  blocked: []

next_steps:
  - priority: 1
    task: "scenario-validator 서브에이전트 호출"
    file: "workflow_design/05_scenario/SCENARIO_TASK-007.md"
  - priority: 2
    task: "검증 결과를 VALIDATION_TASK-007.json 에 원본 그대로 저장"

decisions:
  - decision: "기존 PerformanceRegistrationApiTest.java 의 23개 시나리오는 GWT로 다시 옮겨 적지 않고, 코드 변경 없이 재실행하는 것으로 AC1의 회귀 조항을 검증한다. 대신 Phase 1에서 확정한 가장 위험한 분기(price/seatsPerRow '존재 여부'만 Bean Validation, 하한값은 Service 잔존)를 SC-08(seatsPerRow=0)로 대표 검증한다"
    rationale: "같은 코드 경로를 반복 기술하지 않는다(coverage-policy.md §3). 23건을 전부 옮기면 새 시나리오 문서가 기존 테스트 파일의 사본이 되어 유지보수 비용만 늘어난다"
    alternatives_considered: ["23개 시나리오 전부 GWT로 재작성", "회귀를 아예 언급하지 않음(AC1 조항 누락 위험)"]
    impact: "SCENARIO_TASK-007.md 에 '커버리지 결정 노트' 섹션으로 근거를 남김 — Phase 4가 실제로 기존 테스트 파일이 변경 없이 그대로 green인지 확인해야 함"
  - decision: "price의 @NotNull 존재-여부 검증은 별도 시나리오를 만들지 않고 seatsPerRow의 @NotNull(SC-06)로 대표한다. rowEnd 형식 검증도 rowStart(SC-05)로 대표한다"
    rationale: "동일한 애노테이션 종류·동일한 FieldError 라우팅 코드 경로를 타 독립적으로 실패할 수 있는 결함이 없다"
    alternatives_considered: ["price/seatsPerRow, rowStart/rowEnd 각각 별도 시나리오(총 10건)"]
    impact: "시나리오 8건으로 유지 — '보통' 복잡도 기준선(4~7건)을 1건 초과하지만 6개 파일에 걸친 태스크 규모를 감안하면 과함이 아니라고 판단. scenario-validator 검증에서 이견이 있으면 재조정"

recovery_prerequisites:
  - CP-2.1

execution_context:
  test_command: "cd backend && ./gradlew test --tests '*PerformanceRegistrationApiTest*'"
  build_command: "cd backend && ./gradlew build"
  env_required: []
  main_files:
    - "workflow_design/05_scenario/SCENARIO_TASK-007.md"
    - "workflow_design/05_scenario/SCENARIO_TASK-007.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/SCENARIO_TASK-007.md"
    - path: "workflow_design/05_scenario/SCENARIO_TASK-007.json"
---

## 무엇을 했나

Plan의 F1~F4를 Given/When/Then 8건으로 옮겼다. AC 4건 모두 최소 1개 시나리오가
매핑됐다(uncovered 없음, 역방향도 확인 — 시나리오가 Plan의 flow 밖 동작을 검증하지
않음).

가장 신경 쓴 부분은 "무엇을 새로 시나리오로 쓰고 무엇을 기존 테스트 재실행에
맡길지"였다. 기존 23개 회귀 테스트를 전부 옮겨 적는 대신, Phase 1에서 확정한
가장 위험한 분기점(SC-08)만 대표로 새로 썼다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/SCENARIO_TASK-007.md` | 시나리오 SoT |
| `workflow_design/05_scenario/SCENARIO_TASK-007.json` | MD에서 파생한 구조화 데이터 |

## 재개 방법

1. `SCENARIO_TASK-007.md`/`.json` 을 읽는다
2. `scenario-validator` 서브에이전트를 호출해 독립 검증을 받는다
3. `VALIDATION_TASK-007.json` 에 응답을 그대로 저장한다
