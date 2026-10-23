# TASK-011 시나리오

대상: `AuthService`(소셜 로그인 판정·가입 완료), `JwtTokenProvider`(pendingToken), `CustomOAuth2UserService`(제공자별 정규화), `OAuth2LoginSuccessHandler`·`OAuth2LoginFailureHandler`, `SecurityConfig`(OAuth2 로그인·접근 정책), `AuthController`(`POST /api/auth/social/complete`), `V4__create_social_accounts.sql`.

**테스트는 제공자 서버를 두지 않는다.** "제공자가 ~ 사용자 정보를 돌려줬다"는 제공자 userinfo 응답(attributes)을 직접 넣어 콜백 이후(정규화 → 판정 → 리다이렉트)를 실행하는 것으로 만든다. 제공자 인증 단계(인가 요청·토큰 교환)는 시나리오 SC-16 의 인가 요청 리다이렉트와 SC-08 의 실패 콜백만 본다.

## 규약 (정본)

시나리오가 이 값을 그대로 단언한다. 바꾸려면 여기부터 고친다.

| 항목 | 값 |
|---|---|
| 프론트 기준 주소 | 환경변수 `FRONTEND_BASE_URL` (테스트 값 `https://app.example.test`). 앞뒤 공백과 끝 슬래시는 제거해 쓴다. 제공자 설정이 하나라도 있는데 이 값이 비었거나 `http(s)://` 절대 주소가 아니면 서버는 기동하지 않는다(오류 메시지에 `FRONTEND_BASE_URL`). 요청 파라미터·헤더로는 절대 바뀌지 않는다 |
| 로그인 성공 리다이렉트 | 302 `{기준 주소}/oauth2/redirect?token=<accessToken>` |
| 가입 대기 리다이렉트 | 302 `{기준 주소}/oauth2/redirect?pendingToken=<pendingToken>` |
| 실패 리다이렉트 | 302 `{기준 주소}/login?error=social` (쿼리에 token·pendingToken 없음) |
| pendingToken | HS256, `typ=pending`, 클레임 `email` `name` `provider` `providerUserId`, 발급 시각 + 5분(그 정각부터 만료) |
| 오류 코드 | `PENDING_SIGNUP_EXPIRED`(401) · `EMAIL_ALREADY_EXISTS`(409) · `SOCIAL_ACCOUNT_ALREADY_LINKED`(409) · `INVALID_REQUEST`(400) · `INVALID_CREDENTIALS`(401) · `UNAUTHORIZED`(401) · `WRITE_KEY_INVALID`(401) |
| 제공자 설정 환경변수 | `KAKAO_CLIENT_ID` `KAKAO_CLIENT_SECRET` `NAVER_CLIENT_ID` `NAVER_CLIENT_SECRET` `GOOGLE_CLIENT_ID` `GOOGLE_CLIENT_SECRET` |
| 이메일 정규화 | 앞뒤 공백(U+0020)만 제거하고 소문자로 — `users.email` 과 같은 규칙. 제어문자·짝 없는 서로게이트는 변환하지 않고 거부한다 (ADR-0019) |
| 이메일 검증됨 | 제공자 플래그가 불리언 `true` 일 때만. 문자열 `"true"`·숫자·`null`·누락은 검증되지 않음 |

## SC-01 (happy) 처음 로그인하는 소셜 사용자는 가입 대기가 된다

- **Given** `users` 에 이메일 `new@example.com` 인 사용자가 없고 `social_accounts` 가 비어 있다
- **And** 구글이 `{"sub":"g-100","email":"new@example.com","email_verified":true,"name":"신규"}` 를 돌려줬다
- **When** 구글 콜백을 처리한다
- **Then** 302 `https://app.example.test/oauth2/redirect?pendingToken=<토큰>` 으로 이동하고 쿼리에 `token` 은 없다
- **And** `users`·`social_accounts` 행 수가 그대로다 (아무것도 만들지 않는다)
- **And** pendingToken 의 클레임은 `email=new@example.com` `name=신규` `provider=GOOGLE` `providerUserId=g-100` `typ=pending` 이고 만료는 발급 시각 + 5분이다

covers: 처음 로그인(기존 이메일과 매칭되지 않음)하면 users 행을 생성하지 않고 pendingToken을 발급해 역할 선택 화면으로 보낸다
flow: F1

## SC-02 (happy) 이메일이 일치하고 검증됐으면 기존 계정에 연결하고 바로 로그인한다

- **Given** 이메일로 가입한 사용자 A(`hong@example.com`, 역할 `ORGANIZER`, 이름 `홍길동`)가 있고 `social_accounts` 는 비어 있다
- **And** 제공자가 아래 표의 사용자 정보를 돌려줬다
- **When** 해당 제공자 콜백을 처리한다
- **Then** 302 `https://app.example.test/oauth2/redirect?token=<accessToken>` 으로 이동하고 쿼리에 `pendingToken` 은 없다
- **And** 그 accessToken 으로 `GET /api/auth/me` 를 호출하면 A 의 정보(역할 `ORGANIZER`)가 나온다
- **And** `users` 행 수는 그대로이고 A 의 이메일·이름·역할·비밀번호 해시는 바뀌지 않는다
- **And** `social_accounts` 에 (표의 provider, 표의 providerUserId, A) 행이 정확히 1건 생긴다

