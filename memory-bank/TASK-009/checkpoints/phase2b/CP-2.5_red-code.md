---
status: ACTIVE
saved_at: 2026-10-21T00:00:00Z
summary: "[재진입] 테스트 19건 — 구현이 이미 있는 Red 재작성. 새 행 중 SC-13 정각(200)·SC-17(401×7)·SC-18(409×2) 3건이 현재 구현에서 실제로 실패(Phase 4 결함과 일치), SC-19 및 SC-08·09 새 행은 already_passing. 변이 3건 모두 사망(생존했던 login 72바이트 가드는 SC-08 보완 후 사망 — BCrypt 가 잘라서 비교하는 보안 문제). 이전 105건 통과. HITL#2 재승인됨(2026-10-21)."

next_steps:
  - "HITL#2 재승인 후 Phase 3 — 만료 정각 401(Spring 검증기 대신 직접 구현), ERROR 디스패치 permitAll, 정규화 후 길이 검증"
  - "(이전)"
  - "Phase 3 — wf-develop 로 구현. spring-boot-starter-security 와 security-test 추가, V3 마이그레이션, 엔티티 제약 애노테이션, AuthService·컨트롤러·SecurityConfig·JWT"
  - "Green 에서 확인할 위험은 TEST_TASK-009.json 의 risks_for_green (role 숫자 enum, email @Size(255), JWT_SECRET 기동 검증)"

decisions:
  - decision: "[재진입] SC-17 은 MockMvc 가 아니라 RANDOM_PORT 실제 서버 + java.net.http.HttpClient"
    rationale: "MockMvc 는 서블릿 컨테이너의 ERROR 디스패치(sendError → /error)를 재현하지 못한다 — 이 회귀는 MockMvc 로 쓰면 구현이 틀려도 통과하는 가짜 테스트가 된다. 현재 구현에서 401 로 실제 실패함을 확인"
  - decision: "[재진입] SC-19 는 가드를 켠 별도 테스트 클래스(WriteGuardAuthApiTest)"
    rationale: "클래스 properties 로 WRITE_GUARD_ENABLED=true 를 실제 주입. already_passing 이지만 WriteGuardFilter 에 /api/auth 면제 변이를 넣으면 실패함을 확인"
  - decision: "[재진입] 생존 변이(login 72바이트 가드)는 2b 에서 시나리오를 고치지 않고 사람에게 판단을 구했고, 사람이 시나리오 보완을 선택"
    rationale: "SCENARIO 수정은 2b 에서 금지(HITL#1 확정). 2a 로 돌아가 SC-08 에 72바이트 비밀번호 사용자와 '비밀번호 + 1글자' 행을 추가·재검증·재승인한 뒤, 테스트를 고쳐 변이가 사망함을 확인(가드 제거 시 73바이트 입력이 200)"
  - decision: "spring-boot-starter-security 를 Red 에서 넣지 않고 crypto·oauth2-jose 만 추가"
    rationale: "starter 의 자동 구성이 모든 엔드포인트를 막아 기존 공연 API 테스트 89건이 Red 에서 깨진다. 컴파일에 필요한 BCrypt·Nimbus 만 추가"
  - decision: "User 엔티티는 구조 애노테이션과 접근자(throw)만, 제약 애노테이션 없음. UserRepository 는 빈 JpaRepository"
    rationale: "memory/feedback_java_tdd_red_skeleton — 제약은 Green. 테스트는 findAll·count·deleteAll 만 쓴다"
  - decision: "SC-16 은 JwtTokenProvider.fromEnv(secret, clock) 정적 팩토리로 검증하고 앱 기동 행은 빈 값 하나만"
    rationale: "WriteGuardFilter.fromEnv 와 같은 관례. 환경변수 미설정은 test properties 가 값을 채우므로 null 로 대신"
  - decision: "한 시나리오 = 테스트 함수 하나. 표의 행은 SoftAssertions 로 독립 단언"
    rationale: "인벤토리 16 == 16. 한 행이 실패해도 나머지 행의 결과가 함께 보인다"
  - decision: "SC-15(회귀)는 already_passing — 억지로 실패시키지 않는다"
    rationale: "공연 API 무인증·CORS 사전 요청은 Security 도입 전에도 참이다. Green 이후의 안전망"
  - decision: "토큰은 구현과 무관하게 Nimbus 로 직접 만들고, 도구 정확성은 임시 테스트로 검증 후 삭제"
    rationale: "위조 행이 형식 깨짐 때문에 우연히 401 이 되는 가짜 통과를 막는다"
  - decision: "SC-15 Given 공연은 오픈 전(openAt = now+5일)으로 두고, 정리는 좌석 → 공연 순서"
    rationale: "오픈된 공연은 PUT 이 409. 등록 API 가 만든 좌석이 남으면 deleteAll 이 FK 위반으로 실패해 PerformanceApiTest 4건을 오염시켰다(수정 후 89건 통과)"

attempts:
  - phase: "2b"
    round: 1
    result: "수정 후 통과"
    reason: "첫 실행에서 기존 PerformanceApiTest 4건 실패 — 내 SC-15 가 남긴 공연·좌석이 원인(테스트 정리 누락), SC-15 PUT 409"
    fix: "정리 순서(seat → performance)와 Given 공연 오픈 시각 수정"
---
