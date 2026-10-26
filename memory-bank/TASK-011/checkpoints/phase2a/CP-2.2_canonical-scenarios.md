---
status: ACTIVE
saved_at: 2026-10-23T03:00:00Z
summary: "[HITL#1 승인됨 2026-10-26 · Phase 4 FAIL 롤백 재진입 · 4차 검증 PASS (경고 3)] 21건(SC-21 추가), AC 9/9. 사용자 결정: 롤백 지점 Phase 2a, 자동 연결 정책 '검증된 소셜 계정에만'(ADR-0020). RC-1·RC-2 재현 변종 전부 반영 — 규약 표(emailVerified 클레임·연결 대상·소셜 이메일 형식), SC-01·02·03·07·09·10·12 보강, SC-21 신규. 경고: V1 JSON 총계 낡음(수정함)·V6 When 단일성(SC-21 포함)·V9 SC-12/SC-21 409 겹침. [이전 요약] 20건 HITL#1 승인(2026-10-23)."

next_steps:
  - "Phase 2b 재진입 (HITL#1 승인 완료) — wf-red: 바뀐 시나리오 행(SC-01·02·03·07·09·10·12)을 고치고 SC-21 을 추가, Red 스켈레톤에 users.email_verified·pendingToken emailVerified 추가. 변이 검증으로 가짜 통과가 아님을 증명(구현이 이미 있는 Red 재작성)"
  - "2차 독립검증 결과 처리 — PASS 면 HITL#1(시나리오가 8~12 기준선 초과 20건임을 알린다), FAIL 이면 지적 보완(재검증 최대 3회)"
  - "HITL#1 승인 후 Phase 2b — wf-red. 표 행마다 테스트 케이스, 환경변수 시나리오(SC-16·19)와 쓰기 가드(SC-18)는 설정 주입 수단을 TEST JSON 에 적는다(config_applied_via)"

decisions:
  - decision: "[롤백 재진입] 자동 연결 대상을 users.email_verified=true 계정으로 좁힌다 (ADR-0020, 사용자 선택 '검증된 소셜 계정에만 자동 연결')"
    rationale: "Phase 4 RC-1 — 이메일 가입 선점·미검증 소셜 가입 선점·네이버 계정이 임시 테스트로 재현됐다. 이메일 가입자가 같은 이메일로 소셜 로그인하면 연결되지 않고 /complete 가 409 — 연결 수단은 후속 태스크. 정당한 사용자의 가입이 선점 이메일에 막히는 것은 수용한 잔여 위험(Plan unresolved)"
  - decision: "[롤백 재진입] 소셜 이메일 형식은 이메일 가입과 같은 규칙으로 — 정확히 하나의 @, 앞뒤 비어 있지 않음, 공백류·서식 문자(Cf)·제어문자·짝 없는 서로게이트 거부. pendingToken 검증도 같은 규칙을 재적용"
    rationale: "RC-2 — 규칙이 toProfile 에만 있어 서명된 토큰의 값이 DB 오류(500)·'?' 치환으로 샌다. 규칙을 공통 지점 한 곳에 둔다. 정당한 경로에서는 도달하지 않으므로 심각도는 낮음"
  - decision: "[롤백 재진입] 4차 검증 경고 중 JSON 총계만 고쳤다. V6·V9·보강 후보(SC-19 http:// 정상 행, SC-16 한쪽 빈 값 행, name 앞뒤 공백)는 보류"
    rationale: "V6/V9 는 3차에서 이미 사유를 적어 보류한 항목과 같은 성격. 보강 후보는 FAIL 사유가 아니고 이번 롤백의 뿌리와 무관 — HITL#1 요약에 알린다"
  - decision: "속성 타입은 엄격히 본다 — 문자열이어야 할 값이 숫자·불리언·객체·배열이면 문자열로 바꿔 쓰지 않고 실패. 카카오 id 는 양의 정수(Long 범위)만"
    rationale: "2차 V11. ADR-0019(조용한 변형 금지)와 검증 플래그의 '불리언만 인정' 규칙과 같은 방향. 숫자를 toString 으로 받으면 다른 제공자 ID 와 충돌하거나 500 이 난다"
  - decision: "FRONTEND_BASE_URL 은 http(s) 절대 주소만, 스킴 대소문자 무관(값은 바꾸지 않음), 경로 허용, 쿼리·프래그먼트·사용자 정보·호스트 없음은 기동 실패"
    rationale: "2차 V11. Location 을 조립하는 값이라 모호하면 오픈 리다이렉트나 잘못된 주소가 된다"
  - decision: "이메일 길이는 소문자화 뒤에 센다 (İ 케이스를 SC-07 에 넣었다)"
    rationale: "TASK-009 첫 FAIL 의 뿌리(소문자화로 길이가 늘어나는 이메일)와 같은 경계를 소셜 입력에서도 대본다"
  - decision: "새로 작성했다 (기존 시나리오 파일 없음)"
    rationale: "TASK-011 첫 2a"
  - decision: "Plan unresolved 를 시나리오에서 확정: 리다이렉트 규약(성공 ?token / 대기 ?pendingToken / 실패 /login?error=social), 오류 코드(PENDING_SIGNUP_EXPIRED 401, SOCIAL_ACCOUNT_ALREADY_LINKED 409 신설), 이메일·이름 누락·길이 초과는 자르거나 대체하지 않고 실패(SC-07), /complete 충돌은 409(SC-12), 제공자 설정 없음 → 404 미등록·앱 기동(SC-16), 네이버는 플래그 없음 → 자동 연결 안 함(SC-03)"
    rationale: "시나리오가 Red 테스트의 단언이 되므로 모호한 채로 넘기지 않는다. ADR-0019(조용한 변형 금지)·AC3 문구를 그대로 따랐다"
  - decision: "제공자 설정이 하나라도 있는데 FRONTEND_BASE_URL 이 비었거나 http(s) 절대 주소가 아니면 서버는 기동하지 않는다 (제공자 설정이 없으면 기준 주소 없이 기동)"
    rationale: "1차 검증 V11. 'null/oauth2/redirect' 같은 잘못된 Location 으로 열리는 fail-open 을 막는다. JWT_SECRET·WRITE_API_KEY 와 같은 기동 실패 관례"
  - decision: "카카오는 is_email_verified 와 is_email_valid 가 둘 다 불리언 true 일 때만 검증됨"
    rationale: "Plan 이 두 필드를 본다고 적었고 검증자 V11(4)가 조합 누락을 지적. 한쪽만 true·누락은 미검증으로 본다"
  - decision: "AC 밖 흐름(F8·F9)과 설정·가드·리다이렉트 안전 시나리오의 covers 는 가장 가까운 AC 에 매단 것이고 커버리지로 세지 않는다"
    rationale: "V3 지적(covers 부풀림). AC 7개는 SC-01~06·10·14 가 덮는다"
  - decision: "OOS-1(TASK-010): 소셜 입력의 앞 NUL 은 trim 으로 지우지 않고 거부로 못박았다(SC-07)"
    rationale: "ADR-0019. 이메일 가입(TASK-009) 서버 수정은 이 태스크 범위 밖"
  - decision: "표로 묶은 근거는 SCENARIO_TASK-011.json coverage.merged 에 기록했다 (제공자 3종 × 판정, 필드별 이상 입력, pendingToken 무효 11행, 설정 환경변수, 콜백 조작 × 상황)"
    rationale: "coverage-policy §3 — 같은 코드 경로의 입력은 하나로 묶고 이유를 남긴다"

