---
checkpoint_id: CP-2.5
checkpoint_name: "Red 테스트 작성 완료(경고 포함)"
task_id: TASK-008
phase: "2b"
phase_name: "Phase 2b - Red"
saved_at: 2026-09-17T02:30:00Z
status: ACTIVE

work_summary: "PerformanceSectionSummaryApiTest.java 신규 작성, 4개 시나리오 1:1 테스트 함수. SC-01/SC-04는 진짜 Red(sections null), SC-02/SC-03은 이 태스크가 바꾸지 않는 기존 동작을 검증하는 회귀 가드라 즉시 통과"

progress:
  completed:
    - "backend/src/test/java/com/example/ticket_booking/api/PerformanceSectionSummaryApiTest.java 작성 — SC-01~04 각각 test_sc01~04 함수로 1:1 매핑"
    - "MockMvc/jsonPath 블랙박스 테스트라 컴파일 스켈레톤(신규 DTO/Repository 메서드)이 필요 없음을 확인 — 테스트 코드가 Java 타입으로 새 프로덕션 클래스를 참조하지 않는다"
    - "./gradlew test --tests '*PerformanceSectionSummaryApiTest*' 실행 — 4건 중 2건 실패(SC-01, SC-04), 2건 통과(SC-02, SC-03)"
    - "실패 원인 확인 — 둘 다 'Expecting actual not to be null'(sections 키가 JSON에 없음) — 진짜 미구현 Red, 테스트 코드 결함 아님"
    - "전체 백엔드 스위트(47건) 실행 — 실패 2건(새로 추가한 것과 동일), 기존 45건 전부 통과 유지"
    - "PerformanceApiTest.test_sc09와 SC-02가 동일한 경로를 검증하는 중복임을 확인·기록"
    - "TEST_TASK-008.json 작성 — SC-02/SC-03의 already_passing 상태와 사유를 숨기지 않고 명시"
  in_progress: "HITL#2 승인 요청 준비"
  blocked: []

next_steps:
  - priority: 1
    task: "HITL#2 승인 요청 — SC-02/SC-03이 Red가 아니라 already_passing 상태임을 명확히 알리고 진행 여부를 확인받는다"

decisions:
  - decision: "SC-02/SC-03을 억지로 실패시키지 않고 already_passing 상태 그대로 보고한다"
    rationale: "둘 다 '무엇을 하지 않아도 참'인 부정형 회귀 시나리오(404 유지, sections 미포함 유지)라 구조적으로 Red가 불가능하다 — 이 프로젝트 TASK-001 선례(PerformanceApiTest.java 파일 상단 주석: 'SC-12~14는 기존 구현이 이미 올바르게 처리하던 분기라 추가 즉시 통과한다')와 같은 패턴. 억지로 실패하게 만들려면 기존 동작을 일부러 깨뜨려야 하는데 그건 더 나쁘다"
    alternatives_considered: ["SC-02/SC-03을 시나리오에서 제외하고 별도 회귀 확인으로만 남김", "assertion을 더 엄격하게 바꿔 강제로 실패시킴(예: 존재하지 않는 필드에 대한 오탐)"]
    impact: "TEST_TASK-008.json에 두 시나리오를 status=already_passing으로 명시, HITL#2에서 사용자에게 투명하게 공개"
  - decision: "SC-02(404)는 PerformanceApiTest.test_sc09와 동일한 경로를 검증하는 중복이지만 별도 파일에 유지한다"
    rationale: "이 프로젝트 관례가 태스크별 전용 테스트 파일(파일 상단에 'TASK-NNN 시나리오 SC-...' 주석)이라, TASK-008의 SCENARIO 문서가 승인한 시나리오를 태스크 경계를 넘어 다른 파일의 기존 테스트로 대체하면 추적성이 흐려진다"
    alternatives_considered: ["SC-02를 새로 쓰지 않고 test_sc09를 '재사용'으로 표기만 함"]
    impact: "약간의 테스트 중복(4줄) — 무시할 수 있는 비용으로 판단"

recovery_prerequisites:
  - CP-2.4

execution_context:
  test_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew -p backend test --tests \"*PerformanceSectionSummaryApiTest*\""
  build_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew -p backend test"
  env_required: []
  main_files:
    - "backend/src/test/java/com/example/ticket_booking/api/PerformanceSectionSummaryApiTest.java"
    - "workflow_design/05_scenario/TEST_TASK-008.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/TEST_TASK-008.json"
    - path: "backend/src/test/java/com/example/ticket_booking/api/PerformanceSectionSummaryApiTest.java"
---

## 무엇을 했나

SCENARIO_TASK-008.md의 시나리오 4건을 새 테스트 파일 하나에 1:1로 옮겼다. 실행 결과
SC-01/SC-04는 sections 필드가 아직 없어 진짜로 실패했고(Red), SC-02/SC-03은 이 태스크가
바꾸지 않는 기존 동작(404, 목록 응답 형태)을 검증하는 회귀 가드라 즉시 통과했다 — 이
프로젝트의 기존 선례(TASK-001)와 같은 패턴이라 그대로 투명하게 보고하기로 했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `backend/src/test/java/com/example/ticket_booking/api/PerformanceSectionSummaryApiTest.java` | Red 테스트 4건 |
| `workflow_design/05_scenario/TEST_TASK-008.json` | 실행 결과·인벤토리 기록 |

## 재개 방법

1. TEST_TASK-008.json과 이 CP를 사용자에게 요약해 HITL#2를 요청한다
2. 승인되면 human_review.approved=true로 갱신, CP-2.6 저장 후 커밋
