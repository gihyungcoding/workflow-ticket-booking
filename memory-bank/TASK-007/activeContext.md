---
task_id: TASK-007
title: "공연 등록 API DTO를 Bean Validation으로 마이그레이션"
phase: "2a"
phase_name: "Phase 2a - Scenario Design (완료)"
status: ACTIVE
created_at: 2026-09-29
last_updated: 2026-09-30

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-007-bean-validation-migration"

last_checkpoint: CP-2.4
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-007.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-007.md"
  test: null
---

## 지금 무엇을 하고 있나

Phase 2a를 마쳤다. 시나리오 11건(happy 1 / error 1 / regression 9), 독립검증
3회 모두 pass, HITL#1 승인 완료. generate_red_trigger=true.

## 다음 한 걸음

`wf-red` 스킬로 Phase 2b(Red 테스트 작성)를 시작한다.

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
  INVALID_SECTION, 아니면 INVALID_REQUEST로 판정하기로 Phase 1에서 확정.
  **단, 최상위 필드와 section 필드를 동시에 위반하면 최상위(INVALID_REQUEST)가
  우선한다** — Phase 2a HITL에서 확정(SC-11), 'sections'로 시작하지 않는
  FieldError가 하나라도 있으면 INVALID_REQUEST, 전부 'sections'일 때만
  INVALID_SECTION
- **이 태스크는 사실상 순수 리팩토링이다** — AC1~3이 검증하는 입력은 전부
  현재(마이그레이션 전) 코드에서도 이미 같은 응답을 낸다. Phase 2b에서 실제로
  새로 실패하는 시나리오는 SC-07(HttpMessageNotReadableException 핸들러 부재)
  하나뿐이다. 나머지 regression 시나리오(SC-02~06/08/09/10/11)는 "현재도 통과 +
  Phase 3 이후에도 계속 통과해야 하는" 캐릭터라이제이션 테스트로 Phase 2b가
  다뤄야 한다
- **SC-03/04/05/08/09/10은 기존 PerformanceRegistrationApiTest.java의
  sc22/21/19/18/24/23과 입력·단언이 완전히 동일하다(3차 독립검증 V9에서 파일
  대조로 확인)** — Phase 2b가 이 6건을 신규 테스트 메서드로 쓸지, 기존 테스트
  재실행 참조로 대체할지 명시해야 한다(미해결, VALIDATION_TASK-007.json
  post_validation_fixes.V9 참고)
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
