# TASK-016 시나리오

대상: `HandleGenerator`(아이디 생성), `AuthService`(`signUp`·`completeSocialSignUp`·`getMe`·대기 정보 조회),
`JwtTokenProvider`(`PendingSignup.expiresAt`), `AuthController`(`POST /api/auth/social/pending`),
`AuthExceptionHandler`(503), `V5__add_users_handle.sql`.

**난수원은 테스트가 고정한다.** 아래 「뽑힌 값」은 주입한 `RandomGenerator` 가 차례로 내는
(단어 번호, 숫자) 쌍이다. 운영은 `SecureRandom` 이다.

**SC-07 만 PostgreSQL(Testcontainers) 위에서 Flyway 로 돈다.** 나머지는 기존 H2 테스트 구성 그대로다.

## 규약 (정본)

시나리오가 이 값을 그대로 단언한다. 바꾸려면 여기부터 고친다.

| 항목 | 값 |
|---|---|
| 아이디 형식 | `<단어><숫자 4자리>` — 정규식 `^[a-z]{3,6}[0-9]{4}$`. 숫자는 앞을 0 으로 채운다 (`42` → `0042`) |
| 단어 목록 (번호 0~31, 이 순서) | `wave star stage encore aria beat chord drum echo flute harp jazz lyric melody note opera piano rhythm solo tempo tune verse viola waltz cello choir disco folk hymn oboe lute gig` |
| 이메일·이름 | 아이디 생성에 쓰지 않는다 |
| 겹침 | 이미 있는 아이디가 뽑히면 다시 뽑는다. 10번째 뽑기까지 모두 겹치면 실패 |
| 실패 응답 | 503 `{"code":"HANDLE_GENERATION_FAILED", ...}` |
| pendingToken | HS256, `typ=pending`, 발급 시각 + 300초 만료(그 정각부터 만료) — 기존 규약(TASK-011). 클레임 타입·규칙은 PLAN `design.claim_schema` |
| 대기 정보 응답 | 200 `{"provider":"KAKAO","email":"…","name":"…","expiresAt":"<ISO-8601 UTC>"}` |
| 오류 코드 | `PENDING_SIGNUP_EXPIRED`(401) · `INVALID_REQUEST`(400) · `EMAIL_ALREADY_EXISTS`(409) · `HANDLE_GENERATION_FAILED`(503) |

## SC-01 (happy) 이메일 가입 응답과 내 정보에 형식에 맞는 아이디가 있다

- **Given** `users` 가 비어 있다
- **And** 뽑힌 값이 (0, 7302) 이다
- **When** `harin.kim@kakao.com` / 이름 `김하린` / 역할 `AUDIENCE` 로 `POST /api/auth/signup` 을 호출한다
- **Then** 201 이고 응답의 `handle` 은 `wave7302` 다
- **And** 같은 이메일·비밀번호로 `POST /api/auth/login` 을 하면 응답 `user.handle` 이 `wave7302` 다
- **And** 그 응답의 accessToken 으로 `GET /api/auth/me` 를 호출하면 `handle` 이 `wave7302` 다
- **And** `users` 를 새로 읽으면 그 행의 `handle` 이 `wave7302` 다

covers: 이메일 가입 응답과 `GET /api/auth/me` 응답에 `handle` 이 있고, 단어 목록의 단어 하나 + 숫자 4자리(`^[a-z]{3,6}[0-9]{4}$`) 형식이다
flow: F1

## SC-02 (happy) 단어 번호와 숫자의 경계에서도 형식이 맞다

- **Given** `users` 가 비어 있다
- **And** 뽑힌 값이 아래 표의 쌍이다 (행마다 새로 가입한다)
- **When** 이메일 가입을 한다
- **Then** 응답의 `handle` 이 표와 같고 `^[a-z]{3,6}[0-9]{4}$` 에 맞는다

| 뽑힌 값 | handle | 확인하는 것 |
|---|---|---|
| (0, 0) | `wave0000` | 첫 단어 · 숫자 하한 · 0 채움 |
| (0, 42) | `wave0042` | 두 자리 숫자 0 채움 |
| (7, 5) | `drum0005` | 한 자리 숫자 0 채움 |
| (3, 1000) | `encore1000` | 6자 단어 (최대 길이) |
| (31, 9999) | `gig9999` | 마지막 단어 · 3자 단어 · 숫자 상한 |

