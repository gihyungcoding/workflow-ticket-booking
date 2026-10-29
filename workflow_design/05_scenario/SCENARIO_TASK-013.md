# TASK-013 시나리오

대상: `frontend/src/components/SocialLoginButtons.tsx`, `pages/OAuthRedirectPage.tsx`, `pages/RoleSelectionPage.tsx`(신규), `pages/LoginPage.tsx`, `pages/SignUpPage.tsx`, `api/auth.ts`(`completeSocialSignUp`), `App.tsx`(라우트 `/oauth2/redirect`·`/oauth2/role`), `auth/messages.ts`.
테스트는 서버를 두지 않는다 — "서버가 ~로 응답한다"는 `fetch` 응답(상태·본문)으로 만든다. 이동은 라우터의 현재 경로(`MemoryRouter`)로 확인한다(jsdom 은 페이지 이동을 하지 않는다).
토큰은 `localStorage` 의 키 `accessToken` 에 저장한다 (ADR-0015). 콜백 토큰은 query 로 온다 — 사용자 결정(2026-10-29), ADR-0021 의 fragment 전환은 후속 태스크.

## 화면 문구 (정본)

시나리오가 이 문구를 그대로 단언한다. 바꾸려면 여기부터 고친다. **「제가 정한 값」** 은 AC·feature 문서에 없어 제안한 값이다 — HITL#1 에서 확인받는다.

| 위치 | 문구 |
|---|---|
| 소셜 버튼 | `카카오` `네이버` `구글` `애플` (이 순서) · 버튼 묶음 이름 `소셜 로그인` · 구분선 `또는` — 제가 정한 값(문구) |
| 로그인 실패 배너 | `로그인이 취소되었거나 실패했습니다` (feature §2-5) |
| 콜백 처리 중 | 스피너, 접근 이름 `로그인 처리 중` — 제가 정한 값 |
| 역할 선택 제목 | `거의 다 됐어요` (feature §2-4 와이어프레임) |
| 역할 선택 안내 | `<이름>님, <이메일> 으로 가입합니다` / 읽을 수 없으면 `소셜 계정으로 가입합니다` — 제가 정한 값 |
| 역할 선택 | 라디오 `관객` `주최자` · 버튼 `계속하기` · 미선택 오류 `역할을 선택해 주세요` (가입 화면과 같은 문구) |
| 만료 | `로그인 정보가 만료되었습니다. 다시 로그인해 주세요` (feature §2-5) + 버튼 `로그인 화면으로` — 버튼 이름은 제가 정한 값 |
| 가입 완료 일반 실패 | `가입을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요` — 제가 정한 값 |
| 이미 가입(409) | `이미 가입된 계정입니다. 로그인해 주세요` + 링크 `로그인`(→ `/login`) — 제가 정한 값 |
| 쓰기 거부 | `쓰기 권한 확인에 실패했습니다. 관리자에게 문의해 주세요` (기존 `WRITE_REJECTED_MESSAGE`) |

경로: 콜백 `/oauth2/redirect`, 역할 선택 `/oauth2/role` (제가 정한 값), 로그인 `/login`, 홈 `/`.

**제가 정한 값 (AC·feature 에 없는 것)**

1. 콜백이 정상이 아닌 모든 경우(쿼리 없음·둘 다·빈 값·사용자 조회 실패)는 한 곳 — `/login?error=social` 로 이동해 같은 배너를 보인다. 이동은 이력에 남지 않는다(뒤로 가기로 콜백에 돌아가지 않는다)
2. 콜백 쿼리는 `token` 과 `pendingToken` 중 **공백을 뺀 값이 비어 있지 않은 것이 정확히 하나**일 때만 정상이다. 빈 값·공백만인 쪽은 없는 것으로 본다(`?token=&pendingToken=x` 는 pendingToken 만 있는 것). 사용하는 값은 앞뒤 공백을 제거한 것이다(`?token=%20tok-1%20` 의 토큰은 `tok-1`)
3. `/complete` 의 만료 외 실패는 일반 문구 + 입력 유지·재시도 가능. 409 두 종류는 한 문구 + 로그인 링크
4. 역할 선택 화면은 이동할 때 함께 넘어온 pendingToken 이 없으면(직접 진입·소실) 안내 없이 `/login` 으로 이동한다
5. 콜백 화면을 새로고침하면 쿼리가 이미 지워져 있어 정상이 아닌 콜백(1번)으로 처리된다 — 허용
6. 로그인 배너(`?error=social`)는 표시한 뒤에도 주소에서 지우지 않는다 — 새로고침해도 같은 안내
7. 이미 로그인한 사용자가 새 token 콜백으로 오면 새 토큰으로 덮어쓴다. 소셜 로그인은 `from`(이전 화면)을 잃으므로 로그인 성공은 항상 홈이다(AC2). 이미 로그인한 사용자가 역할 선택에 와도(이동 때 넘어온 값이 있으면) 그대로 진행한다 — 제출이 성공하면 새 로그인으로 덮어쓴다

