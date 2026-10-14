---
task_id: TASK-014
title: "배포용 백엔드 준비 — CORS·쓰기 가드·운영 설정"
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
status: BLOCKED
track: light
created_at: 2026-10-14
last_updated: 2026-10-14

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-014-backend-deploy-cors-guard"

last_checkpoint: CP-2.2
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-014.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-014.md"

blocked_reason: "scenario-validator 3회 시도(RETRY_SCENARIO 상한) 모두 FAIL — 입력 경계값 누락이 반복됨. reject-state-machine.md §3 에 따라 ROLLBACK_PLAN 을 사용자에게 제안해야 하는 상태"
blocked_since: 2026-10-14
unblock_condition: "사용자가 ROLLBACK_PLAN(태스크 분할)과 시나리오 2건 보강 후 수동 승인 중 하나를 선택"
---

## 지금 무엇을 하고 있나

시나리오 3차 작성·독립검증 3회(모두 FAIL) 끝. 남은 FAIL 2건은 좁지만
(Origin 빈값/공백, WRITE_GUARD_ENABLED 대소문자), 매 라운드 독립검증자가 "전체
21건이 coverage-policy §4 복잡도 상한(8~12건)을 크게 넘는다"는 경고(V9)를 반복해,
이 태스크 자체가 WRU 하나보다 크다는 신호로 본다 — CORS·쓰기가드·설정외부화·
Dockerfile 네 concern 이 한 태스크에 묶여 있다.

## 다음 한 걸음

사용자에게 ROLLBACK_PLAN(Phase 1 로 돌아가 태스크를 분할) 과 "시나리오 2건만
더 보강하고 사람이 직접 승인" 중 선택받는다. 자동 재검증은 더 돌리지 않는다
(RETRY_SCENARIO 상한 3회 소진).

## 알아둬야 할 것

- CORS는 서블릿 필터 수준(`CorsFilter` 빈, 최우선 순서)에 둬야 한다 — 추후 TASK-009가
  Spring Security를 도입해도 사전 요청(OPTIONS)이 보안 필터에서 거부되지 않게 하기 위함
  (ADR-0016)
- `WRITE_GUARD_ENABLED=true`인데 `WRITE_API_KEY`가 비어 있으면 기동에 실패해야 한다
  (열린 채로 뜨는 사고 방지)
- 검사 가능한 제약 DEPLOY-001(백엔드 주소 하드코딩 금지), DEPLOY-002(CORS 와일드카드
  금지)가 걸려 있다 — Phase 4에서 `check_architecture.py --id DEPLOY-002` 로 확인
- 실제 배포·Neon 연결 확인은 이 태스크 범위 밖(첫 배포 체크리스트에서 별도 확인)
