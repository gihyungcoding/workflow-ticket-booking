---
status: ARCHIVED
saved_at: 2026-11-05T05:00:00Z
summary: "Red 18건·기존 테스트 기대값 교체 — 통합 승인 완료(2026-11-05, 게이트 예외 포함)."

next_steps:
  - "Phase 3 — 토큰·테마·GlobalTokens·부품 9종·StatusBadge·index.html 구현, 병렬 시간 초과 원인 확인"

decisions:
  - decision: "부품 스켈레톤은 props 인터페이스 + notImplemented() 한 줄"
    rationale: "TS 컴파일 언어 Red 규칙"
  - decision: "기대 색값과 색 변환 도우미는 theme/task017.expected.ts 에 둔다"
    rationale: "SC-10 이 테스트 파일의 색 리터럴(DESIGN-002)을 잡았다 — 테스트도 검사 대상"
  - decision: "정본 CSS·index.html 은 node 모듈 동적 import / ?raw 로 읽는다"
    rationale: "앱 tsconfig 에 node 타입이 없다(TASK-015 관례). vitest 의 URL 해석이 /@fs 를 붙여 저장소 루트는 문자열로 자른다"

attempts:
  - round: 1
    result: "SC-10 이 테스트 파일 색 리터럴로 실패 → theme/ 로 이동, tsc node 타입 오류 → 동적 import"
---
