---
checkpoint_id: CP-3.1
checkpoint_name: "구현 전략 수립"
task_id: TASK-007
phase: "3"
phase_name: "Phase 3 - Green"
saved_at: 2026-10-01T00:00:00Z
status: ACTIVE

work_summary: "Bean Validation 마이그레이션 구현 순서와 재사용 대상을 확정했다"

progress:
  completed:
    - "TEST_TASK-007.json human_review.approved == true 확인"
    - "PLAN_TASK-007.json 의 target_files 6개·reusable 2건 확인"
  in_progress: "DTO 애노테이션 부여부터 구현 시작"
  blocked: []

next_steps:
  - priority: 1
    task: "DTO 3개에 jakarta.validation 애노테이션 부여 (SectionRequest → Register → Update)"
  - priority: 2
    task: "PerformanceController 의 register/update @RequestBody 에 @Valid 부여"
  - priority: 3
    task: "PerformanceExceptionHandler 에 MethodArgumentNotValidException·HttpMessageNotReadableException 핸들러 추가"
  - priority: 4
    task: "PerformanceService 에서 이관된 필드 형태 검사 제거 (잔존 도메인 규칙만 유지)"

decisions:
  - decision: "구현 순서를 'DTO → Controller → ExceptionHandler → Service 정리' 로 잡는다. Service 정리를 마지막에 하는 이유는, 그 전까지는 Bean Validation 과 기존 수동 검증이 이중으로 동작해 테스트가 계속 green 을 유지하기 때문이다 — Service 를 먼저 비우면 중간 단계에서 회귀 테스트가 대량 깨져 무엇이 원인인지 분간하기 어려워진다"
    rationale: "already_passing 테스트 10건이 안전망 역할을 하므로, 각 단계마다 테스트를 돌려 '아직 깨지지 않았음' 을 확인하며 전진할 수 있다"
    alternatives_considered: ["Service 를 먼저 비우고 애노테이션으로 메우기"]
    impact: "중간 단계마다 전체 스위트를 돌려 회귀 지점을 즉시 특정할 수 있다"
  - decision: "PerformanceService.validateRequired 는 메서드째 삭제한다 — 그 내용(title/venue blank·길이, 시각 3개 null)이 전부 이관 대상 목록에 들어 있다"
    rationale: "태스크 설명이 '대응하는 수동 검증 코드를 걷어낸다'(ADR-0010 결과 절)고 명시했다. 남겨두면 두 곳에서 같은 것을 검사하는 상태가 되어 ADR 의 목적(검증 지점을 한 곳으로)이 달성되지 않는다"
    alternatives_considered: ["이중 방어를 위해 Service 검사도 유지"]
    impact: "Controller 경계(@Valid)가 유일한 필드 형태 검증 지점이 된다. Service 를 직접 호출하면 NPE 가능 — ADR-0010 이 '감수하는 것' 으로 명시한 트레이드오프이며, 호출자는 Controller 뿐임을 Phase 1 에서 확인했다"
  - decision: "validateSection 은 삭제하지 않고 축소한다 — section==null, price<0, seatsPerRow<1, rowStart>rowEnd 4가지만 남긴다"
    rationale: "4가지 모두 Phase 1(CP-1.3)에서 이관 범위 밖으로 확정한 도메인 규칙이다. SC-08/SC-10 과 기존 sc17/sc18/sc23 이 이 4가지를 직접 검증한다"
    alternatives_considered: []
    impact: "검증이 두 갈래로 나뉘는 ADR-0010 의 설계가 코드에 그대로 드러난다"

recovery_prerequisites:
  - CP-2.6

execution_context:
  test_command: "cd backend && ./gradlew test --tests '*PerformanceRegistrationApiTest*'"
  build_command: "cd backend && ./gradlew build"
  env_required: ["JAVA_HOME=JDK 21 경로 (기본 java 는 8)"]
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/api/dto/SectionRequest.java"
    - "backend/src/main/java/com/example/ticket_booking/api/dto/RegisterPerformanceRequest.java"
    - "backend/src/main/java/com/example/ticket_booking/api/dto/UpdatePerformanceRequest.java"
    - "backend/src/main/java/com/example/ticket_booking/api/PerformanceController.java"
    - "backend/src/main/java/com/example/ticket_booking/api/PerformanceExceptionHandler.java"
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/04_plan/PLAN_TASK-007.json"
    - path: "workflow_design/05_scenario/TEST_TASK-007.json"
---

## 무엇을 했나

구현 순서를 정했다. 핵심 판단은 **Service 정리를 마지막에 둔다**는 것이다 —
애노테이션을 먼저 붙이면 그 시점에는 Bean Validation 과 기존 수동 검증이
이중으로 동작해 테스트가 계속 green 을 유지하므로, 단계마다 전체 스위트를 돌려
회귀 지점을 즉시 특정할 수 있다.

## 산출물

(해당 없음 — 전략 수립만)

## 재개 방법

1. `PLAN_TASK-007.json` 의 `codebase_analysis.target_files` 를 읽는다
2. 위 next_steps 순서대로 구현한다
3. 각 단계마다 `./gradlew test` 로 회귀를 확인한다
