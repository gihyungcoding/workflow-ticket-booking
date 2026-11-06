---
status: ACTIVE
saved_at: 2026-11-06T09:00:00Z
summary: "SCENARIO_TASK-018 34건(happy 14·error 13·boundary 5·regression 2), AC 16/16, 입력 경계 162/162 1차 작성에서 판정. 독립검증 전."

next_steps:
  - "scenario-validator 1회차"

decisions:
  - decision: "새로 쓴다 — TASK-010·013 시나리오는 확장하지 않고, 옛 기대값을 단언하는 기존 테스트는 2b 에서 새 기대값으로 바꾼다"
    rationale: "화면 구조·문구·이동이 시안으로 바뀌어 옛 시나리오의 Then 이 상충한다 (PLAN #1·#2)"
  - decision: "시안에 없는 문구·동작 14건을 「제가 정한 값」 으로 SCENARIO 상단에 둔다 — HITL#1·2 에서 확인"
    rationale: "해요체 빈 칸 오류·대기 정보 실패·만료 경계(now >= expiresAt)·가는 중 만료는 서버 판정·가입 화면 소셜 버튼 제거 등"
  - decision: "feature §2-5 상태 표 대조 — login(기본 SC-01·제출 중 SC-16·자격 증명 SC-02·소셜 실패 SC-07·서버 오류 SC-03), signup(기본 SC-08·칸 오류 SC-09·중복 SC-11·제출 중 SC-16·서버 오류 SC-12), callback(확인 중 SC-18·실패 SC-19), role-select(기본 SC-20·로딩 SC-20·만료 SC-23/24·제출 중 SC-16), joined(기본 SC-31·복사 SC-28·복사 실패 SC-29) — 빠진 상태 없음"
    rationale: "Step 2 상태 표 대조"
  - decision: "PLAN inputs constraints 대조 — 모두 Then 에 단언됨. 단, 긴 이메일·이름·help 의 화면 넘침은 Phase 4 수동 증거(SC-34)"
    rationale: "jsdom 이 레이아웃을 계산하지 않는다"
  - decision: "user_handle·pending_email/name·field_help 의 max+1 은 n/a — PLAN 에 길이 상한 없음"
    rationale: "상한이 없는 값에 상한+1 칸은 의미가 없다"

attempts: []
---