**단위 테스트로 지킬 수 없는 것 → Phase 4 증거**: 개발 프록시(`vite.config.ts` 가 `/oauth2/authorization` 만 백엔드로 보내고 `/oauth2/redirect` 는 보내지 않는다)는 jsdom 으로 검증할 수 없다. Phase 4 에서 앱을 실제로 띄워 두 경로의 응답을 확인하고 VERIFY 에 기록한다.

**이 태스크가 바꾸지 않는 상태(feature §2-5)**: 가입·로그인 화면의 기본·제출 중·검증 오류·중복·자격 증명 오류·성공은 TASK-010 의 기존 테스트(`AuthScreens.test.tsx`)가 지키며 이 태스크 이후에도 통과해야 한다. 소셜 버튼 추가가 이메일 폼 제출을 바꾸지 않는다는 것은 SC-14 가 명시적으로 지킨다.

## SC-01 (happy) 로그인·가입 화면에 소셜 버튼 네 개가 보이고 각각 인가 주소로 이동한다

- **Given** 로그인하지 않았고 `VITE_API_BASE_URL` 은 표의 값이다
- **When** 표의 화면을 연다
- **Then** `소셜 로그인` 묶음 안에 `카카오` `네이버` `구글` `애플` 링크가 이 순서로 있다
- **And** 각 링크의 이동 주소(`href`)는 `<기준 주소>/oauth2/authorization/<제공자>` 이다 (제공자 `kakao` `naver` `google` `apple`)
- **And** 이메일 폼과 소셜 버튼 사이에 `또는` 구분선이 있다
- **And** 이메일 폼의 입력란·제출 버튼은 그대로 있다

| 화면 | `VITE_API_BASE_URL` | 카카오 링크의 `href` |
|---|---|---|
| `/login` | 미설정 | `/oauth2/authorization/kakao` |
| `/signup` | 미설정 | `/oauth2/authorization/kakao` |
| `/login` | 빈 문자열 | `/oauth2/authorization/kakao` |
| `/login` | 공백만 | `/oauth2/authorization/kakao` |
| `/login` | `https://api.example.test` | `https://api.example.test/oauth2/authorization/kakao` |
| `/login` | `https://api.example.test/` (끝 슬래시) | `https://api.example.test/oauth2/authorization/kakao` |
| `/signup` | `https://api.example.test//` (연속 슬래시) | `https://api.example.test/oauth2/authorization/kakao` |

covers: 로그인/회원가입 화면에 카카오/네이버/구글/애플 버튼이 보이고 각각 /oauth2/authorization/{provider}로 이동한다
flow: F1

## SC-02 (happy) 콜백에 token 이 오면 사용자 정보를 얻어 로그인하고 홈으로 이동한다

