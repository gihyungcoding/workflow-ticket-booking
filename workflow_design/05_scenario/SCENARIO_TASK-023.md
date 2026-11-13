# TASK-023 시나리오

대상:
- `V8__add_performance_published.sql`, `Performance.published`
- `PerformanceRepositoryImpl`(관객 조회 공개 조건, 주최자 목록), `PerformanceService`(공개 전환·주최자 조회·수정 규칙)
- `PerformanceStatusRules`(`editable`·`visibilityChangeable`), `OrganizerPerformanceResponse`·`OrganizerPerformanceListResponse`
- `OrganizerPerformanceController`, `PerformanceController`(publish·unpublish, 쓰기 응답 타입), `SecurityConfig`(`/api/organizer/**`)

**시각은 테스트가 고정한다.** 아래의 `T` 는 `MutableClock` 에 넣은 시각 `2026-11-13T09:00:00Z` 다(`ClockTestConfig`, ADR-0005).

**DB 를 쓰는 방식은 두 가지다.**
- SC-01 만 PostgreSQL(Testcontainers) 위에서 Flyway 로 돈다.
- 나머지는 기존 H2 테스트 구성(ddl-auto)이다. "DB 가 바뀌지 않는다"와 "DB 에 반영된다"는 클래스 레벨 `@Transactional` 없이
  리포지토리 `findById()`·`count()` 로 새로 읽어 확인한다(CLAUDE.md 리뷰 지시 — dirty checking 으로 커밋이 미뤄지는 UPDATE 경로).

## 규약 (정본)

시나리오는 이 값을 그대로 단언한다. 값을 바꾸려면 여기부터 고친다.

| 항목 | 값 |
|---|---|
| 주최자 `O1`·`O2` / 관객 `A1` | `AuthTestHelper` 로 역할 `ORGANIZER`·`ORGANIZER`·`AUDIENCE` 로 가입하고 `login` 으로 얻은 access 토큰. 서로 다른 사용자다 |
| 공연 표기 | `P(소유자, 공개 여부, 취소 여부, openAt, closeAt, startAt)`. 따로 적지 않으면 소유자 `O1`·공개·취소 안 됨·openAt `T+7d`·closeAt `T+29d`·startAt `T+30d`·좌석 100/100. `repository.save` 로 만든다 |
| 기존 생성자 | `ownerId` 와 `published` 를 주지 않는 기존 생성자로 만든 공연은 **공개**다(`published = true`). 서비스의 등록 경로만 `published = false` 를 명시한다 — 승인 테스트의 픽스처가 관객 조회에서 사라지지 않게 하는 PLAN 결정 |
| 주최자 응답 키 | `id`·`title`·`venue`·`startAt`·`openAt`·`closeAt`·`totalSeats`·`availableSeats`·`status`·`published`·`editable`·`visibilityChangeable`. `posterUrl` 은 값이 있을 때만, `sections` 는 **단건 조회에서만** 있다. `ownerId` 키는 어디에도 없다 |
| 관객 응답 키 | 기존과 같다 — `id`·`title`·`venue`·`startAt`·`openAt`·`closeAt`·`totalSeats`·`availableSeats`·`status`, `posterUrl`(값이 있을 때), 단건은 `sections` 추가. `published`·`editable`·`visibilityChangeable`·`ownerId` 는 없다 |
| `editable` | `!cancelled && (!published || now < openAt)` — 지금 수정 요청이 성공하는가 |
| `visibilityChangeable` | `!cancelled && now < openAt` — 지금 publish·unpublish 요청이 성공하는가 |
| 검사 순서 | 인가(401·403) → 소유(404) → 취소(409 `PERFORMANCE_CANCELLED`) → 시각(공개 전환 409 `VISIBILITY_LOCKED`, 수정 409 `REGISTRATION_ALREADY_OPEN`). 앞 단계에서 걸리면 뒤 단계 오류는 나오지 않는다 |
| 동시 쓰기 | 같은 공연에 대한 쓰기(수정·취소·publish·unpublish)는 한 번에 하나씩 처리된다. 다른 요청이 그 공연을 변경 중이면 끝날 때까지 기다리고, 기다린 뒤의 **최신 행과 그때의 시각**으로 위 검사 순서를 밟는다. 변경은 최신 행 위에 반영돼 다른 요청의 변경을 덮어쓰지 않는다(Phase 4 RC-1) |
| 401 / 403 본문 | TASK-022 규약 그대로 — 401 `{"code":"UNAUTHORIZED",…}`, 403 `{"code":"FORBIDDEN_ROLE","message":"주최자만 이용할 수 있습니다"}`. 이 태스크는 값을 바꾸지 않고 `/api/organizer/**` 에도 같은 규칙을 건다 |
| 404 본문 | `{"code":"PERFORMANCE_NOT_FOUND","message":"Performance not found: <요청한 id>"}` — 남의 공연·소유자 없음·없는 id·(관객 단건 조회의) 비공개가 같은 형태다. 근거: `PerformanceExceptionHandler.handleNotFound`, TASK-022 규약 |
| 409 본문 | `code` 가 각각 `VISIBILITY_LOCKED`·`PERFORMANCE_CANCELLED`·`REGISTRATION_ALREADY_OPEN` 이다. 메시지 문구는 단언하지 않는다 |
| 목록 응답 | `{"items":[…],"page":n,"size":n,"totalElements":n}` — 관객 목록의 `content` 키와 다르다. 항목에는 `sections` 키가 없다 |
| 목록 요청 오류 | `page < 0` 또는 `size < 1`, 또는 `page × size` 가 int 범위를 넘으면 **400 `INVALID_REQUEST`** 다. `size > 100` 은 100 으로 자른다. 숫자가 아닌 값·소수·long 초과는 프레임워크 타입 불일치로 400(본문 없음)이다. 근거: 관객 목록은 `page=-1`·`size=0`·`page=2147483647` 에서 처리되지 않은 예외(운영 500)가 나는 것을 프로브로 확인했다(2026-11-13) — 이 태스크의 새 목록은 그렇게 두지 않는다. 관객 목록(`GET /api/performances`)도 같은 규칙이다 — SC-21 (Phase 4 RC-2 로 이번 태스크에 포함, 사용자 결정 2026-11-13) |

