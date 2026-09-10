---
checkpoint_id: CP-3.2
checkpoint_name: "테스트 Green 확인 (retry3)"
task_id: TASK-003
phase: "3"
phase_name: "Phase 3 - Green (재시도 3)"
saved_at: 2026-09-10T02:20:00Z
status: ACTIVE

work_summary: "code-reviewer 3차 재검증에서 발견한 color=\"text.secondary\" 무효 표기 결함(네 번째, 같은 부류) 수정. 테스트 12/12, 실브라우저로 ink-muted 색상까지 최종 확인"

progress:
  completed:
    - "PerformanceListPage.tsx: color=\"text.secondary\"(MUI v9에서 무효한 점 표기, 아무 규칙도 생성 안 됨) → color=\"textSecondary\"로 수정"
    - "PerformanceDetailPage.tsx: 장소·공연 일시·예매 오픈·예매 마감에 color=\"textSecondary\" 추가(원래 color 지정 자체가 없었음) — design-tokens.css의 --color-ink-muted 주석('장소·일시')과 정확히 대응. 잔여 좌석은 본문색 유지(주석 범위 밖)"
    - "theme/index.test.tsx 렌더 테스트에 color=\"textSecondary\" 렌더 결과(#5A6570) 단언 추가 — 이 패턴의 결함을 앞으로 자동으로 잡음"
    - "SCENARIO_TASK-003.md/.json에 해당 Then 추가, 'Phase 4 FAIL 이후 보강'에 retry3 이력 기록"
    - "npx vitest run — 12/12 통과"
    - "npx tsc --noEmit — 오류 0건, npm run build 성공, npm run lint error 0"
    - "backend ./gradlew test — BUILD SUCCESSFUL"
    - "python3 scripts/check_architecture.py --json — error 0, no_target: [], files_scanned: 44"
    - "실브라우저 최종 확인 — 목록/상세 모두 venue color=rgb(90,101,112)(#5A6570) 정확히 렌더, 상세의 '잔여 좌석'은 의도대로 본문색(#1A2027) 유지"
  in_progress: "없음"
  blocked: []

next_steps:
  - priority: 1
    task: "DEV_TASK-003.json(attempt 4) 저장 완료, CP-3.4 retry3 저장"
  - priority: 2
    task: "wf-verify 스킬로 Phase 4 재진입(attempt 2 최종) — VERIFY_TASK-003.json을 PASS로 갱신하고 HITL#3 진행"

decisions:
  - decision: "PerformanceDetailPage의 '잔여 좌석'은 textSecondary로 바꾸지 않고 본문색 유지"
    rationale: "design-tokens.css의 --color-ink-muted 주석이 정확히 '장소·일시'만 지목한다 — 잔여 좌석은 그 범주 밖이고, 목록 카드의 위계(제목>배지, 장소/일시는 보조, 잔여좌석 같은 실질 정보는 없음)와 달리 상세 화면에서는 잔여 좌석이 사용자가 실제로 확인하려는 핵심 정보 중 하나라 본문색이 자연스럽다"
    alternatives_considered: ["전체 필드를 일괄 textSecondary로 — 근거 문서(design-tokens.css 주석)가 지목한 범위를 벗어나는 과잉 적용이라 기각"]
    impact: "잔여 좌석만 진하게 남아 화면에서 오히려 강조됨(스크린샷으로 확인)"

recovery_prerequisites:
  - CP-3.2_tests-green_retry2
---
