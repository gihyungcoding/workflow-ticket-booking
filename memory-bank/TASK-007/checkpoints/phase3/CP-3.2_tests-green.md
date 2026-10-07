---
checkpoint_id: CP-3.2
checkpoint_name: "테스트 Green 달성"
task_id: TASK-007
phase: "3"
phase_name: "Phase 3 - Green"
saved_at: 2026-10-01T00:30:00Z
status: ARCHIVED

work_summary: "Bean Validation 마이그레이션 구현 완료 — 전체 58개 테스트 통과(실패 0)"

progress:
  completed:
    - "SectionRequest/RegisterPerformanceRequest/UpdatePerformanceRequest 에 jakarta.validation 애노테이션 부여"
    - "PerformanceController 의 register/update @RequestBody 에 @Valid 부여"
    - "PerformanceExceptionHandler 에 MethodArgumentNotValidException 핸들러 추가 — 최상위 필드 우선 분기(HITL#1 확정 규칙)"
    - "PerformanceExceptionHandler 에 HttpMessageNotReadableException 핸들러 추가 — SC-07 이 Red 에서 Green 으로 전환"
    - "중간 확인: 애노테이션 부여 직후 34/34 통과(이중 검증 상태) — 회귀 없음 확인"
    - "PerformanceService.validateRequired 메서드 삭제 + 호출부 2곳 제거, validateSection 을 도메인 규칙 4가지로 축소, 미사용 상수 3개·헬퍼 2개 제거"
    - "최종 확인: cleanTest 로 전체 재실행 — 58개 전부 통과(6개 테스트 클래스)"
  in_progress: "리팩토링 및 린트"
  blocked: []

next_steps:
  - priority: 1
    task: "CP-3.3 리팩토링 기록 저장"
  - priority: 2
    task: "DEV_TASK-007.json 저장 후 커밋"

decisions:
  - decision: "구현 순서를 'DTO → Controller → ExceptionHandler → Service 정리' 로 지켰고, Service 정리 직전에 중간 테스트를 한 번 더 돌려 이중 검증 상태에서도 34/34 통과를 확인했다"
    rationale: "CP-3.1 에서 정한 전략대로 — 회귀가 발생했다면 어느 단계가 원인인지 즉시 특정할 수 있게 하기 위함"
    alternatives_considered: []
    impact: "실제로 회귀가 한 번도 발생하지 않아 롤백 없이 전진했다"

recovery_prerequisites:
  - CP-3.1

execution_context:
  test_command: "cd backend && ./gradlew cleanTest test"
  build_command: "cd backend && ./gradlew build"
  env_required: ["JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home (기본 java 는 8)"]
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
    - path: "backend/src/main/java/com/example/ticket_booking/api/PerformanceExceptionHandler.java"
---

## 무엇을 했나

PLAN 의 target_files 6개를 모두 구현했다. 실행 결과:

```
cleanTest test → tests=58 failures=0 errors=0

TicketBookingApplicationTests            1
api.PerformanceApiTest                  12
api.PerformanceRegistrationApiTest      34   (기존 23 + TASK-007 신규 11)
api.PerformanceSectionSummaryApiTest     4
domain.PerformanceStatusRulesTest        1
repository.PerformanceConstraintTest     6
```

Phase 2b 에서 유일한 Red 였던 `test_t7_sc07`(파싱 불가능한 JSON → 빈 body)은
`HttpMessageNotReadableException` 핸들러를 추가한 시점에 Green 으로 바뀌었다.
나머지 10건(already_passing)은 구현 전후 모두 통과해 회귀 안전망 역할을 했다.

가장 조심한 지점은 **Service 를 비우는 순서**였다. 애노테이션만 붙인 중간
상태에서 한 번 테스트를 돌려 34/34 통과를 확인한 뒤(이중 검증 상태)
`validateRequired` 를 삭제했다 — 회귀가 생겼다면 어느 쪽이 원인인지 바로
알 수 있는 상태를 유지한 것이다. 실제로 회귀는 한 번도 없었다.

## 산출물

| 파일 | 역할 |
|---|---|
| `api/dto/SectionRequest.java` | grade/rowStart/rowEnd/price/seatsPerRow 애노테이션 |
| `api/dto/RegisterPerformanceRequest.java` | 최상위 5필드 애노테이션 + sections @Valid cascade |
| `api/dto/UpdatePerformanceRequest.java` | 최상위 5필드 애노테이션 |
| `api/PerformanceController.java` | register/update 에 @Valid |
| `api/PerformanceExceptionHandler.java` | 검증 실패·역직렬화 실패 핸들러 2개 |
| `service/PerformanceService.java` | 이관된 검사 제거, 도메인 규칙만 잔존 |

## 재개 방법

1. `./gradlew cleanTest test` 로 58개 통과를 재확인한다
2. CP-3.3(리팩토링)·DEV_TASK-007.json 을 저장하고 커밋한다