## SC-01 (happy) V8 은 기존 공연이 있는 PostgreSQL 에 적용되고 기존 행을 공개로 둔다

- **Given** PostgreSQL 에 V7 까지 적용했다
- **And** 기존 공연 1행과 좌석 20행을 넣었다(`PerformanceOwnerMigrationTest` 의 시드 관례)
- **And** 기존 공연 행의 열 값을 읽어 두었다
- **When** Flyway 를 끝까지 적용한다
- **Then** 적용에 성공하고 현재 버전은 `8` 이다
- **And** 기존 공연의 `published` 는 TRUE 다
- **And** `title`·`venue`·`start_at`·`open_at`·`close_at`·`total_seats`·`available_seats`·`cancelled`·`poster_url`·`owner_id` 는 읽어 둔 값과 같다
- **And** `published` 를 지정하지 않고 새 공연 행을 INSERT 하면 그 행의 `published` 는 FALSE 다(컬럼 기본값)
- **And** `published` 에 NULL 을 넣으면 NOT NULL 위반으로 실패한다

covers: 마이그레이션 후 기존 행은 `published = true` 이고, 새로 등록한 공연은 `published = false` 다
flow: F1

회귀(승인 테스트 수정, 2b 승인에 함께 올린다): `PerformanceOwnerMigrationTest` 가 "끝까지 적용한 현재 버전"을 `7` 로 단언한다 — V8 이 생기면 `8` 로 바뀐다.

## SC-02 (happy) 등록한 공연은 비공개로 저장되고, 응답이 그 상태를 알려 준다

- **Given** 주최자 `O1` 이 로그인해 있다
- **When** `O1` 이 `POST /api/performances` 로 공연을 등록한다(본문은 TASK-022 의 `R0` — openAt `T+7d`, closeAt `T+29d`, startAt `T+30d`)
- **Then** 201 이다
- **And** 응답 본문은 주최자 응답 키를 가지고 `published` 는 false, `editable` 은 true, `visibilityChangeable` 은 true, `status` 는 `UPCOMING` 이다
- **And** 응답에 `ownerId` 키가 없다
- **And** 저장된 행의 `published` 는 false 다(트랜잭션 밖에서 다시 읽는다)

covers: 마이그레이션 후 기존 행은 `published = true` 이고, 새로 등록한 공연은 `published = false` 다
flow: F1, F11

회귀(승인 테스트 수정): `PerformanceZoneRegistrationApiTest:173` 이 등록 직후 관객으로 `GET /api/performances/{id}` 를 불러 구역 응답을 확인한다 — 등록이 비공개가 되면 404 다. 주최자 단건 조회로 바꾸거나 publish 를 거친다.

## SC-03 (happy) 비공개 공연은 관객 조회에 나오지 않고, 공개된 공연은 나온다

- **Given** 공연 `D`(비공개·UPCOMING·openAt `T+1d`), `V`(공개·UPCOMING·openAt `T+2d`), `X`(공개·취소됨)가 있다
- **And** `D` 의 openAt 이 모든 공연 중 가장 이르다
- **When** 토큰 없이 `GET /api/performances`, `GET /api/performances/next-opening`, `GET /api/performances/{id}` 를 `D`·`V`·`X` 에 대해 호출한다
- **Then** 목록의 id 는 `V`·`X` 를 포함하고 `D` 를 포함하지 않으며 `totalElements` 도 `D` 를 센 값이 아니다
- **And** `?status=UPCOMING`·`?status=CANCELLED` 로 걸러도 `D` 는 나오지 않는다
- **And** `next-opening` 은 `V` 를 돌려준다(가장 이른 `D` 가 아니다)
- **And** `GET /api/performances/{D}` 는 404 `PERFORMANCE_NOT_FOUND` 다
- **And** `GET /api/performances/{V}` 는 200 이고, `GET /api/performances/{X}` 는 200 이며 `status` 는 `CANCELLED` 다
- **And** `V`·`X` 를 지워 `D` 만 남기면 `next-opening` 은 204 다

