---
checkpoint_id: CP-2.5
checkpoint_name: "Red 테스트 코드 완료"
task_id: TASK-005
phase: "2b"
phase_name: "Phase 2b - Red"
saved_at: 2026-09-17T02:00:00Z
status: ACTIVE

work_summary: "SC-01~SC-10 을 Vitest 테스트 10건으로 옮기고, TypeScript 컴파일 스켈레톤(신규 컴포넌트 2개 + API 함수 3개)을 먼저 세워 실제로 실패하는 것을 확인했다"

progress:
  completed:
    - "frontend/src/api/performances.ts 확장: SectionSummary/SectionInput/RegisterPerformanceInput/UpdatePerformanceInput 타입, ApiError 클래스, registerPerformance/updatePerformance/cancelPerformance 시그니처(본문은 throw 한 줄), Performance.sections? 필드"
    - "frontend/src/pages/PerformanceRegisterPage.tsx, PerformanceEditPage.tsx 신규 — 컴포넌트 본문은 throw new Error('not implemented') 한 줄"
    - "frontend/src/App.tsx 에 /performances/new, /performances/:id/edit 라우트 추가"
    - "npx tsc -b --noEmit 통과 확인 — 스켈레톤이 컴파일은 통과하되 로직은 없음"
    - "PerformanceRegisterPage.test.tsx(SC-01~05), PerformanceEditPage.test.tsx(SC-06~09), App.test.tsx(SC-10) 작성"
    - "npm test 실행 — 신규 10건 전부 실패, 기존 12건 계속 통과. --reporter=verbose 로 10건 전부 실패 사유가 'not implemented' 동일함을 확인(순수성 체크)"
    - "npm run lint 확인 — 새 코드에서 신규 경고 없음(기존 2건 그대로)"
    - "python3 scripts/check_architecture.py 확인 — 새 위반 없음"

  in_progress: "HITL#2 승인 요청 준비"
  blocked: []

next_steps:
  - priority: 1
    task: "AskUserQuestion으로 HITL#2 승인 요청"
  - priority: 2
    task: "승인되면 TEST_TASK-005.json human_review.approved = true, CP-2.6 저장"

decisions:
  - decision: "컴포넌트 스켈레톤은 렌더 즉시 throw 하는 한 줄로 통일 (반환 타입 명시적으로 never)"
    rationale: "TS는 함수 선언문에서 throw-only 본문의 반환 타입을 항상 never로 추론하지 않아(void로 추론되는 경우 있음) JSX 컴포넌트 타입 에러가 났다. 반환 타입을 명시적으로 :never 로 선언해 해결"
    alternatives_considered: ["빈 JSX(<div/>) 반환 — 이건 SC-01 같은 happy 시나리오를 우연히 부분 통과시킬 위험이 있어 기각(스킬 FORBIDDEN 2)"]
    impact: "PerformanceRegisterPage/EditPage 둘 다 : never 반환 타입"
  - decision: "오류 문구/좌석 요약 표기 형식은 Phase 2a에서 이미 확정된 값(PLAN design.outputs) 그대로 테스트에 하드코딩"
    rationale: "Phase 2a 3차 검증(V8)에서 PLAN에 명시적으로 반영해뒀으므로 Red 단계에서 새로 정할 필요 없음 — 시나리오·PLAN·테스트 3단이 동일 문구를 공유"
    alternatives_considered: []
    impact: "Phase 3(Green) 구현이 문구를 다르게 쓰면 테스트가 깨짐 — 의도된 것"
  - decision: "오류 표시 위치는 data-testid(time-fields-error/section-card-N/form-error/section-summary/seat-preview)로 특정"
    rationale: "시나리오의 '아래에/위에' 같은 위치 관계는 Testing Library로 직접 단언하기 어렵다는 scenario-validator V5 지적을 Red 단계에서 구체적인 컨테이너 계약으로 해소"
    alternatives_considered: ["role=alert 로만 특정 — 페이지에 alert가 여러 개일 때 구분 불가해 기각"]
    impact: "Phase 3 구현이 이 testid들을 그대로 부여해야 함"

recovery_prerequisites:
  - CP-2.4

execution_context:
  test_command: "cd frontend && npm test"
  build_command: "cd frontend && npx tsc -b --noEmit"
  env_required: ["PATH에 $HOME/.nvm/versions/node/v22.23.1/bin 추가 필요 — 기본 PATH에 node/npm 없음"]
  main_files:
    - "frontend/src/pages/PerformanceRegisterPage.tsx"
    - "frontend/src/pages/PerformanceRegisterPage.test.tsx"
    - "frontend/src/pages/PerformanceEditPage.tsx"
    - "frontend/src/pages/PerformanceEditPage.test.tsx"
    - "frontend/src/App.tsx"
    - "frontend/src/App.test.tsx"
    - "frontend/src/api/performances.ts"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/TEST_TASK-005.json"
---

## 무엇을 했나

TypeScript는 컴파일 언어라 스켈레톤이 선행돼야 한다는 wf-red 규칙에 따라,
`registerPerformance`/`updatePerformance`/`cancelPerformance` 함수 시그니처와
`PerformanceRegisterPage`/`PerformanceEditPage` 컴포넌트를 먼저 만들되 본문은
`throw new Error('not implemented')` 한 줄만 뒀다. `npx tsc -b --noEmit`이
통과하는 것으로 "컴파일은 되지만 로직은 없음"을 확인한 뒤, SC-01~SC-10을
Vitest 테스트로 옮겨 실행했다. 10건 전부 같은 이유(스켈레톤의 throw)로
실패했고, 기존 12건은 그대로 통과했다.

시나리오의 "필드 아래/폼 상단" 같은 위치 관계는 Testing Library로 직접
단언하기 어려워(Phase 2a scenario-validator V5), Red 단계에서 data-testid
계약(`time-fields-error`, `section-card-{n}`, `form-error`, `section-summary`,
`seat-preview`)을 새로 정해 테스트에 반영했다 — 이는 Phase 3 구현이 지켜야
할 구체적 계약이 된다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/TEST_TASK-005.json` | Red 결과 SoT |
| `frontend/src/pages/PerformanceRegisterPage.test.tsx` | SC-01~05 |
| `frontend/src/pages/PerformanceEditPage.test.tsx` | SC-06~09 |
| `frontend/src/App.test.tsx` | SC-10 |

## 재개 방법

1. `TEST_TASK-005.json`을 읽는다
2. HITL#2(AskUserQuestion)로 승인받는다
3. 승인되면 `human_review.approved=true`, CP-2.6 저장, 테스트 코드와 함께 커밋
