# TASK-019 시나리오

대상:
- `TimeController`(`GET /api/time`), `SecurityConfig`
- `PerformanceController`: 등록 시 `posterUrl`·`zones` 를 받고, `GET /next-opening` 을 연다
- `PerformanceService`: 포스터·구역 검증, 구역 저장, 상세 sections 병합, `getNextOpening`
- `PerformanceRepositoryImpl.findNextOpening`
- `PerformanceExceptionHandler`
- `V6__add_poster_and_zones.sql`

**시각은 테스트가 고정한다.** 아래의 `T` 는 `MutableClock` 에 넣은 시각 `2026-11-11T08:00:00Z` 다
(`ClockTestConfig`, ADR-0005).

**DB 를 쓰는 방식은 두 가지다.**
- SC-14 만 PostgreSQL(Testcontainers) 위에서 Flyway 로 돈다.
- 나머지는 기존 H2 테스트 구성 그대로 돈다.
- 거부 경로에서 "행이 생기지 않는다"를 확인할 때는 클래스 레벨 `@Transactional` 없이
  리포지토리 `count()` 를 새로 읽는다.

## 규약 (정본)

시나리오는 이 값을 그대로 단언한다. 값을 바꾸려면 여기부터 고친다.

| 항목 | 값 |
|---|---|
| 기본 등록 요청 `R0` | title `루미너스 콘서트`, venue `KSPO DOME`, openAt `T+7d`, closeAt `T+29d`, startAt `T+30d`, sections `S0` |
| `S0` (등록 순서) | `2층` C열 10석 77000 · `F1` A열 10석 150000 · `1층` B열 10석 99000 |
| `S1` (등록 순서) | `2층` C열 10석 77000 · `F2` B열 10석 120000 · `F1` A열 10석 150000 · `1층` D열 10석 99000 |
| 서버 시각 응답 | 200 `{"now": "<ISO-8601 UTC, Z 로 끝남>"}`, 헤더 `Cache-Control` 값이 정확히 `no-store` |
| posterUrl 규칙 | 스킴이 `https`(대소문자 무관)이고 호스트가 있는 절대 URI(RFC 3986 문법을 따르되 호스트·경로의 비 ASCII 글자는 받는다). 호스트는 권한 부분에서 userinfo(`…@`)와 포트(`:…`)를 뗀 것이다 — 밑줄·비 ASCII 글자 호스트는 받는다(경로의 비 ASCII 와 같게). 호스트에 `%XX` 가 있거나, `@` 앞 userinfo 가 비었거나, `:` 뒤 포트가 비었으면 거부한다. IPv6(`[…]`)·userinfo·포트가 갖춰진 주소는 받는다. 제어문자·짝 없는 서로게이트는 거부한다. UTF-16 코드 유닛 500 이하. 빈 문자열·공백도 거부한다. 변환 없이 받은 그대로 저장한다 |
| 구역 문자열 | `area` 는 필수, 공백 아님, 30자 이하. `note` 는 선택이지만 주면 공백이 아니고 60자 이하. 둘 다 제어문자(`\p{Cc}`)·짝 없는 서로게이트를 거부한다(ADR-0019). 변환 없이 저장한다 |
| grades | 필수, 빈 배열 아님, 원소는 공백 아님. sections 의 grade 와 **정확히 같은 문자열**이어야 한다(대소문자·공백 구분) |
| stageOrder | `zones` 를 펼친 순서(묶음 → 묶음 안 grades)로 1, 2, … 를 매기고, `zones` 에 없는 등급이 sections 에 처음 나온 순서대로 그 뒤에 붙는다. 그렇게 붙은 등급은 `area` = 등급 이름이고 `areaNote` 는 없다. `zones` 가 없거나 null 이거나 `[]` 이면 모든 등급이 이렇게 된다 |
| 상세 sections 정렬 | 구역 행이 있으면 `stageOrder` 오름차순이고, 같은 stageOrder 안에서는 price 내림차순이다. 구역 행이 없는 기존 공연은 기존 순서(price 내림차순, grade 오름차순)를 그대로 두고 그 순서로 stageOrder 1..n 을 매긴다 |
| areaNote 없음 | 키를 생략한다 |
| posterUrl 없음 | 키를 생략한다(목록·상세·등록·next-opening 모두) |
| 오류 우선순위 | Bean Validation·역직렬화(`INVALID_REQUEST`) → 기존 서비스 검증 → `INVALID_POSTER_URL` → 구역 검증. 구역 검증은 펼친 grades 를 순서대로 보고 처음 걸린 위반(`ZONE_GRADE_UNKNOWN` 또는 `ZONE_GRADE_DUPLICATED`)을 낸다 |
| next-opening 후보 | 취소되지 않았고, `openAt > now`, `startAt >= now` 인 공연. `openAt` 이 가장 이른 것을 고르고, 같으면 id 가 작은 것을 고른다. 응답은 200 `PerformanceResponse`(sections 키 없음)이고, 후보가 없으면 204 에 빈 본문 |

