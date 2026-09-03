---
task_id: TASK-001
title: "공연 목록/상세 조회 API"
phase: "2b"
phase_name: "Phase 2b - Red"
status: ACTIVE
created_at: 2026-09-03
last_updated: 2026-09-03

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-001-performance-list-detail-api"

last_checkpoint: CP-2.6
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-001.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-001.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-001.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-001.json"
  test: "workflow_design/05_scenario/TEST_TASK-001.json"
---

## 지금 무엇을 하고 있나

Phase 2b(Red)를 완료하고 HITL#2 승인을 받았다. 시나리오 11건을 테스트 11개로
1:1 옮겨 전부 실패시켰다(UnsupportedOperationException 9건, AssertionError 2건).
기존 테스트는 계속 통과한다. 다음은 Phase 3(Green — 실제 구현)이다.

## 다음 한 걸음

`wf-develop` 스킬로 스켈레톤의 `UnsupportedOperationException`을 실제 로직으로
바꾸고, Phase 2b가 미룬 파일(V1 마이그레이션, PerformanceExceptionHandler,
ErrorResponse, PerformanceNotFoundException, Performance 엔티티의 NOT NULL/CHECK
애노테이션)을 채워 11개 테스트를 모두 통과시킨다.

## 알아둬야 할 것

- 예매 상태(UPCOMING/OPEN/SOLD_OUT/CLOSED/CANCELLED)는 DB 컬럼이 아니라
  `java.time.Clock` 주입 기반 시각과 `availableSeats` 로 계산한다 (ADR-0005,
  SQL `now()` 사용 금지)
- 목록은 항상 `start_at >= now` 조건이 상태 필터와 별개로 적용된다
- 이 태스크는 읽기 전용 API만 다룬다 — 공연 등록/수정(쓰기)과 좌석 단위 데이터는
  범위 밖 (`docs/product/features/performance-availability.md` §6)
- 마이그레이션 도구 미정 — Flyway 권장 (동 문서 §5)
- TASK-002(프론트 목록/상세 화면)가 이 태스크에 의존한다
- **실제 패키지 루트는 `com.example.ticket_booking`** — 기능 문서·tasks.json의
  `com.ticketbooking` 은 실재하지 않는 경로다. Phase 2b/3에서 파일을 만들 때
  `PLAN_TASK-001.json` 의 `target_files` 경로(정정됨)를 따른다
- `status` 쿼리 필터는 DB WHERE 절로 구현해야 한다(F3) — 메모리 필터링은
  페이지네이션/totalElements를 깨뜨린다
- **후속 확인 필요**: `constraints.yaml` 의 ARCH-001/002 glob이 `**/controller/**`
  인데 실제 API 계층 위치는 `**/api/**` (architecture.md §2) — 이대로면 Phase 4에서
  이 태스크의 신규 컨트롤러가 검사 대상에서 빠진다. Phase 4 전에 사용자와 확인해
  glob을 정정할지 결정한다
- **Plan이 Phase 2a 중 개정됨**: F7(엔티티 제약-마이그레이션 제약 일치)과 AC8을
  사용자 요청으로 추가했다 — `PLAN_TASK-001.json.amendments` 참고. Performance
  엔티티는 V1 마이그레이션의 NOT NULL/CHECK를 애노테이션으로도 표현해야 하고,
  Phase 2b는 SC-10/SC-11(저장이 "DB에 실제로 반영되는 시점까지" 거부되는지)을
  `@DataJpaTest`로 옮겨야 한다
- SC-07/SC-08은 Clock 고정 시각을 **실제 시스템 시각과 다르게**(예: 몇 년 뒤) 잡아야
  한다 — 그래야 구현이 Clock 대신 SQL now()/시스템 시각을 쓰는 결함을 status 값
  자체로 잡아낼 수 있다
- **이 저장소의 `./gradlew`는 JAVA_HOME이 JDK 17+ 를 가리켜야 동작한다** — 시스템
  기본 `java`는 8이라 그대로 실행하면 "Gradle requires JVM 17 or later" 로 즉시
  실패한다. 이 머신엔 JDK 21(Microsoft) 이 `/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home`
  에 있다 — 매 gradlew 호출 앞에 `JAVA_HOME=<이 경로>` 를 붙인다
- **Spring Boot 4.1.1 + Jackson 3 패키지 변경 주의** — `@DataJpaTest`는
  `org.springframework.boot.data.jpa.test.autoconfigure`, `@AutoConfigureMockMvc`는
  `org.springframework.boot.webmvc.test.autoconfigure`, Jackson `ObjectMapper`는
  `tools.jackson.databind`(`JsonMapper.builder().build()`로 생성) — 흔히 아는
  Spring Boot 2/3 패키지가 아니다 (memory: project-spring-boot4-package-changes)
- **Phase 2b는 Java 컴파일 언어라 스켈레톤 전략을 썼다** — 프로덕션 클래스는
  컴파일만 되는 최소 구조(필드/시그니처)이고 메서드 본문은
  `UnsupportedOperationException`. 예외: SC-10/SC-11은 엔티티의 NOT NULL/CHECK
  애노테이션 자체가 구현이라 아직 넣지 않았다 — Phase 3에서 `Performance.java`에
  `@Column(nullable=false)`/`@Check` 등을 추가하는 것이 그 시나리오의 진짜 구현이다.
  또한 V1 마이그레이션, `PerformanceExceptionHandler`, `ErrorResponse`,
  `PerformanceNotFoundException`은 Red에 불필요해 아직 만들지 않았다
  (memory: feedback-java-tdd-red-skeleton, `TEST_TASK-001.json.java_compiled_language_notes`)