covers: 이메일 가입 응답과 `GET /api/auth/me` 응답에 `handle` 이 있고, 단어 목록의 단어 하나 + 숫자 4자리(`^[a-z]{3,6}[0-9]{4}$`) 형식이다
flow: F1

## SC-03 (happy) 아이디는 이메일·이름과 무관하다

- **Given** `users` 가 비어 있다
- **When** 아래 표의 이메일·이름으로, 표의 뽑힌 값일 때 가입한다 (행마다 데이터를 비운다)
- **Then** `handle` 이 표와 같다 — 이메일·이름이 달라도 뽑힌 값이 같으면 같은 아이디이고, 이메일 앞부분이 단어처럼 보여도 쓰이지 않는다

| 이메일 | 이름 | 뽑힌 값 | handle |
|---|---|---|---|
| `harin.kim@kakao.com` | `김하린` | (1, 123) | `star0123` |
| `minji.jung@gmail.com` | `정민지` | (1, 123) | `star0123` |
| `wave9999@example.com` | `wave` | (1, 123) | `star0123` |

covers: 아이디는 단어 목록과 숫자로만 만들어지고 이메일·이름 값이 생성에 쓰이지 않는다 (같은 난수 순서면 이메일이 달라도 같은 아이디가 나온다)
flow: F1

## SC-04 (happy) 이미 있는 아이디가 뽑히면 다시 뽑는다

- **Given** 아이디 `wave7302` 인 회원이 이미 있다
- **And** 다음 가입에서 뽑힌 값이 차례로 (0, 7302), (0, 7302), (1, 1) 이다
- **When** `new@example.com` 으로 이메일 가입한다
- **Then** 201 이고 `handle` 은 `star0001` 이다
- **And** `users` 에 `wave7302` 인 행은 여전히 하나다

covers: 뽑은 아이디가 이미 있으면 다시 뽑아 겹치지 않는 아이디를 주고, 10번 연속 겹치면 가입이 503 `HANDLE_GENERATION_FAILED` 로 실패하며 `users` 행이 생기지 않는다
flow: F1

## SC-05 (error) 10번 연속 겹치면 가입이 503 이고 아무것도 만들지 않는다

- **Given** 아이디 `wave7302` 인 회원이 이미 있고 `users` 행 수를 기록했다
- **And** 다음 가입에서 뽑힌 값이 아래 표와 같다
- **When** `new@example.com` 으로 이메일 가입한다
- **Then** 결과가 표와 같다

| 뽑힌 값 | 결과 |
|---|---|
| (0, 7302) 9번, 그다음 (1, 1) | 201, `handle` = `star0001` (10번째 뽑기는 아직 시도 안에 든다) |
| (0, 7302) 10번 | 503 `HANDLE_GENERATION_FAILED`, `users` 행 수 그대로, `new@example.com` 행 없음 |

covers: 뽑은 아이디가 이미 있으면 다시 뽑아 겹치지 않는 아이디를 주고, 10번 연속 겹치면 가입이 503 `HANDLE_GENERATION_FAILED` 로 실패하며 `users` 행이 생기지 않는다
flow: F1

## SC-06 (happy) 소셜 최초 가입으로 만든 회원도 아이디를 받는다

- **Given** 유효한 pendingToken(제공자 `KAKAO`, 이메일 `harin.kim@kakao.com`, 이름 `김하린`)이 있다
- **And** 아이디 `wave7302` 인 회원이 이미 있고, 뽑힌 값이 차례로 (0, 7302), (2, 77) 이다
- **When** 역할 `AUDIENCE` 로 `POST /api/auth/social/complete` 를 호출한다
- **Then** 200 이고 응답 `user.handle` 은 `stage0077` 이다
- **And** 응답의 accessToken 으로 `GET /api/auth/me` 를 호출하면 `handle` 이 `stage0077` 이다

covers: 소셜 최초 가입(`/social/complete`)으로 만든 회원도 `handle` 을 받는다
flow: F3

## SC-07 (happy) 마이그레이션 뒤 기존 회원 전원이 겹치지 않는 아이디를 갖는다

PostgreSQL 컨테이너에서 Flyway 를 V4 까지 적용한 뒤 V5 를 적용한다.

- **Given** V4 까지 적용된 `users` 에 아래 표의 회원이 있다
- **When** V5 를 적용한다
- **Then** 표의 모든 행의 `handle` 이 NULL 이 아니고 `^[a-z]{3,6}[0-9]{4}$` 에 맞는다
- **And** 숫자를 뗀 단어가 규약 표의 단어 목록 안에 있다
- **And** `handle` 값이 서로 모두 다르다
- **And** 이어서 `handle` 없이 회원을 저장하면 제약 위반으로 실패한다
- **And** 이미 있는 `handle` 로 회원을 저장하면 제약 위반으로 실패한다

