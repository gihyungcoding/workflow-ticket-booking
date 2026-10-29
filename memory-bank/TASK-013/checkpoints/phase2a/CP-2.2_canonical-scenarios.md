---
status: ACTIVE
saved_at: 2026-10-29T00:00:00Z
summary: "[Phase 2a 작성 완료, 독립검증 전] 시나리오 12건(happy 4 / error 5 / boundary 3), AC 5/5 매핑. 표로 묶은 행이 많아 케이스는 약 60. 상태 표(feature §2-5) 전 행 대조 완료."

next_steps:
  - "scenario-validator 호출 → VALIDATION_TASK-013.json 에 원문 저장 → 경고 반영 → HITL#1(제가 정한 값 6개와 새 문구를 명시)"

decisions:
  - decision: "새로 쓴다(기존 시나리오 확장 아님). 정책이 바뀐 롤백이 아니라 전수 대조 대상 없음"
    rationale: "TASK-013 은 신규 화면이고 기존 TASK-010 시나리오의 정의를 바꾸지 않는다. TASK-010 의 가입·로그인 상태는 기존 테스트가 그대로 지킨다"
  - decision: "feature §2-5 상태 표 대조 — 회원가입 5상태·로그인 4상태는 TASK-010 범위(변경 없음, 기존 테스트 유지). 역할 선택: 기본=SC-04·05, 제출 중=SC-06, pendingToken 만료=SC-08. OAuth 콜백: 처리 중=SC-02, 제공자 취소/실패=SC-03·SC-10. 성공→홈=SC-02·SC-06"
    rationale: "wf-scenario 새 지침(상태 표를 covers 와 대조). 대응 없는 행 없음. 표에 없지만 구현이 정해야 하는 상태(만료 외 실패·409·state 소실·응답 대기 중 변화)는 SC-09·11·12 로 추가했다"
  - decision: "AC·feature 에 없는 값 6개를 '제가 정한 값' 으로 SCENARIO 상단에 명시: 비정상 콜백은 /login?error=social 한 곳 · /complete 만료 외 실패는 일반 문구+재시도, 409 두 종류는 한 문구+로그인 링크 · /oauth2/role 과 state 소실 시 /login · 새로고침은 실패로 처리 · error=social 은 주소에서 지우지 않음 · 이미 로그인한 사용자의 콜백은 덮어쓰기"
    rationale: "PLAN unresolved 6건의 제안값. HITL#1 에서 사람이 확인한다"
  - decision: "coverage-policy §2-1 적용: 서버 응답 code·본문 분기(SC-03 /me, SC-09 /complete — 같은 상태에 다른 code·빈 바디·비 JSON), 재제출 시 꺼짐(SC-09), 비동기 결과 적용(SC-12 — 복원 경쟁·언마운트), 환경변수 표기(SC-01 — 미설정·빈·공백·끝 슬래시), 외부 입력 변환(pendingToken base64url·UTF-8 디코드 SC-05, 퍼센트 디코드 SC-02)"
    rationale: "문자열 필드 전수(NUL·서로게이트)는 적용하지 않음 — 이 화면은 입력 문자열을 저장·비교하지 않고 토큰 원문을 서버로 그대로 보낸다. 서버가 판정(ADR-0019)"
  - decision: "표로 묶은 것은 SCENARIO_TASK-013.json 의 coverage.merged 에 사유와 함께 남김"
    rationale: "12건은 복잡 기준선(8~12) 안 — 12건 초과 알림 대상 아님"

attempts: []
---
