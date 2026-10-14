---
status: ACTIVE
saved_at: 2026-10-14T09:00:00Z
summary: "Phase 4 1차 — FAIL. 테스트 89/89·린트·제약 통과, 그러나 SC-04/08 통합 테스트가 가드 OFF + @Transactional 로 가짜 통과(R1). 롤백 선택 대기."

next_steps:
  - "사용자 롤백 결정: Phase 2b(테스트 재작성, RETRY_RED_CODE) 후 Phase 3 재검증"
  - "R3(fail-open 정책)·R4(Xmx/USER) 는 사용자 결정 후 Phase 3 에 반영"

decisions:
  - decision: "R1 을 FAIL 로 판정"
    rationale: "AC '401 이면 DB 변경 없음' 을 증명하는 통합 테스트가 없고 현 테스트는 가드를 켜지 않는다. 리뷰어 주장 중 가드 설정 부재는 직접 확인했다"
  - decision: "R2(필터 순서)는 WARN"
    rationale: "리뷰어는 추정이었으나 프로브에서 401 에 ACAO 가 붙는 것을 실측. 다만 테스트로 고정되어 있지 않다"

attempts: []
---