| 이메일 | 비밀번호 해시 | email_verified |
|---|---|---|
| `harin.kim@kakao.com` | 없음 (소셜) | true |
| `harin.kim@naver.com` | 있음 | false |
| `wave7302@example.com` | 있음 | false |
| `ab@example.com` | 있음 | false |
| `김하린@example.com` | 있음 | false |

covers: 마이그레이션 뒤 기존 회원 전원이 같은 형식의 겹치지 않는 `handle` 을 갖는다 (NOT NULL·UNIQUE, 단어는 Java 목록 안)
flow: F4

## SC-13 (happy) 회원이 없을 때도 마이그레이션이 성공한다

PostgreSQL 컨테이너에서 Flyway 를 V4 까지 적용한 뒤 V5 를 적용한다.

- **Given** V4 까지 적용된 `users` 가 비어 있다
- **When** V5 를 적용한다
- **Then** 적용이 성공한다
- **And** 이어서 `handle` 없이 회원을 저장하면 제약 위반으로 실패한다

covers: 마이그레이션 뒤 기존 회원 전원이 같은 형식의 겹치지 않는 `handle` 을 갖는다 (NOT NULL·UNIQUE, 단어는 Java 목록 안)
flow: F4

## SC-08 (happy) 소셜 가입 대기 정보를 돌려준다

- **Given** 시각 T 에 발급한 유효한 pendingToken(제공자는 표의 값, 이메일 `harin.kim@kakao.com`, 이름 `김하린`)이 있다
- **And** `users`·`social_accounts` 의 현재 행 수를 기록했다
- **When** 아래 표의 시각에 `POST /api/auth/social/pending` 을 `{"pendingToken": "<토큰>"}` 으로 호출한다
- **Then** 200 이고 본문이 `{"provider":"<표의 제공자>","email":"harin.kim@kakao.com","name":"김하린","expiresAt":"<T+300초, 초 단위 ISO-8601 UTC>"}` 다
- **And** `users`·`social_accounts` 행 수가 그대로다
- **And** 이메일 클레임이 아래 「정규화」 표의 값이면 200 이고 `email` 이 표의 값이다 (제공자 `KAKAO`, 시각 T)

| 이메일 클레임 (원문) | 응답 `email` |
|---|---|
| `  Harin.Kim@KAKAO.com  ` (앞뒤 공백·대문자) | `harin.kim@kakao.com` |
| 소문자 255자 이메일 (최대) | 같은 값 |
| 앞뒤 공백 1개씩 + 대문자를 섞은 255자 이메일 (원문 257자) | 소문자·공백 제거한 255자 |


| 제공자 | 호출 시각 |
|---|---|
| `KAKAO` | T (발급 직후 — exp 가 now+300초와 같다) |
| `KAKAO` | T + 299초 (만료 1초 전) |
| `NAVER` | T |
| `GOOGLE` | T |
| `APPLE` | T |

covers: `POST /api/auth/social/pending` 이 유효한 토큰에 제공자·이메일·이름·만료 시각을 돌려준다
flow: F5

## SC-09 (error) 만료·위조된 pendingToken 으로 대기 정보를 조회하면 401 이다

- **Given** `users`·`social_accounts` 의 현재 행 수를 기록했다
- **And** 요청의 `pendingToken` 이 아래 표의 상태다
- **When** `POST /api/auth/social/pending` 을 호출한다
- **Then** 401 `{"code":"PENDING_SIGNUP_EXPIRED", ...}` 이다
- **And** 표의 모든 행에서 응답의 `code` 와 `message` 가 같다 (만료와 위조를 구분해 알리지 않는다)
- **And** `users`·`social_accounts` 행 수가 그대로다

**토큰 자체** (서명·형식·시각)

| 토큰 상태 |
|---|
| 만료 정각 (T + 300초에 호출) |
| 만료 후 (T + 301초) |
| 다른 키로 서명 |
| 페이로드를 바꿈 (이메일 변조, 서명 그대로) |
| 형식이 깨짐 (임의 문자열 `abc`) |
| 8,192자 임의 문자열 (길이 상한은 두지 않는다 — 저장하지 않고 서명 검증에서 거부된다) |
| 요청 문자열에 NUL 문자 / 짝 없는 서로게이트(`\ud800`) 가 섞임 |

