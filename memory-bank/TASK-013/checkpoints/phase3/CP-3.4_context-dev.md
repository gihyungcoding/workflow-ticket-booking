---
status: ACTIVE
saved_at: 2026-10-29T00:00:00Z
summary: "[Phase 3 완료] 269/269 통과(대상 93케이스 + 기존 176). 변경 6 + 신규 3. tsc·build 통과, 린트 새 경고 0. 변이 15건 중 14 사망·M2 코드 제거·M15 생존(Phase 4 수동 확인). 승인 테스트 1건 수정(SC-06 둘째 클릭, 사용자 승인)."

next_steps:
  - "Phase 4 — wf-verify. 필수 증거: (1) 앱+백엔드를 띄워 화면 상태 실제 확인(콜백 처리 중·역할 선택 기본/제출 중/만료·로그인 실패 배너) (2) M15: 대기 중 주소창에 토큰이 없는지 (3) vite 프록시 두 경로 (4) R-1: 애플 form_post 쿠키를 실제 브라우저로 가능한 만큼 (5) code-reviewer 에 비동기 경쟁·StrictMode·pendingToken 디코드 관점 요청"
  - "code-reviewer 호출 시 JPA 지시는 해당 없음(프론트)"

decisions:
  - decision: "구현 순서: api/auth.ts(completeSocialSignUp·디코더) → SocialLoginButtons → LoginPage·SignUpPage·messages → OAuthRedirectPage → RoleSelectionPage → App 라우트 → vite 프록시. 한 번에 전부 연결한 뒤 실패 3건(SC-02 StrictMode·SC-06 2)을 하나씩 처리"
    rationale: "Red 91건 중 88건이 연결만으로 통과했다. 남은 것은 아래 두 원인"
  - decision: "OAuthRedirectPage 는 cancelled 플래그 대신 mounted ref 로 화면 이탈을 판정한다"
    rationale: "처음에는 AuthProvider 와 같은 cancelled 패턴을 썼으나 StrictMode 의 첫 실행 응답을 버려 로그인이 안 됐다(SC-02 StrictMode). mounted ref 는 시뮬레이트된 언마운트 뒤 다시 마운트되면 true 로 돌아오고, 진짜 이탈에서만 false 로 남는다"
  - decision: "SC-06 둘째 클릭은 pointerEventsCheck 해제로 승인된 테스트를 수정(사용자 승인)"
    rationale: "user-event 가 disabled 버튼을 거부 — 구현과 무관. TASK-010 SC-12 선례, 단언 불변"
  - decision: "리팩토링: login/completeSocialSignUp 응답 검증 공통화. 변이 M2 가 살아남아 started ref 제거"
    rationale: "중복 규칙 방지 / 필요 없는 코드 제거"
  - decision: "M15(주소 토큰 지우기) 생존을 코드 제거로 풀지 않고 Phase 4 수동 확인으로 넘긴다"
    rationale: "PLAN·ADR-0021 이 요구한 노출 완화다. 시나리오가 대기 중 주소를 단언하지 않아 테스트가 못 지킬 뿐 — 테스트 보강은 승인 테스트 변경이라 사용자 판단 몫"

attempts: []
---
