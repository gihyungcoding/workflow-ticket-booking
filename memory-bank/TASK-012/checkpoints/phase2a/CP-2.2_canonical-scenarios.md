---
status: ACTIVE
saved_at: 2026-10-27T00:00:00Z
summary: "[2a 독립검증 통과, HITL#1 대기] 시나리오 12건(happy 7 / boundary 3 / error 1 / regression 1), AC 3/3 매핑. 복잡 기준선(8~12) 상단. 검증 2회차 pass(fail 0, warn 1 — V6 SC-02 When). 1회차는 V11 fail 6건(code 누락·토큰 응답 변형·이름 100자·env 개별 공백 등)을 표 행으로 추가해 해소."

next_steps:
  - "HITL#1 — 시나리오 12건 승인 받기 (요약에 2a 확정 unresolved 4건, 사용자 확인 2건, 보류한 경고 V6 을 명시)"
  - "승인되면 SCENARIO_TASK-012.json 의 human_input.generate_red_trigger 를 true 로, validate_phase2a_gate.py 실행"

decisions:
  - decision: "새로 작성 (기존 시나리오 확장 아님)"
    rationale: "TASK-012 는 TASK-011 의 AuthService 를 바꾸지 않고 애플 입력만 추가한다. TASK-011 시나리오는 수정하지 않고 규약(리다이렉트·pendingToken·오류 코드·자동 연결 대상)만 그대로 인용한다"
  - decision: "표시 이름은 firstName lastName 순서, 공백 1칸 (사용자 선택, AskUserQuestion)"
    rationale: "애플이 주는 순서 그대로이고 로케일 추측이 없다. 한국 이름은 '길동 홍' 이 되는 한계를 사용자가 알고 선택했다. 이름 변경 API 가 없어 한 번 정해지면 바뀌지 않는다는 점은 Phase 5 후보"
  - decision: "신규인데 이름이 없으면 고정 문자열 'Apple 사용자' (사용자 선택, AskUserQuestion)"
    rationale: "애플은 앱 연결을 해제하기 전까지 이름을 다시 보내지 않으므로 가입 거부는 그 사용자를 영구히 막는다. 사용자 입력의 변형이 아니라 부재의 대체라 ADR-0019 와 충돌하지 않는다"
  - decision: "2a 에서 제안값으로 확정(HITL#1 에서 검토): 이름 구조 이상은 실패(부재와 구분) · JSON null 은 없는 값 · 앞뒤 공백 부분은 변환하지 않고 보존 · 애플 설정 일부 누락은 조용히 미등록(키 형식은 네 값이 갖춰졌을 때만 검사) · user 는 apple+POST 에서만 읽고 user.email 은 읽지 않음 · Client Secret exp=iat+300 · aud 는 문자열"
    rationale: "PLAN unresolved 의 [결정 필요 2a] 4건. 기존 관례(다른 제공자의 조용한 미등록, JWT_SECRET 의 fail-fast)와 ADR-0019(조용한 변형 금지)를 따랐다. 사용자가 거절하면 해당 표 행만 고친다"
  - decision: "email_verified 변종은 양방향을 모두 시나리오로 가져옴: SC-05(true 로 읽히는 3행) + SC-06(true 로 읽히지 않는 약 20행)"
    rationale: "TASK-011 Phase 4 인계(RC-1 변종, deferred_to TASK-012). 잘못 true 면 계정 선점 경로가 열리고 잘못 false 면 정당한 사용자가 409 에 이른다"
  - decision: "같은 코드 경로 입력은 표로 묶어 21개 안팎의 후보를 12건으로 줄임 (coverage.merged 에 근거)"
    rationale: "coverage-policy §3. 이름 변형 15행→SC-03, 외부 문자열 거부/허용 약 45행→SC-04(결과 열), 설정·키 형식 23행→SC-11, 인증/id_token 실패 11행→SC-09. 12건은 복잡 기준선(8~12) 상단이며 넘지 않았다"
  - decision: "TASK-011 이 이미 검증한 경로는 다시 쓰지 않음 (coverage.skipped): 미검증 대상 계정 비연결(SC-21), 리다이렉트 주소 조작(SC-20), 쓰기 가드(SC-18), pendingToken 만료·위조(SC-10), /complete 충돌(SC-12)"
    rationale: "AuthService·AuthController·핸들러의 해당 코드를 이 태스크가 바꾸지 않는다. 같은 입력으로 실패할 수 있는 별개 결함이 없다(coverage-policy §3)"
  - decision: "SC-09·SC-10 은 가짜 애플 서버(토큰·JWKS)로 실제 보안 필터 체인을 탄다. 브라우저 쿠키(교차 사이트 POST 에 세션이 가는가)는 증명 대상이 아니다"
    rationale: "SC-03~08 이 핸들러 직접 호출이면 form_post 가 필터 체인을 통과하는지·id_token 검증 설정(issuerUri, aud)이 적용되는지는 아무도 보지 않는다. 쿠키 동작은 MockMvc 로 불가 → Phase 3 설계에서 정하고 Phase 4 에서 실제 브라우저 또는 curl 프로브로 확인"

  - decision: "경고 V6(SC-02 When 에 교환 두 번)은 보류 — 시나리오를 쪼개지 않는다"
    rationale: "'요청마다 새로 만든다'는 서로 다른 시각의 요청 두 건이 있어야 관찰된다. 쪼개면 13건이 되어 기준선을 넘고, 한 건씩의 시나리오는 상수 반환 구현을 통과시킨다. 1회차 지적을 받아 When 에 두 요청을 명시하고 Then 을 요청별로 나눴다. 사람이 판단할 몫으로 HITL#1 에 알린다"
  - decision: "경고 V8(SC-11 의 signup/login 확인이 Plan outputs 밖)은 해당 문장을 삭제해 반영"
    rationale: "TASK-011 SC-16 에서 가져온 문장이며 이 태스크의 출력이 아니다. 기동 성공 여부는 표의 요청 결과로 이미 관찰된다"
  - decision: "검증자 1회차 fail(V11)은 표 행 추가로 반영: 콜백 code 누락·빈 값, 토큰 엔드포인트 응답 변형 6종, 이름 단독 100자·UTF-16 경계, 환경변수 개별 공백, 릴레이 주소(is_private_email). FRONTEND_BASE_URL 형식 변형은 skipped 로 근거를 적었다"
    rationale: "frontendBaseUrl() 이 제공자 무관 단일 함수이고 TASK-011 SC-19 가 전수 검증했다. 애플이 더하는 것은 '애플만 설정돼도 적용된다'는 연결뿐이라 SC-11 #12 가 본다"

attempts:
  - phase: "2a"
    round: 1
    outcome: "독립검증 FAIL — V11 (콜백 code 누락, 토큰 응답 변형, 이름 100자 경계, env 개별 공백 등)"
  - phase: "2a"
    round: 2
    outcome: "독립검증 PASS (warn 1: V6)"
---
