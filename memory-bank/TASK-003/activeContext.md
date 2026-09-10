---
task_id: TASK-003
title: "디자인 토큰·서체 적용"
phase: "3"
phase_name: "Phase 3 - Green (재시도 완료, Phase 4 재진입 대기)"
status: ACTIVE
created_at: 2026-09-09
last_updated: 2026-09-10

primary_category: Frontend
sub_categories: ["audience"]
target_repo: "."
branch: "feature/task-003-design-tokens-typography"

last_checkpoint: CP-3.4 (retry1)
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-003.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-003.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-003.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-003.json"
  test: "workflow_design/05_scenario/TEST_TASK-003.json"
  dev: "workflow_design/06_dev/DEV_TASK-003.json (attempt 2)"
  verify_attempt1: "workflow_design/07_verify/VERIFY_TASK-003.json (attempt 1, FAIL — 재검증 전 기록)"
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

## Phase 3 요약

`theme/index.ts`(createAppTheme), `StatusBadge.tsx`(문구 3개),
`index.html`(lang/title/Google Fonts), `PerformanceListPage.tsx`/
`PerformanceDetailPage.tsx`(공연명 sx), `main.tsx`(theme 배선)를 채워
대상 테스트 11/11을 Green으로 만들었다. 첫 실행에서 3건 실패를 발견해
수정: (1) `index.html.test.ts`의 link 검색 predicate가 preconnect
link까지 잘못 매칭 → `rel="stylesheet"` 조건 추가 (2)(3)
PerformanceListPage/DetailPage.test.tsx의 `renderPage`가 `ThemeProvider`
로 감싸지 않아 본문 서체 검증이 MUI 기본 테마를 보고 있었음 →
`ThemeProvider(theme)` 추가. `check_architecture.py --id DESIGN-001`
위반 0건 확인(AC1). 백엔드 회귀 없음. 리팩토링은 검토 후 하지 않기로
결정(CP-3.3).

## Phase 4 요약 — status: FAIL

실브라우저 확인(모의 서버, 8080 — 이번엔 비어 있어 포트 변경 불필요)으로
lang/title/서체 로드(document.fonts.check)/색상 토큰/그림자 없음/카드
radius를 실측 확인, SC-06(서체 폴백)도 웹폰트 강제 비활성화로 확인 완료.
그런데 **code-reviewer가 실제 결함을 발견해 status: FAIL**:

1. **[correctness]** `index.html`의 Google Fonts URL이 Noto Serif KR을
   `wght@600;700`만 요청하는데, 실제 쓰이는 굵기는 h5=400/h6=500 —
   CSS 폰트 매칭 규칙상 600으로 강제 치환됨. 폰트 로드 성공/실패에 따라
   굵기 위계가 달라지는 실제 버그
2. **[design]** `StatusBadge`가 MUI Chip 기본 pill(16px)을 그대로 써서
   `--radius-badge`(2px) 토큰이 화면에 전혀 도달하지 않음.
   `tokens.ts`의 `radiusBadge`가 어디서도 import 안 됨 — PLAN의
   unresolved/생략 이유 목록에 없던, 인지하지 못하고 놓친 항목
3. **[design]** 그림자를 전부 제거했는데 design.md가 짝으로 요구한
   구분선(`--color-rule`)을 적용하지 않아 목록 카드 경계가 사실상 사라짐
   (실브라우저에서도 직접 확인됨)
4. **[design]** `tsconfig.app.json`에 `types: ["node"]`를 추가한 것이
   target_files 밖 변경이고, 브라우저 번들 코드 전체에 Node 전역 타입을
   노출하는 부작용(scope_deviations 참고)
5. **[test]** `theme.palette.text.secondary` 미검증(사소한 갭)

## Phase 3 재시도(retry1) 완료

5건 모두 수정 완료:
1. `index.html` Google Fonts URL을 `wght@600;700`→`400;500`으로 수정
   (h5=400/h6=500 실사용 굵기와 일치)
2. `theme/index.ts`에 `components.MuiChip.styleOverrides.root.borderRadius
   = radiusBadge` 추가
3. `theme/index.ts`에 `components.MuiCard.defaultProps.variant='outlined'`
   + `MuiPaper.styleOverrides.outlined.borderColor = colorRule` 추가
4. `tsconfig.app.json` 원상복구(`node` 타입/include 제거)
5. `theme/index.test.ts`(SC-01)에 `text.secondary`·배지 radius·카드
   테두리색 단언 추가, `SCENARIO_TASK-003.md/.json`도 동기화

실브라우저로 4건 실측 재확인 완료(chipRadius=2px, cardBorder=
`1px solid #D3D8DC`, titleWeight 500/400, Noto Serif KR 400·500 로드
확인). 테스트 11/11, 빌드/tsc/린트 error 0, 아키텍처 error 0/no_target
없음, 백엔드 회귀 없음.

## 다음 한 걸음

`wf-verify` 스킬로 Phase 4를 재진입한다 — code-reviewer 재호출 포함
전체 절차를 처음부터 다시 수행한다(부분 재검증이 아니라 재실행).

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
