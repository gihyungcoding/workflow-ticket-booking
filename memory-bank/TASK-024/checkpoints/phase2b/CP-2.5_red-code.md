---
status: ARCHIVED
saved_at: 2026-11-12T00:00:00Z
summary: "Red — 새 파일 68 케이스 중 52 실패, SC-13 기존 행 9 실패. already_passing: SC-12·14·16(회귀). 기존 테스트 그 밖 실패 0. 통합 HITL#1·2 승인"

next_steps:
  - "RC-1 롤백 — SC-17 Red(6 실패) 뒤 통합 HITL#1·2 재승인 → Phase 3 에서 세션 토큰 정본"
  - "(1회차) Phase 3 — wf-develop. 순서: http.ts(Bearer·키 제거) → performances.ts(401/403 오류) → RequireOrganizer·App 라우트 → 등록·수정 화면 401/403 처리 → 인증 화면 rejected 분기·env 제거"

decisions:
  - decision: "SC-01~12·15·16 은 새 파일 frontend/src/auth/task024.organizer.test.tsx 하나에, SC-13·14 는 기존 승인 테스트의 행·Given 을 바꿔 옮긴다"
    rationale: "시나리오가 기존 행의 기대를 바꾸는 것이라 같은 단언을 새 파일에 복제하면 같은 검사가 두 곳에 생긴다"
  - decision: "컴파일 스켈레톤 없음"
    rationale: "테스트는 App 과 request 만 import 한다 — 새 심볼(RequireOrganizer·UnauthorizedError)을 테스트가 직접 부르지 않는다"
  - decision: "버린 프로브로 로그인 복귀·로그아웃 버튼·쓰기 동작 하네스를 확인하고 지웠다"
    rationale: "Red 에서 멈춘 테스트의 뒷부분(재로그인·늦은 응답)이 Green 에서 하네스 결함으로 드러나지 않게(wf-red Step 3, TASK-022 교훈)"

attempts:
  - n: 2
    type: RETRY_SCENARIO
    note: "Phase 4 1회차 RC-1 → SC-17 추가. 지금 구현에서 a~f 실패, g 통과(정상 로그아웃 회귀)"
  - n: 1
    type: RETRY_SCENARIO
    note: "SC-09 Then 관찰 불가(인증 화면은 전역 헤더 없음) → 로그인 화면 제목으로. 검증 2회차 PASS(경고 5)"
---

# CP-2.5 Red — TASK-024

- 테스트: `frontend/src/auth/task024.organizer.test.tsx`
- 바뀐 승인 테스트: http · WriteKeyRejected(삭제) · AuthScreens · task018 · SocialLoginScreens · App.test
- 산출물: `workflow_design/05_scenario/TEST_TASK-024.json`
