---
status: ACTIVE
saved_at: 2026-11-06T11:00:00Z
summary: "Red — 새 테스트 35건(시나리오 1:1, 행 361) 중 red 32·already_passing 2(SC-10·SC-19)·Phase 4 수동 1(SC-34). 기존 AuthScreens·SocialLoginScreens 옛 기대값 교체(단언 삭제 0). 통합 승인(HITL#1·2) 받음 2026-11-09."

next_steps:
  - "Phase 3 — wf-develop (Green)"

decisions:
  - decision: "새 테스트는 frontend/src/auth/task018.auth.test.tsx 한 파일, 앱 전체(App)+MemoryRouter 로 실제 라우트를 지난다"
    rationale: "기존 AuthScreens·SocialLoginScreens 관례. 새 모듈을 직접 import 하지 않아 스켈레톤이 Field.help 시그니처 하나로 끝난다"
  - decision: "기존 테스트 두 파일의 옛 기대값을 이번 Red 에서 함께 바꾼다 — 문구·버튼 이름·가입 뒤 /joined·handle 픽스처·헤더 숨김·SC-05 를 서버 안내로"
    rationale: "Green 에서 처음 드러나면 승인된 테스트를 고치려 사람 승인을 한 번 더 받는다 (TASK-017 교훈)"
  - decision: "앱 밖 이동·다른 계정 로그인·로그아웃은 테스트용 버튼(Outside)으로 일으킨다"
    rationale: "인증 화면에 전역 헤더가 없어져 헤더 링크로 이동을 만들 수 없다"
  - decision: "SC-27·SC-29 의 '오류·경고 없음' 은 console.error 스파이 대신 화면(실패 문구 없음·경로 그대로)으로 단언"
    rationale: "React 18 은 언마운트 뒤 setState 경고를 내지 않아 콘솔 단언이 아무것도 증명하지 못하고, 다른 라이브러리 경고로 가짜 실패만 만든다"

attempts: []
---
