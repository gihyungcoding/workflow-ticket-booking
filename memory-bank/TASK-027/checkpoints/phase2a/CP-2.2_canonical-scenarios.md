---
status: ACTIVE
saved_at: 2026-11-19T15:10:00Z
summary: "시나리오 19건(happy 8 · error 7 · boundary 4) · 입력 경계 칸 90 판정(covered 58 · n/a 32) · AC 10/10 — 독립검증 대기"

next_steps:
  - "scenario-validator 1회차"

decisions:
  - decision: "새로 쓴다 — 메일·토큰 기반은 기존 시나리오가 없다"
    rationale: "TASK-027 은 신규 기반"
  - decision: "기동 시나리오(SC-03~05)는 설정 파일을 읽지 않는 기동으로 값을 넣는다"
    rationale: "프로브(2026-11-19, 커밋 안 함): 테스트 application.properties 값이 SpringApplicationBuilder.properties(map) 를 이기고 --인자 가 파일을 이긴다. 파일에 FRONTEND_BASE_URL·MAIL_TRANSPORT 를 넣으면 '없음' 행을 만들 수 없다. 같은 이유로 승인 테스트 SocialProviderConfigTest·AppleProviderConfigTest 의 값 주입 하네스가 바뀌어야 한다(2b HITL)"
  - decision: "승인 테스트와 정면 충돌 1건 — SocialProviderConfigTest:332(제공자 없으면 FRONTEND_BASE_URL 없이 기동 성공) ↔ SC-05 a"
    rationale: "사용자 결정(항상 필수). 2b HITL 의 「같이 바뀌어야 하는 기존 테스트」로 올린다. 마이그레이션 버전 \"8\" 단언 2곳도 같다"
  - decision: "PLAN unresolved 의 'log 선택 시 기동 WARN' 은 시나리오로 쓰지 않고 구현하지 않는다"
    rationale: "AC 밖이고 '운영' 판별이 없어 모든 로컬 기동에 WARN 이 남는다 — 테스트 없는 동작을 만들지 않는다"
  - decision: "동시 발급(같은 사용자·용도 두 issue 겹침)은 시나리오로 쓰지 않는다"
    rationale: "PLAN unresolved — 수용. 순차 재발급(SC-11)만 단언"
  - decision: "60초 경계는 정확히 60초를 '보내지 않음'(SC-12 d), 시계 역행(SC-12 g)도 보내지 않음"
    rationale: "요구사항 §3-3 SQL '<' 를 그대로 따른다"
  - decision: "PLAN 입력 제약 대조 — 모든 inputs.constraints 가 SC-01~19 의 Then 에 있다(대소문자 무시·앞뒤 공백 = SC-03 f~h, smtp 필수값 = SC-04, URL 규칙 = SC-05, 무효 결과·예외 없음 = SC-08, 정각 만료 = SC-07, 간격 '<' = SC-12, 발송 예외 비전파 = SC-17)"
    rationale: "wf-scenario Step 2 입력 제약 대조"

attempts: []
---

# CP-2.2 시나리오 — TASK-027

- `workflow_design/05_scenario/SCENARIO_TASK-027.md`
