---
status: ACTIVE
saved_at: 2026-10-21T00:00:00Z
summary: "Phase 4 판정 FAIL — AC8 위반(공연 API 의 프레임워크 오류 응답 400/405/415 → 401, 프로브로 확인). 보조 결함: 소문자화로 길이가 늘어나는 이메일이 409 로 오보. 나머지 AC 1~7 PASS."

next_steps:
  - "HITL#3 — FAIL 이라 승인 선택지 없음. 롤백 선택: 시나리오로(Phase 2a, 권장) 또는 구현으로(Phase 3)"
  - "권장 이유: 두 결함 모두 시나리오가 덮지 않은 경계다(프레임워크 오류 응답·정규화 후 길이). SC-15 에 오류 행과 SC-05 에 정규화 후 길이 행을 추가한 뒤 Red→Green"
  - "수정 방향(참고): ERROR 디스패치 permitAll(예: dispatcherTypeMatchers(ERROR).permitAll()), 정규화 후 길이 검증 또는 UNIQUE 위반으로 catch 한정"

decisions:
  - decision: "프레임워크 오류 응답 회귀를 WARN 이 아니라 FAIL 로 판정"
    rationale: "AC8 은 공연 API 가 '그대로 동작' 해야 한다고 적었고, develop 과 비교한 프로브에서 7건이 실제로 달라졌다. 정상 경로만 확인한 테스트가 통과했다고 PASS 로 쓸 수 없다"
  - decision: "리뷰어가 '프로브 필요'라고 한 추정을 직접 프로브로 확인한 뒤 판정에 반영"
    rationale: "추론만으로는 결함이 아니다. develop 워크트리와 브랜치를 같은 postgres 에 각각 띄워 같은 요청을 비교했다"
  - decision: "e2e 는 로컬 PostgreSQL 16.14 에서만 확인 — 운영 Neon 은 18.6"
    rationale: "한계로 VERIFY 에 기록했다"

attempts: []
---