## SC-01 (happy) 서버 시각은 인증 없이 주입된 시각을 캐시 금지로 돌려준다

- **Given** 시계가 아래 표의 시각을 가리킨다
- **When** Authorization 헤더 없이 `GET /api/time` 을 호출한다
- **Then** 200 이다
- **And** 본문의 `now` 문자열이 `Z` 로 끝나고, `Instant` 로 파싱하면 표의 시각과 같다
- **And** 본문 키는 `now` 하나뿐이다
- **And** 응답 헤더 `Cache-Control` 의 값은 정확히 `no-store` 다 (`no-cache, no-store, max-age=0, must-revalidate` 처럼 다른 지시가 섞이지 않는다)

| # | 시계 |
|---|---|
| a | `2026-11-11T08:55:28.123Z` |
| b | `2026-11-11T09:00:00Z` (같은 테스트 컨텍스트에서 a 다음에 — 앞 값이 남지 않는다) |

covers: `GET /api/time` 이 인증 없이 200 과 주입된 `Clock` 의 시각을 `now`(ISO-8601 UTC)로 돌려주고, 응답에 `Cache-Control: no-store` 가 있다
flow: F1

## SC-02 (happy) 포스터·구역 없이 등록하면 등록 순서대로 기본 구역이 매겨진다

- **Given** 공연·좌석·구역 행이 없다
- **When** 아래 표의 본문으로 `POST /api/performances` 를 호출한다
- **Then** 201 이고 응답 본문 키는 정확히 `id, title, venue, startAt, openAt, closeAt, totalSeats, availableSeats, status` 다 (`posterUrl` 키가 없다)
- **And** 그 id 로 `GET /api/performances/{id}` 를 호출하면 `posterUrl` 키가 없다
- **And** 상세 `sections` 는 순서대로 `2층`(stageOrder 1, area `2층`) · `F1`(2, `F1`) · `1층`(3, `1층`) 이다. 기존 정렬이었다면 `F1`·`1층`·`2층` 순이었을 것이다
- **And** 각 section 의 키는 정확히 `grade, price, seatCount, area, stageOrder` 다 (`areaNote` 키가 없다)
- **And** 구역 테이블을 새로 읽으면 그 공연의 행이 3개다

| 본문 | 
|---|
| `R0` (posterUrl·zones 키 없음) |
| `R0` + `"posterUrl": null, "zones": null` |
| `R0` + `"zones": []` |

covers: `posterUrl`·`zones` 없이 등록하면 지금과 같이 201 이고, 상세 `sections` 는 등록 순서대로 `stageOrder` 1, 2, … 와 `area` = 등급 이름을 갖는다
flow: F2

## SC-03 (happy) 구역을 주면 펼친 순서로 정렬되고 빠진 등급은 뒤에 붙는다

- **Given** 공연·좌석·구역 행이 없다
- **When** `R0` 의 sections 를 `S1` 로 바꾸고 `"posterUrl": "https://cdn.example.com/luminous.jpg"` 와 `"zones": [{"area": "플로어", "note": "무대 앞 바닥, F1이 가장 앞", "grades": ["F1", "F2"]}, {"area": "2층 지정석", "grades": ["2층"]}]` 를 더해 `POST /api/performances` 를 호출한다
- **Then** 201 이고 응답 `posterUrl` 이 `https://cdn.example.com/luminous.jpg` 다
- **And** 상세 `sections` 는 아래 표의 순서·값이다. sections 의 등록 순서(2층·F2·F1·1층)가 아니라 grades 순서(F1·F2)를 따른다
- **And** `areaNote` 가 없다고 적힌 행에는 `areaNote` 키가 없다
- **And** 상세 `posterUrl` 이 `https://cdn.example.com/luminous.jpg` 다

