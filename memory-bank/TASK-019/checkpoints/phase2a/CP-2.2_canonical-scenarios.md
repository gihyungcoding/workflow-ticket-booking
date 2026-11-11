---
status: ARCHIVED
saved_at: 2026-11-11T00:00:00Z
summary: "SCENARIO_TASK-019 — 16건(happy 5 · error 5 · boundary 3 · regression 3), AC 7/7, 입력 경계 40/40. 독립검증 1회차 PASS(경고 4) — 일부 반영, 재검증 안 함."

next_steps:
  - "Phase 2b — Red 테스트. SC-14·16 은 Testcontainers PostgreSQL, 나머지 H2. 기존 Performance 8인자 생성자 유지"

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

  - decision: "검증 경고 반영 — V6: SC-01 의 2회차 호출을 시각 표(a·b)로 바꿔 When 을 하나로, SC-14 의 제약 INSERT 를 SC-16 으로 분리. V7: 규약의 'java.net.URI' 를 'RFC 3986 문법' 으로, SC-01 괄호의 'Spring Security 기본값' 언급 제거. V11 참고: max+1 칸의 by 에서 SC-06(정확히 상한) 제거. V12(2): 이모지 코드 유닛 상한 행 추가 — SC-05 n, SC-06 i, SC-08 f2·k2"
    rationale: "검증자 1회차 경고. 통과 + 경고라 재검증하지 않는다"
  - decision: "검증 경고 보류 — V9(SC-13 중복: 저장된 공연 0건의 빈 본문을 따로 지킨다), V12(1)(sections[].grade 는 기존 필드 — 같은 grade 여러 행은 SC-04 가 확인), V12(3)(userinfo·포트·IP 호스트는 https 절대 URL 규칙 안이라 받는다 — 새 규칙을 만들지 않는다), V12(4)(시나리오에 명시됨), V12(5)(같은 grade 두 행 + zones 조합 — SC-04 와 SC-03 의 병합 경로가 같다)"
    rationale: "통합 HITL 에서 사람이 판단한다"

  - decision: "RETRY_SCENARIO(Phase 3 자체 리뷰 RC-1) — posterUrl 호스트 규칙을 '권한 부분에서 userinfo·포트를 뗀 것' 으로 정의, SC-05 o~t · SC-06 j~m 추가. 2회차 경고 반영: V5 SC-06 Then 에 'note 를 보내지 않으면 areaNote 키 없음', V6 SC-16 When 문구, V12(1) 포트만·숫자 아닌 포트·닫히지 않은 IPv6 행, V12(3) 규칙 문구, merged 행 수. 보류: V12(1) 범위 밖 포트(:99999 — URI 가 받는다, 새 규칙 만들지 않음), V12(2)(4) userinfo·경로의 %XX·#frag 는 받는다, V9·V7·(5)(6)(7) 1회차와 같음"
    rationale: "사용자 결정 '관대하게'(2026-11-11)"

  - decision: "RETRY_SCENARIO 2/3(VERIFY 1회차 RC-1·RC-2) — 호스트 ':'·userinfo '@'·ASCII 아닌 포트 숫자 거부, '공백 아님' 을 유니코드 공백+NBSP+U+200B 로 정의(area·note 같은 판정). SC-05 u~y, SC-08 e2~e4·j2~j6, SC-06 n. 3회차 경고 보류: V12(1) grades 원소의 NBSP·U+200B — sections 에 같은 등급이 없으면 ZONE_GRADE_UNKNOWN 으로 거부되므로 새 규칙 없음, V12(2)~(8)·V5·V7·V9 는 이전 회차와 같음"
    rationale: "사용자 결정 2026-11-11 (HITL#3)"

attempts:
  - round: 1
    result: "PASS — fail 0, warn 4 (V6·V7·V9·V12)"
  - round: 2
    result: "PASS — fail 0, warn 6 (V5·V7·V9·V12·부기) — RETRY_SCENARIO 재검증"
  - round: 3
    result: "PASS — fail 0, warn 6 — RETRY_SCENARIO 2/3 재검증"
---
