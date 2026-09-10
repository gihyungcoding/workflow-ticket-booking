---
checkpoint_id: CP-2.2
checkpoint_name: "Canonical 시나리오 생성 완료"
task_id: TASK-003
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-09T00:25:00Z
status: ARCHIVED

work_summary: "시나리오 7건(happy 5 / error 1 / regression 1) 작성, acceptance_criteria 7/7 커버"

progress:
  completed:
    - "SCENARIO_TASK-003.md 작성 — SC-01~07, 각각 covers/flow 명시"
    - "SCENARIO_TASK-003.json 파생, JSON 파싱 확인, uncovered: []"
    - "SC-07(regression)에 AC6+AC7을 병합 — 같은 검증 행위(기존 테스트 재실행)로 동시 충족됨을 문서화"
    - "SC-07의 Red 처리 방식(새 테스트 함수 없이 기존 6개 재실행)을 MD에 명시적으로 메모"
  in_progress: "없음"
  blocked: []

next_steps:
  - priority: 1
    task: "scenario-validator 서브에이전트 호출"
  - priority: 2
    task: "검증 결과를 VALIDATION_TASK-003.json 에 원본 그대로 저장"

decisions:
  - decision: "SC-02(목록)와 SC-03(상세)을 별도 시나리오로 분리 — 병합하지 않음"
    rationale: "같은 '공연명에 Noto Serif KR 적용' 요구지만 실제 구현은 두 파일(PerformanceListPage.tsx/PerformanceDetailPage.tsx)에 각각 독립적으로 sx를 추가하는 것이라, 한쪽만 빠뜨리는 결함이 실제로 가능하다(coverage-policy §3 실패 독립성 기준) — 같은 코드 경로가 아니므로 병합 대상이 아님"
    alternatives_considered: ["하나의 시나리오에서 두 페이지를 모두 렌더해 확인 — When이 두 개가 되어 canonical 규칙(When은 하나) 위반이라 기각"]
    impact: "시나리오 6→7건으로 유지(병합 안 함)"
  - decision: "SC-07(regression)은 AC6+AC7을 하나로 묶고, 새 테스트 함수 대신 기존 6개 함수 재실행으로 Red/Green을 대체"
    rationale: "이 두 AC는 '기존 동작이 안 깨졌는가'를 묻는 순수 회귀 검증이라 이 태스크만으로는 애초에 Red 상태(실패)를 만들 수 없다 — 새로 테스트를 추가하면 오히려 무엇을 검증하는지 불명확해진다"
    alternatives_considered: ["AC6/AC7을 시나리오화하지 않고 Phase 3 Green의 '전체 스위트 통과' 절차로만 처리 — EXIT GATE가 요구하는 acceptance_criteria 전체 커버리지를 시나리오 파일에서 확인할 수 없게 되어 기각"]
    impact: "TEST_TASK-003.json에서 SC-07은 red_scenarios에 있되 status가 처음부터 'green(pre-existing)'으로 기록될 예정 — Phase 2b에서 이 처리 방식 확정"

recovery_prerequisites:
  - CP-2.1
---