| 순서 | grade | price | seatCount | area | areaNote | stageOrder |
|---|---|---|---|---|---|---|
| 1 | F1 | 150000 | 10 | 플로어 | 무대 앞 바닥, F1이 가장 앞 | 1 |
| 2 | F2 | 120000 | 10 | 플로어 | 무대 앞 바닥, F1이 가장 앞 | 2 |
| 3 | 2층 | 77000 | 10 | 2층 지정석 | (없음) | 3 |
| 4 | 1층 | 99000 | 10 | 1층 | (없음) | 4 |

covers: `zones` 를 주고 등록하면 상세 `sections` 가 `zones` 를 펼친 순서의 `stageOrder` 로 정렬되고 `area`·`areaNote` 를 갖는다. `zones` 에 없는 등급은 그 뒤에 붙는다
flow: F3

## SC-04 (boundary) 같은 등급이 두 구역 행이면 구역은 하나이고 그 안에서는 가격순이다

- **Given** 공연·좌석·구역 행이 없다
- **When** `R0` 의 sections 를 `R` A열 10석 80000 · `VIP` B열 10석 120000 · `R` C열 10석 70000 (이 순서)로 바꿔 `POST /api/performances` 를 호출한다
- **Then** 201 이다
- **And** 상세 `sections` 는 순서대로 `R` 80000(stageOrder 1) · `R` 70000(1) · `VIP` 120000(2) 이고, 모두 area 가 자기 grade 다
- **And** 구역 테이블을 새로 읽으면 그 공연의 행이 2개(`R`, `VIP`)다

covers: `posterUrl`·`zones` 없이 등록하면 지금과 같이 201 이고, 상세 `sections` 는 등록 순서대로 `stageOrder` 1, 2, … 와 `area` = 등급 이름을 갖는다
flow: F2

## SC-05 (error) 포스터 주소가 https 절대 URL 이 아니면 INVALID_POSTER_URL

- **Given** 공연·좌석·구역 행이 없다
- **When** `R0` 에 아래 표의 `posterUrl` 을 더해 `POST /api/performances` 를 호출한다
- **Then** 400 이고 `code` 는 `INVALID_POSTER_URL` 이다
- **And** 공연·좌석·구역 테이블을 새로 읽으면 모두 0행이다

| # | posterUrl | 무엇 |
|---|---|---|
| a | `http://cdn.example.com/a.jpg` | http |
| b | `/posters/a.jpg` | 상대 경로 |
| c | `cdn.example.com/a.jpg` | 스킴 없음 |
| d | `https://cdn.example.com/`(24자) + `a` × 477 (총 501자) | 최대 + 1 |
| e | `""` | 빈 값 |
| f | `"   "` | 공백만 |
| g | `https:///a.jpg` | 호스트 없음 |
| h | `ftp://cdn.example.com/a.jpg` | 다른 스킴 |
| i | `javascript:alert(1)` | 스크립트 스킴 |
| j | ` https://cdn.example.com/a.jpg` (앞 공백 1개) | 앞뒤 공백 — 걷어내지 않는다 |
| k | `https://cdn.example.com/a b.jpg` | URI 로 파싱되지 않는 문자 |
| l | `https://cdn.example.com/a` + U+0000 + `.jpg` | NUL |
| m | `https://cdn.example.com/a` + U+D800 + `.jpg` | 짝 없는 서로게이트 |
| n | `https://cdn.example.com/`(24자) + `🎤` × 238 + `a` (총 501 코드 유닛) | 최대 + 1 — 코드 유닛으로 센다 |
| o | `https://ex%41mple.com/a.jpg` | 호스트의 퍼센트 인코딩 |
| p | `https://@cdn.example.com/a.jpg` | 빈 userinfo |
| q | `https://cdn.example.com:/a.jpg` | 빈 포트 |
| r | `https://:8443/a.jpg` | 호스트 없이 포트만 |
| s | `https://cdn.example.com:abc/a.jpg` | 숫자가 아닌 포트 |
| t | `https://[::1/a.jpg` | 닫히지 않은 IPv6 |

