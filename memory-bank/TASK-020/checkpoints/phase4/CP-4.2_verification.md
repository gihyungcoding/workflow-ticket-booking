---
status: ACTIVE
saved_at: 2026-11-16T00:00:00Z
summary: "Phase 4 1회차 FAIL — 테스트 1029/1029·린트 0·빌드·아키텍처 error 0·화면 6개 상태 PASS. 리뷰어 [correctness] 3건(뿌리 RC-2 잔여·RC-3·RC-4)"

next_steps:
  - "롤백 확정: 시나리오로(2a) — 사용자 선택(2026-11-16). RC-3(시계 측정 시간 제한)·RC-4(Date.parse 관대함)·RC-2 잔여(비동기 popstate)는 시나리오에 없는 동작일 수 있다 — 2a 면 변종 전부를 한 번에 가져간다"
  - "다시 Phase 4 로 오면 이전 root_causes 를 리뷰어에 넘기고, not_probed 변종은 Phase 3 테스트로 프로브한 뒤 evidence 에 식별자를 적는다"

decisions:
  - decision: "status FAIL — 리뷰어 [correctness] 3건"
    rationale: "Phase 4 는 코드를 고치지 않는다. 뿌리 4(변종 9), 재현 RC-4·RC-2 일부"
  - decision: "화면 확인은 모의 서버 + Vite(5190, VITE_API_BASE_URL=4020)로 했고 PID 로만 종료했다"
    rationale: "8080 의 다른 java 프로세스는 건드리지 않았다"
