---
status: ARCHIVED
saved_at: 2026-11-04T16:00:00Z
summary: "Red 14건 — 시나리오+Red 통합 승인 완료(HITL#1·2, 2026-11-04). 경고 V6·V9 는 그대로 수용."

next_steps:
  - "Phase 3 — wf-develop 로 Green 구현 (하네스 계약 준수, SC-14 기존 결함 포함)"

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
