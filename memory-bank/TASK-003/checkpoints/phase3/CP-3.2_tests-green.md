---
checkpoint_id: CP-3.2
checkpoint_name: "테스트 Green 확인"
task_id: TASK-003
phase: "3"
phase_name: "Phase 3 - Green"
saved_at: 2026-09-10T00:30:00Z
status: ACTIVE

work_summary: "theme/index.ts, StatusBadge.tsx, index.html, PerformanceListPage/DetailPage.tsx, main.tsx 구현 완료. 대상 테스트 11/11 통과, 빌드 성공, 아키텍처 DESIGN-001 위반 0건, 백엔드 회귀 없음"

progress:
  completed:
    - "theme/index.ts: createAppTheme()이 tokens.ts 값으로 createTheme() 조립 — palette(background/text/success), typography.fontFamily, shape.borderRadius=2, shadows=Array(25).fill('none')"
    - "StatusBadge.tsx: STATUS_LABEL 3개 변경(UPCOMING/CLOSED/CANCELLED), STATUS_COLOR 불변"
    - "index.html: lang=ko, title='공연 예매', Google Fonts preconnect+stylesheet(display=swap) 추가"
    - "PerformanceListPage.tsx/PerformanceDetailPage.tsx: 공연명 Typography에 fontDisplay sx 적용"
    - "main.tsx: 빈 createTheme() 제거, theme/index.ts의 theme 사용"
    - "npx vitest run — 첫 시도 3건 실패 발견·수정: (1) index.html.test.ts의 link 검색 predicate가 preconnect link까지 매칭하던 버그 → rel=\"stylesheet\" 조건 추가 (2)(3) PerformanceListPage/DetailPage.test.tsx의 renderPage가 ThemeProvider로 감싸지 않아 본문 서체 검증이 MUI 기본 테마(Roboto)를 보고 있었음 → ThemeProvider(theme) 추가. 수정 후 11/11 통과"
    - "npm run build — 성공"
    - "python3 scripts/check_architecture.py --id DESIGN-001 — 위반 0건(AC1 충족 확인), 전체 실행 시 DESIGN-002도 0건, DESIGN-003 경고 2건은 범위 밖(무변경)"
    - "npm run lint(oxlint) — error 0, warning 2(기존과 동일)"
    - "backend ./gradlew test — BUILD SUCCESSFUL, 회귀 없음"
  in_progress: "없음"
  blocked: []

next_steps:
  - priority: 1
    task: "리팩토링 검토(변경 규모가 작아 생략 판단 예상)"
  - priority: 2
    task: "DEV_TASK-003.json 저장, CP-3.4 저장, 커밋"

decisions:
  - decision: "테스트 렌더 헬퍼에 ThemeProvider를 추가한 것은 단언 완화가 아니라 테스트 환경을 프로덕션과 일치시킨 것"
    rationale: "main.tsx는 항상 ThemeProvider로 앱 전체를 감싸므로, 컴포넌트 단위 테스트도 같은 조건이어야 '본문 서체가 IBM Plex Sans KR이다'라는 AC를 실제로 검증한 것이 된다. ThemeProvider 없이 통과시키려면 오히려 단언을 약화(예: 서체 문자열 대신 sx prop 존재만 확인)해야 했을 것"
    alternatives_considered: ["venue 텍스트 서체 단언을 포기하고 title만 검증 — AC2 커버리지가 줄어들어 기각"]
    impact: "두 테스트 파일의 renderPage 헬퍼가 앞으로 이 프로젝트의 테마 인지 테스트 관례가 됨(Phase 5 회고 후보)"
  - decision: "index.html.test.ts의 link 검색 로직 버그를 Green 단계에서 직접 수정"
    rationale: "이 버그는 프로덕션 코드가 아니라 테스트 코드 자체의 결함(선택자가 너무 느슨함)이었다 — FORBIDDEN의 '단언 약화'가 아니라 오히려 더 정확한 대상을 검사하도록 강화한 것"
    alternatives_considered: ["Phase 2b로 롤백해 검증자에게 다시 검토받음 — 단순 선택자 버그로 판단해 과함"]
    impact: "SC-05가 실제로 검증하려던 대상(Google Fonts stylesheet link)을 정확히 짚게 됨"

recovery_prerequisites:
  - CP-3.1

execution_context:
  test_command: "nvm use v22.23.1 && npm --prefix frontend test"
  build_command: "nvm use v22.23.1 && npm --prefix frontend run build"
  env_required: ["Node.js — nvm use v22.23.1"]
  main_files:
    - "frontend/src/theme/index.ts"
    - "frontend/src/components/StatusBadge.tsx"
    - "frontend/index.html"
    - "frontend/src/pages/PerformanceListPage.tsx"
    - "frontend/src/pages/PerformanceDetailPage.tsx"
    - "frontend/src/main.tsx"
---
