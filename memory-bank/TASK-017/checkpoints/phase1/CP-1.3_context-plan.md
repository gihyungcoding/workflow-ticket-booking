---
status: ARCHIVED
saved_at: 2026-11-05T02:00:00Z
summary: "PLAN_TASK-017.json — route Frontend, flows 7건이 AC 7건을 전부 덮음."

next_steps:
  - "Phase 2a — 시나리오 (토큰 대조·배지 5+2 뜻·토스트 타이머·기존 테스트 기대값 교체, 레이아웃·서체는 화면 확인)"

decisions:
  - decision: "토큰은 tokens.ts 미러 + GlobalTokens 로 같은 이름의 CSS 변수 주입, 부품은 out/components.css 와 같은 var() 사용"
    rationale: "시안과 나란히 대조하기 쉽고, 색 리터럴이 theme/ 밖으로 나가지 않는다(DESIGN-002)"
  - decision: "부품 고정 치수·한 열 최대 폭(375px)은 components.css·시안 뷰포트 값을 출처와 함께 tokens.ts 에"
    rationale: "새 값을 만들지 않고 매직 넘버도 남기지 않는다"
  - decision: "AppHeader 교체는 범위 밖(AC 에 없음) — AppBar 부품만"
    rationale: "MUST 6 범위 확장 금지"

attempts:
  - round: 1
    type: ROLLBACK_PLAN
    escalated_from: RETRY_SCENARIO
    reason: "2a 3회 V11 FAIL — prop 스키마 표 부재"
    fix: "design.prop_schema (부품 7종 × prop 경계 규칙)"
---