covers: `posterUrl` 이 `http://`·상대 경로·501자면 400 `INVALID_POSTER_URL`, `zones` 에 없는 등급이면 400 `ZONE_GRADE_UNKNOWN`, 중복 등급이면 400 `ZONE_GRADE_DUPLICATED` 이고 공연·좌석·구역 행이 생기지 않는다
flow: F4

## SC-06 (boundary) 상한 안의 값과 표기 변형은 받은 그대로 저장된다

- **Given** 공연·좌석·구역 행이 없다
- **When** `R0` 에 아래 표의 필드를 더해 `POST /api/performances` 를 호출한다 (구역 행은 `"zones": [{"area": <area>, "note": <note>, "grades": ["F1"]}]`, 표에 없는 값은 area `플로어`·note 없음)
- **Then** 201 이다
- **And** 상세에서 `posterUrl`, `F1` section 의 `area`·`areaNote` 가 보낸 문자열과 정확히 같다 (공백·대소문자를 바꾸지 않는다)
- **And** note 를 보내지 않은 행은 `F1` section 에 `areaNote` 키가 없다

| # | 필드 | 값 | 무엇 |
|---|---|---|---|
| a | posterUrl | `https://cdn.example.com/`(24자) + `a` × 476 (총 500자) | 정확히 상한 |
| b | posterUrl | `HTTPS://CDN.EXAMPLE.COM/A.JPG` | 스킴 대문자 — 받는다, 바꾸지 않는다 |
| c | posterUrl | `https://cdn.example.com/포스터.jpg` | 비 ASCII 경로 |
| d | area | `가` × 30 | 정확히 상한 |
| e | note | `나` × 60 | 정확히 상한 |
| f | area | ` 플로어 ` (앞뒤 공백) | 걷어내지 않는다 |
| g | area · note | `플로어 🎤` · `무대 🎤 앞` | 짝이 맞는 서로게이트(이모지)는 받는다 |
| h | note | ` 무대 앞 ` (앞뒤 공백) | 걷어내지 않는다 |
| i | area | `🎤` × 15 (30 코드 유닛) | 이모지로 정확히 상한 — 코드 유닛으로 센다 |
| j | posterUrl | `https://cdn_1.example.com/a.jpg` | 호스트에 밑줄 |
| k | posterUrl | `https://포스터.example.com/a.jpg` | 비 ASCII 호스트 |
| l | posterUrl | `https://[::1]:8443/a.jpg` | IPv6 호스트 + 포트 |
| m | posterUrl | `https://user@cdn.example.com:8443/a.jpg` | userinfo + 포트 |

covers: `zones` 를 주고 등록하면 상세 `sections` 가 `zones` 를 펼친 순서의 `stageOrder` 로 정렬되고 `area`·`areaNote` 를 갖는다. `zones` 에 없는 등급은 그 뒤에 붙는다
flow: F3

## SC-07 (error) 포스터·구역 필드의 JSON 타입이 다르면 INVALID_REQUEST

- **Given** 공연·좌석·구역 행이 없다
- **When** `R0` 에 아래 표의 필드를 더해 `POST /api/performances` 를 호출한다
- **Then** 400 이고 `code` 는 `INVALID_REQUEST` 다 (숫자·불리언을 문자열로 바꿔 받아 201 이 되지 않는다)
- **And** 공연·좌석·구역 테이블을 새로 읽으면 모두 0행이다

| # | 필드 | 값 |
|---|---|---|
| a | posterUrl | `123` |
| b | posterUrl | `true` |
| c | posterUrl | `{}` |
| d | posterUrl | `["https://cdn.example.com/a.jpg"]` |
| e | zones | `"플로어"` |
| f | zones | `{}` |
| g | zones | `1` |
| h | zones | `[{"area": 1, "grades": ["F1"]}]` |
| i | zones | `[{"area": "플로어", "note": true, "grades": ["F1"]}]` |
| j | zones | `[{"area": "플로어", "grades": "F1"}]` |
| k | zones | `[{"area": "플로어", "grades": [1]}]` (sections 의 `F1` 을 등급 `1` 로 바꾼 요청에서도) |
| l | zones | `["플로어"]` |

covers: `posterUrl` 이 `http://`·상대 경로·501자면 400 `INVALID_POSTER_URL`, `zones` 에 없는 등급이면 400 `ZONE_GRADE_UNKNOWN`, 중복 등급이면 400 `ZONE_GRADE_DUPLICATED` 이고 공연·좌석·구역 행이 생기지 않는다
flow: F4

