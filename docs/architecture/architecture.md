# 아키텍처

**최종 갱신**: 2026-11-25

---

## 1. 전체 구조

```
  ┌──────────┐     ┌──────────┐     ┌──────────────┐     ┌────────────┐
  │  React   │────►│  API     │────►│   Service    │────►│ PostgreSQL │
  │ (frontend)│     │(Controller)│   └──────┬───────┘     └────────────┘
  └──────────┘     └──────────┘            │
                                     ┌──────▼───────┐     ┌────────────┐
                                     │  Repository  │────►│   Redis    │
                                     └──────────────┘     │ (대기열 전용) │
                                                          └────────────┘
```

백엔드는 Spring(Java) 단일 서비스, 프론트엔드는 React SPA. Redis 는 **대기열 순번과
입장권에만** 쓴다 — 캐시나 좌석 선점 계층으로 쓰지 않는다 (ADR-0023).

## 2. 계층

| 계층 | 책임 | 하지 않는 것 | 위치 |
|---|---|---|---|
| API (Controller) | HTTP 요청 파싱, 응답 변환, 인증 컨텍스트 전달 | 비즈니스 로직, DB 접근 | `backend/.../api` |
| Service | 도메인 로직, 트랜잭션 경계, 좌석 선점 규칙 | HTTP 관심사, SQL/영속성 프레임워크 타입 직접 사용(쿼리 조립 포함) | `backend/.../service` |
| domain | 도메인 모델(엔티티, 값 객체, 상태를 나타내는 enum) | 영속성 접근, HTTP 관심사, 트랜잭션 제어 — 영속 여부와 무관하게 "핵심 개념을 나타내는가"로 판단한다 | `backend/.../domain` |
| Repository | 영속성 접근, 쿼리(조회 조건 조립 포함) | 도메인 규칙 판단 | `backend/.../repository` |
| Frontend (React) | 화면 렌더링, 사용자 입력, API 호출 | 도메인 규칙(선점 유효성 등은 서버가 최종 판단) | `frontend/` |

API/Service/Repository는 요청이 흐르는 순서고, domain은 그 위에 별도로 있는
계층이 아니라 Service와 Repository가 공유하는 모델이다 — 그래서 §1 흐름
다이어그램에는 없다. 응답 DTO(`PerformanceResponse` 등)는 domain이 아니라
그 값을 만드는 계층(Service)이 소유한다 — Controller가 반환 타입으로
가져다 쓰는 것은 허용된 방향(API→Service)이라 문제없지만, DTO를
`api/dto`에 두면 Service가 그 타입을 만들기 위해 `api` 패키지를 import해야
해서 아래 의존 방향을 어긴다.

## 3. 의존 방향

<!--
  어느 방향의 호출이 허용되고 금지되는가.
  여기서 정한 것 중 검사 가능한 것을 constraints.yaml 로 옮긴다.
-->

```
허용:  API ──► Service ──► Repository ──► DB
금지:  API ──╳─► Repository        (트랜잭션 경계가 흐려진다)
       Service ──╳─► API           (역방향 의존)
```

## 4. 데이터 흐름

**읽기 — 공연 조회/좌석 현황**
`React → API(GET) → Service → Repository → PostgreSQL` 조회 후 그대로 반환. 캐시 없음.

**읽기 — 좌석 조회 (REQ-002)**
`React → API(GET /api/performances/{id}/seats) → Service → Repository`. 로그인한 관객만 —
비로그인은 401 (§4-2 매처 순서). 좌석별 `available`(선점·판매를 구분하지 않는다)과 등급별 남은 석수만
돌려준다. `available` 은 `seat_lock` 에 유효한 잠금(`now < expires_at` 또는 `expires_at IS NULL`)이 없는 좌석이다.

