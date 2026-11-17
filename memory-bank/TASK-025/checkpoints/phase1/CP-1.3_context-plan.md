---
status: ACTIVE
saved_at: 2026-11-17T00:00:00Z
summary: "PLAN_TASK-025.json — route Frontend, flows 12건이 AC 10건을 전부 덮음(+사용자 확정 범위 확장 3건). input_fields 15. 변경 대상 15(신규 4). unresolved 13."

next_steps:
  - "Phase 2a — coverage_matrix.py --skeleton 으로 칸을 닫는다. unresolved #4(이동 기대값 승인 테스트)·#5(SC-36 경로 표기 표에 /organizer 행)·#7(복원 중 logout) 을 시나리오 회귀 행으로 명시해 2b 승인에 함께 올린다"

decisions:
  - decision: "머리글은 「{이름} 님」 + 상태별 고정 제목 3종(0건·비공개 있음·그 외), 회사명은 쓰지 않는다"
    rationale: "사용자 확정. AuthUser 에 회사명이 없고 시안 제목 규칙이 어느 문서에도 없다 — 시안 문구와 다르다는 사실을 Phase 4 에 남긴다"
  - decision: "/organizer 는 전역 AppHeader 없이 시안의 자체 앱바 한 장 전체"
    rationale: "사용자 확정. 인증·상세 화면과 같은 판별(PAGE_ROUTES·FULL_PAGE_PATHS). 이 화면에서 로그아웃·홈 이동은 없다"
  - decision: "「더 보기」를 추가한다(홈과 같은 20개씩·세대 규칙)"
    rationale: "사용자 확정. 서버가 20건씩 주는데 시안에 더 보기가 없어 20건 넘는 공연이 사라진다. AC 밖 범위 확장"
  - decision: "소셜 로그인 콜백(OAuthRedirectPage)도 역할별로 이동한다"
    rationale: "사용자 확정. AC 2 의 「로그인」을 이메일·소셜 둘로 읽는다. 태스크 implementation_spec.paths 밖 — scope 근거로 남긴다"
  - decision: "모든 행을 링크로 한다 (시안의 취소·마감 비링크 행은 따르지 않는다)"
    rationale: "AC 8 이 행 선택 → 상세라고 하고 상세 시안(organizer-cancelled·show--done)이 있어 취소·마감 공연도 상세로 들어가야 한다"
  - decision: "한 행에 배지 하나 — 비공개(취소 아님)면 「비공개」만, 아니면 상태 배지"
    rationale: "design.md: 시안이 이긴다. 기능 문서 §2-5 의 「상태 배지 앞에 붙는다」와 어긋남 — unresolved #11"
  - decision: "TASK-024 follow_up(복원 중 logout → restoring 영구 true)을 F12 로 받는다"
    rationale: "사용자 결정 이관. 로그아웃 진입 경로가 늘어 도달 가능해진다. logout 에 setRestoring(false) 한 줄"

attempts: []
---

# CP-1.3 Phase 1 설계 정규화 — TASK-025 (track light)

- 산출물: `workflow_design/04_plan/PLAN_TASK-025.json`
- 참조 ADR: 0002·0008·0018·0022·0027·0029·0030
- 조사: Explore 1회(`exploration.method: explore`), 메인이 직접 연 파일 14 — 에이전트 보고의 핵심 14건을 열어 확인했고 AppHeader 줄 번호 1건이 달랐다(보고 :282 → 실제 :14)
- 백엔드는 건드리지 않는다 — 서버 계약은 TASK-023(PR #52)이 정본
- 상세 라우트(`/organizer/performances/:id`)는 TASK-026 — 행 선택이 그리로 가므로 두 태스크를 같은 배포로 낸다(unresolved #2)
- 시안: `out/screens/organizer.html`·`organizer--empty.html`·`organizer-only.html`
