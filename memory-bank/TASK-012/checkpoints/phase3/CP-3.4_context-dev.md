---
status: ARCHIVED
saved_at: 2026-10-27T00:00:00Z
summary: "[Phase 3 재작업 완료 — Phase 4 1라운드 FAIL(RC-1) 대응] 전체 146건 통과 · 실패 0 · 건너뜀 0. 대상 13건(승인 12 + RC-1 회귀 1) Green. 린트 error 0, 아키텍처 위반 0. 재현된 변종 3개를 새 테스트로 먼저 실패시킨 뒤 공통 지점 1곳에서 고쳤고, 승인된 테스트는 고치지 않았다. 변이 N1~N3 모두 잡힘."

next_steps:
  - "Phase 4 재검증 — wf-verify. 이전 root_causes(RC-1)를 그대로 이어받아 변종이 모두 해소됐는지 보고, 수정 커밋도 code-reviewer 로 최소 1회 다시 리뷰한다(이전 소견 카테고리 correctness 를 지목해 같은 부류 전수 확인 요청)"
  - "재검증 필수: ① 체인 프로브(email_verified 15종)를 다시 돌려 \"TRUE\"·\"True\"·\"tRue\" 가 가입 대기인지 ② 실제 PostgreSQL 프로브(이름 경계) ③ 앱 기동 후 실제 HTTP 로 인가 요청 확인. Docker 는 이미 켜져 있다(검증용 컨테이너는 삭제했으니 다시 띄운다: postgres:16-alpine, 포트 55433)"
  - "R-1(세션 쿠키)은 사용자가 수용하고 후속 태스크로 결정 — 승인 근거에 '실제 브라우저 검증 필요' 를 남긴다"
  - "Phase 5 회고 후보: ① 프레임워크가 입력을 정규화하는 계층이 있으면 attributes 를 직접 주입하는 단위/핸들러 테스트가 변환 이전 값을 가정할 수 있다 — 그 계층 이후 값 기준의 통합 테스트를 같이 쓴다 ② Phase 2b 하네스 프로브에 프레임워크 변환기를 포함한다 ③ architecture.md 계층 표에 config 행이 없다"

