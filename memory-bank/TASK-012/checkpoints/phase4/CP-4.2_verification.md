---
status: ACTIVE
saved_at: 2026-10-27T00:00:00Z
summary: "[Phase 4 1라운드 FAIL, HITL#3 대기] 테스트 H2 145/145 · PostgreSQL DB 연결 클래스 113/113 · 린트·아키텍처 error 0. 그러나 리뷰어의 [correctness] 결함을 체인 프로브로 독립 재현: 운영 경로에서 id_token email_verified \"TRUE\"/\"True\"/\"tRue\" 가 자동 연결로 읽혀 승인된 SC-06 과 다르다(뿌리 1 · 변종 8, 재현 3). 실익 있는 악용 경로는 없음(애플 서명 토큰). R-1(세션 쿠키)은 별도 사용자 결정 대기."

next_steps:
  - "HITL#3 — FAIL 이라 승인 선택지 없음. 롤백 대상(구현 / 시나리오)과 R-1 처리 방침을 사용자에게 받는다"
  - "구현 롤백이면 Phase 3: RC-1 의 reproduced 변종 3개를 '새' 플로우 테스트로 먼저 실패시킨 뒤(승인된 테스트는 고치지 않는다) 공통 지점(애플 OIDC 디코더 팩토리의 email_verified 클레임 변환 제거)에서 고친다. DEV 의 rollback_fixes 에 {root_cause, variants, tests, fix_locus: common, sites} 기록"
  - "시나리오 롤백이면 Phase 2a: SC-06 의 \"True\"/\"TRUE\"/\"tRue\" 행을 운영 경로 의미(Spring 변환 이후)에 맞춰 고치고 SC-05 로 옮긴다 — HITL#1·#2 재승인 필요"
  - "재검증 시 이전 root_causes 를 그대로 이어받고, 수정 커밋도 최소 1회 다시 리뷰한다. PostgreSQL 프로브와 체인 프로브(email_verified 변종)를 다시 실행한다"
  - "Docker Desktop 은 검증을 위해 검증자가 켰다(원래 꺼져 있었다). 검증용 컨테이너 tb-verify-012 는 정리함"

decisions:
  - decision: "리뷰어 소견을 믿지 않고 실제 필터 체인으로 독립 재현했다"
    rationale: "리뷰어는 변환기를 단독 실행했고 \"True\" 는 추정만 했다. 승인된 SC-06 과 충돌하는 주장이라 판정(FAIL)을 내리기 전에 증거가 필요했다. 가짜 애플 서버로 서명한 id_token 의 email_verified 15종을 전체 체인에 통과시켜 \"TRUE\"/\"True\"/\"tRue\" 가 자동 연결, \" true\"·\"yes\"·\"1\"·정수 1 등은 가입 대기임을 확인. 프로브 파일은 삭제"
  - decision: "내 Phase 3 프로브가 이 결함을 놓친 이유: 변환기가 없는 순수 NimbusJwtDecoder 로 확인했다"
    rationale: "Phase 2b 의 하네스 프로브는 JWKS 서명 검증 가정만 확인했고 Spring 의 OidcIdTokenDecoderFactory 클레임 변환은 포함하지 않았다. Phase 5 회고 후보: '입력을 정규화하는 프레임워크 계층이 있으면 그 계층 이후의 값을 기준으로 시나리오·테스트를 쓴다 — attributes 를 직접 주입하는 핸들러 테스트는 변환 이전 값을 가정할 수 있다'"
  - decision: "PostgreSQL 전체 스위트 실행에서 10건이 실패한 것은 구현 결함이 아니라 환경변수 오버라이드가 만든 인공물로 판단하고, 제외 사유를 VERIFY 에 그대로 기록했다"
    rationale: "자체 H2 URL 을 넘기는 SpringApplicationBuilder 테스트 4개와 @DataJpaTest 슬라이스(PostgreSQL 전용 TIMESTAMPTZ 마이그레이션이 H2 에서 실행됨)가 원인임을 실패 메시지와 소스로 확인했다. 이 태스크는 domain·repository·resources 를 건드리지 않았다. 해당 클래스는 H2 전체 실행에서 통과"
  - decision: "R-1(세션 쿠키)은 결함이 아니라 사용자 결정 사항으로 둔다"
    rationale: "리뷰어 의견: 보안 결함이 아니라 가용성 위험(중간). 실제 HTTP 로 JSESSIONID 에 SameSite·Secure 가 없음을 확인했다. 선택지 (a) SameSite=None;Secure 설정 (b) 쿠키 기반 저장소+ADR 은 AC 밖의 배포 설정 결정이라 HITL#3 에서 묻는다"

attempts:
  - phase: "4"
    round: 1
    outcome: "Phase 4 FAIL — RC-1: 운영 경로의 Spring 클레임 변환 때문에 email_verified 대소문자 변형이 검증됨으로 읽힘(승인된 SC-06 과 불일치)"
---
