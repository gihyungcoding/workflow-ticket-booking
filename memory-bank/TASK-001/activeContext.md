---
task_id: TASK-001
title: "공연 목록/상세 조회 API"
phase: "2a"
phase_name: "Phase 2a - Scenario Design (RETRY)"
status: ACTIVE
created_at: 2026-09-03
last_updated: 2026-09-03

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-001-performance-list-detail-api"

last_checkpoint: CP-4.2
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-001.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-001.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-001.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-001.json"
  test: "workflow_design/05_scenario/TEST_TASK-001.json"
  dev: "workflow_design/06_dev/DEV_TASK-001.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-001.json"
---

## 지금 무엇을 하고 있나

Phase 4(검증)에서 REJECT(RETRY_SCENARIO, COVERAGE_INSUFFICIENT)를 받아 Phase 2a로
되돌아왔다. 정확성 결함 2건이 확인됐다: (1) `PerformanceService`의 CLOSED 상태
판정이 `statusOf()`와 `statusSpecification()`에서 서로 달라 `openAt>closeAt`인
데이터에서 모순된 결과를 낸다, (2) `title`/`venue` 컬럼 길이가 마이그레이션
(200)과 엔티티(JPA 기본값 255)에서 어긋난다. 둘 다 `VERIFY_TASK-001.json` 에
기록되어 있다. (1)은 status 필터의 UPCOMING/SOLD_OUT/CANCELLED 분기를 검증하는
시나리오가 애초에 없어서 놓친 것이라 시나리오 보강부터 다시 시작한다.

## 다음 한 걸음

`wf-scenario` 스킬로 돌아가 다음을 보강한다 (거절 사유가 지목한 부분만 — 처음부터
다시 쓰지 않는다):
1. status=UPCOMING, status=SOLD_OUT, status=CANCELLED 필터를 검증하는 시나리오 추가
   (현재는 OPEN(SC-02)과 CLOSED(SC-06)만 필터 경로를 실행함)
2. `openAt > closeAt` 같은 경계 데이터에서 상태 계산과 필터 결과가 일관되는지
   검증하는 boundary 시나리오 추가 (CLOSED 버그의 재발 방지)
3. SC-10/SC-11의 `assertThatThrownBy` 단언을 `Exception.class`보다 좁게(예:
   `DataIntegrityViolationException`) — code_review 지적
4. title/venue 길이 제약(마이그레이션 VARCHAR(200))을 새 acceptance_criteria로
   추가할지 사용자와 확인 (Plan에 없던 항목)

체크포인트는 `_retry1` 접미사로 저장한다 (`docs/workflow/reject-state-machine.md` §5).

## 알아둬야 할 것

- 예매 상태(UPCOMING/OPEN/SOLD_OUT/CLOSED/CANCELLED)는 DB 컬럼이 아니라
  `java.time.Clock` 주입 기반 시각과 `availableSeats` 로 계산한다 (ADR-0005,
  SQL `now()` 사용 금지) — `PerformanceService.statusOf`/`statusSpecification`
  두 곳에 같은 규칙이 의도적으로 중복되어 있다 (하나가 DB WHERE절, 하나가 응답용)
- 목록은 항상 `start_at >= now` 조건이 상태 필터와 별개로 적용된다
- 이 태스크는 읽기 전용 API만 다룬다 — 공연 등록/수정(쓰기)과 좌석 단위 데이터는
  범위 밖 (`docs/product/features/performance-availability.md` §6)
- TASK-002(프론트 목록/상세 화면)가 이 태스크에 의존한다
- **실제 패키지 루트는 `com.example.ticket_booking`** (기능 문서·tasks.json의
  `com.ticketbooking` 은 실재하지 않는 경로였다)
- **PerformanceResponse/PerformanceListResponse는 `service` 패키지에 있다** —
  Plan 원안(`api/dto`)대로 두면 Service가 API 계층을 import해 ARCH-002를
  위반하기 때문에 Phase 3에서 옮겼다 (`DEV_TASK-001.json.scope_deviations`)
- **이 저장소의 `./gradlew`는 JAVA_HOME이 JDK 17+ 를 가리켜야 동작한다** — 시스템
  기본 `java`는 8이다. 이 머신엔 JDK 21(Microsoft)이
  `/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home` 에
  있다 — 매 gradlew 호출 앞에 `JAVA_HOME=<이 경로>` 를 붙인다
- **Spring Boot 4.1.1 + Jackson 3 패키지 변경 주의** — `@DataJpaTest`는
  `org.springframework.boot.data.jpa.test.autoconfigure`, `@AutoConfigureMockMvc`는
  `org.springframework.boot.webmvc.test.autoconfigure`, Jackson `ObjectMapper`는
  `tools.jackson.databind`(`JsonMapper.builder().build()`로 생성) — 흔히 아는
  Spring Boot 2/3 패키지가 아니다 (memory: project-spring-boot4-package-changes)
- **NOT NULL/CHECK는 `jakarta.persistence.@Table(check=@CheckConstraint(...))`
  로 표현했다** — `org.hibernate.annotations.Check`는 Hibernate 7에서
  deprecated라 표준 JPA 3.2 API를 썼다
- **backend 린트 도구가 생겼다** — `./gradlew spotlessCheck`/`spotlessApply`
  (google-java-format). Phase 3 시점엔 없었으나 develop 병행 작업으로 도입되어
  feature 브랜치에 리베이스+포맷 적용됨(커밋 `fe85807`, 로직 변경 없음)
- **정정된 것**: Phase 1에서 남겼던 `constraints.yaml` ARCH-001/002 glob 우려는
  develop 병행 작업(커밋 `1e38ee7`)으로 이미 `api/**`로 정정되어 있다 — 더 이상
  들고 다닐 필요 없음
- **CLOSED 버그 상세**: `PerformanceService.statusSpecification()`의 CLOSED 분기
  (`cancelled=false AND closeAt<now`)에 `openAt<=now` 조건이 빠져 있다.
  `statusOf()`는 UPCOMING을 먼저 체크하므로(`now<openAt`) 우선순위가 있는데
  Specification 쪽엔 그 우선순위가 없다 — 같은 행이 `?status=UPCOMING`과
  `?status=CLOSED` 양쪽에 잡힐 수 있다. 수정 시 CLOSED 조건에
  `cb.lessThanOrEqualTo(openAt, now)` 를 추가하거나, `openAt<=closeAt` 을
  스키마/엔티티 제약으로 강제하는 것을 검토한다
- **컬럼 길이 버그 상세**: `Performance.title`/`venue`에 `@Column(length=...)`이
  없어 JPA 기본값 255가 적용된다. `V1__create_performance.sql`은 VARCHAR(200)이다
  — 엔티티에 `length = 200` 을 추가해야 한다
