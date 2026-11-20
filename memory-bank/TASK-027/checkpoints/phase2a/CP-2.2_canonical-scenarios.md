---
status: ARCHIVED
saved_at: 2026-11-19T15:10:00Z
summary: "RETRY_SCENARIO 3회차 — 시나리오 27건, 칸 122, 독립검증 5회차 PASS(경고 6, V12 ①②③·V4 반영)"

next_steps:
  - "Phase 2b — SC-27 Red, HITL#1·2 재승인"

decisions:
  - decision: "RETRY_SCENARIO 1회차 — 자체 리뷰 RC-1·RC-2·RC-4 의 변종 전부를 한 번에 시나리오로(SC-21 a~d, SC-22 a·b, SC-23, SC-24, 머리말 기동)"
    rationale: "wf-develop Step 5.5 — 시나리오 밖 [correctness] 는 Phase 4 로 넘기지 않는다. 사용자 결정"
  - decision: "SC-21 은 User 에 변경 메서드가 없어 공연(changePublished)으로 관찰한다"
    rationale: "같은 영속성 컨텍스트의 어느 엔티티든 clear 의 영향은 같다 — RC-1 변종 3(다른 엔티티)을 그대로 덮는다"
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
  - n: 8
    result: PASS
    reason: "5회차 fail 0 · warn 6"
    fix: "재검증 없이 V12 ①(MAIL_FROM 안쪽 공백·CR·LF 기동 실패 — SC-27 j·k, PLAN inputs), ②(개행만 호스트 — SC-27 i), ③·V4(PLAN input_fields note·F2 step). 보류: V5·V6·V7·V9, V12 ④⑤⑥ — 이전 회차와 같은 사유"
  - n: 7
    result: "RETRY_SCENARIO 3회차 (Phase 4 → 2a, 상한)"
    outcome: "Phase 4 1회차 FAIL — RC-2 later: SPRING_MAIL_HOST 앞뒤 공백(재현). 사용자 결정 「시나리오로 롤백」"
    back_to: "Phase 2a"
    fix: "SC-27(호스트·보내는 사람 정리 8행), SPRING_MAIL_HOST·MAIL_FROM 표기 칸 n/a → covered, PLAN inputs 제약 3줄"
  - n: 6
    result: PASS
    reason: "4회차 fail 0 · warn 5"
    fix: "재검증 없이 V12 ①(log 이면 프로토콜·타임아웃 무시 — SC-25 p·q, JNDI 는 전송 무관 실패 — r), ②(타임아웃 ASCII 숫자·int 상한 — SC-22 k·l·m), ③(smtps 이면 mail.smtp.* 무시 — SC-25 o), V4(PLAN F2 step), V11·V1 참고(PLAN note·머리말 DB 목록). 보류: V5 ②(블랙홀은 읽기 타임아웃만 관찰 — 연결·쓰기 기본은 코드 리뷰로), ③(JNDI 메시지는 Boot 오류가 먼저일 수 있어 'jndi'), ④(로그 문구), V6·V7·V9, V12 ④⑤⑥"
  - n: 5
    result: "RETRY_SCENARIO 2회차 (Phase 3 → 2a)"
    outcome: "자체 리뷰 2라운드 — RC-2 변종 later 3(smtps 타임아웃 무시 재현, JNDI 세션, 종료 인터럽트) 시나리오 밖. 사용자 결정 「2a 로 한 번 더」"
    back_to: "Phase 2a"
    fix: "SC-25(프로토콜 smtp|smtps·JNDI 14행), SC-26(종료 대기 인터럽트). PLAN inputs 2·input_fields SMTP_PROTOCOL·MAIL_JNDI_NAME 추가"
  - n: 4
    result: PASS
    reason: "3회차 fail 0 · warn 6"
    fix: "재검증 없이 V12 ①~③(타임아웃 0·음수·숫자 아님 → 기동 실패, 빈 값·공백·키 없음 → 키별 10초 기본, PLAN inputs 수정, SC-22 행 c~j), V5(SC-24 의 멈춤 수단·신호 = @PreDestroy 직접 호출과 그 반환) 반영. 보류: V6·V7·V9·V12 ④⑤ — 이전 회차와 같은 사유"
  - n: 3
    result: "RETRY_SCENARIO (Phase 3 → 2a)"
    outcome: "Phase 3 자체 리뷰 1라운드 [correctness] 뿌리 3개가 시나리오 밖 — 사용자 결정 2026-11-20 「2a 로 돌아가 한번에」"
    back_to: "Phase 2a"
    fix: "RC-1 → SC-21, RC-2 → SC-22·23·24, RC-4 → 머리말 「기동」(OS 환경변수 제외). RC-3 은 수용 위험 — PLAN unresolved 에 호출 규칙. PLAN inputs 3개·input_fields SMTP_TIMEOUT 추가"
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