covers: 비공개 공연은 `GET /api/performances` 목록·`/next-opening` 에 나오지 않고 `GET /api/performances/{id}` 는 404 다
flow: F2

## SC-04 (happy) 주최자 목록은 요청자 소유 공연만 `start_at DESC, id DESC` 로 돌려준다

- **Given** `O1` 소유 `P1`(startAt `T+30d`), `P2`(startAt `T+10d`, 공개), `P3`(startAt `T-20d` — 이미 지난 공연, 비공개), `P4`(startAt `T+10d`, `P2` 와 같은 시각, 나중에 저장)가 있다
- **And** `O2` 소유 공연 `Q1`, 소유자가 NULL 인 기존 공연 `N1` 이 있다
- **When** `O1` 이 `GET /api/organizer/performances` 를 호출한다
- **Then** 200 이고 `items` 의 id 순서는 `P1`, `P4`, `P2`, `P3` 이다(같은 startAt 은 id 내림차순)
- **And** `Q1`·`N1` 은 없고, 지난 공연 `P3` 은 있다
- **And** `totalElements` 는 4, `page` 는 0, `size` 는 20 이다
- **And** 각 항목은 주최자 응답 키를 가지되 `sections` 키는 없다
- **And** `O2` 가 같은 요청을 하면 `Q1` 만 나온다
- **And** 소유 공연이 하나도 없는 주최자는 `items` 가 빈 배열, `totalElements` 는 0 이다

covers: `GET /api/organizer/performances` 는 요청자 소유 공연만 `start_at DESC` 로 돌려주고, 다른 주최자 공연·소유자 NULL 공연은 없다
flow: F3

## SC-05 (happy) `filter` 는 값마다 정해진 조건으로 걸러지고 공개 여부와 무관하다

- **Given** `T` 에 `O1` 소유 공연 아홉 개가 아래 표의 상태로 있다(startAt 은 모두 미래)
- **When** `O1` 이 `?filter=` 를 표의 값으로 바꿔 가며 `GET /api/organizer/performances` 를 호출한다
- **Then** 각 값의 결과 id 집합이 표와 같다

| 공연 | 공개 | 취소 | openAt | closeAt | 남은 좌석 | 상태 |
|---|---|---|---|---|---|---|
| `a` | 아니오 | 아니오 | T+7d | T+29d | 100 | UPCOMING |
| `b` | 예 | 아니오 | T+7d | T+29d | 100 | UPCOMING |
| `c` | 아니오 | 아니오 | T-1d | T+5d | 10 | OPEN |
| `d` | 예 | 아니오 | T-1d | T+5d | 10 | OPEN |
| `e` | 예 | 아니오 | T-1d | T+5d | 0 | SOLD_OUT |
| `f` | 예 | 아니오 | T-10d | T-1d | 10 | CLOSED |
| `g` | 예 | 예 | T+7d | T+29d | 100 | CANCELLED |
| `h` | 아니오 | 예 | T+7d | T+29d | 100 | CANCELLED |
| `i` | 예 | 아니오 | T-3d | T(`now == closeAt`) | 10 | OPEN |

| filter | 기대 id 집합 |
|---|---|
| (생략) | 전부 — a b c d e f g h i |
| `ALL` | 전부 — a b c d e f g h i |
| `DRAFT` | a c (비공개이고 취소 안 됨 — `h` 는 취소라 빠진다) |
| `UPCOMING` | a b (공개 여부 무관) |
| `OPEN` | c d i (`i` 는 `now == closeAt` 이라 아직 OPEN) |
| `SOLD_OUT` | e |
| `CLOSED` | f (`i` 는 `closeAt < now` 가 아니라 빠진다) |
| `CANCELLED` | g h |

- **And** `now` 를 `T+1ms` 로 올리고 `?filter=CLOSED` 를 다시 부르면 `i` 가 나오고 `?filter=OPEN` 에는 `i` 가 없다
- **And** 각 `filter` 의 `totalElements` 는 위 집합의 크기와 같다
- **And** `a` 와 `b` 의 응답 `status` 는 둘 다 `UPCOMING` 이고 `published` 만 다르다

covers: `filter=DRAFT` 는 취소되지 않은 비공개 공연만, `filter=UPCOMING|OPEN|SOLD_OUT|CLOSED|CANCELLED` 는 공개 여부와 무관하게 해당 상태만 돌려준다
flow: F3

## SC-06 (error) `filter` 가 정의 밖이면 400 이고, 빈 값은 전체로 본다

- **Given** `O1` 이 로그인해 있다
- **When** `O1` 이 아래 표의 `filter` 로 `GET /api/organizer/performances` 를 호출한다
- **Then** 상태가 표와 같다

