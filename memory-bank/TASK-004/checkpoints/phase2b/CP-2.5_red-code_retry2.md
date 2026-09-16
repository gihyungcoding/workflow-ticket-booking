---
checkpoint_id: CP-2.5
checkpoint_name: "Red 코드 작성 완료 (attempt 3)"
task_id: TASK-004
phase: "2b"
phase_name: "Phase 2b - Red (attempt 3)"
saved_at: 2026-09-15T16:45:00Z
status: ARCHIVED

work_summary: "SC-22(title 길이/POST)·SC-23(sections null 원소)·SC-24(title 길이/PUT) 3건을 PerformanceRegistrationApiTest.java에 추가하고 실행해 전부 Red를 확인했다. 기존 20개 시나리오는 계속 통과. registerJsonWithTitle 헬퍼를 신규 추가했다."

progress:
  completed:
    - "test_sc22/test_sc23/test_sc24 작성 — 기존 SC-16~21 관례(함수명에 시나리오ID, // Given/When/Then 주석) 그대로 따름"
    - "registerJsonWithTitle 헬퍼 추가 — 기존 registerJson과 동일하되 title을 파라미터로 받음"
    - "./gradlew test --tests PerformanceRegistrationApiTest --rerun-tasks 실행 — 23개 중 3개 실패(SC-22/23/24), 20개 통과"
    - "인벤토리 대조 — 시나리오 23 == 테스트 함수 23"
    - "SC-24가 'expected 400 but was 200'으로 실패함을 확인 — code-reviewer가 사전 경고한 @Transactional 사각지대(길이 검사 미구현 시 DB 제약조차 걸리지 않고 그냥 통과)를 정확히 재현. Service 계층 검증 없이 DB 제약에만 의존하면 이 테스트는 여전히 통과 못 한다는 것이 이제 코드로 강제됨"
  in_progress: "HITL#2 승인 요청"
  blocked: []

next_steps:
  - priority: 1
    task: "AskUserQuestion으로 HITL#2 승인 요청"
  - priority: 2
    task: "승인되면 Phase 3(Green) — validateRequired에 title/venue 길이 검사 추가(등록·수정 공유), toSectionSpec이 null 원소를 통과시키고 validateSection이 null을 잡도록 수정"

decisions:
  - decision: "SC-24는 DB 제약(스키마)이 아니라 서비스 계층 검증으로만 통과 가능하게 설계했다 — 결과적으로 이 테스트 자체가 Phase 3 구현 방식을 강제한다"
    rationale: "@Transactional 테스트 하네스가 PUT 경로의 DB 제약 위반을 가린다는 것을 code-reviewer가 이미 발견했다. Given/When/Then은 순수하게 'PUT 요청 → 400/INVALID_REQUEST'만 단언하지만, 그 결과로 DB 제약에만 의존하는 잘못된 구현은 이 테스트를 통과시킬 수 없다 — 의도한 안전장치"
    alternatives_considered: ["TestEntityManager.flush()나 비트랜잭션 테스트로 하네스 자체를 고침 — 기존 클래스 전체의 트랜잭션 전략을 바꾸는 것은 이번 롤백 범위를 넘어선다고 판단해 기각"]
    impact: "없음 — 오히려 Phase 3 구현을 더 정확히 검증하게 됨"

recovery_prerequisites:
  - CP-2.4_hitl1-approved_retry2

execution_context:
  test_command: "cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test"
  build_command: "cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew build"
  env_required: ["JAVA_HOME을 JDK21로 설정"]
  main_files:
    - "backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/TEST_TASK-004.json"
---

## 무엇을 했나

승인된 시나리오 SC-22/23/24를 `PerformanceRegistrationApiTest.java`에 1:1로 옮겼다.
기존 클래스 관례(함수명에 시나리오 ID, `// Given`/`// When`/`// Then` 주석, `postJson`/
`putJson`/`registerJson`/`updateJson` 헬퍼 재사용)를 그대로 따랐고, title을 파라미터로
받는 `registerJsonWithTitle` 헬퍼만 새로 추가했다. 실행 결과 3건 모두 Red(각기 다른
원인 — DB 제약 위반, NPE, 어서션 실패)이고 기존 20건은 계속 Green이다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/TEST_TASK-004.json` | Red 결과 기록(attempt 3) |
| `backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java` | Red 테스트 코드 |

## 재개 방법

1. `TEST_TASK-004.json` 의 `red_scenarios_attempt3`/`red_result_attempt3` 를 읽는다
2. HITL#2를 `AskUserQuestion` 으로 진행한다
3. 승인되면 `wf-develop` 스킬로 Phase 3 진입
