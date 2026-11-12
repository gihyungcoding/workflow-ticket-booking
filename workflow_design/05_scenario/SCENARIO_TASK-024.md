# TASK-024 시나리오

이 시나리오가 다루는 범위:
- `frontend/src/api/http.ts` 의 `request()`
- `frontend/src/api/performances.ts` 의 쓰기 3함수와 `UnauthorizedError`·`ForbiddenRoleError`
- 새로 만드는 `frontend/src/auth/RequireOrganizer.tsx`
- `App.tsx` 의 라우트
- 등록·수정 화면
- 인증 화면 세 곳(로그인·가입·소셜 역할 선택)의 쓰기 키 분기 제거

**화면은 실제 라우트를 지난다.** 시나리오의 "주소를 연다"는 `App` + `MemoryRouter` + `AuthProvider` 로 그 주소를 그린다는 뜻이다. 기존 `AuthScreens.test.tsx` 와 같은 방식이다. 서버는 `fetch` 스텁이고, 응답마다 새 `Response` 를 만든다. 실린 헤더는 스텁이 기록한다.

## 규약 (정본)

시나리오는 이 값을 그대로 단언한다. 값을 바꾸려면 이 표부터 고친다.

| 항목 | 값 |
|---|---|
| 주최자 `ORG` | `{ id: 1, email: 'org@ticket.on', name: '주최자', role: 'ORGANIZER', handle: 'org01' }`. 토큰 `tok-org` |
| 관객 `AUD` | `{ id: 2, email: 'aud@ticket.on', name: '관객', role: 'AUDIENCE', handle: 'aud01' }`. 토큰 `tok-aud` |
| 로그인 상태로 시작 | `localStorage.accessToken` 에 토큰을 두고, `GET /api/auth/me` 가 그 사용자를 200 으로 준다 |
| 비로그인으로 시작 | `localStorage.accessToken` 이 없다 |
| 보호 경로 | `/performances/new`(등록), `/performances/5/edit`(수정). 수정 화면의 공연 `P5` 는 `GET /api/performances/5` 가 200 으로 준다(id 5, title `가을 재즈 콘서트`, 오픈 전) |
| 쓰기 동작 3개 | 등록 제출(`POST /api/performances`), 수정 저장(`PUT /api/performances/5`), 취소 확인(`POST /api/performances/5/cancel`) |
| 주최자 전용 안내 | 제목 「주최자만 이용할 수 있어요」, 부제 「공연 등록과 관리는 주최자 계정에서 할 수 있어요」, 버튼 「공연 둘러보기」(누르면 `/`). 폼(라벨 「공연명」)은 없다 |
| 폼이 보인다 | 라벨 「공연명」 입력란이 있다 |
| 아무것도 그리지 않는다 | 가드 자리에 폼도, 안내도 없고, 주소가 바뀌지 않는다 |
| 옛 쓰기 키 문구 | 「쓰기 권한 확인에 실패했습니다. 관리자에게 문의해 주세요」. 어떤 화면에도 나오지 않는다 |
| 요청 토큰의 정본 | 화면이 보여주는 로그인 상태(`AuthProvider` 의 세션)다. 로그인하면 그 토큰을, 로그아웃하면 토큰 없음을 요청에 쓴다 — 저장소(`localStorage`)에 쓰거나 지우는 데 실패해도, 다른 탭이 저장소를 바꿔도 같다. 세션이 아직 정해지지 않았으면(앱이 뜨기 전·`AuthProvider` 밖) 저장소 값을 쓴다(SC-01~03) |

## SC-01 (happy) 저장된 토큰이 있으면 요청에 Bearer 가 실리고, 쓰기 키는 어디에도 없다

- **Given** `localStorage.accessToken` 이 `tok-org` 다
- **And** 빌드 환경변수 `VITE_WRITE_KEY` 를 `k-123` 으로 둔다(옛 코드라면 키를 실었을 조건)
- **When** `request('/api/x', { method })` 를 아래 표의 메서드로 차례로 보낸다
- **Then** 모든 요청의 `Authorization` 헤더가 `Bearer tok-org` 다
- **And** 어떤 요청에도 `X-Write-Key` 헤더가 없다
- **And** 요청 주소는 기존 `apiUrl` 규칙 그대로다(`VITE_API_BASE_URL` 미설정이면 `/api/x`)

