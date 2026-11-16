---
status: ACTIVE
saved_at: 2026-11-16T12:30:00Z
summary: "Red 테스트 29건(435케이스) 작성·커밋 — 424 실패·11 통과(전부 「바뀌지 않는 기존 동작」 행). 기존 테스트 1168 통과 유지. HITL#1·2 통합 승인 완료(2026-11-16, AskUserQuestion)."

next_steps:
  - "Phase 3 — wf-develop. 새 컴포넌트(PerformanceCard·StatusChips·Poster)·getNextOpening·parseList·formatWhen 구현, 상세의 Poster 추출, 승인 테스트 수정 7행"

decisions:
  - decision: "새 테스트 파일 3개 + 공용 헬퍼 1개 — pages/task021.home.test.tsx(SC-01~12)·task021.list.test.tsx(SC-13~23)·task021.misc.test.tsx(SC-24~29), test/task021.ts"
    rationale: "표 시나리오는 test.each 한 줄(= 시나리오 한 건). 계약 위반 표 CV 와 목록·후보 응답기를 헬퍼 한 곳에 둬 SC-05·13·17·22 가 같은 표를 쓴다"
  - decision: "스켈레톤은 API 한 파일뿐 — getNextOpening() 은 throw, getPerformances 는 시그니처만 늘림. 컴포넌트는 만들지 않았다"
    rationale: "tsc -b 가 테스트까지 검사해 새 심볼이 필요한 곳은 SC-28 뿐이다. 나머지는 App 을 통해 렌더하고 소스는 import.meta.glob ?raw 로 읽어 파일이 없으면 Red 가 된다. 로직 한 줄도 넣지 않았다"
  - decision: "SC-28 행 l 에 「정상 응답이 먼저 성공한다」 선행 단언을 넣었다"
    rationale: "스켈레톤이 던지기만 해도 getNextOpening 이 거부된다는 단언이 통과해 가짜 Red 를 숨긴다. 1차 실행에서 그 행이 통과해 발견했다"
  - decision: "SC-03 호출 키 오타(GET /performances/.. → GET /api/performances/..)와 상세 heading 질의(level 2 가 여럿 → 이름으로 좁힘)는 테스트 결함이라 Red 를 확인한 뒤 고쳤다"
    rationale: "실패 사유가 구현 부재가 아니었다(1차 실행 로그)"
  - decision: "Green 이 따라야 하는 DOM 규약 — 칩은 role=button·aria-pressed, 카드는 /performances/{id} 링크(안에 포스터 data-testid=poster·배지·공연명·일시·장소 줄), 머리글은 h1(「… 공연 보기」는 링크), 로딩은 data-testid=performance-card-skeleton ×4(aspect-ratio 1/1)·hero-skeleton, 더 보기는 button 「더 보기」·실패는 button 「불러오지 못했어요 · 다시 시도」"
    rationale: "시나리오 규약 표에 적힌 것을 테스트가 그대로 질의한다. 사람이 승인에서 이 약속을 본다"
  - decision: "jsdom 이 못 하는 것(-webkit-line-clamp·375px 시안 대조)은 Phase 4 브라우저 증거로 둔다 — TEST json harness_probe.not_verified"
    rationale: "관찰 불가한 단언을 만들지 않는다"

attempts: []
---
