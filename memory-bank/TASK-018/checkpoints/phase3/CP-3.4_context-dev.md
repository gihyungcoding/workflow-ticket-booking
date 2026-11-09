---
status: ARCHIVED
saved_at: 2026-11-09T06:00:00Z
summary: "Phase 4 2회차 롤백 뒤 Green 706/706 (todo 1). isOneOf → matchRoutes, 콜백 스피너 시안대로. 자체 리뷰 결함 0, 미해결 0."

next_steps:
  - "Phase 4 3회차 — 사용자 진행 지시 대기. 바뀐 화면(콜백 스피너·인코딩 경로) 다시 캡처"

decisions:
  - decision: "구현 순서 — api/auth·문구·제공자 기록 → Field·새 부품 → 화면 5개 → App 라우트"
    rationale: "화면이 쓰는 계약(응답 규칙·부품)을 먼저 고정"
  - decision: "인증 화면 공용 조각을 auth/AuthParts.tsx 로, CtaBar 에 form·Page 에 plain 추가 (scope_deviations)"
    rationale: "시안 클래스를 네 화면이 공유한다. 하단 고정 버튼이 폼 밖이라 form 연결이 필요"
  - decision: "승인 테스트 3곳의 관찰 방법 수정 — 단언 불변"
    rationale: "사용자 승인 2026-11-09 (AskUserQuestion). DEV test_modifications"
  - decision: "리팩토링 — mounted·inFlight ref 를 공용 훅으로 빼지 않음"
    rationale: "기존 화면의 인라인 관례 유지. 회고 후보"
  - decision: "만료 상태 제목은 시안과 달리 '가입 시간이 지났어요' 만 (승인 시나리오 SC-23)"
    rationale: "401 만료에서는 이메일·이름을 모른다"

  - decision: "롤백 — RC-2 는 auth/authPaths.ts isOneOf 로 공통화(App·LoginPage), RC-3 은 화면별. 시안 값은 componentSizes 에 출처 주석과 함께"
    rationale: "같은 판별을 위치별로 고치면 빠뜨린 한 곳이 다음 FAIL — 실제로 safeDestination 이 그랬다"
  - decision: "만료 화면 제목 안 email(name) 은 고치지 않음"
    rationale: "승인 시나리오 SC-23 과 충돌 — HITL#3 에서 묻는다"

attempts:
  - round: 1
    outcome: "Phase 4 1회차 FAIL (RC-2·RC-3) → 롤백"
    back_to: "phase3"
  - round: 2
    outcome: "Phase 4 2회차 FAIL → 2a·2b 재승인 → 구현 (15aff9c)"
    back_to: "phase3"
---
