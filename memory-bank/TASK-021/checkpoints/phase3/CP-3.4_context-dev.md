---
status: ARCHIVED
saved_at: 2026-11-16T14:30:00Z
summary: "Green — 전체 1604 중 1603 통과·실패 0(새 435 전부 통과, 기존 1168 유지). tsc·build·린트 error 0. 자체 리뷰 1라운드: [correctness] 0, design 1(낮음, 미재현) 보류."

next_steps:
  - "Phase 4 — wf-verify. 브라우저 증거로 확인할 것: 긴 공연명(201자)이 머리글 링크에서 넘치지 않는가, 한국 시간 표기(이 환경의 Chrome 은 Intl timeZone 을 무시해 UTC 로 나온다 — 시각 표기는 jsdom 테스트가 지킨다), 빈 목록·칩별 빈 목록, 375px 나란히(전역 헤더 한 줄은 사용자 결정으로 차이에서 뺀다)"
  - "Phase 4 리뷰어에게 넘길 것: test_modifications 6건(승인 근거 포함), pre_review.deferred 3건"

decisions:
  - decision: "구현 순서 — API 계약(parseList·getNextOpening·인자 검증) → formatWhen → Poster 추출 → StatusChips·PerformanceCard → 홈 페이지. 새 435케이스 중 426 이 한 번에 통과했고 나머지 9 는 2b 하네스 결함 두 가지였다"
    rationale: "테스트가 질의하는 DOM 규약(role·aria-pressed·data-testid)을 규약 표대로 구현해 구현 쪽 실패는 없었다"
  - decision: "카드는 today(kstDay 문자열)만 받고 memo — useServerClock 은 고치지 않는다"
    rationale: "PLAN 의 미결(매초 재렌더). 시각 줄만 매초 바뀌고 카드 N장은 건너뛴다. 훅은 TASK-020 Phase 4 를 통과한 코드라 건드리지 않았다"
  - decision: "목록 검증은 원소 하나라도 위반이면 전체 오류, 정의 밖 status 도 오류 — 계약은 홈이 쓰는 필드뿐"
    rationale: "SC-22·29. 쓰지 않는 closeAt·totalSeats·sections 를 계약에 넣으면 기존 인증 테스트 픽스처가 홈에서 오류가 된다"
  - decision: "getPerformances 의 page 는 isSafeCount(안전 정수 ≥ 0)로 거부하고 status 는 STATUSES 에 없으면 거부"
    rationale: "SC-28 i — isCount(Number.isInteger)는 2^53 을 통과시켜 별도 안전 정수 판정을 썼다. 화면이 만드는 값이라 버그를 일찍 드러낸다"
  - decision: "홈 루트에서 Container 거터를 음수 마진으로 상쇄(MUI xs 16·sm 24)하고 머리글에 --page-x"
    rationale: "375px 나란히에서 블록이 시안보다 16px 안쪽이었다. App.tsx 는 바꾸지 않는다(사용자 결정). MUI Container 의 거터 규칙에 기대는 값이라 Container 의 거터를 바꾸면 함께 봐야 한다"
  - decision: "리팩토링 — fetchFirst/loadFirst 분리(효과 안 setState 경고 제거), formatWhen 공용화, MoreLink 위치는 그대로"
    rationale: "MoreLink 를 옮기면 인증 5화면 import 가 바뀌어 범위 밖"
  - decision: "승인된 테스트 수정 — 7행(2b 승인) 외 4건(task024 SC-16·task020.clock SC-05·30·40)과 2b 하네스 결함 2건을 AskUserQuestion 으로 별도 승인받아 고쳤다. 단언 의도는 그대로 — 호출 수 기대값은 홈 몫(3·1)만큼 늘렸다"
    rationale: "Phase 3 규칙 3요건(단언 유지·승인·DEV test_modifications 기록). 2b 의 PLAN/시나리오 표가 task024·task020.clock 영향을 놓친 것은 reflect 후보 — 「홈을 렌더하거나 거치는 기존 테스트」를 /api 호출 수·키 목록으로 Grep 하지 않았다"
  - decision: "화면 확인 — 모의 API(DTO 필드 그대로)·별도 포트 vite·375px iframe 두 장. 기본·더 보기·오류·로딩을 만들었고 빈 목록·긴 공연명 링크는 못 봤다. 이 Chrome 은 Intl timeZone 을 무시한다(UTC 출력)"
    rationale: "환경 한계는 구현 결함이 아니다 — 순수 Intl 호출로 재현. 한국 시간 표기는 jsdom(Node)에서 단언된다"

attempts: []
---
