---
checkpoint_id: CP-2.5
checkpoint_name: "Red 코드 재작성 (retry1)"
task_id: TASK-001
phase: "2b"
phase_name: "Phase 2b - Red (RETRY)"
saved_at: 2026-09-04T08:30:00Z
status: ARCHIVED

work_summary: "시나리오 12~18(7건)을 기존 테스트 파일에 추가하고, F9(PerformanceStatusRules) 스켈레톤 + 단위 테스트 1건을 신규 작성했다. 이번 재시도는 기존 구현이 이미 Green이었던 코드베이스 위에서 진행돼, 전체 20개 중 3개(SC-15, SC-16, PerformanceStatusRulesTest)만 Red이고 나머지는 이미 올바른 기존 구현 덕에 Green이다 — 이는 정상이며 wf-red Step 3의 '이미 구현됐는지 확인' 절차로 검증했다."

progress:
  completed:
    - "PerformanceApiTest.java에 test_sc12(UPCOMING 필터), test_sc13(SOLD_OUT 필터), test_sc14(CANCELLED 필터) 추가"
    - "PerformanceConstraintTest.java에 test_sc15(openAt>closeAt 거부), test_sc16(title 201자 거부), test_sc17(title 200자 저장, 재조회 확인), test_sc18(openAt==closeAt 저장, 재조회 확인) 추가. 기존 SC-10/11 단언을 DataIntegrityViolationException으로 강화"
    - "PerformanceStatusRules.java 스켈레톤 생성 (of/matches, UnsupportedOperationException)"
    - "PerformanceStatusRulesTest.java 작성 — 5개 상태 대표 경계값의 상호배타성 검증 (Spring 컨텍스트 없음)"
    - "./gradlew clean test 실행 — 20개 중 17 통과, 3 실패(예상과 일치)"
    - "실패 3건 확인: SC-15/16은 'Expecting code to raise a throwable'(F8/길이 제약 미구현), PerformanceStatusRulesTest는 UnsupportedOperationException"
    - "통과 5건(SC-12/13/14/17/18)이 '이미 구현됨' 때문임을 코드 검토로 확인 — 단언 약화가 아님"
    - "인벤토리 대조: 시나리오 18 == test_sc 함수 18, PerformanceStatusRulesTest는 승인된 예외로 별도 관리"
    - "TEST_TASK-001.json 갱신 (attempt=2, retry_notes로 mixed 결과 설명)"

next_steps:
  - priority: 1
    task: "AskUserQuestion으로 HITL#2 재승인 요청"

decisions:
  - decision: "SC-12/13/14/17/18이 추가 즉시 Green인 것을 결함으로 보지 않고 그대로 인정한다"
    rationale: "wf-red Step 3 표: '통과한 테스트가 있으면 단언을 강화하거나, 그 기능이 이미 있는지 확인한다' — 코드 검토 결과 이 5건은 attempt 1의 기존 구현이 처음부터 해당 로직을 올바르게 처리하고 있었다(UPCOMING/SOLD_OUT/CANCELLED 필터는 애초에 버그가 없었고, SC-17/18은 아직 제약이 없어 당연히 통과). 단언을 억지로 약화한 것이 아니라 실제로 이미 맞는 동작이다"
    alternatives_considered: ["이 5건도 인위적으로 실패하게 만들기 (예: 잘못된 단언을 넣었다가 되돌리기 — 불필요한 작업)"]
    impact: "TEST_TASK-001.json.red_scenarios에 각 항목의 green/red 사유를 명시적으로 기록해 다음 사람이 헷갈리지 않게 함"

recovery_prerequisites:
  - CP-2.4 (retry1)

execution_context:
  test_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew clean test"
  build_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew build"
  env_required: ["JAVA_HOME을 JDK 21 이상으로 설정"]
  main_files:
    - "workflow_design/05_scenario/TEST_TASK-001.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/TEST_TASK-001.json"
---

## 무엇을 했나

시나리오 7건(SC-12~18)과 F9 단위 테스트를 기존 코드베이스에 추가했다. 이미
Green이었던 코드 위에서 작업하다 보니 "전부 Red"가 아니라 "새로 발견된 버그
부분만 Red"가 정상인 특수한 재시도였다 — 이를 wf-red의 기존 절차(이미 구현된
기능인지 확인)로 검증하고 명시적으로 기록했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `backend/src/test/java/.../api/PerformanceApiTest.java` | SC-12~14 추가 |
| `backend/src/test/java/.../repository/PerformanceConstraintTest.java` | SC-15~18 추가, SC-10/11 강화 |
| `backend/src/main/java/.../service/PerformanceStatusRules.java` | F9 스켈레톤 |
| `backend/src/test/java/.../service/PerformanceStatusRulesTest.java` | F9 단위 테스트 |
| `workflow_design/05_scenario/TEST_TASK-001.json` | Red 결과 기록 (attempt 2) |

## 재개 방법

1. `TEST_TASK-001.json` 을 읽는다
2. `AskUserQuestion` 으로 HITL#2 재승인을 받는다
