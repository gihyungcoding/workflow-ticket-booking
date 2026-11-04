---
status: ARCHIVED
saved_at: 2026-11-04T14:00:00Z
summary: "16건 독립검증 PASS (Phase 1 롤백 2/2 후 1회차, 경고 5) — RC-2 변종 전부 이관."

next_steps:
  - "Phase 2b — wf-red 로 Red 테스트 작성 후 시나리오+Red 통합 승인(HITL#1·2)"

decisions:
  - decision: "새로 작성 (기존 시나리오 확장 아님)"
    rationale: "아이디·대기 정보 조회는 새 기능. pendingToken 거부 표는 TASK-011 SC-10 의 행 구성을 따랐다"
  - decision: "난수원을 테스트가 고정하는 「뽑힌 값」 표기로 시나리오를 결정적으로 만든다"
    rationale: "무작위 생성의 재시도·10회 실패·이메일 무관성을 관찰 가능한 값으로 단언하기 위해"
  - decision: "동시 가입 아이디 경합(UNIQUE 위반 오보 방지)은 시나리오로 쓰지 않고 Phase 4 코드 리뷰로"
    rationale: "결정적 재현이 어렵다. AC3 은 존재 확인 재시도로 덮는다"
  - decision: "feature 문서 §2-5 상태 표는 화면(TASK-018) 소관 — 이 태스크 시나리오와 대조하지 않는다"
    rationale: "route Backend"

  - decision: "1회차 경고 반영: SC-10·SC-11 covers 정정, Plan F2.covers 낡은 문장 정정, SC-01 에 이메일 로그인 응답 handle 단언, SC-07 구현명 제거, SC-05 의 재가입 문장 제거"
    rationale: "검증자 V3·V4·V7·V8·V9"
  - decision: "보류: SC-01·06 의 GET /me, SC-07 의 INSERT 실패는 Then 에 둔다 (V6)"
    rationale: "같은 결과(만들어진 아이디·제약)를 다른 입구에서 관찰하는 확인이라 별도 시나리오로 나누면 Given 이 중복된다. 2b 통합 HITL 에서 사람이 판단"
  - decision: "보류: SC-02·SC-03 병합 안 함 (V9)"
    rationale: "SC-03 은 이메일을 쓰는 구현만 잡는다 — 검증자도 존재 가치를 인정"

attempts:
  - round: 1
    result: FAIL
    fail: ["V11 — pendingToken 클레임 경계(provider 정의 밖·대소문자, name/email/provider/emailVerified/exp 누락·null, email·name NUL·서로게이트·길이+1, emailVerified 표기 변형), 소셜 가입(F3) 10회 실패 경로"]
    fix: "SC-09 표 17행으로 전수 확장 + 응답에 토큰 값 미노출 단언, SC-12 신규(소셜 10회 실패). 같은 종류 대조: /social/complete 의 클레임 거부는 TASK-011 SC-10 이 이미 덮는다"
  - round: 2
    result: FAIL
    fail: ["V11 — providerUserId 클레임 행 전무, email·name 빈 값·공백·형식 불량·비문자열, provider 비문자열, 요청 문자열 자체의 NUL·서로게이트·길이 상한, /social/complete 경계를 다루지 않는 근거 미기재"]
    fix: "SC-09 를 「토큰 자체 8행」 + 「클레임 7개 × 이상값 9종 매트릭스」로 재작성 — 지적된 칸만이 아니라 모든 클레임에 같은 열을 대봄. 요청 문자열 길이 상한은 두지 않는다고 Plan inputs 에 명시. /social/complete 와 마이그레이션 재시도는 「쓰지 않은 것」에 근거 기재. 경고 반영: SC-07 빈 테이블을 SC-13 으로 분리(V6), SQL 표현 추상화(V7), Plan inputs 낡은 문구 정정(V8), SC-11 에 handle 불변 단언(V10)"
---

## 롤백 (2026-11-04)

3회차 V11 FAIL(exp·typ·emailVerified 잔여 칸) → 재시도 상한 초과 → 사용자 결정으로 Phase 1 롤백(ROLLBACK_PLAN 1/2).
PLAN `design.claim_schema` 를 기준으로 SC-09 클레임 매트릭스를 다시 썼다. 검증 회차는 롤백 후 1회차부터 다시 센다.
3회차 경고 반영: JSON 파생에서 꼬리 절 흡수·순서 어긋남 수정(V1), SC-08 제공자 4종(V11 참고 provider 매핑).
보류: V5·V6·V9 (Then 안 추가 확인, 재시도 경로 중복) — 2b 통합 HITL 에서 사람이 판단.

