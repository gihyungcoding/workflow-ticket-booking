---
checkpoint_id: CP-3.2
checkpoint_name: "테스트 Green 확인"
task_id: TASK-002
phase: "3"
phase_name: "Phase 3 - Green"
saved_at: 2026-09-08T02:00:00Z
status: ARCHIVED

work_summary: "api/performances.ts, StatusBadge.tsx, PerformanceListPage.tsx, PerformanceDetailPage.tsx 최소 구현 완료. 대상 테스트 6/6 통과, 빌드 성공, 백엔드 회귀 없음"

progress:
  completed:
    - "api/performances.ts: fetch 기반 getPerformances/getPerformance 구현, 404→PerformanceNotFoundError 변환"
    - "StatusBadge.tsx: MUI Chip으로 5개 상태→라벨/색 매핑"
    - "PerformanceListPage.tsx: loading/error/empty/success 4분기, 스켈레톤에 data-testid='performance-card-skeleton' 부여(SC-05 계약)"
    - "PerformanceDetailPage.tsx: loading/not-found/error/success 4분기"
    - "npx vitest run — 6/6 통과"
    - "npm run build(tsc -b && vite build) 성공 — 빌드 과정에서 타입 오류 2종 발견·수정: (1) @testing-library/jest-dom의 vitest 매처 타입이 tsconfig types 배열이 아니라 'jest-dom/vitest' 서브패스 임포트로만 활성화됨을 확인해 setupTests.ts를 '@testing-library/jest-dom/vitest'로 교체, tsconfig.app.json types에서 '@testing-library/jest-dom' 제거 (2) 설치된 MUI 9.4.0의 StackOwnProps가 justifyContent/alignItems 직접 prop을 더 이상 노출하지 않아(v9 API 변경) sx={{ justifyContent, alignItems }}로 이전"
    - "npx oxlint — error 0, warning 2건(react/set-state-in-effect, 데이터 페칭 시 로딩 상태를 effect에서 동기적으로 setState — 의도된 재시도 패턴이라 그대로 둠)"
    - "backend: ./gradlew test — BUILD SUCCESSFUL(UP-TO-DATE), 프론트 작업이 백엔드에 영향 없음 확인"
  in_progress: "없음"
  blocked: []

next_steps:
  - priority: 1
    task: "리팩토링 검토(Step 4) — 필요 없다고 판단되면 그 판단을 기록"
  - priority: 2
    task: "DEV_TASK-002.json 저장, CP-3.4 저장, 커밋"

decisions:
  - decision: "oxlint의 set-state-in-effect 경고 2건을 수정하지 않고 유지"
    rationale: "데이터 페칭 시작 시 로딩 상태로 되돌리는 것은 [다시 시도] 버튼(SC-03)의 의도된 동작 — 경고이지 에러가 아니며(exit gate는 error 0을 요구), 대안(파생 상태로 전환)은 재시도 흐름을 오히려 복잡하게 만든다"
    alternatives_considered: ["useReducer로 전환", "key prop을 바꿔 컴포넌트를 remount시켜 재시도 구현"]
    impact: "lint 결과에 warning 2건이 남지만 error 0으로 게이트 통과"

recovery_prerequisites:
  - CP-3.1

execution_context:
  test_command: "nvm use v22.23.1 && npm --prefix frontend test"
  build_command: "nvm use v22.23.1 && npm --prefix frontend run build"
  env_required: ["Node.js — nvm use v22.23.1"]
  main_files:
    - "frontend/src/api/performances.ts"
    - "frontend/src/components/StatusBadge.tsx"
    - "frontend/src/pages/PerformanceListPage.tsx"
    - "frontend/src/pages/PerformanceDetailPage.tsx"
---
