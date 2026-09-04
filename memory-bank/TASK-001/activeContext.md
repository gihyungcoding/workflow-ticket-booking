---
task_id: TASK-001
title: "공연 목록/상세 조회 API"
phase: "2a"
phase_name: "Phase 2a - Scenario Design (RETRY, 완료)"
status: ACTIVE
created_at: 2026-09-03
last_updated: 2026-09-04

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-001-performance-list-detail-api"

last_checkpoint: CP-2.4 (retry1)
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-001.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-001.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-001.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-001.json"
  test: "workflow_design/05_scenario/TEST_TASK-001.json (Phase 2b에서 재작성 예정)"
  dev: "workflow_design/06_dev/DEV_TASK-001.json (Phase 3에서 재작성 예정)"
  verify: "workflow_design/07_verify/VERIFY_TASK-001.json (attempt 1, FAIL)"
---

## 지금 무엇을 하고 있나

Phase 4 REJECT(RETRY_SCENARIO, COVERAGE_INSUFFICIENT) 이후 Phase 2a 재시도를
마치고 HITL#1 재승인을 받았다. 시나리오 11→18건, Plan flow 7→9개(F8/F9),
acceptance_criteria 8→11개로 확장했다. 다음은 Phase 2b(Red) 재진입이다 —
기존 구현(스켈레톤 이후 이미 Green까지 갔던 코드)이 있는 채로 시작하므로,
이번엔 "테스트를 실패시키는" 것이 아니라 "새로 추가된 부분만 실패하고 기존
부분은 여전히 통과하는지"를 구분해서 봐야 한다.

## 다음 한 걸음

`wf-red` 스킬로 Phase 2b 재진입:
1. 새 시나리오(SC-12~18) 테스트 함수 작성 — 기존 코드(Phase 3 산출물)가
   CLOSED 버그와 길이 버그를 아직 안 고쳤으므로 SC-15/16/17/18은 Red여야
   정상이다. SC-12/13/14(UPCOMING/SOLD_OUT/CANCELLED 필터)는 이미 구현된
   로직으로 이미 통과할 수도 있다 — 그렇다면 그건 실제로 Red가 아니라
   기존 구현이 이미 맞았다는 뜻이니 통과를 그대로 인정하고 넘어간다
   (다만 "이미 통과하는 새 테스트"가 있으면 왜 그런지 반드시 확인한다)
2. `PerformanceStatusRules` 순수 유닛 테스트 1건 추가 (F9, SC와 대응 없는 예외 —
   `SCENARIO_TASK-001.md` "Phase 2b 인벤토리 예외" 섹션 참고). 인벤토리는
   시나리오 18 + 이 예외 1 = 테스트 함수 19개가 정상
3. 인벤토리 검증 시 이 19개 기준으로 확인한다

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
- SC-10/11/15/16의 Then은 `DataIntegrityViolationException`을 직접 명시한다
  (Exception.class보다 좁힘, code_review 지적 반영) — Spring Data 예외 변환에
  의존하는 트레이드오프임을 인지하고 유지하기로 함(HITL#1)
