# TASK-027 시나리오

대상:
- `V9__add_email_token.sql`, `EmailToken`·`EmailTokenPurpose`·`EmailTokenRepository`, `User.accountMailSentAt`·`UserRepository`(간격 UPDATE)
- `MailGateway`·`MailMessage`·`EmailTokenService`·`AccountMailService`·`AccountMails`
- `SmtpMailGateway`·`LoggingMailGateway`·`MailConfig`, `SocialLoginConfig`(FRONTEND_BASE_URL 공용 검증)
- `docs/architecture/architecture.md` §5

엔드포인트는 없다. 시나리오의 When 은 서비스 메서드 호출(이후 TASK-028·029 가 부를 입구)이거나 애플리케이션 기동이다.

**시각은 테스트가 고정한다.** `T` 는 `MutableClock` 에 넣은 `2026-11-20T09:00:00Z` 다(`ClockTestConfig`, ADR-0005).

**DB.** 토큰·간격·마이그레이션 시나리오(SC-06~SC-14, SC-17, SC-18)는 PostgreSQL(Testcontainers) 위에서 Flyway 로 끝까지
적용한 스키마로 돈다(ADR-0006). 「DB 가 바뀌지 않는다」「DB 에 반영된다」는 클래스 레벨 `@Transactional` 없이 JDBC/리포지토리로
새로 읽어 확인한다(요구사항 §5, CLAUDE.md JPA 리뷰 지시).

**메일.** 따로 적지 않으면 `MailGateway` 는 보낸 메일을 순서대로 모으는 테스트 구현(`@Primary`)이다. 비동기 발송은 「T+5초 안에
모인 메일」로 기다려 단언하고, 「보내지 않는다」는 같은 대기 뒤에도 0통인지로 본다.

**기동.** 기동 시나리오(SC-03~SC-05)는 실제 애플리케이션을 `SpringApplicationBuilder`(서블릿, 임의 포트, H2)로 띄운다. 값은
**설정 파일(테스트 `application.properties`)을 읽지 않는 기동**에 넣는다 — 테스트 설정 파일 값이 기본 프로퍼티보다 우선해
「없음」 행을 만들 수 없기 때문이다(프로브 2026-11-19: 파일 `from-file` 이 `.properties(map)` 를 이겼고 `--인자` 는 파일을 이겼다).
기본으로 넣는 값은 `JWT_SECRET`(테스트 값)·`MAIL_TRANSPORT=log`·`FRONTEND_BASE_URL=https://app.example` 이고 행마다 하나를 바꾸거나 뺀다.

## 규약 (정본)

| 항목 | 값 |
|---|---|
| 사용자 | `E`: 이메일 가입 행(비밀번호 있음, `email_verified=false`, 이메일 `e@ticket.on`). `S`: 소셜 가입 행(비밀번호 없음, `email_verified=true`). `UserFixtures` 로 만든다 |
| 토큰 원문 | 43자, 문자 집합 `[A-Za-z0-9_-]`(URL-safe Base64, 패딩 없음) |
| 저장 해시 | `token_hash` = 원문 UTF-8 바이트의 SHA-256 을 16진 소문자로 쓴 64자 |
| TTL | `VERIFY_EMAIL` 24시간, `RESET_PASSWORD` 30분. `expires_at = 발급 시각 + TTL` |
| 유효 | `used_at IS NULL AND expires_at > now AND purpose 일치` — 정각(`now == expires_at`)은 만료 |
| 무효 결과 | `consume` 이 빈 결과(`Optional.empty()`)를 돌려준다. 예외를 던지지 않는다. DB 의 그 행(`used_at`)은 바뀌지 않는다 |
| 간격 | `account_mail_sent_at IS NULL OR account_mail_sent_at < now - 60초` 일 때만 보낸다. 보내면 `account_mail_sent_at = now`. 안 보내면 그 값이 그대로다. 결과는 `true`(보냄)/`false`(건너뜀) |
| 메일 문구 | 아래 표(요구사항 §3-4). `{B}` 는 정리된 `FRONTEND_BASE_URL`(앞뒤 공백·끝 `/` 제거) |
| 기동 실패 | 컨텍스트 기동이 예외로 끝나고, 예외 체인의 메시지 어딘가에 해당 변수 이름이 있다 |
| 발송 실패 로그 | WARN 1줄. 받는 사람은 앞 2글자만 보이고 나머지는 가린다(`e@***` 꼴이 아니라 `e@` 앞 부분 기준 — `ab***@ticket.on`). 토큰 원문·본문은 없다 |

