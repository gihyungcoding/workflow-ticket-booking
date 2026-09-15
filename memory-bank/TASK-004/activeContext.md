---
task_id: TASK-004
title: "공연 등록/수정/취소 API"
phase: "2b"
phase_name: "Phase 2b - Red (attempt 3 진입 준비 완료, HITL#1 승인됨)"
status: ACTIVE
created_at: 2026-09-11
last_updated: 2026-09-15

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-004-performance-registration-api"

last_checkpoint: CP-2.4_hitl1-approved_retry2
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-004.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-004.md"
  test: "backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java"
  dev: "workflow_design/06_dev/DEV_TASK-004.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-004.json"
---

## 지금 무엇을 하고 있나

Phase 4 재검증(attempt 2) FAIL → Phase 2a(attempt 3) 롤백을 마쳤다. code-reviewer가
재현한 잔여 결함 2건(title/venue 200자 초과, sections 배열 null 원소)에 대응하는
시나리오 SC-22(title 길이, POST)·SC-23(sections null 원소)·SC-24(title 길이, PUT —
PUT 경로도 같은 결함을 공유함을 code-reviewer가 확인해 신규 추가)를 확정했다.
scenario-validator PASS(경고 3건, 모두 이번 범위 밖으로 판단·미수정), HITL#1 승인
완료. `validate_phase2a_gate.py` 7/7 통과.

## 다음 한 걸음

`wf-red` 스킬로 Phase 2b(attempt 3)를 시작한다 — SC-22/23/24를 실패하는 테스트로
옮긴다. 유의할 점:
- SC-24(PUT)를 작성할 때 `PerformanceRegistrationApiTest` 클래스 레벨의
  `@Transactional`이 PUT 경로의 DB 제약 위반(500)을 가린다는 것을 code-reviewer가
  발견했다 — 서비스 계층에서 던지는 예외/400 응답을 직접 단언하도록 작성한다
- title/venue 길이 검사는 `validateRequired`(등록·수정 공유) 한 곳에서 처리하되,
  SC-22(POST)와 SC-24(PUT) 둘 다 Red로 옮겨 공유가 실제로 되는지 강제한다
- sections null 원소(SC-23)는 Controller의 `toSectionSpec` 변환이 원소를 그대로
  통과시키고 Service의 F11 검증에서 null을 잡도록 — Service만 고치면 Controller
  단계에서 먼저 NPE가 난다

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
