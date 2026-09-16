---
checkpoint_id: CP-3.2
checkpoint_name: "테스트 Green 확인 (retry1)"
task_id: TASK-003
phase: "3"
phase_name: "Phase 3 - Green (재시도)"
saved_at: 2026-09-10T01:20:00Z
status: ARCHIVED

work_summary: "Phase 4 FAIL 결함 4건 + 테스트 갭 1건 수정 완료. 테스트 11/11, 빌드/tsc/린트/아키텍처 전부 통과, 실브라우저 재확인으로 결함 해소 확증"

progress:
  completed:
    - "index.html: Google Fonts Noto Serif KR 요청을 wght@600;700 → 400;500으로 수정(h5=400/h6=500 실사용 굵기와 일치, MUI createTypography.js로 확인)"
    - "theme/index.ts: components.MuiChip.styleOverrides.root.borderRadius=radiusBadge(2) 추가 — 배지가 MUI 기본 pill(16px) 대신 토큰값 적용"
    - "theme/index.ts: components.MuiCard.defaultProps.variant='outlined' + MuiPaper.styleOverrides.outlined.borderColor=colorRule 추가 — 그림자 대신 구분선으로 카드 위계 구성(design.md §4)"
    - "theme/index.test.ts: SC-01에 palette.text.secondary·배지 radius·카드 테두리색 단언 3건 추가(테스트 갭 해소)"
    - "SCENARIO_TASK-003.md/.json: SC-01의 Then에 위 3건 반영, 'Phase 4 FAIL 이후 보강' 절로 사유 기록(재검증 미호출 사유 포함)"
    - "tsconfig.app.json 원상복구 — types에서 'node' 제거, include에서 index.html.test.ts 제거(scope_deviations 해소)"
    - "npx vitest run — 11/11 통과"
    - "npx tsc -p tsconfig.app.json --noEmit — 오류 0건"
    - "npm run build — 성공"
    - "npm run lint — error 0, warning 2(기존과 동일)"
    - "backend ./gradlew test — BUILD SUCCESSFUL"
    - "python3 scripts/check_architecture.py --json — error 0, no_target: [], files_scanned: 43"
    - "실브라우저 재확인(모의 서버) — chipRadius=2px, cardBorder=1px solid #D3D8DC, cardShadow=none, titleWeight 500(목록)/400(상세), Noto Serif KR 400·500 두 인스턴스 모두 document.fonts.check()로 로드 확인. 4건 결함 전부 실측으로 해소 확증"
  in_progress: "없음"
  blocked: []

next_steps:
  - priority: 1
    task: "DEV_TASK-003.json(attempt 2) 저장 완료, CP-3.4 retry1 저장"
  - priority: 2
    task: "wf-verify 스킬로 Phase 4 재진입"

decisions:
  - decision: "SC-01 보강을 재검증(scenario-validator) 없이 처리"
    rationale: "새 시나리오나 새 AC가 아니라 기존 SC-01('테마가 토큰 값으로 초기화된다')의 범위를 더 촘촘히 검증하는 것 — reject-state-machine.md §5 '지목된 부분만 고친다' 원칙에 부합"
    alternatives_considered: ["Phase 2a로 롤백해 정식 재검증"]
    impact: "SCENARIO_TASK-003.md/.json에 'Phase 4 FAIL 이후 보강' 절로 투명하게 기록"

recovery_prerequisites:
  - CP-3.1_impl-strategy_retry1
---
