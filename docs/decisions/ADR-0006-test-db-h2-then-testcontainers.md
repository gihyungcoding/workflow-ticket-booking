# ADR-0006: 테스트 DB는 당분간 H2 인메모리를 쓰고, 좌석 선점 착수 전 Testcontainers로 전환한다

- **상태**: 채택됨
- **날짜**: 2026-09-03
- **관련**: TASK-001, [ADR-0004](ADR-0004-seat-hold-concurrency.md), `docs/product/features/performance-availability.md`

---

## 맥락

TASK-001(공연 목록/상세 조회 API)의 테스트를 붙이려면 백엔드 테스트가 실제로 붙을
DB가 필요하다. 프로덕션은 PostgreSQL([ADR-0003](ADR-0003-layered-architecture-and-stack.md))
이지만, 로컬 개발 환경에는 아직 Docker가 준비되어 있지 않아 Testcontainers로
PostgreSQL을 띄울 수 없다.

동시에 이 프로젝트에는 이미 PostgreSQL 전용 SQL에 의존하는 결정이 하나 있다 —
[ADR-0004](ADR-0004-seat-hold-concurrency.md)의 좌석 선점 동시성 제어는
`ON CONFLICT ... DO UPDATE ... WHERE` 조건부 UPSERT를 쓴다. H2는 이 구문을
지원하지 않으므로, 좌석 선점 기능이 시작되면 H2로는 그 기능을 검증할 수 없다.
반면 TASK-001은 단순 조회(`SELECT` 필터링·페이지네이션)만 다루고 표준 SQL 범위를
벗어나지 않는다.

즉 "지금 당장 필요한 것"과 "곧 필요해질 것"이 다르다 — 이 결정은 그 사이에서
언제·왜 전환하는지를 명확히 해 둔다.

## 검토한 선택지

### A. 처음부터 Testcontainers로 PostgreSQL을 띄워 테스트한다

- 장점: 프로덕션과 동일한 DB로 테스트하므로 PostgreSQL 전용 문법(향후 `ON CONFLICT`,
  윈도우 함수, JSONB 등)이 항상 검증된다. Flyway 마이그레이션도 테스트 실행마다
  실제로 적용되어 검증된다. 나중에 다시 전환할 필요가 없다.
- 단점: 로컬에 Docker 인프라가 아직 준비되어 있지 않다. 지금 이 인프라를 갖추는
  데 드는 시간이, TASK-001처럼 표준 SQL만 쓰는 조회 기능을 검증하는 데는 과한
  비용이다 — 검증하려는 대상이 요구하지 않는 정합성까지 미리 사는 셈이다.

### B. H2 인메모리로 테스트한다 (PostgreSQL 모드)

- 장점: 인메모리라 Docker 없이 즉시 실행되고 테스트 속도가 빠르다. 설정 비용이
  거의 없다(`MODE=PostgreSQL` 호환 모드로 표준 SQL 차이도 최소화). TASK-001의
  범위(단순 조회)에는 충분하다.
- 단점: PostgreSQL 전용 문법을 테스트할 수 없다 — H2가 통과시킨 쿼리가 운영에서
  깨질 수 있다. Flyway 마이그레이션(`spring.flyway.enabled=false`)도 테스트에서
  검증되지 않는다. 이 간극을 방치하면 좌석 선점 기능([ADR-0004](ADR-0004-seat-hold-concurrency.md))
  의 Red 테스트 자체를 쓸 수 없다.

## 결정

**B를 지금 단계에 채택하되, 영구 전략이 아니라 시한부 전략으로 채택한다.**
당분간 테스트 DB는 H2 인메모리(PostgreSQL 호환 모드)를 쓴다. 다음 트리거 중
하나라도 해당하면 **그 즉시** Testcontainers(PostgreSQL) 로 전환한다:

- 좌석 선점 기능에 착수할 때
- `ON CONFLICT`, 윈도우 함수, JSONB 등 PostgreSQL 전용 기능을 쓰는 코드가 생길 때
- Flyway 마이그레이션 자체를 테스트로 검증해야 할 때

### 이유

A(Testcontainers 우선)를 고르지 않은 이유는 TASK-001이 요구하지 않는 정합성을
지금 인프라 비용을 들여 미리 사는 것이기 때문이다 — 표준 SQL만 쓰는 조회 기능에
Docker 기반 실제 DB를 강제할 근거가 없다. 반대로 B(H2)를 영구 전략으로 두지 않는
이유는 [ADR-0004](ADR-0004-seat-hold-concurrency.md)가 이미 PostgreSQL 전용
구문에 의존하기로 확정되어 있어서다 — H2로는 그 결정을 TDD Red 단계에서부터
검증할 수 없고, 이는 `CLAUDE.md` 절대 규칙 6(테스트를 먼저 실패시킨다)과 충돌한다.
그래서 "지금은 H2, 좌석 선점 전 Testcontainers"라는 phased 전략으로 결정했다 —
전환 시점을 트리거로 명시해 판단을 미루지 않게 한다.

## 결과

- 좋아지는 것: TASK-001을 Docker 인프라 준비를 기다리지 않고 바로 시작할 수 있다.
  테스트 실행이 인메모리라 빠르다.
- 감수하는 것: 지금 작성하는 테스트는 PostgreSQL 전용 문법이나 Flyway 마이그레이션
  자체를 검증하지 않는다 — H2를 통과한 쿼리가 운영 PostgreSQL에서 깨질 가능성이
  남아있다. 이 간극은 전환 전까지 구조적으로 존재한다.
- 되돌리려면: 테스트 DB 설정이 `backend/src/test/resources/application.properties`
  한 곳에 모여 있으므로, 전환 시점에는 이 파일과 `build.gradle`의 테스트
  의존성(H2 → Testcontainers)만 바꾸면 된다. 트리거가 늦게 발견될수록(예: 좌석
  선점 기능을 H2인 채로 시작해버리면) 되돌리는 비용이 커진다 — 착수 시점에
  전환하는 것이 가장 싸다.

## 검사 가능한 제약

- 없음 — 전환 트리거(좌석 선점 착수 등)는 기능 단위 판단이라 정적 검사로 걸기
  어렵다. 좌석 선점 기능(Phase 1)에서 H2가 여전히 쓰이고 있다면 그 시점에
  `constraints.yaml`에 "테스트가 PostgreSQL 전용 구문을 요구하면 H2 금지" 같은
  제약 추가를 검토한다.
