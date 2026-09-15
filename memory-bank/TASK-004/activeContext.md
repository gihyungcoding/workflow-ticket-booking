---
task_id: TASK-004
title: "공연 등록/수정/취소 API"
phase: "4"
phase_name: "Phase 4 - Verify (FAIL, 롤백 대기)"
status: ACTIVE
created_at: 2026-09-11
last_updated: 2026-09-15

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-004-performance-registration-api"

last_checkpoint: CP-4.2
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-004.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-004.md"
  test: "backend/src/test/java/com/example/ticket_booking/api/PerformanceRegistrationApiTest.java"
  dev: "workflow_design/06_dev/DEV_TASK-004.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-004.json"
---

## 지금 무엇을 하고 있나

Phase 4 검증에서 code-reviewer가 재현 가능한 결함을 찾아 status: FAIL. 좌석 상한
(AC4)이 역방향 행 범위·정수 오버플로로 우회되고(실제 실행으로 재현), 인증 없는
등록 엔드포인트가 이 경로로 OOM을 유발할 수 있다(DoS). Plan이 명시한 입력 검증도
구현되지 않았다. 테스트/린트/아키텍처 제약 자체는 모두 통과했다 — 시나리오가
다루지 않은 입력(오입력) 경로에서 결함이 드러났다.

## 다음 한 걸음

사용자에게 롤백 범위를 확인받는다 — Phase 2a(오류 시나리오 보강: 역방향 행 범위,
seatsPerRow 범위, 필수 필드 누락) → Phase 2b → Phase 3(Bean Validation + Seat FK
애노테이션 추가) → Phase 4 재검증.

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
