---
task_id: TASK-009
title: "이메일 인증 + Spring Security 기반 구축"
phase: "2a"
phase_name: "Phase 2a - Scenario (Phase 4 3차 FAIL 롤백)"
status: ACTIVE
track: standard
created_at: 2026-10-21
last_updated: 2026-10-21

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-009-email-auth-security"

last_checkpoint: "CP-4.2"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-009.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-009.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-009.json"
  tests: "workflow_design/05_scenario/TEST_TASK-009.json"
  dev: "workflow_design/06_dev/DEV_TASK-009.json"
---

## 지금 무엇을 하고 있나

TASK-009 워크플로우를 시작했다. 아직 Plan 이 없다. (estimate 14h → standard 트랙)
기준: develop `dda8e64` (TASK-015 · 배포 체크리스트 기록 PR #13 머지 포함). 백엔드 테스트 통과 확인.
출처 문서: `docs/product/features/user-authentication.md` (§1-2 Task A, §3 API 명세, §4 흐름, §5 Task A 완료 조건, §6 구현 참고)

## 다음 한 걸음

Phase 4 3차 FAIL(6c59d6d) → 시나리오로 롤백(Phase 2a, RETRY_SCENARIO 3/3 소진). 이메일 짝 없는 서로게이트(PG \'?\' 치환·409 충돌)와 isUniqueViolation 두 경로 테스트 공백을 시나리오에 추가하고, 문자열 입력 전체의 DB 무손실 저장 변종을 한 번에 덮는다. 다음 Phase 4 FAIL 이면 Phase 1 로 상승.

## 새 세션 메모

- develop(#14~#16)을 17c7961 로 병합했다. `wf-verify` 에 **FAIL 뿌리·변종 기록 절차**(`references/root-cause-sweep.md`, `scripts/check_root_causes.py`)와 보호 훅(`scripts/hooks/protect_gates.py`)이 새로 생겼다 — Phase 4 3차를 시작할 때 새 SKILL.md 를 먼저 읽는다. 백엔드 코드 변경 없음.

## 알아둬야 할 것

- 범위: 이메일 가입·로그인 + `GET /api/auth/me` + `SecurityFilterChain` + JWT. **소셜 로그인과 소유권 검사는 제외** (TASK-011~013, 별도 문서)
- 기존 `/api/performances/**` 는 `permitAll` 유지하며 회귀가 없어야 한다 (AC 8)
- 관련 ADR: ADR-0012(JWT 인증), ADR-0013(Spring Security 도입), ADR-0014(소셜 자동 연결, 이번 범위 밖), ADR-0015(access token localStorage), ADR-0016(분리 배포·쓰기 가드)
- CORS 는 이미 서블릿 필터(`CorsFilter`, 최우선)라 Security 체인과 충돌하지 않게 설계됨 (ADR-0016)
- 새 마이그레이션은 `V3__create_users.sql` (현재 V1·V2). 운영 DB(Neon)에는 첫 배포에서 V1·V2 적용 완료
- 첫 배포 체크리스트(`docs/release/first-deploy-checklist.md`)에서 배포 환경이 실제로 떠 있다 —
  새 환경변수(JWT 비밀키 등)는 Render 에도 입력이 필요하다는 점을 Plan 에 적는다
- 회고 개선안 미반영 2건 (TASK-015 #0 coverage-policy.md, #1 wf-red) — 서로 다른 대상이라 시작을 막지 않는다

## 열린 질문 — 1번은 결정됨(ADR-0017: 가드 유지), 나머지는 PLAN.unresolved 로 이관

1. **쓰기 가드(ADR-0016)와의 관계.** ADR 은 "TASK-009 가 들어오면 가드를 제거한다" 고 했지만, 이 태스크는
   `/api/performances/**` 를 `permitAll` 로 두고 소유권 검사도 하지 않는다. 가드를 지우면 **누구나 공연을
   등록·수정·취소할 수 있게 된다.** 가드를 유지할지, 제거 시점을 소유권 검사 태스크로 미룰지 정해야 한다.
   또 `POST /api/auth/signup·login` 이 쓰기 가드를 통과해야 하는지도 같이 정한다.
2. **JWT 비밀키·만료 설정.** 환경변수 이름과 기본값(로컬 개발용)을 정한다. 운영 값은 저장소에 두지 않는다.
3. **첫 배포 환경에 미치는 영향.** V3 마이그레이션과 새 환경변수가 Render 에 필요하다 — 배포는 이 태스크 범위 밖이지만 Plan 에 기록한다.

## Phase 1 에서 새로 확인한 것

- **배포 영향**: `JWT_SECRET` 이 필수이고 없으면 기동 실패한다. Render 가 develop 을 자동 배포하면 머지 순간 현재 배포가 내려갈 수 있다 —
  **머지 전에 Render 에 `JWT_SECRET` 을 먼저 넣어야 한다.** V3 도 기동 시 Neon 에 적용된다. Ship 단계에서 사람에게 알린다
- 테스트 DB 는 H2 + flyway 꺼짐이라 V3 SQL 이 테스트에서 실행되지 않는다 — Phase 4 에서 docker postgres 로 확인
- ADR-0017 추가(e3f5242): 쓰기 가드 제거 시점을 소유권 검사 태스크로 대체. 후속 요구사항 문서·태스크는 아직 없다