| 행 | method |
|---|---|
| a | `GET`(기본값, method 생략) |
| b | `POST` |
| c | `PUT` |
| d | `PATCH` |
| e | `DELETE` |
| f | `post`(소문자) |

covers: 로그인 상태에서 공연 쓰기 요청에 `Authorization: Bearer <토큰>` 이 실리고, `X-Write-Key` 는 어떤 요청에도 없다
flow: F1

## SC-02 (boundary) 토큰이 없거나 헤더에 실을 수 없으면 Authorization 없이 요청은 나간다

- **Given** `localStorage.accessToken` 을 아래 표처럼 둔다
- **When** `request('/api/performances', { method: 'POST' })` 를 보낸다
- **Then** 요청이 정확히 1번 나간다
- **And** `Authorization` 헤더는 표의 값이다("없음"은 헤더 항목 자체가 없다는 뜻이다)
- **And** `X-Write-Key` 헤더는 없다

| 행 | 저장된 값 | Authorization |
|---|---|---|
| a | 키 없음 | 없음 |
| b | `''` | 없음 |
| c | `'   '` | 없음 |
| d | `localStorage.getItem` 이 예외를 던진다 | 없음 |
| e | `' tok-1 '` | `Bearer tok-1` (앞뒤 공백을 걷는다) |
| f | `'tok-1\n'` | `Bearer tok-1` |
| g | `'tok\u00001'`(NUL 포함) | 없음 (헤더에 실을 수 없는 값) |
| h | `'토큰'`(헤더 바이트 범위 밖) | 없음 |
| i | `'tök'`(Latin-1 안) | `Bearer tök` (그대로 싣는다. 판정은 서버가 한다) |
| j | `'Bearer tok-1'`(접두사까지 저장된 값) | `Bearer Bearer tok-1` (해석하지 않고 그대로 싣는다) |

covers: 로그인 상태에서 공연 쓰기 요청에 `Authorization: Bearer <토큰>` 이 실리고, `X-Write-Key` 는 어떤 요청에도 없다
flow: F1

## SC-03 (boundary) 호출부가 Authorization 을 실었으면 저장된 토큰으로 덮어쓰지 않는다

- **Given** 저장된 토큰과 호출부 `init.headers.Authorization` 을 아래 표처럼 둔다
- **When** `request('/api/auth/me', { headers })` 를 보낸다
- **Then** 실린 `Authorization` 은 표의 값이다

| 행 | 저장된 토큰 | 호출부 Authorization | 실린 값 |
|---|---|---|---|
| a | `tok-A` | `Bearer tok-B` | `Bearer tok-B` |
| b | 없음 | `Bearer tok-B` | `Bearer tok-B` |
| c | `tok-A` | 없음 | `Bearer tok-A` |
| d | 없음 | 없음 | 없음 |

covers: 로그인 상태에서 공연 쓰기 요청에 `Authorization: Bearer <토큰>` 이 실리고, `X-Write-Key` 는 어떤 요청에도 없다
flow: F1

## SC-04 (happy) 주최자는 등록 화면에서 공연을 등록하고, 요청에 자기 토큰이 실린다

- **Given** `ORG` 로그인 상태로 시작한다
- **And** `POST /api/performances` 가 201 `{ id: 42, … }` 를 준다
- **When** `/performances/new` 를 열고, 유효한 값을 채워 제출한다
- **Then** 폼이 보인다(제출 전)
- **And** `POST /api/performances` 요청의 `Authorization` 이 `Bearer tok-org` 이고 `X-Write-Key` 는 없다
- **And** 주소가 `/performances/42` 가 된다(기존 이동 그대로)

covers: 로그인 상태에서 공연 쓰기 요청에 `Authorization: Bearer <토큰>` 이 실리고, `X-Write-Key` 는 어떤 요청에도 없다
flow: F1, F2

## SC-05 (error) 비로그인으로 보호 경로에 들어가면 로그인으로 가고, 로그인 후 원래 경로로 돌아온다

- **Given** 비로그인으로 시작한다
- **And** `POST /api/auth/login` 이 아래 표의 사용자로 200 을 준다
- **When** 표의 주소를 연다
- **Then** 주소가 `/login` 이 되고 로그인 화면이 보인다
- **And** 폼(라벨 「공연명」)은 그려진 적이 없다
- **And** 수정 경로면 `GET /api/performances/5` 를 부르지 않았다
- **When** 이메일·비밀번호를 채워 로그인한다
- **Then** 주소가 표의 "돌아온 주소"가 된다(경로·쿼리·해시까지 같다)
- **And** 표의 화면이 보인다

