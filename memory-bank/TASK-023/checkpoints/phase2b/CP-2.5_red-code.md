---
status: ACTIVE
saved_at: 2026-11-13T00:00:00Z
summary: "Red 테스트 19건 작성·실행 — 신규 18 red + 1 already_passing(SC-18), 수정한 승인 테스트 6건 red, 인벤토리 19==19, 사람 승인 완료(HITL#1·2)"

next_steps:
  - "Phase 3 — wf-develop: V8 마이그레이션, 리포지토리 published 조건·주최자 목록, 서비스 publish/unpublish/조회·수정 규칙, PerformanceStatusRules 플래그, 컨트롤러·예외 매핑·SecurityConfig, first-deploy-checklist V8"
  - "Phase 3 자체 리뷰·Phase 4 에서 code-reviewer 에 JPA 지시(CLAUDE.md) 포함 — 비트랜잭션 프로브, 거부 경로 DB 변경 없음, Given 설정 적용 여부"

decisions:
  - decision: "승인 테스트 3개 파일을 Red 단계에서 수정한다 (PerformanceOwnerMigrationTest 7→8, PerformanceOwnershipApiTest SC-11 에 publish 단계, PerformanceZoneRegistrationApiTest 의 detail() 을 주최자 단건 조회로 + 응답 키 3개)"
    rationale: "등록이 비공개로 바뀌고 V8 이 생기면 확실히 깨진다 — Green 에서 처음 드러나 사람 승인을 또 받는 것보다 2b 에서 함께 승인받는다. 시나리오 SC-01·02 회귀 항목"
  - decision: "스켈레톤은 Performance 에 published 필드(기본 TRUE)·getter·생성자 오버로드만"
    rationale: "HTTP 경계 테스트라 새 서비스 메서드가 필요 없다. 기본값 TRUE 는 기존 픽스처 보호용 PLAN 결정. nullable=false 는 넣지 않음(SC-01 이 검증)"
  - decision: "SC-18 은 already_passing"
    rationale: "관객 응답 키 불변 회귀 — 구현 전에도 참"
  - decision: "SC-05·07 의 NPE 를 num()·getOrDefault 로 막아 단언 실패로 드러나게 함"
    rationale: "엔드포인트가 없을 때 NPE 는 테스트 결함이지 Red 가 아니다"

attempts: []
---
