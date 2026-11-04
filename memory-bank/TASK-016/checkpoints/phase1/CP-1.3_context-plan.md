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
  - decision: "'20자 이하' 는 접미사 포함 전체 길이"
    rationale: "컬럼 30자 안이지만 화면·공유용 아이디는 짧아야 한다. 문서 §3-2 문구는 Phase 4 에서 맞춘다"

attempts: []
---

## 요약

- 변경 대상 11건(신규 4: V5 마이그레이션·HandleGenerator·PendingSignupResponse·PendingSignupRequest)
- 재사용: PendingSignupExpiredException(401 매핑), StrictStringDeserializer, verifyPendingToken, PendingTokenTestHelper, MutableClock
- 미해결 5건은 PLAN 의 `unresolved` — #2(아이디 선점은 양도 화면에서 이름 확인으로)는 흐름 5 문서로 넘긴다
