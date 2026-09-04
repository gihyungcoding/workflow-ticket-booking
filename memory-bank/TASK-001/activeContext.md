---
task_id: TASK-001
title: "공연 목록/상세 조회 API"
phase: "2b"
phase_name: "Phase 2b - Red (RETRY, 완료)"
status: ACTIVE
created_at: 2026-09-03
last_updated: 2026-09-04

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-001-performance-list-detail-api"

last_checkpoint: CP-2.6 (retry1)
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-001.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-001.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-001.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-001.json"
  test: "workflow_design/05_scenario/TEST_TASK-001.json (attempt 2)"
  dev: "workflow_design/06_dev/DEV_TASK-001.json (Phase 3에서 재작성 예정)"
  verify: "workflow_design/07_verify/VERIFY_TASK-001.json (attempt 1, FAIL)"
---

## 지금 무엇을 하고 있나

Phase 2b(Red) 재시도를 마치고 HITL#2 재승인을 받았다. SC-12~18 테스트와
`PerformanceStatusRules` 단위 테스트(F9)를 추가했다. 전체 20개 테스트 중
3개(SC-15, SC-16, PerformanceStatusRulesTest)만 Red이고 나머지 17개는
기존(attempt 1) 구현이 이미 올바르거나 아직 제약이 없어 자연스럽게 Green —
이는 정상이며 TEST_TASK-001.json에 사유를 전부 기록했다.

## 다음 한 걸음

`wf-develop` 스킬로 Phase 3(Green) 재진입:
1. `Performance.java`: `title`/`venue`에 `@Column(length = 200)` 추가,
   `@Table(check=...)` 에 `open_at <= close_at` 조건 추가 (F7/F8)
2. `V1__create_performance.sql`: `title`/`venue` 컬럼은 이미 VARCHAR(200)이라
   변경 불필요, `CHECK (open_at <= close_at)` 만 추가 (F8)
3. `PerformanceStatusRules.of()`/`matches()` 실제 로직 구현 (기존
   `PerformanceService.statusOf()`의 로직을 그대로 옮긴다) — F9
4. `PerformanceService.toResponse()` 가 private `statusOf()` 대신
   `PerformanceStatusRules.of()` 를 호출하도록 변경, 기존 private
   `statusOf()` 메서드는 제거
5. `PerformanceService.statusSpecification()` 의 CLOSED 분기는 **코드를
   고치지 않는다** — F8이 성립하면 이미 올바르다 (Plan target_files 설명 참고).
   단, 이 동치를 코드 주석으로 남긴다
6. 전체 테스트 20개 + PerformanceStatusRulesTest 모두 통과 확인, 아키텍처
   제약·spotlessCheck 재확인

## 알아둬야 할 것

- 예매 상태(UPCOMING/OPEN/SOLD_OUT/CLOSED/CANCELLED)는 `java.time.Clock` 기반
  시각과 `availableSeats` 로 계산한다 (ADR-0005, SQL `now()` 금지)
- **CLOSED 버그의 근본 해결책은 코드 패치가 아니라 F8(openAt<=closeAt 불변조건)
  추가다** — 이 불변조건이 성립하면 기존 `statusSpecification()`의 CLOSED
  분기 코드는 고칠 필요가 없어진다(수학적으로 동치가 됨, Plan F8/target_files
  참고). Phase 3에서 `Performance.java`와 `V1__create_performance.sql`에
  `CHECK (open_at <= close_at)` 를 추가하는 것이 실제 수정 작업이다
- **컬럼 길이 버그**: `Performance.title`/`venue`에 `@Column(length = 200)`
  추가 필요 (현재 JPA 기본값 255, 마이그레이션은 VARCHAR(200))
- **F9: PerformanceStatusRules 신규 클래스** — `of(...)`/`matches(...)` 순수
  정적 메서드로 상태 판정 규칙을 분리한다. `PerformanceService.toResponse()`는
  기존 private `statusOf()` 대신 이걸 호출하도록 바꾼다. 이 분리는 상호배타성을
  단위 테스트로 보장하기 위함(HITL#1 재승인 시 사용자 승인) — 단, JPA
  Specification과의 런타임 동치까지는 보장 못 한다는 한계가 Plan에 명시돼 있다
- 이 태스크는 읽기 전용 API만 다룬다 — 공연 등록/수정과 좌석 단위 데이터는
  범위 밖 (`docs/product/features/performance-availability.md` §6)
- TASK-002(프론트 목록/상세 화면)가 이 태스크에 의존한다
- 실제 패키지 루트는 `com.example.ticket_booking`
- `PerformanceResponse`/`PerformanceListResponse`는 `service` 패키지에 있다
  (ARCH-002 위반 회피, `DEV_TASK-001.json.scope_deviations` — attempt 1 기록,
  attempt 2에서도 유지)
- **`./gradlew`는 JAVA_HOME이 JDK 17+ 를 가리켜야 동작한다** — 시스템 기본
  `java`는 8. JDK 21(Microsoft)이
  `/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home` 에 있다
- **Spring Boot 4.1.1 + Jackson 3 패키지 변경 주의** — `@DataJpaTest`는
  `org.springframework.boot.data.jpa.test.autoconfigure`, `@AutoConfigureMockMvc`는
  `org.springframework.boot.webmvc.test.autoconfigure`, Jackson `ObjectMapper`는
  `tools.jackson.databind`(`JsonMapper.builder().build()`) (memory:
  project-spring-boot4-package-changes)
- NOT NULL/CHECK는 `jakarta.persistence.@Table(check=@CheckConstraint(...))`
  로 표현 (`org.hibernate.annotations.Check`는 Hibernate 7에서 deprecated)
- backend 린트: `./gradlew spotlessCheck`/`spotlessApply` (google-java-format,
  develop 병행 작업으로 도입됨, 커밋 `fe85807`)
- `TestEntityManager`는 `org.springframework.boot.jpa.test.autoconfigure`
  패키지(아티팩트 `spring-boot-jpa-test`, `data-` 접두어 없음)에 있다 — SC-17/18
  재조회 검증에 사용 중 (memory: project-spring-boot4-package-changes)
- SC-10/11/15/16의 Then은 `DataIntegrityViolationException`을 직접 명시한다
  (Exception.class보다 좁힘, code_review 지적 반영) — Spring Data 예외 변환에
  의존하는 트레이드오프임을 인지하고 유지하기로 함(HITL#1)