## SC-08 (error) 구역 원소의 형태가 틀리면 INVALID_REQUEST

- **Given** 공연·좌석·구역 행이 없다
- **When** `R0` 에 아래 표의 `zones` 를 더해 `POST /api/performances` 를 호출한다 (`{…}` 는 `{"area": "플로어", "grades": ["F1"]}` 에서 표의 칸만 바꾼 것)
- **Then** 400 이고 `code` 는 `INVALID_REQUEST` 다
- **And** 공연·좌석·구역 테이블을 새로 읽으면 모두 0행이다

| # | zones | 무엇 |
|---|---|---|
| a | `[null]` | null 원소 |
| b | `[{"grades": ["F1"]}]` | area 누락 |
| c | `[{area: null}]` | area null |
| d | `[{area: ""}]` | area 빈 값 |
| e | `[{area: "   "}]` | area 공백만 |
| f | `[{area: "가" × 31}]` | area 최대 + 1 |
| f2 | `[{area: "🎤" × 15 + "가"}]` (16 코드포인트, 31 코드 유닛) | area 최대 + 1 — 코드 유닛으로 센다 |
| g | `[{area: "플로" + U+0000 + "어"}]` | area NUL |
| h | `[{area: "플로" + U+D800}]` | area 짝 없는 서로게이트 |
| i | `[{note: ""}]` | note 빈 값 |
| j | `[{note: "   "}]` | note 공백만 |
| k | `[{note: "나" × 61}]` | note 최대 + 1 |
| k2 | `[{note: "🎤" × 30 + "나"}]` (61 코드 유닛) | note 최대 + 1 — 코드 유닛으로 센다 |
| l | `[{note: "무대" + U+0000}]` | note NUL |
| m | `[{note: "무대" + U+DC00}]` | note 짝 없는 서로게이트 |
| n | `[{"area": "플로어"}]` | grades 누락 |
| o | `[{grades: null}]` | grades null |
| p | `[{grades: []}]` | grades 빈 배열 |
| q | `[{grades: [null]}]` | grades null 원소 |
| r | `[{grades: ["  "]}]` | grades 원소 공백만 |

covers: `posterUrl` 이 `http://`·상대 경로·501자면 400 `INVALID_POSTER_URL`, `zones` 에 없는 등급이면 400 `ZONE_GRADE_UNKNOWN`, 중복 등급이면 400 `ZONE_GRADE_DUPLICATED` 이고 공연·좌석·구역 행이 생기지 않는다
flow: F4

## SC-09 (error) 구역의 등급이 sections 에 없거나 겹치면 등급 오류

- **Given** 공연·좌석·구역 행이 없다
- **When** `R0`(sections `S0`: `2층`·`F1`·`1층`)에 아래 표의 `zones` 를 더해 `POST /api/performances` 를 호출한다 (각 묶음의 area 는 `플로어`, `A`, `B` 순)
- **Then** 400 이고 `code` 는 표의 값이다
- **And** 공연·좌석·구역 테이블을 새로 읽으면 모두 0행이다

| # | zones 의 grades (묶음별) | code | 무엇 |
|---|---|---|---|
| a | `["VIP"]` | ZONE_GRADE_UNKNOWN | 없는 등급 |
| b | `["f1"]` | ZONE_GRADE_UNKNOWN | 대소문자 — 같다고 보지 않는다 |
| c | `["F1 "]` | ZONE_GRADE_UNKNOWN | 끝 공백 — 걷어내지 않는다 |
| d | `["F1" + U+0000]` | ZONE_GRADE_UNKNOWN | NUL |
| e | `["F1" + U+D800]` | ZONE_GRADE_UNKNOWN | 짝 없는 서로게이트 |
| f | `["F1", "F1"]` | ZONE_GRADE_DUPLICATED | 한 묶음 안 중복 |
| g | `["F1"]`, `["F1"]` | ZONE_GRADE_DUPLICATED | 묶음 사이 중복 |
| h | `["F1", "2층", "1층", "F1"]` | ZONE_GRADE_DUPLICATED | 등급 수 + 1 개 |
| i | `["F1"]`, `["2층"]`, `["1층"]`, `["F1"]` | ZONE_GRADE_DUPLICATED | 묶음 수가 등급 수 + 1 |
| j | `["F1", "VIP"]`, `["F1"]` | ZONE_GRADE_UNKNOWN | 펼친 순서에서 미지 등급이 먼저 |
| k | `["F1", "F1"]`, `["VIP"]` | ZONE_GRADE_DUPLICATED | 펼친 순서에서 중복이 먼저 |
| l | `["VIP"]` + `"posterUrl": "http://a.jpg"` | INVALID_POSTER_URL | 포스터 검증이 구역보다 먼저 |
| m | `["VIP"]` + closeAt `T+31d`(startAt 뒤) | INVALID_TIME_ORDER | 기존 검증이 먼저 |

