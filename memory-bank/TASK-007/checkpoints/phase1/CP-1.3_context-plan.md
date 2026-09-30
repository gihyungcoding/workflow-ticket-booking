---
checkpoint_id: CP-1.3
checkpoint_name: "설계 정규화 완료"
task_id: TASK-007
phase: "1"
phase_name: "Phase 1 - Plan"
saved_at: 2026-09-30T00:00:00Z
status: ACTIVE

work_summary: "공연 등록 API DTO의 Bean Validation 마이그레이션(TASK-007)의 입력·출력·흐름을 정규화하고 PLAN_TASK-007.json 을 작성했다"

progress:
  completed:
    - "architecture.md §2·§3, constraints.yaml(ARCH-001~003), decisions/README.md, ADR-0010 본문을 읽고 계층 배치·의존 방향 확인"
    - "check_architecture.py 실행 — 기존 warn 3건(DESIGN-003, 프론트엔드) 외 신규 위반 없음, 백엔드 제약 전부 통과"
    - "대상 파일 6개(RegisterPerformanceRequest/SectionRequest/UpdatePerformanceRequest/PerformanceController/PerformanceExceptionHandler/PerformanceService)를 코드로 직접 읽어 현재 검증 로직 확인"
    - "PerformanceRegistrationApiTest.java 의 기존 23개 시나리오(sc01~sc24, sc14 결번)를 읽어 회귀 대상과 에러 코드 기대값 확인 — 특히 sc16(sections 필드 없음→INVALID_REQUEST, 이관 범위 밖)·sc17(rowStart>rowEnd 역방향→INVALID_SECTION, 도메인 규칙)·sc18(seatsPerRow=0→INVALID_SECTION, 범위 미검사 잔존)·sc23(sections 배열 null 원소→INVALID_SECTION, cascade 대상 아님) 을 도메인 규칙 잔존 근거로 확정"
    - "PLAN_TASK-007.json 작성 — inputs 8 / outputs 3 / flows 4, acceptance_criteria 4건 전부 flows.covers 로 커버(역방향 확인 통과: extra covers 없음)"

  in_progress: "-"
  blocked: []

next_steps:
  - priority: 1
    task: "wf-scenario 스킬로 넘어가 Given/When/Then 시나리오 작성"
    file: "workflow_design/05_scenario/SCENARIO_TASK-007.md"

decisions:
  - decision: "태스크 설명에 나열된 필드(필수/blank, title·venue 길이, grade 형식·길이, price 존재 여부, rowStart/rowEnd 형식, seatsPerRow 존재 여부)만 Bean Validation으로 옮기고, price>=0·seatsPerRow>=1 임계값 검사와 rowStart<=rowEnd 역방향 판정, sections 리스트 자체의 null/empty 검사는 PerformanceService에 수동 검증으로 남긴다"
    rationale: "태스크 설명이 '존재 여부'라고 명시해 price/seatsPerRow의 하한 검사를 제외했고, rowStart<=rowEnd는 태스크가 명시적으로 '이번 범위가 아니다'라고 한 도메인 규칙 4건 중 하나다. sections null/empty 검사는 validateRequired/validateSection 메서드 밖(registerPerformance 본문)에 있어 태스크가 나열한 이관 대상에 포함되지 않는다"
    alternatives_considered: ["price/seatsPerRow의 하한 검사까지 @Min 으로 전부 이관", "sections null/empty 검사도 @NotEmpty 로 이관"]
    impact: "PerformanceService.validateSection 은 완전히 사라지지 않고 축소된 형태로 남는다 — sc17/sc18/sc23 이 코드 변경 없이 그대로 통과해야 회귀 조건을 만족한다"
  - decision: "MethodArgumentNotValidException 하나로 INVALID_REQUEST 와 INVALID_SECTION 두 에러 코드를 모두 응답해야 하므로(AC2 vs AC3), PerformanceExceptionHandler가 BindingResult의 FieldError 경로가 'sections'로 시작하는지로 분기한다"
    rationale: "태스크 설명은 두 예외 타입을 'INVALID_REQUEST로 통일'한다고 썼지만 AC3는 section 필드 오류를 INVALID_SECTION 으로 요구한다 — 문면 그대로 읽으면 모순이라, section 관련 필드 오류만 INVALID_SECTION으로 남기고 나머지(RegisterPerformanceRequest/UpdatePerformanceRequest 최상위 필드, JSON 파싱 실패)를 INVALID_REQUEST로 통일하는 것으로 해석했다"
    alternatives_considered: ["모든 MethodArgumentNotValidException 을 예외 없이 INVALID_REQUEST 로 통일(AC3 위반이라 기각)", "SectionRequest 전용 별도 검증 예외 클래스 신설(record cascade 로 충분해 불필요)"]
    impact: "Phase 3에서 FieldError 순회·경로 판정 로직을 새로 작성해야 한다. Phase 2a 시나리오는 이 분기를 명시적으로 다뤄야 한다"
  - decision: "route=Backend 단일 태스크로 유지, 프론트엔드 변경 없음"
    rationale: "에러 응답 형태(ErrorResponse{code,message})가 동일하게 유지되므로 프론트엔드가 이미 소비하는 계약이 바뀌지 않는다"
    alternatives_considered: []
    impact: "PLAN 은 backend 파일만 target_files 에 포함"