| # | filter | 기대 |
|---|---|---|
| a | `NOPE` | 400 |
| b | `open`(소문자) | 400 |
| c | `Draft` | 400 |
| d | `filter=` (빈 값) | 200, `ALL` 과 같은 결과 — 관객 목록의 `?status=` 가 전체를 돌려주는 것과 같다(프로브 2026-11-13) |

covers: `filter=DRAFT` 는 취소되지 않은 비공개 공연만, `filter=UPCOMING|OPEN|SOLD_OUT|CLOSED|CANCELLED` 는 공개 여부와 무관하게 해당 상태만 돌려준다
flow: F3

## SC-07 (error) `page`·`size` 의 경계는 400 이거나 자른 값으로 응답한다

- **Given** `O1` 소유 공연 25개가 있다
- **When** `O1` 이 아래 표의 쿼리로 `GET /api/organizer/performances` 를 호출한다
- **Then** 상태와 응답이 표와 같다

| # | 쿼리 | 기대 |
|---|---|---|
| a | `page=-1` | 400 `INVALID_REQUEST` |
| b | `size=0` | 400 `INVALID_REQUEST` |
| c | `size=-1` | 400 `INVALID_REQUEST` |
| d | `page=0` | 200, `page` 0, `items` 20개 |
| e | `page=1` | 200, `items` 5개, `totalElements` 25 |
| f | `page=1000` | 200, `items` 빈 배열, `totalElements` 25 |
| g | `size=101` | 200, 응답 `size` 가 100 이고 `items` 25개 |
| h | `page=2147483647` | 400 `INVALID_REQUEST` (`page × size` 가 int 범위 초과) |
| i | `page=2147483648` | 400 (long 초과가 아니라 int 초과 — 타입 불일치) |
| j | `page=1.5` | 400 |
| k | `size=1.5` | 400 |
| l | `page=9223372036854775808` | 400 (long 초과) |
| m | `page=` 또는 `size=` (빈 값) | 200, 기본값 0·20 |
| n | `size=9223372036854775808` | 400 (long 초과 — 타입 불일치) |
| o | `size=2147483648` | 400 (int 초과 — 타입 불일치) |

- **And** 400 으로 끝난 요청이 공연 행을 바꾸지 않는다(공연 수 25 그대로)

covers: `GET /api/organizer/performances` 는 요청자 소유 공연만 `start_at DESC` 로 돌려주고, 다른 주최자 공연·소유자 NULL 공연은 없다
flow: F3

## SC-08 (happy) 주최자 단건 조회는 좌석 요약과 가능 여부 플래그를 담는다

- **Given** `O1` 소유 공연 `P1`(비공개, openAt `T+7d`)이 있고 구역 `R`(A~B열 열당 10석 55000), `S`(C열 열당 10석 33000)로 등록돼 있다
- **When** `O1` 이 `GET /api/organizer/performances/{P1}` 을 호출한다
- **Then** 200 이다
- **And** 응답은 주최자 응답 키를 가지며 `sections` 를 포함한다
- **And** `sections` 는 두 구역을 담고 각 `seatCount` 는 20·10, `price` 는 55000·33000 이다(관객 단건 조회의 `sections` 와 같은 형태·순서)
- **And** `published` 는 false, `editable` 은 true, `visibilityChangeable` 은 true 다
- **And** 같은 공연이 비공개인데도 관객 `GET /api/performances/{P1}` 은 404 다

covers: `GET /api/organizer/performances/{id}` 는 자기 공연이면 `sections`·`published`·`editable`·`visibilityChangeable` 을 담고, 남의 공연·없는 공연은 404 다
flow: F4

## SC-09 (error) 남의 공연·소유자 없는 공연·없는 공연은 같은 404 이고 DB 는 그대로다

- **Given** `O1` 소유 `P1`, `O2` 소유 `Q1`(비공개), 소유자 NULL 인 기존 공연 `N1`(공개)이 있다
- **And** 세 공연의 `published`·`cancelled`·title 을 읽어 두었다
- **When** `O1` 이 아래 표의 요청을 보낸다(`Q1`·`N1` 은 존재하는 id, `없음` 은 존재하지 않는 id)
- **Then** 모두 404 이고 본문은 규약의 404 본문이다(`<id>` 만 다르다)
- **And** 요청 뒤 `Q1`·`N1` 의 `published`·`cancelled`·title 이 읽어 둔 값 그대로다

| # | 요청 | 대상 |
|---|---|---|
| a | `GET /api/organizer/performances/{id}` | `Q1`, `N1`, 없음 |
| b | `POST /api/performances/{id}/publish` | `Q1`, `N1`, 없음 |
| c | `POST /api/performances/{id}/unpublish` | `Q1`, `N1`, 없음 |

- **And** 같은 `Q1` 에 대한 `O1` 의 응답 본문이 없는 id 에 대한 응답 본문과 형태(키·`code`)가 같다
- **And** `O1` 이 자기 공연 `P1` 을 같은 경로로 조회하면 200 이다(대조군)

covers: `GET /api/organizer/performances/{id}` 는 자기 공연이면 `sections`·`published`·`editable`·`visibilityChangeable` 을 담고, 남의 공연·없는 공연은 404 다
flow: F4, F5