covers: `posterUrl` 이 `http://`·상대 경로·501자면 400 `INVALID_POSTER_URL`, `zones` 에 없는 등급이면 400 `ZONE_GRADE_UNKNOWN`, 중복 등급이면 400 `ZONE_GRADE_DUPLICATED` 이고 공연·좌석·구역 행이 생기지 않는다
flow: F4

## SC-10 (happy) 목록·상세에 포스터가 있고 없는 공연은 키가 없다

- **Given** 시계는 `T` 다
- **And** 공연 A 는 posterUrl `https://cdn.example.com/a.jpg` 로 등록돼 있다
- **And** 공연 B 는 posterUrl 없이 등록돼 있다 (둘 다 `R0` 의 시각)
- **When** `GET /api/performances` 를 호출한다
- **Then** 200 이고 `content` 에서 A 의 `posterUrl` 은 `https://cdn.example.com/a.jpg` 다
- **And** B 항목에는 `posterUrl` 키가 없다
- **And** `GET /api/performances/{A}` 의 `posterUrl` 은 `https://cdn.example.com/a.jpg` 이고, `GET /api/performances/{B}` 에는 `posterUrl` 키가 없다

covers: 목록·상세 응답에 `posterUrl` 이 있고, 없는 공연은 키가 없다
flow: F6

## SC-11 (happy) 머리글 후보는 아직 열리지 않은 공연 중 가장 이른 오픈이다

- **Given** 시계는 `T` 다
- **And** 아래 표의 공연이 저장돼 있다 (P2 는 posterUrl `https://cdn.example.com/p2.jpg`)
- **When** 인증 없이 `GET /api/performances/next-opening` 을 호출한다
- **Then** 200 이고 `id` 는 P2, `status` 는 `UPCOMING`, `posterUrl` 은 `https://cdn.example.com/p2.jpg` 다
- **And** 응답에 `sections` 키가 없다

| 공연 | 취소 | openAt | closeAt | startAt | 무엇 |
|---|---|---|---|---|---|
| P1 | 아니오 | T+2h | T+5d | T+6d | 열리지 않음, 더 늦음 |
| P2 | 아니오 | T+1h | T+5d | T+6d | **정답** |
| P3 | 예 | T+30m | T+5d | T+6d | 취소 — 더 이르지만 제외 |
| P4 | 아니오 | T−1h | T+5d | T+6d | 이미 열림 — 제외 |
| P5 | 아니오 | T−3d | T−2d | T−1d | 지난 공연 — 제외 |

covers: `next-opening` 이 아직 열리지 않은 공연 중 `openAt` 이 가장 이른 1건을 돌려준다. 취소된 공연·이미 열린 공연·지난 공연은 고르지 않고, 후보가 없으면 204
flow: F5

## SC-12 (boundary) 머리글 후보의 시각 경계와 같은 오픈 시각

- **Given** 시계는 `T` 다
- **And** 아래 표의 행마다 그 공연만 저장돼 있다 (저장소로 직접 넣는다 — f·g 는 등록 API 가 막는 시각 조합이다)
- **When** `GET /api/performances/next-opening` 을 호출한다
- **Then** 표의 결과다 (204 면 본문이 비어 있다)

