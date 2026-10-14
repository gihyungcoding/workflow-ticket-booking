---
task_id: TASK-014
title: "배포용 백엔드 준비 — CORS·쓰기 가드·운영 설정"
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
status: ACTIVE
track: light
created_at: 2026-10-14
last_updated: 2026-10-14

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-014-backend-deploy-cors-guard"

last_checkpoint: CP-1.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-014.json"
---

## 지금 무엇을 하고 있나

Phase 1(Plan) 완료. route=Backend, flows 8건이 acceptance_criteria 9건을 모두
덮는다. 새 컴포넌트(CorsConfig, WriteGuardFilter)는 architecture.md 의 계층표에는
없지만 ADR-0016이 "서블릿 필터 수준"으로 이미 배치를 정해뒀다.

## 다음 한 걸음

`wf-scenario` 스킬로 Given/When/Then 시나리오를 설계한다.

## 알아둬야 할 것

- CORS는 서블릿 필터 수준(`CorsFilter` 빈, 최우선 순서)에 둬야 한다 — 추후 TASK-009가
  Spring Security를 도입해도 사전 요청(OPTIONS)이 보안 필터에서 거부되지 않게 하기 위함
  (ADR-0016)
- `WRITE_GUARD_ENABLED=true`인데 `WRITE_API_KEY`가 비어 있으면 기동에 실패해야 한다
  (열린 채로 뜨는 사고 방지)
- 검사 가능한 제약 DEPLOY-001(백엔드 주소 하드코딩 금지), DEPLOY-002(CORS 와일드카드
  금지)가 걸려 있다 — Phase 4에서 `check_architecture.py --id DEPLOY-002` 로 확인
- 실제 배포·Neon 연결 확인은 이 태스크 범위 밖(첫 배포 체크리스트에서 별도 확인)
