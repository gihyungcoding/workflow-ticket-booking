---
task_id: TASK-001
title: "공연 목록/상세 조회 API"
phase: "3"
phase_name: "Phase 3 - Green"
status: ACTIVE
created_at: 2026-09-03
last_updated: 2026-09-03

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-001-performance-list-detail-api"

last_checkpoint: CP-3.4
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-001.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-001.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-001.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-001.json"
  test: "workflow_design/05_scenario/TEST_TASK-001.json"
  dev: "workflow_design/06_dev/DEV_TASK-001.json"
---

## 지금 무엇을 하고 있나

Phase 3(Green)을 완료했다. 스켈레톤의 `UnsupportedOperationException`을 실제
로직(F1~F7)으로 교체하고, Red가 미뤄둔 파일(V1 마이그레이션, 엔티티 NOT
NULL/CHECK 애노테이션, `PerformanceExceptionHandler`, `ErrorResponse`,
`PerformanceNotFoundException`)을 채웠다. 테스트 12/12 통과(대상 11 + 기존 1),
아키텍처 제약(ARCH-001/002) 통과. EXIT GATE 통과, 다음은 Phase 4(검증)다.

## 다음 한 걸음

`wf-verify` 스킬로 acceptance_criteria 8건 충족, 회귀, 보안, 범위 이탈을
확인하고 HITL#3 승인을 받는다. `DEV_TASK-001.json.scope_deviations`(DTO 패키지
위치 변경)를 검토 대상에 포함시킨다.

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
- backend 린트 도구는 아직 미정 (CLAUDE.md) — 이번 태스크에서 새로 도입하지 않았다
