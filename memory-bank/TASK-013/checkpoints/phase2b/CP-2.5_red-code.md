---
status: ACTIVE
saved_at: 2026-10-29T00:00:00Z
summary: "[Phase 2b 완료 — HITL#2 승인] 테스트 14개 정의(케이스 93) → 91 실패(기능 없음) + 2 already_passing(SC-14 회귀). 기존 176/176 통과. 컴파일 스켈레톤 없음(App 경유 렌더·fetch 응답만 사용). tsc·oxlint 통과."

next_steps:
  - "Phase 3 — wf-develop. 구현 순서: auth.ts completeSocialSignUp → SocialLoginButtons → LoginPage·SignUpPage·messages → OAuthRedirectPage(쿼리 한 번 읽기) → RoleSelectionPage → App 라우트 → vite.config 프록시. 승인된 테스트는 고치지 않는다"
  - "Green 직후 변이 검증(StrictMode 빼기·복원 가드 제거·pendingToken 을 URL 에 남기기·replace→push·중복 제출 가드 제거)으로 가짜 통과를 점검"

decisions:
  - decision: "테스트는 App 을 MemoryRouter 로 렌더하고 새 모듈을 직접 import 하지 않는다 — 컴파일 스켈레톤 없음"
    rationale: "라우트·페이지가 없으면 요소를 찾지 못해 단언이 실패하는 정직한 Red 다. 스텁을 두면 던지는 컴포넌트가 렌더 오류로 실패해 원인이 흐려진다"
  - decision: "새 파일 SocialLoginScreens.test.tsx 로 둔다. 기존 AuthScreens.test.tsx 와 헬퍼는 복사(공유 파일 추출 없음)"
    rationale: "승인된 기존 테스트 파일을 건드리지 않는다(훅이 사람에게 묻는다). 헬퍼 중복은 감수"
  - decision: "SC-02 '이미 로그인한 상태' 행은 헤더에 옛사용자를 띄운 뒤 테스트 전용 Nav 로 콜백에 이동한다"
    rationale: "시나리오 Given(헤더에 옛사용자)을 충실히 만든다. 전체 페이지 로드의 복원 동시 진행은 SC-13 restore 행이 맡는다"
  - decision: "SC-14 는 already_passing — 소셜 버튼 추가 전에도 이메일 폼은 그대로 동작한다"
    rationale: "부정형 회귀 시나리오는 구조적으로 Red 불가(wf-red 예외). Green 에서 버튼이 제출을 가로채면 깨지는 안전망"

attempts: []
---
