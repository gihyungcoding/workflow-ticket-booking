# 기술 결정 기록 (ADR)

마지막 재생성: 2026-09-08 · 총 8건

> 이 파일은 `scripts/rebuild_doc_index.py` 가 생성합니다. 직접 편집하지 마세요 — 다음 재생성 때 사라집니다.

새 결정을 기록하려면 `/wf-adr "<제목>"`. ADR 은 고치지 않고 쌓습니다 — 결정이 바뀌면 새 ADR 이 이전 것을 대체합니다.

| ID | 제목 | 상태 | 날짜 |
|---|---|---|---|
| [ADR-0001](ADR-0001-record-architecture-decisions.md) | 아키텍처 결정을 ADR로 기록한다 | 채택됨 | 2026-09-01 |
| [ADR-0002](ADR-0002-ui-library-mui.md) | UI 라이브러리로 MUI(Material UI)를 쓴다 | 채택됨 | 2026-09-02 |
| [ADR-0003](ADR-0003-layered-architecture-and-stack.md) | 레이어드 아키텍처와 Java/Spring + React + PostgreSQL 스택을 쓴다 | 채택됨 | 2026-09-02 |
| [ADR-0004](ADR-0004-seat-hold-concurrency.md) | 좌석 선점 동시성 제어는 DB 트랜잭션(유니크 제약 + 조건부 UPSERT)만으로 처리한다 | 채택됨 | 2026-09-02 |
| [ADR-0005](ADR-0005-clock-injection-for-time-based-decisions.md) | 시각 기준 판정은 주입된 Clock 을 쓰고 DB now() 를 직접 쓰지 않는다 | 채택됨 | 2026-09-03 |
| [ADR-0006](ADR-0006-test-db-h2-then-testcontainers.md) | 테스트 DB는 당분간 H2 인메모리를 쓰고, 좌석 선점 착수 전 Testcontainers로 전환한다 | 채택됨 | 2026-09-03 |
| [ADR-0007](ADR-0007-frontend-test-framework-vitest.md) | 프론트엔드 테스트 프레임워크로 Vitest + Testing Library를 쓴다 | 채택됨 | 2026-09-08 |
| [ADR-0008](ADR-0008-client-routing-react-router-dom.md) | 클라이언트 라우팅으로 react-router-dom을 쓴다 | 채택됨 | 2026-09-08 |