| 메일 | 제목 | 본문에 반드시 있는 것 | 본문에 없어야 하는 것 |
|---|---|---|---|
| 인증 | `이메일 인증을 해 주세요` | `{B}/verify-email#token={원문}`, `24시간`, `직접 가입하지 않았다면 무시해 주세요` | — |
| 재설정 | `비밀번호를 다시 정해 주세요` | `{B}/reset-password#token={원문}`, `30분`, `요청하지 않았다면 무시해 주세요` | — |
| 변경 알림 | `비밀번호를 바꿨어요` | `비밀번호 찾기` | `http`, 비밀번호 문자열 |
| 소셜 안내 | `소셜 계정으로 로그인해 주세요` | `{B}/login` | `#token=` |


## SC-01 (happy) log 전송이면 메일이 로그로 남는다

- **Given** 애플리케이션이 `MAIL_TRANSPORT=log` 로 기동했다(테스트 수집 구현으로 바꾸지 않는다)
- **And** `MAIL_FROM` 은 설정하지 않았다
- **When** `MailGateway.send(MailMessage("e@ticket.on", "이메일 인증을 해 주세요", "링크 https://app.example/verify-email#token=abc"))` 를 부른다
- **Then** 예외 없이 끝난다
- **And** 출력 로그에 받는 사람 `e@ticket.on`·제목 `이메일 인증을 해 주세요`·링크 `https://app.example/verify-email#token=abc` 가 있다
- **And** 선택된 빈이 `LoggingMailGateway` 다

covers: `MAIL_TRANSPORT=log` 이면 메일(받는 사람·제목·링크)이 로그로 남고, `smtp` 이면 SMTP 로 나간다. 테스트는 보낸 메일을 모아 단언한다
flow: F1

## SC-02 (happy) smtp 전송이면 SMTP 서버가 메일을 받는다

- **Given** 테스트용 SMTP 서버가 임의 포트에서 떠 있다
- **And** 애플리케이션이 `MAIL_TRANSPORT=smtp`·`SPRING_MAIL_HOST=localhost`·`SPRING_MAIL_PORT=<그 포트>`·`MAIL_FROM=no-reply@ticket.on` 으로 기동했다
- **When** SC-01 과 같은 메일을 `MailGateway.send` 로 보낸다
- **Then** SMTP 서버가 메일 1통을 받는다
- **And** 그 메일의 From 은 `no-reply@ticket.on`, To 는 `e@ticket.on`, 제목은 `이메일 인증을 해 주세요` 다
- **And** 본문에 링크가 그대로 있다
- **And** 선택된 빈이 `SmtpMailGateway` 다

covers: `MAIL_TRANSPORT=log` 이면 메일(받는 사람·제목·링크)이 로그로 남고, `smtp` 이면 SMTP 로 나간다. 테스트는 보낸 메일을 모아 단언한다
flow: F1

## SC-03 (error) MAIL_TRANSPORT 가 정의 밖이면 기동이 실패하고 정의된 값이면 그 구현을 쓴다

- **Given** 설정 파일을 읽지 않는 기동에 기본 값을 넣었다(머리말 「기동」)
- **And** `smtp` 행은 `SPRING_MAIL_HOST=localhost`·`MAIL_FROM=no-reply@ticket.on` 을 함께 넣는다
- **When** 표의 `MAIL_TRANSPORT` 로 기동한다
- **Then** 「결과」 열대로다 — 실패 행은 예외 체인에 `MAIL_TRANSPORT` 가 있다
- **And** 성공 행은 「구현」 열의 `MailGateway` 빈이 선택된다

| 행 | MAIL_TRANSPORT | 결과 | 구현 |
|---|---|---|---|
| a | (키 없음) | 실패 | — |
| b | `""` | 실패 | — |
| c | `"   "` | 실패 | — |
| d | `"log"` | 성공 | `LoggingMailGateway` |
| e | `"smtp"` | 성공 | `SmtpMailGateway` |
| f | `"LOG"` | 성공 | `LoggingMailGateway` |
| g | `"Smtp"` | 성공 | `SmtpMailGateway` |
| h | `"  log  "` | 성공 | `LoggingMailGateway` |
| i | `"1"` | 실패 | — |
| j | `"yes"` | 실패 | — |
| k | `"true"` | 실패 | — |
| l | `"smtp,log"` | 실패 | — |
| m | `"log,"` | 실패 | — |
| n | `"lo"` | 실패 | — |
| o | `"logging"` | 실패 | — |
| p | `"smtps"` | 실패 | — |
| q | `"ses"` | 실패 | — |

