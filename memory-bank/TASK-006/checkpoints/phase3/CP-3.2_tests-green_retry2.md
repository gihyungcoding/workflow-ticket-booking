---
checkpoint_id: CP-3.2
checkpoint_name: "Green 확인 — 3차 (Phase 4 2차 FAIL 수정)"
task_id: TASK-006
phase: "3"
phase_name: "Phase 3 - Green (retry 2)"
saved_at: 2026-09-28T01:40:00Z
status: ARCHIVED

work_summary: "Phase 4 2차 FAIL의 잔존 결함(isIntegral 자체가 scale 언더플로로 예외) 수정 — 범위 확인을 isIntegral()보다 먼저 실행하도록 순서 변경. 전체 55/55 통과"

progress:
  completed:
    - "validateSection의 price/seatsPerRow 검증 순서를 signum/compareTo(범위) → isIntegral() 순으로 변경 (기존엔 isIntegral()이 먼저였음)"
    - "순서가 안전에 필수적인 이유를 설명하는 주석 추가"
    - "회귀 테스트 2건 추가: test_task006_sc07(price, 100E+2147483647), sc08(seatsPerRow, 100E+2147483647) — 2차 리뷰가 정확히 재현한 입력"
    - "./gradlew compileJava compileTestJava → BUILD SUCCESSFUL"
    - "./gradlew test --tests \"*PerformanceRegistrationApiTest*\" → 신규 8건 전부 통과"
    - "./gradlew cleanTest test (전체) → 55/55 통과"
    - "./gradlew spotlessCheck → error 0"
  in_progress: "없음 — 리팩토링 검토로 진행"
  blocked: []

next_steps:
  - priority: 1
    task: "리팩토링 필요성 검토 후 DEV_TASK-006.json(attempt 3) 저장, CP-3.4 retry2 저장"

decisions:
  - decision: "범위 확인(signum·compareTo)을 isIntegral()보다 먼저 실행한다"
    rationale: "2차 code-reviewer가 실측 확인: isIntegral()의 stripTrailingZeros()는 scale이 Integer.MIN_VALUE 아래로 내려가면(가수에 후행 0이 있는 극단적 지수 표기) ArithmeticException을 던진다. 그런 값은 정의상(unscaledValue가 0이 아닌 한 10^|scale|배 이상이므로) 반드시 MAX_INT_VALUE를 초과하고, compareTo는 이런 극단적 scale 차이에서도 안전하다는 것을 code-reviewer가 실측(1~2ms, 예외 없음)으로 확인했다. 따라서 범위 확인을 먼저 두면 위험한 값이 isIntegral()에 도달하지 않는다"
    alternatives_considered: ["isIntegral() 자체에 try-catch로 ArithmeticException을 잡아 InvalidSectionException으로 변환", "BigDecimal precision/scale에 대한 별도 상한 검사 도입"]
    impact: "코드 변경은 검증 순서 재배치뿐(로직 추가 최소화), 새 회귀 테스트 2건으로 정확한 재현 케이스 고정"
  - decision: "seatsPerRow 상한 초과 시 에러코드가 크기에 따라 SEAT_LIMIT_EXCEEDED/INVALID_SECTION으로 갈리는 지적(2차 리뷰 [design] 소견)은 이번에 고치지 않는다"
    rationale: "리뷰어 스스로 '비차단 사항'으로 분류했다 — 둘 다 400이라 계약 위반은 아니며, 도메인 규칙(5,000석 상한)과 구현상 방어(int 변환 상한)가 다른 층의 문제라 별도 판단이 필요하다. 지금 고치면 범위가 다시 넓어진다"
    alternatives_considered: ["seatsPerRow 합산 비교를 long 기반으로 통일해 코드를 SEAT_LIMIT_EXCEEDED 하나로 수렴"]
    impact: "Phase 5 회고의 개선 후보로 남김"

recovery_prerequisites:
  - CP-4.2

execution_context:
  test_command: "./gradlew cleanTest test"
  build_command: "./gradlew build"
  env_required: ["JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home"]
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"
    - "backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/06_dev/DEV_TASK-006.json"
---

## 무엇을 했나

Phase 4 2차 FAIL이 지적한 잔존 결함(isIntegral() 자체가 특정 극단적 지수 표기에서
ArithmeticException을 던지는 문제)을 수정했다 — 코드를 더 추가하는 대신, 이미 안전함이
검증된 범위 확인(signum·compareTo)을 isIntegral()보다 먼저 실행하도록 순서만 바꿨다.
위험한 값은 정의상 범위를 벗어나므로 이 재배치만으로 isIntegral()에 위험한 입력이
전혀 도달하지 않는다. 정확한 재현 케이스(100E+2147483647)를 고정하는 회귀 테스트
2건을 추가했다. 전체 55개 테스트가 통과했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `backend/src/main/java/.../PerformanceService.java` | 검증 순서 재배치 + 근거 주석 |
| `backend/src/test/java/.../PerformanceRegistrationApiTest.java` | 회귀 테스트 2건 추가 |

## 다음 단계

리팩토링 검토 후 DEV JSON·CP-3.4 저장.
