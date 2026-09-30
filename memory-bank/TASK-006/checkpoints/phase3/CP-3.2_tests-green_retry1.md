---
checkpoint_id: CP-3.2
checkpoint_name: "Green 확인 — 2차 (Phase 4 FAIL 수정)"
task_id: TASK-006
phase: "3"
phase_name: "Phase 3 - Green (retry 1)"
saved_at: 2026-09-28T01:15:00Z
status: SUPERSEDED

work_summary: "Phase 4 1차 FAIL의 code_review 지적 2건 수정 — int 상한 검증 추가, isIntegral() 구현 교체. 전체 53/53 통과"

progress:
  completed:
    - "MAX_INT_VALUE 상수 추가, validateSection에 price/seatsPerRow 각각 int 상한(Integer.MAX_VALUE) 초과 시 InvalidSectionException 추가"
    - "isIntegral()을 remainder(BigDecimal.ONE) 방식에서 stripTrailingZeros().scale()<=0 방식으로 교체"
    - "회귀 테스트 3건 추가: test_task006_sc04(price int 범위 초과), sc05(seatsPerRow int 범위 초과), sc06(price 극단적 지수 표기 1E+2000000000)"
    - "./gradlew compileJava compileTestJava → BUILD SUCCESSFUL"
    - "./gradlew test --tests \"*PerformanceRegistrationApiTest*\" → 신규 6건 전부 통과"
    - "./gradlew test (전체) → 53/53 통과"
    - "./gradlew spotlessCheck → error 0"
  in_progress: "없음 — 리팩토링 검토로 진행"
  blocked: []

next_steps:
  - priority: 1
    task: "리팩토링 필요성 검토 후 DEV_TASK-006.json(attempt 2) 저장, CP-3.4 retry1 저장"

decisions:
  - decision: "SC-04/05/06을 SCENARIO_TASK-006.md에 소급 추가하지 않고, DEV JSON의 scope_deviations에 근거와 함께 기록한다"
    rationale: "이 세 테스트는 승인된 시나리오 범위 밖의 경계값이지만, 이 태스크가 만든 새 코드 경로(BigDecimal 도입)의 결함을 고정하는 회귀 테스트다. 시나리오를 다시 쓰고 재승인받는 것(Phase 2a 재진입)은 이미 발견·확정된 결함 수정에 비해 과도한 절차라고 판단했다 — Phase 4(2차)가 이 편입 판단 자체를 재검토한다"
    alternatives_considered: ["Phase 2a로 롤백해 SC-04/05/06을 정식 시나리오로 추가 후 재승인"]
    impact: "Phase 4 2차 검증에서 code_review가 이 판단을 다시 확인해야 함"

recovery_prerequisites:
  - CP-4.2

execution_context:
  test_command: "./gradlew test"
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

Phase 4 1차 FAIL이 지적한 correctness 결함 2건을 수정했다 — price/seatsPerRow에
int 상한(Integer.MAX_VALUE) 검증을 추가해 범위 초과 값이 500이 아니라 400/INVALID_SECTION을
반환하게 했고, isIntegral()을 PLAN이 원래 명시했던 `stripTrailingZeros().scale()<=0`
방식으로 바꿔 극단적 지수 표기 입력에서도 예외 없이 안전하게 판정하게 했다. 두
결함을 각각 재현하는 회귀 테스트 3건을 추가했다. 전체 53개 테스트가 통과했고
포맷 검사도 통과했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `backend/src/main/java/.../PerformanceService.java` | int 상한 검증 + isIntegral 재구현 |
| `backend/src/test/java/.../PerformanceRegistrationApiTest.java` | 회귀 테스트 3건 추가 |

## 다음 단계

리팩토링 검토 후 DEV JSON·CP-3.4 저장.
