---
task_id: TASK-021
title: "홈 화면을 시안대로"
phase: "2b"
phase_name: "Phase 2b - Red (HITL#1·2 승인 대기)"
status: IN_PROGRESS
track: standard
created_at: 2026-11-16
last_updated: 2026-11-16

primary_category: Frontend
sub_categories: [audience]
target_repo: "."
branch: "feature/task-021-home-screen-redesign"

last_checkpoint: "checkpoints/phase2b/CP-2.5_red-code.md"
source: "docs/product/features/booking-ux-02-browse.md (Task C)"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-021.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-021.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-021.json"
  test: "workflow_design/05_scenario/TEST_TASK-021.json"
  dev: "workflow_design/06_dev/DEV_TASK-021.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-021.json"
---

## 지금 무엇을 하고 있나

Phase 2b — Red 테스트 29건(435케이스, 424 실패·11 기존 동작 통과) 커밋 완료. 시나리오 + Red + 승인 테스트 수정 7행의 통합 승인(HITL#1·2) 대기.

## 다음 한 걸음

승인되면 Phase 3(`wf-develop`). 승인 전에는 구현하지 않는다.

## 알아둬야 할 것

- 근거: ADR-0022(시안이 디자인 정본), ADR-0024·0026(티켓온 기준 시각, 0초 재조회 없음)
- 선행 TASK-019(`next-opening`·`/api/time`·포스터)·TASK-020(`useServerClock`·날짜 표기 유틸)은 develop 에 머지됨 — 재사용한다
- 견적 10h → track standard. 검증은 줄이지 않는다
- Frontend 태스크 — Phase 1·4 에 `docs/product/design.md`, 시안 `out/screens/home.html` 대조 (375px 나란히)
- 수정·신규(implementation_spec.paths): `api/performances.ts`, `pages/PerformanceListPage.tsx`(→ 홈), `components/PerformanceCard.tsx`·`StatusChips.tsx`(신규), `components/StatusBadge.tsx`
- 시안과 다르게 두는 것(§8): 카드 일시는 항상 날짜+시각, "내 예매" 행 없음
- 범위 외: 검색·장르·랭킹, 내 예매 진입(흐름 5), 스크랩, 회차(흐름 4), 대기열(흐름 3)
- frontend 명령은 `export PATH="$HOME/.nvm/versions/node/v22.23.1/bin:$PATH"` 후 실행. 시작 시점 develop 에서 `npm test` 1168 통과·1 todo
- 사용자 결정(2026-11-16): 홈은 AppHeader 유지(App.tsx 불변) · Poster 는 components/Poster.tsx 로 공용 추출(상세 파일도 변경)
- 회고 개선안: 같은 대상 미반영 2건씩(screen-verification.md·coverage_matrix.py·wf-plan·wf-red·wf-develop) 이 쌓여 있으나 사용자가 "그대로 시작"을 선택했다(2026-11-16)
