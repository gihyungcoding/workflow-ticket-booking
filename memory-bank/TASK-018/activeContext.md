---
task_id: TASK-018
title: "인증 화면 5종을 시안대로"
phase: "4"
phase_name: "Phase 4 - Verify"
status: IN_PROGRESS
track: standard
created_at: 2026-11-06
last_updated: 2026-11-09

primary_category: Frontend
sub_categories: [audience, organizer]
target_repo: "."
branch: "feature/task-018-auth-screens-redesign"

last_checkpoint: "CP-3.4"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-018.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-018.md"
  test: "workflow_design/05_scenario/TEST_TASK-018.json"
  dev: "workflow_design/06_dev/DEV_TASK-018.json"
---

## 지금 무엇을 하고 있나

Phase 3 Green 완료 — 665/665(todo 1), 자체 리뷰 2라운드(RC-1 수정·미해결 0), 승인 테스트 관찰 방법 3곳 수정(사용자 승인).

## 다음 한 걸음

`wf-verify` 로 Phase 4. SC-34: 다섯 화면 + 오류 3종을 같은 출처 375px iframe 으로 띄워 out/screens 와 나란히 캡처.

## 알아둬야 할 것

- **새 입력 경계 행렬의 첫 적용** — PLAN `design.input_fields` 필수(`coverage_matrix.py --kinds`). 회고에서 2a 재시도 횟수를 이전과 비교한다
- node 는 PATH 에 `~/.nvm/versions/node/v22.23.1/bin` 을 붙여서 쓴다
- backend 는 `JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home`
- 문구는 해요체, 시안 문구가 정본 (ADR-0022)
- 범위 외: 아이디·비밀번호 찾기, 홈 화면(흐름 2)
- TASK-017 이월: 앱바 긴 제목 높이 넘침(줄 수 제한) · Sheet `--c-track` · Badge 사용자 문구 말줄임 · 배지/버튼 같은 말 — 이 태스크 화면에 해당하는 것만 Phase 1 에서 판정