covers: 운영에서 `MAIL_TRANSPORT` 가 비었거나 `FRONTEND_BASE_URL` 이 올바르지 않으면 기동이 실패한다 / `MAIL_TRANSPORT=log` 이면 메일(받는 사람·제목·링크)이 로그로 남고, `smtp` 이면 SMTP 로 나간다. 테스트는 보낸 메일을 모아 단언한다
flow: F2

## SC-04 (error) smtp 인데 SMTP 호스트나 보내는 사람이 없으면 기동이 실패한다

- **Given** 머리말 「기동」의 기본 값에 `MAIL_TRANSPORT=smtp` 를 넣었다
- **When** 표의 `SPRING_MAIL_HOST`·`MAIL_FROM` 으로 기동한다
- **Then** 「결과」 열대로다 — 실패 행은 예외 체인에 「이름」 열의 변수 이름이 있다

| 행 | SPRING_MAIL_HOST | MAIL_FROM | 결과 | 이름 |
|---|---|---|---|---|
| a | (키 없음) | `no-reply@ticket.on` | 실패 | `SPRING_MAIL_HOST` |
| b | `""` | `no-reply@ticket.on` | 실패 | `SPRING_MAIL_HOST` |
| c | `"   "` | `no-reply@ticket.on` | 실패 | `SPRING_MAIL_HOST` |
| d | `localhost` | (키 없음) | 실패 | `MAIL_FROM` |
| e | `localhost` | `""` | 실패 | `MAIL_FROM` |
| f | `localhost` | `"   "` | 실패 | `MAIL_FROM` |
| g | `localhost` | `no-reply@ticket.on` | 성공 | — |
| h | (키 없음) | (키 없음) — 단 `MAIL_TRANSPORT=log` | 성공 | — |

covers: 운영에서 `MAIL_TRANSPORT` 가 비었거나 `FRONTEND_BASE_URL` 이 올바르지 않으면 기동이 실패한다
flow: F2

## SC-05 (error) 소셜 제공자가 없어도 FRONTEND_BASE_URL 이 없거나 틀리면 기동이 실패한다

- **Given** 소셜 제공자 환경변수(`*_CLIENT_ID`·`*_CLIENT_SECRET`·`APPLE_*`)는 하나도 넣지 않았다
- **And** 나머지는 머리말 「기동」의 기본 값이다
- **When** 표의 `FRONTEND_BASE_URL` 로 기동한다
- **Then** 「결과」 열대로다 — 실패 행은 예외 체인에 `FRONTEND_BASE_URL` 이 있다
- **And** 성공 행은 기동 뒤 `AccountMails` 의 인증 메일 링크가 「링크 앞부분」 열로 시작한다

| 행 | FRONTEND_BASE_URL | 결과 | 링크 앞부분 |
|---|---|---|---|
| a | (키 없음) | 실패 | — |
| b | `""` | 실패 | — |
| c | `"   "` | 실패 | — |
| d | `ftp://app.example` | 실패 | — |
| e | `https://user@app.example` | 실패 | — |
| f | `https://app.example?x=1` | 실패 | — |
| g | `https://app.example#` | 실패 | — |
| h | `https://` | 실패 | — |
| i | `app.example` | 실패 | — |
| j | `https://app.example` | 성공 | `https://app.example/verify-email#token=` |
| k | `"  https://app.example/  "` | 성공 | `https://app.example/verify-email#token=` |
| l | `https://app.example///` | 성공 | `https://app.example/verify-email#token=` |
| m | `HTTPS://App.Example` | 성공 | `HTTPS://App.Example/verify-email#token=` |
| n | `http://localhost:5173` | 성공 | `http://localhost:5173/verify-email#token=` |

covers: 운영에서 `MAIL_TRANSPORT` 가 비었거나 `FRONTEND_BASE_URL` 이 올바르지 않으면 기동이 실패한다
flow: F2

## SC-06 (happy) 발급한 토큰의 원문은 43자이고 DB 에는 해시만 남는다

