---
status: ACTIVE
saved_at: 2026-10-22T03:40:00Z
summary: "[작성] SCENARIO_TASK-010 20건(happy 8 / error 6 / boundary 5 / regression 1) 작성, AC 6/6 매핑. 독립검증 대기."

next_steps:
  - "scenario-validator 호출 → VALIDATION_TASK-010.json 저장"
  - "HITL#1 승인 후 Phase 2b"

decisions:
  - decision: "PLAN unresolved 3건을 시나리오에서 정했다 — 가입 성공 안내 없음(이동만), 복원 중 헤더는 아무것도 그리지 않음, 72바이트 초과 비밀번호는 클라이언트가 검증하지 않고 서버 400 을 일반 문구로 표시"
    rationale: "AC 가 이동만 요구한다 / 깜빡임 방지 / 서버가 최종 판단(architecture.md §2)"
  - decision: "복원 실패 시 401 이면 토큰 삭제, 500·네트워크 오류·비정상 200 이면 토큰 유지"
    rationale: "일시 오류로 사용자를 로그아웃시키지 않는다. 401 은 토큰 무효의 증거"
  - decision: "화면 문구를 시나리오 상단 표를 정본으로 고정"
    rationale: "design.md §5 에 인증 문구가 없다. Red 테스트가 문구를 단언한다"
  - decision: "응답 이상 케이스(SC-08·13·18)는 표로 묶었다"
    rationale: "같은 응답 해석 경로, 같은 Then. TASK-009 회고의 '응답 code 분기' 경계값 행을 적용했다 (다른 code·빈 바디·비 JSON)"
  - decision: "문자열 필드 전수(NUL·서로게이트)는 프론트에서 재검증하지 않는다"
    rationale: "서버가 거부한다(TASK-009 RC-1). 화면은 입력을 변환하지 않고 보내며(SC-06) 400 은 일반 문구(SC-08·13)"

attempts: []
---
