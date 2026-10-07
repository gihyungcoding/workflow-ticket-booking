---
checkpoint_id: CP-3.3
checkpoint_name: "리팩토링 완료"
task_id: TASK-007
phase: "3"
phase_name: "Phase 3 - Green"
saved_at: 2026-10-01T00:45:00Z
status: ARCHIVED

work_summary: "새로 추가한 검증 실패 핸들러를 단일 경로로 정리하고, 필드 경로 판정을 더 정확한 접두어로 바꿨다"

progress:
  completed:
    - "handleValidationFailure: allMatch + filter 2회 순회를 '최상위 위반을 먼저 찾는' 단일 판정으로 정리"
    - "필드 경로 접두어를 \"sections\" → \"sections[\" 로 좁힘"
    - "isSectionElementField 를 static 으로 통일(describe 와 일관)"
    - "spotlessApply 로 주석 줄바꿈 포맷 정리"
    - "리팩토링 후 cleanTest test 재실행 — 58개 전부 통과 유지 확인"
  in_progress: "-"
  blocked: []

next_steps:
  - priority: 1
    task: "DEV_TASK-007.json 저장 후 커밋"

decisions:
  - decision: "구역 필드 판정 접두어를 \"sections\" 가 아니라 \"sections[\" 로 좁혔다"
    rationale: "@Valid cascade 로 생기는 FieldError 경로는 항상 'sections[0].grade' 처럼 인덱스를 포함한다. 반면 sections 자체에 대한 위반(지금은 없지만 누군가 @NotEmpty 를 붙이면 생긴다)은 경로가 정확히 'sections' 이고, 그것은 '구역 하나의 형태 오류' 가 아니라 최상위 필드 문제이므로 INVALID_REQUEST 가 맞다 — 기존 sc16(sections 누락 → INVALID_REQUEST) 과도 일관된다. \"sections\" 접두어는 이 둘을 구분하지 못하고, 나중에 sectionsNote 같은 최상위 필드가 생기면 오분류한다"
    alternatives_considered: ["\"sections\" 접두어 유지", "equals(\"sections\") || startsWith(\"sections[\") || startsWith(\"sections.\") 로 장황하게 분기"]
    impact: "현재 테스트 결과는 동일하지만(모든 cascade 경로가 인덱스를 가짐) 미래의 오분류 가능성이 사라진다"
  - decision: "fieldErrors 가 비어 있는 경로를 남겨뒀다 — INVALID_REQUEST 와 일반 메시지로 응답한다"
    rationale: "단일 경로로 더 짧게 줄이면 fieldErrors.get(0) 이 IndexOutOfBounds 로 터져 400 이 아니라 500 이 나간다. 지금은 클래스 레벨 제약이 없어 도달 불가하지만, 누군가 추가하는 순간 요청 경로에서 500 이 발생하는 종류의 잠복 결함이라 한 줄로 막아뒀다"
    alternatives_considered: ["도달 불가하므로 분기 제거"]
    impact: "분기가 3개로 늘었지만 우선순위 규칙은 여전히 위에서 아래로 읽힌다"
  - decision: "기존 8개 핸들러에 공통된 ResponseEntity.status(...).body(new ErrorResponse(...)) 중복은 건드리지 않았다"
    rationale: "badRequest(code, message) 헬퍼를 뽑으면 8개 메서드를 모두 고쳐야 해 이 태스크의 변경 범위를 크게 벗어난다. Phase 4 가 범위 이탈로 지적할 사안이고, 이 중복은 TASK-007 이 만든 것이 아니다"
    alternatives_considered: ["badRequest 헬퍼 추출 후 전체 핸들러 적용", "새 핸들러 2개만 헬퍼 사용(부분 적용으로 오히려 불일치)"]
    impact: "핸들러 파일의 기존 스타일이 그대로 유지된다"

recovery_prerequisites:
  - CP-3.2

execution_context:
  test_command: "cd backend && ./gradlew cleanTest test spotlessCheck"
  build_command: "cd backend && ./gradlew build"
  env_required: ["JAVA_HOME=JDK 21 경로"]
  main_files:
    - "backend/src/main/java/com/example/ticket_booking/api/PerformanceExceptionHandler.java"

integrity:
  schema_version: "1.1"
  source_files:
    - path: "backend/src/main/java/com/example/ticket_booking/api/PerformanceExceptionHandler.java"
---

## 무엇을 했나

리팩토링 대상은 **이 태스크가 새로 쓴 코드로 한정**했다. `handleValidationFailure`
가 처음엔 리스트를 두 번 순회(allMatch 로 "전부 구역 필드인가" 확인 후 다시
filter 로 최상위 위반 찾기)했는데, 우선순위 규칙이 코드에서 바로 읽히지 않았다.
"최상위 위반을 먼저 찾아 있으면 그걸로 응답한다" 는 단일 판정으로 바꿨다.

경로 판정 접두어도 `"sections"` → `"sections["` 로 좁혔다. cascade 로 생기는
경로는 항상 인덱스를 포함하므로 동작은 같지만, `sections` 자체에 대한 위반과
구역 원소의 위반을 구분할 수 있게 된다.

리팩토링 후 `cleanTest test` 를 다시 돌려 58개 통과를 재확인했다.

## 산출물

| 파일 | 역할 |
|---|---|
| `api/PerformanceExceptionHandler.java` | handleValidationFailure 단일 경로화, 접두어 정밀화 |

## 재개 방법

1. `./gradlew cleanTest test spotlessCheck` 로 green·포맷을 재확인한다
2. DEV_TASK-007.json 을 저장하고 커밋한다
