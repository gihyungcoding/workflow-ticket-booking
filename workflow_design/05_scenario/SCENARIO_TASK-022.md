# TASK-022 시나리오

대상:
- `JwtTokenProvider`(`verifyAccessClaims`), `AccessClaims`, `JwtAuthenticationFilter`, `SecurityConfig`, `CorsFilter`
- `PerformanceController`의 등록·수정·취소
- `PerformanceService`의 소유자 지정·소유권 검사
- `Performance.ownerId`
- `V7__add_performance_owner.sql`
- `WriteGuardFilter`·`WriteGuardConfig` 삭제

**시각은 테스트가 고정한다.** 아래의 `T` 는 `MutableClock` 에 넣은 시각 `2026-11-11T08:00:00Z` 다
(`ClockTestConfig`, ADR-0005).

**DB 를 쓰는 방식은 두 가지다.**
- SC-01 만 PostgreSQL(Testcontainers) 위에서 Flyway 로 돈다.
- 나머지는 기존 H2 테스트 구성 그대로 돈다.
- "DB 가 바뀌지 않는다"와 "DB 에 반영된다"는 클래스 레벨 `@Transactional` 없이 확인한다.
  리포지토리 `count()`·`findById()` 로 새로 읽는다(CLAUDE.md 리뷰 지시).

## 규약 (정본)

시나리오는 이 값을 그대로 단언한다. 값을 바꾸려면 여기부터 고친다.

| 항목 | 값 |
|---|---|
| 주최자 `O1`·`O2` | `AuthTestHelper.signUp` 으로 역할 `ORGANIZER` 로 가입하고, `login` 으로 얻은 access 토큰을 쓴다. 서로 다른 사용자다 |
| 관객 `A1` | 같은 방법으로 역할 `AUDIENCE` 로 가입한 사용자 |
| 기본 등록 요청 `R0` | title `소리풍경 밴드 나이트`, venue `롤링홀`, openAt `T+7d`, closeAt `T+29d`, startAt `T+30d`, sections `R` A~B열 열당 10석 55000 |
| 기본 수정 요청 `U0` | title `소리풍경 밴드 나이트 앙코르`, venue·시각은 `R0` 과 같다 |
| 쓰기 경로 3개 | `POST /api/performances`(본문 `R0`), `PUT /api/performances/{id}`(본문 `U0`), `POST /api/performances/{id}/cancel` |
| 접근 규칙 | `/api/performances/**` 의 `GET` 은 누구나 쓸 수 있다. 그 밖의 메서드는 `ROLE_ORGANIZER` 가 있어야 한다 |
| 401 본문 | `{"code":"UNAUTHORIZED","message":…}` — 기존 entry point 와 같다 |
| 403 본문 | `{"code":"FORBIDDEN_ROLE","message":"주최자만 이용할 수 있습니다"}`, `Content-Type` `application/json` |
| 404 본문 | `{"code":"PERFORMANCE_NOT_FOUND","message":"Performance not found: <요청한 id>"}` — 남의 공연·소유자 없음·없는 id 가 같다 |
| 유효한 사용자 클레임 | `sub` 는 JSON 문자열이고, 1 이상의 정수를 정규형 10진으로 적은 것이어야 한다 — ASCII 숫자만, 부호·앞자리 0 없음, long 범위. 그 밖이면 토큰 전체가 무효다(쓰기 경로에서 401) |
| 유효한 역할 클레임 | `role` 이 문자열 `"ORGANIZER"` 또는 `"AUDIENCE"` 와 **정확히** 같아야 한다. 그 밖의 값(누락·null·문자열이 아닌 값·정의 밖 문자열·대소문자 다름)이면 토큰 전체가 무효다. 무효 토큰은 쓰기 경로에서 401 이다 |
| 응답의 소유자 | 공연 응답 JSON 에 `ownerId` 키가 없다(등록·수정·취소·목록·상세) |
| CORS 허용 헤더 | preflight 응답의 `Access-Control-Allow-Headers` 는 정확히 `Content-Type, Authorization` 이다 |

## SC-01 (regression) V7 은 기존 공연이 있는 PostgreSQL 에 적용되고 소유자를 비워 둔다