| # | 공연 (openAt / closeAt / startAt) | 결과 | 무엇 |
|---|---|---|---|
| a | Q: T / T+5d / T+6d | 204 | openAt == now 는 이미 열림 |
| b | Q: T+1ms / T+5d / T+6d | 200 Q | now 바로 뒤 |
| c | Q: T, R: T+1h (둘 다 closeAt T+5d / startAt T+6d) | 200 R | 같은 시각의 열린 공연이 더 이른 후보로 끼지 않는다 |
| d | X(id 작음)·Y(id 큼) 둘 다 T+1h / T+5d / T+6d | 200 X | 같은 openAt 이면 id 작은 것 |
| e | Y(id 큼): T+1h, X(id 작음): T+2h (같은 closeAt·startAt) | 200 Y | id 가 아니라 openAt 이 먼저 |
| f | Q: T+1h / T+1h / T (startAt == now) | 200 Q | startAt == now 는 지나지 않았다 |
| g | Q: T+1h / T+1h / T−1ms | 204 | startAt < now 는 지난 공연 |

covers: `next-opening` 이 아직 열리지 않은 공연 중 `openAt` 이 가장 이른 1건을 돌려준다. 취소된 공연·이미 열린 공연·지난 공연은 고르지 않고, 후보가 없으면 204
flow: F5

## SC-13 (error) 후보가 없으면 204 이고 본문이 비어 있다

- **Given** 시계는 `T` 다
- **And** 아래 표의 상태다
- **When** `GET /api/performances/next-opening` 을 호출한다
- **Then** 204 이고 본문 길이가 0 이다

| # | 저장된 공연 |
|---|---|
| a | 없음 |
| b | SC-11 의 P3·P4·P5 만 |

covers: `next-opening` 이 아직 열리지 않은 공연 중 `openAt` 이 가장 이른 1건을 돌려준다. 취소된 공연·이미 열린 공연·지난 공연은 고르지 않고, 후보가 없으면 204
flow: F5

## SC-14 (regression) V6 는 기존 데이터가 있는 PostgreSQL 에 적용되고 기존 행이 그대로다

- **Given** PostgreSQL 에 Flyway 를 V5 까지 적용했다
- **And** performance 1행(취소 아님, `R0` 의 시각, total/available 20)과 그 공연의 seat 20행(`R` 80000 10행, 그다음 `VIP` 120000 10행)을 SQL 로 넣었다
- **When** Flyway 를 끝(V6)까지 적용한다
- **Then** 적용이 성공하고 현재 버전이 6 이다
- **And** 그 performance 행의 `poster_url` 은 NULL 이고, 기존 행의 다른 열(title·venue·시각·좌석 수)은 그대로다
- **And** `performance_zone` 은 0행이다

covers: V6 마이그레이션이 기존 데이터가 있는 DB 에 적용되고 기존 공연의 목록·상세 응답이 바뀌지 않는다(`posterUrl` 없음, `sections` 에 기본 구역값만 추가)
flow: F7

## SC-15 (regression) 구역 행이 없는 기존 공연의 응답은 기본 구역값만 더해진다

- **Given** 시계는 `T` 다
- **And** 공연 L 과 좌석을 저장소로 직접 넣었다: `R` 80000 10석, `VIP` 120000 10석, `S` 80000 5석 (이 순서). posterUrl 은 없고 구역 행도 없다 (V6 이전에 등록된 공연)
- **When** `GET /api/performances/{L}` 을 호출한다
- **Then** 200 이고 `posterUrl` 키가 없다
- **And** `sections` 는 기존 순서(price 내림차순 → grade 오름차순)대로 `VIP` 120000(stageOrder 1) · `R` 80000(2) · `S` 80000(3) 이다
- **And** 각 section 의 `area` 는 자기 grade 이고, 키는 정확히 `grade, price, seatCount, area, stageOrder` 다
- **And** `GET /api/performances` 의 L 항목 키는 정확히 `id, title, venue, startAt, openAt, closeAt, totalSeats, availableSeats, status` 다

covers: V6 마이그레이션이 기존 데이터가 있는 DB 에 적용되고 기존 공연의 목록·상세 응답이 바뀌지 않는다(`posterUrl` 없음, `sections` 에 기본 구역값만 추가)
flow: F7

## SC-16 (regression) V6 가 만든 구역 테이블은 잘못된 행을 받지 않는다

- **Given** SC-14 처럼 V5 까지의 데이터 위에 V6 까지 적용했다
- **And** 그 공연에 `(grade 'R', area 'R', stage_order 1)` 한 행이 들어가 있다 (이 INSERT 는 성공한다)
- **When** 아래 표의 행을 넣는다 — h 는 공연 행을 고친다 (행마다 새 트랜잭션)
- **Then** 각각 제약 위반으로 실패하고, `performance_zone` 은 1행 그대로다