**서명은 맞지만 클레임이 규칙 밖** — 기준은 PLAN `design.claim_schema`. 올바른 키로 서명했고, 표의 클레임 하나만 바꾸고 나머지는 유효하다.

| 클레임 (규칙) | 누락 | `null` | 빈 문자열 | 공백만 | 타입 다름 | NUL 포함 | 짝 없는 서로게이트 | 길이·범위 밖 | 형식·열거 밖 |
|---|---|---|---|---|---|---|---|---|---|
| `typ` (문자열, 정확히 `pending`) | ✓ | ✓ | ✓ | ✓ | 숫자 `1` · 객체 `{}` · 배열 `[]` · 불리언 `true` | `pend\u0000ing` | `pending\ud800` | 해당 없음 — 값이 하나로 정해져 있다 | `access` · `PENDING` |
| `exp` (정수 초, now < exp ≤ now+300초) | ✓ | ✓ | `""` | `"   "` | 문자열 `"later"` · 객체 `{}` · 배열 `[]` · 불리언 `true` | 해당 없음 — 숫자 | 해당 없음 — 숫자 | 소수 `T+120.5` · `T+301` (먼 미래) · `9.3e18` (Instant 범위 초과, 양) · `0` · 음수 `-1` · `-9.3e18` (Instant 범위 초과, 음) | 해당 없음 |
| `email` (문자열, 정규화 후 1~255자 이메일) | ✓ | ✓ | ✓ | ✓ | 숫자 `1` · 객체 `{}` · 배열 `[]` · 불리언 `true` | ✓ | ✓ | 정규화 후 256자 (소문자·공백 없이 256자 / 대문자를 섞어 256자) | `@` 없음 · `@` 두 개 · `@` 앞이 빔 `@a.com` · `@` 뒤가 빔 `a@` · 내부 공백 `a b@c.com` · 내부 NBSP `a\u00a0b@c.com` · 서식 문자 `\u200b` 포함 |
| `name` (문자열, 1~100자) | ✓ | ✓ | ✓ | ✓ | 숫자 `1` · 객체 `{}` · 배열 `[]` · 불리언 `true` | ✓ | ✓ | 101자 | 해당 없음 — 형식 규칙이 없다 |
| `provider` (문자열, `KAKAO` `NAVER` `GOOGLE` `APPLE`) | ✓ | ✓ | ✓ | ✓ | 숫자 `1` · 객체 `{}` · 배열 `[]` · 불리언 `true` | `KAKAO\u0000` | `KAKAO\ud800` | 해당 없음 — 열거값 | 정의 밖 `FOO` · 소문자 `kakao` |
| `providerUserId` (문자열, 1~255자) | ✓ | ✓ | ✓ | ✓ | 숫자 `1` · 객체 `{}` · 배열 `[]` · 불리언 `true` | ✓ | ✓ | 256자 | 해당 없음 — 형식 규칙이 없다 |
| `emailVerified` (JSON 불리언) | ✓ | ✓ | `""` | `"  "` | 문자열 `"true"` · `"TRUE"` · 숫자 `1` · `0` · 객체 `{}` · 배열 `[]` | 해당 없음 — 불리언 | 해당 없음 — 불리언 | 해당 없음 | 해당 없음 |

✓ 와 값이 적힌 칸마다 한 번씩 호출한다 (값이 여러 개면 값마다). 「해당 없음」은 그 칸이 그 클레임의 타입에 없는 경우다.

- **And** 어느 행에서도 응답 본문에 토큰 안의 이메일·이름 값이 나오지 않는다

covers: 만료·위조 토큰이면 401 `PENDING_SIGNUP_EXPIRED` 이고 DB 변화가 없다
flow: F6

## SC-14 (error) 가입 완료도 같은 클레임 규칙으로 거부하고 아무것도 만들지 않는다

대기 정보 조회와 가입 완료는 같은 토큰 검증을 쓴다. 이번에 그 검증에 새 규칙(exp 상한·소수·범위, typ 타입 등)이 들어가므로
가입 완료 입구에서도 확인한다.

- **Given** `users`·`social_accounts` 의 현재 행 수를 기록했다
- **And** 요청의 `pendingToken` 이 SC-09 의 「토큰 자체」 표와 「클레임」 표의 각 칸 상태다 (SC-09 와 같은 값)
- **When** 역할 `AUDIENCE` 로 `POST /api/auth/social/complete` 를 호출한다
- **Then** 모든 칸에서 401 `{"code":"PENDING_SIGNUP_EXPIRED", ...}` 이다
- **And** `users`·`social_accounts` 행 수가 그대로다

