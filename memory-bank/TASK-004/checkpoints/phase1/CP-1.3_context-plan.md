---
checkpoint_id: CP-1.3
checkpoint_name: "설계 정규화 완료"
task_id: TASK-004
phase: "1"
phase_name: "Phase 1 - Plan"
saved_at: 2026-09-11T00:00:00Z
status: ARCHIVED

work_summary: "공연 등록/수정/취소 API의 route를 Backend로 확정하고 PLAN_TASK-004.json 작성"

progress:
  completed:
    - "architecture.md §2~§4, decisions/README.md, constraints.yaml 확인 — 관련 ADR: ADR-0003(계층 구조), ADR-0005(Clock 주입), ADR-0006(테스트 DB), ADR-0009(좌석 개별 행)"
    - "check_architecture.py 사전 점검 — ARCH-001~003 통과, DESIGN 경고 2건은 프론트엔드 기존 부채로 이 태스크와 무관"
    - "기존 코드 조사: PerformanceService/Controller/ExceptionHandler/Repository, Performance 도메인(상태변경 메서드 없음 확인), ClockConfig/ClockTestConfig/MutableClock, PerformanceApiTest 패턴"
    - "route=Backend 확정, inputs 5 / outputs 9 / flows 9 로 acceptance_criteria 9건 전부 커버 확인"
    - "PLAN_TASK-004.json 저장 및 JSON 파싱·AC 커버리지 검증 완료"
  in_progress: "없음 — Phase 1 완료"
  blocked: []

next_steps:
  - priority: 1
    task: "wf-scenario 스킬로 Phase 2a 진입 — PLAN_TASK-004.json 의 9개 flow를 Given/When/Then 시나리오로 옮긴다"
    file: "workflow_design/04_plan/PLAN_TASK-004.json"

decisions:
  - decision: "Service 메서드 시그니처에 api.dto 타입을 쓰지 않고 service 패키지 소유의 SectionSpec 레코드를 새로 만든다"
    rationale: "TASK-001 이력(PLAN_TASK-001.json amendments)에서 Service가 api.dto를 import해 ARCH-002를 실제로 위반한 전례가 있다. Controller가 api.dto→service.SectionSpec 변환을 담당하게 해 재발을 막는다"
    alternatives_considered: ["api.dto.SectionRequest를 Service가 직접 받기 (ARCH-002 위반 위험)"]
    impact: "신규 파일 1개(service/SectionSpec.java) 추가되지만 계층 경계가 명확해진다"
  - decision: "구역 간 '행 범위 겹침' 판정은 행 문자 구간의 교집합만으로 본다 (좌석 번호 조합은 보지 않음)"
    rationale: "performance-registration.md §1-3 문구가 '행 범위가 겹치면'이라고만 되어 있어 가장 단순하고 보수적인 해석을 택했다"
    alternatives_considered: ["행+번호 조합까지 정밀 비교"]
    impact: "Phase 2a 시나리오에서 이 정의를 명시적으로 확정해야 한다 (PLAN의 unresolved에 기록)"
  - decision: "PerformanceResponse, PerformanceNotFoundException, ErrorResponse, ClockConfig/MutableClock 을 모두 그대로 재사용하고 새 타입을 만들지 않는다"
    rationale: "totalSeats 필드가 이미 PerformanceResponse에 있고, 404/Clock 패턴이 이미 검증된 형태로 존재한다"
    alternatives_considered: []
    impact: "구현 범위가 좁아지고 TASK-001 테스트 패턴을 그대로 재사용할 수 있다"

recovery_prerequisites: []

execution_context:
  test_command: "cd backend && ./gradlew test"
  build_command: "cd backend && ./gradlew build"
  env_required: []
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"
    - "backend/src/main/java/com/example/ticket_booking/api/PerformanceController.java"
    - "backend/src/main/java/com/example/ticket_booking/domain/Seat.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/04_plan/PLAN_TASK-004.json"
    - path: "docs/product/features/performance-registration.md"
---

## 무엇을 했나

TASK-004(공연 등록/수정/취소 API)의 요구사항 문서(§1-3, §3, §4, §5, §6)와 기존
`Performance` 관련 코드를 조사해 Backend route로 확정했다. 핵심은 구역(등급×행×열)
입력을 개별 `seat` 행으로 생성하는 알고리즘(ADR-0009)과, 등록/수정/취소 3개
엔드포인트의 검증 순서(시각 순서 → 좌석 상한 → 행 범위 겹침 → 오픈 여부)다. 기존
`PerformanceResponse`/`PerformanceNotFoundException`/`ClockConfig` 를 그대로 재사용해
신규 타입을 최소화했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/04_plan/PLAN_TASK-004.json` | Phase 1 정규화 결과 (inputs/outputs/flows/target_files) |

## 재개 방법

1. `PLAN_TASK-004.json` 을 읽는다
2. `wf-scenario` 스킬로 Phase 2a를 시작해 9개 flow를 GWT 시나리오로 옮긴다
3. `unresolved` 의 "행 범위 겹침 정의"를 시나리오에서 명시적으로 확정한다