decisions:
  - decision: "AuthService·Repository·스키마를 바꾸지 않고 config 5곳만 고쳤다 (Plan 의 target_files 와 같다). 추가로 SocialInputRules.text() 가시성만 넓혔다"
    rationale: "애플은 SocialProfile 만 만들어 기존 loginWithSocial 에 합류한다. 이름의 '공백만'(없는 값)과 '제어 문자'(거부)를 가르려면 같은 규칙을 그대로 써야 해서 규칙을 복제하는 대신 1줄 가시성 확대를 택했다 (scope_deviations)"
  - decision: "토큰 교환은 RestClientAuthorizationCodeTokenResponseClient 의 setParametersConverter 로 기본 변환기를 감싸 애플 등록일 때만 client_secret 을 set"
    rationale: "addParametersConverter 는 기본 변환기의 값에 addAll 하므로 client_secret 이 중복된다. 기본 변환기를 감싸 set 하면 교체가 되고 다른 제공자의 파라미터는 그대로다(SC-12 가 지킨다). 빈은 애플이 설정됐을 때만 만들어 SecurityConfig 가 ifAvailable 로 연결한다"
  - decision: "애플 등록의 client_secret 은 비워 둔다 (placeholder 없음)"
    rationale: "ClientRegistration 빌더가 CLIENT_SECRET_POST 에 빈 secret 을 허용함을 빌드·테스트로 확인했다. 정적 값이 아예 없으므로 교체가 빠지면 SC-02 가 바로 실패한다"
  - decision: "개인 키 파싱은 '-----' 줄을 걷어낸 본문을 PKCS#8 로 읽는 한 가지 방식. 오류는 원인 예외를 잇지 않는다"
    rationale: "형태별(실제 개행·\\n 리터럴·헤더 없음·앞뒤 공백)을 한 경로로 받고, SEC1·공개 키·RSA·P-384·깨진 본문은 본문을 P-256 PKCS#8 로 읽지 못해 거부된다. 원인 예외 메시지에 키 조각이 실릴 수 있어 잇지 않는다. 라벨 정규식은 변이 검증에서 불필요로 드러나 제거"
  - decision: "리팩토링은 parsePrivateKey 단순화 1건. 나머지는 하지 않기로 함"
    rationale: "CustomOAuth2UserService 의 apple 분기는 기존 제공자 분기와 같은 모양(프로필 함수 + 헬퍼)이라 이미 관례에 맞는다. SocialLoginConfig 는 제공자별 빌더 메서드 + 조건 클래스 구조를 그대로 따랐다. 더 합치면 제공자 간 결합만 늘어난다"
  - decision: "인가 요청 state 저장은 HttpSession 그대로 두고 R-1 로 기록해 HITL#3 에 올린다"
    rationale: "PLAN 이 Phase 3 에서 정하라고 한 항목이지만 (a) 는 모든 제공자의 배포 설정·HTTPS 전제를 건드리고 (b) 는 새 컴포넌트(ADR)라서 AC 밖의 사용자 결정이다. 브라우저 기본 동작으로 대부분의 흐름은 동작하지만 Chrome 의 2분 예외에 기댄다는 점은 확인하지 못했다. 조용히 바꾸지도, 조용히 넘기지도 않고 알린다"
  - decision: "변이 검증을 10건 수행: 잡히지 않은 것은 M9 하나"
    rationale: "한 번에 12건이 전부 초록이면 가짜 통과를 의심해야 한다. 핵심 동작(client_secret 교체·다른 제공자 보호·email_verified 양방향·iss 검증·form_post·GET 무시·부분 변환·exp)을 망가뜨려 해당 테스트가 실제로 실패함을 확인했다"

  - decision: "[Phase 4 롤백] 승인된 SC-06 을 고치지 않고 새 파일 AppleEmailVerifiedFlowTest 를 추가한다"
    rationale: "승인된 테스트는 사람이 승인한 사양이다. SC-06 의 행(\"True\"·\"TRUE\"·\"tRue\" → 가입 대기)은 맞고 구현이 틀렸다. SC-06 은 attributes 직접 주입이라 운영 경로를 증명하지 못하므로, 같은 값을 id_token 으로 실제 필터 체인에 통과시키는 새 테스트가 그 사양을 운영 경로에서 지킨다. 변이 N1 이 새 테스트만 잡는다는 점이 이 분리의 정당화"
  - decision: "[Phase 4 롤백] 고치는 위치는 JwtDecoderFactory<ClientRegistration> 빈 하나 — SecurityConfig 변경 없음"
    rationale: "OAuth2LoginConfigurer.getJwtDecoderFactoryBean() 이 컨텍스트의 이 타입 빈을 자동으로 쓴다(javap 로 확인, 테스트로 입증). 애플 설정이 있을 때만 만들어 기존 제공자·미설정 동작은 그대로(SC-12·SC-11 가드 통과). 변환기 전체를 끄지 않고 email_verified 한 항목만 뺀다 — 나머지 클레임(exp·iat·aud 등)의 타입 변환은 Spring 의 id_token 검증이 의존한다"
  - decision: "[Phase 4 롤백] 리뷰어 low 소견: tokenEndpoint 중복 연결은 그대로 두고, user 파라미터 쿼리스트링 합침은 주석만 정정"
    rationale: "tokenEndpoint 명시 연결은 중복이지만 무해하고 의도를 드러낸다. getParameter 의 쿼리스트링 합침은 영향이 이름 문자열뿐이고 신원은 id_token 만 쓰며 state 바인딩을 통과한 요청이다 — 동작 변경은 AC 밖이라 하지 않고 주석의 '쿼리는 무시' 오해만 바로잡았다"

attempts:
  - phase: "3"
    round: 2
    outcome: "Phase 4 1라운드 FAIL(RC-1) 로 롤백되어 재작업 — 승인된 사양(SC-06)과 운영 경로의 불일치를 공통 지점에서 해소"
    back_to: "4"
---
