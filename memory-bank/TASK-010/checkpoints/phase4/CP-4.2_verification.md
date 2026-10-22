---
status: ACTIVE
saved_at: 2026-10-22T07:30:00Z
summary: "[Phase 4 1차 FAIL] 테스트 161/161·린트 error 0·빌드·아키텍처 error 0, 실제 백엔드(PostgreSQL)·쓰기 가드 ON/OFF 화면 확인 통과. 코드 리뷰 결함 1건 — 토큰 복원 응답이 로그인/로그아웃 뒤 도착하면 상태 덮어쓰기·새 토큰 삭제(뿌리 1·변종 5, 재현 3). 시나리오에 겹침 케이스가 없어 테스트가 못 잡음. 롤백 대기."

next_steps:
  - "롤백 — 겹침 동작이 시나리오에 없으므로 Phase 2a 로 돌아가 변종 5건 전부를 한 번에 시나리오화(wf-scenario) → 2b → 3 → 4 재검증"
  - "Phase 3 수정은 공통 지점에서: 복원에 세션 세대(generation)를 두어 login/logout 이 진행 중 복원을 무효화, catch 의 writeToken(null) 도 세대 확인, login() 은 setRestoring(false)"
  - "Phase 5 — OOS-1(서버 Java trim 과 클라이언트 JS trim 의 NUL 차이)을 문자열 입력 검증 정책 ADR 후보로"

decisions:
  - decision: "검증은 모의 서버가 아니라 실제 백엔드 + PostgreSQL 컨테이너로 했다"
    rationale: "docker 사용 가능. 가입·로그인·401·409·쓰기 가드를 실제 응답으로 확인(스키마 가짜 위험 제거)"
  - decision: "이메일 NUL trim 차이(OOS-1)는 이 태스크의 FAIL 사유로 올리지 않고 범위 밖 발견으로 기록"
    rationale: "프론트 diff 의 결함이 아니라 TASK-009 서버 정규화와의 차이. 시나리오 SC-08 은 중간 NUL 만 다룬다"
  - decision: "'제출 중 로딩 표시' 부재는 WARN"
    rationale: "문서 §2-5 에는 있으나 시나리오·완료 조건에 없다"

attempts:
  - phase: "4"
    round: 1
    outcome: "FAIL — RC-1 복원 응답 경쟁(뿌리 1·변종 5, 재현 3)"
---
