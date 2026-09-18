---
checkpoint_id: CP-3.2
checkpoint_name: "테스트 Green 확인"
task_id: TASK-005
phase: "3"
phase_name: "Phase 3 - Green"
saved_at: 2026-09-18T01:00:00Z
status: ARCHIVED

work_summary: "1차 구현 후 Phase 4 FAIL(code-reviewer 결함 5건, 차단 2건) → Phase 3 재작업으로 전부 수정, 23/23 재확인"

progress:
  completed:
    - "[1차] frontend/src/api/performances.ts: registerPerformance/updatePerformance/cancelPerformance 구현(fetch + 공용 throwIfApiError로 ApiError 변환)"
    - "[1차] frontend/src/pages/PerformanceRegisterPage.tsx, PerformanceEditPage.tsx 전체 구현, frontend/src/utils/datetime.ts 신규(scope_deviation)"
    - "[1차] npm test 22/22 통과, Phase 4에서 실물(Docker postgres + backend + frontend) 확인 중 결함 발견 → code-reviewer 호출 → status: FAIL"
    - "[재작업] PerformanceEditPage.tsx save()/confirmCancel() — setPerformance 시 기존 sections 병합 보존(차단 결함 수정)"
    - "[재작업] datetime.ts fromDatetimeLocalInput 빈값 방어 + 모든 필수 TextField에 required 추가(차단 결함 수정 — 브라우저 네이티브 검증으로 빈 제출 차단, read_network_requests로 요청 미발생 실물 확인)"
    - "[재작업] confirmCancel try/catch 추가, SEAT_LIMIT_EXCEEDED를 전체 구역에 표시하도록 변경, seatCountOf 행 입력 검증 추가"
    - "[재작업] PerformanceEditPage.test.tsx: cancelPerformance mock에서 sections 제거(실제 계약 반영), SC-09에 좌석요약 유지 assertion 추가, REGRESSION 테스트 1건 신규(updatePerformance 성공)"
    - "[재작업] getByLabelText 매칭을 정규식으로 수정(required로 인한 MUI asterisk 대응)"
    - "npx tsc -b --noEmit 통과, npm test 23/23 통과, npm run lint error 0(warning 3, 기존 관례와 동일 패턴), npm run build 성공"
    - "실물 재확인: 빈 폼 제출 시 네트워크 요청 미발생, id=4 등록→저장→취소 전 과정에서 좌석 구성 요약 유지(스크린샷 확보)"
  in_progress: "-"
  blocked: []

next_steps:
  - priority: 1
    task: "DEV_TASK-005.json phase4_rollback_fixes 반영 완료, activeContext.md 갱신, 커밋"
  - priority: 2
    task: "wf-verify(Phase 4) 재호출 — VERIFY_TASK-005.json 재판정"

decisions:
  - decision: "SEAT_LIMIT_EXCEEDED 오류를 마지막 구역이 아니라 전체 구역에 표시하도록 변경(1차 결정 번복)"
    rationale: "1차에서는 '마지막 구역에 표시'로 결정했으나 Phase 4 code-reviewer가 다중 구역에서 엉뚱한 구역을 지목하는 결함이라고 지적 — 서버가 원인 구역을 알려주지 않으므로 하나만 지목하는 것보다 전체에 표시하는 편이 사용자를 덜 오도한다"
    alternatives_considered: ["미리보기/총합 영역에 표시(시나리오 변경 필요해 기각)"]
    impact: "PerformanceRegisterPage.tsx 로직 변경, SC-04(구역 1개) 테스트는 변경 없이 통과"
  - decision: "빈 datetime 방어를 이중으로 처리(HTML required + 유틸 함수 방어)"
    rationale: "required만으로는 프로그래밍적 제출을 완전히 막지 못할 수 있고, 유틸 함수 방어만으로는 사용자에게 아무 피드백 없이 서버 400으로 넘어가는 게 최선이 아니다"
    alternatives_considered: ["required만 적용", "유틸 함수 방어만 적용"]
    impact: "테스트의 getByLabelText 매칭을 정규식으로 바꿔야 했음(MUI가 required 필드 라벨에 '*'를 추가)"
  - decision: "code_review의 test-coverage 지적(mock 계약 불일치, 저장 성공 경로 테스트 부재)을 Phase 3에서 직접 반영"
    rationale: "새 시나리오 추가가 아니라 기존 SC-09 mock을 실제 API 계약에 맞게 교정하는 것이고, 결함 재발 방지 회귀 테스트 추가는 버그 수정의 표준 관행(TASK-008 사례와 동일 판단)"
    alternatives_considered: ["mock 수정 없이 코드만 고치고 넘어감"]
    impact: "PerformanceEditPage.test.tsx에 REGRESSION 테스트 1건 추가, 총 테스트 22→23"

recovery_prerequisites:
  - CP-2.6

execution_context:
  test_command: "cd frontend && npm test"
  build_command: "cd frontend && npx tsc -b --noEmit"
  env_required: [
    "PATH에 $HOME/.nvm/versions/node/v22.23.1/bin 추가 필요",
    "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home (실물 확인 시)",
    "docker start ticket-pg (실물 확인 시)"
  ]
  main_files:
    - "frontend/src/api/performances.ts"
    - "frontend/src/pages/PerformanceRegisterPage.tsx"
    - "frontend/src/pages/PerformanceEditPage.tsx"
    - "frontend/src/utils/datetime.ts"
    - "frontend/src/pages/PerformanceEditPage.test.tsx"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/06_dev/DEV_TASK-005.json"
    - path: "workflow_design/07_verify/VERIFY_TASK-005.json"
---

## 무엇을 했나

1차로 Red 스켈레톤을 실제 구현으로 채워 테스트 22/22를 통과시켰으나,
Phase 4에서 Docker로 백엔드까지 실제로 띄워 화면을 확인하던 중 결함을
발견했고 code-reviewer가 이를 포함해 5건(차단 2건)을 찾아 status: FAIL로
판정됐다. Phase 3으로 롤백해 전부 수정했다 — 핵심은 (1) 저장/취소 성공
응답(sections 없음)으로 화면 상태를 통째로 교체하던 것을 기존 값과 병합하는
방식으로 고쳤고, (2) 빈 날짜 필드가 RangeError를 내던 것을 `required` +
유틸 함수 방어로 이중 처리했다. 테스트 mock이 실제 API 계약과 달라 결함을
가렸던 부분도 바로잡고 회귀 테스트를 추가했다. 수정 후 자동화 테스트
23/23, 타입체크/린트/빌드 전부 통과를 확인했고, 실제 브라우저로 재현
시나리오를 다시 밟아 결함이 해소됐음을 확인했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/06_dev/DEV_TASK-005.json` | Green 결과 SoT + `phase4_rollback_fixes` |

## 재개 방법

1. `DEV_TASK-005.json`을 읽는다 (특히 `phase4_rollback_fixes`)
2. `wf-verify`(Phase 4)를 다시 호출해 재판정한다
