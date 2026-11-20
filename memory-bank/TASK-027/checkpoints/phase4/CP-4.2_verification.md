---
status: ARCHIVED
saved_at: 2026-11-20T08:00:00Z
summary: "Phase 4 2회차 WARN → 예외 승인(verified_commit da2f1b1). 233 통과, 리뷰 결함 0, AC 10/10, 수용 위험 2(ADR-0035)"

next_steps:
  - "Phase 5 — wf-reflect"

decisions:
  - decision: "HITL#3 예외 승인(사용자)"
    rationale: "WARN 항목을 exceptions 에 기록 — STARTTLS 문구는 TASK-028 에서"
  - decision: "FAIL 로 판정 — 낮음이라도 [correctness] 재현"
    rationale: "wf-verify Step 5 — 결함은 FAIL. 시나리오가 호스트 표기 칸을 n/a 로 닫아 Phase 3 로 바로 갈 수 없다"
  - decision: "[security] 낮음(STARTTLS 안내·mail.debug 안내)은 롤백 때 체크리스트 문구로 함께 반영 후보"
    rationale: "코드 결함이 아니라 운영 안내"

attempts:
  - n: 2
    result: WARN
    reason: "RC-2 later 해소, 새 결함 0. WARN — 수용 위험 2, STARTTLS 문구 정밀도, NBSP·test-connection 참고, 시간 단언, AUTH-001 승격 기한"
  - n: 1
    result: FAIL
    reason: "RC-2 later — SPRING_MAIL_HOST 공백(재현). 뿌리 4 · 변종 15 · 재현 14"
---

# CP-4.2 검증 — TASK-027

- `workflow_design/07_verify/VERIFY_TASK-027.json`
