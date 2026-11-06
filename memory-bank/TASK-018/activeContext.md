---
task_id: TASK-018
title: "인증 화면 5종을 시안대로"
phase: "1"
phase_name: "Phase 1 - Plan"
status: IN_PROGRESS
track: standard
created_at: 2026-11-06
last_updated: 2026-11-06

primary_category: Frontend
sub_categories: [audience, organizer]
target_repo: "."
branch: "feature/task-018-auth-screens-redesign"

last_checkpoint: null
artifacts: {}
---

## 지금 무엇을 하고 있나

TASK-018 워크플로우를 시작했다. 아직 Plan 이 없다. (estimate 12h → standard 트랙)
기준: develop `628c54a` (PR #35 입력 경계 행렬 머지 뒤). 흐름 1(booking-ux-01) 세 번째 태스크 —
선행 TASK-016(아이디·대기 정보 API)·TASK-017(토큰·공통 부품) 모두 머지됨.
골격 확인: backend `./gradlew test` exit 0 · frontend `npm test` 단독 실행 통과
(전체 실행에서 PerformanceRegisterPage·WriteKeyRejected 가 부하(load 17) 로 5초 타임아웃 3~5건 —
단독 12/12 통과, 이 태스크와 무관).

## 다음 한 걸음

`wf-plan` 으로 Phase 1. 출발점: `docs/product/features/booking-ux-01-auth.md` §1-3·§2·§4·§5 Task C,
시안 `out/screens/{login,login--error,signup,signup--error,oauth-callback,role-select,role-select--error,joined}.html`,
기존 `frontend/src/pages/{Login,SignUp,OAuthRedirect,RoleSelection}Page.tsx`, `components/ui/*`(TASK-017).

## 알아둬야 할 것

- **새 입력 경계 행렬의 첫 적용** — PLAN `design.input_fields` 필수(`coverage_matrix.py --kinds`). 회고에서 2a 재시도 횟수를 이전과 비교한다
- node 는 PATH 에 `~/.nvm/versions/node/v22.23.1/bin` 을 붙여서 쓴다
- backend 는 `JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home`
- 문구는 해요체, 시안 문구가 정본 (ADR-0022)
- 범위 외: 아이디·비밀번호 찾기, 홈 화면(흐름 2)
- TASK-017 이월: 앱바 긴 제목 높이 넘침(줄 수 제한) · Sheet `--c-track` · Badge 사용자 문구 말줄임 · 배지/버튼 같은 말 — 이 태스크 화면에 해당하는 것만 Phase 1 에서 판정
