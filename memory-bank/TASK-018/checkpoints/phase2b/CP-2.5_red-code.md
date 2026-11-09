---
status: ACTIVE
saved_at: 2026-11-06T11:00:00Z
summary: "Phase 4 롤백 Red — SC-36~39 를 task018.auth.test.tsx 에(롤백 임시 테스트 흡수·삭제). Red 5행(퍼센트 인코딩 4·스피너 1), 나머지는 변이 M1~M9 로 확인. 통합 재승인 받음 2026-11-09."

next_steps:
  - "Phase 3 — isOneOf 에 라우터와 같은 퍼센트 디코딩, 콜백 스피너 시안대로(32px·3px·트랙·호)"

decisions:
  - decision: "새 테스트는 frontend/src/auth/task018.auth.test.tsx 한 파일, 앱 전체(App)+MemoryRouter 로 실제 라우트를 지난다"
    rationale: "기존 AuthScreens·SocialLoginScreens 관례. 새 모듈을 직접 import 하지 않아 스켈레톤이 Field.help 시그니처 하나로 끝난다"
  - decision: "기존 테스트 두 파일의 옛 기대값을 이번 Red 에서 함께 바꾼다 — 문구·버튼 이름·가입 뒤 /joined·handle 픽스처·헤더 숨김·SC-05 를 서버 안내로"
    rationale: "Green 에서 처음 드러나면 승인된 테스트를 고치려 사람 승인을 한 번 더 받는다 (TASK-017 교훈)"
  - decision: "앱 밖 이동·다른 계정 로그인·로그아웃은 테스트용 버튼(Outside)으로 일으킨다"
    rationale: "인증 화면에 전역 헤더가 없어져 헤더 링크로 이동을 만들 수 없다"
  - decision: "SC-27·SC-29 의 '오류·경고 없음' 은 console.error 스파이 대신 화면(실패 문구 없음·경로 그대로)으로 단언"
    rationale: "React 18 은 언마운트 뒤 setState 경고를 내지 않아 콘솔 단언이 아무것도 증명하지 못하고, 다른 라이브러리 경고로 가짜 실패만 만든다"

attempts:
  - round: 2
    outcome: "Phase 4 2회차 FAIL → 2a 롤백 뒤 Red 재작성"
    back_to: "2b"
---
