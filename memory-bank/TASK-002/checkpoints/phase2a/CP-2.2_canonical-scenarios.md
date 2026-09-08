---
checkpoint_id: CP-2.2
checkpoint_name: "Canonical 시나리오 생성 완료"
task_id: TASK-002
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-08T00:20:00Z
status: ACTIVE

work_summary: "시나리오 6건(happy 2 / error 2 / boundary 2) 작성, acceptance_criteria 6/6 커버"

progress:
  completed:
    - "SCENARIO_TASK-002.md 작성 — SC-01~06, 각각 covers/flow 명시"
    - "SCENARIO_TASK-002.json 파생, JSON 파싱 확인, uncovered: []"
    - "regression 생략 이유 문서화(최초 프론트 화면, 되돌릴 기존 동작 없음)"
    - "F7(상세 로딩)과 버튼 클릭 후 동작은 공식 AC 밖이라 시나리오에서 제외, 문서에 근거 기록"
  in_progress: "없음"
  blocked: []

next_steps:
  - priority: 1
    task: "scenario-validator 서브에이전트 호출"
  - priority: 2
    task: "검증 결과를 VALIDATION_TASK-002.json 에 원본 그대로 저장"

decisions:
  - decision: "SC-01에 상태 배지 2종(OPEN/SOLD_OUT)을 한 시나리오에 담음"
    rationale: "카드 렌더링과 배지 매핑은 같은 코드 경로 — 배지 라벨/색 매핑 자체의 정확성은 별도 단위 검증보다 이 시나리오 하나로 충분히 결함을 드러낸다(coverage-policy §3)"
    alternatives_considered: ["상태 5종(UPCOMING/OPEN/SOLD_OUT/CLOSED/CANCELLED)마다 시나리오 분리"]
    impact: "시나리오 수 절약, Red 테스트에서 fixture 배열 확장으로 대체 가능"
  - decision: "로딩·빈 목록을 boundary로 분류"
    rationale: "canonical 타입(happy/error/boundary/regression) 중 정상 데이터 경로가 아닌 화면 상태 전환이라 boundary가 가장 근접"
    alternatives_considered: []
    impact: "타입 집계 happy 2/error 2/boundary 2로 coverage-policy '보통' 기준선(4~7건) 안에 듦"

recovery_prerequisites:
  - CP-2.1
---