| 제공자 | 돌려준 사용자 정보 | provider | providerUserId |
|---|---|---|---|
| 카카오 | `{"id":777,"kakao_account":{"email":"hong@example.com","is_email_verified":true,"is_email_valid":true,"profile":{"nickname":"길동"}}}` | `KAKAO` | `777` |
| 구글 | `{"sub":"g-1","email":"hong@example.com","email_verified":true,"name":"길동"}` | `GOOGLE` | `g-1` |
| 구글 (이메일 대문자·앞뒤 공백) | `{"sub":"g-2","email":"  Hong@Example.COM  ","email_verified":true,"name":"길동"}` | `GOOGLE` | `g-2` |

covers: 이미 이메일 회원가입된 계정과 같은 이메일로, email_verified가 true인 소셜 로그인을 하면 users 행을 새로 만들지 않고 기존 계정에 social_accounts 행을 추가한 뒤 바로 accessToken을 발급한다(역할은 기존 값 유지)
flow: F2

## SC-03 (boundary) 이메일이 같아도 검증되지 않았으면 연결하지 않고 가입 대기가 된다

- **Given** 이메일로 가입한 사용자 A(`hong@example.com`)가 있고 `social_accounts` 는 비어 있다
- **And** 제공자가 아래 표의 사용자 정보를 돌려줬다 (이메일은 모두 A 와 같다)
- **When** 해당 제공자 콜백을 처리한다
- **Then** 302 `…/oauth2/redirect?pendingToken=<토큰>` 으로 이동하고 쿼리에 `token` 은 없다
- **And** `users`·`social_accounts` 행 수가 그대로이고 A 의 정보는 바뀌지 않는다
- **And** pendingToken 의 `email` 은 `hong@example.com` 이다

| 제공자 | 검증 플래그 상태 | 돌려준 사용자 정보 |
|---|---|---|
| 구글 | `email_verified: false` | `{"sub":"g-1","email":"hong@example.com","email_verified":false,"name":"길동"}` |
| 구글 | 필드 누락 | `{"sub":"g-1","email":"hong@example.com","name":"길동"}` |
| 구글 | 문자열 `"true"` | `{"sub":"g-1","email":"hong@example.com","email_verified":"true","name":"길동"}` |
| 구글 | 숫자 `1` | `{"sub":"g-1","email":"hong@example.com","email_verified":1,"name":"길동"}` |
| 구글 | `null` | `{"sub":"g-1","email":"hong@example.com","email_verified":null,"name":"길동"}` |
| 구글 | 문자열 `"TRUE"` | `{"sub":"g-1","email":"hong@example.com","email_verified":"TRUE","name":"길동"}` |
| 구글 | 배열 `[true]` / 객체 `{"v":true}` | `{"sub":"g-1","email":"hong@example.com","email_verified":[true],"name":"길동"}` |
| 카카오 | `is_email_verified: false` | `{"id":777,"kakao_account":{"email":"hong@example.com","is_email_verified":false,"profile":{"nickname":"길동"}}}` |
| 카카오 | 필드 누락 | `{"id":777,"kakao_account":{"email":"hong@example.com","profile":{"nickname":"길동"}}}` |
| 카카오 | `is_email_verified` 가 문자열 `"true"` / 숫자 `1` / `null` | `{"id":777,"kakao_account":{"email":"hong@example.com","is_email_verified":"true","is_email_valid":true,"profile":{"nickname":"길동"}}}` |
| 카카오 | `is_email_valid` 가 문자열 `"true"` / 숫자 `1` / `null` (`is_email_verified` 는 `true`) | `{"id":777,"kakao_account":{"email":"hong@example.com","is_email_verified":true,"is_email_valid":"true","profile":{"nickname":"길동"}}}` |
| 카카오 | `is_email_verified: true` 이지만 `is_email_valid: false` | `{"id":777,"kakao_account":{"email":"hong@example.com","is_email_verified":true,"is_email_valid":false,"profile":{"nickname":"길동"}}}` |
| 카카오 | `is_email_verified: true` 이지만 `is_email_valid` 누락 | `{"id":777,"kakao_account":{"email":"hong@example.com","is_email_verified":true,"profile":{"nickname":"길동"}}}` |
| 네이버 | 플래그를 제공하지 않는다 | `{"response":{"id":"n-1","email":"hong@example.com","name":"길동"}}` |

covers: email_verified가 false(또는 미제공)인 경우에는 이메일이 같아도 자동 연결하지 않고 신규 가입과 동일하게 pendingToken을 발급한다
flow: F3

## SC-04 (happy) 이미 연결된 제공자 계정은 이메일이 바뀌거나 미검증이어도 같은 사용자로 로그인한다

- **Given** 사용자 B(`b@example.com`)가 있고 `social_accounts` 에 (`GOOGLE`, `g-9`, B) 가 있다
- **And** 구글이 `{"sub":"g-9","email":"other@example.com","email_verified":false,"name":"바뀐이름"}` 를 돌려줬다 (이메일이 다르고 검증도 되지 않았다)
- **When** 구글 콜백을 처리한다
- **Then** 302 `…/oauth2/redirect?token=<accessToken>` 으로 이동하고 그 토큰의 사용자는 B 다
- **And** `social_accounts` 는 여전히 (`GOOGLE`, `g-9`, B) 1건뿐이고 `users` 행 수·B 의 이메일·이름은 그대로다

