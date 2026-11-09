---
status: ACTIVE
saved_at: 2026-11-09T06:00:00Z
summary: "Green 665/665 (todo 1 = SC-34 화면 확인), lint error 0, tsc·build 통과. 자체 리뷰 2라운드 — RC-1(달력 밖 시각) 수정, 미해결 0."

next_steps:
  - "Phase 4 — wf-verify (SC-34 375px 시안 대조 캡처 포함)"

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

attempts: []
---
