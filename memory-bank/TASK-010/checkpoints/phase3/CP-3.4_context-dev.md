---
status: ARCHIVED
saved_at: 2026-10-22T06:00:00Z
summary: "[Phase 4 롤백 재수정] RC-1 수정 — 세션 세대 ref 로 복원 응답 무효화(공통 지점 stale()). 전체 176/176 통과(신규 133 + 기존 43), tsc 0, 린트 error 0. [이전] [Green] 전체 161/161 통과(신규 118 + 기존 43). tsc 0, 린트 error 0(경고 4, 신규 1은 only-export-components). 리팩토링 없음. SC-12 테스트 결함 1건은 사용자 승인으로 둘째 클릭만 수정."

next_steps:
  - "Phase 4 2차 — wf-verify. code-reviewer 에 이전 root_causes(RC-1) 지목, 수정 커밋 리뷰, 복원·로그인·로그아웃 겹침과 StrictMode 를 실제 앱으로 재확인"
  - "Phase 4 — wf-verify. 앱을 띄워 가입·로그인·헤더·중복 안내를 화면으로 확인, code-reviewer 호출, check_architecture"
  - "carry_to_phase4 는 DEV_TASK-010.json 참고"

decisions:
  - decision: "[롤백 재수정] 세션 세대(useRef 카운터)로 복원 응답을 무효화한다 — 취소(cleanup)와 세대 불일치를 stale() 하나로 묶어 then·catch·finally 에 모두 적용"
    rationale: "RC-1 변종 5건이 모두 '복원 결과를 세션 변경 뒤 적용' 하는 한 지점이라 공통 지점에서 막는다. 변종별로 분기하지 않는다. 상태(state)가 아니라 ref 인 이유: 비동기 콜백이 렌더 시점 값이 아닌 현재 세대를 봐야 한다"
  - decision: "Provider 밖의 useAuth 는 비로그인 기본값을 돌려준다"
    rationale: "기존 App.test.tsx 가 AuthProvider 없이 <App/> 을 렌더한다 — 승인된 테스트를 고치지 않고 통과시킨다"
  - decision: "검증은 제출 시도 뒤 입력마다 다시 계산(파생값)한다"
    rationale: "SC-29 — 불일치를 고치면 제출 없이 바로 오류가 사라져야 한다. 오류 상태를 따로 들고 있다 동기화하는 것보다 단순"
  - decision: "폼은 noValidate, 이메일 입력은 type=text"
    rationale: "SC-07·08·09 가 브라우저 검증·jsdom 값 정제 없이 앱 규칙 그대로 동작해야 한다"
  - decision: "fetchMe 만 401 을 TokenRejectedError 로 구분한다"
    rationale: "SC-25 — 401 이면 토큰 삭제, 500·네트워크·비정상 200 은 토큰 유지"
  - decision: "리팩토링하지 않음"
    rationale: "가입·로그인 폼은 필드·검증·오류 모델이 달라 합치면 분기만 늘어난다. 문구 상수만 messages.ts 로 공유"
  - decision: "SC-12 테스트 2건의 둘째 클릭 수정 (사용자 승인)"
    rationale: "disabled MUI 버튼에 user-event 가 예외 — 구현 동작(제출 중 disabled)은 시나리오 요구 그대로. 단언 약화 없음"

attempts:
  - phase: "3"
    round: 1
    outcome: "Green — 159/161 → SC-12 테스트 결함 수정 후 161/161"
---
