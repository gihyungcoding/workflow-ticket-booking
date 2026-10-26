---
task_id: TASK-011
title: "소셜 로그인 연동(카카오/네이버/구글)"
phase: "3"
phase_name: "Phase 3 - Green 재진입 완료 (Phase 4 FAIL 롤백)"
status: ACTIVE
track: standard
created_at: 2026-10-23
last_updated: 2026-10-26

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-011-social-login-oauth2"

last_checkpoint: "CP-3.4"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-011.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-011.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-011.json"
  red: "workflow_design/05_scenario/TEST_TASK-011.json"
  dev: "workflow_design/06_dev/DEV_TASK-011.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-011.json"
---

## 지금 무엇을 하고 있나

TASK-011 워크플로우를 시작했다. 아직 Plan 이 없다. (estimate 16h → standard 트랙, risk high, complexity 복잡)
기준: develop `f9e700a` (PR #21 · TASK-010 병합 포함). 백엔드 테스트 112/112 통과 확인(`cleanTest test` 실행).
출처 문서: `docs/product/features/user-authentication.md` (§1-2 Task C, §3 API 명세, §4 흐름, §5 Task C 완료 조건, §6 구현 참고, §7 범위 외, §8 결정)

## 다음 한 걸음

Phase 3 재진입 완료(133/133, spotlessCheck·아키텍처 error 0). SC-05 Given 정정은 사용자 승인(HITL#1·#2). Phase 4 재검증 — wf-verify: RC-1·RC-2 변종 전부 재프로브, 실제 PostgreSQL 로 V4 수정본·loginWithSocial 프로브, code-reviewer 에 root_causes 이어받기.

## 알아둬야 할 것

- 범위: `social_accounts` 테이블(V4, `users.password_hash` nullable 전환 포함), 제공자별 사용자 정보 정규화(`CustomOAuth2UserService`), 이메일 일치 + `email_verified=true` 일 때만 자동 연결, 신규는 `pendingToken`(5분, `typ=pending`) 발급, `POST /api/auth/social/complete`. **애플 로그인(TASK-012)과 프론트 화면(TASK-013)은 제외**
- 현재 `build.gradle` 에는 `spring-boot-starter-security`·`crypto`·`oauth2-jose` 만 있고 **`spring-boot-starter-oauth2-client` 가 없다** — ADR-0013 의 OAuth2 Login 을 쓰려면 Plan 에서 의존성 추가를 다뤄야 한다
- 관련 ADR: ADR-0012(JWT), ADR-0013(Spring Security 전체 도입), ADR-0014(자동 연결 정책), ADR-0016/0017(쓰기 가드), ADR-0019(문자열 입력 검증은 서버가 정본)
- 기존 자산(TASK-009): `JwtTokenProvider`(`issueAccessToken`·검증, HS256), `AuthService`, `SecurityConfig`, `User`, `UserRepository`, `LoginResponse`, 쓰기 가드 필터(`X-Write-Key`)
- TASK-009·010 회고에서 가져올 것
  - 테스트 DB(H2)와 운영 DB(PostgreSQL)가 달라 저장·비교되는 문자열은 H2 로 못 잡는 결함이 있다(RC-1). 소셜 제공자가 준 이메일·이름·providerUserId 는 외부 입력이다 — 문자열 필드 전수(NUL·서로게이트·길이) 행과 PostgreSQL 통합 테스트(미반영 제안 wf-plan #1)를 Plan 의 test_strategy 에서 판단한다
  - 서버 응답 code·본문으로 분기하는 로직, 에러 상태 재제출은 시나리오 경계값 행에 넣는다
  - 비동기 결과·세션 변경 맥락(TASK-010 RC-1)은 프론트 쪽이지만, `pendingToken` 만료·재사용·typ 혼용은 같은 부류의 경계다
- 크기: 16h 로 정상 WRU 범위(4~16h)의 상한이다. 시나리오가 기준선(8~12)을 넘으면 2a 에서 사용자에게 알린다
- 열린 회고 개선안 7건은 서로 다른 대상이라 시작을 막지 않는다 (TASK-009 #1·#2, TASK-010 #0~#3, TASK-015 #1)
- TASK-010 브랜치의 로컬 커밋 `14b74ba`(PR 정보 기록)는 푸시되지 않은 채 그 브랜치에 남아 있다 — PR #21 은 이미 머지됐다