| # | 넣는 행 | 걸리는 제약 |
|---|---|---|
| a | grade `R`, area `x`, stage_order 2 | UNIQUE (performance_id, grade) |
| b | grade `VIP`, area `x`, stage_order 1 | UNIQUE (performance_id, stage_order) |
| c | grade `VIP`, area `x`, stage_order 0 | CHECK stage_order > 0 |
| d | grade `VIP`, area NULL, stage_order 2 | area NOT NULL |
| e | performance_id 999(없음), grade `VIP`, area `x`, stage_order 1 | FK performance(id) |
| f | grade `VIP`, area `가` × 31, stage_order 2 | area VARCHAR(30) |
| g | grade `VIP`, area `x`, area_note `나` × 61, stage_order 2 | area_note VARCHAR(60) |
| h | performance.poster_url 을 `a` × 501 로 UPDATE | poster_url VARCHAR(500) |

covers: V6 마이그레이션이 기존 데이터가 있는 DB 에 적용되고 기존 공연의 목록·상세 응답이 바뀌지 않는다(`posterUrl` 없음, `sections` 에 기본 구역값만 추가)
flow: F7

## 입력 경계 칸

| 칸 | 판정 | 근거 |
|---|---|---|
| posterUrl@F4:json_field.wrong_type | covered | SC-07 |
| posterUrl@F4:string.empty | covered | SC-05 |
| posterUrl@F4:string.blank | covered | SC-05 |
| posterUrl@F4:string.max+1 | covered | SC-05 |
| posterUrl@F4:string_stored.nul | covered | SC-05 |
| posterUrl@F4:string_stored.surrogate | covered | SC-05 |
| posterUrl@F4:string_stored.normalized | covered | SC-05, SC-06 |
| posterUrl@F4:converted.raw_vs_converted | covered | SC-05, SC-06 |
| posterUrl@F4:converted.real_entry | covered | SC-05 |
| zones@F4:json_field.wrong_type | covered | SC-07 |
| zones@F4:array.empty | covered | SC-02 |
| zones@F4:array.null_element | covered | SC-08 |
| zones@F4:array.max+1 | covered | SC-09 |
| zones[].area@F4:required.missing | covered | SC-08 |
| zones[].area@F4:required.null | covered | SC-08 |
| zones[].area@F4:json_field.wrong_type | covered | SC-07 |
| zones[].area@F4:string.empty | covered | SC-08 |
| zones[].area@F4:string.blank | covered | SC-08 |
| zones[].area@F4:string.max+1 | covered | SC-08 |
| zones[].area@F4:string_stored.nul | covered | SC-08 |
| zones[].area@F4:string_stored.surrogate | covered | SC-08, SC-06 |
| zones[].area@F4:string_stored.normalized | covered | SC-06 |
| zones[].note@F4:json_field.wrong_type | covered | SC-07 |
| zones[].note@F4:string.empty | covered | SC-08 |
| zones[].note@F4:string.blank | covered | SC-08 |
| zones[].note@F4:string.max+1 | covered | SC-08 |
| zones[].note@F4:string_stored.nul | covered | SC-08 |
| zones[].note@F4:string_stored.surrogate | covered | SC-08, SC-06 |
| zones[].note@F4:string_stored.normalized | covered | SC-06 |
| zones[].grades@F4:required.missing | covered | SC-08 |
| zones[].grades@F4:required.null | covered | SC-08 |
| zones[].grades@F4:json_field.wrong_type | covered | SC-07 |
| zones[].grades@F4:array.empty | covered | SC-08 |
| zones[].grades@F4:array.null_element | covered | SC-08 |
| zones[].grades@F4:array.max+1 | covered | SC-09 |
| zones[].grades@F4:string_stored.nul | covered | SC-09 |
| zones[].grades@F4:string_stored.surrogate | covered | SC-09 |
| zones[].grades@F4:string_stored.normalized | covered | SC-09 |
| openAt@F5:datetime.now_eq_boundary | covered | SC-12 |
| startAt@F5:datetime.now_eq_boundary | covered | SC-12 |
