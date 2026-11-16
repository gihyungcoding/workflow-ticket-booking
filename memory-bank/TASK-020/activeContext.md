---
task_id: TASK-020
title: "기준 시계·공연 상세·구역 위치 시트를 시안대로"
phase: "4"
phase_name: "Phase 4 - Verify 2회차 WARN (HITL#3 대기)"
status: IN_PROGRESS
track: standard
created_at: 2026-11-13
last_updated: 2026-11-13

primary_category: Frontend
sub_categories: [audience]
target_repo: "."
branch: "feature/task-020-detail-clock-zone-sheet"

last_checkpoint: "checkpoints/phase4/CP-4.2_verification.md"
source: "docs/product/features/booking-ux-02-browse.md (Task B)"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-020.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-020.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-020.json"
  test: "workflow_design/05_scenario/TEST_TASK-020.json"
  dev: "workflow_design/06_dev/DEV_TASK-020.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-020.json"
---

## 지금 무엇을 하고 있나

Phase 4 1회차 FAIL — 리뷰어 [correctness] 3건(RC-2 잔여·RC-3·RC-4). 사용자가 시나리오 롤백(2a)을 선택했다(2026-11-16, AskUserQuestion). 뿌리 4·변종 9 전부(RC-1~4)를 시나리오로 가져간다.

## 다음 한 걸음

Phase 2a — VERIFY root_causes 변종 전부를 시나리오로. 이후 검증자 → 2b(HITL#1·2 재승인) → 3 → 4. frontend 테스트는 `--testTimeout=60000` 으로 돌린다(시스템 부하).

## 알아둬야 할 것

- 근거: ADR-0022(시안이 디자인 정본), ADR-0024·0026(티켓온 기준 시각, 0초에 재조회 없이 전환)
- 선행 TASK-019(기준 시각 `GET /api/time`·포스터·구역 위치·`next-opening` API)는 develop 에 머지됨
- 견적 12h → track standard. 검증은 줄이지 않는다
- Frontend 태스크 — Phase 1·4 에 `docs/product/design.md`, 시안 `out/screens/concert.html`·`zone-map.html` 대조 (375px 나란히)
- 새로 만들 것: `api/time.ts`, `hooks/useServerClock.ts`, `components/ZoneMapSheet.tsx`·`ZoneMap.tsx`, `pages/QueuePlaceholderPage.tsx`. 현재 `frontend/src/hooks/` 디렉터리는 없다
- 수정: `utils/datetime.ts`(한국 시간 표기), `api/performances.ts`, `PerformanceDetailPage.tsx`, `ui/AppBar.tsx`(빈 h1 제거), `StatusBadge.tsx`, `App.tsx`
- 시안과 다르게 두는 것(§8): 회차 하나, 오픈 5분 전 안내 없음
- 범위 외: 대기열 동작(흐름 3), 회차·매수 선택(흐름 4), 홈 화면(TASK-021), 오픈 5분 전 안내 문구
- frontend 명령은 `export PATH="$HOME/.nvm/versions/node/v22.23.1/bin:$PATH"` 후 실행. 시작 시점 develop 에서 `npm test` 는 1회차 1건 실패(어느 테스트인지 미확인)·2회차 775 통과 — 간헐 실패 가능성, Phase 3·4 에서 재현되면 원인을 잡는다
- 회고 개선안 중 이번에 해당: TASK-018 #0 design-checks(시안 모양 값 표), #5 screen-verification(iframe 클릭·서버 종료). 같은 대상 2건 이상 누적은 없음
- 사용자 결정(2026-11-13): 상세·대기열 임시 화면은 전역 헤더 없이(시안대로). 승인 테스트 6건 안팎 수정은 2b 승인에 올린다
