---
status: ARCHIVED
saved_at: 2026-11-13T00:00:00Z
summary: "Phase 4 롤백 수정 — RC-1 쓰기 경로 행 잠금(잠근 뒤 시각)·RC-2 page·size 공통 검증, 전체 205/205, spotless 0, 자체 리뷰 2R(차단 결함 없음, 잠금 타임아웃 위험 낮음 unresolved)"

next_steps:
  - "Phase 4 재검증 — code-reviewer 에 이전 root_causes·수정 커밋 범위·JPA 지시 포함"
  - "사용자에게 알릴 것: 락 타임아웃 힌트·409/503 매핑은 시나리오 밖 후속 태스크 후보"

decisions:
  - decision: "RC-1 은 PESSIMISTIC_WRITE 행 잠금(쓰기 경로 한 곳 findOwnedForUpdate)으로 고쳤다 — @Version 이 아님"
    rationale: "컬럼·마이그레이션·409 매핑이 더 필요 없다. 시각은 잠근 뒤에 읽어 SC-20 f·h·i 를 만족한다. 공통 지점은 쓰기 조회 한 곳(fix_locus common)"
  - decision: "잠금 타임아웃·락 예외 매핑은 이번에 만들지 않고 unresolved 로 남김"
    rationale: "시나리오에 없는 동작 — 규칙상 지금 사용자에게 알린다(낮은 위험)"
  - decision: "리팩토링하지 않음"
    rationale: "최소 구현이 findOwned·statusSpecification·toResponse 관례를 재사용해 중복이 없다"
  - decision: "승인 테스트 2건(BrowseResponse SC-10, Ownership SC-13)을 Given 에 publish 를 더해 수정 — 사용자 승인 (AskUserQuestion)"
    rationale: "2b 의 Grep 이 헬퍼 안에서 API 로 등록하는 읽기 경로를 놓쳤다. 단언은 불변"
  - decision: "PerformanceFilter enum 을 domain 에 신규"
    rationale: "쿼리 파라미터 enum 바인딩(잘못된 값 400)과 ARCH-003(Repository 는 도메인 언어로 호출)"
  - decision: "RC-1 동시성은 이 태스크에서 고치지 않음"
    rationale: "시나리오에 없고 기존 update/cancel 과 같은 뿌리 — Phase 4 로 넘기고 사용자에게 알림"

attempts: []
---
