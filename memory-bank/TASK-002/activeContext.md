---
task_id: TASK-002
title: "공연 목록/상세 화면"
phase: "3"
phase_name: "Phase 3 - Green (완료)"
status: ACTIVE
created_at: 2026-09-08
last_updated: 2026-09-08

primary_category: Frontend
sub_categories: ["audience"]
target_repo: "."
branch: "feature/task-002-performance-list-detail-ui"

last_checkpoint: CP-3.4
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-002.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-002.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-002.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-002.json"
  test: "workflow_design/05_scenario/TEST_TASK-002.json"
  dev: "workflow_design/06_dev/DEV_TASK-002.json"
---

## 지금 무엇을 하고 있나

Phase 3(Green)을 마쳤다. `api/performances.ts`(fetch 구현, 404→
PerformanceNotFoundError), `StatusBadge.tsx`(MUI Chip, 5개 상태 매핑),
`PerformanceListPage.tsx`(loading/error/empty/success 4분기),
`PerformanceDetailPage.tsx`(loading/not-found/error/success 4분기)를
채워 대상 테스트 6/6을 Green으로 만들었다. `npm run build` 과정에서 발견한
타입 오류 2종을 수정했다: (1) jest-dom의 vitest 매처 타입은
`'@testing-library/jest-dom/vitest'` 서브패스 임포트로만 활성화됨(일반
`'@testing-library/jest-dom'`으로는 안 됨) (2) 설치된 MUI 9.4.0에서
`Stack`이 `justifyContent`/`alignItems` 직접 prop을 더 이상 지원하지
않아(v9 API 변경) `sx` prop으로 이전. `npx oxlint` error 0(warning 2건,
의도된 패턴이라 유지). 백엔드 `./gradlew test`로 회귀 없음 확인. 리팩토링은
검토 후 하지 않기로 결정(CP-3.3).

## 다음 한 걸음

`wf-verify` 스킬로 Phase 4(검증)를 시작한다 — acceptance_criteria 6건
충족·회귀·보안·범위 이탈을 확인하고 `code-reviewer` 서브에이전트를 호출한
뒤 HITL#3 승인을 받는다.

## 알아둬야 할 것

- **Node.js가 시스템 기본 PATH에 없다** — 매 bash 명령 앞에 `nvm use v22.23.1 &&`
  를 붙여야 한다(TASK-001의 JAVA_HOME 이슈와 같은 종류의 환경 특이사항).
  테스트: `nvm use v22.23.1 && npm --prefix frontend test`, 빌드:
  `nvm use v22.23.1 && npm --prefix frontend run build` (memory:
  project-node-nvm-env)
- Phase 1에서 결정한 두 가지가 여전히 유효: (1) 필터 UI는 공식 AC 밖이라
  구현하지 않음, (2) 프론트-백엔드 연결은 Vite dev proxy로 해결
  (`vite.config.ts` server.proxy)
- SC-05(로딩 스켈레톤) 검증은 `data-testid="performance-card-skeleton"`을
  쓴다 — `PerformanceListPage.tsx`의 `Skeleton`에 이미 부여됨
- 설치된 스택 버전이 최신이라 문서화된 예제와 다를 수 있다: MUI 9.4.0
  (Stack의 justifyContent/alignItems는 sx로), React 19.2.8, Vite 8.2.2,
  TypeScript 6.0.2, react-router-dom 7.18.3, vitest 5.0.0,
  @testing-library/jest-dom 7.0.1(vitest 서브패스 필요)
- 이번 태스크에서는 `frontend/`에 아키텍처 제약(`constraints.yaml`)이
  아직 없다 — Phase 4의 `check_architecture.py`는 백엔드 제약 3건만 검사함

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
