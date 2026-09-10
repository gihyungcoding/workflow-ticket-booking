---
task_id: TASK-003
title: "디자인 토큰·서체 적용"
phase: "2b"
phase_name: "Phase 2b - Red (완료)"
status: ACTIVE
created_at: 2026-09-09
last_updated: 2026-09-10

primary_category: Frontend
sub_categories: ["audience"]
target_repo: "."
branch: "feature/task-003-design-tokens-typography"

last_checkpoint: CP-2.6
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-003.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-003.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-003.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-003.json"
  test: "workflow_design/05_scenario/TEST_TASK-003.json"
---

## 지금 무엇을 하고 있나

Phase 1(Plan)을 마쳤다. route는 Frontend로 확정. inputs 3 / outputs 4 /
flows 7(F1~F7)로 정규화했고 acceptance_criteria 7건을 전부 커버했다.
target_files 7건 중 `frontend/src/theme/tokens.ts`·`index.ts`만 신규이고
나머지 5개는 TASK-002가 만든 기존 파일을 수정한다.

두 가지를 unresolved로 남겼다:
1. `PerformanceListPage.tsx`/`PerformanceDetailPage.tsx`는 tasks.json의
   `implementation_spec.paths`에 없었지만, AC2(공연명 서체)를 충족하려면
   공연명을 렌더하는 이 두 파일에 `fontDisplay` sx 한 줄씩 추가할 수밖에
   없어 target_files에 넣었다.
2. `design-tokens.css`(docs/product/)를 frontend 빌드에 직접 import하지
   않고 `theme/tokens.ts`가 값을 수기로 미러링한다 — 두 파일의 값 일치를
   자동 검증할 수단이 없어 Phase 4에서 사람이 대조해야 한다.

### 배경 (참고)

TASK-002(공연 목록/상세 화면) 완료 이후 별도로 Foundation에 디자인
시스템이 신설됐다 — `docs/product/design.md`(방향·의미 매핑·하지 않을
것), `docs/product/design-tokens.css`(색·타이포·치수 정본),
`constraints.yaml`의 `DESIGN-001~003`(현재 severity: warn — TASK-003이
해소하면 error로 승격 예정).

이 태스크는 그 결정 중 **화면 구조와 무관한 것만** 적용한다: MUI 테마에
토큰 주입(빈 `createTheme()` 해소), 서체 실제 로드(Noto Serif KR/IBM Plex
Sans KR), `index.html`의 `lang`/`title` 수정, 상태 문구를 design.md §5
의미 매핑에 맞춤(예정→예매예정, 마감→예매마감, 취소→공연취소). 상태 배지의
색 구조·행 물러남·스켈레톤 치수는 **포함하지 않는다** — product.md §7에
기록됐다가 제거된 데이터 모델 누락 3건(가격·포스터·공연 기간)이 반영되면
목록 화면이 다시 설계되어 지금 만들면 버려지기 때문(tasks.json
absorbed_steps/description 참고).

## Phase 2a 요약

시나리오 7건(happy 5/error 1/regression 1) 작성, acceptance_criteria 7/7
커버. 독립검증 attempt 1 FAIL(SC-07의 flow가 MD/JSON 불일치) → 수정 →
attempt 2 PASS(경고 5건, 저비용 4건은 승인 전 반영). HITL#1 승인 완료.

**중요**: 이번 태스크에서 실제로 새 vitest 테스트를 작성하는 것은
SC-01~05 5건뿐이다. SC-06(서체 폴백)은 jsdom이 폰트 로딩을 재현할 수
없어 Phase 4 실브라우저 확인으로, SC-07(회귀)은 기존 6개 테스트 재실행
으로 처리한다 — 새 테스트 함수를 억지로 만들지 않는다.

## Phase 2b 요약

SC-01~05를 새 테스트 5건으로 옮겨 전부 Red 확인. tokens.ts는 데이터
상수라 실제 값을 전부 채웠고(구조), theme/index.ts의 `createAppTheme()`
만 `throw new Error('Not implemented')` 스켈레톤으로 남겼다. 신규 파일:
`theme/tokens.ts`, `theme/index.ts`, `theme/index.test.ts`,
`components/StatusBadge.test.tsx`, `index.html.test.ts`. 기존 파일에
테스트 1건씩 추가: `PerformanceListPage.test.tsx`(SC-02),
`PerformanceDetailPage.test.tsx`(SC-03). SC-06/07은 계획대로 새 테스트
없음 — 기존 6건이 무변경으로 계속 통과해 SC-07이 이미 성립함을 확인.
tsconfig.app.json에 `index.html.test.ts`를 include 추가, `types`에
`"node"` 추가(node:fs 등 사용). tsc 오류 0건. HITL#2 승인 완료.

## 다음 한 걸음

`wf-develop` 스킬로 Phase 3(Green)을 시작한다 — SC-01~05를 통과시키는
최소 구현을 `theme/index.ts`·`StatusBadge.tsx`·`index.html`·
`PerformanceListPage.tsx`·`PerformanceDetailPage.tsx`에 채운다. Green
이후 SC-06(서체 폴백)은 Phase 4에서 실브라우저로 별도 확인해야 함을
잊지 않는다.

## 알아둬야 할 것

- **Node.js가 시스템 기본 PATH에 없다** — `nvm use v22.23.1 &&`를 매
  bash 명령 앞에 붙인다 (memory: project-node-nvm-env)
- TASK-002가 남긴 잔여 결함 3건(Roboto 미로드, `lang="en"`, title이
  템플릿 기본값 "frontend")을 이 태스크가 흡수한다 — absorbed_steps 참고
- PLAN의 target_files 7건: `frontend/src/theme/tokens.ts`(신규),
  `frontend/src/theme/index.ts`(신규), `frontend/src/main.tsx`,
  `frontend/src/components/StatusBadge.tsx`, `frontend/index.html`,
  `frontend/src/pages/PerformanceListPage.tsx`,
  `frontend/src/pages/PerformanceDetailPage.tsx`
- acceptance_criteria 7건 중 1건은 `check_architecture.py --id DESIGN-001`
  위반 0건을 요구 — Phase 4에서 반드시 스크립트로 확인
- 색상 리터럴은 `design-tokens.css`(및 그 미러인 `theme/tokens.ts` —
  DESIGN-002 exclude_paths 대상)에만 두어야 한다 — 다른 컴포넌트 코드에
  `#`, `rgb(`, `rgba(` 등을 직접 쓰지 않는다
- 상태 문구 변경(예정→예매예정 등)은 `StatusBadge.tsx`에 영향을 주지만,
  기존 테스트 6건은 '예매가능'·'매진'만 단언해 회귀 위험이 없음을 Phase 1
  에서 grep으로 확인했다(AC7)
- StatusBadge의 색 매핑(STATUS_COLOR)은 이번 태스크에서 건드리지 않는다 —
  색 구조 재설계는 범위 밖(tasks.json 설명)
- 의존 태스크 TASK-002는 DONE·머지됨(PR #3)
