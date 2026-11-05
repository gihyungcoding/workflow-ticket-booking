---
status: ACTIVE
saved_at: 2026-11-05T02:30:00Z
summary: "SCENARIO_TASK-017 13건(happy 10 · error 2 · regression 1), AC 7/7. 독립검증 대기."

next_steps:
  - "scenario-validator 독립검증"

decisions:
  - decision: "새로 작성"
    rationale: "테마·부품은 새 구현. 옛 테마 테스트(TASK-003)의 기대값은 2b 에서 교체"
  - decision: "서체 로드·375px 가로 넘침은 '(화면 확인)' Then 으로 — jsdom 이 계산하지 않는다"
    rationale: "테스트는 그 상태를 만드는 설정(링크·#root 스타일)을 확인, 실제 결과는 Phase 4 브라우저 증거"
  - decision: "neutral 배지 바탕은 #F1F2F6 (흰 블록 위 --c-fill)"
    rationale: "시안의 배지가 모두 흰 블록·행 위에 놓인다"

attempts: []
---