## SC-10 (error) 신규 경로는 토큰이 없으면 401, 관객이면 403 이고 DB 는 그대로다

- **Given** `O1` 소유 비공개 공연 `P1`(openAt `T+7d`)이 있고 `published` 를 읽어 두었다
- **When** 아래 세 경로 각각에 아래 표의 `Authorization` 을 실어 보낸다(`{id}` 는 `P1`)
- **Then** 상태와 `code` 가 표와 같다
- **And** a·b 의 요청마다 `P1` 의 `published` 가 그대로 false 다

세 경로: `GET /api/organizer/performances`, `GET /api/organizer/performances/{id}`, `POST /api/performances/{id}/publish`

| # | Authorization | 기대 |
|---|---|---|
| a | 헤더 없음 | 401 `UNAUTHORIZED` |
| b | `Bearer <A1 토큰>` | 403 `FORBIDDEN_ROLE` (본문은 규약 표 그대로) |
| c | `Bearer <O1 토큰>` | 200 (대조군 — publish 는 200) |

- **And** `POST /api/performances/{id}/unpublish` 도 a·b 에서 같은 결과다

covers: `now < openAt` 인 자기 공연에 `publish` 를 보내면 `published = true` 로 DB 에 반영되고 관객 목록에 나타난다. `unpublish` 는 반대
flow: F3, F4, F5

## SC-11 (happy) 오픈 전 자기 공연은 공개·비공개 전환이 DB 와 관객 목록에 반영된다

- **Given** `O1` 소유 비공개 공연 `P1`(openAt `T+7d`)이 있다
- **And** 관객이 토큰 없이 `GET /api/performances` 를 불러 `P1` 이 없음을 확인했다
- **When** `O1` 이 `POST /api/performances/{P1}/publish` 를 호출한다
- **Then** 200 이고 응답 `published` 는 true, `status` 는 `UPCOMING`, `editable` 은 true, `visibilityChangeable` 은 true 다
- **And** 저장된 행의 `published` 는 true 다(트랜잭션 밖에서 다시 읽는다)
- **And** 관객 `GET /api/performances` 에 `P1` 이 나타나고 `GET /api/performances/{P1}` 은 200 이다
- **When** `O1` 이 `POST /api/performances/{P1}/unpublish` 를 호출한다
- **Then** 200 이고 응답 `published` 는 false 다
- **And** 저장된 행의 `published` 는 false 이고, 관객 목록에서 `P1` 이 사라지며 `GET /api/performances/{P1}` 은 404 다

covers: `now < openAt` 인 자기 공연에 `publish` 를 보내면 `published = true` 로 DB 에 반영되고 관객 목록에 나타난다. `unpublish` 는 반대
flow: F5

## SC-12 (happy) 이미 그 상태인 공연의 전환은 변경 없이 200 이다

- **Given** `O1` 소유 공개 공연 `P1`, 비공개 공연 `P2` 가 있다(둘 다 openAt `T+7d`, 취소 안 됨)
- **And** 두 공연의 행 값(`published`·title·시각)을 읽어 두었다
- **When** `O1` 이 `P1` 에 `publish`, `P2` 에 `unpublish` 를 호출한다
- **Then** 둘 다 200 이고 응답 `published` 는 각각 true·false 다
- **And** 두 행의 값이 읽어 둔 값 그대로다
- **And** 이 멱등은 `now < openAt` 에서만이다 — `now >= openAt` 에서는 SC-13 의 409 가 먼저다

covers: 이미 공개된 공연에 `publish` 를 보내면 200 이고 값이 그대로다
flow: F5

## SC-13 (error) 오픈 뒤에는 공개·비공개 전환이 409 `VISIBILITY_LOCKED` 이고 DB 는 그대로다

- **Given** `O1` 소유 공연의 openAt 이 `T` 이고, 시계를 아래 표의 시각으로 놓는다
- **When** 공연 상태별로 `publish`·`unpublish` 를 호출한다
- **Then** 결과가 표와 같다
- **And** 409 인 요청 뒤 행의 `published` 가 요청 전 값 그대로다

| # | now | 공연 | 요청 | 기대 |
|---|---|---|---|---|
| a | `T-1ms` | 비공개 | publish | 200 (잠기기 직전 — 대조군) |
| b | `T` (`now == openAt`) | 비공개 | publish | 409 `VISIBILITY_LOCKED` |
| c | `T` | 공개 | unpublish | 409 `VISIBILITY_LOCKED` |
| d | `T` | 공개 | publish | 409 `VISIBILITY_LOCKED` (이미 공개여도 멱등 200 이 아니다) |
| e | `T` | 비공개 | unpublish | 409 `VISIBILITY_LOCKED` |
| f | `T+1ms` | 공개 | unpublish | 409 `VISIBILITY_LOCKED` |
| g | `T+1ms` | 비공개 | publish | 409 `VISIBILITY_LOCKED` |

