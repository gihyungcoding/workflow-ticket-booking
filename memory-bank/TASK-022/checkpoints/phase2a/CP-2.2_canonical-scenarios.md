---
status: ARCHIVED
saved_at: 2026-11-12T00:00:00Z
summary: "SCENARIO_TASK-022 — 15건(happy 5·error 5·regression 5), AC 8/8, 입력 경계 71/71. RETRY_SCENARIO 2/3(Phase 3 자체 리뷰 RC-1) — SC-05 에 sub 비정규 표기 8행. 독립검증 5회차 PASS(경고 6)"

next_steps:
  - "Phase 2b 재진입 — SC-05 w~ad 테스트를 먼저 실패시키고(구현 46af97b 기준) 통합 HITL 재승인 → Phase 3 sub 정규형 검사"
  - "Phase 2b — wf-red. 승인 테스트 변경 6건을 Red 와 함께 통합 HITL 에 올린다"

decisions:
  - decision: "새로 쓴다 — 기존 시나리오를 확장하지 않는다"
    rationale: "이 태스크의 대상(역할 인가·소유권)을 다룬 기존 시나리오가 없다"
  - decision: "/api/performances/** 는 GET 만 공개, 그 밖의 메서드는 모두 ROLE_ORGANIZER"
    rationale: "DELETE·PATCH 가 토큰 없이 405 로 떨어지던 회귀 표는 쓰기 행에 토큰을 실어 숫자를 유지한다(SC-12). 미지원 메서드를 따로 열어 두는 규칙을 만들지 않는다"
  - decision: "403 메시지는 '주최자만 이용할 수 있습니다'"
    rationale: "기능 문서 §3-3 의 FORBIDDEN_ROLE. 프론트 안내 문구(TASK-024)와 같은 뜻"
  - decision: "승인 테스트 변경 6건을 시나리오 MD 끝 표로 명시"
    rationale: "PLAN unresolved — 2b 통합 승인에 함께 올린다"
  - decision: "PLAN inputs 의 제약 대조: Authorization(SC-04·06), role(SC-05), sub(기존 JwtTokenFormatTest — 이 태스크에서 바꾸지 않음), id(SC-07), WRITE_GUARD 환경변수(SC-09), 본문(기존 테스트)"
    rationale: "Step 2 입력 제약 대조"

  - decision: "PLAN input_fields 보완(분류 보완 — 롤백 아님): sub(required·integer_notation), WRITE_GUARD 환경변수(config), role 에 string 종류"
    rationale: "1회차 V11 plan_gap 3건"
  - decision: "1회차 경고 반영: V6 — SC-08 을 SC-08·13·14 로, SC-10 을 SC-10·15 로 나눔. V5 — SC-01 Given 에 '읽어 둔다', SC-07 f·g 는 상태만 단언, SC-12 를 12행 표로 직접 적음. V7 — SC-06 Then 의 클래스명 제거, SC-04 f 헬퍼명 제거(SC-10 의 이름은 삭제 확인이라 유지). V8 — PLAN inputs 에 typ/exp·pendingToken, outputs 에 400. V12 — (1) SC-05 u·v(typ·만료) (2) 본문 검증이 소유권보다 먼저 — SC-07 에 400 동일 단언 (3) SC-12 k·l(관객 DELETE·PATCH 403)"
    rationale: "재검증을 어차피 하므로 함께 반영"
  - decision: "보류: V9 중복(SC-05 a·b, SC-09 d) — 표의 기준 행이라 남긴다. V12 (4) 취소된 공연 재취소는 TASK-023 이 409 로 정한다 — 이 태스크는 동작을 바꾸지 않아 단언하지 않는다. V12 (5) 는 SC-07 b 와 같은 경로"
    rationale: "경고는 사람 판단 몫 — 2b 통합 HITL 요약에 올린다"

  - decision: "2회차 경고 반영(재검증 없음): V8 — PLAN SecurityConfig 변경을 'GET 외 모든 메서드 hasRole(ORGANIZER)' 로, outputs 에 405·415. V12(3) — BEARER 정규식이 CASE_INSENSITIVE 임을 코드로 확인(JwtAuthenticationFilter) — SC-06 b 는 기존 동작"
    rationale: "PASS 후 경고는 재검증하지 않는다"
  - decision: "2회차 경고 보류(HITL 에서 판단): V5 SC-02·12 의 기존 테스트 위임 — SC-12 는 표로 적었고 SC-02 는 기존 승인 테스트 단언을 그대로 쓴다. V6 SC-09 연쇄(가입→로그인→등록은 앞 응답이 다음 입력이라 하나의 흐름)·SC-07 마지막 And(2b 에서 별도 테스트 메서드로 나눈다). V7 ownerId(관찰 대상이 DB 값이라 필드명이 곧 관찰 지점). V9 중복. V12 (1) typ/exp·pendingToken 은 기존 규칙 그대로라 칸을 두지 않음 (2) exp 정각 경계는 기존 notExpiredValidator 규칙 — 이 태스크가 바꾸지 않음 (4) sub 사용자 없음 500 은 PLAN unresolved (5) 재취소는 TASK-023"
    rationale: "경고는 사람 판단 몫"
  - decision: "RETRY_SCENARIO(1/3, Phase 3 → 2a, 사용자 선택) — SC-06 k(LF 꼬리)·l(NUL 꼬리)의 기대를 401 → 400 · 공연 수 그대로로. Then 두 번째 문장을 '201 이 아닌 행' 으로, notes 에 'k·l 은 상태만 단언' 추가"
    rationale: "Green 실행에서 400·본문 없음. 같은 헤더를 MockMvc 프로브로 보내면 LF·NUL 은 400, 제어 문자 없는 무효 토큰은 401. Red 실행(구현 전)에서도 k·l 만 상태 단언 1건씩 실패(20 = 9행×2 + 2행×1)해 이 태스크 전부터 같은 거부였다. 보안 체인이 요청을 컨트롤러 전에 거부해 DB 가 바뀌지 않으므로 시나리오 의도(거부·DB 그대로)는 유지된다"
  - decision: "정책 바뀜 대조 — '401' 기대를 쓰는 다른 시나리오(SC-04·05·12)는 제어 문자 헤더를 쓰지 않아 고치지 않음"
    rationale: "SC-06 표의 다른 행·SC-05 의 클레임 변형은 헤더에 제어 문자가 들어가지 않는다(클레임 안의 제어 문자는 base64url 로 인코딩됨)"
  - decision: "4회차 경고 반영(재검증 없음): V7 신규 — SC-06 k 의 괄호 사유에서 '보안 체인의 요청 방화벽' 을 '요청 자체가 거부된다' 로. V12 신규(400 근거) — 위 프로브·Red 실행 기록이 실제 요청 확인이다. 2b Red 재실행에서 다시 관찰한다"
    rationale: "PASS 후 경고는 재검증하지 않는다"
  - decision: "RETRY_SCENARIO(2/3, Phase 3 자체 리뷰 RC-1 → 2a, 사용자 선택) — 규약 표에 '유효한 사용자 클레임'(1 이상 정규형 10진, ASCII 숫자, 부호·앞자리 0 없음, long 범위). SC-05 두 번째 표에 w(+)·x(앞자리 0)·y(아라비아-인도 숫자)·z(음수)·aa(\"0\")·ab(소수) — 모두 401"
    rationale: "리뷰어가 서명된 토큰으로 '+7'·'07'·'٧'·'-7' 이 사용자 7(-7)로 통과함을 재현. 시나리오의 sub 행(p~t)에 없는 변종이라 Phase 3 에서 만들지 않고 2a 로. 변종 전부 한 번에"
  - decision: "PLAN input_fields 보완(분류 보완 — 롤백 아님): sub kinds 에 integer·converted, note 갱신. 새 칸 14개를 SC-05 로 판정"
    rationale: "integer_notation 종류에 부호·앞자리 0·유니코드 숫자 칸이 없다. check_plan.py 의 위반 9건은 target_files 의 new·존재 검사 — 구현으로 파일이 생기거나 지워진 결과이고 보완 전 PLAN 에서도 같은 9건이다"
  - decision: "5회차 경고 반영(재검증 없음): V12 신규 — sub 앞 공백(ac)·전각 숫자(ad) 행 추가. 같은 뿌리(RC-1)의 변종이라 한 번에 가져간다"
    rationale: "PASS 후 경고는 재검증하지 않는다"
