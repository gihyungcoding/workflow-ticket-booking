---
checkpoint_id: CP-4.2
checkpoint_name: "검증 완료 — WARN (attempt 3)"
task_id: TASK-004
phase: "4"
phase_name: "Phase 4 - Verify (재검증, attempt 3)"
saved_at: 2026-09-16T00:05:00Z
status: ARCHIVED

work_summary: "attempt 2 FAIL의 목표 결함 2건(title/venue 길이, sections null 원소)은 code-reviewer가 @Transactional 없는 프로브로 실제 커밋까지 확인해 해소를 검증했다. 새 결함 1건(price/seatsPerRow 소수 절삭으로 음수 가격 가드 우회) 발견 — 사용자와 상의해 WARN으로 낮추고 예외 승인, 별도 팔로우업 태스크로 추적하기로 했다."

progress:
  completed:
    - "증거 수집: ./gradlew test --rerun-tasks(43/43 통과), ./gradlew spotlessCheck(error 0), git diff --stat(19 files, +1271/-1)"
    - "acceptance_criteria 9건 재대조 — 전부 PASS"
    - "check_architecture.py --json — error 0, no_target 없음, warn 3건(기존 프론트 부채, 무관)"
    - "code-reviewer 서브에이전트 호출(백그라운드) — @Transactional 없는 임시 프로브 테스트로 실제 커밋 여부까지 확인하는 방식으로 title/venue 길이·sections null 원소 해소를 검증. attempt 1/2 회귀 없음도 재확인"
    - "code-reviewer가 새 결함(price/seatsPerRow 소수 절삭) 발견 — AskUserQuestion으로 사용자에게 WARN/FAIL 여부 확인, WARN+예외승인으로 결정"
  in_progress: "HITL#3 진행 — CP-4.3 저장"
  blocked: []

next_steps:
  - priority: 1
    task: "AskUserQuestion으로 HITL#3(최종 승인) 진행"
  - priority: 2
    task: "승인되면 CP-4.3_hitl3-approved 저장, verified_commit 기록, Phase 5(Reflect)로 진행"
  - priority: 3
    task: "price/seatsPerRow 소수 절삭 팔로우업 태스크를 별도로 만든다(task-authoring)"

decisions:
  - decision: "price/seatsPerRow 소수 절삭 결함을 WARN으로 낮추고 EXCEPTION_APPROVE로 처리한다"
    rationale: "사용자 직접 확인 — 크래시·DoS 없음, 결제/예매 기능이 아직 없어 당장의 비즈니스 영향이 없다. 막으려면 Jackson 설정 변경 또는 DTO 타입 변경이 필요해 범위가 이 태스크(입력 검증 버그 수정)를 넘어선다"
    alternatives_considered: ["FAIL로 처리하고 Phase 2a로 4번째 롤백 — 3연속 FAIL로 인한 워크플로우 피로, 크래시가 없는 결함이라는 점을 고려해 사용자가 기각"]
    impact: "Phase 5 진입 가능. 팔로우업 태스크 생성이 next_steps에 남음"
  - decision: "HttpMessageNotReadableException 빈 body, DataIntegrityViolationException 폴백 부재, Bean Validation 미사용은 이번에도 시나리오화하지 않는다"
    rationale: "전부 새 관찰 가능한 결함이 아니라 구조적/설계 개선 성격 — attempt 2부터 Phase 5 rule_proposals 후보로 이미 분류돼 있다"
    alternatives_considered: []
    impact: "없음 — Phase 5에서 다룰 항목으로 확정"

recovery_prerequisites:
  - CP-3.4_context-dev_retry2

execution_context:
  test_command: "cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test"
  build_command: "cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew build"
  env_required: ["JAVA_HOME을 JDK21로 설정"]
  main_files:
    - "workflow_design/07_verify/VERIFY_TASK-004.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/07_verify/VERIFY_TASK-004.json"
---

## 무엇을 했나

Phase 4 재검증(attempt 3)을 실행했다. code-reviewer에게 이전 두 라운드에서 "고쳤다고
생각했는데 실행하면 또 다른 결함이 있었다"는 패턴이 반복됐다는 점을 명시하고, 표면적
코드 읽기가 아니라 실제 실행(특히 @Transactional 없는 프로브로 DB 커밋까지 확인)으로
검증해달라고 요청했다. 목표 결함 2건은 해소가 실제로 확인됐고, 대신 세 번째 새 결함
(price/seatsPerRow 소수 절삭)을 발견했다. 이번엔 크래시가 아니라 값이 조용히 잘못
저장되는 정도이고 결제 기능이 아직 없어 실질적 위험이 낮다는 점을 사용자에게 확인받아
WARN으로 낮추고 예외 승인했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/07_verify/VERIFY_TASK-004.json` | 검증 결과(attempt 3), code_review 원문, status: WARN, human_review.decision: EXCEPTION_APPROVE |

## 재개 방법

1. `VERIFY_TASK-004.json` 을 읽는다
2. `CP-4.3_hitl3-approved` 저장 — HITL#3 승인 기록(이미 실질적으로 받음)
3. `wf-reflect` 스킬로 Phase 5 진입
4. price/seatsPerRow 소수 절삭 팔로우업 태스크 생성
