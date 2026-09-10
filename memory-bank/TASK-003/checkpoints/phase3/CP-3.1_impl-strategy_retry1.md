---
checkpoint_id: CP-3.1
checkpoint_name: "구현 전략 (retry1 — Phase 4 FAIL 대응)"
task_id: TASK-003
phase: "3"
phase_name: "Phase 3 - Green (재시도)"
saved_at: 2026-09-10T01:00:00Z
status: ACTIVE

work_summary: "VERIFY_TASK-003.json(FAIL)의 code_review.findings 5건을 고치는 전략 수립"

progress:
  completed:
    - "VERIFY_TASK-003.json 로드, 결함 5건 확인"
    - "이전 CP-3.1~3.4를 SUPERSEDED로 변경(파일은 보존)"
  in_progress: "수정 순서: index.html(폰트 굵기) → StatusBadge.tsx(radius) → theme/index.ts(카드 구분선) → tsconfig.app.json(원상복구) → theme/index.test.ts(text.secondary 단언 추가)"
  blocked: []

next_steps:
  - priority: 1
    task: "index.html Google Fonts URL에 Noto Serif KR wght 400;500 추가(또는 Typography에 fontWeight 명시)"
  - priority: 2
    task: "StatusBadge.tsx에 MUI Chip radius 오버라이드 적용(theme 또는 컴포넌트 레벨) — radiusBadge 토큰 사용"
  - priority: 3
    task: "theme/index.ts 또는 PerformanceListPage.tsx의 Card에 colorRule 구분선 적용"
  - priority: 4
    task: "tsconfig.app.json 원상복구(node 타입/include 제거) — index.html.test.ts는 tsc 검사 없이도 vitest로 정상 동작함을 재확인"
  - priority: 5
    task: "theme/index.test.ts에 palette.text.secondary 단언 추가"

decisions:
  - decision: "폰트 굵기 문제는 Google Fonts URL에 400;500을 추가하는 방식으로 해결 — Typography에 fontWeight를 개별 지정하지 않음"
    rationale: "design.md/design-tokens.css 어디에도 굵기 지정이 없다는 것은 '지정하지 않음(=variant 기본값을 그대로 쓴다)'는 의도로 해석하는 것이 자연스럽다. 그렇다면 로드해야 할 것은 실제 사용되는 굵기(400/500)이지, 임의로 fontWeight를 재지정해 원래 없던 값을 새로 만드는 것이 아니다"
    alternatives_considered: ["PerformanceListPage/DetailPage의 Typography에 fontWeight={400}/{500}을 명시 — 이러면 오히려 '어디서도 굵기를 지정하지 않는다'는 원래 상태와 달라져 새로운 결정을 몰래 추가하는 셈이 됨"]
    impact: "index.html의 font URL만 변경, 컴포넌트 코드는 변경 없음"
  - decision: "카드 구분선은 MuiCard variant=outlined + colorRule 보더로 해결"
    rationale: "code-reviewer가 제시한 두 대안(Phase 4 수동 확인 항목화 vs 컴포넌트 수정) 중, design.md §4가 '위계는 선과 여백으로 만든다'를 명시적 결정으로 이미 내려놓았고 --color-rule 토큰까지 준비돼 있어 지금 적용하는 것이 유예할 이유가 없는 간단한 수정"
    alternatives_considered: ["그림자를 완전히 없애지 않고 아주 옅은 그림자 하나만 남김 — design.md '그림자 없음' 결정과 정면으로 배치돼 기각"]
    impact: "PerformanceListPage.tsx의 Card에 variant=\"outlined\" 추가 또는 theme의 MuiCard 기본 옵션 조정"

recovery_prerequisites:
  - CP-4.2
---
