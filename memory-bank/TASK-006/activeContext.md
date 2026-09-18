---
task_id: TASK-006
title: "구역 price/seatsPerRow 정수 강제 검증"
phase: "0"
phase_name: "시작 - Phase 1 진입 대기"
status: ACTIVE
created_at: 2026-09-18
last_updated: 2026-09-18

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-006-section-numeric-validation"

last_checkpoint: null
artifacts: {}
---

## 지금 무엇을 하고 있나

태스크를 시작했다. memory-bank 구조와 브랜치(`develop`에서 분기)를 만들었다.
아직 Phase 1(Plan)을 시작하지 않았다.

## 다음 한 걸음

`wf-plan` 스킬로 넘겨 Phase 1을 진행한다.

## 알아둬야 할 것

- 원인은 TASK-004 Phase 4 검증(attempt 3)에서 code-reviewer가 이미 재현·확인함
  (Jackson `ACCEPT_FLOAT_AS_INT` 기본 설정으로 소수가 0 방향 절삭됨). 재조사 불필요
  (`absorbed_steps` 참고)
- price=-0.5 → 0으로 절삭되어 `PerformanceService.validateSection` 의 "price는 0 이상"
  검사를 우회, 무료 좌석 생성
- seatsPerRow=1.9 → 1로 절삭되어 클라이언트 좌석수 미리보기와 서버 실제 생성 좌석수가 어긋남
- 대상 파일: `backend/src/main/java/com/example/ticket_booking/api/dto/SectionRequest.java`,
  `backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java`
- AC 3건: 비정수 price → 400/INVALID_SECTION·좌석 미생성 / 비정수 seatsPerRow →
  400/INVALID_SECTION·좌석 미생성 / 기존 정수 요청은 그대로 201 성공(회귀)
- 위험도 low, 규모 6h(정상 WRU 범위)
- 결제 기능이 아직 없어 크래시·DoS는 아니라는 이유로 TASK-004에서는 WARN 예외 승인하고
  이 태스크로 분리됨 — 이번 태스크에서는 예외 없이 정식으로 막아야 함
