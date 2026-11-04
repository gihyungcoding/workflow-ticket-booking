---
status: ARCHIVED
saved_at: 2026-11-04T13:00:00Z
summary: "PLAN_TASK-016.json 작성 완료 — route Backend, flows 6건이 AC 7건을 전부 덮음. EXIT GATE 통과."

next_steps:
  - "Phase 2a — wf-scenario 로 시나리오 설계 (아이디 규칙 경계값: 2자/3자, 20자/21자, 허용 밖 문자, 접미사 길이 / pending 조회 만료 경계)"

decisions:
  - decision: "아이디 생성은 service/HandleGenerator, 존재 확인은 UserRepository.existsByHandle"
    rationale: "규칙은 도메인 판단(Service), 쿼리는 Repository 소유(ARCH-003)"
  - decision: "AC5(기존 회원 채우기)는 Testcontainers PostgreSQL + Flyway(V4→V5) 테스트 1개로만 검증, 나머지는 H2 유지"
    rationale: "V5 는 PostgreSQL 문법이라 H2 로는 검증 불가. ADR-0006 의 전면 전환은 흐름 4(좌석 선점)에서"
  - decision: "UNIQUE 위반은 제약 이름으로 이메일/아이디를 구분 — 아이디 경합을 이메일 중복 409 로 오보하지 않는다"
    rationale: "기존 signUp 은 모든 23505 를 이메일 중복으로 본다. 재시도는 AC 밖이라 Phase 3 판단"
  - decision: "(2026-11-04 변경) 아이디는 이메일과 무관한 「단어 32개 중 하나 + 숫자 4자리」 무작위 생성, 10회 겹치면 503"
    rationale: "보안 리뷰가 이메일 파생 아이디의 이메일 추측 위험을 지적 — 사용자가 1안(무관한 자동 아이디) 선택. 문서 §3-2·§8, AC 갱신"

attempts:
  - round: 1
    type: ROLLBACK_PLAN
    escalated_from: RETRY_SCENARIO
    reason: "2a 독립검증 3회 V11 FAIL — 클레임 스키마 부재로 대조 기준이 없었다"
    fix: "design.claim_schema 추가 (클레임 7개 × 이상값 열 9종). 사용자 결정으로 롤백"
---

## 요약

- 변경 대상 11건(신규 4: V5 마이그레이션·HandleGenerator·PendingSignupResponse·PendingSignupRequest)
- 재사용: PendingSignupExpiredException(401 매핑), StrictStringDeserializer, verifyPendingToken, PendingTokenTestHelper, MutableClock
- 미해결 5건은 PLAN 의 `unresolved` — #2(아이디 선점은 양도 화면에서 이름 확인으로)는 흐름 5 문서로 넘긴다