| 행 | 연 주소 | 로그인 사용자 | 돌아온 주소 | 화면 |
|---|---|---|---|---|
| a | `/performances/new` | `ORG` | `/performances/new` | 폼 |
| b | `/performances/5/edit` | `ORG` | `/performances/5/edit` | 폼(값 `가을 재즈 콘서트`) |
| c | `/performances/5/edit?tab=info#top` | `ORG` | `/performances/5/edit?tab=info#top` | 폼 |
| d | `/performances/new` | `AUD` | `/performances/new` | 주최자 전용 안내 |

covers: 비로그인으로 `/performances/new`·`/performances/:id/edit` 에 들어가면 `/login` 으로 가고, 로그인 후 원래 경로로 돌아온다
flow: F2

## SC-06 (boundary) 표기가 달라도 라우터가 등록·수정 화면으로 고르는 주소면 가드를 지난다

- **Given** 비로그인으로 시작한다
- **When** 아래 표의 주소를 연다
- **Then** 주소가 `/login` 이 되고, 넘긴 `from` 이 연 주소 그대로다
- **And** 폼은 그려진 적이 없다
- **Given** (같은 표, 다시) `AUD` 로그인 상태로 시작한다
- **When** 같은 주소를 연다
- **Then** 주최자 전용 안내가 보이고 폼은 없다

| 행 | 연 주소 | 라우터가 고르는 화면 |
|---|---|---|
| a | `/performances/new/` | 등록 |
| b | `/Performances/NEW` | 등록 |
| c | `/performances/%6Eew` | 등록 |
| d | `/performances/5/edit/` | 수정 |
| e | `/PERFORMANCES/5/Edit` | 수정 |
| f | `/performances/5/%65dit` | 수정 |
| g | `/performances/%E0/edit` | 수정(깨진 인코딩의 id) |

covers: 비로그인으로 `/performances/new`·`/performances/:id/edit` 에 들어가면 `/login` 으로 가고, 로그인 후 원래 경로로 돌아온다; 관객으로 같은 경로에 들어가면 폼 대신 「주최자만 이용할 수 있어요」와 「공연 둘러보기」가 보인다
flow: F2

notes: 표의 "라우터가 고르는 화면"은 지금 `App` 의 `Routes` 가 그 주소로 고르는 화면이다(react-router `matchRoutes` — 대소문자 무시·끝 슬래시 허용·퍼센트 디코딩. TASK-018 SC-36 이 인증 경로에서 같은 성질을 보였다). Red 에서 행마다 실제로 고른 화면을 기록하고, 다르면 HITL 전에 설명한다.

## SC-07 (error) 관객이 보호 경로에 들어가면 폼 대신 주최자 전용 안내가 보인다

- **Given** `AUD` 로그인 상태로 시작한다
- **When** 아래 표의 주소를 연다
- **Then** 주최자 전용 안내(제목·부제·「공연 둘러보기」)가 보인다
- **And** 폼(라벨 「공연명」)이 없고, 주소는 연 주소 그대로다
- **And** 수정 경로면 `GET /api/performances/5` 를 부르지 않았다
- **When** 「공연 둘러보기」를 누른다
- **Then** 주소가 `/` 가 된다

| 행 | 연 주소 |
|---|---|
| a | `/performances/new` |
| b | `/performances/5/edit` |

covers: 관객으로 같은 경로에 들어가면 폼 대신 「주최자만 이용할 수 있어요」와 「공연 둘러보기」가 보인다
flow: F2

## SC-08 (boundary) 토큰을 복원하는 동안 가드는 아무것도 그리지 않고, 결과에 따라 갈린다

- **Given** `localStorage.accessToken` 이 `tok-org` 다
- **And** `GET /api/auth/me` 응답을 붙들어 둔다
- **When** 표의 주소를 연다
- **Then** 가드는 아무것도 그리지 않는다(폼·안내가 없고 주소가 그대로다)
- **And** 수정 경로면 `GET /api/performances/5` 를 아직 부르지 않았다
- **When** 붙들어 둔 `/api/auth/me` 를 표의 응답으로 끝낸다
- **Then** 표의 결과가 된다

