---
status: ACTIVE
saved_at: 2026-10-26T05:30:00Z
summary: "[Phase 4 FAIL 롤백 후 재진입] Green 완료 — 전체 133/133(새 21 + 기존 112), spotlessCheck·아키텍처 error 0. RC-1(검증된 계정에만 자동 연결, users.email_verified·pendingToken emailVerified)과 RC-2(SocialInputRules 공통 규칙, toProfile·verifyPendingToken 공유)를 공통 지점에서 고쳤다. 첫 실행 132/133 — SC-05 Given 이 ADR-0020 과 충돌해 사용자 승인(HITL#1·#2)으로 Given 만 정정했다."

next_steps:
  - "Phase 4 재검증 — wf-verify. RC-1·RC-2 변종 전부 재프로브, 실제 PostgreSQL 로 V4 수정본·loginWithSocial(자동 연결·동시 콜백) 프로브"
  - "code-reviewer 호출 시 root_causes 이어받기(origin: later 규칙), 수정 커밋 재리뷰, CLAUDE.md JPA 지시 포함"
  - "DEV unresolved_for_phase4 를 HITL#3 요약에 보인다"

decisions:
  - decision: "문자열 규칙을 config.SocialInputRules 한 곳에 두고 toProfile·verifyPendingToken 이 같이 쓴다"
    rationale: "RC-2 변종 3종의 뿌리가 경로별 규칙 불일치다. 필드마다 따로 붙이면 빠뜨린 한 곳이 다음 FAIL 이 된다. 규칙 위반은 null 로 돌려 호출 쪽이 OAuth2AuthenticationException(제공자 응답) 또는 빈 Optional→401(토큰)로 바꾼다"
  - decision: "이메일은 NBSP 등 공백류(Z*)와 서식 문자(Cf)를 주소 안에서 거부, 이름은 기존 규칙(제어문자·서로게이트)만"
    rationale: "눈으로 구별되지 않는 다른 이메일 계정을 막는 것이 목적이다. 이름은 공백이 정상이고 SC-07/10 이 요구하지 않는다"
  - decision: "V4 마이그레이션 파일에 email_verified 컬럼을 추가했다 (V5 가 아니라)"
    rationale: "V4 는 병합·배포된 적이 없어 체크섬 충돌 대상이 없다. 로컬에 V4 를 적용해 둔 DB 가 있다면 재생성이 필요하다"
  - decision: "자동 연결 조건에 대상 계정의 email_verified 를 추가 (User::isEmailVerified 필터)"
    rationale: "ADR-0020. 미검증 대상이면 pendingToken → /complete 가 409 EMAIL_ALREADY_EXISTS"
  - decision: "SC-05 Given 정정은 승인된 테스트 수정이라 사용자에게 물어 처리했다"
    rationale: "wf-develop: 테스트가 잘못됐으면 임의로 고치지 않고 Phase 2 로 알린다. 사용자가 'Given 만 수정'을 택했고 HITL#1·#2 를 이 건에 한해 다시 받았다. 변이(복구 제거)로 가짜 통과가 아님을 확인"

attempts:
  - phase: "3"
    round: 2
    result: "통과 — 재진입 첫 실행 132/133(SC-05) → 시나리오·테스트 Given 정정 승인 후 133/133"
---