covers: `now >= openAt` 이면 `publish`·`unpublish` 가 409 `VISIBILITY_LOCKED` 이고 DB 가 바뀌지 않는다
flow: F5, F6

## SC-14 (error) 취소된 공연은 공개 전환·수정 모두 409 `PERFORMANCE_CANCELLED` 이고 DB 는 그대로다

- **Given** `O1` 소유 취소된 공연을 만든다(아래 표의 공개 여부·시각)
- **And** 행의 `published`·title·`cancelled` 를 읽어 두었다
- **When** 표의 요청을 보낸다(수정 본문은 TASK-022 의 `U0`)
- **Then** 결과가 표와 같다
- **And** 요청 뒤 행이 읽어 둔 값 그대로다

| # | 공개 | now 와 openAt | 요청 | 기대 |
|---|---|---|---|---|
| a | 아니오 | `now < openAt` | publish | 409 `PERFORMANCE_CANCELLED` |
| b | 예 | `now < openAt` | unpublish | 409 `PERFORMANCE_CANCELLED` |
| c | 아니오 | `now < openAt` | PUT | 409 `PERFORMANCE_CANCELLED` |
| d | 예 | `now < openAt` | PUT | 409 `PERFORMANCE_CANCELLED` |
| e | 아니오 | `now >= openAt` | PUT | 409 `PERFORMANCE_CANCELLED` (비공개는 시각 제한이 없지만 취소가 먼저다) |
| f | 예 | `now >= openAt` | publish | 409 `PERFORMANCE_CANCELLED` (`VISIBILITY_LOCKED` 가 아니다 — 검사 순서) |
| g | 예 | `now >= openAt` | PUT | 409 `PERFORMANCE_CANCELLED` (`REGISTRATION_ALREADY_OPEN` 이 아니다) |

- **And** 같은 공연의 `GET /api/organizer/performances/{id}` 는 200 이고 `status` 는 `CANCELLED`, `editable`·`visibilityChangeable` 은 모두 false 다

covers: 취소된 공연에 `publish`·`unpublish`·수정을 보내면 409 이고 DB 가 바뀌지 않는다
flow: F7

## SC-15 (happy) 수정은 비공개면 시각 제한이 없고 공개면 오픈 전에만 된다

- **Given** `O1` 소유 공연의 openAt 이 `T` 이고, 시계를 표의 시각으로 놓는다
- **When** `O1` 이 `PUT /api/performances/{id}` 로 title 을 `소리풍경 밴드 나이트 앙코르`(`U0`)로 바꾼다
- **Then** 결과가 표와 같다
- **And** 200 이면 저장된 행의 title 이 바뀌어 있고, 409 이면 title 이 그대로다(트랜잭션 밖에서 읽는다)

| # | 공개 | now | 기대 |
|---|---|---|---|
| a | 아니오 | `T-1ms` | 200, 반영 |
| b | 아니오 | `T` (`now == openAt`) | 200, 반영 |
| c | 아니오 | `T+1ms` | 200, 반영 |
| d | 예 | `T-1ms` | 200, 반영 |
| e | 예 | `T` | 409 `REGISTRATION_ALREADY_OPEN`, 그대로 |
| f | 예 | `T+1ms` | 409 `REGISTRATION_ALREADY_OPEN`, 그대로 |

- **And** 비공개이고 `now > openAt` 인 공연에 시간 순서가 어긋난 본문(closeAt 이 startAt 보다 뒤)을 보내면 400 `INVALID_TIME_ORDER` 이고 행은 그대로다(시각 제한을 풀어도 본문 검증은 남는다)
- **And** 같은 공연에 `O2` 가 PUT 하면 404 다(소유 검사는 그대로)

covers: 비공개 공연은 `now >= openAt` 이어도 수정이 200 이고 반영된다. 공개 공연은 기존처럼 409
flow: F8

## SC-16 (happy) `editable`·`visibilityChangeable` 은 같은 시각의 실제 요청 결과와 일치한다

- **Given** `O1` 소유 공연의 openAt 이 `T` 다
- **When** 아래 표의 모든 (공연 상태 × 시각) 조합에서 `GET /api/organizer/performances/{id}` 를 불러 플래그를 읽고, 같은 시각에 **새로 만든 같은 상태의 공연**에 실제 `PUT`(`U0`)·`publish`(공개 상태면 `unpublish`)를 보낸다
- **Then** 플래그가 표와 같고, `editable` 은 PUT 이 200 인지와, `visibilityChangeable` 은 전환 요청이 200 인지와 각각 일치한다
- **And** 목록 `GET /api/organizer/performances` 의 항목 플래그도 단건과 같다

| 공연 상태 | now | editable | visibilityChangeable |
|---|---|---|---|
| 비공개 | `T-1ms` | true | true |
| 비공개 | `T` | true | false |
| 비공개 | `T+1ms` | true | false |
| 공개 | `T-1ms` | true | true |
| 공개 | `T` | false | false |
| 공개 | `T+1ms` | false | false |
| 취소(비공개) | `T-1ms` | false | false |
| 취소(공개) | `T+1ms` | false | false |

