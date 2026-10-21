---
task_id: TASK-009
title: "이메일 인증 + Spring Security 기반 구축"
phase: "1"
phase_name: "Phase 1 - Plan"
status: IN_PROGRESS
track: standard
created_at: 2026-10-21
last_updated: 2026-10-21

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-009-email-auth-security"

last_checkpoint: null
artifacts: {}
---

## 지금 무엇을 하고 있나

TASK-009 워크플로우를 시작했다. 아직 Plan 이 없다. (estimate 14h → standard 트랙)
기준: develop `dda8e64` (TASK-015 · 배포 체크리스트 기록 PR #13 머지 포함). 백엔드 테스트 통과 확인.
출처 문서: `docs/product/features/user-authentication.md` (§1-2 Task A, §3 API 명세, §4 흐름, §5 Task A 완료 조건, §6 구현 참고)

## 다음 한 걸음

`wf-plan` 스킬로 Phase 1 설계를 한다. Plan 전에 아래 "열린 질문" 을 사용자와 정한다.

## 알아둬야 할 것

- 범위: 이메일 가입·로그인 + `GET /api/auth/me` + `SecurityFilterChain` + JWT. **소셜 로그인과 소유권 검사는 제외** (TASK-011~013, 별도 문서)
- 기존 `/api/performances/**` 는 `permitAll` 유지하며 회귀가 없어야 한다 (AC 8)
- 관련 ADR: ADR-0012(JWT 인증), ADR-0013(Spring Security 도입), ADR-0014(소셜 자동 연결, 이번 범위 밖), ADR-0015(access token localStorage), ADR-0016(분리 배포·쓰기 가드)
- CORS 는 이미 서블릿 필터(`CorsFilter`, 최우선)라 Security 체인과 충돌하지 않게 설계됨 (ADR-0016)
- 새 마이그레이션은 `V3__create_users.sql` (현재 V1·V2). 운영 DB(Neon)에는 첫 배포에서 V1·V2 적용 완료
- 첫 배포 체크리스트(`docs/release/first-deploy-checklist.md`)에서 배포 환경이 실제로 떠 있다 —
  새 환경변수(JWT 비밀키 등)는 Render 에도 입력이 필요하다는 점을 Plan 에 적는다
- 회고 개선안 미반영 2건 (TASK-015 #0 coverage-policy.md, #1 wf-red) — 서로 다른 대상이라 시작을 막지 않는다

## 열린 질문 (Phase 1 에서 사용자와 결정)

1. **쓰기 가드(ADR-0016)와의 관계.** ADR 은 "TASK-009 가 들어오면 가드를 제거한다" 고 했지만, 이 태스크는
   `/api/performances/**` 를 `permitAll` 로 두고 소유권 검사도 하지 않는다. 가드를 지우면 **누구나 공연을
   등록·수정·취소할 수 있게 된다.** 가드를 유지할지, 제거 시점을 소유권 검사 태스크로 미룰지 정해야 한다.
   또 `POST /api/auth/signup·login` 이 쓰기 가드를 통과해야 하는지도 같이 정한다.
2. **JWT 비밀키·만료 설정.** 환경변수 이름과 기본값(로컬 개발용)을 정한다. 운영 값은 저장소에 두지 않는다.
3. **첫 배포 환경에 미치는 영향.** V3 마이그레이션과 새 환경변수가 Render 에 필요하다 — 배포는 이 태스크 범위 밖이지만 Plan 에 기록한다.
