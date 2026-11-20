---
status: ACTIVE
saved_at: 2026-11-20T00:30:00Z
summary: "RETRY_SCENARIO 2회차 Red — 함수 26 == 시나리오 26. 전체 231 중 실패 3(SC-22 l·m, SC-25 15행, SC-26), 기존 228 통과. HITL#1·2 재승인 대기"

next_steps:
  - "HITL#1·2 재승인 → Phase 3 3회차(프로토콜·JNDI·인터럽트·타임아웃 표기)"

decisions:
  - decision: "컴파일 스켈레톤은 테스트가 import 하는 타입만 — MailConfig·엔티티·리포지토리·V9 는 Phase 3"
    rationale: "wf-red 컴파일 언어 절. 메서드 본문 13개 전부 throw 한 줄"
  - decision: "기동 하네스 BareBoot — 설정 파일 끄기(--spring.config.location) + 값은 최우선 MapPropertySource"
    rationale: "프로브: 파일 값이 기본 프로퍼티를 이긴다. AppleProviderConfigTest 의 기존 이유(인자 파싱으로 공백·역슬래시가 바뀌지 않게)를 지키려 값은 인자가 아니라 맵으로"
  - decision: "승인 테스트 4파일 수정 — 단언 불변 2(Social·Apple 하네스), 버전 8→9 2, Social 제공자 없음 덩어리에 FRONTEND_BASE_URL 추가"
    rationale: "PLAN unresolved #1~3, HITL 에서 승인받는다"
  - decision: "PostgreSQL 위 스프링 컨텍스트 테스트를 처음 도입(PostgresTestContainer, Flyway on, ddl-auto none)"
    rationale: "조건부 UPDATE·TIMESTAMPTZ·FK 를 운영 엔진으로(ADR-0006). 기존 테스트는 H2 그대로"
  - decision: "SC-17 e 의 로그 단언을 위해 행마다 다른 이메일(sc17a… 처럼 e@ticket.on 을 포함하지 않는 주소)"
    rationale: "출력 어디에도 e@ticket.on 이 없다는 단언이 다른 행 주소에 걸리지 않게"

attempts:
  - n: 4
    result: "RETRY_SCENARIO 2회차 Red"
    outcome: "SC-22 l·m, SC-25(15/18행), SC-26 red — 실제 결함. PLAN 정정: JNDI 키만으로는 Boot 가 기동을 막지 않음(MailConfig 가 검사)"
    back_to: "Phase 2b"
  - n: 3
    result: "RETRY_SCENARIO Red"
    outcome: "SC-21~24 Red 4건 — 구현이 이미 있어 새 테스트가 실제 결함으로 실패(RC-1 재현, RC-2 무한 대기·상한 없음·종료 대기 없음). BareBoot 시스템 환경변수 제외(RC-4)는 이 머신에서 Red 관찰 불가"
    back_to: "Phase 2b"
  - n: 1
    result: "실행 불가"
    reason: "Docker Desktop 꺼짐 — PostgreSQL 테스트 initializationError(기존 마이그레이션 테스트 포함). 사용자가 Docker 기동"
  - n: 2
    result: "하네스 결함 1"
    reason: "CapturingMailConfig 가 같은 객체를 @Primary 빈 둘로 등록 → Task027AccountMailTest 주입 실패. 빈 하나로 고쳐 6/6 이 구현 부재로 red"
---

# CP-2.5 Red — TASK-027

- `workflow_design/05_scenario/TEST_TASK-027.json`