- **Given** 사용자 `E` 가 있다
- **And** 시각은 `T` 다
- **When** `EmailTokenService.issue(E.id, VERIFY_EMAIL)` 를 두 번 부른다(각각 원문 `r1`·`r2`)
- **Then** `r1`·`r2` 는 43자이고 규약의 문자 집합만 쓴다
- **And** `r1 ≠ r2` 다
- **And** JDBC 로 `email_token` 을 읽으면 E·VERIFY_EMAIL 행이 1개이고 `token_hash` 는 `sha256hex(r2)` 다
- **And** `email_token` 의 어느 행·어느 열 값에도 `r1`·`r2` 문자열이 들어 있지 않다
- **And** `expires_at` 은 `T + 24시간`, `used_at` 은 NULL 이다

covers: 발급한 토큰의 원문은 43자이고, DB 에는 SHA-256 해시만 있다(DB 조회로 원문이 없음을 확인)
flow: F3

## SC-07 (boundary) 만료는 인증 24시간·재설정 30분이고 정각에는 이미 만료다

- **Given** 사용자 `E` 에게 시각 `T` 에 표의 용도로 토큰을 발급했다
- **When** 시각을 「소모 시각」 열로 옮기고 같은 용도로 `consume(원문, 용도)` 를 부른다
- **Then** 「결과」 열대로다 — 성공이면 `E.id`, 만료면 빈 결과
- **And** 만료 행은 DB 의 `used_at` 이 NULL 그대로다
- **And** 성공 행은 `used_at` 이 소모 시각이다

| 행 | 용도 | 저장된 expires_at | 소모 시각 | 결과 |
|---|---|---|---|---|
| a | VERIFY_EMAIL | `T+24h` | `T+24h-1ms` | 성공 |
| b | VERIFY_EMAIL | `T+24h` | `T+24h` | 만료 |
| c | VERIFY_EMAIL | `T+24h` | `T+24h+1ms` | 만료 |
| d | RESET_PASSWORD | `T+30m` | `T+30m-1ms` | 성공 |
| e | RESET_PASSWORD | `T+30m` | `T+30m` | 만료 |
| f | RESET_PASSWORD | `T+30m` | `T+30m+1ms` | 만료 |

covers: 만료는 인증 24시간, 재설정 30분이다. 만료 시각 정각에는 이미 만료다
flow: F4

## SC-08 (error) 무효한 토큰은 빈 결과이고 DB 가 바뀌지 않는다

- **Given** 사용자 `E` 에게 시각 `T` 에 VERIFY_EMAIL 토큰 `r` 을 발급했다(행마다 새로 발급)
- **And** 「준비」 열이 있으면 그것을 먼저 한다
- **When** 시각 `T+1m` 에 「입력」 열의 문자열과 「용도」 열로 `consume` 을 부른다
- **Then** 예외 없이 빈 결과다
- **And** DB 의 `r` 행 `used_at` 이 「준비」 뒤의 값 그대로다(준비가 없으면 NULL)

| 행 | 입력 | 용도 | 준비 |
|---|---|---|---|
| a | `null` | VERIFY_EMAIL | — |
| b | `""` | VERIFY_EMAIL | — |
| c | `"   "` | VERIFY_EMAIL | — |
| d | `"\u3000"`·`"\u00A0"`·`"\u200B"`·`"\u2028"` 각각 | VERIFY_EMAIL | — |
| e | `r` 의 앞 42자(잘림) | VERIFY_EMAIL | — |
| f | `r` 의 앞 20자 | VERIFY_EMAIL | — |
| g | `r + "x"` | VERIFY_EMAIL | — |
| h | `r` 의 마지막 글자를 다른 글자로 | VERIFY_EMAIL | — |
| i | `r.toUpperCase()` (원문과 다를 때) | VERIFY_EMAIL | — |
| j | `" " + r`, `r + " "`, `r + "\n"`, `r + "="`, `"=" + r`, `"~" + r` 각각 | VERIFY_EMAIL | — |
| k | `r` 가운데 `"\u0000"` 삽입, `r + "\u0000"` | VERIFY_EMAIL | — |
| l | `r + "\uD800"`(짝 없는 서로게이트), `r + "é"`(비 ASCII) | VERIFY_EMAIL | — |
| m | 처음 보는 43자 원문(발급하지 않은 값) | VERIFY_EMAIL | — |
| n | `r` | RESET_PASSWORD(용도 다름) | — |
| o | `r` | VERIFY_EMAIL | `r` 을 한 번 소모했다(`used_at = T+1m`) |
| p | 10,000자 문자열 | VERIFY_EMAIL | — |

