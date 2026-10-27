---
status: ARCHIVED
saved_at: 2026-10-27T00:00:00Z
summary: "[2b 완료, HITL#2 승인] 테스트 12건(시나리오 12 == 함수 12) — 11건 Red + SC-12 already_passing(부정형 회귀). 전체 145건 중 실패 11건은 모두 새 테스트, 기존 133건 통과. 컴파일 스켈레톤은 AppleClientSecretGenerator 하나(본문 UOE). 하네스 가정 2가지는 던져 버릴 프로브로 확인."

next_steps:
  - "Phase 3 (wf-develop) — 승인된 테스트 12건을 통과시키는 최소 구현 + 리팩토링. 승인 후 테스트·린트 설정을 고치면 훅이 사람에게 묻는다"
  - "먼저 TEST_TASK-012.json 의 risks_for_green 7건을 읽는다 (API 고정: AppleClientSecretGenerator 생성자·generate, Client Secret 규격 exp=iat+300·aud 문자열, 애플 등록은 OIDC·registrationId=apple, 지금 통과하는 가드 SC-12·SC-11 #2~#10·#23)"
  - "Phase 3 에서 정할 것: 인가 요청 state 저장(세션 vs 쿠키, form_post 교차 사이트 POST) — 쿠키 저장소를 택하면 ADR. Spring Security 7.1.1 API 확인 항목(PLAN unresolved). risks_for_green 7건은 TEST_TASK-012.json 참고"

decisions:
  - decision: "테스트를 4개 클래스로 나눈다: AppleClientSecretGeneratorTest(SC-01) · AppleLoginCallbackTest(SC-03~08, 핸들러 직접 호출) · AppleOAuth2FlowTest(SC-02·09·10·12, 실제 필터 체인) · AppleProviderConfigTest(SC-11, 행마다 앱 기동)"
    rationale: "컨텍스트 구성이 다르다(제공자 환경변수 없음 / 네 제공자 + 가짜 서버 / 행마다 새 컨텍스트). 기존 TASK-011 테스트의 클래스 분할과 같은 방식"
  - decision: "SC-02 를 토큰 응답 클라이언트 빈 직접 호출이 아니라 필터 체인 전체(두 번 로그인)로 쓴다"
    rationale: "빈 타입·이름을 테스트가 강제하면 Phase 3 의 구현 선택을 불필요하게 묶는다. 필터 체인으로 보면 '토큰 교환 요청에 실린 client_secret' 이라는 시나리오 문장 그대로를 관찰하고 구현 방식(변환기·클라이언트 교체)은 자유롭다"
  - decision: "가짜 서버는 JDK HttpServer, 주소 교체는 ClientRegistrationRepository 를 후처리하는 BeanPostProcessor"
    rationale: "새 의존성(WireMock·MockWebServer)이 없고, 운영 코드에 테스트 전용 설정 키(tokenUri 재정의)를 만들지 않는다. 애플의 나머지 등록 내용(scope·issuerUri·인증 방식)은 구현이 정한 그대로 검증된다"
  - decision: "새 빈을 스켈레톤으로 넣지 않는다 — 컴파일용 스켈레톤은 AppleClientSecretGenerator 하나"
    rationale: "SocialProviderConfigTest 가 제공자 환경변수를 켠 채 앱을 띄우므로 UOE 를 던지는 빈은 기존 테스트를 깬다. 나머지 시나리오는 기존 심볼만 쓰므로 구현 부재 = 단언 실패가 정직한 Red"
  - decision: "SC-12 는 already_passing 으로 둔다"
    rationale: "부정형 회귀 — 다른 제공자의 토큰 교환 방식은 이 태스크가 바꾸지 않는 기존 동작이다. 필터 체인으로 가짜 서버가 받은 요청을 직접 단언하므로 가짜 통과가 아니고, Phase 3 이 client_secret 교체를 모든 제공자에 적용하면 실패한다"
  - decision: "구현 전에 하네스를 프로브로 검증하고 프로브는 삭제한다"
    rationale: "Green 에서 하네스 결함이 드러나면 승인된 테스트를 고쳐야 해 훅이 사람에게 묻는다. JWKS 서명 검증과 POST form_post 콜백 도달을 구현 없이 확인했다"

attempts: []
---
