---
status: ARCHIVED
saved_at: 2026-11-20T02:00:00Z
summary: "Green 2회차 — RC-1·RC-2 수정(97c9214), 229 통과. 자체 리뷰 2라운드(상한) RC-2 변종 later 3 시나리오 밖 → RETRY_SCENARIO 2회차(사용자 결정). 주석 치환 누락 2건을 리뷰어가 찾아 75e4885·04e1221 로 보정"

next_steps:
  - "Phase 2a 로 롤백 — SC-21~24 추가, 독립검증 → 2b HITL → Phase 3 2회차"

decisions:
  - decision: "PLAN Phase 3 확인 항목 닫음 — Awaitility 는 spring-boot-starter-test 4.1.1 에 포함(pom 확인), SMTP 확인 수단은 FakeSmtpServer(2b)"
    rationale: "plan_unresolved_resolution 에 옮긴다"
  - decision: "User.account_mail_sent_at 은 insertable/updatable=false 읽기 매핑, 갱신은 네이티브 조건부 UPDATE"
    rationale: "H2 ddl 스키마에도 열이 생기고, 불러 둔 User 저장이 옛 값으로 덮어쓰지 않게"
  - decision: "AccountMails 는 @Component 가 아니라 MailConfig 가 FrontendBaseUrl.from 으로 만든다"
    rationale: "기준 주소 검증이 항상 기동 때 일어나게(사용자 결정 — 항상 필수)"
  - decision: "리팩토링 — SocialLoginConfig 의 위임 메서드를 지우고 FrontendBaseUrl.from 을 직접 부른다. 재실행 225 통과"
    rationale: "한 줄 위임만 남은 중복"

attempts:
  - n: 2
    result: "자체 리뷰 2라운드 — RC-1·RC-4 해소, RC-2 대부분 해소. later: smtps(재현)·JNDI·종료 인터럽트. RC-3 javadoc 누락(치환 실패) 지적 → 보정"
    outcome: "RETRY_SCENARIO"
    back_to: "Phase 2a"
  - n: 1
    result: "자체 리뷰 1라운드 — RC-1(clearAutomatically) · RC-2(SMTP 타임아웃·무한 큐·종료) · RC-3(issue 직접 동시 호출, 수용 위험) · RC-4(BareBoot OS 환경변수)"
    outcome: "RETRY_SCENARIO"
    back_to: "Phase 2a"
---

# CP-3.4 Green — TASK-027 (1회차, 2a 롤백)

- 자체 리뷰 원문 요지: VERIFY 대신 DEV 의 pre_review 로 2회차 Green 때 옮긴다
