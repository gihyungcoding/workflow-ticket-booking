---
status: ARCHIVED
saved_at: 2026-11-11T00:00:00Z
summary: "VERIFY 2회차 WARN — 180/180(단독 재실행)·spotless 0·아키텍처 error 0 · 리뷰 [correctness] 0. RC-1·RC-2 해소, 보이지 않는 서식 문자는 accepted_risk(ADR-0019). HITL#3 예외 승인."

next_steps:
  - "Phase 5 — wf-reflect (standard: 완료 리포트 Artifact)"

decisions:
  - decision: "나머지 검사를 끝까지 돌렸다 — 아키텍처·산문 대조·보안·회귀"
    rationale: "롤백 한 번에 결함을 전부 가져간다"
  - decision: "테스트 증거는 --rerun-tasks 로 다시 실행(첫 실행이 UP-TO-DATE 캐시)"
    rationale: "실행하지 않은 결과를 적지 않는다"

  - decision: "HITL#3 — 시나리오로 롤백. userinfo '@' 거부, NBSP·U+200B 만인 area·note 거부 (사용자 결정 2026-11-11)"
    rationale: "변종 행이 승인 시나리오 밖"

  - decision: "2회차 테스트 첫 실행 FAIL 은 리뷰어의 동시 --tests 실행이 in-progress-results-generic.bin 을 지운 빌드 도구 오류 — 단독 재실행 180/180. 두 실행 모두 VERIFY evidence.tests.runs 에"
    rationale: "wf-verify 「실행 순서」 — 같은 프로젝트 동시 빌드"
  - decision: "보이지 않는 서식 문자(Cf)·U+2800·U+3164 만인 area·note 는 수용 — accepted_risk ADR-0019"
    rationale: "사용자 결정 2026-11-11"

attempts:
  - round: 1
    result: "FAIL — [correctness] 3"
  - round: 2
    result: "WARN — [correctness] 0, 판단 요청 1(수용), DESIGN-003 warn 3(기존 파일)"
---
