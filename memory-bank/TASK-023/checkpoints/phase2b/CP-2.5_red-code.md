---
status: ACTIVE
saved_at: 2026-11-13T00:00:00Z
summary: "Phase 4 롤백 2b — SC-20·SC-21 Red 2건 작성·실행(205 중 2 실패), 인벤토리 21==21, 사람 승인 대기"

next_steps:
  - "HITL #1·2 재승인 — 추가 시나리오 SC-20·SC-21 과 Red 테스트"
  - "승인되면 Phase 3 — 쓰기 경로 행 잠금(PESSIMISTIC_WRITE)·시각을 잠근 뒤에 읽기, 관객·주최자 목록 page·size 공통 검증"

decisions:
  - decision: "SC-20 은 별도 트랜잭션의 JPA 표준 잠금 + 별도 스레드 HTTP 요청으로 결정적으로 만든다"
    rationale: "경쟁을 우연에 맡기지 않고, 구현(락/@Version)과 무관하게 최종 DB 값으로 단언한다. Red 실행이 RC-1 변종(취소·title·공개 되돌림, 시각 판정)을 실제로 재현했다"
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
