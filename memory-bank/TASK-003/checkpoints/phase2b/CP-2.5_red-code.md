---
checkpoint_id: CP-2.5
checkpoint_name: "Red 테스트 코드 작성 및 실패 확인"
task_id: TASK-003
phase: "2b"
phase_name: "Phase 2b - Red"
saved_at: 2026-09-09T01:10:00Z
status: ARCHIVED

work_summary: "SC-01~05를 새 테스트 5건으로 옮김, 전부 Red 확인. SC-06/07은 계획대로 새 테스트 없이 처리. 기존 6건 회귀 없음"

progress:
  completed:
    - "frontend/src/theme/tokens.ts 신규 — design-tokens.css 값을 JS 상수로 완전히 채움(데이터 미러링은 '구조'이지 '로직'이 아니므로 스켈레톤 예외 대상 아님, TASK-002의 api/performances.ts 타입 정의와 같은 성격)"
    - "frontend/src/theme/index.ts 신규 — createAppTheme(): Theme 이 throw new Error('Not implemented')만 담은 스켈레톤"
    - "frontend/src/theme/index.test.ts 신규 — SC-01, createAppTheme() 호출 시 throw 직접 발생"
    - "frontend/src/components/StatusBadge.test.tsx 신규 — SC-04, 5개 상태 렌더 후 문구 5개 단언(UPCOMING 문구만 실제 검증 실패, OPEN/SOLD_OUT은 이미 값이 같아 그 자체로는 통과하지만 CLOSED/CANCELLED가 실패해 테스트 전체는 Red)"
    - "frontend/index.html.test.ts 신규 — SC-05, fs로 실제 파일을 읽어 lang/title/Google Fonts link/display=swap 확인"
    - "frontend/src/pages/PerformanceListPage.test.tsx에 SC-02 추가 — getComputedStyle로 공연명/장소 font-family 확인"
    - "frontend/src/pages/PerformanceDetailPage.test.tsx에 SC-03 추가 — 위와 동일 패턴"
    - "tsconfig.app.json 보강 — include에 index.html.test.ts 추가, types에 'node' 추가(node:fs/path/url 사용을 위해). tsc --noEmit 오류 0건 확인 — Red가 컴파일 오류가 아님을 보장"
    - "npx vitest run — 11개 중 5개 실패(신규), 6개 통과(기존 TASK-002 테스트, 무변경) — SC-07(회귀)이 Red 단계에서부터 이미 성립함을 확인"
    - "인벤토리 대조: 신규 Red 대상 시나리오 SC-01~05(5건) == 신규 테스트 함수 5건, 1:1 일치. SC-06/07은 의도적으로 신규 테스트 없음"
    - "TEST_TASK-003.json 저장 — SC-06/07의 no_new_test/covered_by_existing_tests 상태를 명시적으로 기록"
  in_progress: "없음"
  blocked: []

next_steps:
  - priority: 1
    task: "HITL#2 승인 요청 (AskUserQuestion)"

decisions:
  - decision: "tokens.ts는 스켈레톤이 아니라 실제 값으로 완전히 작성"
    rationale: "색상/서체/치수 상수는 로직이 없는 데이터 선언이라 TASK-002의 api/performances.ts 타입 정의, TASK-001의 Java DTO/enum 필드 선언과 같은 '구조' 범주 — throw로 막을 대상(로직)이 아니다"
    alternatives_considered: ["tokens.ts도 빈 값/undefined로 스텁 처리 — 값이 없으면 SC-01의 Then이 무엇을 단언해야 할지 애매해지고, 데이터 상수에 UnsupportedOperationException 상당의 처리를 적용하는 것은 관례에 맞지 않음"]
    impact: "SC-01의 Red는 tokens.ts가 아니라 theme/index.ts(이 값들을 실제로 palette에 조립하는 로직)의 부재에서 발생 — 정확한 지점에서 실패함"
  - decision: "createAppTheme()을 함수로 두고 함수 본문에서 throw — 모듈 스코프에서 즉시 throw하지 않음"
    rationale: "TASK-002의 페이지 컴포넌트가 렌더 시점(함수 호출 시점)에 throw하는 패턴과 일치시킴. 모듈 스코프에서 바로 throw하면 이 파일을 import하는 모든 곳이 즉시 깨져 원인 파악이 더 어려워짐"
    alternatives_considered: ["모듈 스코프에서 `export const theme = createTheme(...)`을 즉시 계산하며 throw"]
    impact: "테스트가 createAppTheme()을 명시적으로 호출하는 지점에서만 Red가 발생, import 자체는 안전"
  - decision: "SC-04(StatusBadge) 테스트는 5개 상태를 한 번에 렌더해 5개 단언으로 확인 — 상태별로 테스트를 쪼개지 않음"
    rationale: "SCENARIO_TASK-003.md의 시나리오 자체가 '다섯 개를 각각 렌더한다'는 단일 When으로 파라미터화돼 있어, 테스트도 그 구조를 그대로 옮김(canonical 규칙의 파라미터화 허용 범위)"
    alternatives_considered: ["상태 5개를 각각 별도 test()로 분리 — 시나리오가 이미 5개 텍스트를 판정 배열로 묶어놓은 것과 불일치해 기각"]
    impact: "테스트 함수 1개가 SC-04 전체를 담당, 5개 단언 중 OPEN/SOLD_OUT은 우연히 이미 통과하지만 CLOSED/CANCELLED/UPCOMING이 실패해 테스트 전체는 정상적으로 Red"

recovery_prerequisites:
  - CP-2.4

execution_context:
  test_command: "nvm use v22.23.1 && npm --prefix frontend test"
  build_command: "nvm use v22.23.1 && npm --prefix frontend run build"
  env_required: ["Node.js — nvm use v22.23.1"]
  main_files:
    - "frontend/src/theme/tokens.ts"
    - "frontend/src/theme/index.ts"
    - "frontend/src/theme/index.test.ts"
    - "frontend/src/components/StatusBadge.test.tsx"
    - "frontend/index.html.test.ts"
    - "frontend/src/pages/PerformanceListPage.test.tsx"
    - "frontend/src/pages/PerformanceDetailPage.test.tsx"
    - "frontend/tsconfig.app.json"
---
