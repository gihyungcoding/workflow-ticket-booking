---
task_id: TASK-027
title: "메일 발송 기반·일회용 이메일 토큰"
phase: "2a"
phase_name: "Phase 2a - 시나리오 독립검증 통과"
status: IN_PROGRESS
track: standard
created_at: 2026-11-19
last_updated: 2026-11-19

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-027-mail-email-token"

last_checkpoint: "checkpoints/phase2a/CP-2.2_canonical-scenarios.md"
source: "docs/product/features/email-verification-password-reset.md (Task A)"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-027.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-027.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-027.json"
---

## 지금 무엇을 하고 있나

Phase 2a 완료 — 시나리오 20건, 칸 90, 독립검증 2회차 PASS.

## 다음 한 걸음

Phase 2b — `wf-red`. Red 테스트 + 승인 테스트 수정 3묶음을 HITL#1·2 에 함께.

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
