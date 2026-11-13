---
status: ACTIVE
saved_at: 2026-11-13T00:00:00Z
summary: "시나리오 19건(happy 11·error 7·regression 1)·경계 칸 48 판정 완료, 독립검증 2회차 통과(FAIL 1 → PASS, 경고 6)"

next_steps:
  - "Phase 2b — wf-red 로 시나리오를 실패하는 테스트로 옮기고 통합 HITL 요약에 보류 경고를 올린다"
  - "2b 승인에 승인 테스트 수정 2건(PerformanceOwnerMigrationTest 버전 7→8, PerformanceZoneRegistrationApiTest:173)과 쓰기 응답 키 점검, mass assignment(published·ownerId 본문 필드) 제안을 올린다"

decisions:
  - decision: "목록 page<0·size<1·offset int 초과는 새 목록에서 400 INVALID_REQUEST (관객 목록은 건드리지 않음)"
    rationale: "프로브로 관객 목록이 page=-1·size=0·page=2147483647 에서 미처리 예외(운영 500)가 남을 확인. 새 목록까지 같은 결함을 들이지 않는다 — PLAN unresolved 의 선택지 중 400 을 택했다"
  - decision: "now>=openAt 경계에서 취소 > 잠금 > 멱등 순으로 검사 (SC-13 d, SC-14 f·g)"
    rationale: "AC 의 '이미 공개면 200' 과 'now>=openAt 이면 409' 충돌을 잠금 우선으로 고정. 멱등 200 은 now<openAt 에서만"
  - decision: "id 경계 25칸(F4~F8)을 SC-17 한 표로 묶음 — GET/publish/unpublish/PUT 네 경로"
    rationale: "같은 경로 변수 변환·소유 조회 코드를 타 결함이 있으면 같이 깨진다. 엔드포인트를 표에 명시해 F6·F7 칸도 같은 publish 경로로 닫음"
  - decision: "page·size 경계 10칸을 SC-07 한 표로 묶음, closeAt 경계는 SC-05 의 i 행"
    rationale: "같은 PageRequest 조립 경로, now==closeAt 은 statusSpecification 의 OPEN/CLOSED 분기 한 곳"
  - decision: "Authorization.null 은 n/a — HTTP 헤더에 null 이 없음"
    rationale: "TASK-022 PLAN 과 같은 판단. 토큰 형식 변형은 기존 TokenFormatApiTest 가 덮음"
  - decision: "F11(AC 밖)은 SC-19 로 덮되 covers 에 AC 밖임을 명시"
    rationale: "PLAN 결정 — 사용자가 원치 않으면 SC-19·SC-02 의 응답 단언 일부와 함께 뺀다"
  - decision: "SC-06 의 뒤 공백 enum 가설 행은 삭제 — 확정 못 하는 기대를 시나리오에 두지 않는다"
    rationale: "독립검증 1회차 V5 경고. 빈 값(filter=)은 관객 목록 프로브로 확정돼 남김"
  - decision: "보류한 경고(통합 HITL 요약용): V6 When 단일성(SC-03·05·11·12·16·19), V9 SC-13/15/16 중복, V7 구현 용어, V8 PLAN outputs 에 없는 400 INVALID_REQUEST, V12 경계 제안 6건(mass assignment·공개 공연 cancel 후 published·unpublish 401/403 등)"
    rationale: "경고만 남으면 재검증하지 않는다 — 사람이 2b 끝에서 판단"

attempts:
  - n: 1
    outcome: "독립검증 FAIL — V11 false_cover size@F3:integer.overflow (SC-07 에 size 초과 행 없음)"
    back_to: "2a"
---
