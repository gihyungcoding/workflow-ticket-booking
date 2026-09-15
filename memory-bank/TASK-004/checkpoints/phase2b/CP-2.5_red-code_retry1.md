---
checkpoint_id: CP-2.5
checkpoint_name: "Red 코드 작성 완료 (재시도)"
task_id: TASK-004
phase: "2b"
phase_name: "Phase 2b - Red"
saved_at: 2026-09-15T03:00:00Z
status: SUPERSEDED

work_summary: "SC-16~21 6건을 기존 PerformanceRegistrationApiTest.java에 추가. 신규 예외 2종(InvalidRequestException, InvalidSectionException)과 핸들러 추가(구조적 매핑, 스텁 아님). 전체 실행 40개 중 신규 6개만 실패, 기존 34개 통과"

progress:
  completed:
    - "InvalidRequestException/InvalidSectionException 신규 작성 (PerformanceNotFoundException과 동일 패턴)"
    - "PerformanceExceptionHandler에 두 예외 → 400 매핑 추가"
    - "PerformanceRegistrationApiTest.java에 test_sc16~test_sc21 6개 추가 (PerformanceService는 attempt 1 상태 그대로 두고 손대지 않음)"
    - "컴파일 확인, 개별 실행(20개 중 6개 실패), 전체 실행(40개 중 6개 실패, 기존 34개 통과) 확인"
    - "실패 원인 6건 개별 확인 — NPE(SC-16), DB CHECK 위반(SC-17/18/20), StringIndexOutOfBoundsException(SC-19), AssertionFailedError(SC-21) — 전부 '검증이 아직 없어서'라는 공통 원인"
    - "TEST_TASK-004.json에 attempt 2 결과 추가(기존 attempt 1 기록은 보존)"
  in_progress: "HITL#2 재승인 대기"
  blocked: []

next_steps:
  - priority: 1
    task: "AskUserQuestion으로 HITL#2 재승인 요청"
  - priority: 2
    task: "승인 시 human_review.approved=true, CP-2.6 retry1 저장, 커밋"
  - priority: 3
    task: "wf-develop으로 Phase 3 재진입 — F10/F11 검증 구현, long 산술, Seat FK 애노테이션"

decisions:
  - decision: "PerformanceService 는 이 라운드에서 전혀 건드리지 않았다 (attempt 1 구현 그대로)"
    rationale: "Red 단계는 테스트만 추가하고 구현은 Green(Phase 3)에서 한다는 원칙을 그대로 지켰다. 이미 구현된 메서드에 새 분기를 추가해야 하는 상황이라 컴파일 스켈레톤(UnsupportedOperationException)이 필요 없었고, 대신 '현재 동작이 기대와 다름'을 실패로 확인했다"
    alternatives_considered: []
    impact: "실패 원인이 6건 모두 제각각(NPE/DB제약/StringIndexOutOfBounds/AssertionFailedError)이지만, 전부 '검증이 아직 없다'는 동일한 근본 원인이라 유효한 Red로 판정"

recovery_prerequisites:
  - CP-2.4_retry1

execution_context:
  test_command: "cd backend && ./gradlew test"
  build_command: "cd backend && ./gradlew build"
  env_required: ["JAVA_HOME을 JDK21로 설정"]
  main_files:
    - "backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java"
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/TEST_TASK-004.json"
---

## 무엇을 했나

승인된 오류 시나리오 6건(SC-16~21)을 기존 Red 테스트 파일에 추가했다. attempt 1의
14개 테스트와 PerformanceService 구현은 전혀 건드리지 않았다. 전체 40개 중 신규
6개만 실패하고 기존 34개(TASK-001/002 포함)는 그대로 통과함을 확인했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java` | Red 테스트 20개(신규 6 + 기존 14) |
| `workflow_design/05_scenario/TEST_TASK-004.json` | attempt 2 실행 결과 (attempt 1 기록 보존) |

## 재개 방법

1. HITL#2 재승인을 받는다
2. 승인되면 `human_review.approved=true`, CP-2.6 retry1 저장, 커밋
3. `wf-develop` 스킬로 Phase 3 재진입
