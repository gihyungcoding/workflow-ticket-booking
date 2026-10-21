# TASK-009 시나리오

대상: `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/auth/me`, Security 필터 체인, JWT.
경로는 출처 문서 §3 의 `/api/auth/*` 를 따른다 (완료 조건의 `/auth/*` 표기와 같다).

공통 전제 — 별도 언급이 없으면 쓰기 가드는 꺼져 있고(`WRITE_GUARD_ENABLED` 미설정), 요청에 CSRF 토큰·쿠키·`Authorization` 은 없다.
에러 본문은 모두 기존 `ErrorResponse` 형식 `{code, message}` 이다. 표의 각 행은 **독립된 단언**이다 — 한 행이 실패해도 다른 행과 구분돼야 한다.

## SC-01 (happy) 올바른 정보로 가입하면 201 과 비밀번호 없는 사용자 정보를 받는다

- **Given** `users` 가 비어 있다
- **When** 아래 표의 각 행으로 `POST /api/auth/signup` 을 호출한다 (`password` · `passwordConfirm` 은 `password123`, `name` 은 `홍길동`)
- **Then** 모든 행이 201 이다
- **And** 응답 본문의 키는 정확히 `id`, `email`, `name`, `role` 네 개다 (`password`·`passwordConfirm`·`passwordHash` 를 포함한 어떤 비밀번호 관련 키도 없다)
- **And** 응답의 `email` 은 표의 "기대 email", `role` 은 요청한 값과 같고 `id` 는 숫자다

| 요청 email | role | 기대 email |
|---|---|---|
| `user@example.com` | `AUDIENCE` | `user@example.com` |
| `organizer@example.com` | `ORGANIZER` | `organizer@example.com` |
| `  User@Example.COM  ` (앞뒤 공백·대문자) | `AUDIENCE` | `user@example.com` |

covers: 올바른 정보로 POST /auth/signup 호출 시 201과 함께 UserResponse(비밀번호 제외)를 반환한다
flow: F1

## SC-02 (happy) 저장된 비밀번호는 평문이 아니라 BCrypt 해시다

- **Given** `user@example.com` / `password123` 으로 가입이 끝났다
- **When** `users` 의 해당 행을 요청과 별개로 새로 조회한다 (가입 요청과 같은 트랜잭션이 아니다)
- **Then** DB 에 저장된 비밀번호 값(`password_hash` 컬럼)은 `password123` 과 다르다
- **And** 그 값은 BCrypt 형식(`$2a$`·`$2b$`·`$2y$` 로 시작하는 60자)이다
- **And** 그 값은 `password123` 의 BCrypt 해시로 검증되고(일치), `password124` 의 해시로는 검증되지 않는다(불일치)

covers: 저장된 비밀번호가 평문이 아니라 BCrypt 해시임을 DB 조회로 확인한다
flow: F3

## SC-03 (error) 이미 있는 이메일로 가입하면 409 다

- **Given** `user@example.com` 이 이름 `홍길동`, 비밀번호 `password123` 으로 가입돼 있다
- **When** 아래 표의 각 행으로 다른 이름(`김철수`)·다른 비밀번호(`password999`)로 가입을 요청한다
- **Then** 모든 행이 409 이고 `code` 는 `EMAIL_ALREADY_EXISTS` 다
- **And** `users` 의 행 수는 1 그대로다
- **And** 기존 사용자의 이름은 `홍길동`, 비밀번호 해시는 처음 저장된 값 그대로다

| 요청 email | 설명 |
|---|---|
| `user@example.com` | 같은 표기 |
| `USER@EXAMPLE.COM` | 대소문자만 다름 |
| `  user@example.com  ` | 앞뒤 공백만 다름 |

covers: 이미 존재하는 이메일로 가입 시도 시 409를 반환한다
flow: F2

## SC-04 (error) 필수 값이 없거나 형식이 틀리면 400 이다

- **Given** `users` 가 비어 있다
- **When** 유효한 요청에서 아래 표의 항목 하나만 바꿔 가입을 요청한다
- **Then** 모든 행이 400 이고 `code` 는 `INVALID_REQUEST` 다
- **And** `users` 에는 행이 생기지 않는다

| 항목 | 바꾼 값 |
|---|---|
| `email` | 누락 / `null` / `""` / `"   "` |
| `email` 형식 | `plainaddress` / `a@` / `@b.com` / `a b@example.com` |
| `password` | 누락 / `null` / `""` / 공백 8개 (`"        "`) |
| `passwordConfirm` | 누락 / `null` / `""` |
| `name` | 누락 / `null` / `""` / `"   "` |
| `role` | 누락 / `null` |
| `role` 정의 밖 값 | `"ADMIN"` / `"audience"` (소문자) / `1` (숫자) |
| 본문 | 본문 없음 / 깨진 JSON |

