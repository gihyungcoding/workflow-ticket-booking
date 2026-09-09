---
task_id: TASK-003
title: "디자인 토큰·서체 적용"
phase: "1"
phase_name: "Phase 1 - Plan"
status: ACTIVE
created_at: 2026-09-09
last_updated: 2026-09-09

primary_category: Frontend
sub_categories: ["audience"]
target_repo: "."
branch: "feature/task-003-design-tokens-typography"

last_checkpoint: (없음 — 워크플로우 시작 직후)
artifacts: {}
---

## 지금 무엇을 하고 있나

워크플로우를 막 시작했다. TASK-002(공연 목록/상세 화면) 완료 이후 별도로
Foundation에 디자인 시스템이 신설됐다 — `docs/product/design.md`(방향·의미
매핑·하지 않을 것), `docs/product/design-tokens.css`(색·타이포·치수 정본),
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

프로젝트 골격(백엔드/프론트 테스트) 정상 동작 확인 완료. `wf-plan`
스킬로 Phase 1을 시작한다 — route는 Frontend로 예상되며, Step 1.5에서
`design.md`/`design-tokens.css`를 필수로 참조해야 한다.

## 알아둬야 할 것

- **Node.js가 시스템 기본 PATH에 없다** — `nvm use v22.23.1 &&`를 매
  bash 명령 앞에 붙인다 (memory: project-node-nvm-env)
- TASK-002가 남긴 잔여 결함 3건(Roboto 미로드, `lang="en"`, title이
  템플릿 기본값 "frontend")을 이 태스크가 흡수한다 — absorbed_steps 참고
- `implementation_spec.paths`: `frontend/src/theme/tokens.ts`,
  `frontend/src/theme/index.ts`, `frontend/src/main.tsx`,
  `frontend/src/components/StatusBadge.tsx`, `frontend/index.html`
- acceptance_criteria 7건 중 1건은 `check_architecture.py --id DESIGN-001`
  위반 0건을 요구 — Phase 4에서 반드시 스크립트로 확인
- 색상 리터럴은 `design-tokens.css`에만 두어야 한다(DESIGN-002) — 컴포넌트
  코드에 `#`, `rgb(`, `rgba(` 등을 직접 쓰지 않는다
- 상태 문구 변경(예정→예매예정 등)은 `StatusBadge.tsx`와 기존 테스트
  (`PerformanceListPage.test.tsx`/`PerformanceDetailPage.test.tsx`)의
  문자열 단언에도 영향을 준다 — 기존 테스트 6건이 계속 통과해야 함(AC7)
- 의존 태스크 TASK-002는 DONE·머지됨(PR #3)
