---
checkpoint_id: CP-4.2
checkpoint_name: "검증 완료 — FAIL"
task_id: TASK-004
phase: "4"
phase_name: "Phase 4 - Verify"
saved_at: 2026-09-15T01:00:00Z
status: ACTIVE

work_summary: "code-reviewer가 재현 가능한 정확성/보안 결함을 확인 — status: FAIL. AC4(좌석 상한)가 역방향 행 범위/정수 오버플로로 우회되고, 인증 없는 등록 엔드포인트가 단일 요청으로 OOM을 유발할 수 있다. Plan이 명시한 입력 검증이 전혀 구현되지 않았다"

progress:
  completed:
    - "증거 수집: ./gradlew test(34/34 통과), ./gradlew spotlessCheck(error 0), git diff --stat"
    - "acceptance_criteria 9건 대조 — 7건 PASS, AC4는 code-reviewer 재현으로 FAIL, AC5는 정상 입력 한정 PASS(같은 근본 원인으로 우회 가능)"
    - "check_architecture.py --json — error 0, no_target 없음, warn 3건(기존 프론트 부채, 무관)"
    - "prose_coverage 대조 — 미인코딩 3개 규칙 모두 이번 diff에서 직접 확인해 위반 없음"
    - "회귀 확인 — PerformanceService 생성자 변경/Performance 신규 메서드의 다른 호출처 없음(grep)"
    - "code-reviewer 서브에이전트 호출 — 실제 코드 실행으로 재현한 결함 9건(correctness 4, security 2, design 2, test 1) + low priority 2건"
  in_progress: "사용자에게 FAIL 결과와 롤백 범위 제안"
  blocked: []

next_steps:
  - priority: 1
    task: "사용자에게 롤백 범위(Phase 2a 시나리오 보강 + Phase 3 재구현) 제안 및 확인"
  - priority: 2
    task: "확인되면 Phase 2a로 롤백 — 역방향 행 범위/음수 seatsPerRow/필수 필드 누락에 대한 오류 시나리오 추가"

decisions:
  - decision: "status를 FAIL로 판정하고 HITL#3 승인 선택지를 제시하지 않는다"
    rationale: "wf-verify 규칙상 FAIL은 롤백만 가능하다 — code-reviewer가 실제 실행으로 재현한 correctness/security 결함(AC4 우회, DoS 가능)이 있어 WARN으로 낮출 수 없다"
    alternatives_considered: ["EXCEPTION_APPROVE로 낮추고 별도 태스크로 미룸 — 좌석 상한 우회가 이 태스크의 핵심 AC 자체를 무력화하므로 기각"]
    impact: "Phase 5 진입 불가, 롤백 필요"
  - decision: "역방향 행 범위/seatsPerRow 범위/필수 필드 검증은 Phase 2a(시나리오 보강)부터 다시 하고, Seat FK 애노테이션 누락은 Phase 3에서 시나리오 없이 바로 고친다"
    rationale: "전자는 새 관찰 가능한 오류 계약(400 응답)을 만드는 것이라 이 프로젝트의 TDD 원칙상 시나리오·Red 없이 구현하면 안 된다. 후자는 테스트/프로덕션 스키마 정합성이라는 내부 정합성 문제로 어떤 시나리오의 관찰 가능한 Then도 바꾸지 않는다(Performance.java 선례와 동일 처리)"
    alternatives_considered: ["전부 Phase 3에서 즉시 수정"]
    impact: "롤백 범위가 Phase 2a까지 넓어지지만 워크플로우 원칙에 맞다"

recovery_prerequisites:
  - CP-3.4

execution_context:
  test_command: "cd backend && ./gradlew test"
  build_command: "cd backend && ./gradlew build"
  env_required: ["JAVA_HOME을 JDK21로 설정"]
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"
    - "backend/src/main/java/com/example/ticket_booking/api/dto/SectionRequest.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/07_verify/VERIFY_TASK-004.json"
---

## 무엇을 했나

Phase 4 검증을 실행했다. 테스트·린트·아키텍처 제약은 전부 통과했지만, code-reviewer가
실제 코드를 실행해 좌석 상한(AC4)이 역방향 행 범위나 정수 오버플로로 완전히 우회되고
(예: VIP A~Y + R Z~E 두 구역이면 합산이 5,000으로 보이지만 실제로 25,000개 좌석이
생성된다), 인증 없는 등록 엔드포인트가 이 경로로 OOM을 유발할 수 있음을 확인했다.
Plan이 명시한 입력 검증도 전혀 구현되지 않아 필수 필드 누락이 500으로 샌다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/07_verify/VERIFY_TASK-004.json` | 검증 결과, code_review 원문, status: FAIL |

## 재개 방법

1. `VERIFY_TASK-004.json` 의 `code_review.findings` 를 읽는다
2. 사용자에게 롤백 범위를 확인받는다
3. Phase 2a로 롤백 — 오류 시나리오 추가(역방향 행 범위, seatsPerRow 범위, 필수 필드 누락)
4. Phase 2b → Phase 3 (Bean Validation 적용 + Seat FK 애노테이션 추가) → Phase 4 재검증