covers: 올바른 정보로 POST /auth/signup 호출 시 201과 함께 UserResponse(비밀번호 제외)를 반환한다
flow: F9

## SC-05 (boundary) 필드 길이는 상한 +1 에서 거부되고 상한 정확히는 통과한다

- **Given** `users` 가 비어 있다
- **When** 유효한 요청에서 아래 표의 항목 하나만 바꿔 가입을 요청한다 (통과 행은 행마다 서로 다른 이메일을 쓴다)
- **Then** 각 행의 상태가 "기대" 와 같다
- **And** 400 행에서는 `code` 가 `INVALID_REQUEST` 이고 `users` 에 행이 생기지 않는다 (500 이 아니다)

| 항목 | 입력 | 기대 |
|---|---|---|
| `email` | 총 255자 (형식이 유효한 주소) | 201 |
| `email` | 총 256자 | 400 |
| `password` | ASCII 7자 | 400 |
| `password` | ASCII 8자 | 201 |
| `password` | ASCII 72자 | 201 |
| `password` | ASCII 73자 | 400 |
| `name` | 100자 | 201 |
| `name` | 101자 | 400 |

covers: 올바른 정보로 POST /auth/signup 호출 시 201과 함께 UserResponse(비밀번호 제외)를 반환한다
flow: F9

## SC-06 (boundary) 비밀번호는 UTF-8 72바이트를 넘지 않고 확인값과 같아야 한다

- **Given** `users` 가 비어 있다
- **When** 아래 표의 각 행으로 가입을 요청한다 (`password` 와 `passwordConfirm` 은 표의 값)
- **Then** 각 행의 상태가 "기대" 와 같다
- **And** 400 행에서는 `code` 가 `INVALID_REQUEST` 이고 응답 본문에 입력한 비밀번호 문자열이 없다
- **And** 400 행에서는 `users` 에 행이 생기지 않고, 500 이 아니다

| password | passwordConfirm | 설명 | 기대 |
|---|---|---|---|
| 한글 24자 (72바이트) | 같은 값 | 글자 수·바이트 모두 한도 이내 | 201 |
| 한글 25자 (75바이트) | 같은 값 | 글자 수는 72 이하지만 바이트가 한도 초과 | 400 |
| 이모지 19개 (76바이트) | 같은 값 | UTF-16 길이는 72 이하지만 바이트가 한도 초과 | 400 |
| `password123` | `password124` | 확인값 불일치 | 400 |
| `password123` | `Password123` | 대소문자만 다른 확인값 | 400 |

covers: 올바른 정보로 POST /auth/signup 호출 시 201과 함께 UserResponse(비밀번호 제외)를 반환한다
flow: F9

## SC-07 (happy) 올바른 이메일/비밀번호로 로그인하면 200 과 유효한 accessToken 을 받는다

- **Given** `user@example.com` / `password123` 으로 가입돼 있다 (이름 `홍길동`, role `AUDIENCE`, id 는 N)
- **When** 아래 표의 각 행으로 `POST /api/auth/login` 을 호출한다
- **Then** 모든 행이 200 이다
- **And** `accessToken` 은 비어 있지 않은 점 두 개로 구분된 세 부분 문자열이고, `tokenType` 은 `Bearer`, `expiresInSeconds` 는 `86400` 이다
- **And** `user` 는 `{id: N, email: "user@example.com", name: "홍길동", role: "AUDIENCE"}` 이고 비밀번호 관련 키가 없다
- **And** 토큰의 클레임은 `sub = "N"`, `email = "user@example.com"`, `role = "AUDIENCE"`, `typ = "access"` 이고 `exp − iat` 은 86400초다

| 요청 email | password |
|---|---|
| `user@example.com` | `password123` |
| `  USER@Example.com  ` (앞뒤 공백·대문자) | `password123` |

covers: 올바른 이메일/비밀번호로 POST /auth/login 호출 시 200과 유효한 accessToken을 반환한다
flow: F4

## SC-08 (error) 틀린 비밀번호와 없는 이메일은 똑같은 401 을 받는다

- **Given** `user@example.com` / `password123` 으로 가입돼 있다
- **When** 아래 표의 각 행으로 로그인을 요청한다
- **Then** 두 행 모두 401 이고 `code` 는 `INVALID_CREDENTIALS` 다
- **And** 두 행의 응답 본문 전체(`code` 와 `message`)가 글자 단위로 같다
- **And** 응답 본문에 `accessToken` 이 없다

| email | password | 설명 |
|---|---|---|
| `user@example.com` | `wrong-password` | 틀린 비밀번호 |
| `nobody@example.com` | `password123` | 존재하지 않는 이메일 |

covers: 틀린 비밀번호 또는 존재하지 않는 이메일로 로그인 시 401을 반환하며, 두 경우 응답 메시지가 동일하다
flow: F5

