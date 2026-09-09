---
checkpoint_id: CP-2.5
checkpoint_name: "Red 테스트 코드 작성 및 실패 확인"
task_id: TASK-002
phase: "2b"
phase_name: "Phase 2b - Red"
saved_at: 2026-09-08T01:00:00Z
status: ARCHIVED

work_summary: "frontend 스캐폴딩(Vite+React+MUI+react-router-dom+Vitest+Testing Library) 구축, SC-01~06을 테스트 6건으로 옮김, 전부 Red 확인"

progress:
  completed:
    - "nvm으로 Node v22.23.1 확보(node/npm이 PATH 기본값엔 없음 — 매 bash 호출마다 'nvm use v22.23.1' 필요, JAVA_HOME 이슈와 유사한 환경 특이사항)"
    - ".gitkeep 제거 후 'npm create vite@latest . -- --template react-ts'로 스캐폴딩"
    - "의존성 설치: @mui/material·@emotion/react·@emotion/styled·react-router-dom(런타임), vitest·@testing-library/react·@testing-library/jest-dom·@testing-library/user-event·jsdom(개발)"
    - "package.json에 test 스크립트 추가('vitest run'), vite.config.ts에 server.proxy(/api→localhost:8080)와 test(jsdom, setupTests) 설정 추가"
    - "tsconfig.app.json types에 vitest/globals, @testing-library/jest-dom 추가"
    - "기본 보일러플레이트(App.css, hero.png, react.svg, vite.svg, 카운터 데모) 제거, index.css 최소화"
    - "main.tsx: MUI ThemeProvider+CssBaseline, BrowserRouter 부착. App.tsx: / → PerformanceListPage, /performances/:id → PerformanceDetailPage 라우팅 골격"
    - "**주의 — 최초 시도 오류 정정**: api/performances.ts·StatusBadge.tsx·PerformanceListPage.tsx·PerformanceDetailPage.tsx를 처음에 실제 동작하는 구현으로 작성했다가, Phase 2b는 프로덕션 코드를 작성하지 않는다는 FORBIDDEN 규칙을 어긴 것을 발견하고 전부 스켈레톤으로 되돌렸다. Java TDD Red skeleton 관례(memory: feedback-java-tdd-red-skeleton)를 TS/React에 적용 — 타입/인터페이스(구조)는 유지하고 함수·컴포넌트 본문은 `throw new Error('Not implemented')`(반환형 `never`)로 교체"
    - "tsc -p tsconfig.app.json --noEmit 오류 0건 — 스켈레톤이 컴파일 오류 없이 타입 체크 통과함을 확인(Red가 구문/컴파일 오류가 아님을 보장)"
    - "SC-01~06을 PerformanceListPage.test.tsx(4건)/PerformanceDetailPage.test.tsx(2건)로 옮김, GWT를 주석으로 그대로 남김"
    - "npx vitest run 실행 — 6/6 실패, 전부 'Error: Not implemented' 컴포넌트 throw 직접 발생(NameError/AttributeError에 대응하는 직접 호출 패턴)"
    - "인벤토리 대조: 시나리오 6건 == 테스트 함수 6건, SC-01~06 ID 전부 1:1 일치"
    - "TEST_TASK-002.json 저장(red_scenarios, red_result, inventory_check, skeleton_convention 포함)"
  in_progress: "없음"
  blocked: []

next_steps:
  - priority: 1
    task: "HITL#2 승인 요청 (AskUserQuestion)"

decisions:
  - decision: "스켈레톤 함수/컴포넌트는 전부 `throw new Error('Not implemented')` + 반환형 `never`로 통일"
    rationale: "Java 프로젝트(TASK-001)에서 이미 승인된 관례를 언어만 바꿔 적용 — 구조(타입/인터페이스)와 구현(로직)을 분리해 컴파일은 통과하되 실행하면 반드시 실패하게 함. 이번 태스크에는 제약-어노테이션처럼 '스텁 자체가 구현'이 되는 시나리오(memory 4번 예외)가 없어 예외 처리가 필요 없었다"
    alternatives_considered: ["컴포넌트가 빈 화면(null)을 렌더 — 그러면 단언 실패(AssertionError)가 나지만 어떤 단언이든 우연히 통과할 위험이 있어 기각"]
    impact: "6개 테스트 전부 'Error: Not implemented' 직접 throw로 동일한 방식의 실패 발생"
  - decision: "SC-05(로딩 스켈레톤) 검증에 data-testid='performance-card-skeleton' 사용"
    rationale: "MUI Skeleton은 접근성 role/텍스트가 없어 관찰 가능한 Then을 쓰려면 testid가 필요 — 테스트 목적의 훅이라 구현 세부 누출로 보지 않음(canonical 규칙 V7 참고)"
    alternatives_considered: ["MUI 클래스명(.MuiSkeleton-root)으로 쿼리 — 라이브러리 내부 클래스에 결합되어 기각"]
    impact: "Phase 3에서 Skeleton 컴포넌트에 이 testid를 3~6개 부여해야 SC-05가 Green이 된다"
  - decision: "PerformanceDetailPage.test.tsx는 vi.mock 팩토리에서 importOriginal로 PerformanceNotFoundError는 실물 유지, getPerformance만 vi.fn()으로 교체"
    rationale: "automock이 클래스를 어떻게 다루는지 불확실성을 없애기 위함 — instanceof 검사가 테스트와 컴포넌트 양쪽에서 동일한 클래스 참조를 봐야 함"
    alternatives_considered: ["PerformanceListPage.test.tsx처럼 전체 automock(vi.mock('../api/performances') 인자 없이) — PerformanceNotFoundError를 쓰지 않는 목록 테스트에는 문제없어 그대로 둠"]
    impact: "두 테스트 파일의 vi.mock 패턴이 다르지만 각자의 필요에 맞음"

recovery_prerequisites:
  - CP-2.4

execution_context:
  test_command: "nvm use v22.23.1 && npm --prefix frontend test"
  build_command: "nvm use v22.23.1 && npm --prefix frontend run build"
  env_required: ["Node.js — nvm use v22.23.1 (시스템 기본 PATH에는 node/npm 없음)"]
  main_files:
    - "frontend/src/pages/PerformanceListPage.tsx"
    - "frontend/src/pages/PerformanceDetailPage.tsx"
    - "frontend/src/components/StatusBadge.tsx"
    - "frontend/src/api/performances.ts"
    - "frontend/src/pages/PerformanceListPage.test.tsx"
    - "frontend/src/pages/PerformanceDetailPage.test.tsx"
---