attempts:
  - phase: "2a"
    round: "재진입 1 (Phase 4 FAIL 롤백)"
    outcome: "RC-1·RC-2 변종을 시나리오로 가져옴, 4차 검증 PASS(경고 3) — HITL#1 대기"
    back_to: "2a"
  - phase: "2a"
    round: 3
    outcome: "PASS — 경고 4(V5 SC-16 정상 동작 모호, V6 SC-19·16·17 다중 호출, V7 SC-08 Spring 오류 코드명, V8 Plan outputs 에 인가 요청 302·기동 실패 없음). V11 은 통과하되 카카오 플래그 비불리언·/complete pendingToken JSON 타입 행 보강 권고"
  - phase: "2a"
    round: 2
    outcome: "FAIL — V11(카카오 id 정수 경계 없음, email·name·providerUserId 의 문자열 아닌 타입 없음 — 이 둘이 fail 사유 / FRONTEND_BASE_URL 표기 변형·알 수 없는 {provider} 경로값은 보강 권고) · WARN 3(V6 SC-19 이중 When, V7 SC-17 permitAll 용어, V8 인가 요청 입력이 Plan 에 없음)"
    reject_type: "VALIDATOR_FAIL 2/3"
  - phase: "2a"
    round: 1
    outcome: "FAIL — V11(FRONTEND_BASE_URL 환경변수 행 없음, 오픈 리다이렉트 방지 시나리오 없음 — 이 둘이 fail 사유 / name·providerUserId 제어문자 행, 카카오 is_email_valid 조합은 보강 권고) · WARN 3(V5 SC-18 내부 순서 Then·SC-05/13 동시성, V6 SC-17 다중 When, V8 Plan outputs 에 404·WRITE_KEY_INVALID·SOCIAL_ACCOUNT_ALREADY_LINKED 없음)"
    reject_type: "VALIDATOR_FAIL 1/3"
---
