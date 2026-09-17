---
task_id: TASK-005
title: "공연 등록/수정 화면"
phase: "1"
phase_name: "Phase 1 - Plan"
status: ACTIVE
created_at: 2026-09-17
last_updated: 2026-09-17

primary_category: Frontend
sub_categories: ["organizer"]
target_repo: "."
branch: "feature/task-005-performance-register-edit-screen"

last_checkpoint: "-"
artifacts: {}
---

## 지금 무엇을 하고 있나

태스크를 시작했다. TASK-004(등록/수정/취소 API)와 TASK-008(구역별 좌석
구성 요약 API)이 모두 done 이라 의존성이 해소됐다. Phase 1(wf-plan)을
시작할 차례다.

## 다음 한 걸음

`wf-plan` 스킬로 넘겨 코드베이스를 조사하고 PLAN_TASK-005.json 을 만든다.

## 알아둬야 할 것

- 요구사항 문서: `docs/product/features/performance-registration.md`
  (Task B, §1-2·2·4·5·6)
- 인터랙티브 좌석 맵 드래그 편집기는 범위 밖(§2-2, §7)
- 등록: 기본 정보 + 구역 반복 입력(등급·가격·행 범위·행당 좌석 수) +
  생성될 좌석 수 미리보기(클라이언트 계산)
- 수정: `title`/`venue`/`startAt`/`openAt`/`closeAt` 만 변경 가능. 좌석
  구성은 읽기 전용 요약만 표시(TASK-008이 만든 sections 응답 사용)
- 검증 오류 매핑: `INVALID_TIME_ORDER`/`SEAT_LIMIT_EXCEEDED`/
  `EMPTY_SECTIONS`(400), `DUPLICATE_SEAT_RANGE`(409, 등록),
  `REGISTRATION_ALREADY_OPEN`(409, 수정), `PERFORMANCE_NOT_FOUND`(404)
- 취소는 확인 다이얼로그 → `POST /api/performances/{id}/cancel` (멱등)
- 구현 대상 경로(§6): `frontend/src/api/performances.ts`(확장),
  `frontend/src/pages/PerformanceRegisterPage.tsx`(신규),
  `frontend/src/pages/PerformanceEditPage.tsx`(신규)
- UI 라이브러리 MUI(ADR-0002), 디자인 정본 `docs/product/design.md` ·
  `design-tokens.css`
- TASK-008 노트: `PerformanceResponse` 를 직접 생성하는 곳은
  `PerformanceService.java` 한 곳뿐 — sections 응답 스키마는
  grade/price/seatCount, ORDER BY로 정렬 보장됨
