---
checkpoint_id: CP-4.2
checkpoint_name: "검증 완료 — FAIL"
task_id: TASK-005
phase: "4"
phase_name: "Phase 4 - Verify"
saved_at: 2026-09-18T00:00:00Z
status: ACTIVE

work_summary: "테스트/타입체크/린트/빌드 통과, 실제 브라우저+백엔드(Docker Postgres)로 8개 화면 상태를 실물 확인. code-reviewer가 correctness 결함 5건(차단 2건) 발견 — status: FAIL, Phase 3 롤백"

progress:
  completed:
    - "cd frontend && npm test/lint/build, npx tsc -b --noEmit 전부 통과(증거 기록)"
    - "Docker로 기존 postgres 컨테이너(ticket-pg) 기동, JAVA_HOME=JDK21로 backend bootRun, frontend vite dev 기동"
    - "Chrome으로 등록/수정 화면 11개 상태를 실물 확인 — SC-01~09 전부 실제 화면에서 재현·스크린샷 확보"
    - "실물 확인 중 결함 발견: 취소 확인 직후 좌석 구성 요약이 화면에서 사라짐(서버 데이터는 정상, GET 재조회 시 복구)"
    - "code-reviewer 서브에이전트 호출 — 동일 결함이 저장(PUT) 경로에도 있음을 확인(더 치명적), 추가로 빈 datetime 필드 시 RangeError로 잘못된 오류 배너 표시(요청 미전송), confirmCancel 예외 삼킴, SEAT_LIMIT_EXCEEDED 인덱스 하드코딩 등 발견"
    - "check_architecture.py --json: error 0, warn 3(전부 기존 관례), no_target 없음"
    - "VERIFY_TASK-005.json 저장 — status: FAIL"
  in_progress: "-"
  blocked: []

next_steps:
  - priority: 1
    task: "사용자에게 FAIL 결과 보고, Phase 3 롤백 승인 요청(HITL — FAIL은 승인 선택지 없이 롤백만 가능)"
  - priority: 2
    task: "Phase 3에서 5개 결함 수정 후 재차 Green 확인 → Phase 4 재검증"

decisions:
  - decision: "Frontend 태스크의 화면 확인을 위해 Docker로 PostgreSQL을 띄우고 백엔드까지 실제 기동해 E2E로 확인"
    rationale: "컴포넌트 테스트(mock)만으로는 실제 서버 응답 계약과의 불일치(sections 소실 등)를 잡지 못했다 — 실제로 이번에 발견한 핵심 결함이 mock이 실제 계약과 달랐던 것이 원인이었다"
    alternatives_considered: ["컴포넌트 테스트만으로 화면 확인 갈음"]
    impact: "이 판단이 없었다면 sections 소실 버그를 실물에서 놓쳤을 것 — mock이 실제 API 계약과 다르게 작성되어 있었기 때문"

recovery_prerequisites:
  - CP-3.4

execution_context:
  test_command: "cd frontend && npm test"
  build_command: "cd frontend && npm run build"
  env_required: [
    "PATH에 $HOME/.nvm/versions/node/v22.23.1/bin 추가",
    "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home",
    "docker start ticket-pg (postgres, db=ticket user=ticket password=ticket)",
    "backend: SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ticket 등 3개 env 필요(application.properties에 DB 설정 없음)"
  ]
  main_files:
    - "frontend/src/pages/PerformanceEditPage.tsx"
    - "frontend/src/pages/PerformanceRegisterPage.tsx"
    - "frontend/src/utils/datetime.ts"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/07_verify/VERIFY_TASK-005.json"
---

## 무엇을 했나

테스트·타입체크·린트·빌드는 전부 통과했지만, Frontend 태스크의 화면 확인
의무(MUST 3)를 지키기 위해 실제로 Docker postgres + backend bootRun +
frontend vite dev를 전부 띄워 브라우저로 확인했다. 그 과정에서 "취소 확인
직후 좌석 구성 요약이 사라지는" 결함을 직접 목격했고, code-reviewer에게
이를 포함해 전체 diff 리뷰를 맡겼다. 리뷰 결과 같은 결함이 저장(PUT) 경로에도
있다는 것(더 자주 발생), 그리고 빈 날짜 필드 제출 시 RangeError가 발생해
"네트워크 실패"로 잘못 보고되는 별도의 차단급 결함을 추가로 발견했다.
컴포넌트 테스트가 이를 못 잡은 이유는 `PerformanceEditPage.test.tsx`의
`cancelPerformance` mock이 실제로는 없는 `sections` 필드를 포함하는 등
실제 API 계약과 어긋나게 작성됐기 때문이다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/07_verify/VERIFY_TASK-005.json` | 검증 결과 SoT — status: FAIL |

## 재개 방법

1. `VERIFY_TASK-005.json`의 `code_review.findings`를 읽는다
2. 사용자에게 FAIL을 보고하고 Phase 3 롤백 승인을 받는다
3. `wf-develop`으로 돌아가 5개 결함을 수정한다(특히 blocking 2건 우선)
