---
checkpoint_id: CP-3.2
checkpoint_name: "Green 확인 — 전체 테스트 통과"
task_id: TASK-006
phase: "3"
phase_name: "Phase 3 - Green"
saved_at: 2026-09-28T00:35:00Z
status: ACTIVE

work_summary: "validateSection에 정수 확인(F1/F2) 추가, intValue()→intValueExact() 교체. 대상 테스트 3/3 + 전체 스위트 50/50 통과, spotlessCheck error 0"

progress:
  completed:
    - "validateSection에 isIntegral() 헬퍼와 price/seatsPerRow 정수 확인 추가 (null 체크와 범위 체크 사이)"
    - "검증 통과 후 변환을 intValue()→intValueExact()로 교체 (registerPerformance 합산, generateSeats 반복문·Seat 생성자)"
    - "./gradlew compileJava compileTestJava → BUILD SUCCESSFUL"
    - "./gradlew test --tests \"*PerformanceRegistrationApiTest*\" → 26/26 통과"
    - "./gradlew test (전체 스위트) → 50/50 통과, 회귀 없음"
    - "./gradlew spotlessCheck → BUILD SUCCESSFUL, error 0"
  in_progress: "리팩토링 검토 (Step 4)"
  blocked: []

next_steps:
  - priority: 1
    task: "리팩토링 필요성 검토 후 DEV_TASK-006.json·CP-3.4 저장"

decisions:
  - decision: "isIntegral 판정은 stripTrailingZeros().scale() 대신 remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO)==0 방식을 쓴다"
    rationale: "BigDecimal.ZERO.stripTrailingZeros()는 구버전 JDK에서 scale이 0이 아닌 값으로 남는 알려진 버그가 있었다 — remainder 방식은 부호·0 경계에서 항상 안전하다"
    alternatives_considered: ["stripTrailingZeros().scale() <= 0"]
    impact: "isIntegral(BigDecimal.ZERO) == true, isIntegral(new BigDecimal(\"120000.00\")) == true 모두 정확히 판정됨(수동 확인)"

recovery_prerequisites:
  - CP-2.6

execution_context:
  test_command: "./gradlew test"
  build_command: "./gradlew build"
  env_required: ["JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home"]
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/06_dev/DEV_TASK-006.json"
---

## 무엇을 했나

Red였던 SC-01/SC-02를 통과시키는 최소 구현을 했다 — `validateSection`에 `isIntegral()`
정수 확인을 price/seatsPerRow 각각의 null 체크와 범위 체크 사이에 추가했다. 검증을
통과한 뒤의 BigDecimal→int 변환은 `intValue()`(절삭)에서 `intValueExact()`로 바꿔
"이미 정수임이 보장된 값을 안전하게 변환한다"는 의도를 명확히 했다. 대상 테스트
3건과 전체 스위트 50건이 모두 통과했고, 포맷 검사도 통과했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `backend/src/main/java/.../PerformanceService.java` | isIntegral 검증 + intValueExact 변환 |

## 다음 단계

리팩토링 필요성 검토 (Step 4) 후 DEV JSON·CP-3.4 저장.
