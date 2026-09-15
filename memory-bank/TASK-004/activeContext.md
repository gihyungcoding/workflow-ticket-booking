---
task_id: TASK-004
title: "공연 등록/수정/취소 API"
phase: "2b"
phase_name: "Phase 2b - Red (재시도, attempt 2)"
status: ACTIVE
created_at: 2026-09-11
last_updated: 2026-09-15

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-004-performance-registration-api"

last_checkpoint: CP-2.4_retry1
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-004.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-004.md"
  test: "backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java"
  dev: "workflow_design/06_dev/DEV_TASK-004.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-004.json"
---

## 지금 무엇을 하고 있나

Phase 4 FAIL(code-reviewer가 좌석 상한 우회·DoS 가능성·입력검증 부재를 재현) 이후
RETRY_SCENARIO로 Phase 2a를 재작업했다. 오류 시나리오 6건(SC-16~21: 필수 필드
누락→INVALID_REQUEST/F10, 역방향 행 범위·seatsPerRow 범위·빈 rowStart·grade
길이→INVALID_SECTION/F11, 정수 오버플로→SEAT_LIMIT_EXCEEDED/F3 boundary)을 추가해
시나리오 총 20건, 독립검증 3회 후 PASS, HITL#1 재승인 받음. Plan에도 F10/F11 흐름과
새 출력 2종을 반영했다.

## 다음 한 걸음

`wf-red` 스킬로 Phase 2b를 재개한다 — SC-16~SC-21 6개를 기존
`PerformanceRegistrationApiTest.java`에 이어서 Red 테스트로 작성한다(기존 14개는
그대로 둔다). 전체 실행 시 기존 14개는 계속 통과, 신규 6개만 실패해야 한다.

## 알아둬야 할 것 (Phase 3 구현 시 반영할 설계 결정)

- **Bean Validation을 쓰지 않는다.** 기존 코드베이스가 예외-당-규칙 패턴(예:
  `EmptySectionsException`)을 이미 쓰고 있어, 새 검증도 `InvalidRequestException`
  (F10, 최상위 필드 null/blank)과 `InvalidSectionException`(F11, 구역 필드 형식
  오류)이라는 수동 예외 2개로 구현한다. `@Valid`/`jakarta.validation` 은 도입하지
  않는다
- **검증 순서(반드시 이 순서로 구현)**: F10(필수 필드) → F9(빈 sections) →
  F11(구역 형식) → F2(시각 순서) → F3(좌석 상한, long 산술) → F4(행 범위 겹침).
  `sections=null`(INVALID_REQUEST)과 `sections=[]`(EMPTY_SECTIONS)은 서로 다른
  코드 경로 — null 체크가 먼저다
- `grade` 는 20자로 제한한다 (seat_label VARCHAR(30) 초과 방지 — F11)
- 좌석수 합산은 `long` 으로 계산하고 5,000 이하 확인 후에만 int로 캐스팅한다 (int
  오버플로로 상한을 우회하던 보안 결함 수정)
- `price`/`rowStart`/`rowEnd`(A~Z 단일 대문자, rowStart<=rowEnd)/`seatsPerRow`(1
  이상) 검증은 모두 `InvalidSectionException` 하나로 묶는다 — 시나리오는
  SC-17/18/19/21 네 갈래만 있지만 구현은 F11 note에 적힌 다섯 갈래(rowStart>rowEnd,
  seatsPerRow<1, 빈/잘못된 row, grade 길이, price 음수/null)를 전부 방어해야 한다
- Seat 엔티티에 FK 애노테이션을 추가한다 (아래 기존 항목 참고) — 시나리오 없이
  바로 고치는 순수 스키마 정합성 수정

## 알아둬야 할 것 (Phase 4 FAIL 상세)

- AC4 우회 재현: sections=[{VIP,A~Y,1000},{R,Z~E,1000}] → totalSeats 합산이 int
  음수 상쇄로 5,000 이하처럼 보여 상한 검사 통과, 실제로는 seat 25,000행 생성
- int 오버플로 재현: seatsPerRow를 12억대로 두 구역에 주면 합산이 음수로 래핑되어
  상한 검사를 우회하고 generateSeats가 수억 개 객체 생성을 시도(OOM 위험)
- Plan의 design.inputs 제약(필수/길이/범위/A~Z 단일 대문자/rowStart<=rowEnd)이
  전혀 구현되지 않음 — spring-boot-starter-validation 의존성은 있으나 미사용
- seat_label VARCHAR(30)이 grade(50자)+행+번호 최대 조합(56자)을 못 담음 — grade
  입력 길이를 검증으로 좁히는 쪽이 스키마 변경보다 간단
- Seat 엔티티가 V2 마이그레이션의 FK를 애노테이션으로 표현하지 않아 테스트/프로덕션
  스키마가 갈림 (Performance.java의 ADR-0006 선례와 불일치) — 시나리오 없이 Phase 3
  에서 바로 고칠 수 있는 내부 정합성 문제
- 상세: `workflow_design/07_verify/VERIFY_TASK-004.json` 의 `code_review.findings`

## 알아둬야 할 것

- ADR-0009: 좌석은 구역(등급)×행×열 입력으로 서버가 개별 `seat` 행을 생성한다 — 이 태스크의
  핵심 데이터 모델 결정
- 주최자 인증/소유권 확인은 이번 태스크 범위 밖이다 (`performance-registration.md` §7)
- 시각 순서(`openAt <= closeAt <= startAt`)와 좌석 총수(≤5,000) 검증은 DB 제약이 아니라
  서비스 계층 검증이다 (`performance-registration.md` §1-3)
- 취소 응답은 `cancelled` 필드를 새로 노출하지 않는다 — `PerformanceResponse` 에는
  `status` 만 있고, 취소되면 `PerformanceStatusRules` 가 이를 최우선으로 `CANCELLED` 로
  계산한다. Red 테스트는 `status == CANCELLED` 를 단언한다 (Phase 2a 검증에서 발견,
  CP-2.3 참고)
- 행 범위 겹침 판정은 `[rowStart, rowEnd]` 알파벳 구간의 교집합만으로 본다 (좌석 번호
  조합은 보지 않음). `rowStart`/`rowEnd` 는 A~Z 단일 대문자만 지원한다
- 기존 `performance` 테이블 스키마는 바뀌지 않는다 — `seat` 테이블만 신설
