---
checkpoint_id: CP-3.2
checkpoint_name: "테스트 Green 확인"
task_id: TASK-005
phase: "3"
phase_name: "Phase 3 - Green"
saved_at: 2026-09-17T03:00:00Z
status: ACTIVE

work_summary: "performances.ts/PerformanceRegisterPage/PerformanceEditPage 를 구현해 10개 대상 테스트 + 기존 12개 전부 통과"

progress:
  completed:
    - "frontend/src/api/performances.ts: registerPerformance/updatePerformance/cancelPerformance 구현(fetch + 공용 throwIfApiError로 ApiError 변환)"
    - "frontend/src/pages/PerformanceRegisterPage.tsx: 등록 폼 전체 구현"
    - "frontend/src/pages/PerformanceEditPage.tsx: 수정 폼 전체 구현"
    - "frontend/src/utils/datetime.ts 신규(scope_deviation으로 기록) — 두 페이지가 공유하는 datetime-local↔ISO 변환"
    - "npx tsc -b --noEmit 통과, npm test 22/22 통과(대상 10 + 기존 12), npm run lint error 0(warning 3, 기존 관례와 동일 패턴)"
    - "python3 scripts/check_architecture.py 확인 — 새 error 없음, warn만 1건 추가(기존과 동일 성격)"
  in_progress: "-"
  blocked: []

next_steps:
  - priority: 1
    task: "DEV_TASK-005.json 저장, activeContext.md 갱신, 커밋"
  - priority: 2
    task: "wf-verify(Phase 4)로 넘어간다"

decisions:
  - decision: "SEAT_LIMIT_EXCEEDED 오류는 항상 마지막 구역 카드(sections.length-1)에 표시"
    rationale: "시나리오는 구역 1개인 케이스만 다뤄 '해당 구역'이 어느 구역인지 명시하지 않음. 여러 구역일 때 서버가 어느 구역 탓인지 알려주지 않으므로 최소 구현으로 마지막 구역에 표시"
    alternatives_considered: ["모든 구역에 동일 오류 표시", "구역 선택 없이 폼 상단에 표시"]
    impact: "AC4/SC-04(구역 1개) 시나리오는 그대로 통과. 다중 구역 케이스의 UX는 이후 태스크에서 재검토 여지"
  - decision: "새 datetime.ts 유틸 파일 추가 — scope_deviation으로 기록"
    rationale: "PLAN target_files에 없었지만 두 폼 페이지가 동일 변환 로직을 필요로 해 중복을 피하려 분리"
    alternatives_considered: ["각 페이지에 중복 정의"]
    impact: "DEV_TASK-005.json scope_deviations에 근거 남김, Phase 4가 검토"

recovery_prerequisites:
  - CP-2.6

execution_context:
  test_command: "cd frontend && npm test"
  build_command: "cd frontend && npx tsc -b --noEmit"
  env_required: ["PATH에 $HOME/.nvm/versions/node/v22.23.1/bin 추가 필요"]
  main_files:
    - "frontend/src/api/performances.ts"
    - "frontend/src/pages/PerformanceRegisterPage.tsx"
    - "frontend/src/pages/PerformanceEditPage.tsx"
    - "frontend/src/utils/datetime.ts"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/06_dev/DEV_TASK-005.json"
---

## 무엇을 했나

Red 스켈레톤(throw)을 실제 구현으로 채웠다. `performances.ts`의 세 함수는
fetch 호출 후 실패 시 응답 body의 `code`로 `ApiError`를 던지는 공용 헬퍼
(`throwIfApiError`)를 공유한다. 두 폼 페이지는 `PerformanceListPage`/
`PerformanceDetailPage`의 기존 패턴(로딩 상태 유니온, Alert+다시시도 배너,
useEffect fetch)을 그대로 재사용했고, `StatusBadge`도 새로 만들지 않고
그대로 가져다 썼다. 10개 대상 테스트를 한 번에 통과시킨 뒤 전체 스위트를
돌려 기존 12개가 깨지지 않았음을 확인했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/06_dev/DEV_TASK-005.json` | Green 결과 SoT |

## 재개 방법

1. `DEV_TASK-005.json`을 읽는다
2. `wf-verify`(Phase 4)로 넘어간다