- **Given** PostgreSQL 에 V6 까지 적용했다
- **And** 기존 공연 1행과 좌석 20행을 넣었다(`seedV5Performance` 관례)
- **And** 사용자 1행을 넣었다
- **And** 기존 공연 행의 열 값을 읽어 두었다
- **When** Flyway 를 끝까지 적용한다
- **Then** 적용에 성공하고 현재 버전은 `7` 이다
- **And** 기존 공연의 `owner_id` 는 NULL 이다
- **And** `title`·`venue`·`start_at`·`open_at`·`close_at`·`total_seats`·`available_seats`·`cancelled`·`poster_url` 은 읽어 둔 값과 같다
- **And** 없는 사용자 id 로 `owner_id` 를 갱신하면 외래 키 위반으로 실패한다
- **And** 있는 사용자 id 로 갱신한 뒤 그 사용자 행을 지우면, 그 공연의 `owner_id` 가 NULL 이 된다(공연 행은 남는다)

covers: 마이그레이션 후 기존 `performance` 행의 `owner_id` 가 NULL 이고, 기존 관객 조회 API 응답이 마이그레이션 전과 같다
flow: F1

## SC-02 (regression) 소유자 없는 기존 공연의 관객 응답은 그대로다

- **Given** `ownerId` 를 주지 않는 기존 생성자로 공연 `P0` 을 저장했다(소유자 NULL)
- **When** 토큰 없이 `GET /api/performances` 와 `GET /api/performances/{P0}` 을 호출한다
- **Then** 둘 다 200 이다
- **And** 목록 항목과 상세의 키 집합이 이 태스크 전과 같다(`PerformanceBrowseResponseApiTest` 의 기존 키 단언 그대로)
- **And** 어느 응답에도 `ownerId` 키가 없다

covers: 마이그레이션 후 기존 `performance` 행의 `owner_id` 가 NULL 이고, 기존 관객 조회 API 응답이 마이그레이션 전과 같다
flow: F1

## SC-03 (happy) 주최자가 등록하면 그 사람이 소유자다

- **Given** 공연이 없다
- **When** `O1` 의 토큰을 `Authorization: Bearer <토큰>` 으로 실어 `R0` 으로 `POST /api/performances` 를 호출한다
- **Then** 201 이다
- **And** 응답 본문에 `ownerId` 키가 없다
- **And** 트랜잭션 밖에서 응답 id 로 다시 읽은 공연의 `ownerId` 가 `O1` 의 사용자 id 다
- **And** 그 공연의 좌석은 20행이다(기존 등록 규칙 그대로)

covers: 주최자 토큰으로 `POST /api/performances` 를 보내면 201 이고, 저장된 행의 `owner_id` 가 그 사용자 id 다 (비트랜잭션 조회로 확인)
flow: F2

## SC-04 (error) 토큰이 없거나 무효면 401, 관객이면 403 이고 DB 는 그대로다

- **Given** `O1` 소유의 공연 `P1` 이 있다(openAt `T+7d`, 취소 안 됨)
- **And** 공연 수·좌석 수를 트랜잭션 밖에서 세어 두었다
- **When** 쓰기 경로 3개 각각에 아래 표의 `Authorization` 을 실어 보낸다(수정·취소의 `{id}` 는 `P1`)
- **Then** 상태와 `code` 가 표와 같다
- **And** 요청마다 공연 수·좌석 수가 그대로다
- **And** `P1` 의 title 은 `소리풍경 밴드 나이트` 이고 `cancelled` 는 false 다

| # | Authorization | 기대 |
|---|---|---|
| a | 헤더 없음 | 401 `UNAUTHORIZED` |
| b | 빈 문자열 `""` | 401 `UNAUTHORIZED` |
| c | `Bearer ` (토큰 없음) | 401 `UNAUTHORIZED` |
| d | `Bearer <O1 토큰의 서명 한 글자를 바꾼 것>` | 401 `UNAUTHORIZED` |
| e | `Bearer <A1 토큰>` | 403 `FORBIDDEN_ROLE` (본문은 규약 표 그대로) |
| f | `Bearer <A1 토큰의 payload 에서 role 만 ORGANIZER 로 바꾸고 서명은 그대로 둔 것>` | 401 `UNAUTHORIZED` |

covers: 토큰 없이 `POST`·`PUT /api/performances/**` 를 보내면 401, 관객 토큰이면 403 `FORBIDDEN_ROLE` 이고 DB 가 바뀌지 않는다
flow: F3

## SC-05 (error) 역할 클레임이 정확한 값이 아니면 토큰이 무효다

- **Given** 공연이 없다
- **And** 아래 표의 `role` 로 테스트 비밀(`JWT_SECRET`)로 직접 서명한 access 토큰을 만들었다
  - `sub` 는 `O1` 의 id, `typ` 는 `access`, 만료는 `T+1h` 다
  - `role` 외의 클레임은 정상이다
