---
checkpoint_id: CP-3.2
checkpoint_name: "테스트 통과 확인 (retry1)"
task_id: TASK-001
phase: "3"
phase_name: "Phase 3 - Green (RETRY)"
saved_at: 2026-09-04T09:00:17Z
status: ACTIVE

work_summary: "F7(길이)/F8(불변조건)/F9(순수 로직) 구현 완료. 20개 테스트 전부 통과(1회 시도), 아키텍처 제약·spotlessCheck 통과. CLOSED 버그는 코드를 고치지 않고 F8 CHECK 제약 추가로 근본 해결."

progress:
  completed:
    - "Performance.java: title/venue length=200 추가, @Table check에 open_at<=close_at 추가"
    - "V1__create_performance.sql: CHECK (open_at <= close_at) 추가"
    - "PerformanceStatusRules.of()/matches() 실제 구현 — of()는 기존 statusOf() 로직 이동, matches()는 독립적 우선순위 기반 표현으로 새로 작성 (of() 호출 안 함 — 트리비얼 위임 방지)"
    - "PerformanceService: private statusOf() 제거, toResponse()가 PerformanceStatusRules.of() 호출. statusSpecification() CLOSED 분기는 코드 변경 없음 — F8 의존 동치를 주석으로 남김"
    - "./gradlew clean test — 20/20 통과 (1회 시도, 추가 수정 불필요)"
    - "python scripts/check_architecture.py — ARCH-001/002 통과"
    - "./gradlew spotlessCheck — 최초 실패(신규/변경 파일 미포맷) → spotlessApply 적용 → 재확인 통과"
  in_progress: null
  blocked: false

next_steps:
  - priority: 1
    task: "DEV_TASK-001.json(attempt 2) 저장 및 커밋"
  - priority: 2
    task: "wf-verify 스킬로 Phase 4(검증) 재진입"

decisions:
  - decision: "PerformanceStatusRules.matches()를 of()에 위임하지 않고 독립적인 불리언 표현으로 작성"
    rationale: "matches()가 단순히 'status == of(...)' 라면 항상 참인 동어반복이라 단위 테스트가 아무것도 교차검증하지 못한다. 독립적으로 작성해야 두 구현이 실제로 같은 결론에 도달하는지 의미 있게 검증된다"
    alternatives_considered: ["matches(status, ...) { return status == of(...); } (동어반복, 기각)"]
    impact: "PerformanceStatusRulesTest가 실제로 두 개의 독립 구현을 교차검증하게 됨"
  - decision: "PerformanceStatusRulesTest에 openAt>closeAt(malformed) 픽스처를 추가해 F8과 무관하게 순수 로직이 항상 일치함을 증명"
    rationale: "순수 함수는 DB CHECK 제약의 영향을 받지 않으므로 malformed 입력으로도 호출 가능하다 — Phase 4에서 발견된 버그의 정확한 모양(openAt>closeAt)을 이 계층에서 직접 재현해 matches()의 CLOSED 분기가 openAt<=now 를 포함하는지 증명한다"
    alternatives_considered: ["F8이 항상 성립한다고 가정하고 well-formed 픽스처만 사용 (더 약한 보장)"]
    impact: "PerformanceStatusRulesTest가 F8 여부와 무관하게 동작하는 더 강한 회귀 보호를 제공"

recovery_prerequisites:
  - CP-2.6 (retry1)

execution_context:
  test_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew clean test"
  build_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew build"
  env_required: ["JAVA_HOME을 JDK 21 이상으로 설정"]
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceStatusRules.java"
    - "backend/src/main/java/com/example/ticket_booking/domain/Performance.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/TEST_TASK-001.json"
---

## 무엇을 했나

Phase 4에서 발견된 두 결함(CLOSED 로직 불일치, 컬럼 길이 어긋남)을 근본
해결했다. CLOSED 버그는 코드를 고치는 대신 도메인 불변조건(F8)을 추가해
해결했고, 이 판단이 옳았음을 PerformanceStatusRulesTest의 malformed 픽스처로
직접 증명했다. 20개 테스트가 한 번의 시도로 모두 통과했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `backend/src/main/java/.../domain/Performance.java` | F7(길이)/F8(불변조건) 구현 |
| `backend/src/main/java/.../service/PerformanceStatusRules.java` | F9 구현 |
| `backend/src/main/java/.../service/PerformanceService.java` | PerformanceStatusRules 위임 |

## 재개 방법

1. `DEV_TASK-001.json` 을 저장하고 커밋한다
2. `wf-verify` 스킬로 Phase 4에 재진입한다
