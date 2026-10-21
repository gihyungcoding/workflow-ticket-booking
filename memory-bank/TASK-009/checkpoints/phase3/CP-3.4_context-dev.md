---
status: ACTIVE
saved_at: 2026-10-21T00:00:00Z
summary: "Green — 전체 105/105 통과(신규 16 + 기존 89), spotlessCheck·아키텍처 error 0. 변이 8건 전부 사망."

next_steps:
  - "Phase 4 — wf-verify. docker postgres 로 V3 실제 마이그레이션·가입·로그인 확인(H2 에서는 V3 가 실행되지 않음). code-reviewer 호출 시 JPA 지시 포함"
  - "Ship 에서 JWT_SECRET 이 Render 에 먼저 들어가야 한다는 점을 사람에게 알린다"

decisions:
  - decision: "Jackson 3 에서 enum 숫자 거부는 spring.jackson.datatype.enum.fail-on-numbers-for-enums=true"
    rationale: "처음 쓴 spring.jackson.deserialization.* 는 Jackson 3 에서 EnumFeature 로 옮겨져 컨텍스트가 뜨지 않았다. SC-04 의 role=1 이 201 이던 위험을 설정으로 해결"
  - decision: "이메일 trim 은 DTO 컴팩트 생성자, 소문자 정규화와 저장은 AuthService"
    rationale: "@Email 이 앞뒤 공백을 먼저 거부해 SC-01/03/07 의 공백 행이 400 이 됐다. 검증 전에 걷어내야 한다"
  - decision: "JwtAuthenticationFilter 를 빈으로 등록하지 않고 SecurityConfig 가 체인에 직접 끼운다"
    rationale: "서블릿 필터 빈은 Boot 가 보안 체인과 별도로 한 번 더 등록한다"
  - decision: "JWT 검증 실패는 인증을 비워 두고 체인을 계속한다 — 401 은 EntryPoint 가 낸다"
    rationale: "공연 API(permitAll)는 만료·위조 토큰이 실려 와도 통과해야 한다(SC-15)"
  - decision: "리팩토링은 하지 않았다"
    rationale: "중복처럼 보이는 이메일 trim 의 두 곳은 역할이 다르다(검증 전 공백 제거 vs 저장 표기 통일)"
  - decision: "변이 검증 첫 시도의 사고 — 구현을 먼저 커밋한 뒤 변이"
    rationale: "미커밋 파일에는 git checkout 이 통하지 않고, 커밋된 스켈레톤은 스켈레톤으로 되돌려졌다. 복원 후 105건 통과 확인"

attempts:
  - phase: "3"
    round: 1
    result: "통과"
    reason: "1차 실행 101/105 — 이메일 공백 400, role 숫자 201. 수정 후 컨텍스트 로드 실패(Jackson 속성 이름) → 정정 후 105/105"
---
