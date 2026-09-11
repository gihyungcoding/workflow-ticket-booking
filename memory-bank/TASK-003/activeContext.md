---
task_id: TASK-003
title: "디자인 토큰·서체 적용"
phase: "5"
phase_name: "Phase 5 - Reflect (완료)"
status: DONE
created_at: 2026-09-09
last_updated: 2026-09-10

primary_category: Frontend
sub_categories: ["audience"]
target_repo: "."
branch: "feature/task-003-design-tokens-typography"

last_checkpoint: CP-5.3
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-003.json"
  scenario_md: "workflow_design/05_scenario/SCENARIO_TASK-003.md"
  scenario_json: "workflow_design/05_scenario/SCENARIO_TASK-003.json"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-003.json"
  test: "workflow_design/05_scenario/TEST_TASK-003.json"
  dev: "workflow_design/06_dev/DEV_TASK-003.json (attempt 4, 최종)"
  verify: "workflow_design/07_verify/VERIFY_TASK-003.json (attempt 2, PASS, APPROVE)"
  reflect: "workflow_design/08_reflect/REFLECT_TASK-003.json"
  report: "https://claude.ai/code/artifact/ffb4a0e8-5ea3-4c90-873c-cdd5833a2b83"

pull_request:
  number: 4
  url: "https://github.com/gihyungcoding/workflow-ticket-booking/pull/4"
  base: develop
  opened_at: 2026-09-10
---

## 지금 무엇을 하고 있나

**완료됐다.** Phase 1~5를 전부 마쳤다. `docs/product/design.md`가 정한
디자인 토큰(색·서체·치수)을 MUI 테마에 배선하고, 서체를 실제로 로드하고,
상태 문구를 의미 매핑에 맞췄다.

**Phase 4에서 3라운드 재시도가 발생한 것이 이 태스크의 핵심 서사다.**
code-reviewer 리뷰를 3번 거치며 매번 "토큰 값은 theme에 있으나 MUI 내부
하드코딩·오탈자에 가려 화면에 실제로 반영되지 않는다"는 같은 패턴의
결함을 하나씩 발견·수정했다:

1. Google Fonts가 실사용 굵기(h5=400/h6=500)와 다른 굵기(600;700)를
   요청해 CSS 매칭 규칙상 강제 치환됨
2. `StatusBadge`가 MUI Chip 기본 pill(16px)을 그대로 써서
   `--radius-badge`(2px)가 미도달
3. 그림자 제거 후 구분선(`--color-rule`)을 적용하지 않아 카드 경계 소실
4. `lineHeightDisplay`/`lineHeightText` 토큰이 배선되지 않음
5. `color="text.secondary"`가 MUI v9에서 무효한 점 표기라
   `--color-ink-muted`가 전혀 렌더되지 않음

전부 실측(단위 테스트 + 실브라우저)으로 해소를 확인했다. 상세 경위는
`memory-bank/TASK-003/checkpoints/phase3/`·`phase4/`의 각 CP 파일과
[회고 리포트](https://claude.ai/code/artifact/ffb4a0e8-5ea3-4c90-873c-cdd5833a2b83)
에 있다.

태스크는 DONE으로 닫혔고 `/wf-ship`으로 PR #4가 생성됐다 — 머지는 사람이 한다.

## 다음 한 걸음

리뷰 후 PR #4(https://github.com/gihyungcoding/workflow-ticket-booking/pull/4)를
사람이 직접 머지한다. `verified_commit = 08f0a35feceafd2a0629ae9979fbd51c023849d7`.

## 알아둬야 할 것

- **Node.js가 시스템 기본 PATH에 없다** — `nvm use v22.23.1 &&`를 매
  bash 명령 앞에 붙인다 (memory: project-node-nvm-env)
- StatusBadge의 색 매핑(STATUS_COLOR)은 design.md §2("유일한 채도는
  예매가능")와 여전히 불일치한다 — **의도적으로 범위 밖**(product.md §7
  데이터 모델 누락 3건이 반영되면 목록이 재설계되므로 지금 손대면 버려짐).
  후속 태스크 후보
- `theme/tokens.ts`는 `design-tokens.css`를 수기로 미러링한다 — 값을
  고치면 두 파일을 함께 고쳐야 한다(자동 동기화 없음)
- 아키텍처 제약 `DESIGN-001`(빈 테마 금지)이 이 태스크로 해소됨 — 사용자가
  severity를 `warn`→`error`로 승격할지는 아직 미결정
- 회고에서 도출된 규칙 개선안 3건(테마 Then은 렌더 결과 대조 원칙,
  외부 리소스 파라미터는 실사용값 우선 확인, code-reviewer 재호출 시 같은
  패턴 전수 조사)은 사용자가 별도로 승인하지 않는 한 아직 미반영 상태
- 의존 태스크 TASK-002는 DONE·머지됨(PR #3)