covers: 같은 제공자 계정으로 재로그인해도 social_accounts의 유니크 제약(provider, provider_user_id)에 의해 중복 행이 생기지 않는다
flow: F4

## SC-05 (boundary) 같은 제공자 계정의 콜백이 동시에 와도 연결 행은 하나다

- **Given** 이메일로 가입한 사용자 A(`hong@example.com`)가 있고 `social_accounts` 는 비어 있다
- **And** 구글이 `{"sub":"g-5","email":"hong@example.com","email_verified":true,"name":"길동"}` 를 돌려주는 콜백 두 건이 동시에 들어온다
- **When** 두 콜백을 같은 시점에 시작시켜 동시에 처리한다 (어느 쪽이 먼저 끝나도 아래 Then 이 같다)
- **Then** 두 응답 모두 `…/oauth2/redirect?token=<accessToken>` 로 이동하고 두 토큰의 사용자는 모두 A 다
- **And** `social_accounts` 에는 (`GOOGLE`, `g-5`, A) 가 정확히 1건이다

covers: 같은 제공자 계정으로 재로그인해도 social_accounts의 유니크 제약(provider, provider_user_id)에 의해 중복 행이 생기지 않는다
flow: F4

## SC-06 (boundary) 제공자마다 다른 응답 구조에서 같은 필드를 뽑는다

- **Given** `users` 에 해당 이메일이 없고 `social_accounts` 가 비어 있다
- **And** 제공자가 아래 표의 사용자 정보를 돌려줬다
- **When** 해당 제공자 콜백을 처리한다
- **Then** 가입 대기로 이동하고 pendingToken 의 `provider`·`providerUserId`·`email`·`name` 이 표의 값과 같다

| 제공자 | 돌려준 사용자 정보 | provider | providerUserId | email | name |
|---|---|---|---|---|---|
| 카카오 (중첩, 숫자 id) | `{"id":123456,"kakao_account":{"email":"k@example.com","is_email_verified":true,"profile":{"nickname":"카카오닉"}}}` | `KAKAO` | `123456` | `k@example.com` | `카카오닉` |
| 네이버 (`response` 래퍼) | `{"resultcode":"00","response":{"id":"n-abc","email":"n@example.com","name":"네이버이름"}}` | `NAVER` | `n-abc` | `n@example.com` | `네이버이름` |
| 구글 (평면) | `{"sub":"g-abc","email":"g@example.com","email_verified":true,"name":"구글이름"}` | `GOOGLE` | `g-abc` | `g@example.com` | `구글이름` |
| 구글 (이메일 대문자·앞뒤 공백) | `{"sub":"g-abd","email":"  G@Example.COM ","email_verified":true,"name":"구글이름"}` | `GOOGLE` | `g-abd` | `g@example.com` | `구글이름` |

covers: 카카오/네이버(중첩 응답 구조)와 구글(평면 구조) 각각에서 이메일과 이름을 정상적으로 추출한다(제공자별 매핑 단위 테스트)
flow: F5

## SC-07 (error) 필수 값이 없거나 이상한 제공자 응답은 아무것도 만들지 않고 실패로 돌려보낸다

- **Given** `users`·`social_accounts` 의 현재 행 수를 기록했다
- **And** 제공자가 아래 표의 사용자 정보를 돌려줬다 (표에 없는 필드는 올바른 값이다)
- **When** 해당 제공자 콜백을 처리한다
- **Then** 표의 "통과" 행이 아니면 302 `https://app.example.test/login?error=social` 으로 이동하고 쿼리에 `token`·`pendingToken` 이 없다
- **And** `users`·`social_accounts` 행 수가 그대로다
- **And** "통과" 행은 가입 대기(`…?pendingToken=`)로 이동한다 (길이는 UTF-16 코드 유닛 수로 센다)

