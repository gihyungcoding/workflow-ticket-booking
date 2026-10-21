---
status: ACTIVE
saved_at: 2026-10-22T00:50:00Z
summary: "Phase 4 3차 판정 FAIL — 2차 결함(name NUL 등)은 PostgreSQL 에서 해소(name 거부·동시 가입 409 수렴·develop 비교 16건 동일). 새 변종: 이메일 로컬파트의 짝 없는 서로게이트가 @Email 을 통과해 201 로 가입되나 DB 에는 a?b@x.com 으로 치환 저장, 다른 입력 a?b@x.com 은 409 오보(HTTP 로 직접 재현). 뿌리 RC-1 은 2차와 같다(입력 검증이 DB 가 무손실 저장 못 하는 문자열을 막지 않음). AC 1~8 PASS, 테스트 109/109, 아키텍처 error 0"

next_steps:
  - "HITL#3 결과에 따라 롤백 — RETRY_SCENARIO 는 이미 2/3 이라 이번이 3/3, 이후 실패 시 Phase 1 로 상승"
  - "수정 방향: 이메일에도 \\p{Cs} 거부 검증, isUniqueViolation 두 경로(23505→409, 그 외 재던짐) 서비스 테스트로 고정, PostgreSQL 통합 테스트 도입 검토(ADR-0006)"

decisions:
  - decision: "리뷰어 보고(JDBC·Validator 단위 재현)를 HTTP 로 직접 재현한 뒤 FAIL 로 판정"
    rationale: "a\\ud800b@x.com 가입 201, DB 에 a?b@x.com(length 9), a?b@x.com 가입 409 를 확인. 리뷰어는 앱 경유를 못 했다고 했다"
  - decision: "WARN 이 아니라 FAIL"
    rationale: "잘못된 상태 코드·응답과 저장값 불일치인 correctness 결함이고 1·2차와 같은 뿌리다. 발생 가능성이 낮은 것은 수정 우선순위의 근거"

attempts:
  - phase: "4"
    round: 3
    outcome: "FAIL — 이메일 짝 없는 서로게이트(RC-1 변종, origin: later). 2차 결함은 해소"
    back_to: "미정(HITL#3)"
---
