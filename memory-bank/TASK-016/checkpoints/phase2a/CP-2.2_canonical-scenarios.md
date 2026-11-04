---
status: ACTIVE
saved_at: 2026-11-04T14:00:00Z
summary: "SCENARIO_TASK-016 13건(happy 8 · error 4 · regression 1). 2회차 V11 FAIL → 클레임 × 이상값 전수 매트릭스로 재작성, 3회차 검증 대기."

next_steps:
  - "scenario-validator 독립검증"

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