covers: 만료·위조 토큰이면 401 `PENDING_SIGNUP_EXPIRED` 이고 DB 변화가 없다
flow: F6

## SC-10 (error) 대기 정보 요청의 형식이 틀리면 400 이다

- **Given** `users`·`social_accounts` 의 현재 행 수를 기록했다
- **When** `POST /api/auth/social/pending` 을 아래 표의 본문으로 호출한다
- **Then** 400 `{"code":"INVALID_REQUEST", ...}` 이다
- **And** `users`·`social_accounts` 행 수가 그대로다

| 본문 |
|---|
| `pendingToken` 누락 (`{}`) / `null` |
| `pendingToken` 이 빈 문자열 / 공백만 |
| `pendingToken` 이 문자열이 아님: 숫자 / 객체 / 배열 / 불리언 |
| 본문이 비어 있음 / JSON 이 아님 |

covers: `POST /api/auth/social/pending` 이 유효한 토큰에 제공자·이메일·이름·만료 시각을 돌려준다
flow: F5

## SC-11 (regression) 이메일 중복 가입은 여전히 409 이다

- **Given** `harin.kim@kakao.com` 회원이 이미 있다
- **When** 같은 이메일(대문자 `HARIN.KIM@KAKAO.COM` 포함)로 이메일 가입한다
- **Then** 409 `EMAIL_ALREADY_EXISTS` 이고 `users` 행 수가 그대로다
- **And** 기존 회원의 `handle` 이 바뀌지 않는다

covers: 이메일 가입 응답과 `GET /api/auth/me` 응답에 `handle` 이 있고, 단어 목록의 단어 하나 + 숫자 4자리(`^[a-z]{3,6}[0-9]{4}$`) 형식이다
flow: F1

## SC-12 (error) 소셜 가입 완료에서도 10번 연속 겹치면 503 이고 아무것도 만들지 않는다

- **Given** 유효한 pendingToken(제공자 `KAKAO`, 이메일 `harin.kim@kakao.com`)이 있다
- **And** 아이디 `wave7302` 인 회원이 이미 있고 `users`·`social_accounts` 행 수를 기록했다
- **And** 다음 가입에서 뽑힌 값이 아래 표와 같다
- **When** 역할 `AUDIENCE` 로 `POST /api/auth/social/complete` 를 호출한다
- **Then** 결과가 표와 같다

| 뽑힌 값 | 결과 |
|---|---|
| (0, 7302) 9번, 그다음 (1, 1) | 200, `user.handle` = `star0001` |
| (0, 7302) 10번 | 503 `HANDLE_GENERATION_FAILED`, `users`·`social_accounts` 행 수 그대로 |

covers: 소셜 최초 가입(`/social/complete`)으로 만든 회원도 `handle` 을 받는다; 뽑은 아이디가 이미 있으면 다시 뽑아 겹치지 않는 아이디를 주고, 10번 연속 겹치면 가입이 503 `HANDLE_GENERATION_FAILED` 로 실패하며 `users` 행이 생기지 않는다
flow: F3

---

## 시나리오로 쓰지 않은 것

- **동시 가입으로 같은 아이디가 경합해 UNIQUE(`ux_users_handle`) 위반이 나는 경우** — 존재 확인과 INSERT 사이의
  경합이라 결정적으로 재현하기 어렵다. 구현은 위반 원인이 아이디면 이메일 중복(409)으로 오보하지 않는다(PLAN
  unresolved #3). Phase 4 코드 리뷰에서 확인한다
- **`/social/complete` 의 요청 본문 형식(400)** — 바뀌지 않는 기존 입력이고 기존 `SocialCompleteApiTest`(TASK-011 SC-11)가 지킨다.
  클레임 규칙은 이번에 바뀌므로 SC-14 가 가입 완료 입구에서 다시 확인한다
- **마이그레이션 중 무작위 값이 겹쳐 다시 뽑는 경우** — 32만 개 공간에서 시험 규모로는 재현할 수 없다. SC-07 의 「값이 서로
  모두 다르다」와 UNIQUE 제약이 결과를 지킨다. 재시도 루프는 Phase 4 코드 리뷰에서 확인한다
- **쓰기 가드(`X-Write-Key`)** — 모든 POST 에 걸리는 기존 동작이다. 새 엔드포인트에 예외를 두지 않으므로 별도
  시나리오를 두지 않는다
