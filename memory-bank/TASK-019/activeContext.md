---
task_id: TASK-019
title: "기준 시각·포스터·구역 위치·머리글 후보 API"
phase: "1"
phase_name: "Phase 1 - Plan"
status: ACTIVE
track: standard
created_at: 2026-11-11
last_updated: 2026-11-11

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-019-server-time-poster-zones"

last_checkpoint: null
source: "docs/product/features/booking-ux-02-browse.md (Task A)"
artifacts: {}
---

## 지금 무엇을 하고 있나

워크플로우 시작. Phase 1 진입 전.

## 다음 한 걸음

`wf-plan` 으로 코드베이스를 조사해 `PLAN_TASK-019.json` 을 만든다. 출발점은 기능 문서 §3·§6.

## 알아둬야 할 것

- 근거: ADR-0024(서버 시각 오프셋·서버 판정), ADR-0026(기한에 재조회 없음), ADR-0005(Clock 주입)
- backend 는 `JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home`
- 등록 API 의 `posterUrl`·`zones` 는 선택 필드. PUT(수정)은 바꾸지 않는다
- 범위 외: 회차(흐름 4), 포스터 수정·업로드, 주최자 폼 변경, 스크랩, 진입 보장(FR-5, 흐름 3)
- 미반영 회고 제안 4건은 대상이 모두 달라(각 1건) 시작 전 경고 대상 아님
