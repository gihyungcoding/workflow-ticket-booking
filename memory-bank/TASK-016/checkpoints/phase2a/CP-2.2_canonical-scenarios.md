---
status: ACTIVE
saved_at: 2026-11-04T14:00:00Z
summary: "SCENARIO_TASK-016 작성 — 11건(happy 7 · error 3 · regression 1), AC 7건 전부 매핑. 독립검증 대기."

next_steps:
  - "scenario-validator 독립검증"

decisions:
  - decision: "새로 작성 (기존 시나리오 확장 아님)"
    rationale: "아이디·대기 정보 조회는 새 기능. pendingToken 거부 표는 TASK-011 SC-10 의 행 구성을 따랐다"
  - decision: "난수원을 테스트가 고정하는 「뽑힌 값」 표기로 시나리오를 결정적으로 만든다"
    rationale: "무작위 생성의 재시도·10회 실패·이메일 무관성을 관찰 가능한 값으로 단언하기 위해"
  - decision: "동시 가입 아이디 경합(UNIQUE 위반 오보 방지)은 시나리오로 쓰지 않고 Phase 4 코드 리뷰로"
    rationale: "결정적 재현이 어렵다. AC3 은 존재 확인 재시도로 덮는다"
  - decision: "feature 문서 §2-5 상태 표는 화면(TASK-018) 소관 — 이 태스크 시나리오와 대조하지 않는다"
    rationale: "route Backend"

attempts: []
---
