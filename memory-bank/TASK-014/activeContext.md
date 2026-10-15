---
task_id: TASK-014
title: "배포용 백엔드 준비 — CORS·쓰기 가드·운영 설정"
phase: "5"
phase_name: "Phase 5 - Reflect"
status: DONE
track: light
created_at: 2026-10-14
last_updated: 2026-10-14

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-014-backend-deploy-cors-guard"

last_checkpoint: CP-5.2
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-014.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-014.md"
---

## 지금 무엇을 하고 있나

시나리오 23건(happy 4/error 6/boundary 12/regression 1)이 HITL#1 승인을 받았다.
독립검증은 3회 모두 FAIL 했으나(RETRY_SCENARIO 상한 소진), 마지막 FAIL 사유
2건(Origin 빈값/공백 SC-23, WRITE_GUARD_ENABLED 대소문자 SC-24)을 직접 보강한
뒤 4번째 자동 재검증 없이 사용자가 직접 승인했다.

## 다음 한 걸음

Phase 4 FAIL(R1) → Phase 2b 롤백. WriteGuardIntegrationTest 를 가드 ON·비트랜잭션으로 재작성(401+ACAO 순서 단언 포함), HITL#2 재승인 후 Phase 3 에서 R3(WARN 로그) 반영, 이어 Phase 4 재검증. R4(Xmx/USER)는 현행 유지 — Phase 5 로.

## 알아둬야 할 것

- CORS는 서블릿 필터 수준(`CorsFilter` 빈, 최우선 순서)에 둬야 한다 — 추후 TASK-009가
  Spring Security를 도입해도 사전 요청(OPTIONS)이 보안 필터에서 거부되지 않게 하기 위함
  (ADR-0016)
- `WRITE_GUARD_ENABLED=true`인데 `WRITE_API_KEY`가 비어 있으면 기동에 실패해야 한다
  (열린 채로 뜨는 사고 방지) — null 과 공백 문자열 둘 다 "비어 있음"으로 처리(SC-07)
- SC-06(쓰기 메서드 8칸 표), SC-07(키 2행 표)은 Red 단계에서 파라미터화 테스트로
  구현하는 게 자연스럽다(JUnit5 `@CsvSource`/`@MethodSource`)
- 검사 가능한 제약 DEPLOY-001(백엔드 주소 하드코딩 금지), DEPLOY-002(CORS 와일드카드
  금지)가 걸려 있다 — Phase 4에서 `check_architecture.py --id DEPLOY-002` 로 확인
- 실제 배포·Neon 연결 확인은 이 태스크 범위 밖(첫 배포 체크리스트에서 별도 확인)

## 알아둬야 할 것

- CORS는 서블릿 필터 수준(`CorsFilter` 빈, 최우선 순서)에 둬야 한다 — 추후 TASK-009가
  Spring Security를 도입해도 사전 요청(OPTIONS)이 보안 필터에서 거부되지 않게 하기 위함
  (ADR-0016)
- `WRITE_GUARD_ENABLED=true`인데 `WRITE_API_KEY`가 비어 있으면 기동에 실패해야 한다
  (열린 채로 뜨는 사고 방지)
- 검사 가능한 제약 DEPLOY-001(백엔드 주소 하드코딩 금지), DEPLOY-002(CORS 와일드카드
  금지)가 걸려 있다 — Phase 4에서 `check_architecture.py --id DEPLOY-002` 로 확인
- 실제 배포·Neon 연결 확인은 이 태스크 범위 밖(첫 배포 체크리스트에서 별도 확인)