attempts:
  - round: 1
    result: FAIL
    detail: "V11 — affix_entry false_cover 2칸(=·LF·NUL·서로게이트 행 없음), plan_gap 3건(sub·WRITE_GUARD env·role string). 경고 7"
  - round: 2
    result: PASS
    detail: "V11 해소. 경고 7(V5·V6·V7·V8·V9·V12 + 참고)"
  - round: 3
    result: PASS
    detail: "경고 6(V5·V6·V7·V9·V12). 2b Red 작성 중 발견 — SC-09 c 의 social/complete 기대 201 이 실제 응답(200, AuthController·WriteGuardSocialCompleteTest)과 다르다. 200 으로 고치고 재검증"
  - round: 4
    result: PASS
    detail: "RETRY_SCENARIO(1/3, Phase 3 → 2a) — SC-06 k·l 401→400. 경고 6(V5·V6·V7·V9·V12, 신규 V7 k 행 구현 설명·V12 400 근거)"
    outcome: "Phase 3 Green 에서 SC-06 k·l 이 400"
    back_to: "2a"
  - round: 5
    result: PASS
    detail: "RETRY_SCENARIO(2/3, Phase 3 자체 리뷰 RC-1 → 2a) — SC-05 w~ab·규약 행·PLAN sub kinds. 경고 6(신규 V9 p~ab 같은 경로·V12 sub 공백·전각 숫자 → ac·ad 로 반영)"
    outcome: "Phase 3 자체 리뷰 1R [correctness] RC-1 이 시나리오 밖 변종"
    back_to: "2a"
---

# CP-2.2 시나리오 — TASK-022

- `workflow_design/05_scenario/SCENARIO_TASK-022.md` (SoT) · `.json`
