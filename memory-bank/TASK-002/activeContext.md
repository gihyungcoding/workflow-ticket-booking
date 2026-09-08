---
task_id: TASK-002
title: "공연 목록/상세 화면"
phase: "1"
phase_name: "Phase 1 - Plan"
status: ACTIVE
created_at: 2026-09-08
last_updated: 2026-09-08

primary_category: Frontend
sub_categories: ["audience"]
target_repo: "."
branch: "feature/task-002-performance-list-detail-ui"

last_checkpoint: (없음 — 워크플로우 시작 직후)
artifacts: {}
---

## 지금 무엇을 하고 있나

워크플로우를 막 시작했다. TASK-001(공연 목록/상세 조회 API)에 의존하는
프론트엔드 태스크로, `docs/product/features/performance-availability.md`
Task B(§1-2, §4, §5)를 구현 참고로 삼는다. TASK-001은 Phase 5까지 완료(DONE)
됐고 PR #1이 `develop`에 이미 머지되어 있어 API 의존성 문제는 없다.

**중요한 발견**: 이 저장소에는 아직 `frontend/` 디렉터리 자체가 없다
(`package.json` 없음, Vite/React 스캐폴딩 없음, 테스트 러너 없음).
`CLAUDE.md` 의 "frontend 미생성" 이 문자 그대로다. 이 태스크가 이 프로젝트의
**첫 프론트엔드 태스크**이므로 Phase 1(wf-plan)이 코드베이스 조사 단계에서
스캐폴딩 범위(Vite+React+MUI, 테스트 러너 선택 — Vitest + Testing Library가
유력)를 설계에 포함해야 한다. 이는 WRU 조건 3(Red 작성 가능)·5(Verify
자동화 가능)을 충족시키기 위한 선행 조건이다.

## 다음 한 걸음

`wf-plan` 스킬로 Phase 1을 시작한다 — 코드베이스 분석(빈 상태 확인),
설계 라우팅(Frontend 확정), 스캐폴딩 방식 결정, `PLAN_TASK-002.json` 작성.

## 알아둬야 할 것

- 의존 태스크 TASK-001은 DONE — API 는 `GET /api/performances`,
  `GET /api/performances/{id}` (응답 필드: id/title/venue/startAt/openAt/
  closeAt/totalSeats/availableSeats/status)
- UI 라이브러리는 MUI 로 확정(ADR-0002) — 시안 없음
- 좌석 선택 UI·결제 진입은 범위 밖
- 화면 명세(기본/로딩/빈 목록/오류)가 그대로 완료 조건이 된다
  (`performance-availability.md` §5 "화면 명세")
- `implementation_spec.paths`: `frontend/src/api/performances.ts`,
  `frontend/src/pages/PerformanceListPage.tsx`,
  `frontend/src/pages/PerformanceDetailPage.tsx`
- frontend 디렉터리가 비어 있어 스캐폴딩부터 시작해야 한다 — Phase 1이
  이 부분을 설계에 명시하지 않으면 Phase 3에서 즉흥적으로 결정하게 된다
