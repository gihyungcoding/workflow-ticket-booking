---
checkpoint_id: CP-3.2
checkpoint_name: "테스트 Green 확인 (retry2)"
task_id: TASK-003
phase: "3"
phase_name: "Phase 3 - Green (재시도 2)"
saved_at: 2026-09-10T01:50:00Z
status: ARCHIVED

work_summary: "code-reviewer 재검증(attempt 2)에서 lineHeight 미배선 발견 + 테스트 회귀 방어력 지적을 즉시 반영. 테스트 12/12, 실브라우저로 line-height까지 최종 확인"

progress:
  completed:
    - "theme/index.ts: typography에 h5/h6 lineHeight=lineHeightDisplay(1.35), body1/body2 lineHeight=lineHeightText(1.6) 추가"
    - "theme/index.test.ts → index.test.tsx로 확장자 변경(JSX 렌더 테스트 추가로 인해 필요)"
    - "SC-01을 테스트 2개로 재구성: (1) 테마 설정값 단언(palette/shape/shadows/lineHeight) (2) Chip·Card를 실제로 렌더해 getComputedStyle로 배지 radius·카드 테두리색을 확인 — code-reviewer의 '설정만 검증, 렌더 결과는 미검증' 지적 반영"
    - "렌더 기반 테스트에서 jsdom이 box-shadow:none을 빈 문자열로 반환하는 한계 발견 — boxShadow 단언은 제거하고 borderColor만 유지(jsdom 신뢰 가능 범위로 조정)"
    - "SCENARIO_TASK-003.md/.json에 line-height 단언 2건 추가, 'Phase 4 FAIL 이후 보강'에 retry2 이력 기록"
    - "npx vitest run — 12/12 통과"
    - "npx tsc --noEmit — 오류 0건"
    - "npm run build — 성공"
    - "npm run lint — error 0, warning 2(기존과 동일)"
    - "backend ./gradlew test — BUILD SUCCESSFUL"
    - "python3 scripts/check_architecture.py --json — error 0, no_target: [], files_scanned: 44"
    - "실브라우저 최종 확인 — titleLineHeight/fontSize=27px/20px=1.35(명조), venueLineHeight/fontSize=22.4px/14px=1.6(고딕) 정확히 일치"
  in_progress: "없음"
  blocked: []

next_steps:
  - priority: 1
    task: "DEV_TASK-003.json(attempt 3) 저장 완료, CP-3.4 retry2 저장"
  - priority: 2
    task: "wf-verify 스킬로 Phase 4 재진입(attempt 2) — code-reviewer 3차 호출"

decisions:
  - decision: "jsdom에서 box-shadow:none을 신뢰할 수 없어 렌더 기반 테스트에서 제외"
    rationale: "jsdom의 CSS 엔진이 완전하지 않아 명시적으로 설정된 적 없는 기본값(none)을 빈 문자열로 반환함 — 실제 브라우저(TASK-002/003 Phase 4에서 반복 확인)에서는 'none'으로 정확히 나오므로 실브라우저 확인으로 이 부분을 커버하고, jsdom 테스트는 신뢰 가능한 속성(borderColor)만 남김"
    alternatives_considered: ["boxShadow 단언을 toBe('') 로 완화 — 의미가 왜곡돼(설정 안 함≠의도한 none) 기각"]
    impact: "theme/index.test.tsx의 두 번째 SC-01 테스트가 borderColor만 렌더 기반으로 확인"

recovery_prerequisites:
  - CP-3.1_impl-strategy_retry1
---
