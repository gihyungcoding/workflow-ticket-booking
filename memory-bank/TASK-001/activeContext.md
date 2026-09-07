---
task_id: TASK-001
title: "공연 목록/상세 조회 API"
phase: "5"
phase_name: "Phase 5 - Reflect (완료)"
status: DONE
created_at: 2026-09-03
last_updated: 2026-09-07

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-001-performance-list-detail-api"

last_checkpoint: CP-5.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-001.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-001.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-001.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-001.json"
  test: "workflow_design/05_scenario/TEST_TASK-001.json (attempt 2)"
  dev: "workflow_design/06_dev/DEV_TASK-001.json (attempt 2)"
  verify: "workflow_design/07_verify/VERIFY_TASK-001.json (attempt 2, WARN, EXCEPTION_APPROVE)"
  reflect: "workflow_design/08_reflect/REFLECT_TASK-001.json"
  report: "https://claude.ai/code/artifact/b36df245-d9e3-4c31-a212-d779e73c611f"

pull_request:
  number: 1
  url: "https://github.com/gihyungcoding/workflow-ticket-booking/pull/1"
  base: develop
  opened_at: 2026-09-08
---

## 지금 무엇을 하고 있나

완료됐다. Phase 1~5를 모두 마쳤고(2a/2b/3/4는 각각 한 번씩 재시도),
Phase 4는 WARN → EXCEPTION_APPROVE로 승인됐다. Phase 5 회고를 HITL#4에서
승인받아 태스크를 DONE으로 닫았다. `verified_commit`은
`c0de9086e52312b3f8522a6e4b69cc8957b864fb` 로 고정되어 있다 — 이후 코드를
고치면 `/wf-ship`이 재검증을 요구한다. 원격 저장소(`origin` =
gihyungcoding/workflow-ticket-booking, 사용자 제공)를 연결하고
develop/main/feature 브랜치를 푸시한 뒤 PR #1을 생성했다.

## 다음 한 걸음

없음 — 이 태스크는 종료됐고 PR도 열려 있다. 머지는 리뷰 후 사람이 직접
한다 (PR #1 참고, WARN/예외승인 사유가 본문에 명시되어 있다).

미작성 후속 항목(REFLECT_TASK-001.json 참고, 원하면 별도 태스크/ADR로):
- ADR 후보 2건 — Java 컴파일 언어의 TDD Red 전략, 응답 DTO 패키지 소유 원칙
- 규칙 개선안 3건 — coverage-policy.md(enum 필터 커버리지, 순수 함수 검증
  대상 일치 확인), task-schema.md(경로별 경계값 시나리오)
- 예외 승인된 잔여 리스크 2건 — F9(PerformanceStatusRules) 서술 정정,
  status 필터의 now==openAt/closeAt 경계 시나리오 추가

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
  **단, matches()는 실제로 statusSpecification()이 아니라 of()를 재검증하는
  것이라 JPA Specification과의 런타임 동치는 보장 못 한다** (Phase 4 attempt 2
  code_review 지적, exceptions로 예외 승인됨 — 후속 태스크 후보)
- 이 태스크는 읽기 전용 API만 다룬다 — 공연 등록/수정과 좌석 단위 데이터는
  범위 밖 (`docs/product/features/performance-availability.md` §6)
- TASK-002(프론트 목록/상세 화면)가 이 태스크에 의존한다
- 실제 패키지 루트는 `com.example.ticket_booking`
- `PerformanceResponse`/`PerformanceListResponse`는 `service` 패키지에 있다
  (ARCH-002 위반 회피, `DEV_TASK-001.json.scope_deviations` — attempt 1 기록,
  attempt 2에서도 유지). architecture.md가 DTO 소유 계층을 명시하지 않는
  drift로 REFLECT에 기록됨
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
