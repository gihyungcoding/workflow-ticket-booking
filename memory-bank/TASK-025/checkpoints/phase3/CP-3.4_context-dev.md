---
status: ACTIVE
saved_at: 2026-11-18T00:00:00Z
summary: "Green — 전체 1923 통과(새 320 포함) · 린트 error 0 · tsc 0 · 운영 빌드 성공. 자체 리뷰 1라운드 결함 0. 승인 테스트 수정 2건(task024 2b 승인분 · task018 사용자 승인)."

next_steps:
  - "Phase 4 — wf-verify. 시안 대조는 Phase 3 에서 한 번 했다(기본·빈·관객 안내·로딩·오류·더 보기). Phase 4 는 DESIGN-* 와 AC 매핑, 코드 리뷰를 다시 한다. 시각 표기는 브라우저가 UTC 라 jsdom 증거"

decisions:
  - decision: "더 보기 소멸 = items.length >= total 이거나 받은 쪽 수 × 20 >= total (둘 중 하나)"
    rationale: "승인된 SC-17 행 f(쪽 수 기준)와 SC-09 행 n(items 기준, 101행·총 101)을 함께 만족하는 가장 단순한 규칙"
  - decision: "Badge 는 BadgeTone 을 넓히지 않고 BadgeToneOrDraft 로 props 만 확장"
    rationale: "승인된 task017 테스트가 Record<ExpectedTone> 로 BadgeTone 을 인덱싱 — 넓히면 그 테스트 2곳이 타입 오류. PLAN unresolved #10 의 확인 결과"
  - decision: "homePathOf 를 auth/homePath.ts 로 둔다"
    rationale: "가입 완료·이메일 로그인·소셜 콜백 세 곳이 같은 규칙 — 복사하면 한 곳이 어긋난다. PLAN 밖 신규 6줄(scope_deviations)"
  - decision: "performances.ts 의 STATUSES·isSafeCount 를 export 해 organizer.ts 가 홈과 같은 판정을 쓴다"
    rationale: "계약 판정을 두 벌로 두지 않는다. 의미 불변(리뷰어 전수 확인)"
  - decision: "401 은 logout() + /login 이동(from=이 화면), 403 은 같은 화면에서 주최자 전용 안내(앱바 포함)"
    rationale: "SC-10·SC-18 — 상태로 판별(TASK-024). 늦은 응답은 세대로 버린다"
  - decision: "리팩토링 — StatusChips→FilterChips 일반화·homePath 공용화는 했고 ActionButton·Notice·401 이동 코드 공통화는 보류"
    rationale: "보류분은 홈·등록·수정 페이지를 건드려야 해 범위가 번진다 — pre_review.deferred 에 올려 Phase 4 가 판단"
  - decision: "승인된 테스트 수정 — task018 SC-35 F1 role ORGANIZER 행의 기대 주소만 /organizer"
    rationale: "2b 에서 Grep 으로 놓친 곳. AskUserQuestion 승인(2026-11-18), test_modifications 에 기록. 단언 수는 그대로"

attempts: []
---

# CP-3.4 Green — TASK-025

- 산출물: `workflow_design/06_dev/DEV_TASK-025.json`
- 커밋: 구현 `367cf6c` · 승인 테스트 기대값 조정 `c51482c`
- 자체 리뷰 1라운드(code-reviewer) — `[correctness]` 0, 메모 3건은 `pre_review.deferred`
- 화면 확인(375px iframe 시안 대조) 결과는 DEV `screen_check`
