---
checkpoint_id: CP-3.2
checkpoint_name: "테스트 전체 통과 (재시도)"
task_id: TASK-004
phase: "3"
phase_name: "Phase 3 - Green"
saved_at: 2026-09-15T04:00:00Z
status: ACTIVE

work_summary: "F10/F11 검증, long 산술, Seat FK 관계를 구현해 전체 스위트 40/40 통과"

progress:
  completed:
    - "InvalidRequestException/InvalidSectionException 신규, PerformanceExceptionHandler에 400 매핑 2건 추가"
    - "PerformanceService.validateRequired(F10)/validateSection(F11) 추가, register/update 진입부에서 호출"
    - "좌석 합산을 long(totalSeatsLong)으로 계산 후 5,000 이하 확인, 그 다음에만 int로 캐스팅 — 오버플로로 상한 우회되던 보안 결함 수정"
    - "Seat.performanceId(Long) → @ManyToOne performance(Performance)로 교체, getPerformanceId()는 위임으로 기존 API 유지 — V2 마이그레이션 FK를 엔티티가 표현하도록 수정"
    - "PerformanceController: request.sections()가 null이면 NPE 대신 null을 그대로 Service에 전달"
    - "SeatLimitExceededException 생성자를 long으로 변경"
    - "./gradlew test → 40/40 통과 (신규 20 + 기존 20, 회귀 없음)"
    - "./gradlew spotlessCheck 2회 FAIL → spotlessApply 2회 적용 → 재검증 통과, 포맷 적용 후 재실행 40/40 유지"
  in_progress: "리팩토링 검토(Step 4) — 필요 없다고 판단"
  blocked: []

next_steps:
  - priority: 1
    task: "DEV_TASK-004.json 저장 완료 확인 후 CP-3.4 retry1 저장, 커밋"
  - priority: 2
    task: "wf-verify 스킬로 Phase 4 재검증"

decisions:
  - decision: "Seat와 Performance 사이에 @ManyToOne 관계를 도입했지만 Service는 여전히 Long이 아닌 Performance 객체를 넘기는 방식으로 바꿨다 (EntityManager.getReference() 등 JPA 전용 기능은 쓰지 않음)"
    rationale: "registerPerformance가 이미 저장 직후의 Performance 객체를 갖고 있어, 이를 그대로 Seat 생성자에 넘기면 Service가 영속성 프레임워크 타입(EntityManager, Specification 등)을 직접 다루지 않아도 된다 — ARCH-003 위반 없이 FK 관계를 만들 수 있는 유일한 방법이었다"
    alternatives_considered: ["Service에 EntityManager 주입 후 getReference()로 프록시 생성 — ARCH-003(Service는 영속성 프레임워크 타입을 직접 다루지 않는다) 위반"]
    impact: "Seat 생성자 시그니처가 Long → Performance로 바뀌었지만 Service 내부 호출부만 영향받고 테스트의 getPerformanceId() 호출은 그대로 유지된다"

recovery_prerequisites:
  - CP-2.6_retry1

execution_context:
  test_command: "cd backend && ./gradlew test"
  build_command: "cd backend && ./gradlew build"
  env_required: ["JAVA_HOME을 JDK21로 설정"]
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"
    - "backend/src/main/java/com/example/ticket_booking/domain/Seat.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/06_dev/DEV_TASK-004.json"
---

## 무엇을 했나

Phase 4에서 재현된 결함을 전부 고쳤다 — F10(필수 필드)/F11(구역 형식) 검증을
추가하고, 좌석 합산을 long으로 바꿔 오버플로로 상한이 우회되던 보안 결함을 막았고,
Seat 엔티티에 실제 FK 관계를 추가해 테스트/프로덕션 스키마 정합성을 맞췄다.
전체 40개 테스트가 통과하고 린트도 깨끗하다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/06_dev/DEV_TASK-004.json` | attempt 2 변경 파일, 테스트/린트 결과 (attempt 1 기록 보존) |

## 재개 방법

1. CP-3.4 retry1 저장, activeContext 갱신, 커밋
2. `wf-verify` 스킬로 Phase 4 재검증 — 이전 FAIL 사유(AC4 우회, DoS, 입력검증 부재)가
   실제로 해소됐는지 code-reviewer로 다시 확인한다
