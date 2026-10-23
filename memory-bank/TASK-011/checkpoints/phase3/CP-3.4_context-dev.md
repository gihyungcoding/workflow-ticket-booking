---
status: ACTIVE
saved_at: 2026-10-23T09:30:00Z
summary: "Green 완료 — 전체 132/132 통과(새 20 + 기존 112), spotlessCheck 통과, 아키텍처 error 0(경고 3 기존). 첫 실행 16/18 → SC-11(숫자·불리언 pendingToken 이 문자열로 변환돼 401)과 SC-16(미등록 제공자 경로 500) 두 건을 구현 쪽에서 고쳐 통과. 승인된 테스트는 수정하지 않았다."

next_steps:
  - "Phase 4 — wf-verify. 실제 PostgreSQL 로 앱을 띄워 V4 마이그레이션·UNIQUE·FK·이메일/이름/providerUserId 전 문자열 입력을 HTTP 로 프로브(필수, H2 는 V4 를 실행하지 않는다)"
  - "code-reviewer 호출 시 CLAUDE.md 의 JPA 지시 포함(비트랜잭션 확인·Given 설정 실제 적용). loginWithSocial 이 트랜잭션 없이 동작하는 점과 completeSocialSignUp 의 경합 처리를 짚게 한다"
  - "DEV_TASK-011.json 의 unresolved_for_phase4 3건(state 저장소, 프록시 뒤 콜백 주소, PG 프로브)을 HITL#3 요약에 보인다"

decisions:
  - decision: "loginWithSocial 에 @Transactional 을 걸지 않는다"
    rationale: "같은 제공자 계정의 콜백이 동시에 오면 연결 INSERT 하나가 UNIQUE 위반(23505)이 된다. 한 트랜잭션 안에서는 그 실패가 이후 재조회까지 롤백 전용으로 만든다. 호출마다 따로 커밋하고, 위반이면 먼저 들어간 행을 다시 읽어 그 사용자로 로그인시킨다(SC-05)"
  - decision: "completeSocialSignUp 은 @Transactional, users→social_accounts 순서로 saveAndFlush 하고 각각의 UNIQUE 위반을 다른 409 로 바꾼다"
    rationale: "둘 중 하나가 실패하면 둘 다 만들지 않아야 한다(SC-12 행 수 단언). 예외가 던져지면 트랜잭션이 롤백된다. 이메일 존재 확인을 연결 확인보다 먼저 해 같은 토큰 재사용(SC-12 상태 2)이 EMAIL_ALREADY_EXISTS 가 된다"
  - decision: "SocialLoginConfig 를 새 설정 클래스로 분리했다(Plan 의 SecurityConfig 에서 이동)"
    rationale: "조건부 빈 4개와 FRONTEND_BASE_URL 검증이 커서 체인 구성(SecurityConfig)과 섞지 않는다. 제공자 환경변수 쌍이 하나도 없으면 빈이 하나도 만들어지지 않아 oauth2Login 도 달리지 않는다"
  - decision: "미등록 제공자 경로는 인가 요청 resolver 를 감싸 IllegalArgumentException 을 null 로 바꿔 404 로 보낸다"
    rationale: "Spring 기본은 InvalidClientRegistrationIdException(패키지 비공개, IllegalArgumentException 상속)을 500 으로 낸다. 시나리오 SC-16 은 foo·apple·대소문자 변형 모두 404"
  - decision: "pendingToken 필드에만 StrictStringDeserializer 를 붙였다 (전역 coercion 설정 대신)"
    rationale: "전역 설정은 TASK-009 엔드포인트(가입·로그인)의 숫자→문자열 동작까지 바꾼다. ADR-0019(조용한 변환 금지)에 따른 전역 정책은 Phase 5 의 ADR 후보로 남긴다"
  - decision: "AuthService 에 3-인자 생성자를 남겼다"
    rationale: "승인된 기존 테스트(AuthServiceSignUpFailureTest)가 사용한다. 승인된 테스트는 고치지 않는다"
  - decision: "구글은 profile,email 스코프(OIDC 아님)로 userinfo 를 읽는다"
    rationale: "Plan unresolved 제안대로 세 제공자가 같은 경로(OAuth2UserService → attributes → toProfile)를 탄다"
  - decision: "리팩토링: toLoginResponse 추출, JwtTokenProvider.decoderFor 일반화"
    rationale: "로그인 응답 조립과 디코더 생성이 두 곳에서 반복되던 것을 한 지점에 둔다. 리팩토링 뒤 전체 스위트 재실행 132/132"

attempts:
  - phase: "3"
    round: 1
    result: "통과 — 첫 실행 20건 중 18 통과, SC-11·SC-16 실패 → 구현 보완 후 132/132"
---
