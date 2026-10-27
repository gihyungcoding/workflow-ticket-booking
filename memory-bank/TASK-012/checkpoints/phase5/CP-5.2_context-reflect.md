---
status: ARCHIVED
saved_at: 2026-10-27T00:00:00Z
summary: "[Phase 5 완료, HITL#4 승인 — 승인 + 반복 제안 지금 반영] Keep 4 / Problem 3 / Try 5 / 인사이트 3. ADR 후보 2건(이메일 검증 플래그 해석 규칙, 인가 요청 state 저장·세션 쿠키), drift 3건(config 계층 행 없음, Service/domain 미인코딩, feature 문서 계정 연결 정책), 규칙 제안 5건(wf-develop·coverage-policy·wf-plan·wf-red·evidence-integrity). 반복: wf-plan ×3."

next_steps:
  - "반복 제안 5건(wf-plan·wf-develop·coverage-policy·wf-red·evidence-integrity)을 이번 것과 이전 것 모두 한 커밋으로 반영 → REFLECT 들에 status applied·applied_in 기록. 워크플로우 자산이라 스타터에도 같은 변경이 필요함을 사용자에게 알린다"
  - "/wf-ship 으로 PR 준비 — 승인한 코드(verified_commit 8714a94)가 그대로인지 대조한 뒤 PR 을 만든다. 머지는 사람이"
  - "후속 태스크 후보: R-1(SameSite=None;Secure 또는 쿠키 기반 저장소+ADR, 실제 브라우저 검증) · ADR-0021 fragment 리다이렉트 형식 변경 · user-authentication.md §1-3 갱신 · architecture.md 에 config 계층 행 추가"
  - "ADR 후보 2건은 proposed 로 남는다(/wf-adr 로 기록할 때 승격)"

decisions:
  - decision: "KPT 는 이 태스크에서 실제로 일어난 사건만 근거로 썼다: Phase 4 FAIL 1회(RC-1) · Phase 2a 독립검증 fail 1회(V11) · PostgreSQL 전체 스위트 인공물 10건"
    rationale: "FORBIDDEN 1·2(일반론·일어나지 않은 일). 각 Problem 에 VERIFY·PLAN·VALIDATION 파일의 항목을 증거로 달았다"
  - decision: "Problem 1(RC-1)의 원인을 세 겹으로 분해했다: 인계 전제 미확인 / PLAN '[Phase 3 확인]' 항목을 Phase 3 가 닫지 않음 / 2b 하네스 프로브가 프레임워크 변환기를 포함하지 않음"
    rationale: "한 줄 원인('테스트가 attributes 를 직접 주입했다')만 쓰면 규칙으로 옮길 곳이 하나뿐이다. 세 겹으로 나누자 서로 다른 파일(wf-develop·coverage-policy·wf-red)에 Try 를 붙일 수 있었다"
  - decision: "규칙 제안 5건 중 4건은 이미 쌓인 미반영 제안이 있는 파일이라 recurring 을 적었다(wf-plan 2 · coverage-policy 1 · wf-develop 1 · wf-red 1 · evidence-integrity 1)"
    rationale: "wf_status.py --proposals 로 확인. 같은 파일에 제안이 쌓이는 것은 같은 원인의 재작업이 반복된다는 뜻이다. HITL#4 에서 '지금 반영'을 선택지로 올린다"
  - decision: "evidence-integrity.md 제안은 TASK-011 #2 의 미완성 문구를 대체·반영하는 것으로 적었다"
    rationale: "TASK-011 #2 가 '… 자체 H2 컨텍스트를 만드는 테스트는…' 에서 끊겨 있었다. 이번에 같은 문제를 겪어 제외 목록·사유 기록 형식과 Docker 사전 확인까지 구체화했다"
  - decision: "ADR 후보 2건: 이메일 검증 플래그 해석 규칙(신규), 인가 요청 state 저장·세션 쿠키(TASK-011 후보를 실제 HTTP 사실로 재제기)"
    rationale: "둘 다 이 기능을 넘어 모든 소셜 제공자에 걸린다. R-1 은 사용자가 이미 수용·후속 태스크로 결정했으므로 ADR 작성은 후속 태스크의 일이고 HITL#4 에서 '승인 + ADR 작성'으로 시작할 수 있다"
  - decision: "이 Phase 안에서는 문서·스킬을 직접 고치지 않는다"
    rationale: "회고 중 규칙 변경은 검토를 거치지 않는다. 반영은 HITL#4 승인 이후 별도 커밋"

attempts: []
---
