---
task_id: TASK-003
title: "디자인 토큰·서체 적용"
phase: "1"
phase_name: "Phase 1 - Plan (완료)"
status: ACTIVE
created_at: 2026-09-09
last_updated: 2026-09-09

primary_category: Frontend
sub_categories: ["audience"]
target_repo: "."
branch: "feature/task-003-design-tokens-typography"

last_checkpoint: CP-1.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-003.json"
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

## 다음 한 걸음

`wf-scenario` 스킬로 Phase 2a를 시작한다 — F1~F7을 Given/When/Then
시나리오로 옮기고 `scenario-validator` 서브에이전트 검증을 거쳐 HITL#1
승인을 받는다.

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