- **When** 그 토큰으로 `R0` 을 `POST /api/performances` 에 보낸다
- **Then** 상태와 `code` 가 표와 같다
- **And** 201 이 아닌 행에서는 공연 수가 0 그대로다(트랜잭션 밖에서 센다)
- **And** a 행에서 저장된 공연의 `ownerId` 는 `O1` 의 id 다

| # | role 클레임 | 기대 |
|---|---|---|
| a | `"ORGANIZER"` | 201 |
| b | `"AUDIENCE"` | 403 `FORBIDDEN_ROLE` |
| c | 클레임 없음 | 401 `UNAUTHORIZED` |
| d | `null` | 401 `UNAUTHORIZED` |
| e | 숫자 `1` | 401 `UNAUTHORIZED` |
| f | 배열 `["ORGANIZER"]` | 401 `UNAUTHORIZED` |
| g | 객체 `{"name":"ORGANIZER"}` | 401 `UNAUTHORIZED` |
| h | 불리언 `true` | 401 `UNAUTHORIZED` |
| i | `"ADMIN"` (정의 밖) | 401 `UNAUTHORIZED` |
| j | `"organizer"` (소문자) | 401 `UNAUTHORIZED` |
| k | `" ORGANIZER"` (앞 공백) | 401 `UNAUTHORIZED` |
| l | `""` | 401 `UNAUTHORIZED` |
| m | `"   "` (공백만) | 401 `UNAUTHORIZED` |
| n | `"\u3000"` (전각 공백만) | 401 `UNAUTHORIZED` |
| o | `"ORGANIZER\u200B"` (제로폭 공백 꼬리) | 401 `UNAUTHORIZED` |

role 이 `"ORGANIZER"` 로 정상일 때, 다른 클레임만 아래처럼 바꾼 토큰도 같은 요청으로 보낸다.

| # | 바꾼 클레임 | 기대 |
|---|---|---|
| p | `sub` 없음 | 401 `UNAUTHORIZED` |
| q | `sub` 숫자 `7`(문자열이 아님) | 401 `UNAUTHORIZED` |
| r | `sub` `"7.0"` | 401 `UNAUTHORIZED` |
| s | `sub` `"7e0"` | 401 `UNAUTHORIZED` |
| t | `sub` `"9223372036854775808"` (long 초과) | 401 `UNAUTHORIZED` |
| u | `typ` `"pending"` | 401 `UNAUTHORIZED` |
| v | 만료 `T-1s` | 401 `UNAUTHORIZED` |
| w | `sub` `"+<O1 id>"` (부호) | 401 `UNAUTHORIZED` |
| x | `sub` `"0<O1 id>"` (앞자리 0) | 401 `UNAUTHORIZED` |
| y | `sub` 가 `O1` 의 id 를 아라비아-인도 숫자(`٠`~`٩`)로 적은 것 | 401 `UNAUTHORIZED` |
| z | `sub` `"-<O1 id>"` (음수) | 401 `UNAUTHORIZED` |
| aa | `sub` `"0"` | 401 `UNAUTHORIZED` |
| ab | `sub` `"<O1 id>.5"` (소수) | 401 `UNAUTHORIZED` |
| ac | `sub` `" <O1 id>"` (앞 공백) | 401 `UNAUTHORIZED` |
| ad | `sub` 가 `O1` 의 id 를 전각 숫자(`０`~`９`)로 적은 것 | 401 `UNAUTHORIZED` |

w~z·ac·ad 는 `O1` 의 id 를 다르게 적은 것이다 — 읽으면 같은 사용자(또는 그 음수)가 되지만 정규형이 아니라 거부된다.

covers: 토큰 없이 `POST`·`PUT /api/performances/**` 를 보내면 401, 관객 토큰이면 403 `FORBIDDEN_ROLE` 이고 DB 가 바뀌지 않는다
covers: 주최자 토큰으로 `POST /api/performances` 를 보내면 201 이고, 저장된 행의 `owner_id` 가 그 사용자 id 다 (비트랜잭션 조회로 확인)
flow: F2, F3

## SC-06 (error) Authorization 헤더 앞뒤에 붙은 글자는 정상 토큰을 무효로 만든다

- **Given** 공연이 없다
- **And** `O1` 의 정상 토큰을 `<t>` 라 한다
- **When** 아래 표의 헤더로 `R0` 을 `POST /api/performances` 에 보낸다
- **Then** 상태가 표와 같다
- **And** 201 이 아닌 행에서는 공연 수가 0 그대로다

