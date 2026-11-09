---
status: ARCHIVED
saved_at: 2026-11-06T08:00:00Z
summary: "PLAN_TASK-018.json — route Frontend, flows 9건이 AC 16건을 전부 덮음. input_fields 20 → 칸 162(첫 적용)."

next_steps:
  - "Phase 2a — coverage_matrix.py --skeleton 으로 칸을 1차 작성에서 전부 닫는다. 문구 표(시안 + 새 해요체)를 SCENARIO 상단에"

decisions:
  - decision: "자동 로그인 실패 시 /login 으로 보내고 맨 위 '가입은 끝났어요. 로그인해 주세요' + 이메일 칸 채움"
    rationale: "사용자 결정(2026-11-06). 계정은 이미 있어 가입 화면 재제출은 409 가 된다"
  - decision: "인증 화면 5종에서만 전역 AppHeader 숨김"
    rationale: "사용자 결정(2026-11-06). 시안에 없다"
  - decision: "역할 선택 안내는 토큰 디코드(readPendingSignup) 대신 POST /social/pending 응답으로"
    rationale: "feature 문서 §2-3·§3-4. 만료 시각도 같은 응답에서 온다"
  - decision: "만료 판정은 브라우저 시계, 서버 401 이 최종 — ADR-0024 오프셋 미적용"
    rationale: "카운트다운을 보이지 않고 서버가 만료 토큰을 거부한다"
  - decision: "복사 실패 상태(§2-5)를 범위에 넣는다"
    rationale: "AC 엔 없지만 같은 화면의 상태로 feature 문서에 있다 — 넓힌 범위로 기록"
  - decision: "ADR-0021 fragment 미구현(query 사용)은 손대지 않음 — 회고 drift"
    rationale: "AC 밖, 백엔드 변경 필요"

attempts: []
---