covers: 유효한 토큰은 한 번만 소모된다 — 동시에 두 요청을 보내면 한 쪽만 성공한다 / 만료는 인증 24시간, 재설정 30분이다. 만료 시각 정각에는 이미 만료다
flow: F4

## SC-09 (happy) 유효한 토큰은 한 번 소모되고 두 번째는 빈 결과다

- **Given** 사용자 `E` 에게 시각 `T` 에 RESET_PASSWORD 토큰 `r` 을 발급했다
- **When** 시각 `T+10m` 에 `consume(r, RESET_PASSWORD)` 를 부르고, 이어서 한 번 더 부른다
- **Then** 첫 호출은 `E.id` 다
- **And** DB 의 `used_at` 은 `T+10m` 이다
- **And** 두 번째 호출은 빈 결과이고 `used_at` 은 `T+10m` 그대로다

covers: 유효한 토큰은 한 번만 소모된다 — 동시에 두 요청을 보내면 한 쪽만 성공한다
flow: F4

## SC-10 (boundary) 같은 토큰을 동시에 두 번 소모하면 한 쪽만 성공한다

- **Given** 사용자 `E` 에게 VERIFY_EMAIL 토큰 `r` 을 발급했다
- **When** 별도 스레드 두 개가 각자 트랜잭션에서 `consume(r, VERIFY_EMAIL)` 를 동시에 부른다(시작 신호를 맞춘다)
- **And** 이것을 20회 반복한다(회마다 새 토큰)
- **Then** 매 회 성공은 정확히 1, 빈 결과는 정확히 1이다
- **And** 매 회 DB 의 `used_at` 은 NULL 이 아니고 한 번만 채워졌다(두 번째 UPDATE 가 값을 바꾸지 않았다)

covers: 유효한 토큰은 한 번만 소모된다 — 동시에 두 요청을 보내면 한 쪽만 성공한다
flow: F4

## SC-11 (happy) 같은 사용자·용도로 새로 발급하면 이전 토큰은 사라진다

- **Given** 사용자 `E`·`S` 가 있다
- **And** `E` 의 VERIFY_EMAIL 토큰 `a`, `E` 의 RESET_PASSWORD 토큰 `p`, `S` 의 VERIFY_EMAIL 토큰 `s` 를 발급했다
- **When** `E` 에게 VERIFY_EMAIL 토큰 `b` 를 새로 발급한다
- **Then** `consume(a, VERIFY_EMAIL)` 은 빈 결과다
- **And** DB 에 `sha256hex(a)` 행이 없고, E·VERIFY_EMAIL 행은 `b` 의 것 1개다
- **And** `consume(p, RESET_PASSWORD)` 는 `E.id`, `consume(s, VERIFY_EMAIL)` 는 `S.id` 다(다른 용도·다른 사용자는 그대로)
- **And** `consume(b, VERIFY_EMAIL)` 은 `E.id` 다

covers: 같은 사용자·용도로 새 토큰을 발급하면 이전 토큰은 사라진다
flow: F3

## SC-12 (boundary) 마지막 발송 60초 안에는 보내지 않고 지나면 보낸다

- **Given** 사용자 `E` 의 `account_mail_sent_at` 을 「마지막 발송」 열로 JDBC 로 맞췄다
- **And** 시각은 `T` 다
- **When** `AccountMailService` 로 `E` 에게 인증 메일(간격 제한 대상)을 보낸다
- **Then** 결과와 5초 뒤 모인 메일 수가 「결과」·「메일」 열대로다
- **And** DB 의 `account_mail_sent_at` 이 「뒤 값」 열이다

| 행 | 마지막 발송 | 결과 | 메일 | 뒤 값 |
|---|---|---|---|---|
| a | NULL | true | 1 | `T` |
| b | `T-1s` | false | 0 | `T-1s` |
| c | `T-59.999s` | false | 0 | `T-59.999s` |
| d | `T-60s` | false | 0 | `T-60s` |
| e | `T-60.001s` | true | 1 | `T` |
| f | `T-1h` | true | 1 | `T` |
| g | `T+1s`(시계가 뒤로 간 경우) | false | 0 | `T+1s` |

