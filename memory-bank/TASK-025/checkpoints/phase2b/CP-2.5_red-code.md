---
status: ACTIVE
saved_at: 2026-11-17T00:00:00Z
summary: "Red 테스트 23건(320 케이스) 작성·커밋. 전체 1924건 중 실패 303(새 테스트 300 + 승인 테스트 변경 3)·통과 1620, 기존 실패 0. HITL#1·2 통합 승인 대기."

next_steps:
  - "통합 HITL(AskUserQuestion) — 시나리오·검증·Red 를 함께 보여주고 승인받는다. 승인되면 generate_red_trigger·human_review.approved 를 쓰고 Phase 3(wf-develop)으로"

decisions:
  - decision: "새 파일 하나만 컴파일 스켈레톤 — frontend/src/api/organizer.ts (본문은 throw 한 줄)"
    rationale: "SC-14 가 getMyPerformances 를 import 한다. 나머지(페이지·Fab·행·Badge draft·라우트)는 테스트가 import 하지 않아 없는 채로 Red"
  - decision: "시나리오당 테스트 함수 하나 — 표는 test.each, 이질적인 행은 행마다 run 클로저"
    rationale: "인벤토리를 기계로 센다(23 == 23)"
  - decision: "SC-14 거부 행은 요청 0 을 본 뒤 정상 호출 성공을 단언한다"
    rationale: "스켈레톤이 던져서 거부 단언이 우연히 통과하지 않게(TASK-021 SC-28 행 l)"
  - decision: "승인된 테스트 task024.organizer.test.tsx 를 3곳 수정"
    rationale: "SC-17 b·c·보강이 주최자 로그인 뒤 '/' 를 기대 — 새 동작은 /organizer. afterEach 복원은 Red 중단 시 연쇄 실패 2건 방지"
  - decision: "시나리오 결함 1건을 시나리오대로 테스트하고 HITL 에 올린다 — SC-17 행 f"
    rationale: "「더 보기 사라진다」가 홈의 items<total 규칙과 어긋난다. 2a 를 다시 거치지 않고 사람이 규칙(쪽 수 기준)을 정하게 한다"

attempts: []
---

# CP-2.5 Red — TASK-025

- 산출물: `workflow_design/05_scenario/TEST_TASK-025.json`
- 테스트: `frontend/src/api/task025.organizer.test.ts`(SC-14) · `frontend/src/auth/task025.entry.test.tsx`(SC-01~05·20~22) · `frontend/src/pages/task025.list.test.tsx`(SC-06~13·15~19·23) · 공용 `frontend/src/test/task025.tsx`
- 스켈레톤: `frontend/src/api/organizer.ts`
- 이미 통과 20건은 모두 이 태스크가 바꾸지 않는 기존 동작·「없음」 행 — TEST JSON `already_passing_rows`
