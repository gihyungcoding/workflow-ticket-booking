---
task_id: TASK-014
title: "배포용 백엔드 준비 — CORS·쓰기 가드·운영 설정"
phase: "1"
phase_name: "Phase 1 - Plan"
status: ACTIVE
track: light
created_at: 2026-10-14
last_updated: 2026-10-14

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-014-backend-deploy-cors-guard"

last_checkpoint: null
artifacts: {}
---

## 지금 무엇을 하고 있나

TASK-014를 막 시작했다. ADR-0016(프론트 분리 배포 + CORS + 임시 쓰기 가드)에 따라
Phase 1(Plan)에 들어갈 준비를 마쳤다.

## 다음 한 걸음

`wf-plan` 스킬로 코드베이스를 조사해 PLAN_TASK-014.json 을 만든다. ADR-0016의
"세부" 표(CORS 허용 헤더, 쓰기 가드 활성화 조건, 서버 포트/DB 접속 환경변수)를
구조 정보로 그대로 옮긴다.

## 알아둬야 할 것

- CORS는 서블릿 필터 수준(`CorsFilter` 빈, 최우선 순서)에 둬야 한다 — 추후 TASK-009가
  Spring Security를 도입해도 사전 요청(OPTIONS)이 보안 필터에서 거부되지 않게 하기 위함
  (ADR-0016)
- `WRITE_GUARD_ENABLED=true`인데 `WRITE_API_KEY`가 비어 있으면 기동에 실패해야 한다
  (열린 채로 뜨는 사고 방지)
- 검사 가능한 제약 DEPLOY-001(백엔드 주소 하드코딩 금지), DEPLOY-002(CORS 와일드카드
  금지)가 걸려 있다 — Phase 4에서 `check_architecture.py --id DEPLOY-002` 로 확인
- 실제 배포·Neon 연결 확인은 이 태스크 범위 밖(첫 배포 체크리스트에서 별도 확인)
