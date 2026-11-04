---
task_id: TASK-016
title: "티켓온 아이디·소셜 가입 대기 정보 API"
phase: "2a"
phase_name: "Phase 2a - Scenario (Phase 1 롤백 2/2 후)"
status: ACTIVE
track: light
created_at: 2026-11-04
last_updated: 2026-11-04

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-016-user-handle-pending-info"

last_checkpoint: "CP-3.4"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-016.json"
---

## 지금 무엇을 하고 있나

TASK-016 워크플로우를 시작했다. 아직 Plan 이 없다. (estimate 8h → light 트랙)
기준: develop `73d2cad` (PR #28~#30 머지 포함). 백엔드 `./gradlew test` 통과 확인.
흐름 1(booking-ux-01) 세 태스크 중 첫 번째 — 다음은 TASK-017(토큰·공통 부품), TASK-018(인증 화면).

## 다음 한 걸음

Phase 4 FAIL(RC-2) → 사용자 결정으로 시나리오 롤백(RETRY_SCENARIO 1). AC8(토큰 압축 형식) 추가. SC-15(HTTP /me)·SC-16(검증 함수 직접) 을 써서 독립검증 → Red → 공통 지점 수정 → 재검증.

## 알아둬야 할 것

- **테스트는 Flyway 를 끄고 H2 `ddl-auto=create-drop` 으로 돈다** (`backend/src/test/resources/application.properties`).
  V5 마이그레이션의 기존 회원 채우기(backfill)는 기존 테스트 구성으로는 검증되지 않는다 —
  Phase 1 에서 검증 수단(Flyway 를 켠 별도 테스트, Testcontainers — ADR-0006)을 정한다
- 쓰기 가드(`WriteGuardFilter`)는 모든 POST 에 걸린다 — `POST /api/auth/social/pending` 도 같다
- pendingToken 의 만료·위조는 기존 `PendingSignupExpiredException` → 401 `PENDING_SIGNUP_EXPIRED` 재사용
- 아이디 규칙: 이메일 @ 앞부분 소문자, `[a-z0-9._]` 밖 제거, 3자 미만이면 `user`, 20자 자름, 겹치면 2·3… 접미사, 동시 가입 충돌 시 한 번 더 시도
- 범위 밖: 아이디 변경, 아이디로 회원 찾기(흐름 5), 휴대폰 번호
- 미반영 회고 개선안 `screen-verification.md` ×2 (TASK-010·013) — TASK-017 시작 전에 반영 예정
