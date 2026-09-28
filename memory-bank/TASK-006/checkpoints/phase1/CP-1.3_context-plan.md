---
checkpoint_id: CP-1.3
checkpoint_name: "설계 정규화 완료 (PLAN_TASK-006.json)"
task_id: TASK-006
phase: "1"
phase_name: "Phase 1 - Plan"
saved_at: 2026-09-18T00:00:00Z
status: ARCHIVED

work_summary: "구역 price/seatsPerRow 정수 강제 검증 설계 정규화 — Jackson 절삭 문제를 BigDecimal 타입 변경 + Service 정수 검증으로 해결"

progress:
  completed:
    - "architecture.md·ADR 확인 — ADR-0010(Bean Validation, TASK-007이 마이그레이션 담당)이 이 태스크와 직결됨을 확인"
    - "SectionRequest→SectionSpec→PerformanceService.validateSection 흐름 코드 추적, Jackson ACCEPT_FLOAT_AS_INT가 Integer 필드에서 소수를 절삭한다는 원인을 코드 레벨로 재확인"
    - "route=Backend 결정, target_files 3건 확정(SectionRequest.java, SectionSpec.java, PerformanceService.java)"
    - "PLAN_TASK-006.json 작성 — inputs/outputs/flows 3건, AC 3건 모두 flows.covers로 커버"
  in_progress: "없음 — Phase 1 완료"
  blocked: []

next_steps:
  - priority: 1
    task: "wf-scenario로 Phase 2a 진입 — GWT 시나리오 설계"
    file: "workflow_design/05_scenario/SCENARIO_TASK-006.md"

decisions:
  - decision: "SectionRequest·SectionSpec의 price/seatsPerRow 타입을 Integer → BigDecimal로 바꾸고, 정수 여부 확인은 PerformanceService.validateSection에서 한다 (Controller/toSectionSpec에는 로직을 두지 않는다)"
    rationale: "Jackson의 ACCEPT_FLOAT_AS_INT 기본 설정 때문에 Integer 필드는 역직렬화 시점에 이미 소수부가 사라져 Service에서 되돌릴 수 없다. BigDecimal로 바꾸면 원본 값이 보존되어 기존 관례(shape 검증도 아직 Service에 있음 — ADR-0010 마이그레이션 전)대로 Service에서 검증할 수 있다. API 계층에 검증 로직을 두면 architecture.md §2(API는 비즈니스 로직을 하지 않는다)를 어긴다"
    alternatives_considered:
      - "Jackson ObjectMapper에서 ACCEPT_FLOAT_AS_INT=false로 전역 설정 — 기각: 역직렬화 실패가 HttpMessageNotReadableException으로 던져지는데 현재 이를 처리하는 핸들러가 없고, 있다 해도 ADR-0010은 이 경로를 INVALID_REQUEST로 묶기로 예고했다(TASK-007). AC가 요구하는 INVALID_SECTION과 코드가 어긋나고, 아직 시작 안 된 TASK-007의 설계를 선점하게 된다",
      - "Bean Validation(@Digits 등)을 SectionRequest에 지금 바로 도입 — 기각: ADR-0010은 마이그레이션을 TASK-007로 명시적으로 미뤘다. 이 태스크에서 선제 도입하면 TASK-007과 중복 작업·충돌 소지"
    impact: "target_files에 SectionSpec.java 추가 (task의 implementation_spec.paths에는 없었음). PerformanceController.java는 타입만 맞으면 코드 변경 불필요할 것으로 예상 — Phase 3에서 컴파일로 확인"

recovery_prerequisites: []

execution_context:
  test_command: "./gradlew test --tests \"*PerformanceRegistrationApiTest*\""
  build_command: "./gradlew build"
  env_required: []
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/api/dto/SectionRequest.java"
    - "backend/src/main/java/com/example/ticket_booking/service/SectionSpec.java"
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/04_plan/PLAN_TASK-006.json"
---

## 무엇을 했나

TASK-006(구역 price/seatsPerRow 정수 강제 검증)의 설계를 정규화했다. 원인은 TASK-004
Phase 4에서 이미 재현됐지만(Jackson이 Integer 필드에 소수를 절삭), 그 사실만으로는
고칠 수 없어 코드를 직접 추적했다 — SectionRequest(Integer) → Controller.toSectionSpec →
SectionSpec(Integer) → PerformanceService.validateSection. Integer 타입인 이상 Jackson이
역직렬화 시점에 이미 소수부를 버리므로 Service에서는 원본 값을 볼 수 없다는 게 핵심
문제였다. ADR-0010(Bean Validation 도입, 2026-09-16 채택)을 읽고 이 태스크가 그 ADR의
"후속 마이그레이션(TASK-007)"과 겹치지 않는지 확인했다 — ADR-0010은 마이그레이션을
명시적으로 후속 태스크로 미뤘으므로, 이번 태스크는 기존 수동 검증 관례를 그대로 따르되
타입만 BigDecimal로 바꿔 원본 값을 보존하는 방식으로 설계했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/04_plan/PLAN_TASK-006.json` | inputs 2 / outputs 3 / flows 3, AC 3건 전부 커버 |

## 다음 단계

`wf-scenario` 로 Phase 2a 진입 — GWT 시나리오 작성 (happy: 정수 등록 성공,
error: price 비정수, error: seatsPerRow 비정수).