covers: `editable`·`visibilityChangeable` 값이 같은 시각에 실제 수정·전환 요청의 성공 여부와 일치한다 (고정 `Clock` 으로 경계 시각 전후 확인)
flow: F9, F5

## SC-17 (error) 경로의 `id` 가 이상하면 404 또는 400 이다

- **Given** `O1` 이 로그인해 있고 `O1` 소유 공연이 하나 있다
- **When** 아래 표의 `{id}` 로 각 경로를 호출한다
- **Then** 상태가 표와 같다
- **And** 400 이 아닌 요청은 규약의 404 본문이다
- **And** 어느 요청도 공연 행을 바꾸지 않는다

경로: `GET /api/organizer/performances/{id}`, `POST /api/performances/{id}/publish`, `POST /api/performances/{id}/unpublish`, `PUT /api/performances/{id}`(본문 `U0`)

| # | `{id}` | 기대 | 근거 |
|---|---|---|---|
| a | `0` | 404 | 관객 단건이 `0` 에서 404 (프로브 2026-11-13) |
| b | `-1` | 404 | 같음 |
| c | `9223372036854775807` | 404 | long 최댓값 |
| d | `9223372036854775808` | 400 | long 초과 — 타입 불일치 |
| e | `1.5` | 400 | 소수 — 타입 불일치 |
| f | `abc` | 400 | 기존 회귀 표(`PerformanceErrorResponseRegressionTest`)와 같음 |

covers: `GET /api/organizer/performances/{id}` 는 자기 공연이면 `sections`·`published`·`editable`·`visibilityChangeable` 을 담고, 남의 공연·없는 공연은 404 다
flow: F4, F5, F6, F7, F8

## SC-18 (regression) 관객 응답에는 주최자 필드가 없다

- **Given** 공개 공연 `V`(소유자 `O1`, 구역 포함)와 취소된 공개 공연이 있다
- **When** 토큰 없이 `GET /api/performances`, `GET /api/performances/{V}`, `GET /api/performances/next-opening` 을 호출한다
- **Then** 목록 항목의 키 집합은 `id`·`title`·`venue`·`startAt`·`openAt`·`closeAt`·`totalSeats`·`availableSeats`·`status` 이고, 단건은 거기에 `sections` 를 더한 집합이다(`posterUrl` 은 값이 있을 때만)
- **And** 어느 응답 본문에도 `published`·`editable`·`visibilityChangeable`·`ownerId` 문자열이 없다
- **And** 목록 응답의 최상위 키는 `content`·`page`·`size`·`totalElements` 다

covers: 관객 응답(`PerformanceResponse`) JSON 에 `published`·`editable`·`visibilityChangeable`·`ownerId` 키가 없다
flow: F10

근거: 기존 승인 테스트 `PerformanceOwnershipApiTest`(키 집합 단언)·`PerformanceApiTest:109`(목록 최상위 키)가 이 값을 이미 단언한다 — 이 시나리오는 그 단언이 새 필드 추가 뒤에도 유지되는지를 지킨다.

## SC-19 (happy) 수정·취소 응답도 주최자 응답 형태이고 취소 뒤 플래그가 바뀐다

- **Given** `O1` 소유 비공개 공연 `P1`(openAt `T+7d`)이 있다
- **When** `O1` 이 `PUT /api/performances/{P1}`(`U0`)을 호출한다
- **Then** 200 이고 응답은 주최자 응답 키를 가지며 `published` 는 false, `editable` 은 true 다
- **When** `O1` 이 `POST /api/performances/{P1}/cancel` 을 호출한다
- **Then** 200 이고 응답 `status` 는 `CANCELLED`, `editable`·`visibilityChangeable` 은 false, `published` 는 false 다
- **And** 저장된 행의 `cancelled` 는 true 다
- **And** 응답에 `ownerId` 키가 없다

covers: (AC 밖 — 기능 문서 §3-6 이 정한 쓰기 응답 타입. PLAN F11)
flow: F11

회귀(승인 테스트 점검): 등록·수정·취소 응답의 키 집합을 단언하는 `PerformanceRegistrationApiTest`·`PerformanceErrorResponseRegressionTest` 의 행이 새 키(`published`·`editable`·`visibilityChangeable`)에 걸리는지 2b 에서 확인한다.

## SC-20 (error) 같은 공연의 쓰기가 겹치면 하나씩 처리돼 서로의 변경을 덮어쓰지 않는다

- **Given** `O1` 소유 공연 `P` 가 표의 상태(공개 여부)로 있다(openAt `T+7d`, f 행만 openAt `T`)
- **And** 다른 요청이 `P` 를 변경하는 중이다 — 진행 중인 쓰기를 흉내 내는 별도 트랜잭션이 `P` 의 행을 잠그고 표의 변경을 반영했지만 아직 끝내지 않았다(테스트가 JPA 표준 잠금으로 만든다)
- **When** 그 트랜잭션이 끝나기 전에 `O1` 이 표의 요청 `B` 를 보낸다(별도 스레드)
- **Then** `B` 는 그 트랜잭션이 끝나기 전에는 응답하지 않는다(300ms 뒤에도 끝나지 않았다)
- **And** 그 트랜잭션을 끝내면 `B` 가 응답하고, 상태와 `code` 가 표와 같다
- **And** 트랜잭션 밖에서 다시 읽은 `P` 의 `cancelled`·`published`·title 이 표의 최종 값과 같다

