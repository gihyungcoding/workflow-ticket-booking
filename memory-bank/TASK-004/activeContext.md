---
task_id: TASK-004
title: "공연 등록/수정/취소 API"
phase: "1"
phase_name: "Phase 1 - Plan"
status: ACTIVE
created_at: 2026-09-11
last_updated: 2026-09-11

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-004-performance-registration-api"

last_checkpoint: null
artifacts: {}
---

## 지금 무엇을 하고 있나

태스크를 막 시작했다. `docs/product/features/performance-registration.md` (§1-2 Task A)
에서 추출된 태스크이며, `workflow_design/02_tasks/tasks.json` 의 TASK-004 정의를 그대로
따른다. `feature/task-004-performance-registration-api` 브랜치를 `develop` 에서
새로 갈라냈다.

## 다음 한 걸음

`wf-plan` 스킬로 Phase 1을 시작한다 — 요구사항 문서(§1-3, §3, §4, §6)와 기존
`performance` 관련 코드(`PerformanceService`/`PerformanceController`/`PerformanceRepository`)
를 조사해 `PLAN_TASK-004.json` 을 만든다.

## 알아둬야 할 것

- ADR-0009: 좌석은 구역(등급)×행×열 입력으로 서버가 개별 `seat` 행을 생성한다 — 이 태스크의
  핵심 데이터 모델 결정
- 주최자 인증/소유권 확인은 이번 태스크 범위 밖이다 (`performance-registration.md` §7)
- 시각 순서(`openAt <= closeAt <= startAt`)와 좌석 총수(≤5,000) 검증은 DB 제약이 아니라
  서비스 계층 검증이다 (`performance-registration.md` §1-3)
- 기존 `performance` 테이블 스키마는 바뀌지 않는다 — `seat` 테이블만 신설
