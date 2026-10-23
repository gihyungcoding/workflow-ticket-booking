---
status: ACTIVE
saved_at: 2026-10-23T10:10:00Z
summary: "[1차 FAIL] 테스트 132/132·spotlessCheck·아키텍처 error 0, 실제 PostgreSQL 프로브(V4·UNIQUE·FK·동시성·문자열 경계) 통과. code-reviewer 가 뿌리 2건 지적 — RC-1(이메일 소유가 증명되지 않은 계정이 자동 연결 대상: 이메일 가입 선점, 미검증 소셜 가입 선점, 네이버 계정) 은 임시 테스트로 변종 4건 재현, RC-2(외부 문자열 규칙 불일치: 소셜 이메일 형식·서식 문자, 위조 pendingToken 의 DB 오류) 는 PG·JDK 프로브로 재현. 뿌리 2 · 변종 8, 재현 7. HITL#3 대기(FAIL 은 롤백만 가능)."

next_steps:
  - "HITL#3 — FAIL 이라 승인 선택지 없음. 롤백 지점(Phase 2a 정책 결정 vs Phase 3)과 자동 연결 정책을 사용자에게 묻는다"
  - "롤백 시 RC-1·RC-2 변종 전부를 한 번에 가져간다 (root-cause-sweep): emailVerified 를 pendingToken·users 에 기록, 미검증 소유 계정은 자동 연결 대상 제외, 이메일 규칙(형식·서식 문자) 공통 지점화, verifyPendingToken 재검증"
  - "Phase 5 후보: 토큰 쿼리 문자열 노출(F-5, 명세 §4) ADR, 프록시 뒤 콜백 주소·state 저장소 (DEV unresolved_for_phase4)"

decisions:
  - decision: "판정을 FAIL 로 한다 (WARN 예외 승인이 아니라)"
    rationale: "RC-1 은 보안 결함이고 임시 테스트로 재현됐다(PROBE A·B·C). 선점 계정으로 피해자 소셜 로그인 뒤에도 접근 권한이 남는다. FAIL 을 경고로 낮추지 않는다"
  - decision: "리뷰어가 앱을 띄우지 않고 낸 변종은 내가 임시 테스트로 재현했다"
    rationale: "probe 필드를 사실대로 채우기 위해. 임시 파일은 실행 후 삭제했고 git status 가 깨끗하다"
  - decision: "위조 pendingToken 의 DB 오류(RC-2)는 심각도 낮음으로 기록한다"
    rationale: "위조에 JWT_SECRET 이 필요하고, 유출되면 access 토큰도 위조된다. 정상 경로는 toProfile 이 먼저 거부한다. 다만 공통 지점(PendingSignup 생성 시 규칙 재적용)에서 막는 편이 낫다"

attempts:
  - phase: "4"
    round: 1
    outcome: "FAIL — 뿌리 2 · 변종 8 (재현 7, 미프로브 1)"
---
