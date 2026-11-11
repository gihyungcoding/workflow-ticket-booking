---
status: ACTIVE
saved_at: 2026-11-11T00:00:00Z
summary: "SCENARIO_TASK-019 — 15건(happy 5 · error 5 · boundary 3 · regression 2), AC 7/7, 입력 경계 40/40. 독립검증 대기."

next_steps:
  - "scenario-validator 1회차 호출 → VALIDATION_TASK-019.json"

decisions:
  - decision: "새로 작성 (기존 시나리오 확장 아님)"
    rationale: "새 엔드포인트·필드"
  - decision: "PLAN input_fields 의 posterUrl·zones 계열 flows 를 [F2,F4]/[F3,F4] → [F4] 로 좁혔다 — 통과 경계는 SC-06 표에 함께"
    rationale: "같은 검증을 흐름마다 칸으로 펼치면 같은 표를 두 번 가리키게 된다. 분류 보완이라 ROLLBACK_PLAN 아님"
  - decision: "PLAN unresolved #7 판정 — posterUrl·area·note·grades 원소는 StrictStringDeserializer 로 숫자·불리언을 거부(INVALID_REQUEST)"
    rationale: "기본 Jackson 은 123 → \"123\" 으로 받아 준다. grades [1] 이 등급 \"1\" 과 맞아 201 이 되는 경로를 막는다(SC-07 k)"
  - decision: "note 는 선택이지만 빈 값·공백만이면 INVALID_REQUEST. area·note 는 제어문자(\\p{Cc})·짝 없는 서로게이트 거부 — SignUpRequest.name 과 같은 정규식"
    rationale: "화면에 빈 설명을 그리지 않는다. ADR-0019"
  - decision: "posterUrl 은 앞뒤 공백을 걷어내지 않는다(→ URI 파싱 실패로 INVALID_POSTER_URL), 스킴 대문자는 받고 그대로 저장, 비 ASCII 경로는 받는다"
    rationale: "ADR-0019 '변환 없이 그대로'. RFC 3986 스킴 대소문자 무관"
  - decision: "next-opening 의 startAt 경계(SC-12 f·g)는 저장소로 직접 넣은, 등록 API 가 막는 데이터로 확인"
    rationale: "openAt>now 이면 정상 데이터에서 startAt<now 가 될 수 없지만 notPast 조건이 실제로 걸리는지 지키려면 이 데이터가 필요하다"
  - decision: "AC7 의 '응답이 바뀌지 않는다' 는 둘로 나눴다 — SC-14(PostgreSQL 에서 V6 적용·제약), SC-15(H2 에서 구역 행 없는 공연의 응답)"
    rationale: "응답은 애플리케이션 경로, 마이그레이션은 운영 엔진에서만 검증된다(ADR-0006)"
  - decision: "PLAN inputs 제약 대조 — 모든 constraints 항목이 SC-01~15 의 Then 에 단언됨. '시각 API·next-opening·상태 계산이 같은 Clock' 은 SC-01(시각 이동)·SC-11/12(MutableClock T)로"
    rationale: "Step 2 입력 제약 대조"

attempts: []
---