covers: 마지막 메일 발송 60초 안의 요청은 메일을 보내지 않고, 60초가 지나면 보낸다(동시 요청에도 한 통)
flow: F5

## SC-13 (boundary) 간격 제한 대상 메일을 동시에 요청해도 한 통만 나간다

- **Given** 사용자 `E` 의 `account_mail_sent_at` 은 NULL 이다
- **When** 별도 스레드 10개가 각자 트랜잭션에서 `E` 에게 인증 메일을 동시에 요청한다
- **Then** `true` 는 정확히 1개, `false` 는 9개다
- **And** 5초 뒤 모인 메일은 1통이다
- **And** DB 의 `account_mail_sent_at` 은 `T` 다

covers: 마지막 메일 발송 60초 안의 요청은 메일을 보내지 않고, 60초가 지나면 보낸다(동시 요청에도 한 통)
flow: F5

## SC-14 (error) 간격 제한 발송의 대상 사용자가 없으면 아무것도 하지 않는다 · 호출자 인자 오류

- **Given** 사용자 `E` 만 있다
- **When** 표의 인자로 부른다
- **Then** 「결과」 열대로다
- **And** 5초 뒤 모인 메일은 0통이다
- **And** `email_token` 행 수와 `E` 의 `account_mail_sent_at` 이 호출 전과 같다

| 행 | 호출 | 결과 |
|---|---|---|
| a | 간격 제한 발송, userId = `E.id + 1000`(없음) | false |
| b | 간격 제한 발송, userId = `0` | false |
| c | 간격 제한 발송, userId = `-1` | false |
| d | 간격 제한 발송, userId = `null` | `IllegalArgumentException` |
| e | `issue(null, VERIFY_EMAIL)` | `IllegalArgumentException` |
| f | `issue(0, VERIFY_EMAIL)`·`issue(-1, VERIFY_EMAIL)` | `IllegalArgumentException` |
| g | `issue(E.id + 1000, VERIFY_EMAIL)` (없는 사용자) | 예외(데이터 무결성 위반) |
| h | `issue(E.id, null)` | `IllegalArgumentException` |
| i | `consume(r, null)` — `r` 은 E 의 유효 VERIFY 토큰 | `IllegalArgumentException`, `r` 의 `used_at` NULL 그대로 |

covers: 마지막 메일 발송 60초 안의 요청은 메일을 보내지 않고, 60초가 지나면 보낸다(동시 요청에도 한 통)
flow: F5

## SC-15 (happy) 메일 네 종류의 제목·본문·링크

- **Given** `FRONTEND_BASE_URL=https://app.example/` 로 기동했다(끝 `/` 있음)
- **And** 토큰 원문은 `r` 이다
- **When** `AccountMails` 로 표의 메일을 받는 사람 `e@ticket.on` 으로 만든다
- **Then** 받는 사람은 `e@ticket.on` 이다
- **And** 제목과 본문이 규약의 「메일 문구」 표대로다(`{B}` = `https://app.example`)
- **And** 링크 안에 `//verify-email`·`//reset-password`·`//login` 처럼 겹친 `/` 가 없다
- **And** 변경 알림 본문에 `AccountMails` 에 넘긴 어떤 비밀번호 후보 문자열도 없다(넘기는 인자가 없다)

| 행 | 메일 |
|---|---|
| a | 인증(원문 `r`) |
| b | 재설정(원문 `r`) |
| c | 변경 알림 |
| d | 소셜 안내 |

covers: 메일의 링크는 `FRONTEND_BASE_URL` 로 만들어지고, 위조한 `Host` 헤더를 실어도 바뀌지 않는다
flow: F6

## SC-16 (error) 요청의 Host 를 위조해도 메일 링크는 FRONTEND_BASE_URL 이다

- **Given** `FRONTEND_BASE_URL=https://app.example` 로 기동했다
- **And** 현재 스레드의 요청 컨텍스트에 `Host: evil.example`·`X-Forwarded-Host: evil.example`·`X-Forwarded-Proto: http`·`Origin: https://evil.example` 를 가진 요청을 넣었다(`RequestContextHolder`)
- **When** 그 스레드에서 `AccountMailService` 로 `E` 에게 인증 메일을 보낸다(토큰 발급 포함)
- **Then** 모인 인증 메일 1통의 링크는 `https://app.example/verify-email#token=` 로 시작한다
- **And** 메일 본문 어디에도 `evil.example` 이 없다

