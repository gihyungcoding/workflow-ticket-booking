---
task_id: TASK-025
title: "내 공연 목록·주최자 진입 경로"
phase: "2b"
phase_name: "Phase 2b - Red 완료 (HITL#1·2 승인)"
status: IN_PROGRESS
track: light
created_at: 2026-11-17
last_updated: 2026-11-17

primary_category: Frontend
sub_categories: [organizer]
target_repo: "."
branch: "feature/task-025-organizer-list-entry"

last_checkpoint: "checkpoints/phase2b/CP-2.5_red-code.md"
source: "docs/product/features/organizer-flow.md (Task D)"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-025.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-025.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-025.json"
  test: "workflow_design/05_scenario/TEST_TASK-025.json"
---

## 지금 무엇을 하고 있나

Phase 2b (Red) 완료 — 테스트 23개 함수·320 케이스, 실패 300(+승인 테스트 변경 3), HITL#1·2 승인(2026-11-17). 3 진입 가능.

## 다음 한 걸음

Phase 3 — `wf-develop`. 테스트를 통과시키는 최소 구현. 계약: TEST_TASK-025.json `test_contracts_for_green`, SC-17 행 f 는 쪽 수 × 20 ≥ 총수 규칙.

## 알아둬야 할 것

- 근거: 요구사항 `docs/product/features/organizer-flow.md` Task D. 선행 TASK-023(주최자 조회 API, PR #52)·TASK-024(토큰 전송·가드, PR #46) 모두 develop 에 머지됨
- **TASK-024 follow_up 이관 (필수 반영)**: `AuthContext.logout` 이 복원 중이면 `restoring` 을 false 로 돌리지 않는다(develop 기존 결함, 현재 도달 불가). 이 태스크가 로그아웃 진입 경로(AppHeader 「내 공연」 등)를 늘리므로 도달 가능해진다. fix_hint: `logout` 에 `setRestoring(false)`. 시나리오 — 복원 중 logout → stale 복원 결과 뒤 가드가 `/login` 으로 가는 경우 (사용자 결정, TASK-024 Phase 4 2회차)
- 범위 외: 내 공연 상세, 공개 전환, 등록·수정 복귀 경로 (TASK-026)
- 서버 계약은 TASK-023 산출물이 정본 — `GET /api/organizer/performances?filter=` (필터: 전체·비공개·예정·예매 가능·매진·마감·취소). `editable`·`visibilityChangeable` 은 서버 계산
- 디자인: `--c-draft`·`--c-draft-tint`·`--c-draft-tint-text` 토큰을 `docs/product/design-tokens.css` 에서 `frontend/src/theme/tokens.ts` 로 이식, `Badge` 에 `draft` 톤 추가. 정본 `docs/product/design.md`, ADR-0002(MUI)·ADR-0022(시안과 같아야 한다)
- 공통 부품 재사용: `components/ui/`(`Badge`·`Row`·`Block`·`Segmented`·`CtaBar`·`Sheet`)·`components/StatusBadge.tsx`·`StatusChips.tsx`
- frontend 명령은 `export PATH="$HOME/.nvm/versions/node/v22.23.1/bin:$PATH"` 후 실행. 시작 시점 `npm test` 1603 통과 (32 파일)
- track light(견적 8h) — 검증은 줄이지 않는다. Phase 1 완료 보고는 unresolved 와 AC 매핑만
- 시작 시 미반영 회고 개선안 9건 (coverage_matrix.py ×2: TASK-020·022) — 사용자가 「그대로 시작」 선택. 이 태스크는 입력 경계 칸이 적은 화면 위주
- **사용자 확정 (2026-11-17, Phase 1)**: ① 머리글 「{이름} 님」+상태별 제목 3종 ② /organizer 한 장 전체(전역 헤더 없음) ③ 「더 보기」 추가 ④ 소셜 콜백도 역할별 이동 — ③·④ 는 AC·implementation_spec 밖 확장
- 상세 라우트가 없어 행 선택은 TASK-026 전까지 빈 화면 — 같은 배포로 낸다
- **Red 승인 사항 (2026-11-17)**: 승인된 테스트 변경 1파일(task024.organizer.test.tsx — SC-17 b·c·보강·afterEach 복원). SC-17 행 f = 쪽 수 기준(홈과 다름). 기본 제목 「내 공연」과 앱바 제목 중복은 수용