**쓰기 — 좌석 선점·해제 (REQ-002)**
`React → API(POST /api/seat-holds, 본문 performanceId·seatIds) → Service → Repository`.
`seat_lock.seat_id` 유니크 제약 + `INSERT … SELECT … ON CONFLICT (seat_id) DO UPDATE … WHERE
seat_lock.expires_at <= :now … RETURNING` **한 문장**으로 1~4석을 전부-아니면-없음으로 잡는다 —
`seatIds` 오름차순, 반환 수가 요청 수보다 적으면 롤백하고 빠진 좌석 id 를 `SEAT_UNAVAILABLE`(409)에 싣는다.
사용자당 활성 선점은 1개 — 새 선점이 이전 미예약 선점을 대체한다. 해제(`DELETE /api/seat-holds/{holdId}`)는
본인·미예약 hold 만 지우고, 이미 예약된 hold 는 204 no-op 이다.
별도 락 계층(분산 락, 인메모리 큐)은 두지 않는다 — ADR-0004, §6, §7 참고.

**읽기 — 서버 기준 시각**
`React → API(GET /api/time)`. 프론트는 왕복 시간 절반을 보정한 오프셋으로 카운트다운을
그린다. 오픈 여부 판정은 서버의 주입된 `Clock` 으로만 한다 (ADR-0005, ADR-0024).

**쓰기 — 대기열 진입·순번 조회**
`React → API → Service → QueueStore(Repository) → Redis`. 계정당 대기 슬롯 1개.
순번 조회는 DB 를 거치지 않는다. 입장권(TTL)을 받은 관객만 좌석 선택 API 를 호출할 수
있다 (ADR-0023).

**쓰기 — 무통장 예약 (REQ-002)**
`React → API(POST /api/bookings, 본문 holdId) → Service → Repository`. 한 트랜잭션에서
`booking`·`booking_seat` 행을 만들고 잠금을 입금 기한까지 늘린다 — `UPDATE seat_lock SET expires_at = :deadline
WHERE hold_id = :h AND 본인 AND expires_at > :now`, 영향 행 수 = 좌석 수일 때만 성공. 입금 기한은 예약 당일
23:59:00 Asia/Seoul 의 `Instant`. 공연별 「입금 대기 + 확정」 합계 1인 4석을 넘으면 거절한다. 같은 hold 로 다시
부르면 기존 예약을 200 으로 돌려준다(`booking.hold_id` UNIQUE). 예매번호는 DB 시퀀스로 만든다.

**쓰기 — 입금 확인 (REQ-002)**
`운영자(ADMIN) → API(POST /api/admin/bookings/by-number/{bookingNumber}/confirm-deposit) → Service → Repository`.
화면 없는 내부 API 다(ADR-0025). `/api/admin/**` 는 `hasRole(ADMIN)` 이고, 확정 시점에 DB 에서 역할을 한 번 더
확인한다. 한 트랜잭션 조건부 쓰기 `UPDATE … SET expires_at = NULL WHERE hold_id AND expires_at > :now`, 영향 행 수 =
좌석 수일 때만 확정하고 `booking.confirmed_by`·`confirmed_at` 을 남긴다. 기한 뒤면 409 `DEPOSIT_DEADLINE_PASSED`
만 돌려주고 쓰지 않는다. 이미 확정이면 200 멱등 (ADR-0036).

**만료 — 저장하지 않고 판정한다 (REQ-002)**
선점 7분 만료와 입금 기한 경과 취소는 스케줄러가 아니라 `seat_lock.expires_at` 과 주입된 `Clock` 으로 읽기·쓰기
시점에 판정한다. 유효 = `now < expires_at`. 예매 상태(`CONFIRMED`·`PENDING_DEPOSIT`·`CANCELLED`)는 컬럼이 아니라
만료 시각에서 파생한다 (ADR-0037, ADR-0005). 선점 7분·입금 기한 23:59(Asia/Seoul)·최대 4석은 설정 한 곳에 둔다.

**읽기 — 공연의 남은 좌석 수 (REQ-002, D40)**
남은 좌석 = 전체 좌석 − 유효한 잠금(만료 전 선점·입금 대기 예약, 입금 확인된 좌석). 저장값이 아니라 조회 시점에
`seat_lock` 에서 계산한다 (ADR-0037 결정 5). 공연 상태(`PerformanceStatusRules` 의 OPEN/SOLD_OUT), 공개 목록
매진 필터(`PerformanceRepositoryImpl`), 주최자 응답(`OrganizerPerformanceResponse`)이 모두 이 계산을 쓴다.
`performance.available_seats` 저장 컬럼은 더 쓰지 않는다 — 컬럼을 지울지 남길지는 PLAN 에서 정한다.

