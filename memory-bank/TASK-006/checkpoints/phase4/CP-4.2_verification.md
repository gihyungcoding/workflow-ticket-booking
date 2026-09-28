---
checkpoint_id: CP-4.2
checkpoint_name: "Phase 4 검증 — FAIL (1차)"
task_id: TASK-006
phase: "4"
phase_name: "Phase 4 - Verify"
saved_at: 2026-09-28T01:00:00Z
status: SUPERSEDED

work_summary: "테스트/린트/아키텍처 제약은 모두 통과했으나 code-reviewer가 correctness 결함 2건(int 범위 초과 시 500, isIntegral 자체의 예외) 발견 → status: FAIL"

progress:
  completed:
    - "./gradlew test (전체) → 50/50 통과"
    - "./gradlew spotlessCheck → error 0"
    - "python3 scripts/check_architecture.py --json → error_count 0, no_target 없음, warn 2건(기존 DESIGN-003, 이 변경과 무관)"
    - "Step 3.5 prose_coverage 대조 — 미인코딩 항목들이 이 diff에서 실제로 위반되지 않음을 직접 확인"
    - "code-reviewer 서브에이전트 호출 → correctness 결함 2건 확인(실제 요청으로 재현, 저장소는 수정하지 않음)"
    - "VERIFY_TASK-006.json 저장 (status: FAIL)"
  in_progress: "없음 — HITL#3 생략(FAIL은 승인 선택지 없음), Phase 3 롤백 준비"
  blocked: []

next_steps:
  - priority: 1
    task: "Phase 3으로 롤백 — int 범위 검증 추가, isIntegral()을 stripTrailingZeros().scale()<=0 방식으로 교체"
    file: "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"
  - priority: 2
    task: "새로 열린 입력 경계(int 범위 초과값)에 대한 테스트 보강 검토 — Phase 2a로 더 되돌릴지, Phase 3에서 추가 테스트로 충분한지 사용자와 확인"

decisions:
  - decision: "FAIL 판정에 AskUserQuestion으로 승인 여부를 묻지 않고, 결과를 보고한 뒤 바로 Phase 3 롤백으로 진행한다"
    rationale: "wf-verify 스킬 Step 8: 'status: FAIL 이면 승인 선택지를 제시하지 않는다. FAIL은 롤백만 가능하다.' — 선택의 여지가 없는 절차적 판정이라 별도 승인이 필요 없다"
    alternatives_considered: ["AskUserQuestion으로 확인 후 진행"]
    impact: "사용자에게는 통보 형식으로 보고하고 즉시 Phase 3 재진입"

recovery_prerequisites:
  - CP-3.4

execution_context:
  test_command: "./gradlew test"
  build_command: "./gradlew build"
  env_required: ["JAVA_HOME=/Users/gihyung/Library/Java/JavaVirtualMachines/ms-21.0.10/Contents/Home"]
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/07_verify/VERIFY_TASK-006.json"
---

## 무엇을 했나

테스트·린트·아키텍처 제약은 모두 통과했지만, code-reviewer 서브에이전트가 실제 요청을
태워 재현한 결과 correctness 결함 2건을 발견했다 — (1) price/seatsPerRow가 int 범위를
넘으면 `intValueExact()`가 `ArithmeticException`을 던지고 이를 처리하는 핸들러가 없어
500이 반환된다(이전엔 Jackson이 400으로 거부했으므로 회귀), (2) `isIntegral()` 구현이
PLAN이 명시한 `stripTrailingZeros().scale()<=0` 방식 대신 `remainder()` 방식을 써서
극단적 지수 표기 입력에서 그 자체가 예외를 던진다. 둘 다 이 태스크가 만든 새 코드
경로(BigDecimal 도입)에서 생긴 문제이고, 좌석 데이터는 트랜잭션 롤백으로 남지 않지만
상태코드/에러 계약이 깨진다. Phase 4는 코드를 고치지 않으므로 FAIL로 기록하고 Phase 3
으로 되돌린다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/07_verify/VERIFY_TASK-006.json` | status: FAIL, 근거·code_review 전문 |

## 다음 단계

Phase 3 롤백 — int 범위 검증 추가 + isIntegral() 구현 교체.
