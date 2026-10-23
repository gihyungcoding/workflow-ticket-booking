---
status: ACTIVE
saved_at: 2026-10-23T03:00:00Z
summary: "[2차 작성 · 재검증 대기] 1차 독립검증 FAIL(V11: FRONTEND_BASE_URL 환경변수 경계·오픈 리다이렉트 방지 시나리오 누락) 반영 — SC-19(기준 주소 환경변수)·SC-20(콜백 조작 × 상황) 추가, SC-16 에 redirect_uri·X-Forwarded-Host 행, name·providerUserId 제어문자 행, 카카오 is_email_valid 조합, V5·V6·V8 경고 정리. 20건(happy 4 / error 9 / boundary 6 / regression 1). [1차] 18건 작성 → FAIL 1(V11) · WARN 3."

next_steps:
  - "2차 독립검증 결과 처리 — PASS 면 HITL#1(시나리오가 8~12 기준선 초과 20건임을 알린다), FAIL 이면 지적 보완(재검증 최대 3회)"
  - "HITL#1 승인 후 Phase 2b — wf-red. 표 행마다 테스트 케이스, 환경변수 시나리오(SC-16·19)와 쓰기 가드(SC-18)는 설정 주입 수단을 TEST JSON 에 적는다(config_applied_via)"

decisions:
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
    round: 1
    outcome: "FAIL — V11(FRONTEND_BASE_URL 환경변수 행 없음, 오픈 리다이렉트 방지 시나리오 없음 — 이 둘이 fail 사유 / name·providerUserId 제어문자 행, 카카오 is_email_valid 조합은 보강 권고) · WARN 3(V5 SC-18 내부 순서 Then·SC-05/13 동시성, V6 SC-17 다중 When, V8 Plan outputs 에 404·WRITE_KEY_INVALID·SOCIAL_ACCOUNT_ALREADY_LINKED 없음)"
    reject_type: "VALIDATOR_FAIL 1/3"
---