**쓰기 — 공연 등록/관리**
`React → API(POST/PUT) → Service → Repository`. 역할(주최자) 확인은 `SecurityConfig` 의
경로 규칙에서, 소유권 확인은 Service 계층에서 한다. 소유권 위반은 404 (ADR-0029).

### 4-1. 데이터 모델 — 좌석 잠금과 예매 (REQ-002, `V10__create_bookings.sql`)

```
  seat ◄──── seat_lock                      booking ◄──── booking_seat ────► seat
             ├ seat_id   UNIQUE             ├ booking_number UNIQUE (DB 시퀀스)
             ├ hold_id                      ├ hold_id        UNIQUE  ──► seat_lock.hold_id
             ├ (선점한 사용자)                ├ (예매자·공연·금액·입금 기한)
             └ expires_at                   ├ confirmed_by   (ADMIN 사용자)
                 > now   : 선점 또는 입금 대기 └ confirmed_at
                 NULL    : 판매 확정
                 <= now  : 만료 — 다음 선점이 덮어쓴다
```

`seat_lock` 은 좌석당 한 행이고 「지금 누가 이 좌석을 막고 있는가」만 답한다. `booking`·`booking_seat` 는
「누가 무엇을 예약했는가」의 기록이라 잠금이 만료·덮어써져도 남는다. 예매 상태는 `booking.hold_id` 로 찾은
잠금의 `expires_at` 에서 파생한다.

### 4-2. 보안 경로 규칙 (`SecurityConfig`, 위에서 아래로 처음 맞는 규칙)

| 순서 | 매처 | 규칙 | 근거 |
|---|---|---|---|
| 1 | `GET /api/performances/*/seats` | `authenticated` — **아래 permitAll 보다 먼저** | D30·D34, AC-11 |
| 2 | `GET /api/performances/**` | `permitAll` | 기존 |
| 3 | `/api/performances/**`, `/api/organizer/**` | `hasRole(ORGANIZER)` | ADR-0029 |
| 4 | `/api/admin/**` | `hasRole(ADMIN)` | ADR-0036 |
| — | `/api/seat-holds/**`, `/api/bookings/**` | `anyRequest().authenticated()` 에 떨어진다 — 선점은 `/api/performances/**` 밑에 두지 않는다(ORGANIZER 매처를 피한다) | D24 |

역할 거절은 기존 `FORBIDDEN_ROLE` 403, 문구는 역할 중립. `ADMIN` 은 가입 API(`SignUpRequest`·
`CompleteSocialSignUpRequest`)에서 400 으로 거절하고 DB 로만 지정한다 (ADR-0036).

## 5. 외부 의존

| 대상 | 용도 | 실패하면 | 대안 |
|---|---|---|---|
| PostgreSQL | 모든 영속 데이터, 좌석 선점 정합성 | 서비스 전체 중단 | 없음 |
| Redis | 대기열 순번·입장권 (AOF 로 재시작에도 유지) | 대기열 진입·순번 조회 중단. 조회·선점·예매 관리는 동작 | 없음 — 대기열 저장소를 `QueueStore` 인터페이스 뒤에 둔다 (ADR-0023) |
| 메일 발송(SMTP) | 이메일 인증·비밀번호 재설정·변경 알림 메일 | 인증·재설정 메일이 나가지 않는다. 가입·소셜 로그인·이미 인증된 계정의 로그인은 동작하고, 인증 전 이메일 로그인과 비밀번호 찾기가 막힌다 | 없음 — `MailGateway` 인터페이스 뒤에 두어 발송 서비스를 바꾼다 (ADR-0031) |

## 6. 이 구조를 고른 이유