| 행 | 연 주소 | `/api/auth/me` 응답 | 결과 |
|---|---|---|---|
| a | `/performances/new` | 200 `ORG` | 폼 |
| b | `/performances/5/edit` | 200 `ORG` | 폼(값 `가을 재즈 콘서트`) |
| c | `/performances/new` | 200 `AUD` | 주최자 전용 안내 |
| d | `/performances/new` | 401 | `/login`(from `/performances/new`), 저장된 토큰이 지워진다 |
| e | `/performances/new` | 500 빈 본문 | `/login`(from `/performances/new`), 저장된 토큰은 남는다 |
| f | `/performances/new` | 네트워크 오류 | `/login`(from `/performances/new`), 저장된 토큰은 남는다 |
| g | `/performances/new` | 200 `{ …ORG, role: 'ADMIN' }` | `/login`(from `/performances/new`), 저장된 토큰은 남는다 |

covers: 토큰 복원 중에는 가드가 안내·이동 없이 아무것도 그리지 않는다 (복원 후 주최자면 폼이 보인다)
flow: F2

notes: 행 d~g 에서 토큰을 지울지 남길지는 지금 `AuthProvider` 의 규칙이다(401 만 지운다). 가드는 복원 결과(`user`·`restoring`)를 읽기만 한다.

## SC-09 (error) 쓰기 API 가 401 이면 로그아웃되고, 로그인으로 갔다가 같은 화면으로 돌아온다

- **Given** `ORG` 로그인 상태로 표의 주소를 열었다
- **And** 표의 쓰기 동작이 표의 401 응답을 받는다
- **When** 그 쓰기 동작을 한다(취소는 확인 대화상자에서 「확인」까지)
- **Then** 저장된 토큰이 지워진다
- **And** 주소가 `/login` 이 되고, 넘긴 `from` 이 그 화면의 주소다
- **And** 로그인 화면(제목 「쓰던 계정으로 바로 들어와요」)이 보인다
- **When** `ORG` 로 다시 로그인한다
- **Then** 주소가 그 화면의 주소로 돌아오고 폼이 보인다

| 행 | 화면 주소 | 쓰기 동작 | 401 응답 |
|---|---|---|---|
| a | `/performances/new` | 등록 제출 | `{ code: 'UNAUTHORIZED', message: '인증이 필요합니다…' }` |
| b | `/performances/5/edit` | 수정 저장 | `{ code: 'UNAUTHORIZED' }` |
| c | `/performances/5/edit` | 취소 확인 | `{ code: 'UNAUTHORIZED' }` |
| d | `/performances/new` | 등록 제출 | 빈 본문 |
| e | `/performances/new` | 등록 제출 | 비 JSON `'<html>'` |
| f | `/performances/new` | 등록 제출 | `{ code: 'WRITE_KEY_INVALID' }`(옛 쓰기 가드 코드) |
| g | `/performances/5/edit?tab=info` | 수정 저장 | `{ code: 'UNAUTHORIZED' }` |

covers: 쓰기 API 가 401 이면 로그아웃되고 `/login` 으로 간다. 403 이면 「주최자만 이용할 수 있어요」가 보인다
flow: F3

## SC-10 (error) 쓰기 API 가 403 이면 폼 대신 주최자 전용 안내가 보이고, 로그인은 유지된다

- **Given** `ORG` 로그인 상태로 표의 주소를 열었다
- **And** 표의 쓰기 동작이 표의 403 응답을 받는다
- **When** 그 쓰기 동작을 한다
- **Then** 주최자 전용 안내가 보이고 폼이 없다
- **And** 주소는 그 화면의 주소 그대로다
- **And** 저장된 토큰은 그대로다(`tok-org`)
- **And** 옛 쓰기 키 문구는 없다
- **And** 취소 확인 대화상자(행 c)는 닫혀 있다

| 행 | 화면 주소 | 쓰기 동작 | 403 응답 |
|---|---|---|---|
| a | `/performances/new` | 등록 제출 | `{ code: 'FORBIDDEN_ROLE', message: '주최자만 이용할 수 있습니다' }` |
| b | `/performances/5/edit` | 수정 저장 | `{ code: 'FORBIDDEN_ROLE' }` |
| c | `/performances/5/edit` | 취소 확인 | `{ code: 'FORBIDDEN_ROLE' }` |
| d | `/performances/new` | 등록 제출 | 빈 본문 |
| e | `/performances/new` | 등록 제출 | 비 JSON `'Forbidden'` |
| f | `/performances/new` | 등록 제출 | `{ code: 'WRITE_KEY_INVALID' }`(옛 쓰기 가드 코드) |

