---
status: ARCHIVED
saved_at: 2026-10-22T07:30:00Z
summary: "[Phase 4 2차 WARN · HITL#3 예외 승인됨 2026-10-22 · verified_commit 92e0447] RC-1 변종 5건 해소(테스트 176/176 + 실제 앱: 복원 중 로그인 200·401, 로그인 뒤 로그아웃). 남은 것 — §2-5 제출 중 로딩 표시 부재, 도달 불가 not_probed 1건(logout 만 → restoring 잔류), OOS-2 로그인 응답 대기 중 이동 시 늦은 응답이 로그인·이동(사용자가 수락 동작으로 분류). [이전 1차 FAIL] [Phase 4 1차 FAIL] 테스트 161/161·린트 error 0·빌드·아키텍처 error 0, 실제 백엔드(PostgreSQL)·쓰기 가드 ON/OFF 화면 확인 통과. 코드 리뷰 결함 1건 — 토큰 복원 응답이 로그인/로그아웃 뒤 도착하면 상태 덮어쓰기·새 토큰 삭제(뿌리 1·변종 5, 재현 3). 시나리오에 겹침 케이스가 없어 테스트가 못 잡음. 롤백 대기."

next_steps:
  - "Phase 5 (HITL#3 승인 완료) — 회고 재료: RC-1 later 변종(스윕이 놓친 것), OOS-1·OOS-2, logout restoring, 로딩 표시, 시나리오 2a 가 비동기 결과의 적용 맥락(언마운트·이동·세션 변경)을 처음부터 묻지 않은 것, ADR 후보(문자열 입력 검증 정책)"
  - "롤백 — 겹침 동작이 시나리오에 없으므로 Phase 2a 로 돌아가 변종 5건 전부를 한 번에 시나리오화(wf-scenario) → 2b → 3 → 4 재검증"
  - "Phase 3 수정은 공통 지점에서: 복원에 세션 세대(generation)를 두어 login/logout 이 진행 중 복원을 무효화, catch 의 writeToken(null) 도 세대 확인, login() 은 setRestoring(false)"
  - "Phase 5 — OOS-1(서버 Java trim 과 클라이언트 JS trim 의 NUL 차이)을 문자열 입력 검증 정책 ADR 후보로"

decisions:
  - decision: "로그인 응답 대기 중 이동 시 늦은 응답이 로그인·이동을 실행하는 동작(OOS-2)은 프로브로 재현했으나 사용자가 수락하는 동작으로 분류했다 — RC-1 변종으로 올리지 않고 범위 밖 발견으로 기록"
    rationale: "재현된 변종이 남으면 WARN 불가이므로 임의로 resolved 처리하지 않고 사용자에게 분류를 물었다(AskUserQuestion). 영향 낮음: 사용자가 직접 제출한 로그인이고 제출 중 버튼 비활성"
  - decision: "2차 검증 환경은 4초 지연 프록시(/api/auth/me 만)로 복원 pending 을 실제로 만들었다"
    rationale: "경쟁은 모의가 아니라 실제 서버 응답 순서로 확인해야 한다. 프록시는 스크래치 디렉터리, 저장소 변경 없음"
  - decision: "검증은 모의 서버가 아니라 실제 백엔드 + PostgreSQL 컨테이너로 했다"
    rationale: "docker 사용 가능. 가입·로그인·401·409·쓰기 가드를 실제 응답으로 확인(스키마 가짜 위험 제거)"
  - decision: "이메일 NUL trim 차이(OOS-1)는 이 태스크의 FAIL 사유로 올리지 않고 범위 밖 발견으로 기록"
    rationale: "프론트 diff 의 결함이 아니라 TASK-009 서버 정규화와의 차이. 시나리오 SC-08 은 중간 NUL 만 다룬다"
  - decision: "'제출 중 로딩 표시' 부재는 WARN"
    rationale: "문서 §2-5 에는 있으나 시나리오·완료 조건에 없다"

attempts:
  - phase: "4"
    round: 2
    outcome: "WARN — RC-1 변종 5건 해소, not_probed 1·OOS-2·로딩 표시 부재가 남음"
  - phase: "4"
    round: 1
    outcome: "FAIL — RC-1 복원 응답 경쟁(뿌리 1·변종 5, 재현 3)"
---
