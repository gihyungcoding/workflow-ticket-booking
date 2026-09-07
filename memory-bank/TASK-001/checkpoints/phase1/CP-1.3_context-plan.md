---
checkpoint_id: CP-1.3
checkpoint_name: "구조 정규화 완료"
task_id: TASK-001
phase: "1"
phase_name: "Phase 1 - Plan"
saved_at: 2026-09-03T09:21:26Z
status: ARCHIVED

work_summary: "PLAN_TASK-001.json 작성 완료 — route/design(inputs 4, outputs 3, flows 6)/architecture_refs/adr_refs/codebase_analysis(target_files 12, 전부 신규)/test_hints 채움. acceptance_criteria 7건 모두 flows.covers로 커버됨. EXIT GATE 통과."

progress:
  completed:
    - "PLAN_TASK-001.json 저장 및 jq 파싱 검증"
    - "target_files 12건 전부 미존재(신규 생성 대상) 확인 — 실재하지 않는 기존 파일을 가리키지 않았음"
    - "acceptance_criteria 7건 × flows.covers 매핑 확인 (F1~F6)"
  in_progress: null
  blocked: []

next_steps:
  - priority: 1
    task: "activeContext.md phase/last_checkpoint/artifacts.plan 갱신"
  - priority: 2
    task: "git commit (chore(workflow): TASK-001 Phase 1 설계 정규화)"
  - priority: 3
    task: "wf-scenario 스킬로 전이 (Phase 2a)"

decisions:
  - decision: "status 쿼리 파라미터 필터링은 애플리케이션 메모리 필터가 아니라 Repository WHERE 절(DB 레벨)로 구현한다 (F3)"
    rationale: "status는 저장 컬럼이 아니라 계산값이라, 메모리에서 필터링하면 페이지네이션(LIMIT/OFFSET)과 totalElements가 필터 적용 전 기준이 되어 어긋난다. DB 조건으로 변환해야 정확한 페이지네이션이 된다."
    alternatives_considered: ["전체 조회 후 애플리케이션에서 status 계산·필터·수동 페이징 (규모가 커지면 성능/정합성 문제)"]
    impact: "PerformanceRepository에 상태별 조건을 반영한 쿼리 메서드 필요 — Phase 2b Red 테스트가 이 쿼리 자체를 대상으로 삼아야 함"
  - decision: "PerformanceListResponse는 Spring Data Page를 그대로 직렬화하지 않고 커스텀 DTO(content/page/size/totalElements)를 쓴다"
    rationale: "기능 문서 §2 응답 예시가 4개 필드만 요구하며 Spring 기본 Page 직렬화(pageable, totalPages, number 등)와 다르다"
    alternatives_considered: ["Page<PerformanceResponse>를 그대로 반환 (필드 불일치로 AC1 위반)"]
    impact: "api/dto/PerformanceListResponse.java 신규 필요"
  - decision: "Clock 빈이 프로젝트에 없어 config/ClockConfig.java를 신규로 추가한다"
    rationale: "ADR-0005가 Clock 주입을 요구하는데 주입할 빈 정의가 아직 없음"
    alternatives_considered: []
    impact: "target_files에 ClockConfig.java 추가"

recovery_prerequisites:
  - CP-1.1
  - CP-1.2

execution_context:
  test_command: "./gradlew test"
  build_command: "./gradlew build"
  env_required: []
  main_files:
    - "workflow_design/04_plan/PLAN_TASK-001.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/04_plan/PLAN_TASK-001.json"
---

## 무엇을 했나

`PLAN_TASK-001.json`을 완성했다. route는 Backend, 4계층(API/Service/Repository +
DB 마이그레이션)에 걸친 12개 신규 파일을 target_files로 정리했고, acceptance_criteria
7건을 6개 flow(F1~F6)로 전부 커버했다. status가 저장 컬럼이 아니라 계산값이라는
점 때문에 필터링·페이지네이션을 DB 레벨 조건으로 구현해야 한다는 설계 결정을
Phase 2b/3에 넘길 수 있도록 명시했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/04_plan/PLAN_TASK-001.json` | Phase 1 최종 산출물 |

## 재개 방법

1. `PLAN_TASK-001.json` 을 읽는다
2. `wf-scenario` 스킬을 호출해 Phase 2a(시나리오 설계)로 진행한다
