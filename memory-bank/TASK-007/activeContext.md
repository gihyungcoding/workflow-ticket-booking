---
task_id: TASK-007
title: "공연 등록 API DTO를 Bean Validation으로 마이그레이션"
phase: "5"
phase_name: "Phase 5 - Reflect (완료)"
status: DONE
created_at: 2026-09-29
last_updated: 2026-10-07T15:45:00Z

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-007-bean-validation-migration"

last_checkpoint: CP-5.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-007.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-007.md"
  test: "workflow_design/05_scenario/TEST_TASK-007.json"
  dev: "workflow_design/06_dev/DEV_TASK-007.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-007.json"
  reflect: "workflow_design/08_reflect/REFLECT_TASK-007.json"

pull_request:
  number: 9
  url: "https://github.com/gihyungcoding/workflow-ticket-booking/pull/9"
  base: develop
  opened_at: 2026-10-07
---

## 지금 무엇을 하고 있나

완료됐다. Phase 1~5를 모두 거쳤고 HITL 4건 전부 승인받았다. Phase 2a는
독립검증 3회(매회 pass, 경고를 반영해 점진 보완), Phase 4는 WARN(FAIL
없음) — code-reviewer 소견 4건 중 2건은 재현으로 결함 아님 확인, 1건은
ADR-0010이 지목한 ARCH-004를 그 자리에서 추가해 해결, 1건은 예외로 승인.
회고에서 ADR 승격 후보 1건과 규칙 개선안 2건이 나왔으나 사용자가 "승인하고
종료"로 처리 — 별도 후속 작업 없이 제안으로만 기록됨.

**Ship 중 발생한 2차 Phase 4 재검증**: `/wf-ship` 진행 중 ship_preflight.py가
신선도 FAIL을 보고했다 — TASK-006(PR #8)이 develop에 먼저 머지되면서 같은
파일(SectionRequest.java, PerformanceService.java)에 충돌이 발생했다.
`git merge develop`으로 단일 커밋에서 충돌을 해결(TASK-006의 BigDecimal
타입·범위·정수성 검사 보존 + TASK-007의 Bean Validation 이관 적용)했고,
Phase 4를 재수행해 HITL#3 재승인(status: WARN, 5건)을 받았다. 신규 발견
WARN 2건 중 주석 부정확 1건은 그 자리에서 정정, price=null 테스트 공백
1건은 WARN으로 유지.

## 다음 한 걸음

커밋 후 `/wf-ship`을 재시도한다 — `ship_preflight.py` 7항목 재확인.

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
  대조로 확인)** — Phase 2b에서 사용자 확인을 거쳐 "그칙 원칙대로 신규 테스트
  메서드 작성"으로 처리했다(기존 헬퍼 재사용으로 코드 중복 최소화). 해결됨
- **Phase 3에서 확인된 사실**: `List<@Valid SectionRequest>` cascade는 배열의
  null 원소를 검증하지 않고 건너뛴다(Plan unresolved 2번째 항목의 답). 그래서
  `sections=[null]`은 Bean Validation을 통과하고 `PerformanceService`의
  `section == null` 체크가 잡는다 — 이 체크를 지우면 sc23/SC-10이 깨진다
- **Phase 4에서 code-reviewer가 지적한 WARN 3건(결함 아님, 재현으로 확인)**:
  (1) `@Pattern(^[A-Z]$)` 고유 위반(소문자·2자 이상)을 때리는 회귀 테스트가
  없다 — 정규식이 깨져도 58개 테스트가 못 잡는다(현재 코드는 정상),
  (2) venue/시각3개/price의 `@NotNull`도 title 외엔 테스트가 없다(현재 코드는
  500 아니라 400을 정상 반환), (3) `PerformanceExceptionHandler`가 전역
  `@RestControllerAdvice`인데 `"sections["` 라는 공연 도메인 전용 문자열로
  분기한다 — 컨트롤러가 하나뿐인 지금은 문제없지만 다음 컨트롤러가 생기면
  `assignableTypes = PerformanceController.class`로 좁혀야 함. 셋 다 Phase 5
  회고 제안으로 넘김
- **ARCH-004 신규 추가(Phase 4)**: ADR-0010이 이 태스크 안에서 추가하라고
  명시했던 제약 — `@RequestBody` 파라미터에는 `@Valid`가 있어야 한다
  (`constraints.yaml`, severity: error, 패턴 `^(?!.*@Valid).*@RequestBody`)
- **`RegisterPerformanceRequest.sections`에는 `@NotNull`을 붙이지 않았다** —
  붙이면 "필드 누락"(sc16: INVALID_REQUEST)과 "빈 배열"(sc13: EMPTY_SECTIONS)의
  구분이 사라진다. 두 판정은 Service에 남아 있다
- **`validateRequired`는 메서드째 삭제됐다** — Controller 경계(`@Valid`)가 유일한
  필드 형태 검증 지점이다. 그래서 `validateSection`이 `price()<0`,
  `rowStart().charAt(0)` 처럼 null 체크 없이 바로 접근하는 것은 `@NotNull`·
  `@Pattern`의 보장에 의존한 의도적 선택이다(ADR-0010이 명시한 트레이드오프)
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
