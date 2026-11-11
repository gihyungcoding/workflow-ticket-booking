---
task_id: TASK-019
title: "기준 시각·포스터·구역 위치·머리글 후보 API"
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
status: ACTIVE
track: standard
created_at: 2026-11-11
last_updated: 2026-11-11

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-019-server-time-poster-zones"

last_checkpoint: "CP-2.2"
source: "docs/product/features/booking-ux-02-browse.md (Task A)"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-019.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-019.md"
---

## 지금 무엇을 하고 있나

Phase 1 완료(CP-1.3). Phase 2a 시나리오 설계 진입.

## 다음 한 걸음

`coverage_matrix.py --task-id TASK-019 --skeleton` 으로 82칸 골격을 받아 SCENARIO 를 쓴다.

## 알아둬야 할 것

- 근거: ADR-0024(서버 시각 오프셋·서버 판정), ADR-0026(기한에 재조회 없음), ADR-0005(Clock 주입)
- backend 는 `JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home`
- 등록 API 의 `posterUrl`·`zones` 는 선택 필드. PUT(수정)은 바꾸지 않는다
- 범위 외: 회차(흐름 4), 포스터 수정·업로드, 주최자 폼 변경, 스크랩, 진입 보장(FR-5, 흐름 3)
- 미반영 회고 제안 4건은 대상이 모두 달라(각 1건) 시작 전 경고 대상 아님