- UI 라이브러리(MUI) → [ADR-0002](../decisions/ADR-0002-ui-library-mui.md)
- 단일 서비스, 레이어드 아키텍처, Java/Spring + React + PostgreSQL 스택 →
  [ADR-0003](../decisions/ADR-0003-layered-architecture-and-stack.md)
- 좌석 선점 동시성 제어(DB 트랜잭션만으로 처리, 규모 가정과 재검토 트리거) →
  [ADR-0004](../decisions/ADR-0004-seat-hold-concurrency.md)
- 테스트 DB 전략(당분간 H2, 좌석 선점 착수 전 Testcontainers 전환) →
  [ADR-0006](../decisions/ADR-0006-test-db-h2-then-testcontainers.md)
- 프론트엔드 테스트 프레임워크(Vitest + Testing Library) →
  [ADR-0007](../decisions/ADR-0007-frontend-test-framework-vitest.md)
- 클라이언트 라우팅(react-router-dom) →
  [ADR-0008](../decisions/ADR-0008-client-routing-react-router-dom.md)
- 좌석은 구역(등급)×행×열 입력으로 서버가 개별 `seat` 행을 생성한다 (좌표 기반
  인터랙티브 배치는 하지 않음) →
  [ADR-0009](../decisions/ADR-0009-seat-individual-rows.md)
- 요청 DTO의 필드 형태 검증은 Bean Validation, 도메인 규칙(시각 순서·좌석 상한 등)은
  계속 Service 계층 수동 검증 →
  [ADR-0010](../decisions/ADR-0010-bean-validation-for-request-dtos.md)
- 인증 방식으로 세션이 아닌 JWT(Bearer 토큰) 채택 →
  [ADR-0012](../decisions/ADR-0012-jwt-based-authentication.md)
- Spring Security 전체 도입(OAuth2 Client + 커스텀 JWT 인증 필터) →
  [ADR-0013](../decisions/ADR-0013-spring-security-adoption.md)
- 소셜 계정 자동 연결 정책(이메일 일치 + 검증된 경우에만) →
  [ADR-0014](../decisions/ADR-0014-social-account-auto-link-policy.md)
- accessToken 저장 위치로 프론트 localStorage 채택 →
  [ADR-0015](../decisions/ADR-0015-access-token-localstorage.md)
- 소셜 로그인 토큰은 URL fragment 로 프론트에 넘긴다 →
  [ADR-0021](../decisions/ADR-0021-social-login-token-via-url-fragment.md)

- 디자인 정본을 `out/` 시안으로 교체 →
  [ADR-0022](../decisions/ADR-0022-design-source-from-out-mockups.md)
- 대기열은 Redis, 좌석 선점은 DB 유지 →
  [ADR-0023](../decisions/ADR-0023-queue-in-redis-seat-hold-in-db.md)
- 카운트다운은 서버 시각 오프셋, 오픈 판정은 서버 Clock →
  [ADR-0024](../decisions/ADR-0024-countdown-by-server-clock-offset.md)
- 결제는 무통장 입금만, 입금 확인은 내부 API →
  [ADR-0025](../decisions/ADR-0025-bank-transfer-only-with-internal-confirm-api.md)
- 시간 제한 화면은 기한에 재조회 없이 클라이언트가 전환, 판정은 다음 요청에서 서버 →
  [ADR-0026](../decisions/ADR-0026-client-timer-transition-server-judgment.md)
- 표시 문자열의 「공백 아님」은 유니코드 공백·NBSP·U+200B 를 공백으로 보는 한 판정 →
  [ADR-0027](../decisions/ADR-0027-display-string-blank-definition.md)
- 사용자 입력 URL 은 https, 권한 부분을 직접 나눠 하위 요소마다 판정 →
  [ADR-0028](../decisions/ADR-0028-user-url-validation-by-authority-parts.md)
- 쓰기 가드 제거, API 쓰기는 JWT 역할(`SecurityConfig`) + 소유권(Service, 위반은 404)으로만 보호 →
  [ADR-0029](../decisions/ADR-0029-replace-write-guard-with-role-and-ownership.md)