covers: 쓰기 API 가 401 이면 로그아웃되고 `/login` 으로 간다. 403 이면 「주최자만 이용할 수 있어요」가 보인다
flow: F3

## SC-11 (boundary) 화면을 떠난 뒤 도착한 쓰기 응답은 로그인 상태·주소·화면을 바꾸지 않는다

- **Given** `ORG` 로그인 상태로 `/performances/new` 를 열었다
- **And** `POST /api/performances` 응답을 붙들어 둔다
- **When** 등록을 제출한다
- **And** 응답 전에 전역 헤더 「로그아웃」을 누른다
- **Then** 주소가 `/login` 이 된다(사용자가 없어져 가드가 보낸 것이다)
- **When** 표의 "그다음"을 한다
- **And** 붙들어 둔 응답을 표의 응답으로 끝낸다
- **Then** 표의 "남는 상태" 그대로다
- **And** 주최자 전용 안내·옛 쓰기 키 문구·「등록하지 못했습니다」 문구가 보이지 않는다

| 행 | 그다음 | 늦은 응답 | 남는 상태 |
|---|---|---|---|
| a | `ORG` 로 다시 로그인(새 토큰 `tok-org2`) — `/performances/new` 로 돌아온다 | 401 `{ code: 'UNAUTHORIZED' }` | 주소 `/performances/new`, 폼이 보인다, 저장된 토큰 `tok-org2` |
| b | 행 a 와 같다 | 403 `{ code: 'FORBIDDEN_ROLE' }` | 주소 `/performances/new`, 폼이 보인다, 저장된 토큰 `tok-org2` |
| c | 행 a 와 같다 | 500 빈 본문 | 주소 `/performances/new`, 폼이 보인다, 저장된 토큰 `tok-org2` |
| d | 행 a 와 같다 | 네트워크 오류 | 주소 `/performances/new`, 폼이 보인다, 저장된 토큰 `tok-org2` |
| e | 아무것도 하지 않는다 | 403 `{ code: 'FORBIDDEN_ROLE' }` | 주소 `/login`, 저장된 토큰 없음 |
| f | 아무것도 하지 않는다 | 401 `{ code: 'UNAUTHORIZED' }` | 주소 `/login`, 저장된 토큰 없음 |

covers: 쓰기 API 가 401 이면 로그아웃되고 `/login` 으로 간다. 403 이면 「주최자만 이용할 수 있어요」가 보인다
flow: F3

notes: 전역 헤더에는 다른 화면으로 가는 링크가 없다. 그래서 로그아웃(가드가 화면을 내린다)으로 "화면을 떠난" 상태를 만든다. 행 a~d 는 늦은 응답이 다시 로그인한 새 세션을 지우거나 바꾸지 않는지를 본다.

## SC-12 (regression) 401·403 이 아닌 쓰기 실패는 지금 문구 그대로이고, 다시 제출하면 꺼진다

- **Given** `ORG` 로그인 상태로 표의 화면을 열었다
- **And** 표의 쓰기 동작이 표의 응답을 받는다
- **When** 그 쓰기 동작을 한다
- **Then** 표의 문구가 보이고, 폼은 그대로 있다
- **And** 저장된 토큰은 그대로이고, 주소도 그대로다
- **When** (행 a 만) 서버가 201 `{ id: 42 }` 를 주도록 바꾸고 다시 제출한다
- **Then** 문구가 없어지고 주소가 `/performances/42` 가 된다

| 행 | 화면 | 쓰기 동작 | 응답 | 문구 |
|---|---|---|---|---|
| a | 등록 | 등록 제출 | 500 빈 본문 | 「등록하지 못했습니다. 다시 시도해 주세요」 |
| b | 등록 | 등록 제출 | 400 `{ code: 'INVALID_TIME_ORDER' }` | 「예매 오픈·마감·공연 일시 순서가 올바르지 않습니다」 |
| c | 수정 | 수정 저장 | 409 `{ code: 'REGISTRATION_ALREADY_OPEN' }` | 「이미 오픈된 공연은 기본 정보를 수정할 수 없습니다」 |
| d | 수정 | 수정 저장 | 404 `{ code: 'PERFORMANCE_NOT_FOUND' }` | 「저장하지 못했습니다. 다시 시도해 주세요」 |
| e | 수정 | 취소 확인 | 500 빈 본문 | 「취소하지 못했습니다. 다시 시도해 주세요」 |

