---
status: ACTIVE
saved_at: 2026-11-13T00:00:00Z
summary: "Red 테스트 39건 — 새 케이스 241(229 실패·12 구조적 통과), 승인 테스트 6건 새 정본으로 수정(실패). 통합 HITL 승인 완료"

next_steps:
  - "Phase 3 — wf-develop. frontend 테스트는 --testTimeout=60000 으로 돌린다. 스켈레톤 8개를 구현으로 채우고 App.tsx 라우트·레이아웃 분기, AppBar·CtaBar(조건부) 수정"
  - "Green 중 승인 테스트(특히 SC-33 SVG fill·stroke, 시트 가격 color)를 고쳐야 하면 훅이 사람에게 묻는다 — harness_probe.not_verified 참고"

decisions:
  - decision: "가짜 타이머 사용법 — vi.useFakeTimers + vi.setSystemTime, settle(ms)=act 안의 advanceTimersByTimeAsync, findBy 는 쓰지 않는다, 가짜 타이머 파일에서는 userEvent 대신 fireEvent"
    rationale: "프로브로 순차 fetch 순서 [0,400,500]·setSystemTime 이 타이머를 터뜨리지 않음·가시성 이벤트·StrictMode 이중 효과를 확인했다"
  - decision: "SectionSummary.area·stageOrder 는 선택 필드로 선언하고 런타임에 검증한다"
    rationale: "SC-04(계약 위반 → 오류 화면). 필수로 두면 기존 승인 테스트 픽스처 컴파일이 깨진다"
  - decision: "SC-06 은 한 test.each(행 a~k)로 합쳤다"
    rationale: "시나리오 1건 = 테스트 함수 1개(인벤토리 39 == 39)"
  - decision: "포스터 칸에 data-testid=\"poster\" 를 단다는 약속(SC-17·33), 시트 바탕은 기존 sheet-backdrop"
    rationale: "aspect-ratio·배경을 단언할 요소를 구현 방식과 무관하게 찾는다"
  - decision: "시스템 부하로 기존 타이핑 많은 테스트가 5초 제한에서 간헐 실패 — 이 태스크 변경과 무관(단독·30초 제한에서 통과)"
    rationale: "load average 30+ 에서 재현. Phase 3·4 는 --testTimeout 을 늘려 판정한다"

attempts: []
---
