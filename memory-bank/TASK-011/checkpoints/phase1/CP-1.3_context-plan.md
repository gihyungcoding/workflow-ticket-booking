---
status: ARCHIVED
saved_at: 2026-10-23T02:00:00Z
summary: "[Phase 1 완료] route Backend. flows F1~F9 — AC 7개를 F1~F7 이 덮고, F8(소셜 전용 계정 이메일 로그인 401)·F9(제공자 실패 리다이렉트)는 AC 밖이지만 사용자 확인으로 포함. 변경 대상 20건(기존 9·신규 11), 재사용 9. unresolved 15건 중 PostgreSQL 통합 테스트는 '실제 PG 프로브만(Testcontainers 미도입)'으로 결정. check_plan 통과."

next_steps:
  - "Phase 2a — wf-scenario. 제공자 3 × 판정 분기 × 외부 입력 경계 → 기준선(8~12) 초과 예상, 표로 묶고 근거를 decisions 에 남긴다"
  - "2a 에서 확정할 unresolved: 구글 스코프(openid 여부)·제공자 등록 환경변수 처리(미설정·빈 문자열·공백만)·네이버 미제공 플래그·이메일/이름 누락·name 길이 초과·/complete 충돌 응답·pendingToken 오류 코드명·리다이렉트 규약"

decisions:
  - decision: "PostgreSQL 통합 테스트는 Testcontainers 를 도입하지 않고 Phase 4 의 실제 PostgreSQL HTTP 프로브를 필수 항목으로 한다"
    rationale: "사용자 선택(AskUserQuestion). 테스트 인프라 변경 없이 범위를 작게 두되 H2 가 못 잡는 V4·유니크·문자열 저장을 실제 엔진에서 확인한다. 자동 회귀 테스트가 없다는 한계는 VERIFY 에 기록. TASK-009 가 같은 방식으로 4번 FAIL 하며 잡았던 결함 부류다"
  - decision: "F8(소셜 전용 계정의 이메일 로그인은 401)과 F9(제공자 실패·이상 속성 리다이렉트)를 범위에 포함한다"
    rationale: "사용자 선택(AskUserQuestion). F8 은 V4 가 password_hash 를 nullable 로 바꾸는 귀결의 회귀 방지, F9 는 implementation_spec 의 OAuth2LoginFailureHandler 와 users.email·name NOT NULL 의 귀결. AC 를 조용히 넓히지 않고 사용자 확인을 받았다"
  - decision: "판정은 Service(AuthService), 리다이렉트는 config 핸들러, 매핑은 attributes 맵을 받는 순수 함수"
    rationale: "architecture.md §2 — Service 는 HTTP 관심사를 갖지 않는다. 제공자 호출 없이 AC5(제공자별 매핑 단위 테스트)를 쓸 수 있다"
  - decision: "네이버는 이메일 검증 플래그가 없으므로 AC3 문구대로 자동 연결하지 않는다"
    rationale: "AC 를 문자 그대로 따른다. ADR-0014 의 '4개 제공자 모두 플래그 제공' 가정과 어긋나는 점은 Phase 5 drift 후보로 남긴다"
  - decision: "의존성은 spring-boot-starter-security-oauth2-client"
    rationale: "Boot 4.1.1 에서 Maven Central 에 존재함을 확인(옛 이름 spring-boot-starter-oauth2-client 도 있으나 -security- 가 정식). ADR-0013 이 이미 정한 구성이라 새 컴포넌트가 아니다"
  - decision: "AC 역방향 확인: flows 가 AC 밖으로 넓힌 것은 F8·F9 둘뿐이고 둘 다 사용자 확인을 받았다. 애플·프론트·소유권은 넣지 않았다"
    rationale: "Plan MUST 6"

attempts:
  - phase: "1"
    round: 1
    outcome: "Plan 완료 — check_plan 통과"
---