covers: 쓰기 API 가 401 이면 로그아웃되고 `/login` 으로 간다. 403 이면 「주최자만 이용할 수 있어요」가 보인다
flow: F3

notes: 행 d 는 남의 공연·소유자 없는 공연에 대한 저장이다. 이 태스크에서는 지금의 일반 문구를 유지한다(PLAN unresolved #4. 「공연을 찾을 수 없어요」 처리는 TASK-026).

## SC-13 (regression) 인증 화면에서 옛 쓰기 키 거부 응답은 각 화면의 일반 실패 문구로 보인다

- **Given** 아래 표의 기존 승인 테스트 행이다(옛 기대값은 옛 쓰기 키 문구)
- **When** 그 행의 응답을 받는다
- **Then** 표의 "새 기대 문구"가 보인다
- **And** 옛 쓰기 키 문구는 화면에 없다
- **And** 그 행의 나머지 단언(주소·토큰·칸 오류·버튼)은 바꾸지 않는다

| 행 | 테스트 · 표 | 화면 | 응답 | 새 기대 문구 |
|---|---|---|---|---|
| a | `AuthScreens.test.tsx` SC-11 | 가입 | 401 `WRITE_KEY_INVALID` | 가입 일반 실패(`MSG.signupFail`) |
| b | `AuthScreens.test.tsx` SC-17 | 로그인 | 401 `WRITE_KEY_INVALID` | 로그인 일반 실패(`MSG.loginFail`) |
| c | `AuthScreens.test.tsx` 재제출 표(「가입 쓰기 거부 → 201」 행) | 가입 | 첫 응답 401 `WRITE_KEY_INVALID` | 첫 문구가 가입 일반 실패(`MSG.signupFail`)이고, 다시 제출해 201 이면 꺼진다 |
| d | `task018.auth.test.tsx` SC-03 | 로그인 | 401 `WRITE_KEY_INVALID` | `MSG.loginFailed` |
| e | `task018.auth.test.tsx` SC-12 | 가입 | 401 `WRITE_KEY_INVALID` | `MSG.signUpFailed` |
| f | `task018.auth.test.tsx` SC-14 | 가입 뒤 자동 로그인 | 로그인 응답 401 `WRITE_KEY_INVALID` | 주소 `/login`, 맨 위 「가입은 끝났어요. 로그인해 주세요」, 저장된 토큰 없음(그 표의 다른 행과 같다) |
| g | `task018.auth.test.tsx` SC-22 | 소셜 역할 선택(대기 정보 조회) | 401 `WRITE_KEY_INVALID` | `MSG.pendingFailed` + 「다시 시도하기」 |
| h | `task018.auth.test.tsx` SC-26 | 소셜 역할 선택(가입 완료) | 401 `WRITE_KEY_INVALID` | `MSG.completeFailed`, 링크 없음 |
| i | `SocialLoginScreens.test.tsx` 실패 표 | 소셜 가입 완료 | 403 `WRITE_KEY_INVALID` | `MSG.completeFail`, 링크 없음 |
| j | `SocialLoginScreens.test.tsx` SC-10 재제출 표 | 소셜 가입 완료 | 첫 응답 403 `WRITE_KEY_INVALID` | 첫 문구가 일반 실패이고, 다시 누르면 꺼진다 |

covers: `VITE_WRITE_KEY`·`WRITE_REJECTED_MESSAGE`·`isWriteKeyRejected` 가 `frontend/src` 와 `.env.example` 에 없다
flow: F4

notes: `task018.auth.test.tsx`·`SocialLoginScreens.test.tsx` 는 태스크 implementation_spec 경로 밖이다(PLAN unresolved #1 → scope_deviation). 문구 상수 `MSG.writeRejected` 는 각 파일에서 지우고, 행 수는 줄이지 않는다.

## SC-14 (regression) 등록 경로를 그리던 기존 테스트는 주최자 로그인 상태에서 같은 단언을 지킨다

- **Given** 아래 표의 기존 승인 테스트다. 지금은 비로그인으로 `/performances/new` 를 열어 폼을 단언한다
- **When** 같은 주소를 `ORG` 로그인 상태(토큰 + `/api/auth/me`)로 연다
- **Then** 폼(라벨 「공연명」)이 보인다
- **And** 그 테스트의 나머지 단언은 그대로 통과한다

| 행 | 테스트 | 바뀌는 Given | 그대로 두는 단언 |
|---|---|---|---|
| a | `App.test.tsx` SC-10 | `AuthProvider` + `ORG` 로그인 | `getPerformance` 를 부르지 않는다 |
| b | `AuthScreens.test.tsx` SC-28 의 `/performances/new` 단락 | `ORG` 로그인 | 전역 헤더가 있다(로그인 상태라 「로그아웃」 버튼으로 확인) |
| c | `task018.auth.test.tsx` SC-36 의 `/performances/new` 행 | `loggedIn` 을 `ORG` 로 | 전역 헤더가 있다 |

covers: 비로그인으로 `/performances/new`·`/performances/:id/edit` 에 들어가면 `/login` 으로 가고, 로그인 후 원래 경로로 돌아온다
flow: F2

## SC-15 (happy) 쓰기 키 흔적이 소스와 환경 예시에 없다

- **Given** 저장소의 `frontend/src/**`(이 검사 파일 자신은 뺀다)와 `frontend/.env.example` 이다
- **When** 원문을 읽는다
- **Then** `VITE_WRITE_KEY`·`WRITE_REJECTED_MESSAGE`·`isWriteKeyRejected` 가 어느 파일에도 없다(테스트 파일 포함)
- **And** 테스트가 아닌 소스(`*.test.*` 제외)와 `.env.example` 에 `X-Write-Key`·`WRITE_KEY_INVALID` 가 없다
- **And** `frontend/src/pages/WriteKeyRejected.test.tsx` 가 없다
- **And** 옛 쓰기 키 문구가 테스트가 아닌 소스에 없다

covers: `VITE_WRITE_KEY`·`WRITE_REJECTED_MESSAGE`·`isWriteKeyRejected` 가 `frontend/src` 와 `.env.example` 에 없다
flow: F4

notes: 둘째 줄의 패턴은 제약 AUTH-001 의 `forbidden_pattern` 과 같은 범위다. 테스트 파일은 옛 응답 코드 `WRITE_KEY_INVALID` 를 입력값으로 쓰므로(SC-09 f·SC-10 f·SC-13) 둘째 줄에서 뺀다.

## SC-16 (regression) 무효 토큰이 저장돼 있어도 공개 조회 화면은 지금처럼 열린다

- **Given** `localStorage.accessToken` 이 `tok-stale` 이다
- **And** `GET /api/auth/me` 가 401 을 준다(만료·무효 토큰)
- **And** `GET /api/performances` 와 `GET /api/performances/3` 은 토큰과 무관하게 200 을 준다(서버는 무효 토큰을 익명으로 본다 — TASK-022)
- **When** 표의 주소를 연다
- **Then** 표의 화면이 보인다
- **And** 공연 조회 요청이 정확히 그 주소의 것만 나갔고, 오류 문구가 없다
- **And** 전역 헤더는 비로그인 모양이다(「로그인」 링크)

| 행 | 연 주소 | 화면 |
|---|---|---|
| a | `/` | 목록에 `가을 재즈 콘서트` |
| b | `/performances/3` | 상세에 `가을 재즈 콘서트` |

covers: 로그인 상태에서 공연 쓰기 요청에 `Authorization: Bearer <토큰>` 이 실리고, `X-Write-Key` 는 어떤 요청에도 없다
flow: F1

notes: 저장된 토큰은 복원 401 로 지워지므로, 공연 조회 요청에 `tok-stale` 이 실렸는지는 복원 순서에 달렸다. 이 시나리오는 실렸든 아니든 화면이 같은지를 본다(PLAN unresolved #7).

## SC-17 (boundary) 저장소와 화면의 로그인 상태가 갈라져도 요청에는 화면의 로그인 상태가 실린다

- **Given** 아래 표의 저장소 상태와 로그인 경로다
- **When** 표의 "그다음" 뒤에 `request('/api/performances', { method: 'POST' })` 를 보낸다(행 a 는 등록 화면에서 제출한다)
- **Then** 실린 `Authorization` 은 표의 값이다("없음"은 헤더 항목 자체가 없다는 뜻이다)
- **And** 화면의 로그인 상태(전역 헤더의 이름·「로그아웃」 또는 「로그인」 링크)가 표의 "화면" 과 같다

| 행 | 저장소 상태 | 로그인 경로 · 그다음 | Authorization | 화면 |
|---|---|---|---|---|
| a | `setItem` 이 예외를 던진다 | 이메일로 `ORG` 로그인(토큰 `tok-mem`) → `/performances/new` 로 돌아와 등록 제출 | `Bearer tok-mem`, 등록 201 → `/performances/42` | 주최자 로그인 |
| b | `setItem` 이 `QuotaExceededError` 를 던진다 | 이메일로 `ORG` 로그인(토큰 `tok-mem`) | `Bearer tok-mem` | 주최자 로그인 |
| c | `setItem` 이 예외를 던진다 | 소셜 로그인 콜백 `/oauth2/redirect?token=tok-soc` 에서 `/api/auth/me` 가 `ORG` 를 준다 | `Bearer tok-soc` | 주최자 로그인 |
| d | 처음에 `tok-old` 가 저장돼 있고(복원 → `ORG`), `setItem`·`removeItem` 이 예외를 던진다 | 전역 헤더 「로그아웃」 | 없음 (저장소에는 `tok-old` 가 남아 있어도) | 비로그인 |
| e | `tok-org` 로 `ORG` 로그인 상태 | 다른 탭이 저장소 값을 `tok-aud` 로 바꾼다(저장소를 직접 고친다) | `Bearer tok-org` | 주최자 로그인 |
| f | `tok-org` 로 `ORG` 로그인 상태 | 다른 탭이 저장소 값을 지운다(저장소를 직접 지운다) | `Bearer tok-org` | 주최자 로그인 |

covers: 로그인 상태에서 공연 쓰기 요청에 `Authorization: Bearer <토큰>` 이 실리고, `X-Write-Key` 는 어떤 요청에도 없다
flow: F1

notes: Phase 4 1회차 RC-1 의 변종 4개(저장 실패·쿼터·소셜 로그인 경로·다른 탭)를 한 시나리오로 가져왔다. 행 e·f 는 이 탭의 화면 상태를 정본으로 삼는다는 결정이다 — 다른 탭의 로그아웃·다른 계정 로그인이 이 탭의 요청을 다른 사람의 것으로 바꾸지 않는다. 다른 탭의 변화를 이 탭 화면에 반영하는 것(storage 이벤트)은 범위 밖이다.

## 기존 승인 테스트 변경 (2b 에서 함께 승인받는다)

| 파일 | 바뀌는 것 | 근거 |
|---|---|---|
| `frontend/src/api/http.test.ts` | sc04(쓰기 키가 있으면 쓰기 요청에만 `X-Write-Key`)·sc05(키가 없으면 헤더 없음)를 SC-01·SC-02 로 바꾼다. sc01~03(주소)은 그대로 | SC-01·02 |
| `frontend/src/pages/WriteKeyRejected.test.tsx` | 파일 삭제. sc06~08 은 SC-09·SC-10 이, sc09·sc12 는 SC-12 가 대신한다 | SC-09·10·12·15 |
| `frontend/src/auth/AuthScreens.test.tsx` | SC-11·SC-17·재제출 표의 쓰기 키 행(SC-13 a~c), `ALL_ERRORS` 의 `WRITE_REJECTED_MESSAGE`, SC-28 의 등록 단락(SC-14 b) | SC-13·14 |
| `frontend/src/auth/task018.auth.test.tsx` | SC-03·SC-12·SC-14·SC-22·SC-26 의 쓰기 키 행(SC-13 d~h), SC-36 의 등록 행(SC-14 c), `MSG.writeRejected` | SC-13·14 |
| `frontend/src/auth/SocialLoginScreens.test.tsx` | 실패 표·재제출 표의 쓰기 가드 행(SC-13 i·j), `MSG.writeRejected` | SC-13 |
| `frontend/src/App.test.tsx` | SC-10 을 `ORG` 로그인 상태로(SC-14 a) | SC-14 |
