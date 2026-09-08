---
task_id: TASK-002
title: "공연 목록/상세 화면"
phase: "2b"
phase_name: "Phase 2b - Red (완료)"
status: ACTIVE
created_at: 2026-09-08
last_updated: 2026-09-08

primary_category: Frontend
sub_categories: ["audience"]
target_repo: "."
branch: "feature/task-002-performance-list-detail-ui"

last_checkpoint: CP-2.6
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-002.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-002.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-002.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-002.json"
  test: "workflow_design/05_scenario/TEST_TASK-002.json"
---

## 지금 무엇을 하고 있나

Phase 2b(Red)를 마쳤다. `frontend/`가 완전히 비어 있던 상태에서 Vite+React+TS
스캐폴딩부터 시작했다: `npm create vite@latest . -- --template react-ts` →
`@mui/material`/`@emotion/*`/`react-router-dom`(런타임) +
`vitest`/`@testing-library/react`/`@testing-library/jest-dom`/
`@testing-library/user-event`/`jsdom`(개발) 설치 → `vite.config.ts`에
`/api` dev proxy(→localhost:8080)와 vitest 설정 추가.

SC-01~06을 `PerformanceListPage.test.tsx`(4건)·`PerformanceDetailPage.test.tsx`
(2건)로 옮겨 6/6 Red 확인. `PerformanceListPage`/`PerformanceDetailPage`/
`StatusBadge`/`api/performances.ts`는 타입만 실제로 채우고 함수·컴포넌트
본문은 `throw new Error('Not implemented')`(반환형 `never`)로 스텁 처리했다
— TASK-001에서 승인된 Java TDD Red 스켈레톤 관례(memory:
feedback-java-tdd-red-skeleton)를 TS/React에 그대로 적용한 것.
`tsc --noEmit` 오류 0건으로 컴파일 오류가 아님을 확인했다.

HITL#2를 `AskUserQuestion`으로 받아 사용자가 승인했다.

## 다음 한 걸음

`wf-develop` 스킬로 Phase 3(Green)을 시작한다 — 6개 테스트를 통과시키는
최소 구현을 `PerformanceListPage.tsx`/`PerformanceDetailPage.tsx`/
`StatusBadge.tsx`/`api/performances.ts`에 채운다.

## 알아둬야 할 것

- **Node.js가 시스템 기본 PATH에 없다** — 매 bash 명령 앞에 `nvm use v22.23.1 &&`
  를 붙여야 한다(TASK-001의 JAVA_HOME 이슈와 같은 종류의 환경 특이사항).
  테스트: `nvm use v22.23.1 && npm --prefix frontend test`
- Phase 1에서 결정한 두 가지가 여전히 유효: (1) 필터 UI는 공식 AC 밖이라
  구현하지 않음, (2) 프론트-백엔드 연결은 Vite dev proxy로 해결
- SC-05(로딩 스켈레톤) 검증은 `data-testid="performance-card-skeleton"`을
  쓴다 — Phase 3에서 MUI `Skeleton`에 이 testid를 3~6개 부여해야 Green이 됨
- PerformanceDetailPage.test.tsx는 `vi.mock`을 `importOriginal`로 부분
  모킹한다(`PerformanceNotFoundError`는 실물 유지, `getPerformance`만 mock)
  — PerformanceListPage.test.tsx는 전체 automock
- 최초 작성 시 실수로 프로덕션 로직을 전부 구현했다가(FORBIDDEN 위반) 되돌린
  이력이 있다 — Phase 3에서는 이미 설계된 로직(원래 작성했던 fetch/렌더링
  코드)을 참고해도 되지만, git으로 남아있지 않으므로 새로 작성해야 한다

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
