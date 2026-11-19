---
status: ACTIVE
saved_at: 2026-11-19T14:30:00Z
summary: "Phase 1 — route Backend, flows 9 · AC 10 · input_fields 10 · unresolved 15 · ops_impacts 4. 사용자 결정 2건(항상 필수, 비동기 기반 이 태스크)"

next_steps:
  - "Phase 2a — wf-scenario. 먼저 SpringApplicationBuilder 기본 프로퍼티 vs 테스트 application.properties 우선순위 프로브(unresolved #3)"

decisions:
  - decision: "MAIL_TRANSPORT·FRONTEND_BASE_URL 은 어디서나 항상 필수 — 프로필 구분을 두지 않는다"
    rationale: "사용자 결정(2026-11-19). JWT_SECRET 과 같은 방식. 로컬은 MAIL_TRANSPORT=log 를 직접 넣는다"
  - decision: "커밋 뒤 비동기 발송·간격 제한·실패 로그를 AccountMailService 로 이 태스크에서 만든다"
    rationale: "사용자 결정(2026-11-19). B·C 는 호출만 한다. 「동시 요청에도 한 통」을 실제 발송 수로 검증"
  - decision: "MailGateway·MailMessage·AccountMails 는 service, SMTP·로그 구현과 MailConfig 는 config"
    rationale: "ADR-0031·ADR-0023(QueueStore) 방식. 서비스는 JavaMailSender 를 모른다"
  - decision: "EmailToken 은 user_id 숫자 열로 매핑(JPA 연관 없음)"
    rationale: "H2(ddl-auto) 테스트의 userRepository.deleteAll() 에 FK 가 생기지 않게 — PostgreSQL 은 ON DELETE CASCADE"
  - decision: "first-deploy-checklist.md 를 implementation_spec 밖이지만 함께 고친다"
    rationale: "이 태스크만 배포해도 MAIL_TRANSPORT·FRONTEND_BASE_URL 없으면 기동 실패 — ops_impacts"
  - decision: "60초 경계는 요구사항 SQL('<')대로 정확히 60초는 보내지 않는다"
    rationale: "§3-3. AC 「60초가 지나면 보낸다」와 같은 뜻"

attempts: []
---

# CP-1.3 설계 — TASK-027

- `workflow_design/04_plan/PLAN_TASK-027.json`