| 필드 | 이상한 값 | 결과 |
|---|---|---|
| email | 누락 / `null` / 빈 문자열 / 공백만 | 실패 |
| email | 정규화 후 256자 | 실패 |
| email | `a\u0000b@example.com` (NUL) | 실패 |
| email | `\u0000a@example.com` (앞의 NUL — 앞뒤 공백 제거로 지워지지 않고 거부된다) | 실패 |
| email | `a\ud800b@example.com` (짝 없는 서로게이트) | 실패 |
| email | `a\tb@example.com` (탭) / `a\u0001b@example.com` (제어문자) | 실패 |
| email | 정규화(소문자) 후 256자 — 정규화 전에는 255자 이하 (`İ` 가 소문자화로 두 글자가 된다) | 실패 |
| email | 문자열이 아님: 숫자 `123` / 불리언 `true` / 객체 / 배열 | 실패 |
| email | 정규화 후 255자 | 통과 |
| name | 누락 / `null` / 빈 문자열 / 공백만 | 실패 |
| name | 101자 | 실패 |
| name | `홍\u0000길동` (NUL) | 실패 |
| name | `홍\ud800길동` (짝 없는 서로게이트) | 실패 |
| name | `홍\t길동` (탭) / `홍\u0001길동` (제어문자) | 실패 |
| name | 이모지 51개 (102 UTF-16 유닛) | 실패 |
| name | 문자열이 아님: 숫자 / 불리언 / 객체 / 배열 | 실패 |
| name | 100자 / 이모지 50개 (100 UTF-16 유닛) | 통과 |
| providerUserId | 누락 / `null` / 빈 문자열 / 공백만 | 실패 |
| providerUserId | 256자 | 실패 |
| providerUserId | `g\u0000x` (NUL) / `g\ud800x` (짝 없는 서로게이트) | 실패 |
| providerUserId | `g\tx` (탭) / `g\u0001x` (제어문자) | 실패 |
| providerUserId (구글 `sub`·네이버 `response.id`) | 문자열이 아님: 숫자 `123` / 불리언 / 객체 / 배열 — 문자열로 바꿔 쓰지 않는다 | 실패 |
| providerUserId (카카오 `id`) | 정수가 아님: 소수 `1.5` / 문자열 `"123"` / 불리언 `true` / 객체 / 배열 | 실패 |
| providerUserId (카카오 `id`) | 범위 밖: 음수 `-1` / `0` / Long 초과 `99999999999999999999` | 실패 |
| providerUserId (카카오 `id`) | 정수 `1` / Long 최댓값 `9223372036854775807` | 통과 |
| providerUserId | 255자 | 통과 |
| 구조 | 카카오 `kakao_account` 없음 / 네이버 `response` 래퍼 없음 / 카카오 `profile` 없음(이름 누락) | 실패 |
| 구조 | 중첩 값이 객체가 아님: 카카오 `kakao_account`·`profile` 이 문자열·배열 / 네이버 `response` 가 문자열·배열 | 실패 |

covers: 처음 로그인(기존 이메일과 매칭되지 않음)하면 users 행을 생성하지 않고 pendingToken을 발급해 역할 선택 화면으로 보낸다
flow: F9

## SC-08 (error) 제공자 인증이 실패하거나 취소되면 실패 화면으로 돌려보낸다

- **Given** `users`·`social_accounts` 의 현재 행 수를 기록했다
- **And** 제공자 인증 단계가 아래 표처럼 끝났다
- **When** 콜백 실패를 처리한다
- **Then** 302 `https://app.example.test/login?error=social` 으로 이동하고 쿼리에 `token`·`pendingToken` 이 없다
- **And** `users`·`social_accounts` 행 수가 그대로다

| 인증 결과 |
|---|
| 사용자가 제공자 화면에서 취소했다 |
| 제공자와의 토큰 교환이 실패했다 |
| 제공자의 사용자 정보 조회가 실패했다 |

covers: 처음 로그인(기존 이메일과 매칭되지 않음)하면 users 행을 생성하지 않고 pendingToken을 발급해 역할 선택 화면으로 보낸다
flow: F9

## SC-09 (happy) 역할을 고르면 가입이 끝나고 로그인된다

- **Given** `users` 에 `new@example.com` 이 없고, SC-01 이 발급한 pendingToken(구글, `new@example.com`, `신규`, `g-100`)이 유효하다
- **When** `POST /api/auth/social/complete` 를 `{"pendingToken":"<토큰>","role":<표의 role>}` 로 호출한다
- **Then** 200 이고 응답은 `tokenType=Bearer` · `expiresInSeconds=86400` · `user={id, email:"new@example.com", name:"신규", role:<표의 role>}` 이다
- **And** `users` 에 이메일 `new@example.com`·이름 `신규`·역할(표)·비밀번호 해시 없음 인 행이 1건, `social_accounts` 에 (`GOOGLE`, `g-100`, 그 사용자) 행이 1건 생긴다
- **And** 응답의 accessToken 으로 `GET /api/auth/me` 를 호출하면 같은 사용자가 나온다

| 보낸 role | 저장·응답의 role |
|---|---|
| `AUDIENCE` | `AUDIENCE` |
| `ORGANIZER` | `ORGANIZER` |

covers: 처음 로그인(기존 이메일과 매칭되지 않음)하면 users 행을 생성하지 않고 pendingToken을 발급해 역할 선택 화면으로 보낸다
flow: F6

## SC-10 (error) 만료되거나 위조된 pendingToken 은 401 이고 아무것도 만들지 않는다

- **Given** `users`·`social_accounts` 의 현재 행 수를 기록했다
- **And** 요청의 `pendingToken` 이 아래 표의 상태다 (role 은 올바른 `AUDIENCE`)
- **When** `POST /api/auth/social/complete` 를 호출한다
- **Then** "통과" 행이 아니면 401 `{"code":"PENDING_SIGNUP_EXPIRED", ...}` 이고 `users`·`social_accounts` 행 수가 그대로다
- **And** 표의 모든 실패 행에서 응답의 `code` 와 `message` 가 같다 (만료와 위조를 구분해 알리지 않는다)
- **And** "통과" 행은 200 으로 가입이 끝난다

