---
status: ACTIVE
saved_at: 2026-10-29T00:00:00Z
summary: "[Phase 1 완료] route Frontend. flows F1~F5 — AC 5개를 하나씩 덮는다(F1=AC1, F2=AC2, F3=AC3, F4=AC4, F5=AC5). 변경 대상 9건(기존 6·신규 3), 재사용 10. 백엔드 응답 계약은 소스에서 확인(콜백은 token/pendingToken 쿼리만, 사용자 정보는 fetchMe 로, /complete 는 LoginResponse·401 PENDING_SIGNUP_EXPIRED·409 두 종류). 토큰은 query 기준(사용자 결정). unresolved 12건 중 2a 확정 필요 6건. check_plan 통과."

next_steps:
  - "Phase 2a — wf-scenario. 화면 상태 표(feature §2-5)를 한 행씩 covers 와 대조해 CP-2.2 decisions 에 적는다(wf-scenario 새 지침). 콜백 쿼리 변형(token 만/pendingToken 만/둘 다/둘 다 없음/빈 값), 응답 변형(상태 같고 code 다름·빈 바디·비 JSON), pendingToken 페이로드 변형(깨진 base64·한글/4바이트 이름·email 없음)을 경계 시나리오로 대본다"
  - "2a 에서 확정(사용자에게 묻거나 '제가 정한 값' 으로 HITL#1 에 명시): (1) 콜백 비정상·fetchMe 실패의 도착지와 문구 (2) /complete 409·400·가드·네트워크 오류 문구 (3) 역할 선택 라우트와 state 소실 시 동작 (4) 새로고침·뒤로 가기 (5) error=social 배너를 주소에서 지울지 (6) 이미 로그인한 사용자의 콜백 진입"
  - "StrictMode 이중 효과: 쿼리를 첫 렌더에서 한 번만 읽어 보관 — 시나리오가 StrictMode 렌더로 token·pendingToken 양쪽을 검증해야 한다"
  - "문구 정본은 SCENARIO_TASK-013.md 상단 표(TASK-010 관례). AUTH_MESSAGES 에 추가"
  - "Phase 4: 실제 화면 확인 + R-1(애플 form_post 쿠키) 가능한 만큼 실제 브라우저로 확인해 기록(고치지 않음)"

decisions:
  - decision: "토큰은 query(?token=) 기준으로 만들고 파싱을 한 함수에 모은다. 읽은 즉시 주소창에서 지운다"
    rationale: "사용자가 2026-10-29 에 query 기준을 골랐다(AC 문구·현재 백엔드와 일치). ADR-0021(fragment, 채택됨)과 어긋나는 상태를 후속 태스크가 해소한다. 파싱 지점을 한 곳에 모으면 전환 때 수정이 한 곳이고, 즉시 지우기는 ADR-0021 이 '프론트가 지우는 것에 기댄다' 고 한 완화를 query 에서도 미리 적용한다"
  - decision: "token 콜백은 fetchMe(token) 로 사용자를 얻어 auth.login(token,user) 을 부른다"
    rationale: "콜백 쿼리는 토큰만 주고 user 를 주지 않는다(OAuth2LoginSuccessHandler 확인). AuthContext.login 이 토큰 저장·세대 증가(ADR-0018)·복원 대기 해제를 모두 하므로 새 저장 경로를 만들지 않는다"
  - decision: "pendingToken 은 URL 이 아니라 라우터 state 로 역할 선택 화면에 넘기고, 안내 문구용 이메일·이름은 페이로드를 서명 검증 없이 디코드해 표시만 한다"
    rationale: "pendingToken 페이로드에 email·name 이 있고(JwtTokenProvider.issuePendingToken) feature §2-3 이 그것을 데이터 출처로 정했다. 별도 API 가 없다. 표시 전용이고 신원 판단은 서버가 /complete 에서 하므로 서명 검증이 필요 없다. 디코드 실패는 일반 안내로 대체한다"
  - decision: "소셜 버튼은 브랜드 색 없이 중립 버튼 + 제공자 이름 텍스트"
    rationale: "색 리터럴은 theme 밖에 둘 수 없고(DESIGN-002 error) design.md §2 가 채도를 「예매가능」 하나로 한정한다"
  - decision: "개발 프록시에 /oauth2/authorization 만 추가(vite.config.ts)"
    rationale: "기존 프록시는 /api 만이라 로컬에서 버튼이 프론트로 돌아와 동작하지 않는다. /oauth2/redirect 는 프론트 라우트라 /oauth2 전체를 프록시하면 콜백 화면이 막힌다. AC1 의 '각각 /oauth2/authorization/{provider} 로 이동' 이 개발 환경에서도 성립하는 데 필요한 최소 변경"
  - decision: "AC 역방향 확인: 비정상 콜백(파라미터 없음·둘 다·빈 값)과 /complete 의 만료 외 오류는 AC 밖으로 넓힌 것이 아니라 AC4·5 구현이 반드시 정해야 하는 경계"
    rationale: "처리하지 않으면 빈 화면·무한 스피너가 된다. 문구·도착지는 2a 에서 확정한다. 프로필·마이페이지·소셜 연결 해제·ADR-0021 전환·R-1 수정은 넣지 않았다(feature §7·후속)"

attempts: []
---
