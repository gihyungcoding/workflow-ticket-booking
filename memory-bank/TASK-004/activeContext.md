---
task_id: TASK-004
title: "공연 등록/수정/취소 API"
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
status: ACTIVE
created_at: 2026-09-11
last_updated: 2026-09-11

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-004-performance-registration-api"

last_checkpoint: CP-1.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-004.json"
---

## 지금 무엇을 하고 있나

Phase 1(Plan)을 완료했다. route=Backend, inputs 5 / outputs 9 / flows 9로
acceptance_criteria 9건을 전부 커버했다. 좌석 생성은 ADR-0009(구역×행×열 → 개별
`seat` 행)를 그대로 따르고, Service가 `api.dto` 를 import하지 않도록
`service.SectionSpec` 레코드를 새로 두기로 했다(TASK-001에서 실제 위반 전례 있음).

## 다음 한 걸음

`wf-scenario` 스킬로 Phase 2a를 시작한다 — `PLAN_TASK-004.json` 의 9개 flow(F1~F9)를
Given/When/Then 시나리오로 옮기고, unresolved 항목("행 범위 겹침"을 행 문자 구간
교집합만으로 볼지)을 시나리오에서 명시적으로 확정한다.

## 알아둬야 할 것

- ADR-0009: 좌석은 구역(등급)×행×열 입력으로 서버가 개별 `seat` 행을 생성한다 — 이 태스크의
  핵심 데이터 모델 결정
- 주최자 인증/소유권 확인은 이번 태스크 범위 밖이다 (`performance-registration.md` §7)
- 시각 순서(`openAt <= closeAt <= startAt`)와 좌석 총수(≤5,000) 검증은 DB 제약이 아니라
  서비스 계층 검증이다 (`performance-registration.md` §1-3)
- 기존 `performance` 테이블 스키마는 바뀌지 않는다 — `seat` 테이블만 신설
