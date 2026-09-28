---
checkpoint_id: CP-2.5
checkpoint_name: "Red 테스트 작성 및 실행 확인"
task_id: TASK-006
phase: "2b"
phase_name: "Phase 2b - Red"
saved_at: 2026-09-28T00:20:00Z
status: ACTIVE

work_summary: "SC-01/SC-02 테스트 작성 → Red 확인(400 기대, 201 실제). SC-03은 컴파일 스켈레톤만으로 already_passing"

progress:
  completed:
    - "SectionRequest.java·SectionSpec.java의 price/seatsPerRow 타입을 Integer→BigDecimal로 변경"
    - "PerformanceService.java 호출부를 새 타입에 맞게 기계적으로만 어댑테이션(.signum(), .compareTo(BigDecimal.ONE), .intValue()) — 새 정수 검증 로직은 추가하지 않음"
    - "PerformanceRegistrationApiTest.java에 테스트 3건 추가 (test_task006_sc01/02/03)"
    - "./gradlew compileJava compileTestJava → BUILD SUCCESSFUL (컴파일 통과 확인)"
    - "./gradlew test --tests \"*PerformanceRegistrationApiTest*\" → 26 실행, 2 실패(SC-01/SC-02, 정확히 'expected 400 but was 201'), 24 통과(기존 23 + SC-03)"
    - "인벤토리 확인: 시나리오 3 == 테스트 함수 3"
    - "TEST_TASK-006.json 저장"
  in_progress: "없음 — HITL#2 대기"
  blocked: []

next_steps:
  - priority: 1
    task: "HITL#2 — 사용자에게 Red 결과 제시 후 승인 요청"

decisions:
  - decision: "이 태스크는 일반적인 'UnsupportedOperationException 스켈레톤' 관례(memory: feedback_java_tdd_red_skeleton.md)를 적용하지 않는다 — 대신 타입만 바꾸고 기존 호출부를 절삭 방식(.intValue())으로 기계적 어댑테이션한다"
    rationale: "이 태스크는 새 클래스를 스캐폴딩하는 게 아니라, 20건 넘는 기존 통과 테스트가 의존하는 PerformanceService.validateSection 등 기존 메서드의 필드 타입을 바꾸는 작업이다. 몸체를 UnsupportedOperationException으로 비우면 그 기존 테스트 전부가 깨져 EXIT GATE('기존 테스트는 계속 통과')를 위반한다. 판단 기준은 예외 종류가 아니라 '이 실패가 아무것도 구현하지 않았을 때 일어나는가'(같은 메모리 문서 원칙 5)다 — .intValue() 절삭은 새 기능(정수 강제)을 전혀 구현하지 않은 상태 그 자체이므로 이 기준을 만족한다"
    alternatives_considered: ["validateSection 전체를 throw로 스텁화 (기각: 기존 23개 테스트 전부 깨짐)", "SectionSpec 타입을 유지하고 Controller에서만 검증 (기각: architecture.md §2 위반, Phase 1 CP-1.3 결정과 배치)"]
    impact: "SC-01/SC-02는 assertion mismatch(400 기대, 201 실제)로 Red — 컴파일 오류나 테스트 코드 결함이 아님. SC-03은 구조 변경만으로 이미 통과 — already_passing으로 명시"
  - decision: "SC-03을 already_passing으로 처리"
    rationale: "타입 변경(BigDecimal)과 기계적 어댑테이션(.intValue())만으로는 정수 JSON 입력에 한해 기존과 동일하게 동작한다 — 새 정수 검증 로직(F1/F2)이 아직 없기 때문이다. 이는 '구현 전에도 이미 참'인 회귀 성격의 시나리오라 구조적으로 Red를 만들 수 없다"
    alternatives_considered: ["억지로 실패시키기 위해 단언 조작 (금지 사항 — FORBIDDEN #2 위반)"]
    impact: "Phase 3에서 F1/F2 로직을 추가한 뒤에도 SC-03이 계속 통과하는지가 회귀 확인 기준이 된다"

recovery_prerequisites:
  - CP-2.4

execution_context:
  test_command: "./gradlew test --tests \"*PerformanceRegistrationApiTest*\""
  build_command: "./gradlew compileJava compileTestJava"
  env_required: ["JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home"]
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/api/dto/SectionRequest.java"
    - "backend/src/main/java/com/example/ticket_booking/service/SectionSpec.java"
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"
    - "backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/TEST_TASK-006.json"
---

## 무엇을 했나

SC-01~SC-03을 PerformanceRegistrationApiTest.java에 테스트 메서드로 옮겼다. 컴파일
언어라 스켈레톤이 선행돼야 했는데, 이 태스크는 신규 기능이 아니라 기존에 검증된
`PerformanceService.validateSection` 등의 필드 타입을 바꾸는 작업이라 표준 스켈레톤
관례(몸체를 예외로 비움)를 그대로 적용할 수 없었다 — 그러면 기존 23개 테스트가 전부
깨진다. 대신 타입만 Integer→BigDecimal로 바꾸고, 기존 호출부는 컴파일이 되도록
`.intValue()` 절삭 방식으로만 어댑테이션했다(새 정수 검증 로직은 추가하지 않음).
결과: SC-01/SC-02는 "400 기대, 201 실제"로 정확히 Red가 됐고(구현이 없어서 실패,
컴파일 오류 아님), SC-03은 정수 입력에 대해 절삭이 무손실이라 already_passing이
됐다. 기존 23개 테스트는 전부 그대로 통과했다(총 26 실행, 2 실패, 24 통과).

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/05_scenario/TEST_TASK-006.json` | Red 결과, 인벤토리 확인, compile_skeleton 근거 |
| `backend/src/test/java/.../PerformanceRegistrationApiTest.java` | 테스트 3건 추가 (Red 상태로 커밋 예정) |
| `backend/src/main/java/.../SectionRequest.java`, `SectionSpec.java`, `PerformanceService.java` | 컴파일 스켈레톤 (타입 변경 + 기계적 어댑테이션) |

## 다음 단계

HITL#2 — 사용자 승인 요청.
