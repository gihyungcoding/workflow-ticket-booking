---
checkpoint_id: CP-1.3
checkpoint_name: "PLAN_TASK-003.json 저장 완료"
task_id: TASK-003
phase: "1"
phase_name: "Phase 1 - Plan"
saved_at: 2026-09-09T00:10:00Z
status: ACTIVE

work_summary: "inputs 3 / outputs 4 / flows 7(F1~F7, acceptance_criteria 7건 전부 커버) 확정, PLAN_TASK-003.json 저장"

progress:
  completed:
    - "design.inputs/outputs/flows 작성 — F1~F7가 acceptance_criteria 7건을 1:1로 커버"
    - "codebase_analysis.target_files 7건 확정·실재 확인(theme/tokens.ts, theme/index.ts만 신규)"
    - "unresolved 3건 기록: (1) PerformanceListPage/DetailPage.tsx가 implementation_spec.paths에 없었으나 AC2 충족에 필수라 추가 (2) design-tokens.css↔tokens.ts 값 일치를 자동 검증할 수단 없음(수기 대조) (3) STATUS_COLOR/ink-faint는 이번 범위 밖임을 재확인"
    - "PLAN_TASK-003.json JSON 파싱 확인 + AC 커버리지 확인(미커버 0건) + target_files 실재 확인"
  in_progress: "없음 — Phase 1 완료"
  blocked: []

next_steps:
  - priority: 1
    task: "activeContext.md phase/last_checkpoint/artifacts.plan 갱신 후 커밋"
  - priority: 2
    task: "wf-scenario 스킬로 Phase 2a 진입"

decisions:
  - decision: "PerformanceListPage.tsx/PerformanceDetailPage.tsx를 target_files에 추가"
    rationale: "tasks.json의 implementation_spec.paths에는 없었지만, AC2('공연명이 Noto Serif KR로 렌더링된다')를 충족하려면 공연명을 렌더하는 이 두 파일을 건드릴 수밖에 없다 — 색상/스켈레톤/행 구조는 손대지 않고 fontFamily sx 한 줄만 추가하는 최소 변경으로 한정"
    alternatives_considered: ["MUI 커스텀 typography variant(performanceTitle)를 선언해 theme 레벨에서만 처리 — module augmentation 복잡도 대비 이득이 적어 기각, 대신 tokens.ts의 fontDisplay 상수를 각 페이지에서 직접 import"]
    impact: "target_files 5→7건"
  - decision: "design-tokens.css는 frontend 빌드에 직접 import하지 않고, theme/tokens.ts가 값을 수기로 미러링한다"
    rationale: "design-tokens.css가 docs/product/ 에 있어 Vite 빌드 루트(frontend/) 밖이라 직접 import 시 Vite fs.allow 설정이 필요해 복잡도가 늘어난다. DESIGN-002가 frontend/src/theme/**를 exclude_paths로 이미 열어둔 것도 이 미러링 방식을 전제한 설계로 해석"
    alternatives_considered: ["vite.config.ts의 server.fs.allow를 docs/까지 확장해 CSS를 직접 import"]
    impact: "두 파일의 값 일치는 자동 검증 수단이 없어 Phase 4에서 사람이 대조해야 함(unresolved에 기록)"

recovery_prerequisites:
  - CP-1.1
  - CP-1.2

execution_context:
  test_command: "nvm use v22.23.1 && npm --prefix frontend test"
  build_command: "nvm use v22.23.1 && npm --prefix frontend run build"
  env_required: ["Node.js — nvm use v22.23.1"]
  main_files:
    - "workflow_design/04_plan/PLAN_TASK-003.json"
---
