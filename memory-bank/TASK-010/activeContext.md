---
task_id: TASK-010
title: "이메일 회원가입/로그인 화면"
phase: "2a"
phase_name: "Phase 2a - Scenario 승인 (HITL#1)"
status: ACTIVE
track: standard
created_at: 2026-10-22
last_updated: 2026-10-22

primary_category: Frontend
sub_categories: [audience, organizer]
target_repo: "."
branch: "feature/task-010-email-auth-screens"

last_checkpoint: "CP-2.2"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-010.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-010.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-010.json"
---

## 지금 무엇을 하고 있나

TASK-010 워크플로우를 시작했다. 아직 Plan 이 없다. (estimate 10h → standard 트랙)
기준: develop `76087ef` (PR #20 · TASK-009 병합 포함). 프론트 테스트 43/43 통과 확인.
출처 문서: `docs/product/features/user-authentication.md` (§1-2 Task B, §2 화면 설계, §3 API 명세, §4 흐름, §5 Task B 완료 조건, §6 구현 참고, §7 범위 외)

## 다음 한 걸음

Phase 2a 완료(시나리오 28건, 독립검증 5차 PASS, HITL#1 승인). wf-red 로 Phase 2b 를 수행한다 — 표로 묶은 시나리오(SC-07·08·09·11·15·16·17·19·24·25·29·31·32)는 행마다 테스트 케이스가 필요하다.

## 알아둬야 할 것

- 범위: 회원가입·로그인 폼, 폼 검증, `AuthContext`(인증 상태 전역 관리), 헤더 로그인/로그아웃 전환. **소셜 로그인 버튼(TASK-013)과 보호 라우트 가드는 제외**
- 백엔드 API(TASK-009): `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/auth/me`. 가입 요청에 `role`(AUDIENCE/ORGANIZER) 필드가 필요하다
- 관련 ADR: ADR-0002(MUI), ADR-0015(access token localStorage), ADR-0016(분리 배포·쓰기 가드), ADR-0017
- 구현 대상 경로(태스크 명시): `frontend/src/api/auth.ts`, `auth/AuthContext.tsx`, `pages/SignUpPage.tsx`, `pages/LoginPage.tsx`
- TASK-009 회고에서 가져올 것: 서버 응답의 `code`·본문으로 분기하는 로직은 같은 상태의 다른 code·빈 바디·비 JSON 을 시나리오에 넣는다 (coverage-policy §2-1 에 반영됨)
- 열린 회고 개선안 3건은 서로 다른 대상이라 시작을 막지 않는다
