---
checkpoint_id: CP-2.2
checkpoint_name: "Canonical 시나리오 작성 완료"
task_id: TASK-001
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-03T09:41:18Z
status: ACTIVE

work_summary: "시나리오 최종 11건 (happy 4 / boundary 4 / error 3). 처음 9건(acceptance_criteria 7건 매핑) 작성 후 HITL#1 검토 중 사용자 요청으로 SC-10/SC-11(NOT NULL/CHECK 제약 — Plan AC8/F7)을 추가했다."

progress:
  completed:
    - "SC-01~SC-09 작성, 각각 covers/flow 메타데이터 부여"
    - "SCENARIO_TASK-001.json 파생 (MD가 SoT)"
    - "coverage 표 작성 — 최초 7/7, SC-10/11 추가 후 8/8"
    - "사용자 요청 반영: PLAN_TASK-001.json에 F7(엔티티 제약-마이그레이션 제약 일치) flow와 AC8 추가, design.inputs/outputs에 F7 대응 항목 신설, amendments 필드에 변경 이력 기록"
    - "SC-10(NOT NULL), SC-11(CHECK, availableSeats<=totalSeats) 작성 — 영속성 계층 경계를 명시적으로 표시"
  in_progress: null
  blocked: []

next_steps:
  - priority: 1
    task: "scenario-validator 서브에이전트 호출"
    file: "workflow_design/05_scenario/SCENARIO_TASK-001.md"
  - priority: 2
    task: "검증 결과를 VALIDATION_TASK-001.json 에 원본 그대로 저장"

decisions:
  - decision: "SC-05(필터 없음)와 SC-06(status=CLOSED 필터)을 분리 유지"
    rationale: "필터 쿼리 구현 시 base 조건(start_at>=now)을 빠뜨리는 결함은 SC-05만으로는 잡히지 않고 SC-06(F3+F4 상호작용)에서만 드러난다"
    alternatives_considered: ["하나의 boundary 시나리오로 통합"]
    impact: "boundary 시나리오 4건 중 2건이 이 쌍"
  - decision: "SC-07(오픈 정각)과 SC-08(마감 1초 전)을 분리 유지"
    rationale: "openAt 비교와 closeAt 비교는 서로 다른 연산 — 한쪽만 <= 대신 <로 잘못 구현해도 다른 시나리오는 통과하므로 병합하면 결함을 놓친다"
    alternatives_considered: ["하나의 경계 시나리오로 통합"]
    impact: "boundary 시나리오 4건 중 2건이 이 쌍"
  - decision: "status 계산 단건 검증(SC-03, SC-04, SC-07, SC-08)은 목록이 아니라 상세(GET .../{id}) 엔드포인트로 검증"
    rationale: "페이지네이션·정렬 관심사를 섞지 않고 상태 계산 로직 자체만 관찰 가능하게 분리"
    alternatives_considered: ["전부 목록 엔드포인트로 검증"]
    impact: "목록 전용 관심사(F1/F3/F4)는 SC-01/02/05/06 이 담당"
  - decision: "사용자 요청(HITL#1 검토 중)에 따라 Plan에 F7/AC8을 추가하고 SC-10/SC-11을 신설"
    rationale: "Plan의 unresolved 항목 — 테스트는 H2 ddl-auto=create-drop로 엔티티 애노테이션이 스키마를 만들어 V1 마이그레이션 SQL 자체는 검증되지 않는다 — 을 방치하지 않고, 엔티티 애노테이션이 마이그레이션과 같은 제약을 표현하는지 @DataJpaTest로 실제 보호하기 위함"
    alternatives_considered: ["다루지 않고 unresolved로만 남긴다", "별도 태스크로 분리한다"]
    impact: "Plan AC 7→8, flow 6→7(F7), design.inputs/outputs 각 1건 추가. 시나리오 9→11건"
  - decision: "SC-10/SC-11의 When에서 'flush'라는 JPA 어휘를 빼고 'DB에 실제로 반영되는 시점까지'로 표현"
    rationale: "4차 독립검증(V7)이 When에 ORM 구현 어휘가 들어갔다고 지적 — 저장 시점의 모호성(save 호출 자체 vs 실제 반영 시점)은 여전히 명시해야 하므로, 구현 중립적 표현으로 대체"
    alternatives_considered: ["flush 표현 유지", "시점 명시 자체를 포기"]
    impact: "5차 재검증 없이 즉시 반영 — 단언 대상(예외 발생 여부와 시점)은 바뀌지 않고 어휘만 바뀜"

recovery_prerequisites:
  - CP-2.1

execution_context:
  test_command: "./gradlew test"
  build_command: "./gradlew build"
  env_required: []
  main_files:
    - "workflow_design/05_scenario/SCENARIO_TASK-001.md"
    - "workflow_design/05_scenario/SCENARIO_TASK-001.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/SCENARIO_TASK-001.md"
    - path: "workflow_design/05_scenario/SCENARIO_TASK-001.json"
---

## 무엇을 했나

`PLAN_TASK-001.json`의 flow 6개를 시나리오 9건(happy 4 / boundary 4 / error 1)으로
옮겼다. acceptance_criteria 7건 모두 최소 1개 시나리오에 매핑되며, 겉보기엔 비슷해
보이는 두 쌍(SC-05/06, SC-07/08)은 서로 다른 코드 결함을 잡아내므로 병합하지 않고
분리했다 — 근거는 CP의 `decisions`와 MD 하단 커버리지 표에 기록했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/SCENARIO_TASK-001.md` | 시나리오 SoT |
| `workflow_design/05_scenario/SCENARIO_TASK-001.json` | 파생 JSON |

## 재개 방법

1. 위 두 파일을 읽는다
2. `scenario-validator` 서브에이전트를 호출한다
3. 결과를 `workflow_design/05_scenario/validator/VALIDATION_TASK-001.json` 에 그대로 저장한다
