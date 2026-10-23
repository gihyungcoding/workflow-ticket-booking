---
status: ACTIVE
saved_at: 2026-10-23T05:00:00Z
summary: "[Red 작성 · HITL#2 대기] 시나리오 20건 → 새 테스트 4개 클래스(표 행은 한 메서드 안 SoftAssertions). 컴파일 스켈레톤(전부 던지기, 제약 없음) + 의존성 spring-boot-starter-security-oauth2-client. 실행: 132건 중 18 실패·114 통과(기존 112 전부 통과). 새 20건 중 18 실패, SC-14·SC-17 은 기존 동작을 지키는 회귀라 구조적으로 이미 통과(변이 4건으로 가짜 통과 아님을 증명). spotlessCheck 통과."

next_steps:
  - "HITL#2 승인 후 Phase 3 — wf-develop. V4 마이그레이션·SocialAccount 제약·User.passwordHash nullable, AuthService 소셜 판정, JwtTokenProvider pendingToken, CustomOAuth2UserService 정규화, 핸들러 2종, SecurityConfig(oauth2Login·제공자 등록·FRONTEND_BASE_URL 기동 검증), AuthController /complete, AuthExceptionHandler"
  - "Green 위험은 TEST_TASK-011.json 의 risks_for_green (핸들러를 빈으로, 속성 타입 엄격 검사, İ 소문자화, 유니크 위반 처리, 컨텍스트 기동 비용)"
  - "Phase 4 에서 실제 PostgreSQL 프로브가 필수 (Plan 결정 — H2 는 V4 SQL·UNIQUE·FK 를 실행하지 않는다)"

decisions:
  - decision: "테스트는 네 클래스로 나눈다 — 콜백 처리(OAuth2SocialLoginCallbackTest: SC-01~08·20), 가입 완료 API(SocialCompleteApiTest: SC-09~15·17), 제공자·기준 주소 설정(SocialProviderConfigTest: SC-16·19), 쓰기 가드(WriteGuardSocialCompleteTest: SC-18)"
    rationale: "설정 주입 방식이 다르다 — 기본 컨텍스트 공유 / 컨텍스트를 설정별로 새로 기동 / 클래스 properties. 시나리오 하나 = 테스트 메서드 하나(20 == 20)"
  - decision: "제공자 서버를 두지 않고 FakeOAuth2User(attributes)로 성공·실패 핸들러를 직접 실행한다"
    rationale: "DefaultOAuth2User 는 이름 속성이 attributes 에 없으면 생성부터 막아 필수 값이 빠진 응답을 만들 수 없다. 핸들러의 공개 계약(OAuth2AuthenticationToken → 리다이렉트)만 시나리오가 요구한다"
  - decision: "pendingToken 은 구현의 issuePendingToken 에 기대지 않고 Nimbus 로 직접 서명한다"
    rationale: "TASK-009 와 같은 이유 — 검증 로직이 형식 깨짐 때문에 우연히 401 을 내는 가짜 통과를 막는다. 발급 쪽은 SC-01·06 이 클레임을 독립 디코딩해 확인한다"
  - decision: "핸들러를 Spring 빈으로 등록해야 하고 SC-19 가 getBean 으로 기준 주소 적용을 확인한다"
    rationale: "기동한 서버에 SC-01 의 콜백이 들어왔을 때의 Location 을 확인할 외부 수단이 없다(제공자 서버 없음). Green 의 설계 제약으로 TEST JSON 에 남겼다"
  - decision: "SocialAccount 엔티티·User.passwordHash·검증 애노테이션의 제약은 스켈레톤에 넣지 않았다"
    rationale: "memory/feedback_java_tdd_red_skeleton — 제약은 선언이 아니라 동작이다. 넣는 순간 SC-05·13·15 가 통과해 버린다"
  - decision: "SC-14 를 사용자가 실제로 있는 typ=pending 토큰으로 강화했다"
    rationale: "변이 검증에서 typ 검증을 지워도 SC-14 가 통과했다 — sub 사용자가 없으면 /me 가 사용자 조회 실패로 401 이라 가려졌다. 단언을 고친 뒤 변이가 사망"

attempts:
  - phase: "2b"
    round: 1
    outcome: "Red 작성 완료 — 새 20건 중 18 실패, 2 already_passing(SC-14·17), 기존 112 통과"
---