- **Given** 시작 상태는 표와 같다 (`localStorage` 에 `accessToken` 이 없거나, 표의 옛 토큰이 이미 있고 헤더에 옛 사용자 이름이 보인다)
- **And** 서버가 `GET /api/auth/me` 에 `Authorization: Bearer <디코드된 토큰>` 이면 사용자 `{id:1,email:"hong@example.com",name:"홍길동",role:"AUDIENCE"}` 로 응답한다 (응답은 아직 보내지 않고 붙들어 둔다)
- **When** `/oauth2/redirect` 를 표의 쿼리로 연다
- **Then** 응답을 기다리는 동안 접근 이름 `로그인 처리 중` 인 스피너가 보이고 입력란·역할 라디오는 없다
- **And** `GET /api/auth/me` 가 `Authorization: Bearer <디코드된 토큰>` 으로 간다
- **And** 응답이 오면 `localStorage` 의 `accessToken` 이 `<디코드된 토큰>` 이다
- **And** 화면이 `/` (공연 목록)로 이동하고 헤더에 `홍길동` 과 `로그아웃` 이 보인다
- **And** 이동한 뒤 주소에 `token` 쿼리가 남아 있지 않고, 뒤로 가기로 토큰이 든 주소에 돌아가지 않는다

| 렌더 방식 | 시작 상태 | 쿼리 | 디코드된 토큰 |
|---|---|---|---|
| 일반 | 비로그인 | `?token=tok-1` | `tok-1` |
| 개발용 이중 실행(StrictMode) | 비로그인 | `?token=tok-1` | `tok-1` |
| 일반 | 비로그인 | `?token=ab%2Bcd%3D%2F` (퍼센트 인코딩) | `ab+cd=/` |
| 일반 | 비로그인 | `?token=tok-1&foo=bar` (모르는 파라미터 동반) | `tok-1` |
| 일반 | 비로그인 | `?token=%20tok-1%20` (앞뒤 공백) | `tok-1` |
| 일반 | 비로그인 | `?token=tok-1&pendingToken=` (pendingToken 이 빈 값) | `tok-1` |
| 일반 | 비로그인 | `?token=tok-1&pendingToken=%20` (pendingToken 이 공백만) | `tok-1` |
| 일반 | 이미 `old-tok` 로 로그인, 헤더에 `옛사용자` | `?token=tok-1` | `tok-1` (헤더가 `홍길동` 으로 바뀌고 저장 토큰도 `tok-1`) |

covers: 콜백 리다이렉트(/oauth2/redirect)에서 token 쿼리가 오면 즉시 로그인 처리 후 홈으로 이동한다
flow: F2

## SC-03 (error) 콜백이 정상이 아니면 로그인 화면으로 돌아가 오류 배너를 보인다

- **Given** 로그인하지 않았고 `localStorage` 에 토큰이 없다
- **And** 서버가 `GET /api/auth/me` 에 표의 응답을 한다 (표의 쿼리에 비어 있지 않은 `token` 이 있을 때만 해당)
- **When** `/oauth2/redirect` 를 표의 쿼리로 연다
- **Then** 화면이 `/login` 으로 이동하고 주소의 쿼리가 `?error=social` 이다
- **And** `로그인이 취소되었거나 실패했습니다` 배너가 보이고 헤더는 비로그인(`로그인`·`가입` 링크) 이다
- **And** `localStorage` 에 `accessToken` 이 저장되지 않는다
- **And** 표의 응답이 `—` 인 행은 서버로 어떤 요청도 가지 않는다
- **And** 뒤로 가기로 콜백 주소에 돌아가지 않는다

| 쿼리 | `GET /api/auth/me` 응답 |
|---|---|
| (쿼리 없음) | — |
| `?token=` (빈 값) | — |
| `?token=%20` (공백만) | — |
| `?pendingToken=` (빈 값) | — |
| `?pendingToken=%20` (공백만) | — |
| `?token=&pendingToken=` (둘 다 빈 값) | — |
| `?token=%20&pendingToken=%20` (둘 다 공백만) | — |
| `?token=tok-1&pendingToken=pt-1` (둘 다 값이 있음) | — |
| `?foo=bar` (모르는 파라미터만) | — |
| `?token=tok-1` | 401 `{"code":"UNAUTHORIZED"}` |
| `?token=tok-1` | 500 비 JSON |
| `?token=tok-1` | 네트워크 오류(fetch 거부) |
| `?token=tok-1` | 200 이지만 빈 바디 |
| `?token=tok-1` | 200 이지만 비 JSON |
| `?token=tok-1` | 200 이지만 `{"email":"a@b.c","name":""}` (이름이 빈 값) |

covers: 제공자 인증을 사용자가 취소하거나 실패하면 로그인 화면으로 돌아와 오류 메시지를 보여준다
flow: F5

