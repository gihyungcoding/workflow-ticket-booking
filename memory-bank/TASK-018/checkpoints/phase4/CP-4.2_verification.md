---
status: ACTIVE
saved_at: 2026-11-09T08:00:00Z
summary: "Phase 4 1회차 FAIL — 테스트 665/665·lint·tsc·build·제약 통과, 리뷰 RC-2([correctness] 인증 화면 판별 정확 일치) + 시안 대조 차이 RC-3(D1~D5). 뿌리 3·변종 18(재현 10)."

next_steps:
  - "Phase 3 롤백 — RC-2 공통 지점 App.tsx · RC-3 화면별, not_probed 변종 전수. 변종마다 테스트 먼저"

decisions:
  - decision: "화면 확인은 DTO 스키마 모의 서버(:18081) + vite(:5175, VITE_API_BASE_URL) + out 정적 서버(:5176), 같은 출처 375px iframe 으로 시안과 나란히"
    rationale: ":8080 은 다른 프로젝트 — 건드리지 않는다. 세 서버는 PID 로 종료"
  - decision: "시안 차이(RC-3)를 WARN 이 아니라 FAIL 근거에 넣는다"
    rationale: "CLAUDE.md '화면은 시안과 같아야 한다'(ADR-0022), AC16. RC-2 로 어차피 Phase 3 롤백이라 한 번에 가져간다"
  - decision: "만료 제목은 승인 시나리오대로 유지 — D4 는 윗줄·라벨·아이콘만"
    rationale: "SC-23 의 heading 단언과 충돌하지 않는 범위"

attempts:
  - round: 1
    outcome: "FAIL — RC-2 재현 3, RC-3 재현 5"
    back_to: "phase3 (사용자 결정 2026-11-09 — 구현으로 롤백)"
---
