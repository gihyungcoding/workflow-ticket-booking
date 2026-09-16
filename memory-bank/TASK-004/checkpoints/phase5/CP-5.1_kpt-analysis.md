---
checkpoint_id: CP-5.1
checkpoint_name: "KPT 분석 완료"
task_id: TASK-004
phase: "5"
phase_name: "Phase 5 - Reflect"
saved_at: 2026-09-16T00:30:00Z
status: ARCHIVED

work_summary: "Keep 2건, Problem 3건, Try 3건, insight 3건을 도출했다. 핵심은 '수동 검증이 세 라운드 연속 새 케이스를 놓쳤다'는 반복 패턴과 '@Transactional 하네스가 결함을 가릴 뻔했다'는 것. ADR 후보 2건(Bean Validation 도입 여부, 전역 예외 폴백 정책)과 규칙 개선안 3건을 함께 정리했다."

progress:
  completed:
    - "체크포인트 이력 검토 — phase2a/2b/3 각 2회 재시도(_retry1, _retry2), phase4 2회 롤백(attempt1 FAIL, attempt2 FAIL) 후 attempt3 WARN"
    - "각 CP의 decisions 검토 — 특히 SC-24(PUT 대응) 분리 결정, @Transactional 프로브 검증 결정"
    - "VERIFY_TASK-004.json의 WARN/예외 항목(price 소수 절삭) 검토"
    - "DEV_TASK-004.json의 scope_deviations 검토 — SeatRepository 생성자 추가(attempt 1), Seat FK 애노테이션(attempt 2) 둘 다 이미 수용됨, 신규 이탈 없음"
  in_progress: "REFLECT JSON 저장"
  blocked: []

next_steps:
  - priority: 1
    task: "REFLECT_TASK-004.json 저장 — 완료"
  - priority: 2
    task: "완료 리포트 Artifact 발행 후 HITL#4"

decisions: []

recovery_prerequisites:
  - CP-4.3_hitl3-approved

execution_context:
  test_command: "cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test"
  build_command: "cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew build"
  env_required: ["JAVA_HOME을 JDK21로 설정"]
  main_files:
    - "workflow_design/08_reflect/REFLECT_TASK-004.json"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "workflow_design/08_reflect/REFLECT_TASK-004.json"
---

## 무엇을 했나

`memory-bank/TASK-004/checkpoints/` 전체를 훑어 3라운드에 걸친 재시도 이력과
각 라운드의 decisions를 근거로 KPT를 도출했다. 일반론이 아니라 이 태스크에서
실제로 일어난 일(구체적 결함, 구체적 발견 방법)만 담았다.

## 산출물

| 파일 | 역할 |
|---|---|
| `workflow_design/08_reflect/REFLECT_TASK-004.json` | KPT, ADR 후보, 규칙 개선안 |

## 재개 방법

1. `REFLECT_TASK-004.json` 을 읽는다
2. 완료 리포트 Artifact 발행 → HITL#4
