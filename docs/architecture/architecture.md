# 아키텍처

**최종 갱신**: 2026-11-04

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

**쓰기 — 좌석 선점**
`React → API(POST /seats/{id}/hold) → Service → Repository`. 좌석-예매 매핑 테이블의
유니크 제약(좌석당 활성 선점 1건) 과 조건부 UPSERT 로 동시 요청 중 하나만 성공시킨다.
별도 락 계층(분산 락, 인메모리 큐)은 두지 않는다 — §6, §7 참고.

**읽기 — 서버 기준 시각**
`React → API(GET /api/time)`. 프론트는 왕복 시간 절반을 보정한 오프셋으로 카운트다운을
그린다. 오픈 여부 판정은 서버의 주입된 `Clock` 으로만 한다 (ADR-0005, ADR-0024).

**쓰기 — 대기열 진입·순번 조회**
`React → API → Service → QueueStore(Repository) → Redis`. 계정당 대기 슬롯 1개.
순번 조회는 DB 를 거치지 않는다. 입장권(TTL)을 받은 관객만 좌석 선택 API 를 호출할 수
있다 (ADR-0023).

**쓰기 — 예매·입금 확인**
무통장 입금 예약은 선점과 같은 트랜잭션 경계에서 예매 행을 만든다. 입금 확인은
내부 API 로 받는다 (ADR-0025). 보호 수단은 공유 키가 아니라 운영자 역할 + JWT 다 —
방식은 그 API 를 만드는 기능에서 정하고, 정하기 전에는 열지 않는다 (ADR-0029).

**쓰기 — 공연 등록/관리**
`React → API(POST/PUT) → Service → Repository`. 역할(주최자) 확인은 `SecurityConfig` 의
경로 규칙에서, 소유권 확인은 Service 계층에서 한다. 소유권 위반은 404 (ADR-0029).

## 5. 외부 의존

| 대상 | 용도 | 실패하면 | 대안 |
|---|---|---|---|
| PostgreSQL | 모든 영속 데이터, 좌석 선점 정합성 | 서비스 전체 중단 | 없음 |
| Redis | 대기열 순번·입장권 (AOF 로 재시작에도 유지) | 대기열 진입·순번 조회 중단. 조회·선점·예매 관리는 동작 | 없음 — 대기열 저장소를 `QueueStore` 인터페이스 뒤에 둔다 (ADR-0023) |

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

## 7. 알려진 부채

| 무엇 | 왜 이대로 두는가 | 언제 다시 볼 것인가 |
|---|---|---|
| 좌석 선점을 DB 트랜잭션(유니크 제약 + 조건부 UPSERT)만으로 처리, 메모리 선점 계층 없음 | 대기열(ADR-0023)이 좌석 선택 단계의 동시 인원을 입장권 발급 속도로 제한하므로 DB 만으로 감당할 규모가 된다 | 입장권 발급 속도를 낮춰도 선점 API p99 > 1초면 메모리 선점 계층 검토 (ADR-0004 두 번째 단계) |
| Redis 단일 노드 | 대기열 전용이고 AOF 로 재시작은 견딘다 | 메모리 사용 70% 초과 또는 노드 장애가 관측되면 |
