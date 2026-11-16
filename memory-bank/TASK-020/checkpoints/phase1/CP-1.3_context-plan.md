---
status: ARCHIVED
saved_at: 2026-11-13T00:00:00Z
summary: "PLAN_TASK-020.json — route Frontend, flows 10건이 AC 14건을 전부 덮음. input_fields 17. 변경 대상 14(신규 8, 조건부 1). 전역 헤더 제거는 사용자 결정."

next_steps:
  - "Phase 2a — coverage_matrix.py 로 칸을 닫는다. unresolved #1 의 승인 테스트 수정(PerformanceDetailPage·task017.ui·regression·static·AuthScreens SC-13/20/22·task018 SC-32)을 회귀 행으로 시나리오에 명시해 2b 승인에 함께 올린다"
  - "2a 에서 정할 것: AC-5 해석(예정일 때만 재측정), 카운트다운 경계표, 한국 시간 표기 문구, 묶음 계산 규칙, 시트 history 동작, 가짜 타이머 사용법"

decisions:
  - decision: "상세·대기열 임시 화면에서 전역 헤더(AppHeader)·Container 를 없앤다"
    rationale: "시안 concert.html 에 헤더가 없고 AC-14 가 구조 일치를 요구한다. 승인 테스트 6건 안팎(AuthScreens SC-13·20·22·공개 조회, task018 SC-32)을 고쳐야 하고 상세에서 로그인 진입이 사라지는 비용을 사용자에게 알린 뒤 두 번 확인받았다(2026-11-13). 유지안은 AC-14 와 어긋남"
  - decision: "useServerClock 은 status 가 UPCOMING 일 때만 켠다"
    rationale: "다른 상태는 시각이 필요 없고 오픈 직전 불필요한 /api/time 호출을 줄인다. AC-5 는 이 상태 기준으로 읽는다(2a 확인)"
  - decision: "time.ts 는 request() 대신 fetch(apiUrl) 를 직접 쓴다"
    rationale: "request() 가 싣는 Authorization 이 교차 출처 preflight 를 일으켜 왕복 표본을 부풀린다. /api/time 은 permitAll"
  - decision: "상세에 StatusBadge 를 두지 않는다"
    rationale: "시안·AC-6 에 배지가 없고 design.md §6 이 끝난 상태의 배지를 금한다. 그 결과 task017.regression 의 상세 '예매 가능' 배지 단언이 바뀐다"
  - decision: "포스터 없음 면은 --c-bg(회색)로 한다"
    rationale: "기능 문서는 --c-surface 라 쓰지만 그것은 흰색이라 흰 바탕 상세에서 사라진다. 시안 .photo 가 --c-bg (design.md §5 시안이 이긴다)"

attempts: []
---
