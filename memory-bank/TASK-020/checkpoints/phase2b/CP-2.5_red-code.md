---
status: ARCHIVED
saved_at: 2026-11-13T00:00:00Z
summary: "[롤백 재진입] 시나리오 43건 — 새·바뀐 행 중 56 실패(SC-04 21·SC-10 4·SC-17 3·SC-40 5·SC-41 8·SC-42 15), 기존 1029 통과. 통합 HITL#1·2 재승인 완료"

next_steps:
  - "Phase 3 (HITL#1·2 재승인 완료): 뿌리 4 변종 전부를 고친다(공통 지점에서) — parseDetail 시각 형식·title/venue 공백·totalSeats, useServerClock 표본 5초·AbortController, 시트 닫기(열린 채 들어온 주소는 replace), Hero/Row 공백 판정"
  - "Phase 3 — wf-develop. frontend 테스트는 --testTimeout=60000 으로 돌린다. 스켈레톤 8개를 구현으로 채우고 App.tsx 라우트·레이아웃 분기, AppBar·CtaBar(조건부) 수정"
  - "Green 중 승인 테스트(특히 SC-33 SVG fill·stroke, 시트 가격 color)를 고쳐야 하면 훅이 사람에게 묻는다 — harness_probe.not_verified 참고"

decisions:
  - decision: "롤백 재진입 — 비동기 history 하네스: renderAt({ asyncHistory }) = unstable_HistoryRouter + UNSAFE_createMemoryHistory({ v5Compat: true }) 에 history.go 를 20ms 뒤 반영으로 감쌈. 프로브로 v5Compat 없으면 push 가 listener 를 안 부름을 확인"
    rationale: "MemoryRouter 는 동기라 브라우저의 비동기 popstate 틈(RC-2)을 재현할 수 없다"
  - decision: "task020.sheet-close.test.tsx 삭제(SC-41 a~f 가 대신), SC-41·42 는 시나리오당 함수 하나로 한 test.each 에 합침, task020.text.test.ts(isBlankDisplay 16케이스)는 시나리오 밖 단위 테스트로 유지"
    rationale: "Phase 3 가 HITL 없이 추가했던 테스트를 시나리오로 흡수해 승인 대상에 포함"
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
