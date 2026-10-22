---
status: ARCHIVED
saved_at: 2026-10-22T03:00:00Z
summary: "PLAN_TASK-010.json 작성 완료 — route Frontend, flows 7건이 AC 6건을 전부 덮음(F7 은 새로고침 후 로그인 유지 전제). 신규 5 · 기존 3 파일 변경. EXIT GATE 통과."

next_steps:
  - "Phase 2a — wf-scenario 로 시나리오 설계 (경계값: 같은 401 의 세 code 구분, 다른 code·빈 바디·비 JSON, 비밀번호 불일치 해제, 제출 중 중복 방지, 토큰 복원 실패·localStorage throw, from 경로 검증)"
  - "PLAN.unresolved 8건 중 '가입 성공 안내 여부'·'복원 중 헤더 표시'·'72바이트 비밀번호' 는 시나리오에서 정한다"

decisions:
  - decision: "AppHeader 를 새 파일로 추가한다 (태스크 implementation_spec 의 4개 경로 밖)"
    rationale: "AC 6 이 헤더 전환을 요구하는데 현재 App 에 헤더가 없다. 범위를 넓히는 것이 아니라 AC 를 구현하는 데 필요한 파일이다"
  - decision: "오류는 HTTP 상태가 아니라 body.code 로 분기한다"
    rationale: "401 이 INVALID_CREDENTIALS·UNAUTHORIZED·WRITE_KEY_INVALID 세 가지다. TASK-015 가 정한 isWriteKeyRejected 관례를 따른다"
  - decision: "가입·로그인 POST 는 기존 request() 를 거친다 (가드 키 자동 부착)"
    rationale: "ADR-0017 에 따라 인증 API 도 쓰기 가드를 통과해야 한다. 프론트는 쓰기 요청 전부에 키를 붙이므로 추가 작업이 없다"
  - decision: "소셜 로그인 버튼·보호 라우트·가입 성공 안내는 이번 범위에 넣지 않는다"
    rationale: "description 과 AC 가 제외한다 (TASK-013, 소유권 검사 태스크)"

attempts: []
---
