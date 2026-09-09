---
checkpoint_id: CP-2.3
checkpoint_name: "독립검증 통과 (attempt 2)"
task_id: TASK-003
phase: "2a"
phase_name: "Phase 2a - Scenario Design"
saved_at: 2026-09-09T00:45:00Z
status: ACTIVE

work_summary: "attempt 1: overall.pass=false(V1 fail). 수정 후 attempt 2: overall.pass=true(warn 5) — 저비용 개선(flow 배열 통일, 긍정 단언, shape/shadows/display=swap 단언 추가)을 재검증 없이 반영 완료"

progress:
  completed:
    - "scenario-validator attempt 1 호출, 응답을 VALIDATION_TASK-003.json에 원본 그대로 저장 — overall.pass=false, fail 1(V1)/warn 3(V5,V7,V8)"
    - "V1(fail) 수정: SC-07의 flow를 MD/JSON 모두 F6,F7로 일치시킴"
    - "V5+V7(SC-06 관찰 불가·구현 결합) 수정: SC-06을 'tokens.ts 문자열 확인'에서 '웹폰트 요청 차단 시 한글이 정상 표시된다'로 재설계, Phase 4 실브라우저 확인으로 처리"
    - "V8(index.html Google Fonts link 미검증) 수정: SC-05에 Then 추가"
    - "scenario-validator attempt 2 호출 — overall.pass=true, fail 0, warn 5. 응답을 VALIDATION_TASK-003.json에 덮어씀(attempt 2로 갱신)"
    - "attempt 2의 저비용 경고 4건을 재검증 없이 추가 반영: (V8) SC-01에 shape.borderRadius/shadows 단언, (V5) SC-02/03의 부정 단언→긍정 단언, (V8) SC-05에 display=swap 단언, (V5) SC-06 문구에 tofu 용어·판정 방법 구체화. (V4) flow 필드를 SC-01~07 전부 배열로 통일"
    - "미반영 경고 2건(V6 When이 관찰 표현, V7 SC-01의 MUI 내부 경로 단언)은 구조적 특성상 정당화된다고 판단해 SCENARIO_TASK-003.md에 사유를 남기고 그대로 둠"
  in_progress: "없음 — Phase 2a 검증 완료"
  blocked: []

next_steps:
  - priority: 1
    task: "HITL#1 승인 요청 (AskUserQuestion)"

decisions:
  - decision: "SCENARIO json의 flow 필드를 모든 시나리오에서 배열로 통일"
    rationale: "검증자가 attempt 2에서도 'SC-07만 배열이고 나머지는 문자열이라 파서가 타입을 하나로 가정하면 깨진다'고 지적(V4) — 애초에 attempt 1을 고치며 SC-07만 배열로 바꾼 것이 원인이었다. 전부 배열로 통일하는 편이 특이 케이스를 만들지 않는다"
    alternatives_considered: ["SC-07만 배열 유지 — 검증자가 이미 위험하다고 지적해 기각"]
    impact: "TASK-001/002의 SCENARIO json은 전부 flow가 문자열 단수라 이 변경의 영향을 받지 않음 — TASK-003만의 스키마"
  - decision: "SC-06을 Red 테스트 없이 Phase 4 실브라우저 확인으로 처리"
    rationale: "'서체 로드 실패'라는 조건 자체가 jsdom에는 없는 개념(폰트 네트워크 요청을 하지 않음) — 이를 흉내내려고 문자열 값만 확인하면 실제로 아무것도 검증하지 못하면서 검증하는 것처럼 보인다(검증자 V5 지적, attempt 2에서도 재지적). TASK-002 Phase 4에서 이미 화면 상태를 실브라우저로 직접 확인한 선례가 있어 같은 방식을 재사용"
    alternatives_considered: ["CSS @font-face 로드 실패를 모킹하는 라이브러리 도입 — 이 태스크 규모 대비 과함"]
    impact: "이번 태스크의 새 vitest 테스트는 SC-01~05 5건, SC-06·SC-07은 각각 Phase 4·기존 테스트 재사용으로 처리"
  - decision: "attempt 2가 이미 pass여서 저비용 개선 반영 후 재검증(attempt 3)은 호출하지 않음"
    rationale: "추가한 내용은 기존에 이미 승인 대상이던 covers/type/flow ID를 바꾸지 않고 Then 문장만 더 구체화·추가한 것이라 새로운 위험을 도입하지 않는다고 판단(TASK-002 Phase 2a에서도 동일한 논리로 재검증을 생략한 전례가 있음)"
    alternatives_considered: ["scenario-validator attempt 3 호출"]
    impact: "HITL#1에 attempt 2 원본 + 추가 개선 내역을 함께 투명하게 제시"

recovery_prerequisites:
  - CP-2.2
---
