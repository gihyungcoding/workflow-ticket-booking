# 아키텍처

**최종 갱신**: 2026-09-02

---

## 1. 전체 구조

```
  ┌──────────┐     ┌──────────┐     ┌──────────────┐     ┌────────────┐
  │  React   │────►│  API     │────►│   Service    │────►│ PostgreSQL │
  │ (frontend)│     │(Controller)│   └──────┬───────┘     └────────────┘
  └──────────┘     └──────────┘            │
                                     ┌──────▼───────┐
                                     │  Repository  │
                                     └──────────────┘
```

백엔드는 Spring(Java) 단일 서비스, 프론트엔드는 React SPA. 별도 캐시·큐 계층은
두지 않는다 (§6 참고).

## 2. 계층

| 계층 | 책임 | 하지 않는 것 | 위치 |
|---|---|---|---|
| API (Controller) | HTTP 요청 파싱, 응답 변환, 인증 컨텍스트 전달 | 비즈니스 로직, DB 접근 | `backend/.../api` |
| Service | 도메인 로직, 트랜잭션 경계, 좌석 선점 규칙 | HTTP 관심사, SQL 직접 작성 | `backend/.../service` |
| Repository | 영속성 접근, 쿼리 | 도메인 규칙 판단 | `backend/.../repository` |
| Frontend (React) | 화면 렌더링, 사용자 입력, API 호출 | 도메인 규칙(선점 유효성 등은 서버가 최종 판단) | `frontend/` |

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

**쓰기 — 공연 등록/관리**
`React → API(POST/PUT) → Service → Repository`. 주최자 권한 확인은 Service 계층에서.

## 5. 외부 의존

| 대상 | 용도 | 실패하면 | 대안 |
|---|---|---|---|
| PostgreSQL | 모든 영속 데이터, 좌석 선점 정합성 | 서비스 전체 중단 | 없음 (단일 의존으로 유지, §6) |

## 6. 이 구조를 고른 이유

- UI 라이브러리(MUI) → [ADR-0002](../decisions/ADR-0002-ui-library-mui.md)
- 단일 서비스, 레이어드 아키텍처, Java/Spring + React + PostgreSQL 스택 →
  [ADR-0003](../decisions/ADR-0003-layered-architecture-and-stack.md)
- 좌석 선점 동시성 제어(DB 트랜잭션만으로 처리, 규모 가정과 재검토 트리거) →
  [ADR-0004](../decisions/ADR-0004-seat-hold-concurrency.md)

## 7. 알려진 부채

| 무엇 | 왜 이대로 두는가 | 언제 다시 볼 것인가 |
|---|---|---|
| 좌석 선점을 DB 트랜잭션(유니크 제약 + 조건부 UPSERT)만으로 처리, 별도 락/큐 계층 없음 | 현재 규모 가정(§6)에서는 불필요한 복잡도이기 때문 | 동시 접속 1,000명 초과, 오픈 순간 DB 커넥션 풀 포화, 선점 API p99 > 1초 중 하나라도 관측되면. 이때 검토 순서: 진입 제한(대기열) → 메모리 선점 계층 → 비동기 저장 |
