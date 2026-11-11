---
status: ACTIVE
saved_at: 2026-11-11T00:00:00Z
summary: "SCENARIO_TASK-022 — 12건(happy 4·error 4·regression 4), AC 8/8, 입력 경계 30/30. 독립검증 대기"

next_steps:
  - "scenario-validator 1회차"

decisions:
  - decision: "새로 쓴다 — 기존 시나리오를 확장하지 않는다"
    rationale: "이 태스크의 대상(역할 인가·소유권)을 다룬 기존 시나리오가 없다"
  - decision: "/api/performances/** 는 GET 만 공개, 그 밖의 메서드는 모두 ROLE_ORGANIZER"
    rationale: "DELETE·PATCH 가 토큰 없이 405 로 떨어지던 회귀 표는 쓰기 행에 토큰을 실어 숫자를 유지한다(SC-12). 미지원 메서드를 따로 열어 두는 규칙을 만들지 않는다"
  - decision: "403 메시지는 '주최자만 이용할 수 있습니다'"
    rationale: "기능 문서 §3-3 의 FORBIDDEN_ROLE. 프론트 안내 문구(TASK-024)와 같은 뜻"
  - decision: "승인 테스트 변경 6건을 시나리오 MD 끝 표로 명시"
    rationale: "PLAN unresolved — 2b 통합 승인에 함께 올린다"
  - decision: "PLAN inputs 의 제약 대조: Authorization(SC-04·06), role(SC-05), sub(기존 JwtTokenFormatTest — 이 태스크에서 바꾸지 않음), id(SC-07), WRITE_GUARD 환경변수(SC-09), 본문(기존 테스트)"
    rationale: "Step 2 입력 제약 대조"

attempts: []
---

# CP-2.2 시나리오 — TASK-022

- `workflow_design/05_scenario/SCENARIO_TASK-022.md` (SoT) · `.json`
