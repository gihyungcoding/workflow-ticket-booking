---
status: ACTIVE
saved_at: 2026-10-27T00:00:00Z
summary: "[Phase 3 완료] 전체 145건 중 통과 144 · 실패 0 · 건너뜀 1(기존 DockerfileTest 조건부). 대상 12건 전부 Green. 린트 error 0, 아키텍처 제약 위반 0. 변이 검증 10건 중 9건 잡힘, 안 잡힌 1건은 불필요한 검사라 제거. 승인된 테스트는 한 줄도 고치지 않았다."

next_steps:
  - "Phase 4 — wf-verify. code-reviewer 호출 시 CLAUDE.md 의 JPA 리뷰 지시를 따른다(테스트가 클래스 레벨 @Transactional 이 아니라 해당 없음 — 이번 테스트는 모두 비트랜잭션 + 리포지토리 재조회라고 명시)"
  - "Phase 4 필수 증거: 실제 PostgreSQL 로 앱을 띄워 애플 이름 경계(합성 100자·4바이트 문자)를 /api/auth/social/complete 까지 프로브 (PLAN 결정)"
  - "HITL#3 에 올릴 것: DEV_TASK-012.json 의 open_risks R-1 — 교차 사이트 form_post 에서 세션 쿠키 전송은 미검증 가정(Chrome 2분 예외). 선택지 (a) SameSite=None;Secure 설정 (b) 쿠키 기반 저장소+ADR. 사용자 결정 필요"
  - "리뷰어에게 요청: email_verified 해석(불리언/\"true\" 외 전부 false)이 선점 경로를 열지 않는지, Client Secret 서명·파싱 오류 메시지에 키 본문이 새지 않는지, user 파라미터 출처 제한(apple+POST)"

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

attempts: []
---