- 프론트 요청 토큰은 앱에 하나뿐인 AuthProvider 의 세션에서 읽는다(저장소는 세션 전·Provider 밖에서만) →
  [ADR-0030](../decisions/ADR-0030-request-token-from-single-auth-session.md)
- 계정 메일은 `MailGateway` 인터페이스 뒤에서 SMTP 로, 개발·테스트는 로그로 →
  [ADR-0031](../decisions/ADR-0031-account-mail-via-mail-gateway-smtp.md)
- 이메일 링크 토큰은 해시로 저장하고 링크는 fragment, 확인은 POST →
  [ADR-0032](../decisions/ADR-0032-email-link-token-hashed-fragment-post.md)
- 이메일(비밀번호) 로그인은 이메일 소유 확인 뒤에만 →
  [ADR-0033](../decisions/ADR-0033-require-email-ownership-before-password-login.md)
- 입금 확인 호출자는 최소 ADMIN 역할(`/api/admin/**`), ADMIN 은 DB 로만 →
  [ADR-0036](../decisions/ADR-0036-deposit-confirm-caller-minimal-admin-role.md)
- 선점·예약 만료는 만료 시각을 저장하고 조회·쓰기 시점에 판정, 스케줄러 없음 →
  [ADR-0037](../decisions/ADR-0037-judge-hold-and-booking-expiry-at-read-time.md)

## 7. 알려진 부채

| 무엇 | 왜 이대로 두는가 | 언제 다시 볼 것인가 |
|---|---|---|
| 좌석 선점을 DB 트랜잭션(유니크 제약 + 조건부 UPSERT)만으로 처리, 메모리 선점 계층 없음 | 대기열(ADR-0023)이 좌석 선택 단계의 동시 인원을 입장권 발급 속도로 제한하므로 DB 만으로 감당할 규모가 된다 | 입장권 발급 속도를 낮춰도 선점 API p99 > 1초면 메모리 선점 계층 검토 (ADR-0004 두 번째 단계) |
| Redis 단일 노드 | 대기열 전용이고 AOF 로 재시작은 견딘다 | 메모리 사용 70% 초과 또는 노드 장애가 관측되면 |
| 만료·취소된 `seat_lock` 행을 지우지 않는다 (ADR-0037) | 다음 선점이 덮어쓰므로 좌석 수만큼만 쌓인다. 정리 작업이 필요한 완료 조건이 없다 | 만료 알림(메일·문자)이 범위에 들어오거나, `booking` 조회가 잠금 조인 때문에 느려지면 |
| 모든 선점·예약 쿼리가 `expires_at` 조건을 빠짐없이 가져야 한다 (ADR-0037) | 정적 검사로 잡기 어렵다 — 경계 테스트로만 지킨다 | 조건 누락 버그가 한 번이라도 나오면 조회 전용 뷰나 리포지토리 공통 조건으로 모은다 |
| ADMIN 역할 회수가 토큰 만료까지 늦다 — 입금 확인만 DB 에서 재확인한다 (ADR-0036) | ADMIN 이 쓰는 API 가 하나뿐이다 | `/api/admin/**` 에 두 번째 API 가 생기면 |
| 대기열 없이 좌석 선택 API 가 열린다 — 입장권(ADR-0023) 검사가 없다 | REQ-002 가 대기열을 후속 요청으로 미뤘다 | 대기열 요청을 시작할 때 |
| 선점·동시성 테스트만 PostgreSQL(Testcontainers), 나머지는 H2 (ADR-0006) | 기존 H2 테스트를 그대로 둔다 (REQ-002 D29) | H2 와 PostgreSQL 동작 차이로 테스트가 어긋나면 |
| 공연의 남은 좌석 수를 조회마다 `seat_lock` 집계로 계산한다 — 공개 목록은 공연마다 집계가 붙는다 (ADR-0037, D40) | 선점·만료와 같은 기준으로 매진 표시가 바로 맞고, 만료는 쓰기 사건이 아니라 저장값으로는 맞출 수 없다 | 공연 목록 응답이 느려지면 집계 쿼리·인덱스부터. 저장 컬럼 `performance.available_seats` 의 처리는 TASK-034 PLAN 에서 |
