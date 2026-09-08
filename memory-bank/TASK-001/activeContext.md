---
task_id: TASK-001
title: "공연 목록/상세 조회 API"
phase: "5"
phase_name: "Phase 5 - Reflect (완료)"
status: DONE
created_at: 2026-09-03
last_updated: 2026-09-08

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-001-performance-list-detail-api"

last_checkpoint: CP-4.3 (retry2)
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-001.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-001.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-001.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-001.json"
  test: "workflow_design/05_scenario/TEST_TASK-001.json (attempt 2)"
  dev: "workflow_design/06_dev/DEV_TASK-001.json (attempt 2)"
  verify: "workflow_design/07_verify/VERIFY_TASK-001.json (attempt 3, PASS, APPROVE)"
  reflect: "workflow_design/08_reflect/REFLECT_TASK-001.json"
  report: "https://claude.ai/code/artifact/b36df245-d9e3-4c31-a212-d779e73c611f"

pull_request:
  number: 1
  url: "https://github.com/gihyungcoding/workflow-ticket-booking/pull/1"
  base: develop
  opened_at: 2026-09-08
---

## 지금 무엇을 하고 있나

완료됐다. Phase 1~5를 모두 마쳤고 PR #1까지 연 뒤 DONE으로 닫혔던 태스크를,
사용자가 직접 코드를 읽고 지적한 클린코드 문제 2건(PerformanceStatus enum이
domain이 아니라 service에 있어 응집도 위반, PerformanceService가 JPA
Specification을 직접 다뤄 DIP·architecture.md §2 위반) 때문에 Phase 4만
재오픈해 attempt 3을 수행했다. 사용자가 지정한 순서(ARCH-003 제약 선추가 →
위반 1건 확인 → architecture.md domain 계층 문서화 → PerformanceStatus/
PerformanceStatusRules를 domain으로 이동, 쿼리 조립을 PerformanceRepositoryImpl로
이동 → 테스트 20건 무수정 통과 → check_freshness.py가 stale을 정확히
검출함을 확인)를 그대로 따랐다. 리팩터가 순수 구조 변경임을 바이트 단위
diff로 직접 증명했고(code-reviewer 서브에이전트는 인프라 문제로 3회 연속
실패), Phase 4 attempt 3을 PASS로 재승인(APPROVE)받았다. `verified_commit`은
이제 `dbfbf8635fc2aa68480dcd7b4225f92803241751` 로 갱신됐다 — 이후 코드를
고치면 `/wf-ship`이 재검증을 요구한다. Phase 5(회고)는 다시 거치지 않았다 —
이번 재오픈은 새 기능이 아니라 이미 승인된 설계에 대한 사후 품질 수정이라
사용자가 Phase 4 재검증만 명시적으로 지시했다.

## 다음 한 걸음

새 커밋 2개(`1d9b7f9` Red, `dbfbf86` Green)가 아직 `origin`에 push되지
않았다. PR #1 본문 갱신과 push 명령은 사용자가 직접 실행해야 한다
(CLAUDE.md 절대 규칙 9 — 머지·푸시·배포는 사람이 실행). 다음 대화 턴에서
정확한 명령을 제시할 것.

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
  attempt 2·3에서도 유지). **REFLECT에 기록됐던 DTO 소유 계층 drift는 Phase 4
  attempt 3에서 architecture.md에 문서화해 해소함**
- **PerformanceStatus/PerformanceStatusRules는 이제 `domain` 패키지에 있다**
  (attempt 3, `service`에서 이동 — 도메인 응집도 문제 수정). `PerformanceService`는
  더 이상 `org.springframework.data.jpa.*`/`jakarta.persistence.criteria.*`를
  import하지 않는다 — 쿼리 조립(Specification)은 전부
  `repository/PerformanceRepositoryImpl`(`PerformanceRepositoryCustom` 구현,
  `SimpleJpaRepository` 상속)로 이동해 DIP를 달성함. `docs/architecture/
  constraints.yaml`에 `ARCH-003`(Service는 영속성 프레임워크 타입을 직접
  다루지 않는다) 신설, `architecture.md` §2에 domain 계층 문서화됨
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
