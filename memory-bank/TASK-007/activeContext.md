---
task_id: TASK-007
title: "공연 등록 API DTO를 Bean Validation으로 마이그레이션"
phase: "1"
phase_name: "Phase 1 - Plan (완료)"
status: ACTIVE
created_at: 2026-09-29
last_updated: 2026-09-30

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-007-bean-validation-migration"

last_checkpoint: CP-1.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-007.json"
---

## 지금 무엇을 하고 있나

Phase 1을 마쳤다. route=Backend, inputs 8/outputs 3/flows 4, AC 4건 전부
flows.covers 로 커버(역방향 확인 통과). 대상 파일 6개(DTO 3개, Controller,
ExceptionHandler, Service) 확정.

## 다음 한 걸음

`wf-scenario` 스킬로 Phase 2a(시나리오 설계)를 시작한다.

## 알아둬야 할 것

- ADR-0010(2026-09-16 채택)에 따른 태스크. 필드 형태 검증(필수/길이/범위/형식)을
  DTO의 `jakarta.validation` 애노테이션으로 옮기고, 여러 필드·DB 상태를 엮는
  도메인 규칙(시각 순서, 좌석 상한, 행 범위 겹침)은 Service 계층 수동 검증으로
  남긴다 — 두 갈래 판단 기준은 "이 필드 하나만 보고 판단 가능한가"(Bean
  Validation) vs "다른 필드나 DB 상태를 함께 봐야 하는가"(수동)
- **Phase 1 핵심 결정 (CP-1.3 참고)**: price/seatsPerRow는 "존재 여부"(@NotNull)만
  이관하고 하한값 검사(>=0, >=1)는 Service에 남긴다. rowStart<=rowEnd 역방향
  판정과 sections 리스트 자체의 null/empty 검사도 이관 범위 밖 — 코드 변경 없이
  그대로 유지해야 sc17/sc18/sc23이 회귀 없이 통과한다
- **MethodArgumentNotValidException 분기가 이 태스크의 핵심 설계다**: 태스크
  설명은 "INVALID_REQUEST로 통일"이라 썼지만 AC3는 section 필드 오류를
  INVALID_SECTION으로 요구 — FieldError 경로가 `sections`로 시작하면
  INVALID_SECTION, 아니면 INVALID_REQUEST로 판정하기로 Phase 1에서 확정
- `MethodArgumentNotValidException`과 기존 `HttpMessageNotReadableException`을
  모두 `INVALID_REQUEST(ErrorResponse{code,message})`로 통일하는 핸들러를
  `PerformanceExceptionHandler`에 추가해야 한다
- price/seatsPerRow의 JSON 소수 절삭 문제(TASK-006)는 이 태스크가 흡수하지
  않는다 — 별개 문제
- **TASK-006과 대상 파일이 겹친다** (`SectionRequest.java`,
  `PerformanceService.java`). TASK-006은 PR #8이 아직 `develop`에 머지되지
  않은 상태 — 이 브랜치는 TASK-006 변경분을 포함하지 않은 `develop`에서
  분기했다. TASK-006이 먼저 머지되면 이 브랜치에 리베이스/머지 충돌 가능성이
  있음을 Phase 3~4에서 염두에 둘 것
- `ARCH-002`(Service 계층은 `api.*` 패키지 import 금지)와 충돌하지 않는지
  Phase 1에서 확인 필요 — ADR-0010은 애노테이션이 DTO 자체에 선언되고 Spring이
  Controller 경계에서 검증하므로 문제없다고 명시함
- 대상 파일 6개: `RegisterPerformanceRequest.java`, `SectionRequest.java`,
  `UpdatePerformanceRequest.java`, `PerformanceController.java`,
  `PerformanceExceptionHandler.java`, `PerformanceService.java`
- AC 4건: 기존 23개 시나리오 회귀 없음 / title·venue 형식 오류 시 400
  INVALID_REQUEST / section 필드 형식 오류 시 400 INVALID_SECTION / JSON 파싱
  불가 시 400 INVALID_REQUEST(빈 body 아님)
