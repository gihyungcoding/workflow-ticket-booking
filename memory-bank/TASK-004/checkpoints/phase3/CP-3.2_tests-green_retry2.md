---
checkpoint_id: CP-3.2
checkpoint_name: "테스트 Green 전환 완료 (attempt 3)"
task_id: TASK-004
phase: "3"
phase_name: "Phase 3 - Green (attempt 3)"
saved_at: 2026-09-15T17:00:00Z
status: ARCHIVED

work_summary: "SC-22/23/24를 통과시키는 최소 구현 완료. PerformanceService.validateRequired에 title/venue 길이 검사(등록·수정 공유), validateSection에 null 원소 검사를 추가하고 Controller의 toSectionSpec이 null을 그대로 통과시키도록 수정했다. 전체 43/43 통과, 린트 error 0."

progress:
  completed:
    - "PerformanceService: MAX_TITLE_LENGTH/MAX_VENUE_LENGTH=200 상수 추가, validateRequired에 길이 검사 추가(F10)"
    - "PerformanceService: validateSection 최상단에 section==null 검사 추가(F11)"
    - "PerformanceController: toSectionSpec(null) → null 반환 (기존엔 request.grade()에서 NPE)"
    - "PerformanceRegistrationApiTest 클래스만 재실행 — 23/23 통과(SC-22/23/24 포함)"
    - "전체 스위트 재실행 — 43/43 통과(기존 40 + 신규 3), 회귀 없음"
    - "spotlessCheck 최초 FAIL(긴 throw 줄, 주석 줄바꿈) → spotlessApply 1회 적용 후 재검증 통과, error 0"
  in_progress: "리팩토링 판단(불필요로 결론) → DEV JSON 저장"
  blocked: []

next_steps:
  - priority: 1
    task: "DEV_TASK-004.json에 attempt 3 기록 저장 — 완료"
  - priority: 2
    task: "CP-3.4_context-dev_retry2 저장, activeContext.md 갱신, 커밋"
  - priority: 3
    task: "wf-verify 스킬로 Phase 4 재검증(attempt 3) 진입"

decisions:
  - decision: "리팩토링하지 않는다"
    rationale: "추가한 검사가 각 2~3줄로 짧고 기존 validateRequired/validateSection/toSectionSpec 구조에 자연스럽게 들어맞는다 — 중복이나 모호한 이름이 새로 생기지 않았다"
    alternatives_considered: []
    impact: "없음"

recovery_prerequisites:
  - CP-2.6_hitl2-approved_retry2

execution_context:
  test_command: "cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test"
  build_command: "cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew build"
  env_required: ["JAVA_HOME을 JDK21로 설정"]
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"
    - "backend/src/main/java/com/example/ticket_booking/api/PerformanceController.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/06_dev/DEV_TASK-004.json"
---

## 무엇을 했나

SC-22(title 길이/POST)·SC-23(sections null 원소)·SC-24(title 길이/PUT)를 통과시키는
최소 구현을 했다. `validateRequired`(등록·수정이 공유하는 단일 검증 지점)에 title/
venue 200자 초과 검사를 추가해 SC-22와 SC-24를 한 번에 해결했고, `validateSection`
최상단에 null 검사를 추가하되 Controller의 `toSectionSpec`이 null 원소를 그대로
통과시키도록 먼저 고쳐 Service까지 도달하게 했다(Controller에서 NPE가 먼저 나던
문제 해소). 전체 43개 테스트가 통과하고 린트도 error 0이다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/06_dev/DEV_TASK-004.json` | 구현 기록(attempt 3), 테스트/린트 결과 |
| `backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java` | F10/F11 검증 보강 |
| `backend/src/main/java/com/example/ticket_booking/api/PerformanceController.java` | toSectionSpec null 처리 |

## 재개 방법

1. `DEV_TASK-004.json` 을 읽는다
2. `wf-verify` 스킬로 Phase 4 재검증(attempt 3) 진입