| # | Authorization | 기대 |
|---|---|---|
| a | `Bearer <t>` | 201 |
| b | `bearer <t>` (스킴 소문자, RFC 7235) | 201 |
| c | `Bearer  <t>` (공백 두 개) | 401 |
| d | `Bearer <t> ` (뒤 공백) | 401 |
| e | ` Bearer <t>` (앞 공백) | 401 |
| f | `<t>` (스킴 없음) | 401 |
| g | `Bearer <t>~` | 401 |
| h | `Bearer <t>é` (비 ASCII 꼬리) | 401 |
| i | `Basic <t>` | 401 |
| j | `Bearer <t>=` (`=` 꼬리 — 토큰 문법상 허용되는 글자지만 서명이 맞지 않는다) | 401 |
| k | `Bearer <t>\n` (LF 꼬리) | 400 (제어 문자가 든 헤더는 요청 자체가 거부된다 — 이 태스크 전과 같다) |
| l | `Bearer <t>\u0000` (NUL 꼬리) | 400 (k 와 같은 거부) |
| m | `Bearer <t>\uD800` (짝 없는 서로게이트 꼬리) | 401 |

- a·b 는 서로 다른 테스트 실행에서 각각 1건씩 만들어진다
- k·l 은 상태만 단언한다. 거부 응답에는 본문이 없다
- 공연 수는 행마다 직전 값과 비교한다

covers: 토큰 없이 `POST`·`PUT /api/performances/**` 를 보내면 401, 관객 토큰이면 403 `FORBIDDEN_ROLE` 이고 DB 가 바뀌지 않는다
flow: F2, F3

## SC-07 (error) 남의 공연·소유자 없는 공연·없는 공연은 같은 404 이고 DB 는 그대로다

- **Given** `O2` 소유의 공연 `P2` 가 있다
- **And** 소유자가 NULL 인 공연 `P0` 이 있다
- **And** 둘 다 openAt `T+7d` 이고 취소되지 않았다
- **And** 공연 수와 각 공연의 title·`cancelled` 를 트랜잭션 밖에서 읽어 두었다
- **When** `O1` 의 토큰으로 아래 표의 id 에 수정(`U0`)과 취소를 각각 보낸다
- **Then** 상태·본문이 표와 같다
- **And** 남의 공연·소유자 없는 공연·없는 공연 세 행의 본문은 `message` 에 들어간 id 만 다르다(키 집합과 `code` 가 같다)
- **And** `P2`·`P0` 의 title·`cancelled`·`ownerId` 와 공연 수가 그대로다

| # | id | 기대 |
|---|---|---|
| a | `P2` (남의 공연) | 404 `PERFORMANCE_NOT_FOUND`, message `Performance not found: <P2>` |
| b | `P0` (소유자 NULL) | 404 `PERFORMANCE_NOT_FOUND`, message `Performance not found: <P0>` |
| c | `999999` (없음) | 404 `PERFORMANCE_NOT_FOUND`, message `Performance not found: 999999` |
| d | `0` | 404 `PERFORMANCE_NOT_FOUND` |
| e | `-1` | 404 `PERFORMANCE_NOT_FOUND` |
| f | `9223372036854775808` (long 초과) | 400 — 상태만 단언한다(기존 회귀 표와 같은 수준) |
| g | `1.9` | 400 — 상태만 단언한다 |

- **And** 본문 검증은 소유권보다 먼저다. title 이 빈 문자열인 수정 본문을 `P2`·`P0`·`999999` 에 보내면 세 경우 모두 400 `INVALID_REQUEST` 로 같다(소유 여부가 드러나지 않는다)

covers: 다른 주최자의 공연, 소유자가 NULL 인 공연, 없는 id 에 수정·취소를 보내면 모두 같은 형태의 404 이고 DB 가 바뀌지 않는다
flow: F4

## SC-08 (happy) 자기 공연을 오픈 전에 수정하면 반영된다

- **Given** 시계가 `T` 이고, `O1` 이 `R0` 으로 등록한 공연 `P1` 이 있다(openAt `T+7d`)
- **When** `O1` 의 토큰으로 `PUT /api/performances/{P1}` 에 `U0` 을 보낸다
- **Then** 200 이고 응답 title 이 `소리풍경 밴드 나이트 앙코르` 다
- **And** 트랜잭션 밖에서 다시 읽은 `P1` 의 title 도 같고, `ownerId` 는 `O1` 그대로다