## SC-04 (happy) 콜백에 pendingToken 이 오면 역할 선택 화면으로 이동한다

- **Given** 로그인하지 않았다
- **And** pendingToken 의 페이로드가 `{"email":"hong@gmail.com","name":"홍길동","provider":"KAKAO","providerUserId":"k-1","emailVerified":true,"typ":"pending"}` 이다
- **When** `/oauth2/redirect` 를 표의 방식·쿼리로 연다 (`<t>` 는 그 pendingToken)
- **Then** 화면이 `/oauth2/role` 로 이동하고 제목 `거의 다 됐어요` 와 안내 `홍길동님, hong@gmail.com 으로 가입합니다` 가 보인다
- **And** 역할 라디오 `관객` `주최자` 가 있고 둘 다 선택되어 있지 않으며 버튼 `계속하기` 가 있다
- **And** 주소 어디에도 pendingToken 이 남아 있지 않다 (경로·쿼리·해시)
- **And** 서버로 어떤 요청도 가지 않고 `localStorage` 에 `accessToken` 이 없다
- **And** 뒤로 가기로 콜백 주소에 돌아가지 않는다

| 렌더 방식 | 쿼리 |
|---|---|
| 일반 | `?pendingToken=<t>` |
| 개발용 이중 실행(StrictMode) — `/login?error=social` 로 가지 않는다 | `?pendingToken=<t>` |
| 일반 | `?token=&pendingToken=<t>` (token 이 빈 값) |
| 일반 | `?token=%20&pendingToken=<t>` (token 이 공백만) |

covers: 콜백 리다이렉트에서 pendingToken 쿼리가 오면 역할 선택 화면으로 이동하고, 역할 제출 후 로그인 처리된다
flow: F3

## SC-05 (boundary) pendingToken 페이로드가 달라도 역할 선택은 동작하고 안내 문구만 바뀐다

- **Given** 로그인하지 않았고 `/oauth2/redirect?pendingToken=<표의 토큰>` 로 역할 선택 화면에 도착했다
- **When** 역할 선택 화면이 열린다
- **Then** 안내 문구가 표의 값이다
- **And** `관객` `주최자` 라디오와 `계속하기` 버튼이 있다
- **And** 이름·이메일 값은 글자 그대로 보이고 HTML 로 해석되지 않는다

| 토큰의 페이로드(base64url) | 안내 문구 |
|---|---|
| `{"email":"hong@gmail.com","name":"홍길동"}` | `홍길동님, hong@gmail.com 으로 가입합니다` |
| 이름이 4바이트 문자 `😀길동` | `😀길동님, hong@gmail.com 으로 가입합니다` |
| 이름 `ÿÿÿ`, 이메일 `a+b@example.com` — base64url 문자열에 `-` 또는 `_` 가 실제로 들어 있다(테스트가 먼저 단언한다) | `ÿÿÿ님, a+b@example.com 으로 가입합니다` |
| 이름 `<b>길동</b>` | `<b>길동</b>님, hong@gmail.com 으로 가입합니다` (글자 그대로, `<b>` 요소 없음) |
| `email` 누락 | `소셜 계정으로 가입합니다` |
| `name` 누락 | `소셜 계정으로 가입합니다` |
| `name` 이 빈 문자열 | `소셜 계정으로 가입합니다` |
| `name` 이 숫자 `123` | `소셜 계정으로 가입합니다` |
| `email` 이 숫자 `123` | `소셜 계정으로 가입합니다` |
| 페이로드가 JSON 이 아님 | `소셜 계정으로 가입합니다` |
| 페이로드가 JSON 배열 | `소셜 계정으로 가입합니다` |
| base64 가 깨짐(`!!!`) | `소셜 계정으로 가입합니다` |
| 점(`.`)이 없는 임의 문자열 `pt-opaque` | `소셜 계정으로 가입합니다` |

covers: 콜백 리다이렉트에서 pendingToken 쿼리가 오면 역할 선택 화면으로 이동하고, 역할 제출 후 로그인 처리된다
flow: F3

## SC-06 (happy) 역할을 골라 제출하면 가입을 완료하고 로그인해 홈으로 이동한다

