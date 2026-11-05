---
status: ACTIVE
saved_at: 2026-11-05T14:00:00Z
summary: "Phase 4 2회차 WARN — RC-1 해소, 경고 2(앱바 긴 제목 높이 넘침·Sheet --c-track). HITL#3 대기. (1회차: FAIL — 뿌리 1 · 변종 5 (재현 2). 배지 잘림·neutral 배지 투명·토스트 클릭 막힘. 롤백 대상 사용자 결정 대기."

next_steps:
  - "롤백 — neutral 배지 기대값(#F1F2F6)이 승인된 시나리오 규약 표에 박혀 있어 2a 에서 바꿔야 한다. 변종 5개 전부를 시나리오로"

decisions:
  - decision: "8080 은 다른 프로젝트 dev 서버가 점유 — 끄지 않고 모의 서버를 18080, vite 를 5179 에 임시 설정으로 띄움"
    rationale: "사용자 프로세스를 건드리지 않는다. 서버는 PID 로만 종료"

attempts:
  - round: 1
    result: "FAIL — 테스트 287 통과에도 화면에서 배지 잘림·투명 재현"
---

attempts_round2: "WARN — 375px iframe 로 실제 뷰포트 확인(창 크기 조절이 안 돼서)"
