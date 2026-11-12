---
task_id: TASK-024
title: "프론트 토큰 전송·주최자 가드"
phase: "4"
phase_name: "Phase 4 - Verify"
status: ACTIVE
track: light
created_at: 2026-11-12
last_updated: 2026-11-12

primary_category: Frontend
sub_categories: [organizer]
target_repo: "."
branch: "feature/task-024-frontend-token-guard"

last_checkpoint: "checkpoints/phase4/CP-4.2_verification.md"
source: "docs/product/features/organizer-flow.md (Task C)"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-024.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-024.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-024.json"
  test: "workflow_design/05_scenario/TEST_TASK-024.json"
  dev: "workflow_design/06_dev/DEV_TASK-024.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-024.json"
---

## 지금 무엇을 하고 있나

Phase 4 1회차 FAIL — RC-1(가드=메모리 user, 헤더=localStorage). HITL#3 롤백 결정 대기.

## 다음 한 걸음

`wf-verify` — 증거 수집·리뷰·화면 확인 후 HITL#3.

## 알아둬야 할 것

- 근거: ADR-0029(쓰기 가드 제거, 역할·소유권), ADR-0017(대체됨), ADR-0016(키가 번들에 노출되는 한계)
- 선행 TASK-022(PR #44) 는 develop 에 머지됨 — 서버는 이미 쓰기에 주최자 토큰을 요구한다(401·403 `FORBIDDEN_ROLE`·소유권 404)
- 배포: TASK-022 와 같은 배포로. 체크리스트 §5-1 — 프론트(`VITE_WRITE_KEY` 제거) 먼저, 백엔드 다음
- 제약 AUTH-001 은 warn 이고 `promote_after: TASK-024` — 남은 위반 5건(프론트)을 이 태스크가 없앤다. 끝나면 error 승격 판정(회고 drift)
- `frontend/src/api/http.ts` 주석이 없어진 쓰기 가드를 가리킨다(TASK-022 drift) — 이 태스크에서 정리
- frontend 명령은 `export PATH="$HOME/.nvm/versions/node/v22.23.1/bin:$PATH"` 후 실행
- Frontend 태스크 — Phase 1·4 에 `docs/product/design.md`, 시안 `out/screens/*.html` 대조. 안내 문구 「주최자만 이용할 수 있어요」·「공연 둘러보기」
- 범위 외: 내 공연 화면, 진입 링크, 등록 후 이동 경로 변경(TASK-025·026)
- track light(견적 6h) — 검증은 줄이지 않는다
- wf-red 개선안(TASK-018·022)은 PR #45 로 반영됨