- **Given** 역할 선택 화면에 pendingToken `<원문>` 으로 도착했고 로그인하지 않았다
- **And** 표의 역할을 골랐다
- **And** 서버가 `POST /api/auth/social/complete` 에 `{accessToken:"tok-2",tokenType:"Bearer",expiresInSeconds:86400,user:{id:2,email:"hong@gmail.com",name:"홍길동",role:<표의 코드>}}` 로 응답한다 (응답은 붙들어 둔다)
- **When** `계속하기` 를 두 번 연달아 누른다
- **Then** 응답을 기다리는 동안 `계속하기` 가 비활성이고 요청은 한 번만 간다
- **And** 요청 본문이 `{"pendingToken":"<원문>","role":"<표의 코드>"}` 이다
- **And** 응답이 오면 `localStorage` 의 `accessToken` 이 `tok-2` 이다
- **And** 화면이 `/` 로 이동하고 헤더에 `홍길동` 과 `로그아웃` 이 보인다

| 선택한 역할 | 요청 본문의 `role` |
|---|---|
| `관객` | `AUDIENCE` |
| `주최자` | `ORGANIZER` |

covers: 콜백 리다이렉트에서 pendingToken 쿼리가 오면 역할 선택 화면으로 이동하고, 역할 제출 후 로그인 처리된다
flow: F3

## SC-07 (error) 역할을 고르지 않고 제출하면 인라인 오류를 보이고 요청하지 않는다

- **Given** 역할 선택 화면에 도착했고 역할을 고르지 않았다
- **When** `계속하기` 를 누른다
- **Then** `역할 선택` 아래에 `역할을 선택해 주세요` 가 보인다
- **And** 서버로 어떤 요청도 가지 않고 `/oauth2/role` 에 머문다
- **And** 이어서 역할 라디오 하나를 고르면 그 오류가 사라진다

covers: 콜백 리다이렉트에서 pendingToken 쿼리가 오면 역할 선택 화면으로 이동하고, 역할 제출 후 로그인 처리된다
flow: F3

## SC-08 (error) pendingToken 이 만료되면 안내와 로그인 화면으로 가는 버튼을 보인다

- **Given** 역할 선택 화면에서 역할 `관객` 을 골랐다
- **And** 서버가 `POST /api/auth/social/complete` 에 401 `{"code":"PENDING_SIGNUP_EXPIRED","message":"..."}` 로 응답한다
- **When** `계속하기` 를 누른다
- **Then** `로그인 정보가 만료되었습니다. 다시 로그인해 주세요` 와 버튼 `로그인 화면으로` 가 보인다
- **And** 더 제출할 수 없다 — `계속하기` 와 역할 라디오는 보이지 않는다
- **And** `localStorage` 에 `accessToken` 이 없다
- **And** `로그인 화면으로` 를 누르면 `/login` 으로 이동하고 `로그인이 취소되었거나 실패했습니다` 배너는 없다

covers: pendingToken 만료 오류 응답을 받으면 안내 메시지와 함께 로그인 화면으로 이동할 수 있다
flow: F4

## SC-09 (error) 만료가 아닌 가입 완료 실패는 문구를 보이고 입력을 유지한 채 다시 시도할 수 있다

- **Given** 역할 선택 화면에서 역할 `주최자` 를 골랐다
- **And** 서버가 `POST /api/auth/social/complete` 에 표의 응답을 한다
- **When** `계속하기` 를 누른다
- **Then** 표의 문구가 보이고 만료 안내(`로그인 정보가 만료되었습니다…`)는 보이지 않는다
- **And** `주최자` 가 선택된 채 `계속하기` 가 다시 활성이다
- **And** `localStorage` 에 `accessToken` 이 없고 `/oauth2/role` 에 머문다
- **And** 표의 로그인 링크 열이 `있음` 인 행은 `/login` 으로 가는 링크 `로그인` 이 문구 안에 있고, `없음` 인 행은 그 링크가 없다

