---
task_id: TASK-027
title: "메일 발송 기반·일회용 이메일 토큰"
phase: "4"
phase_name: "Phase 4 - 예외 승인"
status: IN_PROGRESS
track: standard
created_at: 2026-11-19
last_updated: 2026-11-19

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-027-mail-email-token"

last_checkpoint: "checkpoints/phase4/CP-4.2_verification.md"
source: "docs/product/features/email-verification-password-reset.md (Task A)"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-027.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-027.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-027.json"
  test: "workflow_design/05_scenario/TEST_TASK-027.json"
  dev: "workflow_design/06_dev/DEV_TASK-027.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-027.json"
---

## 지금 무엇을 하고 있나

Phase 3 1회차 Green(225 통과, 6017bb5) 뒤 자체 리뷰 결함으로 Phase 2a 롤백(RETRY_SCENARIO 1회) — SC-21~24 추가 중.

## 다음 한 걸음

독립검증 3회차 → 2b(Red·HITL) → Phase 3 2회차.

## 알아둬야 할 것

- 근거: 요구사항 `docs/product/features/email-verification-password-reset.md` Task A. ADR-0031(메일 발송), ADR-0032(토큰 설계)
- 포함하지 않음: 엔드포인트, 가입·로그인 변경(TASK-028), 비밀번호 재설정 API(TASK-029), 프론트
- 마이그레이션 다음 번호는 V9(현재 V8 까지). 새 FK 자식 테이블(`email_token` → `users`, ON DELETE CASCADE) — 부모 행을 지우는 테스트 정리와 최신 스키마 버전을 단언하는 테스트를 Phase 1 에서 Grep
- 완료 조건의 「한 번만」「동시 요청에도 한 통」「DB 변경 없음」은 클래스 레벨 `@Transactional` 없이 새로 읽어 확인(요구사항 §5, CLAUDE.md JPA 리뷰 지시). 시각은 `Clock` 고정
- 운영 DB 는 PostgreSQL — 조건부 UPDATE·TIMESTAMPTZ 는 같은 엔진 통합 테스트(ADR-0006)
- 열린 질문(§7): 발송 서비스·발신 도메인은 배포 설정 — Phase 1 에서 정한다
- backend 명령은 JDK 21 JAVA_HOME 후 실행. 시작 시점 `./gradlew test` 통과
- 시작 시 미반영 회고 개선안: coverage_matrix.py ×2(TASK-020·022 — timeout·instant·부호·선행 0·비ASCII 숫자), design-checks.md ×2 — 사용자 「그대로 시작」. 만료 시각·간격은 input_fields 에 수동으로 반영
- 선행 TASK-025 는 PR #56 으로 develop 에 머지됨(fd4e27b)
- track standard(견적 12h)
- **사용자 결정 (2026-11-19, Phase 1)**: ① MAIL_TRANSPORT·FRONTEND_BASE_URL 항상 필수(프로필 없음) ② 커밋 뒤 비동기 발송 기반(AccountMailService)을 이 태스크에서 만든다
- 승인 테스트 수정 예정(2b HITL): 마이그레이션 버전 "8"→"9" 2곳, SocialProviderConfigTest:332(제공자 없으면 FRONTEND_BASE_URL 없이 기동 성공 → 실패)
- **Red 승인 (2026-11-20)**: 승인 테스트 4파일 변경 포함(마이그레이션 버전 8→9 두 곳, Social·Apple 기동 하네스 BareBoot). PostgreSQL 테스트는 Docker Desktop 필요
- **RETRY_SCENARIO 1회 (2026-11-20, 사용자 결정)**: 자체 리뷰 RC-1(clearAutomatically)·RC-2(SMTP 타임아웃·대기열·종료)·RC-4(BareBoot OS 환경변수). RC-3 은 수용 위험(issue 호출 규칙 javadoc)
- **RETRY_SCENARIO 2회 (2026-11-20, 사용자 결정)**: 자체 리뷰 2R — smtps 타임아웃 무시(재현)·JNDI 세션·종료 인터럽트. 상한 3회 중 2회 사용
- **Phase 3 완료 (2026-11-20)**: 232 통과. 수용 위험 2 — RC-3(issue 직접 동시 호출), 대괄호 표기 타임아웃(체크리스트 '환경변수로만')
- **Phase 4 1회차 FAIL → RETRY_SCENARIO 3회 (2026-11-20, 상한)**: SPRING_MAIL_HOST 앞뒤 공백. 다시 FAIL 이면 ROLLBACK_PLAN
- **HITL#3 예외 승인 (2026-11-20)**: verified_commit da2f1b1. 후속 — TASK-028 에서 체크리스트 STARTTLS 문구 정정, ADR-0035 다시 볼 조건(issue 직접 호출자) 확인
