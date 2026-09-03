---
checkpoint_id: CP-3.2
checkpoint_name: "테스트 통과 확인"
task_id: TASK-001
phase: "3"
phase_name: "Phase 3 - Green"
saved_at: 2026-09-03T13:08:36Z
status: ACTIVE

work_summary: "스켈레톤의 UnsupportedOperationException을 실제 로직으로 교체했다. 대상 테스트 11개 + 기존 1개 = 12개 전부 통과. 아키텍처 제약(ARCH-001/002) 재확인 통과."

progress:
  completed:
    - "V1__create_performance.sql 작성"
    - "Performance 엔티티에 NOT NULL/CHECK 애노테이션 + @CreationTimestamp 추가 (F7/AC8 실제 구현)"
    - "PerformanceRepository에 JpaSpecificationExecutor 추가"
    - "PerformanceService: getPerformances(F1/F3/F4/F6), getPerformance(F5), statusOf(F2) 구현"
    - "PerformanceNotFoundException, PerformanceExceptionHandler, ErrorResponse 작성 (404 매핑)"
    - "PerformanceController: size 100 클램프, import 경로 갱신"
    - "./gradlew clean test — 12/12 통과 (대상 11 + 기존 1)"
    - "python scripts/check_architecture.py — ARCH-001/002 통과 확인 (한 차례 위반 발견 후 수정, 아래 결정 참고)"
  in_progress: null
  blocked: []

next_steps:
  - priority: 1
    task: "리팩토링 검토 (Step 4) — 이미 최소 구현이 비교적 정돈되어 있어 큰 변경 불필요"
  - priority: 2
    task: "DEV_TASK-001.json 저장 및 커밋"

decisions:
  - decision: "PerformanceResponse/PerformanceListResponse를 Plan이 지정한 api/dto가 아니라 service 패키지로 배치"
    rationale: "Service가 이 두 타입을 반환값으로 만들어야 하는데 api/dto에 두면 Service→API 역방향 의존(ARCH-002)이 된다. check_architecture.py 실행으로 실제 위반을 확인한 뒤 옮겼다"
    alternatives_considered: ["Service가 도메인 전용 결과 타입을 반환하고 Controller가 별도로 API DTO로 재매핑 (필드가 100% 동일해 boilerplate만 늘어남)"]
    impact: "DEV_TASK-001.json.scope_deviations에 기록. Controller(API)가 service 패키지를 import하는 것은 허용된 방향이라 문제 없음"
  - decision: "Hibernate @Check(deprecated) 대신 jakarta.persistence @Table(check=@CheckConstraint(...)) 사용"
    rationale: "컴파일 시 deprecation 경고 발생 — 표준 JPA 3.2 API가 이미 있어 그쪽으로 교체"
    alternatives_considered: ["경고 무시"]
    impact: "컴파일 경고 0건"
  - decision: "Phase 1에서 남겼던 constraints.yaml glob 우려(controller vs api)는 재확인 결과 이미 해소되어 있었다"
    rationale: "Phase 3 시점에 다시 읽어보니 ARCH-001/002 모두 backend/.../api/** 를 가리키고 있었고, 실제로 ARCH-002가 이번 DTO 배치 실수를 정상적으로 잡아냈다"
    alternatives_considered: []
    impact: "Phase 4 보고에서 이 우려를 다시 꺼낼 필요 없음"

recovery_prerequisites:
  - CP-2.6

execution_context:
  test_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew clean test"
  build_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew build"
  env_required: ["JAVA_HOME을 JDK 21 이상으로 설정"]
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"
    - "backend/src/main/java/com/example/ticket_booking/domain/Performance.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/05_scenario/TEST_TASK-001.json"
---

## 무엇을 했나

Red 테스트 11개를 통과시키는 최소 구현을 완료했다. 상태 계산(F2)과 DB 필터
(F3)는 같은 규칙을 서로 다른 형태(Java 조건문 / JPA Specification)로 표현해야
해서 의도적으로 중복되며, 코드에 그 이유를 주석으로 남겼다. 구현 도중
`check_architecture.py`로 ARCH-002 위반(Service가 api.dto를 import)을 발견해
DTO 패키지를 service로 옮겨 해결했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `backend/src/main/java/.../service/PerformanceService.java` | F1~F6 구현 |
| `backend/src/main/java/.../domain/Performance.java` | NOT NULL/CHECK 애노테이션 (F7) |
| `backend/src/main/resources/db/migration/V1__create_performance.sql` | 마이그레이션 |

## 재개 방법

1. 리팩토링 검토 후 `DEV_TASK-001.json` 을 저장한다
2. 커밋한다
