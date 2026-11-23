---
status: ARCHIVED
saved_at: 2026-11-20T02:00:00Z
summary: "Green 4회차(RETRY_SCENARIO 3회차 뒤) — 233 통과·spotless·아키텍처 error 0. SPRING_MAIL_HOST·MAIL_FROM 정리·검증(79241ab), 체크리스트 STARTTLS·mail.debug"

next_steps:
  - "Phase 4 2회차 — 리뷰어가 수정 커밋 포함 전체를 다시 본다"

decisions:
  - decision: "MailConfig 전면 재작성 — 부분 치환이 포맷터 재정렬 때문에 조용히 실패한 일이 3번 있었다(주석 2, 앵커 1)"
    rationale: "치환 전에 앵커가 정확히 한 번 있는지 assert 하도록 바꿨다"
  - decision: "PLAN Phase 3 확인 항목 닫음 — Awaitility 는 spring-boot-starter-test 4.1.1 에 포함(pom 확인), SMTP 확인 수단은 FakeSmtpServer(2b)"
    rationale: "plan_unresolved_resolution 에 옮긴다"
  - decision: "User.account_mail_sent_at 은 insertable/updatable=false 읽기 매핑, 갱신은 네이티브 조건부 UPDATE"
    rationale: "H2 ddl 스키마에도 열이 생기고, 불러 둔 User 저장이 옛 값으로 덮어쓰지 않게"
  - decision: "AccountMails 는 @Component 가 아니라 MailConfig 가 FrontendBaseUrl.from 으로 만든다"
    rationale: "기준 주소 검증이 항상 기동 때 일어나게(사용자 결정 — 항상 필수)"
  - decision: "리팩토링 — SocialLoginConfig 의 위임 메서드를 지우고 FrontendBaseUrl.from 을 직접 부른다. 재실행 225 통과"
    rationale: "한 줄 위임만 남은 중복"

attempts:
  - n: 4
    result: "Green — Phase 4 1회차 FAIL 수정"
    outcome: "233 통과"
    note: "체크리스트 표에 빈 줄을 넣어 표를 끊는 실수 — 확인하고 바로잡음"
  - n: 3
    result: "자체 리뷰 3라운드(롤백 뒤 수정 재리뷰) — later 3 해소, 앞자리 0 수정(38b6064), 대괄호 표기 위험 수용(사용자 결정, 체크리스트)"
    outcome: "Green"
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
