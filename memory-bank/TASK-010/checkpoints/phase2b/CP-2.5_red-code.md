---
status: ACTIVE
saved_at: 2026-10-22T05:00:00Z
summary: "[재진입 · HITL#2 대기] 시나리오 31건(SC-33~35 추가) — 신규 15케이스 중 10건 실제 실패(RC-1: login() 이 restoring 을 끝내지 않음, 취소된 요청의 401 이 토큰 삭제), 5건은 이미 통과해 변이 6건으로 검증(전부 사망, 구조적 생존 행 기록). 전체 176건 중 10 실패·166 통과. [이전] [HITL#2 승인됨 2026-10-22] 시나리오 28건 → frontend/src/auth/AuthScreens.test.tsx (test.each 행 포함 118케이스). 컴파일 스켈레톤 5개(전부 throw 'not implemented'). 실행: 161건 중 118 실패(전부 'not implemented')·43 통과(기존 전부). tsc -b 오류 0. already_passing 없음."

next_steps:
  - "HITL#2 승인 후 Phase 3 — 공통 지점에서 수정: AuthProvider 에 세션 세대(ref 카운터)를 두고 login/logout 이 올리면 진행 중 복원의 모든 결과(setUser·writeToken(null)·setRestoring)를 무시, login() 은 setRestoring(false). 취소된 effect 의 결과도 무시"
  - "HITL#2 승인 후 Phase 3 — wf-develop 로 api/auth.ts·AuthContext·SignUpPage·LoginPage·AppHeader 구현, App.tsx 라우트·헤더, main.tsx AuthProvider"
  - "Green 위험은 TEST_TASK-010.json 의 risks_for_green 참고 (기존 App.test.tsx 가 AuthProvider 없이 렌더, type=email 의 jsdom 값 정제 등)"

decisions:
  - decision: "테스트는 한 파일(frontend/src/auth/AuthScreens.test.tsx)에 모은다"
    rationale: "SC-07·12·19 처럼 가입·로그인 두 화면에 걸친 시나리오가 있고, 가짜 서버·렌더 헬퍼를 공유한다. 시나리오 하나 = test/test.each 선언 하나"
  - decision: "api/auth 를 mock 하지 않고 fetch 만 대체한다 (Plan 의 mock 전략에서 한 걸음 더 실제로)"
    rationale: "시나리오가 요청 본문·헤더·응답 상태/본문 분기를 단언한다. 상태·code → ApiError 변환까지 실제 코드를 통과시켜야 'code 분기' 경계값이 증명된다"
  - decision: "AuthProvider 를 테스트가 App 바깥에서 감싼다 (main.tsx 와 같은 구조)"
    rationale: "Plan: AuthProvider 는 main.tsx 에서 앱을 감싼다. 이동 결과는 <Where/> 로 MemoryRouter 위치를 읽어 단언한다"
  - decision: "검증자 권고 3건을 테스트 행으로 반영했다"
    rationale: "SC-07 통과 경계(8·72·100·255자, 한글 25자, 로그인 공백만 8자), 이메일 trim 뒤 길이(255 통과/256 거부), SC-03 AUDIENCE·ORGANIZER 두 행"
  - decision: "값 입력은 fireEvent.change, 클릭은 userEvent"
    rationale: "NUL·짝 없는 서로게이트·1000자는 userEvent.type 이 해석·지연한다. 값 자체를 그대로 주입해야 SC-08·09 가 전송 값을 단언할 수 있다"
  - decision: "스켈레톤은 타입·시그니처·ACCESS_TOKEN_KEY 상수만, App.tsx·main.tsx 는 건드리지 않는다"
    rationale: "memory/feedback_java_tdd_red_skeleton — 던지기만. App 을 고치면 기존 App.test.tsx 가 Red 에서 깨진다"

attempts:
  - phase: "2b"
    round: 1
    outcome: "Red 작성 완료 — 118/118 실패(not implemented), 기존 43 통과"
---