| 토큰 상태 | 결과 |
|---|---|
| 만료 1초 전 (`exp - 1s` 시각에 호출) | 통과 |
| 만료 정각 (`exp` 와 같은 시각에 호출) | 실패 |
| 만료 후 (`exp + 1s`) | 실패 |
| 다른 키로 서명 | 실패 |
| 서명 부분 제거 / `alg=none` | 실패 |
| 페이로드를 바꿈 (이메일 변조, 서명 그대로) | 실패 |
| 형식이 깨짐 (점이 2개가 아님 / 임의 문자열) | 실패 |
| accessToken (`typ=access`) 을 보냄 | 실패 |
| `typ` 클레임 없음 | 실패 |
| 필수 클레임(`email` `name` `provider` `providerUserId`) 중 하나 누락 | 실패 |
| `provider` 가 알 수 없는 값(`FOO`) / 빈 문자열 | 실패 |

covers: 만료된 pendingToken으로 POST /api/auth/social/complete 호출 시 401을 반환한다
flow: F6, F7

## SC-11 (error) 가입 완료 요청의 형식이 틀리면 400 이다

- **Given** 유효한 pendingToken 이 있고 `users`·`social_accounts` 의 현재 행 수를 기록했다
- **When** `POST /api/auth/social/complete` 를 아래 표의 본문으로 호출한다
- **Then** 400 `{"code":"INVALID_REQUEST", ...}` 이다
- **And** `users`·`social_accounts` 행 수가 그대로다

| 본문 |
|---|
| `role` 누락 / `null` |
| `role` 이 정의 밖 (`ADMIN`) / 소문자 (`audience`) / 숫자 (`1`) |
| `pendingToken` 누락 / `null` / 빈 문자열 / 공백만 |
| `pendingToken` 이 문자열이 아님: 숫자 / 객체 / 배열 / 불리언 |
| 본문이 비어 있음 / JSON 이 아님 |

covers: 만료된 pendingToken으로 POST /api/auth/social/complete 호출 시 401을 반환한다
flow: F6

## SC-12 (error) 가입 완료 시점에 이미 있는 이메일·연결은 409 이고 아무것도 만들지 않는다

- **Given** 아래 표의 상태가 만들어져 있다 (그 사이 다른 경로로 사용자가 생겼다)
- **And** 요청한 pendingToken 은 유효하다
- **When** `POST /api/auth/social/complete` 를 호출한다
- **Then** 409 이고 응답의 `code` 는 표와 같다
- **And** `users`·`social_accounts` 행 수가 호출 전과 같다

| 상태 | 응답 `code` |
|---|---|
| pendingToken 의 이메일이 `users` 에 이미 있다 (이메일 가입자 — SC-03 에서 이어진 경우) | `EMAIL_ALREADY_EXISTS` |
| 같은 pendingToken 으로 이미 가입을 끝냈다 (두 번째 호출) | `EMAIL_ALREADY_EXISTS` |
| pendingToken 의 (provider, providerUserId) 가 이미 다른 이메일의 사용자에게 연결돼 있다 | `SOCIAL_ACCOUNT_ALREADY_LINKED` |

covers: 처음 로그인(기존 이메일과 매칭되지 않음)하면 users 행을 생성하지 않고 pendingToken을 발급해 역할 선택 화면으로 보낸다
flow: F6

## SC-13 (boundary) 같은 pendingToken 으로 가입 완료가 동시에 와도 사용자는 하나만 생긴다

- **Given** `users` 에 `new@example.com` 이 없고 유효한 pendingToken 이 있다
- **And** 같은 토큰으로 `POST /api/auth/social/complete` 두 건이 동시에 들어온다
- **When** 두 요청을 같은 시점에 시작시켜 동시에 처리한다 (어느 쪽이 먼저 끝나도 아래 Then 이 같다)
- **Then** 한 요청은 200 이고 다른 요청은 409 (`EMAIL_ALREADY_EXISTS` 또는 `SOCIAL_ACCOUNT_ALREADY_LINKED`)이며 500 은 없다
- **And** `users` 에 `new@example.com` 은 정확히 1건, `social_accounts` 에 (`GOOGLE`, `g-100`) 은 정확히 1건이다

covers: 같은 제공자 계정으로 재로그인해도 social_accounts의 유니크 제약(provider, provider_user_id)에 의해 중복 행이 생기지 않는다
flow: F6

## SC-14 (error) pendingToken 은 일반 API 의 인증 토큰으로 쓸 수 없다

- **Given** 유효한 pendingToken 이 있다
- **When** 아래 표의 요청에 그 토큰을 `Authorization: Bearer` 로 보낸다
- **Then** 인증이 필요한 요청은 401 `{"code":"UNAUTHORIZED", ...}` 이다
- **And** 인증이 필요 없는 요청은 토큰이 없을 때와 같은 결과다 (유효하지 않은 Bearer 가 있어도 거부하지 않는다 — 기존 동작)

| 요청 | 기대 |
|---|---|
| `GET /api/auth/me` (유효한 pendingToken) | 401 `UNAUTHORIZED` |
| `GET /api/auth/me` (만료된 pendingToken) | 401 `UNAUTHORIZED` |
| `GET /api/performances` (유효한 pendingToken) | 200 (토큰 없이 부르는 것과 같다) |

