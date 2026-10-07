---
checkpoint_id: CP-4.1
checkpoint_name: "아키텍처·산문 규칙 대조 완료"
task_id: TASK-007
phase: "4"
phase_name: "Phase 4 - Verify"
saved_at: 2026-10-01T19:00:00Z
status: ACTIVE

work_summary: "check_architecture.py 실행(error 0, no_target 없음) + architecture.md §2 산문 규칙 7개를 constraints.yaml과 1:1 대조 + ADR-0010이 명시한 ARCH-004 신규 추가"

progress:
  completed:
    - "python3 scripts/check_architecture.py --json 실행 — error_count=0, warn_count=3(전부 이 태스크와 무관한 기존 frontend DESIGN-003), no_target=[]"
    - "architecture.md §2 '하지 않는 것' 열 7개 항목을 constraints.yaml 6개 제약과 대조 — ARCH-001/002/003 3개는 ✓, 나머지 4개(API 비즈니스 로직, domain 3종, Repository, Frontend)는 미인코딩/검사 불가"
    - "'API는 비즈니스 로직을 갖지 않는다' 규칙이 이번 변경(PerformanceExceptionHandler의 FieldError 우선순위 판정)과 긴장 관계인지 code-reviewer에게 별도로 확인 — 위반 아니라는 판정(Bean Validation 결과의 HTTP 매핑은 API 경계의 정당한 책임, Service로 옮기면 오히려 ARCH-002 위반)"
    - "회귀 확인: PerformanceService.registerPerformance/updatePerformance의 호출자가 PerformanceController 뿐임을 grep으로 확인(null 체크 제거의 안전성 근거)"
    - "code-reviewer가 지적한 'ADR-0010이 명시한 constraints.yaml 추가 누락'을 사용자에게 확인 → '지금 추가' 선택"
    - "architecture-doc 스킬로 ARCH-004('@RequestBody 파라미터에는 @Valid가 있어야 한다', adr: ADR-0010, severity: error) 추가 — 패턴 ^(?!.*@Valid).*@RequestBody"
    - "3단계 검증: (1) 현재 코드 위반 0건 (2) @Valid 제거한 사본에서 위반 1건 정확히 검출 (3) 사본 원복 후 git status 깨끗함 확인"
    - "ADR-0010 §검사 가능한 제약 갱신 — ARCH-004 ID와 반영 내역 기록"
    - "VERIFY_TASK-007.json 갱신 — WARN 4건→3건(scope 소견이 fixed로 전환)"
  in_progress: "-"
  blocked: []

next_steps:
  - priority: 1
    task: "AskUserQuestion으로 HITL#3 승인 요청"

decisions:
  - decision: "'API 비즈니스 로직' 산문 규칙은 constraints.yaml에 추가하지 않는다 — 이 변경의 위반 여부를 code-reviewer로 확인했고 위반이 아니라는 결론이 났으므로 Step 3.5 예외 조항(실제 위반이 있으면 FAIL)에 해당하지 않는다"
    rationale: "규칙 자체를 기계 패턴으로 일반화하기 어렵다(무엇이 '비즈니스 로직'인지가 맥락 의존적). 검증 중에 검증 규칙을 바꾸지 않는다는 wf-verify 원칙에 따라 이 논의는 Phase 5의 rule_proposals로 넘긴다"
    alternatives_considered: ["지금 ADR 제안과 함께 제약 추가"]
    impact: "Phase 5 reflect에서 규칙 개선안 후보로 검토"
  - decision: "ARCH-004는 severity: error로 바로 추가한다 — architecture-doc 스킬의 일반 가이드(새 제약은 warn으로 시작)를 따르지 않는다"
    rationale: "ADR-0010이 '지금 추가해도 위반 0건인 조건이 충족되면 바로 넣는다'는 전제로 이 작업을 이 태스크에 지목했고, 실제로 위반 0건을 검증했다. warn으로 낮추면 ADR이 막으려던 '애노테이션은 있지만 @Valid가 빠져 조용히 무시되는' 상황을 막지 못한다"
    alternatives_considered: ["warn으로 시작 후 별도 태스크에서 error로 승격"]
    impact: "이후 @Valid 없이 @RequestBody를 추가하는 컨트롤러가 생기면 즉시 Phase 4 FAIL"

recovery_prerequisites: []

execution_context:
  test_command: "python3 scripts/check_architecture.py --id ARCH-004"
  build_command: "cd backend && ./gradlew build"
  env_required: ["JAVA_HOME=JDK 21 경로"]
  main_files:
    - "docs/architecture/architecture.md"
    - "docs/architecture/constraints.yaml"
    - "docs/decisions/ADR-0010-bean-validation-for-request-dtos.md"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/07_verify/VERIFY_TASK-007.json"
---

## 무엇을 했나

아키텍처 제약 검사(error 0, no_target 없음)와 산문 규칙 7개 대조를 마쳤다.
가장 중요했던 판단은 `PerformanceExceptionHandler`의 새 분기 로직이
"API는 비즈니스 로직을 갖지 않는다"는 산문 규칙과 긴장 관계인지였다 —
code-reviewer에게 별도로 물어 "Bean Validation 결과를 HTTP 에러 코드로
매핑하는 것은 API 경계의 정당한 책임"이라는 판정을 받았다. 다른 방향(이걸
Service로 옮기면)으로 가면 오히려 ARCH-002(Service→api import 금지)를
깨뜨린다는 점도 확인했다.

code-reviewer가 별도로 지적한 범위 이탈(ADR-0010이 "이 마이그레이션 태스크
안에서 추가하라"고 명시한 constraints.yaml 제약이 빠짐)을 사용자에게 확인받고
`architecture-doc` 스킬로 ARCH-004를 추가했다. 패턴을 세 단계로 검증했다 —
현재 코드 통과, 가짜 위반 생성 시 실제로 잡히는지, 원복 후 git status가
깨끗한지. 세 가지 모두 확인 후 `severity: error`로 바로 적용했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `docs/architecture/constraints.yaml` | ARCH-004 추가 |
| `docs/decisions/ADR-0010-...md` | §검사 가능한 제약 갱신 |
| `workflow_design/07_verify/VERIFY_TASK-007.json` | architecture 필드에 통합 기록 |

## 재개 방법

1. AskUserQuestion으로 HITL#3 승인을 받는다
2. 승인 후 verified_commit에 현재 HEAD 기록, CP-4.3 저장, 커밋
