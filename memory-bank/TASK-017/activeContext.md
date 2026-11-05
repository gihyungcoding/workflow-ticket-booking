---
task_id: TASK-017
title: "디자인 토큰 교체·공통 부품"
phase: "4"
phase_name: "Phase 4 - Verify (2회차)"
status: ACTIVE
track: standard
created_at: 2026-11-05
last_updated: 2026-11-05

primary_category: Frontend
sub_categories: [audience, organizer]
target_repo: "."
branch: "feature/task-017-design-tokens-ui-kit"

last_checkpoint: "CP-3.4"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-017.json"
---

## 지금 무엇을 하고 있나

TASK-017 워크플로우를 시작했다. 아직 Plan 이 없다. (estimate 12h → standard 트랙)
기준: develop `73d2cad`. 프론트 `npm test` 269/269, lint error 0 확인.
흐름 1(booking-ux-01) 두 번째 태스크 — TASK-016 은 PR #31(머지 대기), 이 태스크는 016 과 의존 없음.

## 다음 한 걸음

`wf-plan` 으로 Phase 1. 출발점: `docs/product/design-tokens.css`(값의 출처 out/tokens.css),
`docs/product/design.md` §3~§7, `out/components.css`·`out/components.html`, 기존 `frontend/src/theme/`.

## 알아둬야 할 것

- node 는 PATH 에 `~/.nvm/versions/node/v22.23.1/bin` 을 붙여서 쓴다 (nvm 함수 없음)
- 새 색·새 여백 값을 만들지 않는다 — 토큰 밖 값은 DESIGN-002(error)·003(warn)
- 배지는 design.md §5 의 6가지 뜻만 (예매 가능·예정·매진·마감(+공연 취소)·입금 대기·예매 확정)
- 기존 화면(목록·상세·공연 등록·수정) 테스트의 배지 문구 단언이 바뀐다 — 승인된 테스트 수정이 필요할 수 있다(Phase 1 에서 Grep)
- 화면 재배치는 범위 밖 (흐름 2). 이 태스크는 테마·공통 부품·배지 문구만
- screen-verification.md 개선안은 PR #32 로 반영 대기 (지연 프록시·fetch 래퍼, PID 로만 종료)
