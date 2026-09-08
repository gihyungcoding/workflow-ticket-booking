---
task_id: TASK-002
title: "공연 목록/상세 화면"
phase: "1"
phase_name: "Phase 1 - Plan (완료)"
status: ACTIVE
created_at: 2026-09-08
last_updated: 2026-09-08

primary_category: Frontend
sub_categories: ["audience"]
target_repo: "."
branch: "feature/task-002-performance-list-detail-ui"

last_checkpoint: CP-1.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-002.json"
---

## 지금 무엇을 하고 있나

Phase 1(Plan)을 마쳤다. route는 Frontend로 확정했고, `frontend/` 가
완전히 비어 있는 상태(이 프로젝트 첫 프론트 태스크)임을 확인해
`PLAN_TASK-002.json` 의 `codebase_analysis.target_files` 11건을 전부 신규
생성 대상으로 정리했다(Vite+React+TS 스캐폴딩, MUI, react-router-dom,
Vitest+Testing Library). inputs 3 / outputs 6 / flows 7(F1~F6가 공식
acceptance_criteria 6건을 전부 커버, F7은 화면 명세 보충용)로 정규화했다.

두 가지를 unresolved로 남겼다(사용자 확인 없이 진행하되 추적 가능하게 기록):
1. 요구사항 문서 §1-2 표는 Task B 범위에 "필터"를 언급하지만 §4/tasks.json의
   공식 acceptance_criteria 6건에는 없다 — 공식 AC를 완료 정의로 삼아 이번
   태스크에서는 필터 UI를 구현하지 않는다.
2. 프로덕션 CORS/리버스 프록시 전략은 미정 — 개발 환경은 Vite dev 서버
   proxy(`/api` → `localhost:8080`)로 해결(백엔드 변경 없음, Frontend route
   범위 안).

## 다음 한 걸음

`wf-scenario` 스킬로 Phase 2a를 시작한다 — F1~F7을 Given/When/Then
시나리오로 옮기고 `scenario-validator` 서브에이전트 검증을 거쳐 HITL#1
승인을 받는다.

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