covers: 자기 공연의 수정·취소는 지금 규칙(수정은 `now < openAt`) 그대로 동작하고 DB 에 반영된다
flow: F5

## SC-13 (error) 자기 공연이라도 오픈 시각이 되면 수정할 수 없다

- **Given** `O1` 소유의 공연 `P1` 이 있다(openAt `T+7d`, title `소리풍경 밴드 나이트`)
- **And** 시계가 `T+7d`(openAt 과 같은 시각)다
- **When** `O1` 의 토큰으로 `PUT /api/performances/{P1}` 에 title `다른 제목` 인 본문을 보낸다
- **Then** 409 `REGISTRATION_ALREADY_OPEN` 이다
- **And** 트랜잭션 밖에서 다시 읽은 title 은 `소리풍경 밴드 나이트` 그대로다

covers: 자기 공연의 수정·취소는 지금 규칙(수정은 `now < openAt`) 그대로 동작하고 DB 에 반영된다
flow: F5

## SC-14 (happy) 자기 공연을 취소하면 반영된다

- **Given** `O1` 소유의 공연 `P1` 이 있다(취소 안 됨)
- **When** `O1` 의 토큰으로 `POST /api/performances/{P1}/cancel` 을 보낸다
- **Then** 200 이고 응답 `cancelled` 가 true 다
- **And** 트랜잭션 밖에서 다시 읽은 `P1` 의 `cancelled` 가 true 다

covers: 자기 공연의 수정·취소는 지금 규칙(수정은 `now < openAt`) 그대로 동작하고 DB 에 반영된다
flow: F5

## SC-09 (happy) 쓰기 키 없이 가입·로그인·소셜 가입 완료가 되고, 가드 환경변수를 켜도 막히지 않는다

- **Given** 테스트 컨텍스트에 `WRITE_GUARD_ENABLED=true`, `WRITE_API_KEY=guard-key-0123456789` 를 준다
  (`@SpringBootTest(properties=…)`)
- **And** 그 값이 실제로 들어갔다 — `Environment.getProperty("WRITE_GUARD_ENABLED")` 가 `"true"` 다
- **When** `X-Write-Key` 헤더 없이 아래 요청을 차례로 보낸다
- **Then** 상태가 표와 같다
- **And** 어느 응답 본문의 `code` 도 `WRITE_KEY_INVALID`(지금 가드의 거부 코드)가 아니다

| # | 요청 | 기대 |
|---|---|---|
| a | `POST /api/auth/signup` (ORGANIZER) | 201 |
| b | `POST /api/auth/login` (a 의 계정) | 200, accessToken 있음 |
| c | `POST /api/auth/social/complete` (유효한 pendingToken, 역할 AUDIENCE) | 200 (지금 가입 완료 응답과 같다) |
| d | `POST /api/performances` (b 의 토큰, `R0`) | 201 |

covers: `X-Write-Key` 없이 가입·로그인·소셜 가입 완료가 성공한다. `WRITE_GUARD_ENABLED=true` 를 줘도 쓰기 요청이 키 때문에 거부되지 않는다
flow: F6

## SC-10 (regression) 쓰기 가드 코드가 없다

- **When** 쓰기 가드 필터와 그 설정 클래스(`com.example.ticket_booking.config.WriteGuardFilter`·`…config.WriteGuardConfig`)를 이름으로 불러온다
- **Then** 둘 다 `ClassNotFoundException` 이다
- **And** 애플리케이션 컨텍스트에 `writeGuardFilter` 라는 이름의 빈이 없다

이 시나리오는 삭제를 확인하는 것이라 이름이 그대로 나온다.
테스트 파일 5개(`WriteGuardFilterTest` 등)가 저장소에 없다는 것은 자동 테스트로 확인하지 않는다.
Phase 4 에서 `git ls-files` 로 확인한다.

covers: `WriteGuardFilter`·`WriteGuardConfig` 와 그 테스트가 저장소에 없고, CORS 허용 헤더에 `X-Write-Key` 가 없다
flow: F7

## SC-15 (regression) CORS preflight 가 키 헤더를 허용하지 않는다

- **Given** 허용된 Origin 이 있다
- **When** 그 Origin 으로 `OPTIONS /api/performances` preflight 를 보낸다
  (`Access-Control-Request-Method: POST`, `Access-Control-Request-Headers: authorization, content-type`)
- **Then** `Access-Control-Allow-Headers` 가 정확히 `Content-Type, Authorization` 이다
- **And** 그 값에 `X-Write-Key` 가 없다

