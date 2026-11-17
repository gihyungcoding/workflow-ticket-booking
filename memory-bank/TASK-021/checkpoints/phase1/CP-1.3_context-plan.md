---
status: ARCHIVED
saved_at: 2026-11-16T10:00:00Z
summary: "PLAN_TASK-021.json — route Frontend, flows 9건(AC 9건 전부 + Poster 추출 1건은 사용자 확인한 범위 확장). input_fields 16. 변경 대상 7(신규 3), 재사용 11, 승인 테스트 수정 대상은 unresolved #1·#2 에 정리."

next_steps:
  - "Phase 2a — coverage_matrix.py 로 칸을 닫는다. unresolved #1(PerformanceListPage.test 5건·task017.regression·static 두 개·http.test)을 회귀 행으로 시나리오에 명시해 2b 승인에 함께 올린다"
  - "2a 에서 정할 것: 기준 시각이 없을 때 '오늘' 문구(기기 날짜 대체) · 목록 원소 하나가 계약 위반이면 전체 오류인지 · 정의 밖 status 를 오류로 볼지 배지만 뺄지 · 칩/배지 같은 글자의 쿼리 · 매초 다시 그리기 대응(카드 호출 횟수 불변만 못박기)"

decisions:
  - decision: "홈은 Container + AppHeader 아래에 그린다 (App.tsx 변경 없음)"
    rationale: "시안 home.html 은 헤더 없는 한 장이지만 홈에서 로그인·가입으로 가는 길이 AppHeader 뿐이고 TASK-025 가 AppHeader 에 '내 공연' 링크를 단다. 사용자가 AskUserQuestion 으로 '유지'를 골랐다(2026-11-16). 시안과 다른 점은 맨 위 헤더 한 줄과 좌우 여백 이중 가능성 — Phase 4 에서 차이로 판정하지 않는다"
  - decision: "Poster 를 components/Poster.tsx 로 공용 추출하고 상세도 그것을 쓴다"
    rationale: "카드와 상세가 같은 규칙(http(s) 만·로드 실패 시 회색 면+아이콘)을 두 곳에 두면 갈라진다. 사용자가 '공용으로 추출'을 골랐다(2026-11-16). 상세 파일이 변경 대상에 들어온 것은 이 확장 때문이다 — task020.detail SC-17·33 이 그대로 통과해야 한다"
  - decision: "getPerformances 는 인자가 없으면 쿼리 문자열을 붙이지 않는다"
    rationale: "AuthScreens·SocialLoginScreens·task024 승인 테스트의 fetch 스텁이 'GET /api/performances' 키 하나만 안다. 기본 호출 URL 을 그대로 두면 이 테스트들을 고치지 않는다. size 는 서버 기본 20"
  - decision: "useServerClock 은 홈에서 { enabled: true } 로 쓰고 훅은 고치지 않는다"
    rationale: "ADR-0024 — 오프셋 계산은 훅 하나. 훅은 TASK-020 Phase 4 를 통과했다. 매초 재렌더 비용은 Phase 3 에서 카드 memo 또는 시각 줄 분리로 정한다"

attempts: []
---