## SC-09 (error) 로그인 요청의 필수 값·형식이 틀리면 401 이 아니라 400 이다

- **Given** `user@example.com` / `password123` 으로 가입돼 있다
- **When** 유효한 요청에서 아래 표의 항목 하나만 바꿔 로그인을 요청한다
- **Then** 모든 행이 400 이고 `code` 는 `INVALID_REQUEST` 다 (500 이 아니고 401 도 아니다)
- **And** 응답 본문에 `accessToken` 이 없다

| 항목 | 바꾼 값 |
|---|---|
| `email` | 누락 / `null` / `""` / `"   "` |
| `email` 형식 | `plainaddress` |
| `password` | 누락 / `null` / `""` |
| 본문 | 본문 없음 / 깨진 JSON |

covers: 틀린 비밀번호 또는 존재하지 않는 이메일로 로그인 시 401을 반환하며, 두 경우 응답 메시지가 동일하다
flow: F5

## SC-10 (happy) 유효한 토큰으로 /me 를 조회하면 현재 사용자 정보를 받는다

- **Given** `user@example.com` 으로 가입·로그인해 `accessToken` 을 갖고 있다
- **When** 아래 표의 각 `Authorization` 값으로 `GET /api/auth/me` 를 호출한다
- **Then** 모든 행이 200 이다
- **And** 본문은 `{id, email, name, role}` 이고 로그인 응답의 `user` 와 같다
- **And** 비밀번호 관련 키가 없다

| Authorization | 설명 |
|---|---|
| `Bearer <accessToken>` | 표준 표기 |
| `bearer <accessToken>` / `BEARER <accessToken>` | 인증 스킴은 대소문자를 구분하지 않는다 (RFC 7235) |

covers: 유효한 토큰으로 GET /auth/me 호출 시 200과 해당 사용자 정보를 반환한다
flow: F6

## SC-11 (error) 인증 정보가 없으면 /me 와 허용 목록 밖 경로는 401 이다

- **Given** 가입된 사용자가 있다
- **When** 아래 표의 각 요청을 보낸다
- **Then** 모든 행이 401 이고 `code` 는 `UNAUTHORIZED` 다
- **And** 응답은 `Content-Type: application/json` 이고 본문은 `{code, message}` 형태이며 사용자 정보가 없다

| 요청 | 설명 |
|---|---|
| `GET /api/auth/me` (`Authorization` 없음) | 헤더 부재 |
| `GET /api/auth/me` + `Authorization: Basic dXNlcjpwYXNz` | Bearer 가 아닌 스킴 |
| `GET /api/auth/me` + `Authorization: Bearer ` | 빈 토큰 |
| `GET /api/auth/me` + `Authorization: BearerXYZ` / `Authorization: Bearer<유효 토큰>` (공백 없음) | `Bearer` 로 시작하기만 하는 값을 Bearer 로 오인하지 않는다 |
| `GET /api/auth/me` + `Authorization: Bearer  <유효 토큰>` (공백 두 개) | 스킴과 토큰 사이 구분은 공백 한 개다 — 엄격하게 거부 |
| `GET /api/auth/me` + `Authorization: Bearer <유효 토큰> extra` | 토큰 뒤에 다른 값이 붙은 경우 |
| `GET /api/not-registered` (`Authorization` 없음) | 허용 목록에 없는 경로는 기본 거부 — 404 가 아니라 401 로 수용한다 |

covers: 토큰 없이 또는 만료/위조된 토큰으로 GET /auth/me 호출 시 401을 반환한다
flow: F7

## SC-12 (error) 위조되거나 용도가 다른 토큰은 /me 에서 401 이다

- **Given** 가입된 사용자가 있고 유효한 `accessToken` 이 하나 있다
- **When** 아래 표의 토큰으로 `GET /api/auth/me` 를 호출한다
- **Then** 모든 행이 401 이고 `code` 는 `UNAUTHORIZED` 다 (500 이 아니다)
- **And** 사용자 정보가 응답에 없다

| 토큰 | 설명 |
|---|---|
| `not-a-jwt` / `a.b.c` | 형식이 깨진 토큰 |
| 유효 토큰의 payload 만 `role` 이나 `sub` 를 바꾸고 서명은 그대로 | 변조 |
| 다른 비밀키로 서명한 토큰 (claims 는 유효) | 서명 키 불일치 |
| 서명 알고리즘이 `none` 인 토큰 | 서명 없음 |
| 올바른 키로 서명했지만 `typ = "pending"` 인 토큰 | 용도가 다른 토큰은 일반 API 에 못 쓴다 |

covers: 토큰 없이 또는 만료/위조된 토큰으로 GET /auth/me 호출 시 401을 반환한다
flow: F7

