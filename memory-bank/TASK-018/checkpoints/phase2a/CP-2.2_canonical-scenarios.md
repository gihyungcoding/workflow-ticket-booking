---
status: ACTIVE
saved_at: 2026-11-06T09:00:00Z
summary: "SCENARIO_TASK-018 35건, AC 16/16, 입력 경계 242/242. 1회차 V11 FAIL → 2회차 PASS(경고 7)."

next_steps:
  - "Phase 2b — Red 테스트 (기존 AuthScreens·SocialLoginScreens 옛 기대값 교체 포함)"

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

  - decision: "1회차 plan_gap 으로 PLAN input_fields 보완 — via·access_token·user_name·user_email·user_role 추가, user_handle 에 F1, social_provider 는 F4 만"
    rationale: "분류 보완이라 ROLLBACK_PLAN 으로 세지 않는다 (reject-state-machine §3)"
  - decision: "1회차 경고 중 반영 — V5 조합 문자 표기를 \\u 이스케이프로, V12 ①30일 뒤 expiresAt(타이머 한도) ②오프셋 없는 시각은 실패 ④Enter 이중 제출 ⑦공백 포함 비밀번호 자동 로그인을 표 행으로. 보류 — V4 flow 대응·V6 When 분리·V7 구현 누출·V9 중복 행·V12 ③⑤⑥ (HITL 에서 판단)"
    rationale: "경고는 재검증 사유가 아니다. 테스트가 달라지는 것만 반영"

  - decision: "2회차 경고 중 반영 — V1 SC-35 JSON 표를 둘로(파생 스크립트), V12 ③이메일 길이는 걷은 값으로·①응답 토큰·아이디는 걷지 않음(제가 정한 값 3), ⑤from 숫자·객체 행(SC-04), ⑥복원 중 via 유지(SC-31). 보류 — V4·V6·V7·V9(1회차와 같음), V12 ②빈 email 표시·④복사 연타(TASK-017 Toast 부품 규칙을 그대로 따른다)"
    rationale: "경고는 재검증하지 않는다 — HITL 요약에 반영·보류를 적는다"

attempts:
  - round: 1
    outcome: "V11 FAIL — false_cover 2 (email@F1 NUL 행 없음, SC-09 이메일 '256자' 가 실제 254자), plan_gap 2 (via, 응답 accessToken·user.name·email·role)"
    back_to: "2a Step 2"
    fix: "SC-05 이메일 NUL 행, SC-09 a×250+@b.com, PLAN input_fields 에 via·access_token·user_name·user_email·user_role 추가(social_provider 는 F4 만), SC-35 신설(응답 본문 규칙 × F1·F3·F7·F8), 칸 162→242"
  - round: 2
    outcome: "PASS — fail 0, warn 7 (V1·V4·V5·V6·V7·V9·V12)"
    back_to: null
---