covers: pendingToken으로 /api/auth/social/complete 외의 인증 필요 엔드포인트를 호출하면 거부된다(typ 클레임 검증)
flow: F7

## SC-15 (error) 비밀번호가 없는 소셜 전용 계정으로 이메일 로그인하면 401 이다

- **Given** SC-09 로 가입한 소셜 전용 사용자(`new@example.com`, 비밀번호 해시 없음)가 있다
- **And** 비교용으로 이메일 가입 사용자의 틀린 비밀번호 로그인과 존재하지 않는 이메일 로그인의 응답을 받아 둔다
- **When** `POST /api/auth/login` 을 `new@example.com` 과 아래 표의 비밀번호로 호출한다
- **Then** 401 `{"code":"INVALID_CREDENTIALS", ...}` 이고 500 이 아니다
- **And** 응답의 `code`·`message` 는 비교용 두 응답과 같다 (계정 존재·가입 방식을 알리지 않는다)

| 비밀번호 |
|---|
| `password123` |
| UTF-8 72바이트를 넘는 문자열 (한글 25자) |

covers: 처음 로그인(기존 이메일과 매칭되지 않음)하면 users 행을 생성하지 않고 pendingToken을 발급해 역할 선택 화면으로 보낸다
flow: F8

## SC-16 (boundary) 제공자 설정이 있는 제공자만 인가 요청을 받는다

- **Given** 서버가 `FRONTEND_BASE_URL=https://app.example.test` 와 아래 표의 제공자 환경변수 값으로 기동했다 (표에 없는 제공자는 설정이 없다)
- **When** `GET /oauth2/authorization/<제공자>` 를 표의 요청 조작과 함께 호출한다 (조작이 없으면 그대로)
- **Then** 표의 결과가 된다
- **And** 서버는 어느 경우에도 기동에 성공하고 `POST /api/auth/signup` 은 201, 올바른 자격 증명의 `POST /api/auth/login` 은 200 이다

| `<P>_CLIENT_ID` / `<P>_CLIENT_SECRET` | 결과 |
|---|---|
| 둘 다 값이 있다 | 302 이고 `Location` 이 그 제공자의 인가 주소(카카오 `kauth.kakao.com`, 네이버 `nid.naver.com`, 구글 `accounts.google.com`)로 시작하며 `state` 파라미터가 있다 |
| 둘 다 미설정 | 404 (제공자로 302 하지 않는다) |
| 빈 문자열 | 404 |
| 공백만 | 404 |
| ID 만 있고 SECRET 이 없다(또는 반대) | 404 |
| 값 앞뒤에 공백이 있다 | 공백을 제거한 값으로 취급해 302 |
| (세 제공자 설정이 모두 있는 상태에서) 경로가 `/oauth2/authorization/foo` (알 수 없는 제공자) | 404 |
| (같은 상태에서) `/oauth2/authorization/apple` (이 태스크가 등록하지 않는 제공자) | 404 |
| (같은 상태에서) `/oauth2/authorization/GOOGLE` / `/oauth2/authorization/Google` (대소문자) | 404 |
| 둘 다 값이 있고 요청에 `?redirect_uri=https://evil.example/cb` 를 붙였다 | 302 이고 `Location` 의 `redirect_uri` 파라미터에 `evil.example` 이 없다 (제공자로 보내는 콜백 주소는 서버 설정으로만 정해진다) |
| 둘 다 값이 있고 요청 헤더에 `X-Forwarded-Host: evil.example` 를 붙였다 | 302 이고 `Location` 의 `redirect_uri` 에 `evil.example` 이 없다 |

covers: 처음 로그인(기존 이메일과 매칭되지 않음)하면 users 행을 생성하지 않고 pendingToken을 발급해 역할 선택 화면으로 보낸다
flow: F1

## SC-17 (regression) 기존 이메일 인증과 공연 API 는 그대로 동작한다

- **Given** 소셜 로그인이 도입된 서버이고 제공자 설정은 없다
- **And** 사용자 U(`user@example.com`)가 이메일로 가입돼 있고 로그인으로 받은 accessToken 이 있다
- **When** 아래 표의 요청을 보낸다
- **Then** 표의 결과가 된다

| 요청 | 결과 |
|---|---|
| `POST /api/auth/signup` (새 이메일) | 201 |
| `POST /api/auth/login` (U 의 올바른 자격 증명) | 200 과 accessToken |
| `GET /api/auth/me` (U 의 accessToken) | 200 과 U 의 정보 |
| `GET /api/performances` (인증 없음) | 200 (토큰 없이 부르는 기존 동작 그대로) |

covers: 이미 이메일 회원가입된 계정과 같은 이메일로, email_verified가 true인 소셜 로그인을 하면 users 행을 새로 만들지 않고 기존 계정에 social_accounts 행을 추가한 뒤 바로 accessToken을 발급한다(역할은 기존 값 유지)
flow: F2

## SC-18 (error) 쓰기 가드가 켜져 있으면 가입 완료도 키가 있어야 한다

