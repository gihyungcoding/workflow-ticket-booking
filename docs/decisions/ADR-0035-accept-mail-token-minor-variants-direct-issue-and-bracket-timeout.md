# ADR-0035: 메일·토큰 기반의 경미한 변종 두 건(토큰 직접 동시 발급, 대괄호 표기 타임아웃)은 고치지 않고 수용한다

- **상태**: 채택됨
- **날짜**: 2026-11-20
- **관련**: ADR-0031, ADR-0032, TASK-027, `workflow_design/07_verify/VERIFY_TASK-027.json`(root_causes RC-2·RC-3), `docs/release/first-deploy-checklist.md` §6

---

## 맥락

TASK-027 이 메일 발송(`MailGateway`·`AccountMailService`)과 일회용 이메일 토큰(`EmailTokenService`)을 만들었다. 자체 리뷰와 Phase 4
리뷰가 같은 뿌리의 변종을 여럿 찾았고 대부분 고쳤다. 아래 두 건은 재현됐지만 고치지 않기로 했다(사용자 결정 2026-11-20).

1. **토큰 직접 동시 발급(RC-3)** — `EmailTokenService.issue` 는 같은 사용자·용도의 이전 토큰을 지운 뒤 새 토큰을 넣는다. 사용자 행 잠금 없이
   같은 사용자·용도로 두 번을 동시에 부르면 둘 다 지우고 둘 다 넣어 유효 토큰이 둘 남는다(Phase 4 리뷰어가 임시 PostgreSQL 에서 재현).
   지금 `issue` 를 부르는 곳은 `AccountMailService` 둘뿐이고, 둘 다 간격 제한 UPDATE 가 사용자 행을 먼저 잠근 뒤 부르므로 직렬화된다.
2. **설정 파일 대괄호 표기 타임아웃(RC-2 변종)** — SMTP 타임아웃을 `spring.mail.properties[mail.smtp.timeout]=3000` 처럼 설정 파일의 대괄호
   표기로 넣으면 `Environment.getProperty("spring.mail.properties.mail.smtp.timeout")` 가 찾지 못해 10초 기본으로 덮이고 값 검증도
   건너뛴다(리뷰어 프로브). 환경변수(`SPRING_MAIL_PROPERTIES_MAIL_SMTP_TIMEOUT`)는 완화 바인딩으로 읽힌다. 운영은 환경변수로만 설정하고,
   저장소에는 메일 설정 파일이 없다.

## 검토한 선택지

### A. 지금 고친다

- 1: `issue` 안에서 사용자 행을 `SELECT … FOR UPDATE` 로 잠그거나 `(user_id, purpose)` 에서 미사용 토큰이 하나만 있게 하는 부분 유니크 인덱스를 둔다
- 2: Spring Boot `Binder` 로 `spring.mail.properties` 맵을 읽어 Boot 가 쓰는 값과 같게 한다
- 장점: 위험이 사라진다
- 단점: 시나리오 롤백 상한(3회)을 이미 썼다 — 고치려면 설계(Phase 1)부터 다시 돈다. 둘 다 지금 경로에서는 일어나지 않는다

### B. 수용하고 조건을 문서에 남긴다 (채택)

- 장점: 지금 동작에 영향이 없다. 조건이 깨지는 순간(직접 호출자가 생김, 설정 파일로 옮김)을 문서로 알 수 있다
- 단점: 조건을 지키는 것이 사람·후속 태스크의 몫이다

## 결정

B. 두 변종은 고치지 않는다. `EmailTokenService.issue` 의 javadoc 에 「메일과 함께 발급할 때는 `AccountMailService` 를 거치고, 직접 부르는
호출자는 같은 트랜잭션에서 그 사용자 행을 먼저 잠근다」는 호출 규칙을, 배포 체크리스트 §6 에 「SMTP 타임아웃은 환경변수로만 넣는다」를 적는다.

### 이유

- 1 은 현재 호출 경로에서 일어나지 않는다 — 두 호출자 모두 간격 제한 UPDATE 가 사용자 행을 먼저 잠근다. 유효 토큰이 둘 남아도 둘 다 같은 메일함으로
  가고 만료(24시간·30분)로 사라진다
- 2 는 운영 설정 방식(Render 환경변수)에서 일어나지 않는다. 일어나도 결과는 「사용자 값 대신 10초」로, 무한 대기(RC-2 의 본래 위험)는 아니다

## 결과

- 좋아지는 것: TASK-027 을 설계 롤백 없이 닫는다
- 감수하는 것: 조건이 깨지면 위험이 살아난다
- 다시 볼 조건(하나라도 해당하면 이 ADR 을 대체하는 결정을 쓴다):
  - `AccountMailService` 밖에서 `EmailTokenService.issue` 를 부르는 코드가 생긴다(TASK-028·029 가 그렇게 설계하면)
  - 메일 설정을 설정 파일(`application*.properties`·`yml`)로 옮긴다
- 되돌리려면: 선택지 A 를 구현한다. 데이터 이전은 없다

## 검사 가능한 제약

- 없음 — 1 은 「`issue` 호출자가 `AccountMailService` 뿐」을 Grep 으로 볼 수 있으나 후속 태스크가 규칙대로 직접 부를 수 있어 금지 제약으로 두지
  않는다. Phase 4 리뷰가 호출자를 확인한다
