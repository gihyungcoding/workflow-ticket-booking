---
status: ARCHIVED
saved_at: 2026-10-16T00:00:00Z
summary: "Red 테스트 12건 작성 — 7 red + 5 already_passing, 기존 31건 통과 유지. HITL#2 승인됨."

next_steps:
  - "HITL#2 승인 후 Phase 3 — wf-develop"
  - "Phase 3 에서 변이 확인: '401 이면 무조건 거부 문구' 구현을 넣어 SC-09·SC-12 가 실제로 깨지는지"

decisions:
  - decision: "SC-06~09·12 는 performances 모듈을 mock 하지 않고 fetch 만 stub"
    rationale: "상태·본문 → ApiError 변환(throwIfApiError)까지 통과시켜야 SC-12 의 코드 기준 판별이 검증된다"
  - decision: "SC-10·11 은 fs 대신 import.meta.glob ?raw 사용"
    rationale: "앱 tsconfig(types: vite/client, vitest/globals)에 node 타입이 없어 tsc -b 가 깨진다"
  - decision: "http.ts 스켈레톤은 apiUrl·request 두 시그니처만, 본문은 throw 한 줄"
    rationale: "SC-04·05 가 request() 를 직접 호출한다. 로직 없음"

attempts: []
---
