---
status: ARCHIVED
saved_at: 2026-10-27T00:00:00Z
summary: "[Phase 1 완료] route Backend. flows F1~F7 — AC 3개를 모두 덮는다(F1·F2·F7=AC1, F4=AC2, F3·F5=AC3, F6=AC2·3 거부 경로). 변경 대상 5건(기존 4·신규 1), 재사용 8. AuthService 변경 없음 — 애플은 SocialProfile 만 만들어 기존 loginWithSocial 에 합류. unresolved 14건 중 2a 확정 필요 4건(이름 대체·이름 합성/거부·설정 일부 누락·user 출처 제한), Phase 3 설계 필요 1건(state 저장과 form_post). check_plan 통과."

next_steps:
  - "Phase 2a — wf-scenario. 환경변수 변형 × email_verified 변형 13+ × 이름 변형 × 판정 분기 → 기준선(8~12) 초과 예상, 표로 묶고 근거를 decisions 에 남기고 사용자에게 알린다"
  - "2a 에서 확정: (1) 신규인데 이름이 없을 때 대체 문자열 (2) firstName/lastName 합성 규칙과 user JSON 구조 이상 시 실패 여부 (3) 애플 설정 일부만 있을 때 미등록 vs 기동 실패 (4) user 파라미터는 apple+POST 에서만 읽기"
  - "email_verified 변종은 양방향(잘못 true·잘못 false)을 모두 시나리오로 대본다 — TASK-011 인계 RC-1"
  - "Phase 3 에서 정할 것: 인가 요청 state 저장(세션 vs 쿠키, form_post 교차 사이트 POST) — 쿠키 저장소를 택하면 ADR. Spring Security 7.1.1 API 확인 항목은 PLAN unresolved 참고"
  - "wf-scenario 의 새 지침(PR #23 미머지, 수동 적용): feature 문서 상태 표를 covers 와 대조, 정책이 바뀐 롤백이면 기존 시나리오 전수 대조"

decisions:
  - decision: "AuthService·Repository·스키마를 바꾸지 않고 애플 고유 관심사를 모두 config 에 둔다"
    rationale: "loginWithSocial 이 SocialProfile 하나로 재로그인·자동 연결·pendingToken 을 이미 판정한다(AC3 의 '동일하게 따른다'는 재사용으로 충족). Client Secret·토큰 교환·form_post·클레임 정규화는 OAuth 프로토콜 관심사라 architecture.md §2 의 Service 에 둘 이유가 없다. 새 계층·컴포넌트 없음"
  - decision: "이름은 별도 필터 없이 성공 핸들러가 콜백 요청의 user 파라미터에서 읽는다"
    rationale: "Spring 로그인 필터가 폼 본문을 파싱한 뒤라 같은 요청에서 getParameter 로 읽힌다. 필터·세션 저장소를 늘리지 않고 기존 핸들러 테스트 패턴(MockHttpServletRequest)으로 검증할 수 있다. user 는 서명되지 않은 값이라 이름만 쓰고 이메일은 무시한다"
  - decision: "PostgreSQL 통합 테스트는 만들지 않고 Phase 4 의 실제 PostgreSQL 프로브(애플 이름 100자·4바이트 문자)를 필수 증거로 한다"
    rationale: "신규 wf-plan 지침에 따라 사유를 unresolved 에 적었다. 마이그레이션·SQL·제약 변경이 없고 DB 에 닿는 코드는 TASK-011 이 이미 PG 로 프로브한 경로 그대로다. 새로 들어가는 외부 문자열은 이름뿐이며 SocialInputRules 가 앞서 거부한다. Testcontainers 는 ADR-0006 시점까지 미도입"
  - decision: "ADR-0021(fragment) 리다이렉트 형식 변경은 이 태스크에서 하지 않는다"
    rationale: "acceptance_criteria 밖이고 ADR-0021 이 후속 태스크로 정했다. 같은 핸들러를 쓰므로 후속 태스크가 바꿀 때 애플도 함께 따른다. 조용히 넓히지 않는다"
  - decision: "AC 역방향 확인: F6(규칙 밖 입력 거부)·F7(애플 설정)은 AC 밖으로 넓힌 것이 아니라 AC 구현의 필수 부분"
    rationale: "F7 은 Client Secret 을 쓸 제공자 등록 없이는 AC1 이 성립하지 않고, F6 은 AC2·3 의 입력 경계(ADR-0019)다. 프론트 화면·소유권 검사·fragment 변경은 넣지 않았다 (Plan MUST 6)"

attempts: []
---