covers: `WriteGuardFilter`·`WriteGuardConfig` 와 그 테스트가 저장소에 없고, CORS 허용 헤더에 `X-Write-Key` 가 없다
flow: F7

## SC-11 (happy) 공연 조회는 토큰 없이도, 무효 토큰이 실려도 열린다

- **Given** 시계가 `T` 이고, 공연 `P1`(openAt `T+7d`, 취소 안 됨)이 있다
- **When** 아래 세 조회를 `Authorization` 없이 한 번, `Bearer invalid.token.value` 로 한 번 보낸다
- **Then** 모두 200 이다
- **And** 상세 응답 id 가 `P1` 이다
- **And** next-opening 응답 id 도 `P1` 이다

| 조회 |
|---|
| `GET /api/performances` |
| `GET /api/performances/{P1}` |
| `GET /api/performances/next-opening` |

covers: `GET /api/performances/**` 는 토큰 없이 지금처럼 200 이다
flow: F8

## SC-12 (regression) 기존 오류 응답 회귀 표는 쓰기 행에 주최자 토큰을 실으면 같은 숫자다

- **Given** 공연 `P0`(소유자 NULL)이 있다
- **When** 아래 표의 요청을 표의 토큰으로 보낸다
- **Then** 모든 행의 상태가 표와 같다

| # | 요청 | 토큰 | 기대 |
|---|---|---|---|
| a | `GET /api/performances/abc` | 없음 | 400 |
| b | `GET /api/performances?page=abc` | 없음 | 400 |
| c | `GET /api/performances?status=NOPE` | 없음 | 400 |
| d | `DELETE /api/performances/{P0}` | `O1` | 405 |
| e | `PATCH /api/performances/{P0}` | `O1` | 405 |
| f | `POST /api/performances` (text/plain 본문 `x`) | `O1` | 415 |
| g | `POST /api/performances/abc/cancel` | `O1` | 400 |
| h | `GET /api/performances/99999` | 없음 | 404 |
| i | `DELETE /api/performances/{P0}` | 없음 | 401 |
| j | `POST /api/performances` (text/plain 본문 `x`) | 없음 | 401 |
| k | `DELETE /api/performances/{P0}` | `A1` | 403 |
| l | `PATCH /api/performances/{P0}` | `A1` | 403 |

a~h 는 `PerformanceErrorResponseRegressionTest` 의 8행이다. 기대 숫자는 그대로 두고, 쓰기 행에만 토큰을 싣는다.
그래서 이것은 승인된 테스트를 고치는 일이다(아래 「기존 승인 테스트 변경」).
i~l 은 새로 더하는 행이다.

covers: `GET /api/performances/**` 는 토큰 없이 지금처럼 200 이다
covers: 토큰 없이 `POST`·`PUT /api/performances/**` 를 보내면 401, 관객 토큰이면 403 `FORBIDDEN_ROLE` 이고 DB 가 바뀌지 않는다
flow: F3, F8

---

## 기존 승인 테스트 변경 (2b 에서 함께 승인받는다)

| 파일 | 무엇이 깨지나 | 어떻게 고치나 |
|---|---|---|
| `api/AuthRegressionApiTest` | "Authorization 없이 등록·수정·취소 201/200" 을 단언한다. 새 정책과 반대다 | 쓰기 3행을 401 기대로 바꾸고, 주최자 토큰을 실은 행(201/200)을 더한다. 조회 행은 그대로 둔다 |
| `api/PerformanceErrorResponseRegressionTest` | 쓰기 행을 토큰 없이 보내 401 이 된다 | SC-12 대로 쓰기 행에 주최자 토큰을 싣는다. 기대 숫자는 그대로 둔다 |
| `api/PerformanceRegistrationApiTest`·`api/PerformanceZoneRegistrationApiTest`·`api/PerformanceBrowseResponseApiTest` | 토큰 없이 등록·수정·취소를 한다 | 주최자 토큰 픽스처를 싣는다. 수정·취소 대상은 같은 주최자가 등록한 공연이어야 한다 |
| `repository/PerformanceZoneMigrationTest` SC-14 | 끝까지 적용한 뒤 버전 `6` 을 단언한다 | `flyway("6")` 으로 대상을 고정한다 |
| `config/CorsFilterTest` | 허용 헤더에 `X-Write-Key` 가 있다고 단언한다 | SC-10 ② 로 바꾼다 |
| `config/WriteGuardFilterTest` 외 4개 | 대상 코드가 사라진다 | 파일을 삭제한다 |
