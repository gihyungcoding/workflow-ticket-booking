---
checkpoint_id: CP-1.3
checkpoint_name: "설계 정규화 완료"
task_id: TASK-005
phase: "1"
phase_name: "Phase 1 - Plan"
saved_at: 2026-09-17T00:00:00Z
status: ACTIVE

work_summary: "공연 등록/수정 화면(TASK-005)의 입력·출력·흐름을 정규화하고 PLAN_TASK-005.json 을 작성했다"

progress:
  completed:
    - "architecture.md·decisions/README.md·design.md·design-tokens.css 확인, 관련 ADR(0002/0007/0008/0009) 참조"
    - "check_architecture.py 실행 — 기존 warn 2건(DESIGN-003) 외 신규 위반 없음"
    - "백엔드 PerformanceResponse.java 를 코드로 직접 읽어 sections 필드가 GET 단일 상세 조회에서만 채워짐을 확인(TASK-008 완료로 이제 존재)"
    - "기존 프론트엔드 5개 파일(performances.ts, PerformanceListPage/DetailPage, App.tsx, theme) 조사 — 재사용 패턴 식별"
    - "PLAN_TASK-005.json 작성 — inputs 5 / outputs 7 / flows 7, acceptance_criteria 8건 전부 flows.covers 로 커버(역방향 확인 통과: extra covers 없음)"
  in_progress: "-"
  blocked: []

next_steps:
  - priority: 1
    task: "wf-scenario 스킬로 넘어가 Given/When/Then 시나리오 작성"
    file: "workflow_design/05_scenario/SCENARIO_TASK-005.md"

decisions:
  - decision: "AC에 없는 4개 에러 코드(EMPTY_SECTIONS/INVALID_SECTION/INVALID_REQUEST/DUPLICATE_SEAT_RANGE)는 F4(AC5)의 일반 실패 배너로 폴백 처리한다 — 코드별 전용 UI를 새로 만들지 않는다"
    rationale: "TASK-008 회고(REFLECT_TASK-008.json problem[0])에서 Plan이 acceptance_criteria보다 넓어져 Phase 2a에서야 지적된 사례가 있었다. 이번엔 Phase 1에서 미리 범위를 좁혀 정한다"
    alternatives_considered: ["에러 코드별 전용 인라인 메시지 설계", "acceptance_criteria를 먼저 확장하도록 사용자에게 확인"]
    impact: "PLAN.unresolved 에 근거 기록, Phase 2a 시나리오도 이 경계를 따른다"
  - decision: "수정 화면에서 GET 이 404(PERFORMANCE_NOT_FOUND)를 반환하는 경우도 전용 not-found 상태를 만들지 않고 AC5와 동일한 일반 실패 배너로 처리한다"
    rationale: "acceptance_criteria에 명시되지 않은 범위. PerformanceDetailPage의 not-found 패턴을 그대로 가져오면 새 UI 상태가 생겨 범위가 넓어진다"
    alternatives_considered: ["PerformanceDetailPage와 동일한 not-found 전용 문구 추가"]
    impact: "PerformanceEditPage 상태 유니온이 단순해짐(loading/error/success 3종)"
  - decision: "route=Frontend 단일 태스크로 유지, 새 npm 의존성(예: react-hook-form)을 도입하지 않는다"
    rationale: "기존 페이지 전부 useState 기반 상태 관리이며 package.json 에 폼 라이브러리가 없다 — 새로 추가하는 것은 이 태스크 범위를 넘는 스택 결정(ADR 필요)"
    alternatives_considered: ["react-hook-form 도입"]
    impact: "PerformanceRegisterPage/EditPage 는 useState 로 직접 폼 상태 관리"

recovery_prerequisites: []

execution_context:
  test_command: "cd frontend && npm test"
  build_command: "cd frontend && npm run build"
  env_required: []
  main_files:
    - "frontend/src/api/performances.ts"
    - "frontend/src/pages/PerformanceRegisterPage.tsx"
    - "frontend/src/pages/PerformanceEditPage.tsx"
    - "frontend/src/App.tsx"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/04_plan/PLAN_TASK-005.json"
---

## 무엇을 했나

TASK-005(공연 등록/수정 화면)를 TDD로 진입시킬 수 있도록 입력·출력·흐름을
정규화했다. 가장 중요했던 확인은 백엔드 `PerformanceResponse.java`를 직접 읽어
`sections` 필드가 실제로 존재하는지, 어느 엔드포인트 응답에 채워지는지였다 —
TASK-008이 이 필드를 추가하기 전에는 TASK-005가 Phase 2a에서야 이 공백을
발견해 blocked 됐던 선례가 있었다(TASK-008 회고 problem[2]). 지금은 TASK-008이
done이라 `GET /api/performances/{id}`(단일 상세)에서만 sections가 채워지고
목록/등록/수정/취소 응답에는 없다는 것을 코드로 확인했다.

또한 TASK-008 회고에서 나온 "Plan이 acceptance_criteria보다 넓어지지 않았는가"
체크를 실제로 적용해, AC에 없는 에러 코드 4건과 수정 화면의 404 케이스를 새
UI로 늘리지 않고 기존 AC5 실패 배너로 흡수하기로 결정했다(위 decisions 참고).

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/04_plan/PLAN_TASK-005.json` | 설계 정규화 SoT — route, inputs/outputs/flows, codebase_analysis |

## 재개 방법

1. `PLAN_TASK-005.json` 을 읽는다
2. `wf-scenario` 스킬로 Given/When/Then 시나리오를 작성한다 (F1~F7 각각 최소 1개)
3. `scenario-validator` 서브에이전트로 검증한 뒤 HITL#1 승인을 받는다
