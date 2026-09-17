---
task_id: TASK-005
title: "공연 등록/수정 화면"
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
status: ACTIVE
created_at: 2026-09-17
last_updated: 2026-09-17

primary_category: Frontend
sub_categories: ["organizer"]
target_repo: "."
branch: "feature/task-005-performance-register-edit-screen"

last_checkpoint: CP-1.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-005.json"
---

## 지금 무엇을 하고 있나

Phase 1(Plan)을 마쳤다. route=Frontend, inputs 5/outputs 7/flows 7, AC 8건
전부 flows로 커버됨(EXIT GATE 통과). 백엔드 PerformanceResponse.java를 직접
읽어 sections 필드가 GET 단일 상세 조회에서만 채워짐을 확인했다.

## 다음 한 걸음

`wf-scenario` 스킬로 넘어가 PLAN의 F1~F7 각각에 대응하는 Given/When/Then
시나리오를 작성하고 scenario-validator 검증 → HITL#1 승인을 받는다.

## 알아둬야 할 것

- 요구사항 문서: `docs/product/features/performance-registration.md`
  (Task B, §1-2·2·4·5·6)
- 인터랙티브 좌석 맵 드래그 편집기는 범위 밖(§2-2, §7)
- `sections` 는 `GET /api/performances/{id}`(단일 상세)에서만 채워진다 —
  목록/등록(POST)/수정(PUT)/취소 응답은 `sections=null`이라 키 자체가 없음
  (`@JsonInclude(NON_NULL)`). 수정 폼의 좌석 구성 요약은 반드시 별도 GET으로
  가져온다
- AC에 없는 에러 코드 4건(EMPTY_SECTIONS/INVALID_SECTION/INVALID_REQUEST/
  DUPLICATE_SEAT_RANGE)과 수정 화면의 404는 전용 UI를 만들지 않고 AC5
  실패 배너로 폴백 처리하기로 CP-1.3에서 결정함 — Phase 2a 시나리오도 이
  경계를 따른다
- 검증 오류 매핑: `INVALID_TIME_ORDER`/`SEAT_LIMIT_EXCEEDED`(400, 인라인),
  `REGISTRATION_ALREADY_OPEN`(409, 수정 전용 안내), 나머지는 일반 실패 배너
- 취소는 확인 다이얼로그 → `POST /api/performances/{id}/cancel` (멱등)
- 구현 대상 경로: `frontend/src/api/performances.ts`(확장),
  `frontend/src/pages/PerformanceRegisterPage.tsx`(신규),
  `frontend/src/pages/PerformanceEditPage.tsx`(신규),
  `frontend/src/App.tsx`(라우트 2건 추가 — PLAN에서 새로 발견, 요구사항
  문서 §6에는 없음)
- 새 npm 의존성(폼 라이브러리 등)을 도입하지 않는다 — 기존 페이지처럼
  useState로 직접 폼 상태 관리 (CP-1.3 결정)
- UI 라이브러리 MUI(ADR-0002), 디자인 정본 `docs/product/design.md` ·
  `design-tokens.css`. 재사용: PerformanceListPage의 Alert+[다시 시도]
  패턴(L53-65), PerformanceDetailPage의 상태 유니온+useEffect 패턴
- DESIGN-003(warn) 기존 위반 2건 있음(PerformanceDetailPage/ListPage의
  height={n}) — 새 코드에서 반복하지 않도록 유의
