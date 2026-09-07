---
task_id: TASK-001
title: "공연 목록/상세 조회 API"
phase: "4"
phase_name: "Phase 4 - Verify (RETRY, 완료 — EXCEPTION_APPROVE)"
status: ACTIVE
created_at: 2026-09-03
last_updated: 2026-09-07

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-001-performance-list-detail-api"

last_checkpoint: CP-4.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-001.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-001.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-001.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-001.json"
  test: "workflow_design/05_scenario/TEST_TASK-001.json (attempt 2)"
  dev: "workflow_design/06_dev/DEV_TASK-001.json (attempt 2)"
  verify: "workflow_design/07_verify/VERIFY_TASK-001.json (attempt 2, WARN, EXCEPTION_APPROVE)"
---

## 지금 무엇을 하고 있나

Phase 4(검증) 재시도를 마쳤다 — status: WARN, EXCEPTION_APPROVE로 승인됨.
attempt 1의 정확성 결함 2건(CLOSED 로직, 컬럼 길이)은 실제로 해소됐음을
code-reviewer가 H2 생성 DDL과 실제 SQL 예외 코드까지 확인했다. 새로 발견된
잔여 리스크 2건(F9 단위 테스트가 실제로는 statusSpecification()을 검증하지
않는 문제, status 필터의 now==openAt/closeAt 경계 커버리지 공백)은 활성
결함이 아니라 향후 회귀 검출 공백이라 예외 승인하고 후속 태스크로 남겼다.
`verified_commit`은 `c0de9086e52312b3f8522a6e4b69cc8957b864fb` 로 고정됨 —
이 커밋 이후 코드를 고치면 `/wf-ship`이 재검증을 요구한다.

## 다음 한 걸음

`wf-reflect` 스킬로 Phase 5(회고) 진입:
1. KPT 회고 — 이번 태스크는 Phase 4에서 두 번(attempt 1 FAIL, attempt 2
   WARN/예외승인) 걸렸다. Problem 후보: 시나리오 작성 시 상태 필터 5개
   분기 전부를 처음부터 커버하지 않은 것, Java 컴파일 언어의 TDD Red 정의
   재정립에 초기 라운드를 소비한 것
2. exceptions 배열의 후속 조치 2건(F9 서술 정정, 경계값 픽스처 추가)을
   회고의 개선 제안 또는 별도 후속 태스크로 격상할지 결정
3. 완료 리포트 작성 후 HITL#4 승인
   타당한지, PerformanceStatusRules.matches()가 실제로 of()에 트리비얼하게
   위임하지 않고 독립적인지 재검토를 요청한다
3. python scripts/check_architecture.py, ./gradlew spotlessCheck 재실행
4. VERIFY_TASK-001.json에 attempt=2 로 새 판정 기록 (reject.attempt=1 이었던
   기록은 보존)

## 알아둬야 할 것

- 예매 상태(UPCOMING/OPEN/SOLD_OUT/CLOSED/CANCELLED)는 `java.time.Clock` 기반
  시각과 `availableSeats` 로 계산한다 (ADR-0005, SQL `now()` 금지)
- **CLOSED 버그는 F8(openAt<=closeAt CHECK, Performance.java + V1 마이그레이션)
  추가로 해결 완료** — `statusSpecification()`의 CLOSED 분기 코드는 고치지
  않았다(수학적으로 이미 동치, 코드 주석 참고)
- **컬럼 길이 버그는 해결 완료** — `Performance.title`/`venue`에
  `@Column(length = 200)` 추가함
- **F9: PerformanceStatusRules 구현 완료** — `of(...)`는 기존 `statusOf()`
  로직을 그대로 옮긴 것, `matches(...)`는 of()에 위임하지 않고 독립적인
  우선순위 기반 불리언 식으로 새로 작성(트리비얼 위임이면 단위 테스트가
  아무것도 교차검증 못 하기 때문). `PerformanceStatusRulesTest`의 6번째
  픽스처가 openAt>closeAt(malformed, DB에서는 F8이 막지만 순수 함수는 호출
  가능) 로 원래 버그 모양을 직접 재현해 F8과 무관하게 일치함을 증명한다.
  JPA Specification과의 런타임 동치까지는 보장 못 한다는 한계는 여전히 Plan에
  남아 있다
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
