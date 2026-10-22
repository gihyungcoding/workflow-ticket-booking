---
status: ACTIVE
saved_at: 2026-10-22T03:40:00Z
summary: "[3차 검증 FAIL — 재검증 3/3 소진] V11: 로그인 비밀번호 공백만·가입 비밀번호 확인 공백만 경계 2건 누락. 사용자 결정 대기(Phase 1 롤백 제안 vs 2건 보완 후 예외 재검증). [3차 작성] 2차 독립검증 FAIL(V11: 빈 토큰·문자열 전수 전송·201 본문) 반영 — 31건, 마지막 재검증(3/3) 대기. [2차 작성] 1차 독립검증 FAIL(V11: 재제출 시 오류 초기화·72바이트 경계 누락) 반영 — 28건(happy 9 / error 8 / boundary 10 / regression 1), When 단일화, Plan 에 trim·순환방지·복원실패 보존 규칙 반영. 재검증 대기. [1차] 20건 작성 → FAIL 1·WARN 5."

next_steps:
  - "사용자 결정: 재검증 3/3 소진 — Phase 1 롤백(규칙) 또는 V11 2건만 보완 후 추가 검증(예외)"
  - "HITL#1 승인 후 Phase 2b"

decisions:
  - decision: "가입 성공 판정은 상태 201 만 본다(본문 무시, SC-31). 빈 토큰·공백 토큰은 복원 요청 없이 비로그인(SC-24). 문자열 전수는 가입·로그인 전 필드 표로 전송 값 동일성을 단언(SC-08·09)"
    rationale: "VALIDATION 2차 V11"
  - decision: "SC-19 로 재제출 시 오류 문구 초기화·전환을 추가하고, 72바이트 경계는 클라이언트가 막지 않기로 확정(가입 서버 400, 로그인 서버 401 INVALID_CREDENTIALS)"
    rationale: "VALIDATION 1차 V11. 서버 구현(AuthService.exceedsBcryptLimit)으로 확인"
  - decision: "Plan inputs 에 이메일 trim·공백만 검증·from 순환 방지·복원 실패 시 401 만 토큰 삭제를 반영"
    rationale: "VALIDATION 1차 V8 — 시나리오가 Plan 밖 규칙을 쓰고 있었다"
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

attempts:
  - phase: "2a"
    round: 3
    outcome: "FAIL — V11(로그인 비밀번호 공백만, 가입 비밀번호 확인 공백만) · WARN 4 · 31건은 복잡도 기준선(8~12) 초과"
    reject_type: "VALIDATOR_FAIL 3/3 (소진)"
  - phase: "2a"
    round: 2
    outcome: "FAIL — V11(accessToken 빈 문자열·공백, 문자열 전수 NUL·서로게이트 전송 단언, 가입 201 비정상 본문) · WARN 4(When 이중 SC-05·06·10·22, 오류 없이 모호, SC-13/15 겹침)"
    reject_type: "VALIDATOR_FAIL 2/3"
  - phase: "2a"
    round: 1
    outcome: "FAIL — V11(재제출 시 서버 오류 문구 초기화 시나리오 없음, 72바이트 한글 비밀번호 경계 미결정) · WARN 5(When 이중, 관찰 불가 Then, Plan 밖 규칙)"
    reject_type: "VALIDATOR_FAIL 1/3"
---