| 서버 응답 | 보이는 문구 | 로그인 링크 |
|---|---|---|
| 400 `{"code":"VALIDATION_ERROR"}` | `가입을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요` | 없음 |
| 401 `{"code":"UNAUTHORIZED"}` (만료 code 가 아님) | 위와 같음 | 없음 |
| 401 빈 바디 | 위와 같음 | 없음 |
| 500 비 JSON | 위와 같음 | 없음 |
| 네트워크 오류(fetch 거부) | 위와 같음 | 없음 |
| 200 이지만 `accessToken` 이 없음 | 위와 같음 | 없음 |
| 200 이지만 `user.name` 이 빈 문자열 | 위와 같음 | 없음 |
| 쓰기 가드가 막음(`{"code":"WRITE_KEY_INVALID"}`) | `쓰기 권한 확인에 실패했습니다. 관리자에게 문의해 주세요` | 없음 |
| 409 `{"code":"EMAIL_ALREADY_EXISTS"}` | `이미 가입된 계정입니다. 로그인해 주세요` | 있음 |
| 409 `{"code":"SOCIAL_ACCOUNT_ALREADY_LINKED"}` | 위와 같음 | 있음 |

covers: 콜백 리다이렉트에서 pendingToken 쿼리가 오면 역할 선택 화면으로 이동하고, 역할 제출 후 로그인 처리된다
flow: F3

## SC-10 (error) 가입 완료가 실패한 뒤 다시 제출하면 이전 문구가 사라지고 성공하면 로그인한다

- **Given** 역할 선택 화면에서 역할 `관객` 을 골랐고, 첫 제출이 표의 실패 응답으로 끝나 그 실패 문구가 보인다
- **And** 서버가 이제 `POST /api/auth/social/complete` 에 200 로그인 응답(`accessToken:"tok-3"`, 사용자 `홍길동`)으로 응답한다 (응답은 붙들어 둔다)
- **When** `계속하기` 를 다시 누른다
- **Then** 응답을 기다리는 동안 이전 실패 문구가 보이지 않는다
- **And** 응답이 오면 `localStorage` 의 `accessToken` 이 `tok-3` 이고 화면이 `/` 로 이동한다

| 첫 제출의 실패 응답 | 사라져야 하는 문구 |
|---|---|
| 500 비 JSON | `가입을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요` |
| 409 `{"code":"EMAIL_ALREADY_EXISTS"}` | `이미 가입된 계정입니다. 로그인해 주세요` |
| 쓰기 가드가 막음(`{"code":"WRITE_KEY_INVALID"}`) | `쓰기 권한 확인에 실패했습니다. 관리자에게 문의해 주세요` |

covers: 콜백 리다이렉트에서 pendingToken 쿼리가 오면 역할 선택 화면으로 이동하고, 역할 제출 후 로그인 처리된다
flow: F3

## SC-11 (error) 로그인 화면은 error=social 일 때만 실패 배너를 보인다

- **Given** 로그인하지 않았다
- **When** `/login` 을 표의 쿼리로 연다
- **Then** 배너 `로그인이 취소되었거나 실패했습니다` 의 표시 여부가 표와 같다
- **And** 이메일·비밀번호 입력란, `로그인` 버튼, 네 소셜 링크가 있고 주소의 쿼리가 열 때 그대로다 (지워지지 않는다)

| 쿼리 | 배너 |
|---|---|
| `?error=social` | 보인다 |
| `?error=other` | 없다 |
| `?error=` | 없다 |
| `?error=SOCIAL` (대소문자) | 없다 |
| (쿼리 없음) | 없다 |

covers: 제공자 인증을 사용자가 취소하거나 실패하면 로그인 화면으로 돌아와 오류 메시지를 보여준다
flow: F5

## SC-12 (boundary) 역할 선택 화면은 이동할 때 넘어온 pendingToken 이 없으면 열리지 않는다

- **Given** 로그인하지 않았다
- **When** `/oauth2/role` 을 표의 넘어온 값으로 직접 연다
- **Then** 화면이 `/login` 으로 이동하고 `로그인이 취소되었거나 실패했습니다` 배너는 없다
- **And** 서버로 어떤 요청도 가지 않는다

| 이동 때 넘어온 값(pendingToken) |
|---|
| 없음 |
| 빈 문자열 `""` |
| 공백만 `"  "` |
| 문자열이 아님(숫자 `123`) |

