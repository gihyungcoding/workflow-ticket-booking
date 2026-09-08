---
task_id: TASK-002
title: "공연 목록/상세 화면"
phase: "4"
phase_name: "Phase 4 - Verify (완료)"
status: ACTIVE
created_at: 2026-09-08
last_updated: 2026-09-08

primary_category: Frontend
sub_categories: ["audience"]
target_repo: "."
branch: "feature/task-002-performance-list-detail-ui"

last_checkpoint: CP-4.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-002.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-002.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-002.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-002.json"
  test: "workflow_design/05_scenario/TEST_TASK-002.json"
  dev: "workflow_design/06_dev/DEV_TASK-002.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-002.json (WARN, EXCEPTION_APPROVE)"
---

## 지금 무엇을 하고 있나

Phase 4(Verify)를 마쳤다. acceptance_criteria 6/6을 단위 테스트 + 실제
브라우저 확인(이중 증거)으로 PASS 판정했다. 로컬에 PostgreSQL/Docker가
없고 8080 포트는 사용자의 무관한 다른 프로젝트(IntelliJ 디버그 세션)가
점유 중이라, TASK-001 실제 백엔드 대신 API 명세(§2)와 동일한 스키마의
임시 모의 서버(scratchpad, 8090)로 목록 4상태·상세 2상태를 전부 화면에서
직접 확인했다. `vite.config.ts` proxy는 검증 동안만 8090으로 바꿨다가
`git checkout`으로 정확히 원복(diff 0)했다.

`code-reviewer` 서브에이전트가 소견 7건을 냈다 — 전부 승인된 SC-01~06
범위 밖 엣지 케이스(비숫자 id, PERFORMANCE_NOT_FOUND 외 404 소스, 도달
불가능한 상세→상세 경쟁 조건 등) 또는 테스트 커버리지 갭(SC-01 날짜
미검증)이었다. 별도로 카드→상세 네비게이션 링크가 Plan(F1~F7)에 없었던
것을 발견했다 — 이 링크 없이는 상세 화면에 도달할 UI 경로가 없어 기능이
무의미해지므로 Phase 1의 누락으로 판단, 제거하지 않고 사후 기록했다
(`DEV_TASK-002.json.scope_deviations`는 비어 있었음 — 감사 추적 보존을
위해 소급 수정하지 않고 `VERIFY_TASK-002.json.scope_deviations`에 기록).

`status: WARN`으로 HITL#3에 올렸고 사용자가 EXCEPTION_APPROVE했다.
`verified_commit = e2fed22d0897ecbd96c5d68ca1cbc9ebac97b0a9`.

## 다음 한 걸음

`wf-reflect` 스킬로 Phase 5(회고)를 시작한다 — KPT 회고를 하고 규칙
개선안을 도출한다. 회고에 반드시 반영해야 할 것: (1) code_review의
correctness/design 소견 6건(위 exceptions 참고), (2) Plan이 화면 간
네비게이션 흐름을 놓치기 쉽다는 프로세스 개선안, (3) SC-01 테스트
커버리지 갭.

## 알아둬야 할 것

- **Node.js가 시스템 기본 PATH에 없다** — 매 bash 명령 앞에 `nvm use v22.23.1 &&`
  를 붙여야 한다(TASK-001의 JAVA_HOME 이슈와 같은 종류의 환경 특이사항).
  테스트: `nvm use v22.23.1 && npm --prefix frontend test`, 빌드:
  `nvm use v22.23.1 && npm --prefix frontend run build` (memory:
  project-node-nvm-env)
- Phase 1에서 결정한 두 가지가 여전히 유효: (1) 필터 UI는 공식 AC 밖이라
  구현하지 않음, (2) 프론트-백엔드 연결은 Vite dev proxy로 해결
  (`vite.config.ts` server.proxy, 8080 고정)
- 설치된 스택 버전: MUI 9.4.0(Stack의 justifyContent/alignItems는 sx로),
  React 19.2.8, Vite 8.2.2, TypeScript 6.0.2, react-router-dom 7.18.3,
  vitest 5.0.0, @testing-library/jest-dom 7.0.1(vitest 서브패스 필요)
- 이번 태스크에서는 `frontend/`에 아키텍처 제약(`constraints.yaml`)이
  아직 없다 — Phase 4의 `check_architecture.py`는 백엔드 제약 3건만 검사함
- **8080 포트는 로컬에서 사용자의 다른(무관한) 프로젝트가 점유할 수 있다**
  — 이 저장소의 백엔드를 로컬에서 띄워야 할 일이 있으면 `lsof -nP -iTCP:8080`
  으로 먼저 확인할 것
- 의존 태스크 TASK-001은 DONE — API 는 `GET /api/performances`,
  `GET /api/performances/{id}` (응답 필드: id/title/venue/startAt/openAt/
  closeAt/totalSeats/availableSeats/status)
- 좌석 선택 UI·결제 진입은 범위 밖
- **승인 이후 코드를 고치지 않는다** — `verified_commit` 이후 소스가
  바뀌면 `/wf-ship`이 재검증을 요구한다