- **Given** 서버가 `WRITE_GUARD_ENABLED=true` 와 `WRITE_API_KEY=server-key` 로 기동했다 (설정이 실제로 적용됐다)
- **And** 유효한 pendingToken 이 있고 `users`·`social_accounts` 의 현재 행 수를 기록했다
- **When** `POST /api/auth/social/complete` 를 아래 표의 `X-Write-Key` 로 호출한다
- **Then** 키가 틀리면 pendingToken 이 유효해도 401 `{"code":"WRITE_KEY_INVALID", ...}` 이고 `users`·`social_accounts` 행 수가 그대로다
- **And** 키가 맞으면 SC-09 와 같은 결과(200, 가입 완료)다

| `X-Write-Key` | 결과 |
|---|---|
| 헤더 없음 | 401 `WRITE_KEY_INVALID` |
| 빈 문자열 / 다른 값 | 401 `WRITE_KEY_INVALID` |
| `server-key` | 200 |

covers: 만료된 pendingToken으로 POST /api/auth/social/complete 호출 시 401을 반환한다
flow: F6

## SC-19 (boundary) 프론트 기준 주소 환경변수가 이상하면 기동하지 않거나 정리해서 쓴다

- **Given** 제공자 설정이 있다 (`KAKAO_CLIENT_ID`·`KAKAO_CLIENT_SECRET` 에 값이 있다)
- **And** `FRONTEND_BASE_URL` 이 아래 표의 값이다
- **When** 서버를 기동한다
- **Then** 표의 결과가 된다 (기동에 성공한 서버에 SC-01 의 콜백이 들어오면 돌려주는 `Location` 으로 확인한다)

| `FRONTEND_BASE_URL` | 결과 |
|---|---|
| `https://app.example.test` | 기동 성공, `Location` 이 `https://app.example.test/oauth2/redirect?pendingToken=` 로 시작한다 |
| `https://app.example.test/` (끝 슬래시 1개) / `https://app.example.test//` (2개) | 기동 성공, `Location` 이 `https://app.example.test/oauth2/redirect?pendingToken=` 로 시작하고 스킴 뒤에 `//` 가 없다 |
| `  https://app.example.test  ` (앞뒤 공백) | 기동 성공, 공백을 제거한 값과 같은 `Location` |
| `HTTPS://app.example.test` (스킴 대문자) | 기동 성공, 값을 바꾸지 않고 써서 `Location` 이 `HTTPS://app.example.test/oauth2/redirect?pendingToken=` 로 시작한다 |
| `https://app.example.test/app` (경로 포함) | 기동 성공, `Location` 이 `https://app.example.test/app/oauth2/redirect?pendingToken=` 로 시작한다 |
| `https://app.example.test?x=1` (쿼리) / `https://app.example.test#f` (프래그먼트) / `https://user@evil.example` (사용자 정보) | 기동 실패 — 오류 메시지에 `FRONTEND_BASE_URL` 이 있다 |
| `https://` (호스트 없음) / `httpsfoo://x` (접두사만 일치) | 기동 실패 — 오류 메시지에 `FRONTEND_BASE_URL` 이 있다 |
| 미설정 / 빈 문자열 / 공백만 | 기동 실패 — 오류 메시지에 `FRONTEND_BASE_URL` 이 있다 (`null/oauth2/redirect` 같은 주소로 열리지 않는다) |
| `app.example.test` (스킴 없음) / `javascript:alert(1)` / `ftp://app.example.test` | 기동 실패 — 오류 메시지에 `FRONTEND_BASE_URL` 이 있다 |

그리고 제공자 설정이 하나도 없으면 `FRONTEND_BASE_URL` 이 미설정이어도 서버는 기동에 성공하고 이메일 가입·로그인이 동작한다 (소셜 로그인이 꺼진 상태로 뜬다).

covers: 처음 로그인(기존 이메일과 매칭되지 않음)하면 users 행을 생성하지 않고 pendingToken을 발급해 역할 선택 화면으로 보낸다
flow: F1

## SC-20 (error) 콜백 요청을 조작해도 리다이렉트 주소는 설정값에서만 나온다

- **Given** `FRONTEND_BASE_URL=https://app.example.test` 이고 아래 "상황" 표의 하나가 만들어져 있다
- **When** 아래 "조작" 표의 조작이 붙은 콜백을 처리한다 (조작 × 상황 전부)
- **Then** `Location` 이 상황 표의 값으로 시작한다 — 조작에 쓴 `evil.example` 은 `Location` 어디에도 없다

| 상황 | `Location` 이 시작하는 값 |
|---|---|
| 신규 사용자 (SC-01) | `https://app.example.test/oauth2/redirect?pendingToken=` |
| 재로그인 (SC-04) | `https://app.example.test/oauth2/redirect?token=` |
| 제공자 인증 취소 (SC-08) | `https://app.example.test/login?error=social` |

| 조작 |
|---|
| 쿼리 `returnUrl=https://evil.example` / `redirect=//evil.example/x` / `next=https://evil.example` / `redirect_uri=https://evil.example` |
| 헤더 `Referer: https://evil.example/` / `Origin: https://evil.example` |
| 헤더 `Host: evil.example` / `X-Forwarded-Host: evil.example` / `X-Forwarded-Proto: http` |