covers: 콜백 리다이렉트에서 pendingToken 쿼리가 오면 역할 선택 화면으로 이동하고, 역할 제출 후 로그인 처리된다
flow: F3

## SC-13 (boundary) 응답이 오기 전에 상태가 이미 바뀌었으면 늦은 응답은 그 상태를 덮어쓰지 않는다

- **Given** 표의 상황이 진행 중이고 그 요청의 응답은 붙들어 두었다
- **And** 표의 변화가 이미 일어났다
- **When** 붙들어 둔 응답을 표의 늦은 응답으로 보낸다
- **Then** 표의 기대 열이 `복원 경쟁` 이면 헤더가 새 사용자 `홍길동` 이고 `localStorage` 의 `accessToken` 이 `new-tok` 이다
- **And** 기대 열이 `이동·언마운트` 이면 화면이 변화로 도착한 화면(`/signup`, 뒤로 가기 행은 콜백 이전 화면 `/`)에 머물고 헤더가 비로그인이며 `localStorage` 에 `accessToken` 이 없다
- **And** 기대 열이 `이동·언마운트` 이면 도착한 화면에 `로그인이 취소되었거나 실패했습니다` 배너나 `가입을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요` 문구가 없다

| 진행 중인 상황 | 이미 일어난 변화 | 늦은 응답(행마다 한 케이스) | 기대 |
|---|---|---|---|
| 저장된 토큰 `old-tok` 로 시작 때 보낸 `GET /api/auth/me` 복원이 진행 중 | `/oauth2/redirect?token=new-tok` 의 로그인이 끝났다 | 복원 응답 200(옛 사용자 `옛사용자`) | 복원 경쟁 |
| 위와 같다 | 위와 같다 | 복원 응답 401 | 복원 경쟁 (새 토큰을 지우지 않는다) |
| 위와 같다 | 위와 같다 | 복원 응답 500 / 네트워크 오류 / 200 비 JSON | 복원 경쟁 |
| `/oauth2/redirect?token=tok-1` 의 `GET /api/auth/me` 가 진행 중 | 사용자가 헤더의 `가입` 링크로 `/signup` 에 갔다 | 200 사용자 / 401 / 500 / 네트워크 오류 / 200 비 JSON | 이동·언마운트 |
| 위와 같다 | 사용자가 브라우저 뒤로 가기로 콜백 이전 화면에 갔다 | 200 사용자 / 401 | 이동·언마운트 (콜백 이전 화면 `/` 에 머문다) |
| 역할 선택에서 `POST /api/auth/social/complete` 가 진행 중 | 사용자가 헤더의 `가입` 링크로 `/signup` 에 갔다 | 200 로그인 응답 / 401 만료 code / 409 / 500 / 네트워크 오류 / 200 비 JSON | 이동·언마운트 |

covers: 콜백 리다이렉트(/oauth2/redirect)에서 token 쿼리가 오면 즉시 로그인 처리 후 홈으로 이동한다; 콜백 리다이렉트에서 pendingToken 쿼리가 오면 역할 선택 화면으로 이동하고, 역할 제출 후 로그인 처리된다
flow: F2, F3

## SC-14 (regression) 소셜 버튼이 있어도 이메일 로그인·가입은 그대로 동작한다

- **Given** 표의 화면에서 올바른 값을 모두 입력했고, 서버가 표의 요청에 성공으로 응답한다
- **When** 표의 제출 버튼을 누른다
- **Then** 표의 요청이 한 번만 가고 `/api/auth/` 의 다른 경로나 `/oauth2/authorization/` 로는 어떤 요청도 가지 않는다
- **And** 표의 이동 결과가 된다

| 화면 | 제출 버튼 | 요청 | 이동 결과 |
|---|---|---|---|
| `/login` | `로그인` | `POST /api/auth/login` | `/` 로 이동, `localStorage` 의 `accessToken` 저장, 헤더에 사용자 이름 |
| `/signup` | `가입하기` | `POST /api/auth/signup` | `/login` 으로 이동, `accessToken` 은 저장되지 않음 |

covers: 로그인/회원가입 화면에 카카오/네이버/구글/애플 버튼이 보이고 각각 /oauth2/authorization/{provider}로 이동한다
flow: F1
