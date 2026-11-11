---
status: ACTIVE
saved_at: 2026-11-11T00:00:00Z
summary: "SCENARIO_TASK-022 — 15건(happy 5·error 5·regression 5), AC 8/8, 입력 경계 57/57. 독립검증 2회차 PASS(경고 7)"

next_steps:
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
attempts:
  - round: 1
    result: FAIL
    detail: "V11 — affix_entry false_cover 2칸(=·LF·NUL·서로게이트 행 없음), plan_gap 3건(sub·WRITE_GUARD env·role string). 경고 7"
  - round: 2
    result: PASS
    detail: "V11 해소. 경고 7(V5·V6·V7·V8·V9·V12 + 참고)"
  - round: 3
    result: PENDING
    detail: "2b Red 작성 중 발견 — SC-09 c 의 social/complete 기대 201 이 실제 응답(200, AuthController·WriteGuardSocialCompleteTest)과 다르다. 200 으로 고치고 재검증"
---

# CP-2.2 시나리오 — TASK-022

- `workflow_design/05_scenario/SCENARIO_TASK-022.md` (SoT) · `.json`
