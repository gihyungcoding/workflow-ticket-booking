---
checkpoint_id: CP-1.3
checkpoint_name: "Phase 1 설계 정규화 완료"
task_id: TASK-008
phase: "1"
phase_name: "Phase 1 - Plan"
saved_at: 2026-09-17T01:10:00Z
status: ARCHIVED

work_summary: "PLAN_TASK-008.json 작성 완료 — route Backend, inputs 2 / outputs 3 / flows 3, acceptance_criteria 3건 전부 flows.covers로 커버됨"

progress:
  completed:
    - "PLAN_TASK-008.json 작성 및 JSON 파싱 검증"
    - "acceptance_criteria 3건이 flows[].covers에 전부 포함되는지 스크립트로 확인 — 미커버 0건"
    - "target_files 5개(3개 확장 + 2개 신규) 경로 실재 확인 — 신규 2개는 미존재가 정상"
    - "python3 scripts/check_architecture.py 로 ARCH-002/003 관련 위반 없음 재확인(프론트 DESIGN-003 경고는 이 태스크와 무관)"
  in_progress: null
  blocked: []

next_steps:
  - priority: 1
    task: "wf-scenario 스킬로 Phase 2a 시작 — PLAN_TASK-008.json의 flows 3개를 GWT 시나리오로 전개"
    file: "workflow_design/04_plan/PLAN_TASK-008.json"

decisions:
  - decision: "동일 공연 안에서 grade+price 조합이 우연히 같은 두 구역은 하나의 sections 행으로 합쳐지는 것을 의도된 동작으로 둔다"
    rationale: "feature 문서가 구역 간 grade+price 중복을 금지하지 않는다(행 범위 겹침만 409로 막음) — 이번 태스크의 acceptance_criteria에도 이 경계가 없다"
    alternatives_considered: ["구역 단위(행 범위)로 별도 그룹핑 — 하지만 seat 테이블에 구역 경계 자체가 저장되지 않아 불가능"]
    impact: "Phase 2a에서 이 경계를 boundary 시나리오로 추가할지 판단 필요 — unresolved로 남김"

recovery_prerequisites:
  - CP-1.1
  - CP-1.2

execution_context:
  test_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew -p backend test"
  build_command: "JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home ./gradlew -p backend build"
  env_required: []
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceResponse.java"
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"
    - "backend/src/main/java/com/example/ticket_booking/repository/SeatRepository.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/04_plan/PLAN_TASK-008.json"
---

## 무엇을 했나

TASK-008(공연 상세 API에 구역별 좌석 구성 요약 추가)의 acceptance_criteria 3건을
flow 3개(F1~F3)에 1:1 매핑했다. route는 Backend로 확정했고, DTO 소유 원칙에 따라
Repository 프로젝션(SeatSectionCount)과 Service 응답 DTO(SectionSummaryResponse)를
분리했다. 목록 응답에서는 sections를 NON_NULL로 완전히 생략하기로 했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/04_plan/PLAN_TASK-008.json` | Phase 1 최종 산출물 — Phase 2a의 입력 |

## 재개 방법

1. `workflow_design/04_plan/PLAN_TASK-008.json` 을 읽는다
2. `wf-scenario` 스킬을 호출해 F1~F3을 Given/When/Then 시나리오로 전개한다
