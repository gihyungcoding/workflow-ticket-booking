---
status: ACTIVE
saved_at: 2026-10-21T00:00:00Z
summary: "시나리오 16건(happy 4 / error 7 / boundary 4 / regression 1), AC 8/8 매핑. 독립검증 1차 FAIL(V11 Authorization 경계) → 보완 → 2차 PASS(경고 4). HITL#1 승인 대기."

next_steps:
  - "HITL#1 승인 후 Phase 2b — wf-red 로 Red 테스트 작성 (@SpringBootTest 통합 테스트, 컴파일용 스켈레톤은 UnsupportedOperationException 한 줄만)"

decisions:
  - decision: "시나리오가 16건으로 복잡 기준선(8~12건)을 넘는다 — 표로 묶었음에도 줄이지 못함"
    rationale: "입력 경계(가입 3 + 로그인 1 + 헤더 + 환경변수)와 토큰 검증 4종(위조·typ·만료·사용자 없음)은 서로 다른 코드 경로라 합칠 수 없다. 같은 경로(필수·형식, 길이, 헤더 부재 변형)는 표의 행으로 묶고 각 행을 독립 단언한다. 12건 초과는 HITL#1 에서 사용자에게 알린다"
  - decision: "이메일은 trim + 소문자 정규화(SC-01/03/07)"
    rationale: "출처 문서에 규칙이 없어 Plan 에서 제안했고 시나리오로 확정했다. 사용자 확인은 HITL#1 에서"
  - decision: "허용 목록 밖 경로는 토큰 없이 401(404 아님) — 기본 거부(SC-11)"
    rationale: "anyRequest().authenticated(). 존재하지 않는 경로가 404 대신 401 이 되는 것을 수용. HITL#1 에서 확인"
  - decision: "인증 스킴은 대소문자 무시(SC-10), 그 외 변형('BearerXYZ', 공백 없음·두 개, 토큰 뒤 추가 값)은 401(SC-11)"
    rationale: "RFC 7235 스킴 대소문자 무시. 나머지는 엄격 거부. 독립검증 V11 반영"
  - decision: "만료 경계는 ±1초만 단정하고 정확히 T+24h 는 단정하지 않는다(SC-13)"
    rationale: "라이브러리 기본 비교에 달려 있고 요구사항에 정의가 없다"
  - decision: "SC-09(로그인 요청 형식 오류 400)는 AC5 의 401 문장에 연결했다"
    rationale: "AC5 가 로그인 거부 전반이고 '요청 형식 오류는 401 이 아니라 400' 을 확인한다. 검증자 V8 이 기준과 기대 표기 불일치로 경고 — 사람이 판단"
  - decision: "SC-15 는 공연 회귀에 인증 API 사전 요청(OPTIONS)을 한 표로 함께 둔다"
    rationale: "CORS 필터가 Security 체인보다 앞서는 순서의 회귀(ADR-0016). 검증자 V6 경고 — 분리 여부는 사람이 판단"
  - decision: "DELETE 엔드포인트는 없다 — 공연 쓰기는 POST·PUT·POST cancel"
    rationale: "Plan F8 의 DELETE 표기를 정정했다(컨트롤러 확인)"

attempts:
  - phase: "2a"
    round: 1
    result: "FAIL"
    reason: "V11 — Authorization 헤더의 스킴 대소문자·접두사만 일치·공백 변형 시나리오 누락"
    fix: "SC-10 에 스킴 표기 변형, SC-11 에 접두사·공백·추가값 행 추가. Plan 의 password 공백 불가·DELETE 표기·outputs 정정"
  - phase: "2a"
    round: 2
    result: "PASS (경고 4)"
---