recovery_prerequisites: []

execution_context:
  test_command: "cd backend && ./gradlew test --tests '*PerformanceRegistrationApiTest*'"
  build_command: "cd backend && ./gradlew build"
  env_required: []
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/api/dto/RegisterPerformanceRequest.java"
    - "backend/src/main/java/com/example/ticket_booking/api/dto/SectionRequest.java"
    - "backend/src/main/java/com/example/ticket_booking/api/dto/UpdatePerformanceRequest.java"
    - "backend/src/main/java/com/example/ticket_booking/api/PerformanceController.java"
    - "backend/src/main/java/com/example/ticket_booking/api/PerformanceExceptionHandler.java"
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/04_plan/PLAN_TASK-007.json"
---

## 무엇을 했나

TASK-007(Bean Validation 마이그레이션)을 TDD로 진입시킬 수 있도록 입력·출력·흐름을
정규화했다. 가장 중요했던 판단은 두 가지다.

첫째, 태스크 설명이 나열한 이관 대상("필수/blank, title·venue 길이, grade 형식·길이,
price 존재 여부, rowStart/rowEnd 형식, seatsPerRow 존재 여부")을 문면 그대로 읽어
price/seatsPerRow의 하한값 검사(>=0, >=1)와 rowStart<=rowEnd 역방향 판정은 Service에
수동 검증으로 남기기로 했다. 기존 테스트 sc17/sc18/sc23이 이 경계를 정확히 찌르고
있어(각각 역방향 range, seatsPerRow=0, sections 배열의 null 원소) Phase 3 구현이
이 세 테스트를 코드 변경 없이 그대로 통과시켜야 회귀 조건을 만족한다는 것을
Plan 단계에서 미리 못박아뒀다.

둘째, 태스크 설명은 "MethodArgumentNotValidException을 INVALID_REQUEST로 통일"한다고
썼지만 AC3는 section 필드 오류를 INVALID_SECTION으로 요구해 문면 그대로는 모순이다.
FieldError 경로가 `sections`로 시작하는지로 분기하는 것으로 해석을 확정하고
decisions에 남겼다 — Phase 2a가 이 분기를 시나리오로 명시적으로 다뤄야 한다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/04_plan/PLAN_TASK-007.json` | 설계 정규화 SoT — route, inputs/outputs/flows, codebase_analysis |

## 재개 방법

1. `PLAN_TASK-007.json` 을 읽는다
2. `wf-scenario` 스킬로 Given/When/Then 시나리오를 작성한다 (F1~F4 각각 최소 1개, happy 1 + error 1 최소)
3. `scenario-validator` 서브에이전트로 검증한 뒤 HITL#1 승인을 받는다