## SC-13 (boundary) 토큰은 발급 24시간 뒤에 만료된다

- **Given** 시각 T 에 로그인해 `accessToken` 을 받았다
- **And** 현재 시각이 아래 표의 "현재 시각" 값이다 (시각은 테스트가 고정·이동할 수 있다)
- **When** `GET /api/auth/me` 를 호출한다
- **Then** 각 행의 상태가 "기대" 와 같고, 401 행의 `code` 는 `UNAUTHORIZED` 다

| 현재 시각 | 기대 |
|---|---|
| T + 24시간 − 1초 | 200 |
| T + 24시간 + 1초 | 401 |

정확히 T + 24시간인 순간은 단정하지 않는다 — 같은 순간을 유효로 보는지는 JWT 라이브러리의 기본 비교에 달려 있고 요구사항에 정의가 없다. 경계 양쪽 1초로 "24시간 뒤에 만료" 만 확인한다.

covers: 토큰 없이 또는 만료/위조된 토큰으로 GET /auth/me 호출 시 401을 반환한다
flow: F7

## SC-14 (error) 사용자가 사라진 뒤에는 유효한 토큰도 401 이다

- **Given** `user@example.com` 으로 가입·로그인해 유효한 `accessToken` 을 갖고 있다
- **And** 그 뒤 해당 사용자의 `users` 행이 삭제됐다
- **When** `GET /api/auth/me` 를 호출한다
- **Then** 401 이고 `code` 는 `UNAUTHORIZED` 다 (404·500 이 아니다)
- **And** 삭제된 사용자의 정보가 응답에 없다

covers: 토큰 없이 또는 만료/위조된 토큰으로 GET /auth/me 호출 시 401을 반환한다
flow: F7

## SC-15 (regression) Security 도입 뒤에도 공연 API 는 인증 없이 그대로 동작한다

- **Given** 공연이 하나 등록돼 있다 (id 는 P)
- **When** 아래 표의 각 요청을 `Authorization` 없이, CSRF 토큰 없이 보낸다
- **Then** 모든 행의 상태가 "기대" 와 같다 (인증을 요구하는 401·403 이 아니다)
- **And** 응답 본문은 기존과 같은 필드를 갖는다 — 목록은 `content`·`page`·`size`·`totalElements`, 단건·등록·수정은 `id`·`title`·`venue`·`status`, 취소 응답의 `status` 는 `CANCELLED` 다

| 요청 | 기대 |
|---|---|
| `GET /api/performances` | 200 |
| `GET /api/performances/{P}` | 200 |
| `POST /api/performances` (유효한 본문) | 201 |
| `PUT /api/performances/{P}` (유효한 본문) | 200 |
| `POST /api/performances/{P}/cancel` | 200 |

공연 쓰기는 `POST`·`PUT`·`POST …/cancel` 이 전부다 (`DELETE` 엔드포인트는 없다). 같은 요청에 인증 헤더나 출처가 붙는 경우도 영향이 없다.

| 요청 | 기대 |
|---|---|
| `GET /api/performances` + `Authorization: Bearer <만료된 토큰>` | 200 |
| `GET /api/performances` + `Authorization: Bearer <위조된 토큰>` | 200 |
| `OPTIONS /api/auth/login` (허용된 Origin, `Access-Control-Request-Method: POST`, `Access-Control-Request-Headers: authorization,content-type,x-write-key`) | 2xx 이고 `Access-Control-Allow-Origin` 이 그 Origin 이다 (401·403 이 아니다) |

covers: 기존 /api/performances/** 엔드포인트가 Security 도입 이후에도 인증 없이 그대로 동작한다(회귀 없음)
flow: F8

## SC-16 (boundary) JWT 비밀키는 UTF-8 32바이트 이상이 아니면 기동하지 않는다

- **Given** 애플리케이션을 `JWT_SECRET` 값을 바꿔 가며 기동한다
- **When** 아래 표의 값으로 기동을 시도한다
- **Then** 각 행의 결과가 "기대" 와 같다
- **And** 실패 행에서 기동이 던진 예외 또는 그 원인 체인의 메시지에 `JWT_SECRET` 이 들어 있다 (어느 설정이 문제인지 알 수 있다)

| JWT_SECRET | 기대 |
|---|---|
| 미설정 | 기동 실패 |
| `""` | 기동 실패 |
| 공백만 (`"                                "` 32칸) | 기동 실패 |
| ASCII 31자 (31바이트) | 기동 실패 |
| ASCII 32자 (32바이트) | 기동 성공 |
| 한글 11자 (33바이트) | 기동 성공 |

covers: 올바른 이메일/비밀번호로 POST /auth/login 호출 시 200과 유효한 accessToken을 반환한다; 토큰 없이 또는 만료/위조된 토큰으로 GET /auth/me 호출 시 401을 반환한다
flow: F10
