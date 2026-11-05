---
status: ARCHIVED
saved_at: 2026-11-04T16:00:00Z
summary: "롤백 재작업 Red 16건 — 통합 재승인 완료(2026-11-05). V11 은 범위 밖(별도 태스크 후보)."

next_steps:
  - "Phase 3 — 압축 형식 검사를 verifyAccessToken·verifyPendingToken 공통 지점으로, 페이로드 중복 키 거부"

decisions:
  - decision: "컴파일 스켈레톤 없이 HTTP·JDBC 수준에서만 단언"
    rationale: "프로덕션 타입에 컴파일 의존이 없어 스켈레톤 순수성 문제가 생기지 않는다"
  - decision: "난수원 하네스 계약: nextInt(32) → nextInt(10000), RandomGenerator 타입 주입"
    rationale: "「뽑힌 값」을 결정적으로 재현. Phase 3 구현이 따라야 한다(TEST json harness_contract)"
  - decision: "SC-14 는 행마다 DB 를 비운다"
    rationale: "1차 실행에서 한 행(NUL 토큰)이 가입을 만들어 뒤 행 전부가 행 수로 실패 — 행 독립성 확보"

attempts:
  - round: 1
    result: "하네스 결함 — @Primary 빈 2개로 SequenceRandom 주입 모호 → 1개로 합침"
  - round: 2
    result: "Red 확인 — 14/14 실패, 기존 146 통과. SC-14 에서 기존 결함 발견: 토큰 끝 NUL·서로게이트 허용"
---

## Phase 4 롤백 재작업 (2026-11-05)
- 새 테스트: TokenFormatApiTest(SC-15), JwtTokenFormatTest(SC-16), PendingSignupApiTest SC-09·14 행 확장. 도우미: PendingTokenTestHelper.signedJson·rawJson·insertIntoSegment
- Red: 4 실패 — accessToken 형식 검사 없음(SC-15·16), 중복 키 거부 없음(SC-09·14)
- 변이 M1~M3 로 이미 통과하는 행이 실제 가드를 지킴을 확인, 원복 확인