| # | P 의 공개 | 진행 중인 변경 | 요청 B | B 의 결과 | 최종 DB |
|---|---|---|---|---|---|
| a | 비공개 | 취소 | publish | 409 `PERFORMANCE_CANCELLED` | cancelled true, published false |
| b | 공개 | 취소 | unpublish | 409 `PERFORMANCE_CANCELLED` | cancelled true, published true (그대로) |
| c | 비공개 | 취소 | PUT(`U0`) | 409 `PERFORMANCE_CANCELLED` | cancelled true, title 그대로 |
| d | 비공개 | title 을 `다른 제목` 으로 변경 | publish | 200 | published true, title `다른 제목` (둘 다 반영) |
| e | 비공개 | 공개로 변경(published true) | PUT(`U0`) | 200 | published true, title `앙코르`(`U0` 의 title) (둘 다 반영) |
| f | 공개, openAt `T`, 시각 `T-1ms` 에 B 를 보낸 뒤 대기 중 `T` 로 옮김 | title 변경 | unpublish | 409 `VISIBILITY_LOCKED` (잠금을 얻은 뒤의 시각 `T == openAt` 으로 판정) | published true (그대로), title 변경 반영 |

covers: (AC 밖 — Phase 4 1회차 RC-1. PLAN F12)
flow: F12

근거: 잠금이 없으면 a~e 에서 B 가 읽은 옛 행으로 전 컬럼을 저장해 진행 중이던 변경(취소·title·공개)을 되돌린다. 잠금 대기 시간(H2 기본 2초)보다 짧게 쥔다.

## SC-21 (error) 관객 목록의 `page`·`size` 경계도 400 이거나 자른 값으로 응답한다

- **Given** 공개 공연 25개가 있다(openAt 모두 미래)
- **When** 토큰 없이 아래 표의 쿼리로 `GET /api/performances` 를 호출한다
- **Then** 상태와 응답이 표와 같다(주최자 목록 SC-07 과 같은 규칙)
- **And** 요청 뒤 공연 수는 25 그대로다

| # | 쿼리 | 기대 |
|---|---|---|
| a | `page=-1` | 400 `INVALID_REQUEST` |
| b | `size=0` | 400 `INVALID_REQUEST` |
| c | `size=-1` | 400 `INVALID_REQUEST` |
| d | `page=0` | 200, `page` 0, `content` 20개 |
| e | `page=1` | 200, `content` 5개, `totalElements` 25 |
| f | `page=1000` | 200, `content` 빈 배열, `totalElements` 25 |
| g | `size=101` | 200, 응답 `size` 가 100 이고 `content` 25개 |
| h | `page=2147483647` | 400 `INVALID_REQUEST` (`page × size` 가 int 범위 초과) |
| i | `page=2147483648` | 400 (int 초과 — 타입 불일치) |
| j | `page=1.5` | 400 |
| k | `size=1.5` | 400 |
| l | `page=9223372036854775808` | 400 (long 초과) |
| m | `page=` 또는 `size=` (빈 값) | 200, 기본값 0·20 |
| n | `size=9223372036854775808` | 400 (long 초과) |
| o | `size=2147483648` | 400 (int 초과) |

covers: (AC 밖 — Phase 4 1회차 RC-2. PLAN F13)
flow: F13

근거: a~c·h 는 2a 프로브(2026-11-13)가 처리되지 않은 예외(운영 500)로 확인한 기존 동작을 고치는 행이다. i~l·n·o·m·d~g 는 기존 응답 그대로(프로브 확인) — 회귀로 지킨다.

## 입력 제약 대조 (PLAN `design.inputs[].constraints`)

| 제약 | 단언하는 시나리오 |
|---|---|
| `/api/organizer/**`·`POST /api/performances/**` 는 ROLE_ORGANIZER — 없거나 무효 401, 관객 403 | SC-10 |
| `filter` 는 7개 값, 생략하면 ALL | SC-05, SC-06 |
| `page`·`size` 기본 0·20, size 는 100 으로 자른다 | SC-07, SC-21(관객 목록) |
| 같은 공연의 쓰기는 한 번에 하나씩, 시각은 잠근 뒤 | SC-20 |
| `id` 는 요청자 소유여야 한다 — 아니면 같은 404 | SC-09, SC-17 |
| publish·unpublish 는 `now < openAt` 에서만 | SC-11, SC-12, SC-13 |
| 수정은 공개 공연에서만 `now < openAt` | SC-15 |
| `published` 는 NOT NULL, 기존 행 TRUE, 새 등록 FALSE | SC-01, SC-02 |
