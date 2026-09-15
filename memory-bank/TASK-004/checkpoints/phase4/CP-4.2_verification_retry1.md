---
checkpoint_id: CP-4.2
checkpoint_name: "검증 완료 — FAIL (attempt 2)"
task_id: TASK-004
phase: "4"
phase_name: "Phase 4 - Verify (재검증, attempt 2)"
saved_at: 2026-09-15T14:45:00Z
status: ACTIVE

work_summary: "attempt 1 FAIL의 원인(좌석 상한 우회, 정수 오버플로, 필수 필드 누락 500, grade 길이, FK 애노테이션)은 모두 해소를 확인했다. 그러나 code-reviewer가 실행으로 잔여 결함 2건을 새로 재현했다 — title/venue 200자 초과(등록·수정 양쪽) 500, sections 배열의 null 원소 500(NPE). status: FAIL, Phase 2a로 재롤백(attempt 3 예정)."

progress:
  completed:
    - "증거 수집: ./gradlew test --rerun-tasks(40/40 통과), ./gradlew spotlessCheck(error 0), git diff --stat(19 files, +1160/-1)"
    - "acceptance_criteria 9건 재대조 — 전부 PASS (attempt 1 FAIL 원인이던 AC4 포함)"
    - "check_architecture.py --json — error 0, no_target 없음, warn 3건(기존 프론트 부채, 무관)"
    - "prose_coverage 재대조 — 미인코딩 규칙 대상 파일(Seat.java 등) 직접 확인, 위반 없음"
    - "code-reviewer 서브에이전트 호출(백그라운드) — 실제 테스트 작성·실행으로 재현. 지시한 2개 질문(title/venue 길이, sections null 원소) 모두 결함으로 확인, PUT 경로도 동일 결함 공유한다는 사실과 테스트 하네스(@Transactional)가 PUT 경로 결함을 가린다는 사실을 추가로 발견"
  in_progress: "사용자에게 FAIL 결과와 Phase 2a 롤백(attempt 3) 제안"
  blocked: []

next_steps:
  - priority: 1
    task: "사용자에게 롤백 범위 제안 및 확인 (AskUserQuestion)"
  - priority: 2
    task: "확인되면 Phase 2a로 롤백 — SC-22(title/venue 200자 초과, PUT 대표 포함)·SC-23(sections null 원소) 시나리오 확정 및 검증"

decisions:
  - decision: "status를 FAIL로 판정하고 HITL#3 승인 선택지를 제시하지 않는다"
    rationale: "wf-verify 규칙상 FAIL은 롤백만 가능하다 — code-reviewer가 테스트를 실제로 작성·실행해 재현한 correctness 결함(500 응답 2건)이 있다"
    alternatives_considered: ["EXCEPTION_APPROVE로 낮추고 별도 태스크로 미룸 — 입력 검증 미비로 인한 500은 이 태스크의 핵심 계약(모든 오류를 ErrorResponse로 반환)을 어기므로 기각"]
    impact: "Phase 5 진입 불가, 재롤백 필요 (attempt 3)"
  - decision: "HttpMessageNotReadableException 시 빈 body 응답은 이번 롤백 범위에서 제외한다"
    rationale: "상태 코드(400)는 맞고 이번 diff가 만든 새 결함도 아니다 — quality 성격의 design 지적이라 Phase 5 rule_proposals로 넘긴다"
    alternatives_considered: ["즉시 시나리오에 포함"]
    impact: "attempt 3 범위가 title/venue 길이·sections null 원소 2건으로 좁게 유지됨"
  - decision: "테스트 하네스(@Transactional)가 PUT 경로 DB 제약 위반을 가리는 문제는 별도 스크리닝 없이 Phase 2b(Red 테스트 작성) 시점에 직접 반영한다"
    rationale: "시나리오(관찰 가능한 Given/When/Then) 자체의 문제가 아니라 테스트 구현 방식의 문제라 Phase 2a 시나리오에는 영향 없음 — Red 작성 시 서비스 계층 400 단언으로 이 사각지대를 우회한다"
    alternatives_considered: ["테스트 하네스 자체를 이번 롤백의 별도 항목으로 명시"]
    impact: "next_steps에 메모로만 남기고 별도 시나리오는 만들지 않음"

recovery_prerequisites:
  - CP-3.4_context-dev_retry1

execution_context:
  test_command: "cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test"
  build_command: "cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew build"
  env_required: ["JAVA_HOME을 JDK21로 설정 — 기본 JAVA_HOME은 JVM 8이라 그대로 두면 gradlew가 즉시 실패한다"]
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"
    - "backend/src/main/java/com/example/ticket_booking/api/PerformanceController.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/07_verify/VERIFY_TASK-004.json"
---

## 무엇을 했나

Phase 4 재검증(attempt 2)을 실행했다. 테스트 40/40, 린트 error 0, 아키텍처 제약 0건은
모두 통과했고, attempt 1 FAIL의 원인 5가지(좌석 상한 우회, 정수 오버플로, 필수 필드
누락 500, grade 길이, FK 애노테이션)는 code-reviewer가 실제 코드 실행으로 전부 해소를
재확인했다. 그러나 code-reviewer에게 "attempt 3 초안(작업 트리에 커밋되지 않은
PLAN/SCENARIO 수정)이 지적한 두 가지가 실제 재현되는지"를 구체적으로 확인해달라고
요청한 결과, title/venue 200자 초과와 sections 배열 null 원소 두 경우 모두 실제 테스트
작성·실행으로 500이 재현됨을 확인했다. 추가로 PUT 경로도 같은 title/venue 길이 결함을
공유한다는 것과, 테스트 클래스의 클래스 레벨 @Transactional이 PUT 경로의 DB 제약
위반을 가려 회귀 테스트로 못 잡는다는 구조적 문제를 새로 발견했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/07_verify/VERIFY_TASK-004.json` | 검증 결과(attempt 2), code_review 원문, status: FAIL |

## 재개 방법

1. `VERIFY_TASK-004.json` 의 `code_review.findings`, `reject` 를 읽는다
2. 사용자에게 롤백 범위(Phase 2a, attempt 3)를 확인받는다
3. Phase 2a로 롤백 — SC-22(title/venue 200자 초과)·SC-23(sections null 원소) 확정,
   PUT 경로 대표 포함, `human_input.generate_red_trigger` 를 true로 전환 후 HITL#1
4. Phase 2b(Red, @Transactional 사각지대 유의) → Phase 3(Green) → Phase 4 재검증(attempt 3)
