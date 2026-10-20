---
status: ARCHIVED
saved_at: 2026-10-16T00:00:00Z
summary: "Green — npm test 43/43(대상 12), lint error 0, build 통과(dist/index.html 있음·404.html 없음), DEPLOY-001 위반 0. 변이 2건 모두 사망."

next_steps:
  - "Phase 4 — wf-verify (Frontend: 화면 확인 Step 2.5 필수 — 쓰기 거부 안내를 실제로 띄워 본다)"

decisions:
  - decision: "env 는 호출 시점에 import.meta.env 로 읽는다"
    rationale: "vi.stubEnv 로 검증 가능, 모듈 로드 시점 고정을 피함"
  - decision: "쓰기 거부 판별은 HTTP 상태가 아니라 ApiError.code === WRITE_KEY_INVALID"
    rationale: "향후 TASK-009 인증의 401 과 섞이지 않게. 변이 2건으로 SC-09·SC-12 가 이를 지킴을 확인"
  - decision: "등록 화면은 별도 Alert(재시도 버튼 없음), 수정 화면은 기존 saveError 재사용. 리팩토링 안 함"
    rationale: "표시 방식이 달라 공통화 이득이 작음"
  - decision: "vite.config.ts 는 변경하지 않음"
    rationale: "dev 프록시 유지가 요구사항. 주석 갱신은 범위 밖"

attempts: []
---
