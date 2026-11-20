---
status: ARCHIVED
saved_at: 2026-11-19T15:10:00Z
summary: "시나리오 20건(happy 8 · error 7 · boundary 4 · regression 1) · 입력 경계 칸 90 판정 · AC 10/10 — 독립검증 2회차 PASS(경고 5)"

next_steps:
  - "Phase 2b — wf-red. 승인 테스트 수정 예정: 마이그레이션 버전 8→9 두 곳, SocialProviderConfigTest:332 덩어리, SocialProviderConfigTest·AppleProviderConfigTest 값 주입 하네스(설정 파일을 읽지 않는 기동)"

decisions:
  - decision: "1회차 경고 반영 — V5(마스킹 규약을 첫 1글자 + *** 로 확정, 로그는 OutputCaptureExtension, SC-17 e 를 독립 행으로), V8(인자 검증을 PLAN inputs 에 명시 — 분류 보완, ROLLBACK_PLAN 아님), V10(SC-20 regression — 제공자 설정 기동·소셜 실패 이동), V11 사유 보정(SPRING_MAIL_HOST), V12 ②(SC-12 h·i·j 용도 공유 간격), ④(log WARN 은 두지 않음 — PLAN unresolved 문구 수정), ⑤(스킴 대소문자 허용은 SocialLoginConfig.java:262 의 toLowerCase 비교가 근거)"
    rationale: "Then 을 단언 가능한 값으로 고정하고 기존 동작 회귀를 시나리오로 둔다"
  - decision: "2회차 경고 보류 — V6·V7·V9 는 1회차와 같은 사유, SC-20 의 기동→요청은 한 흐름. V12 ①(SPRING_MAIL_PORT — Spring Boot 바인딩, 앱이 해석하지 않음) ②(스킴 대소문자 — SocialLoginConfig.java:262 toLowerCase 비교) ③(롤백 중 재발급·발급 직후 삭제 — 트랜잭션·CASCADE)"
    rationale: "사람이 2b HITL 에서 판단"
  - decision: "1회차 경고 보류 — V6(SC-06·09 의 두 호출은 '두 번째 호출의 결과'가 관찰 대상이라 한 시나리오), V7(선택된 빈 클래스는 컨텍스트에서 관찰되는 구성 결과 · 예외 클래스는 호출자 계약), V9(경미), V12 ①(SPRING_MAIL_PORT 는 Spring Boot 바인딩에 맡긴다 — 앱이 해석하지 않음), ③(발급 직후 삭제·재발급 중 롤백 — CASCADE·트랜잭션이 보장, TASK-028 에서 호출 경로와 함께)"
    rationale: "사람이 2b HITL 에서 판단"
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

attempts:
  - n: 1
    result: FAIL
    reason: "V11 false_cover 3 — userId@F3·F5 integer.max+1(상한 초과 값 없음), raw_token normalized(정규화 변형 없음)"
    fix: "userId max+1 → n/a(PLAN 에 상한 없음), SC-08 행 q(전각·결합 문자·유사 기호) 추가"
  - n: 2
    result: PASS
    reason: "fail 0 · warn 5(V5·V6·V7·V9·V12)"
    fix: "재검증 없이 V5 ①(SC-20 네 번째 Then 삭제 — 승인 테스트 변경은 2b HITL 에서 다룬다) ②(SC-15 변경 알림 단언 문구 구체화) 반영"
---

# CP-2.2 시나리오 — TASK-027

- `workflow_design/05_scenario/SCENARIO_TASK-027.md`
