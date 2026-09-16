---
checkpoint_id: CP-2.5
checkpoint_name: "Red 코드 작성 완료"
task_id: TASK-004
phase: "2b"
phase_name: "Phase 2b - Red"
saved_at: 2026-09-14T00:40:00Z
status: SUPERSEDED

work_summary: "시나리오 14건(SC-01~13,15)을 테스트 14개로 1:1 옮김. 컴파일용 스켈레톤(Seat 엔티티, Performance 상태변경 메서드, 예외 5종, DTO 3종, Service 메서드 3개)을 추가하고 전체 실행해 14/14 실패(UnsupportedOperationException), 기존 20개 테스트는 그대로 통과 확인"

progress:
  completed:
    - "Phase 2b 진입 전 SC-14(regression) 구조적 결함 발견 — 이 태스크가 안 건드리는 기존 GET 404 경로를 검증해 지금 이미 통과함. Phase 2a로 잠깐 되돌아가 제거, 재승인 받음(CP-2.4 amendment)"
    - "컴파일 스켈레톤 10개 파일 작성/수정 — memory:feedback-java-tdd-red-skeleton 컨벤션 그대로 적용"
    - "PerformanceRegistrationApiTest.java 작성 — 시나리오 14건을 1:1로 옮김 (test_sc01~13, test_sc15)"
    - "./gradlew compileJava compileTestJava 통과"
    - "./gradlew test 전체 실행 — 34 tests, 14 failed(신규), 20 passed(기존 전체)"
    - "순도 검사: 실패 14건 전부 UnsupportedOperationException 기인 (AssertionError 0건 — 로직 누출 없음)"
    - "인벤토리 대조: 시나리오 14 == 테스트 함수 14, ID 완전 일치"
    - "TEST_TASK-004.json 저장"
  in_progress: "HITL#2 승인 대기"
  blocked: []

next_steps:
  - priority: 1
    task: "AskUserQuestion으로 HITL#2 승인 요청"
  - priority: 2
    task: "승인 시 TEST_TASK-004.json human_review.approved=true, CP-2.6 저장, 테스트 코드 커밋"

decisions:
  - decision: "Controller의 DTO 변환 로직과 ExceptionHandler의 신규 매핑 5건은 스텁하지 않고 실제 코드로 작성했다"
    rationale: "조건 분기·계산이 없는 선언적 위임/매핑이라 기존 코드베이스의 toResponse()/handleNotFound() 패턴과 동일한 성격의 '구조'다. 실제 비즈니스 로직(좌석 생성, 검증)은 전부 PerformanceService에 있고 그것만 스텁했다"
    alternatives_considered: ["Controller/Handler도 전부 스텁"]
    impact: "Green 단계 작업량이 Service 로직에만 집중된다"
  - decision: "MockMvc 경계 테스트가 500 응답이 아니라 jakarta.servlet.ServletException(cause: UnsupportedOperationException)을 mockMvc.perform() 호출 자체에서 던지는 것을 확인했다 — 여전히 유효한 Red로 판정"
    rationale: "memory:feedback-java-tdd-red-skeleton §5의 '원인이 UnsupportedOperationException이면 경계 종류와 무관하게 유효한 Red' 기준을 그대로 적용. 실행 결과(build/test-results)로 14건 전부 같은 원인임을 실제로 확인했다"
    alternatives_considered: []
    impact: "테스트 코드에서 별도 예외 처리 없이 andReturn()만 써도 되고, 컨트롤러/핸들러에 예외를 되잡는 특수 로직을 추가할 필요가 없다"

recovery_prerequisites:
  - CP-2.4

execution_context:
  test_command: "cd backend && ./gradlew test"
  build_command: "cd backend && ./gradlew build"
  env_required: ["JAVA_HOME을 JDK21로 설정 (예: /Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home) — 시스템 기본 java가 JDK8이라 gradlew가 JDK17+ 요구를 만족 못 함"]
  main_files:
    - "backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java"
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/TEST_TASK-004.json"
---

## 무엇을 했나

승인된 시나리오 14건을 `PerformanceRegistrationApiTest.java`의 테스트 14개로 옮겼다.
Java는 컴파일 언어라 테스트가 컴파일되려면 프로덕션 스켈레톤이 먼저 있어야 해서,
`Seat` 엔티티·`Performance`의 상태변경 메서드·예외 5종·요청 DTO 3종·`PerformanceService`의
메서드 3개를 추가했다. 비즈니스 로직이 들어가는 메서드 5개(`registerPerformance`,
`updatePerformance`, `cancelPerformance`, `Performance.updateSchedule`, `Performance.cancel`)는
전부 `UnsupportedOperationException` 한 줄만 담았다. 전체 테스트를 실행해 신규 14건이
전부 그 예외로 실패하고, 기존 20건은 그대로 통과함을 확인했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java` | Red 테스트 14건 |
| `workflow_design/05_scenario/TEST_TASK-004.json` | 실행 결과·인벤토리·스켈레톤 기록 |

## 재개 방법

1. `TEST_TASK-004.json` 을 읽는다
2. HITL#2 승인을 받는다 (AskUserQuestion)
3. 승인되면 `human_review.approved=true`, CP-2.6 저장, 테스트 코드를 커밋한다
4. `wf-develop` 스킬로 Phase 3(Green) 진입