covers: 메일의 링크는 `FRONTEND_BASE_URL` 로 만들어지고, 위조한 `Host` 헤더를 실어도 바뀌지 않는다
flow: F6

## SC-17 (error) 메일은 커밋 뒤 비동기로 나가고 롤백이면 나가지 않으며 발송 실패는 호출자에 영향이 없다

- **Given** 사용자 `E`(`account_mail_sent_at` NULL)가 있다
- **When** 표의 방식으로 호출한다
- **Then** 「메일」·「뒤 값」·「호출자」 열대로다

| 행 | 방식 | 메일 | 뒤 값(account_mail_sent_at) | 호출자 |
|---|---|---|---|---|
| a | 바깥 트랜잭션 안에서 간격 제한 발송 → 커밋 전에 모인 메일을 센다 → 커밋 | 커밋 전 0통, 커밋 뒤 1통 | `T` | 정상 |
| b | 바깥 트랜잭션 안에서 간격 제한 발송 → 롤백 | 5초 뒤 0통 | NULL | 정상 |
| c | 트랜잭션 없이 간격 제한 발송 | 1통 | `T` | 정상 |
| d | 수집 구현이 예외(`MailSendException`)를 던지게 하고 간격 제한 발송 | 0통(보내기 시도 1회) | `T` | 정상(`true`), 예외가 전파되지 않는다 |
| e | d 와 같은 조건 — 로그 | — | — | WARN 1줄, 받는 사람 가림 규약대로, 토큰 원문·본문 없음 |
| f | 변경 알림 발송, `account_mail_sent_at = T-10s` | 1통 | `T-10s`(바꾸지 않는다) | 정상 |
| g | 변경 알림 발송을 바깥 트랜잭션에서 하고 롤백 | 0통 | 그대로 | 정상 |

covers: `MAIL_TRANSPORT=log` 이면 메일(받는 사람·제목·링크)이 로그로 남고, `smtp` 이면 SMTP 로 나간다. 테스트는 보낸 메일을 모아 단언한다 / 마지막 메일 발송 60초 안의 요청은 메일을 보내지 않고, 60초가 지나면 보낸다(동시 요청에도 한 통)
flow: F7

## SC-18 (happy) V9 는 기존 사용자 행을 그대로 두고 적용된다

- **Given** PostgreSQL 에 V8 까지 적용했다
- **And** 이메일 가입 행(`email_verified=false`, 비밀번호 있음)과 소셜 가입 행(`email_verified=true`, 비밀번호 없음) + 소셜 연결 1행을 넣고 모든 열 값을 읽어 뒀다
- **When** 끝까지 적용한다
- **Then** 현재 버전은 `9` 다
- **And** 두 사용자 행의 기존 열 값(`email_verified` 포함)이 읽어 둔 값과 같고 `account_mail_sent_at` 은 NULL 이다
- **And** `email_token` 테이블에 `id`·`user_id`·`purpose`·`token_hash`·`expires_at`·`used_at`·`created_at` 열이 있다
- **And** 같은 `token_hash` 를 두 번 넣으면 고유 제약 위반이다
- **And** 없는 `user_id` 로 넣으면 외래 키 위반이다
- **And** 토큰을 가진 사용자 행을 지우면 그 토큰 행도 사라진다

covers: 마이그레이션이 기존 `users` 행(이메일 가입·소셜 가입)을 그대로 두고 적용된다 — `email_verified` 값이 바뀌지 않는다
flow: F8

## SC-19 (happy) architecture.md §5 에 ADR-0031 과 같은 메일 발송 행이 있다

- **Given** 저장소의 `docs/architecture/architecture.md` 가 있다
- **When** §5 외부 의존 표를 읽는다
- **Then** 대상이 `메일 발송(SMTP)` 인 행이 정확히 1개다
- **And** 그 행에 `MailGateway` 와 `ADR-0031` 이 있다
- **And** 실패 열에 「가입·소셜 로그인·이미 인증된 계정의 로그인은 동작」한다는 내용이 있다(ADR-0031 「가입·로그인은 메일 서비스 장애와 무관하게 동작」)

covers: `architecture.md` §5 에 메일 발송 행이 있다(ADR-0031 의 결정과 같은 내용)
flow: F9
