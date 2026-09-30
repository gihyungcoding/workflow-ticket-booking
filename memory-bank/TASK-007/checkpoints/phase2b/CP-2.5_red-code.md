---
checkpoint_id: CP-2.5
checkpoint_name: "Red 테스트 코드 작성 완료"
task_id: TASK-007
phase: "2b"
phase_name: "Phase 2b - Red"
saved_at: 2026-09-30T02:00:00Z
status: ACTIVE

work_summary: "TASK-007 시나리오 11건을 PerformanceRegistrationApiTest.java에 테스트 함수 11개로 옮겼다 — SC-07만 Red, 나머지 10건은 already_passing"

progress:
  completed:
    - "HITL#2 전 V9(기존 테스트와 6건 중복) 처리 방식을 AskUserQuestion으로 확인 — '새 테스트 메서드 작성(그칙 원칙대로)' 채택"
    - "테스트 함수 11개 작성(test_t7_sc01~11) — 기존 헬퍼(registerJson/registerJsonWithTitle/updateJson/performance) 재사용으로 중복 최소화"
    - "./gradlew test --tests '*PerformanceRegistrationApiTest*' 실행 — 34개 중 1개 실패(SC-07), 33개 통과(기존 23 + 신규 10 already_passing)"
    - "SC-07 실패 원인 확인: MismatchedInputException — HTTP status는 이미 400이지만 응답 본문이 완전히 비어 있어 code/message를 읽을 수 없음. AC4가 지적한 결함과 정확히 일치"
    - "인벤토리 검증 — 시나리오 11 == 테스트 함수 11, ID 1:1 대응 확인"
    - "TEST_TASK-007.json 저장"
  in_progress: "HITL#2 승인 요청 준비"
  blocked: []

next_steps:
  - priority: 1
    task: "AskUserQuestion으로 HITL#2 승인 요청"
  - priority: 2
    task: "승인되면 TEST_TASK-007.json human_review.approved=true, CP-2.6 저장, 테스트 코드와 함께 커밋"

decisions:
  - decision: "SC-01~06/08~11(10건)은 already_passing으로 표시한다 — 이 태스크는 외부 동작을 바꾸지 않는 순수 리팩토링이라 구조적으로 Red를 만들 수 없다(Phase 2a에서 이미 예측·확정한 내용)"
    rationale: "wf-red 스킬의 '부정형 회귀 시나리오는 already_passing일 수 있다' 예외 조항에 해당한다. 억지로 실패시키려 assertion을 조작하거나 기존 동작을 깨뜨리지 않는다"
    alternatives_considered: []
    impact: "EXIT GATE의 '테스트 실행 결과가 실제 실패'는 already_passing으로 표시되지 않은 SC-07에만 적용된다 — 실제로 SC-07만 실패했으므로 게이트 충족"
  - decision: "SC-03/04/05/08/09/10(기존 테스트와 완전 동일한 입력)도 별도 테스트 함수로 작성한다 — 기존 테스트를 직접 참조하는 방식은 쓰지 않는다"
    rationale: "HITL#2 전 사용자에게 직접 확인 — wf-red MUST #1(시나리오 하나당 테스트 함수 하나)을 원칙대로 지키기로 결정. 코드 중복은 기존 헬퍼 메서드 재사용으로 최소화"
    alternatives_considered: ["TEST_TASK-007.json의 test_function이 기존 메서드명(test_sc22 등)을 직접 가리키게 해 코드 중복을 0으로"]
    impact: "테스트 파일이 34개 메서드로 늘어났지만 각 코드는 3~5줄 수준으로 짧다(헬퍼 재사용)"

recovery_prerequisites:
  - CP-2.4

execution_context:
  test_command: "cd backend && ./gradlew test --tests '*PerformanceRegistrationApiTest*'"
  build_command: "cd backend && ./gradlew build"
  env_required: ["JAVA_HOME=JDK 21 경로 (기본 java는 8)"]
  main_files:
    - "backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java"
    - "workflow_design/05_scenario/TEST_TASK-007.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/TEST_TASK-007.json"
---

## 무엇을 했나

시나리오 11건을 테스트 함수 11개로 옮기고 실행했다. 34개(기존 23 + 신규 11) 중
1개만 실패했다 — `test_t7_sc07`(파싱 불가능한 JSON). 실패 원인을 직접 확인한
결과 HTTP status는 이미 400으로 정상 반환되지만(Spring 기본
`HttpMessageNotReadableException` 처리), 응답 본문이 완전히 비어 있어
`MismatchedInputException: No content to map due to end-of-input`이 발생한다
— AC4가 지적한 "빈 body" 결함과 정확히 일치하는 증거다.

나머지 10건(SC-01~06/08~11)은 모두 already_passing이다. Phase 2a에서 이미
예측한 대로, 이 태스크는 외부에서 관찰되는 동작을 바꾸지 않는 순수
리팩토링이라 구조적으로 Red를 만들 수 없는 시나리오들이다 — 억지로 실패시키지
않고 wf-red의 "부정형 회귀 시나리오" 예외 조항에 따라 표시했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java` | 테스트 코드 (기존 23개 + 신규 11개 = 34개) |
| `workflow_design/05_scenario/TEST_TASK-007.json` | Red 테스트 인벤토리, 실행 결과, already_passing 근거 |

## 재개 방법

1. `TEST_TASK-007.json`을 읽는다
2. AskUserQuestion으로 HITL#2 승인을 받는다
3. 승인되면 `human_review.approved=true`, CP-2.6 저장, 테스트 코드와 함께 커밋(빨간 상태로 커밋하는 것이 정상)
