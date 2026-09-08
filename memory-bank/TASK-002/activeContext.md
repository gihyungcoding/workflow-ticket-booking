---
task_id: TASK-002
title: "공연 목록/상세 화면"
phase: "2a"
phase_name: "Phase 2a - Scenario Design (완료)"
status: ACTIVE
created_at: 2026-09-08
last_updated: 2026-09-08

primary_category: Frontend
sub_categories: ["audience"]
target_repo: "."
branch: "feature/task-002-performance-list-detail-ui"

last_checkpoint: CP-2.4
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-002.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-002.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-002.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-002.json"
---

## 지금 무엇을 하고 있나

Phase 2a(Scenario)를 마쳤다. SC-01~06(happy 2/error 2/boundary 2)을 작성해
`acceptance_criteria` 6/6을 전부 커버했고, `scenario-validator` 독립검증이
PASS(경고 1건)했다. 경고 두 건은 승인 전에 바로 고쳤다:
- SC-01: "첫 번째/두 번째 카드" 위치 기반 단언 → 제목("재즈의 밤"=OPEN,
  "클래식 갈라"=SOLD_OUT) 기반으로 변경(응답 순서=렌더 순서라는, 명세에
  없는 전제 제거)
- SC-03: Given "500 응답 또는 네트워크 오류"(OR) → "500 응답"으로 단일화
  (Red 단계 재현 대상 확정)

HITL#1을 `AskUserQuestion`으로 받아 사용자가 승인했다.
`validate_phase2a_gate.py` 7/7 통과.

## 다음 한 걸음

`wf-red` 스킬로 Phase 2b를 시작한다 — SC-01~06을 실패하는 테스트 코드로
옮긴다. `frontend/`가 아직 비어 있으므로 Vitest+Testing Library 스캐폴딩도
이 단계에서 함께 갖춰야 한다(PLAN_TASK-002.json codebase_analysis 참고).

## 알아둬야 할 것

- Phase 1에서 결정한 두 가지가 여전히 유효: (1) 필터 UI는 공식 AC 밖이라
  구현하지 않음, (2) 프론트-백엔드 연결은 Vite dev proxy로 해결
- SC-01/SC-03 문구를 수정했지만 재검증은 호출하지 않았다 — 타입·covers·
  flow·개수 불변이라 안전하다고 판단(CP-2.3 decisions 참고)

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
