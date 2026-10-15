---
task_id: TASK-015
title: "배포용 프론트 준비 — API 주소·쓰기 키 환경변수화"
phase: "1"
phase_name: "Phase 1 - Plan"
status: ACTIVE
track: light
created_at: 2026-10-15
last_updated: 2026-10-15

primary_category: Frontend
sub_categories: [audience, organizer]
target_repo: "."
branch: "feature/task-015-frontend-deploy-env"

last_checkpoint: null
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-015.json"
---

## 지금 무엇을 하고 있나

TASK-015 워크플로우를 시작했다. 아직 Plan 이 없다. (estimate 4h → light 트랙)
기준: develop `1090c2a` (TASK-014 머지 포함). 프론트 테스트 31/31, lint error 0 확인.

## 다음 한 걸음

`wf-plan` 스킬로 Phase 1 설계를 한다. 출발점은 `frontend/src/api/performances.ts`
(fetch 호출 5곳, 공통 http 모듈 없음), `frontend/vite.config.ts` 의 dev 프록시,
`docs/decisions/ADR-0016-*.md` 의 「세부」 표·「검사 가능한 제약」.

## 알아둬야 할 것

- `implementation_spec.paths` 에 `frontend/src/api/http.ts` 가 있으나 지금은 없는 파일이다 —
  신규로 만들지, `performances.ts` 안에서 처리할지는 Phase 1 에서 판단한다
- 백엔드(TASK-014)는 머지됨: `X-Write-Key` 가드는 POST·PUT·PATCH·DELETE 에만 적용,
  401 응답 바디는 `{code: "WRITE_KEY_INVALID", message}` (ErrorResponse)
- 쓰기 가드의 키는 번들에 노출된다 — 보안이 아니라 오남용 완화 수준 (ADR-0016)
- 범위 밖: 실제 배포, 콜드스타트 대응(깨우기·재시도), 인증 화면
- 검사 가능한 제약 DEPLOY-001 (소스에 배포 주소 하드코딩 금지) — `check_architecture.py --id DEPLOY-001`
- 기존 PERFORMANCE 화면의 오류 표시 방식(ApiError 처리)을 먼저 확인할 것 — 401 안내가
  일반 오류와 구분돼야 한다
- 규칙 개선안 PR #11(wf-red 통합 테스트 설정 확인·변이 검증 등)은 아직 머지 전이라 이 브랜치에 없다
