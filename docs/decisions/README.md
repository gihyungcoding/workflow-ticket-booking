# 기술 결정 기록 (ADR)

마지막 재생성: 2026-10-21 · 총 17건

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
| [ADR-0009](ADR-0009-seat-individual-rows.md) | 좌석은 구역(등급)×행×열 입력으로 서버가 개별 `seat` 행을 생성한다 | 채택됨 | 2026-09-11 |
| [ADR-0010](ADR-0010-bean-validation-for-request-dtos.md) | 요청 DTO의 필드 형태 검증에 Bean Validation을 쓴다 | 채택됨 | 2026-09-16 |
| [ADR-0011](ADR-0011-promote-design-constraints.md) | 디자인 제약 DESIGN-001·002 를 error 로 승격한다 | 채택됨 | 2026-10-13 |
| [ADR-0012](ADR-0012-jwt-based-authentication.md) | 세션이 아닌 JWT(Bearer 토큰)로 인증한다 | 채택됨 | 2026-10-14 |
| [ADR-0013](ADR-0013-spring-security-adoption.md) | Spring Security를 전체 도입한다 (OAuth2 Client + 커스텀 JWT 인증 필터) | 채택됨 | 2026-10-14 |
| [ADR-0014](ADR-0014-social-account-auto-link-policy.md) | 이메일이 일치하고 검증된 경우에만 소셜 계정을 자동 연결한다 | 일부 대체됨 (ADR-0020) | 2026-10-14 |
| [ADR-0015](ADR-0015-access-token-localstorage.md) | accessToken을 프론트 localStorage에 저장한다 | 채택됨 | 2026-10-14 |
| [ADR-0017](ADR-0017-keep-write-guard-until-ownership-check.md) | 쓰기 가드는 공연 쓰기 API 에 소유권 검사가 붙을 때까지 유지한다 | 채택됨 | 2026-10-21 |
| [ADR-0016](ADR-0016-split-deploy-with-cors-and-write-guard.md) | 프론트와 백엔드를 따로 배포하고, 교차 출처 직접 호출과 임시 쓰기 가드를 둔다 | 채택됨 (쓰기 가드 제거 시점은 ADR-0017 로 대체됨) | 2026-10-14 |
| [ADR-0018](ADR-0018-client-session-generation.md) | 클라이언트 세션 상태는 세대로 직렬화하고, 늦게 온 복원 응답은 버린다 | 채택됨 | 2026-10-22 |
| [ADR-0019](ADR-0019-string-input-validation-server-canonical.md) | 문자열 입력 검증은 서버가 정본이고, 제어문자는 거부하며 이메일 외에는 정규화하지 않는다 | 채택됨 | 2026-10-22 |
| [ADR-0020](ADR-0020-auto-link-only-to-email-verified-accounts.md) | 자동 연결은 이메일 소유가 확인된 계정에만 한다 (ADR-0014 일부 대체) | 채택됨 | 2026-10-23 |

대체된 ADR 도 목록에 남깁니다. 과거에 왜 그렇게 판단했는지가 지워지면 같은 논의를 다시 하게 됩니다.