covers: 처음 로그인(기존 이메일과 매칭되지 않음)하면 users 행을 생성하지 않고 pendingToken을 발급해 역할 선택 화면으로 보낸다
flow: F1, F4, F9

---

## 커버리지·판단 기록

- 기존 시나리오 파일 없음 — 새로 작성했다 (CP-2.2 `decisions` 에 같은 내용).
- Plan unresolved 를 이 문서에서 확정했다: 리다이렉트 규약·오류 코드·환경변수 이름(상단 규약 표), 이메일 누락·이름 누락·길이 초과는 자르거나 대체하지 않고 실패(SC-07), `/complete` 충돌은 409(SC-12), 제공자 설정이 없으면 404 로 미등록(SC-16), 네이버는 플래그가 없어 연결하지 않음(SC-03).
- 경계값(§2-1): 문자열 필드 전수(email·name·providerUserId 각각 NUL·서로게이트·제어문자·길이 ±1 — SC-07), 서버 응답/외부 입력의 code·본문 분기는 해당 없음(제공자 속성의 불리언 `true` 판정 — SC-03), 환경변수 미설정·빈 문자열·공백만·앞뒤 공백·한쪽만(SC-16), 시각 정각 경계(SC-10), 열거형 정의 밖·숫자·소문자(SC-11), 필수 필드 누락·null(SC-07·11), 헤더(Authorization) 부재가 아닌 용도 다른 토큰(SC-14).
- 묶은 것(§3): 제공자 3종 × 같은 판정은 표 행으로(SC-02·03·06), 필드별 이상 입력은 같은 정규화·거부 지점이라 SC-07 한 건의 표, pendingToken 무효 11행은 같은 검증 경로라 SC-10 한 건, 가입 완료 형식 위반은 SC-11, 충돌은 SC-12. 동시성(SC-05·13)은 단일 판정과 경로가 달라(유니크 위반 재조회·재시도) 따로 둔다.
- OOS-1(TASK-010): 서버 `trim()` 이 앞 NUL 을 지워 통과시키던 차이를 소셜 입력에서는 SC-07 의 "앞의 NUL" 행으로 거부로 못박았다. 이메일 가입(TASK-009) 서버 수정은 이 태스크 범위 밖이다.
- PostgreSQL: H2 는 V4 SQL·UNIQUE·FK 를 실행하지 않는다. Phase 4 의 실제 PostgreSQL HTTP 프로브가 필수 증거다(Plan 결정).
- 3차 검증(PASS, 경고 4) 반영: V5(SC-16 '정상 동작'을 상태 코드로)·V7(SC-08 의 Spring 오류 코드명 제거)·V8(Plan outputs 에 인가 요청 302 와 기동 실패)·V11 보강(카카오 검증 플래그의 비불리언 변형, `/complete` 의 pendingToken JSON 타입). 보류: V6 — SC-19 의 기동 + 콜백 확인, SC-16 의 부가 호출, SC-17 의 요청 4개 표는 같은 환경에서 결과만 달리 보는 표라 한 시나리오로 묶었다(Red 에서 행 단위 케이스로 나눈다).
- 1차 검증(V11) 반영: `FRONTEND_BASE_URL` 환경변수 경계(SC-19)와 오픈 리다이렉트 방지(SC-20, SC-16 의 `redirect_uri`·`X-Forwarded-Host` 행)를 추가했고, name·providerUserId 의 제어문자 행과 카카오 `is_email_valid` 조합을 보강했다. 제공자 설정이 있는데 기준 주소가 이상하면 기동하지 않는다 — 잘못된 `Location` 으로 열리는 것보다 낫고 `JWT_SECRET` 과 같은 관례다. V5(SC-18 내부 순서 문구 제거)·V6(SC-17 을 요청 표로)·V8(Plan outputs 에 404·`WRITE_KEY_INVALID`·`SOCIAL_ACCOUNT_ALREADY_LINKED` 반영)도 고쳤다.
- 2차 검증(V11) 반영: §2-1 표를 Plan 입력마다 끝까지 대 보고 채웠다 — 카카오 `id` 정수 행(음수·0·소수·범위 초과·타입 혼동·경계 통과), email·name·providerUserId·중첩 구조의 문자열 아님·객체 아님 행, 정규화(소문자) 뒤 길이가 늘어나는 이메일(`İ`), 검증 플래그의 `"TRUE"`·배열·객체, `FRONTEND_BASE_URL` 표기 변형(스킴 대소문자·경로·쿼리·프래그먼트·사용자 정보·호스트 없음·접두사만 일치), 인가 요청 경로 변수(`foo`·`apple`·대소문자). V6(SC-19 를 기동 한 번으로)·V7(`permitAll` 용어 제거)도 고쳤다.
- AC 밖 흐름(F8·F9)·설정·가드 시나리오(SC-07·08·12·15·16·18·19·20)의 `covers` 는 가장 가까운 AC 에 매단 것이며 커버리지로 세지 않는다 — AC 7개는 SC-01~06·10·14 가 덮는다.
- 시나리오가 8~12 기준선을 넘는다(20건). 제공자 3 × 판정 4 × 외부 입력 경계의 곱이 표 행으로 흡수됐으며 Red 케이스 수는 표 행 수가 정한다.
