---
status: ACTIVE
saved_at: 2026-10-21T00:00:00Z
summary: "Phase 4 2차 판정 FAIL — 1차 결함(오류 디스패치 401 회귀·정규화 길이 오보)은 해결(develop 비교 16개 요청 전부 동일). 새 결함: name 의 NUL(\\u0000)이 PostgreSQL 에서 거부되어 catch 가 409 EMAIL_ALREADY_EXISTS 로 오보 — 직접 재현. AC 1~8 은 모두 PASS."

next_steps:
  - "HITL#3 결과: 시나리오로 롤백(Phase 2a) 선택됨(RETRY_SCENARIO 2/3). 시나리오 보완 → 2b → 3 → 4 재수행"
  - "수정 방향: catch 를 UNIQUE 위반(SQLState 23505, H2 동일)으로 한정 + name 의 NUL·제어 문자 사전 검증 400. 짝 없는 서로게이트 정책도 함께 결정"

decisions:
  - decision: "리뷰어가 '재현하지 못했다'고 한 NUL 결함을 직접 PostgreSQL 에서 재현한 뒤 FAIL 로 판정"
    rationale: "추론만으로는 결함이 아니다 — name=a\\u0000b 가 409 EMAIL_ALREADY_EXISTS 를 받고 DB 에 행이 없음을 확인했다. 1차의 İ 길이 오보와 같은 뿌리(모든 DataIntegrityViolationException → 409)"
  - decision: "이번 결함도 WARN 이 아니라 FAIL"
    rationale: "잘못된 상태 코드·메시지를 돌려주는 correctness 결함이고 같은 부류의 재발이다. 낮은 발생 가능성은 판정이 아니라 수정 우선순위의 근거"
  - decision: "H2 가 NUL 을 받아 주는 한 이 부류는 테스트가 못 잡는다 — 개선안으로 PostgreSQL 통합 테스트 도입을 회고에 올린다"
    rationale: "ADR-0006 이 이미 'Testcontainers 로 전환' 을 예고해 두었다. 이번 태스크에서 같은 이유로 두 번째 결함"

attempts:
  - phase: "4"
    round: 1
    outcome: "FAIL — AC8 회귀(프레임워크 오류 응답 400/405/415 → 401), 이메일 정규화 길이 오보"
    back_to: "2a"
    reject_type: "RETRY_SCENARIO (COVERAGE_INSUFFICIENT) 1/3"
  - phase: "4"
    round: 2
    outcome: "FAIL — name NUL → 409 오보(PostgreSQL 재현). 1차 결함은 해결"
    back_to: "2a"
    reject_type: "RETRY_SCENARIO (COVERAGE_INSUFFICIENT) 2/3"
---