### 롤백 후 1회차 — FAIL (V11)
- 빠진 칸: exp 의 0·음수·음의 Instant 범위 초과 → SC-09 exp 행과 PLAN claim_schema.exp 규칙에 추가. 같은 종류 대조: 숫자 클레임은 exp 하나뿐
- 경고 반영: 토큰 자체 표의 `typ=access` 행 삭제(클레임 표와 중복, V9), PLAN inputs 에 쓰기 가드 적용 명시(V8), SC-08 에 정규화 후 255자 이메일 허용 단언(V11 참고)
- 보류: V5·V6·V7 — 2b 통합 HITL 에서 사람이 판단

### 롤백 후 2회차 — FAIL (V11)
- 빠진 칸: email 형식(@ 앞·뒤 빔, 내부 공백·NBSP), 정규화 전후 길이·동일성, /social/complete 입구의 새 클레임 규칙
- 반영: SC-09 email 칸 확장, SC-08 정규화 표, SC-14 신규(가입 완료 입구 = SC-09 표 전 칸). 같은 종류 대조: 클레임 검증 입구는 두 곳(/pending·/complete)뿐
- 경고 반영: SC-07 의 random() 비결정 단언 삭제(V5)
- 보류: V6·V9 — 2b 통합 HITL 에서 사람이 판단

### 롤백 후 3회차 — PASS
- 경고(보류, 통합 HITL 요약에 올림): V6 Then 안의 후속 호출(SC-01·06·07·08·13), V9 재시도 경로 중복(SC-04/05, SC-06/12)
- 회차 요약: 롤백 전 3회 FAIL(V11) → Phase 1 롤백(claim_schema) → 롤백 후 1·2회 FAIL(V11) → 3회 PASS

## Phase 4 롤백 (2026-11-04, RETRY_SCENARIO 1)
- RC-2 변종 전부를 가져옴: HTTP 로 닿는 접미(=, ==, ~) → SC-15, 검증 함수 직접(나머지 접미·서명 마디 NUL·4마디) → SC-16. not_reproduced 변종(페이로드 마디 NUL)은 서명 불일치로 이미 거부 — SC-16 에 넣지 않음
- AC8 추가(토큰 압축 형식). 기존 시나리오는 accessToken 을 다루지 않아 대조할 옛 정의가 없다

### Phase 4 롤백 후 1회차 — FAIL (V11)
- 검증자 응답에 후행 쉼표가 있어 JSON 으로 파싱되지 않는다 — 가공 금지 규칙대로 원문 그대로 저장(다음 회차가 덮어쓴다)
- 빠진 칸: 중복 exp/typ, 페이로드 마디 NUL, pendingToken 끝 문자·4마디의 실제 입구
- 반영: SC-09 토큰 자체 표에 끝 =·~·!, 4마디, 마디 NUL(페이로드·서명), 중복 키 3행 (SC-14 가 같은 표로 /complete 입구도), SC-15 에 t.abc, SC-16 에 페이로드 NUL·중복 키. PLAN: accessToken 입력·401 UNAUTHORIZED 출력·claim_schema.token_format(중복 키 거부)
- 경고 반영: SC-16 Then 을 「검증 결과가 비어 있다」로 명확화(V5·V7). 보류: V6·V9

### Phase 4 롤백 후 2회차 — FAIL (V11)
- 빠진 칸: exp 숫자 문자열·정수값 소수·지수 표기, email 의 İ 소문자 확장, Bearer 헤더 형식
- 반영: SC-09 exp 행(숫자 문자열·120.0·지수)과 email 256자 칸(İ), PLAN claim_schema.exp = JSON 정수 표기만. Bearer 헤더 형식은 기존 TASK-009 AuthApiTest 가 지킨다고 「쓰지 않은 것」·PLAN 에 명시. SC-15 「그 회원」을 id·email 로 명확화(V5)
- 같은 종류 대조: 숫자 클레임은 exp 뿐, 소문자화로 길이가 바뀌는 문자열은 email 뿐(name·providerUserId 는 정규화하지 않음)
- 보류: V6·V7·V9 — 2b 통합 HITL 에서 판단

### Phase 4 롤백 후 3회차 — FAIL (V11) → Phase 1 롤백 2/2 (사용자 결정)
- 잔여: exp 정수 표기 Instant·long 범위 초과, pendingToken 요청 문자열 변형(==·공백·LF·é)의 실제 입구
- 반영: PLAN claim_schema.exp 범위·request_string, SC-09 토큰 자체 행(끝 =·==·~·!·공백·LF·é, 앞 공백)과 exp 범위 초과 정수 4값. SC-14 가 같은 표를 /complete 입구에 적용 → SC-16 notes 의 「SC-09·SC-14 가 실제 입구로도 확인」이 사실이 됨
- 검증 회차는 롤백 후 1회차부터. 이번 롤백이 마지막 — 다음 상한 초과는 ESCALATE

### Phase 1 롤백 2/2 후 1회차 — PASS
- 경고 5(통합 HITL 에서 판단): V5·V7 SC-16 내부 호출, V6 Then 안 추가 호출, V9 중복, V11 마디 중간 보이는 문자(=·~)와 base64url 미사용 비트 변경(「같은 토큰의 다른 표기」)
