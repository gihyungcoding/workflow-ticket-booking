---
status: ACTIVE
saved_at: 2026-10-22T02:00:00Z
summary: "[5차 WARN] 4차 결함(로그인 password 짝 없는 서로게이트 → '?' 치환 매칭 200)은 PostgreSQL 에서 400 으로 해소, 모든 문자열 입력 HTTP 전수 프로브·리뷰어 5차 모두 새 변종 없음. 테스트 112/112, 아키텍처 error 0, 공연 API 오류 응답 develop 과 동일. 남은 WARN: PostgreSQL 통합 테스트 부재·U+FFFE/bidi 위장 개선 제안. HITL#3 대기. [이전 요약] [4차 FAIL] 3차 결함(이메일·password·로그인 이메일 서로게이트)은 PostgreSQL 에서 해소. 새 변종: 로그인 password 의 짝 없는 서로게이트가 '?' 로 치환돼 저장된 '?' 비밀번호와 BCrypt 매칭(200) — HTTP 재현. 같은 뿌리(RC-1)로 4라운드째, later 변종 4건. 테스트 111/111, 아키텍처 error 0, 공연 API 16건 DIFF 0. [이전] Phase 4 3차 판정 FAIL — 2차 결함(name NUL 등)은 PostgreSQL 에서 해소(name 거부·동시 가입 409 수렴·develop 비교 16건 동일). 새 변종: 이메일 로컬파트의 짝 없는 서로게이트가 @Email 을 통과해 201 로 가입되나 DB 에는 a?b@x.com 으로 치환 저장, 다른 입력 a?b@x.com 은 409 오보(HTTP 로 직접 재현). 뿌리 RC-1 은 2차와 같다(입력 검증이 DB 가 무손실 저장 못 하는 문자열을 막지 않음). AC 1~8 PASS, 테스트 109/109, 아키텍처 error 0"

next_steps:
  - "HITL#3 APPROVE 완료(WARN, verified_commit efcad41) — Phase 5 회고 진행: later 변종 8건(같은 뿌리 5라운드)·PostgreSQL 통합 테스트 부재·프로브 고정 테스트 부재를 rule_proposals 로"
  - "HITL#3 승인 시 verified_commit 기록 후 Phase 5 회고 — later 변종 8건(같은 뿌리 5라운드)과 PostgreSQL 통합 테스트 부재를 rule_proposals 로"
  - "HITL#3 대기 — RETRY_SCENARIO 3/3 소진이라 규칙상 ROLLBACK_PLAN(Phase 1)으로 상승. 설계에서 문자열 입력 정책(입력→검증→비교→저장 값 동일성)과 PostgreSQL 통합 테스트 도입(ADR-0006)을 정한다"
  - "HITL#3 결과: 시나리오로 롤백(Phase 2a) 선택됨(RETRY_SCENARIO 3/3 — 소진, 다음 FAIL 은 Phase 1 로 상승). 문자열 입력 전체(email·name·password)의 DB 무손실 저장 변종을 한꺼번에 덮는다"
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
    back_to: "2a"
    reject_type: "RETRY_SCENARIO (COVERAGE_INSUFFICIENT) 3/3"
---
