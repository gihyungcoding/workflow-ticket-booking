---
checkpoint_id: CP-2.5
checkpoint_name: "Red 코드 작성 및 실패 확인"
task_id: TASK-001
phase: "2b"
phase_name: "Phase 2b - Red"
saved_at: 2026-09-03T12:54:52Z
status: ACTIVE

work_summary: "승인된 시나리오 11건을 테스트 함수 11개로 옮겼다(1:1). Java/Spring(컴파일 언어)이라 wf-red 스킬의 Python 기준 Red 정의가 그대로 적용되지 않아, 사용자와 합의해 '컴파일되는 최소 스켈레톤 + UnsupportedOperationException' 방식을 썼다. ./gradlew test 실행 결과 신규 11개 전부 실패, 기존 테스트 1개는 계속 통과."

progress:
  completed:
    - "사용자와 Java Red 전략 합의: 스켈레톤 프로덕션 클래스(필드/시그니처만) + 메서드 본문은 UnsupportedOperationException"
    - "SC-10/SC-11(제약 애노테이션 자체가 구현)은 예외적으로 '제약 없는 엔티티'로 두어 assertThrows 실패를 유도하기로 합의"
    - "HTTP 경계 테스트(SC-01~09)는 500 응답에 따른 상태 코드 불일치로 실패하는 것을 유효한 Red로 인정하기로 합의 — UnsupportedOperationException이 5xx로 변환되기 때문"
    - "스켈레톤 프로덕션 파일 9개 작성: Performance(엔티티, 제약 없음), PerformanceStatus(enum), PerformanceRepository(빈 인터페이스), PerformanceService(throws UOE), PerformanceController(서비스에 위임), PerformanceResponse/PerformanceListResponse(record), ClockConfig(@Bean Clock — 기존 테스트 유지에 필요해 추가)"
    - "V1 마이그레이션, PerformanceExceptionHandler, ErrorResponse, PerformanceNotFoundException은 Red 컴파일/실행에 불필요해 Phase 3로 미룸"
    - "테스트 지원 클래스 작성: MutableClock, ClockTestConfig (테스트 코드, 프로덕션 아님)"
    - "PerformanceApiTest.java(SC-01~09, @SpringBootTest+MockMvc), PerformanceConstraintTest.java(SC-10~11, @DataJpaTest) 작성"
    - "빌드 중 Spring Boot 4.1.1/Jackson 3의 패키지 변경(DataJpaTest, AutoConfigureMockMvc, ObjectMapper 위치)을 발견하고 수정 — memory에 기록"
    - "./gradlew test 실행 — 신규 11개 전부 실패(UnsupportedOperationException 9건, AssertionError 2건), 기존 TicketBookingApplicationTests 1건 통과"
    - "인벤토리 대조: 시나리오 11 == 테스트 함수 11, ID 완전 일치"
  in_progress: "HITL#2 승인 요청"
  blocked: []

next_steps:
  - priority: 1
    task: "AskUserQuestion으로 HITL#2 승인 요청"

decisions:
  - decision: "Java 컴파일 언어 Red 전략을 사용자와 합의 (별도 feedback memory에 기록)"
    rationale: "wf-red 스킬은 pytest 예시 기준 — 컴파일 에러는 스킬 자체가 Red로 인정하지 않는다. 최소 스켈레톤(구조)과 UnsupportedOperationException(비구현 신호)으로 절충"
    alternatives_considered: ["컴파일 실패 자체를 Red로 간주 (스킬 FORBIDDEN 위반)", "Phase 3와 동시에 컴파일+구현 (Red 단계 생략)"]
    impact: "이 컨벤션은 이후 모든 백엔드 태스크의 wf-red에 재사용됨 (memory: feedback-java-tdd-red-skeleton)"
  - decision: "SC-10/SC-11은 엔티티에 NOT NULL/CHECK 애노테이션을 아직 넣지 않는 방식으로 Red를 만든다"
    rationale: "이 두 시나리오는 애노테이션 자체가 F7/AC8의 구현이라, 넣으면 즉시 통과해버려 Red가 성립하지 않는다"
    alternatives_considered: ["컬럼 매핑을 아예 빼거나 주석 처리 (다른 시나리오의 직렬화를 깨뜨릴 위험)"]
    impact: "테스트가 'AssertionError: Expecting code to raise a throwable'로 실패 — UnsupportedOperationException이 아니지만 유효한 Red"
  - decision: "ClockConfig.java(순수 @Bean 선언)를 스켈레톤에 포함시킨다"
    rationale: "처음엔 Phase 3로 미뤘으나, 없으면 PerformanceService의 Clock 의존성을 프로덕션 컨텍스트가 주입하지 못해 기존 TicketBookingApplicationTests.contextLoads()가 깨졌다 (MUST #4 위반 위험). @Bean Clock clock() { return Clock.systemUTC(); } 는 조건 분기가 없는 순수 선언이라 스켈레톤 허용 범위"
    alternatives_considered: ["TicketBookingApplicationTests에 @MockBean Clock 추가 (기존 테스트 파일 수정 — 더 침습적)"]
    impact: "target_files 순서상 Phase 3로 미룰 계획이었던 파일 하나가 Phase 2b에서 앞당겨짐"

recovery_prerequisites:
  - CP-2.4

execution_context:
  test_command: "JAVA_HOME=<JDK 21 경로> ./gradlew test"
  build_command: "JAVA_HOME=<JDK 21 경로> ./gradlew build"
  env_required: ["JAVA_HOME을 JDK 21 이상으로 설정해야 gradlew가 동작한다 — 시스템 기본 java는 8"]
  main_files:
    - "backend/src/test/java/com/example/ticket_booking/api/PerformanceApiTest.java"
    - "backend/src/test/java/com/example/ticket_booking/repository/PerformanceConstraintTest.java"
    - "workflow_design/05_scenario/TEST_TASK-001.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/TEST_TASK-001.json"
---

## 무엇을 했나

승인된 시나리오 11건을 테스트 11개로 1:1 옮겼다. 이 프로젝트가 Java/Spring이라
wf-red 스킬의 Python 기준 Red 판정이 그대로 안 맞는 문제를 사용자에게 먼저
확인했고, "컴파일되는 최소 스켈레톤(필드·시그니처·구조적 애노테이션만) +
UnsupportedOperationException" 전략으로 합의했다. 단, SC-10/SC-11은 제약
애노테이션 자체가 구현이라 예외적으로 애노테이션을 아예 넣지 않는 방식으로
Red를 만들었다. `./gradlew test` 실행 결과 신규 11개 전부 실패, 기존 테스트
1건은 계속 통과한다.

## 산출물

| 파일 | 역할 |
|---|---|
| `backend/src/test/java/.../api/PerformanceApiTest.java` | SC-01~09 Red 테스트 |
| `backend/src/test/java/.../repository/PerformanceConstraintTest.java` | SC-10~11 Red 테스트 |
| `backend/src/test/java/.../support/MutableClock.java`, `ClockTestConfig.java` | 테스트용 Clock 제어 |
| `backend/src/main/java/.../domain/Performance.java` 외 8개 | 컴파일용 스켈레톤 프로덕션 클래스 |
| `workflow_design/05_scenario/TEST_TASK-001.json` | Red 결과 기록 |

## 재개 방법

1. `TEST_TASK-001.json` 을 읽는다
2. `AskUserQuestion` 으로 HITL#2 승인을 받는다
