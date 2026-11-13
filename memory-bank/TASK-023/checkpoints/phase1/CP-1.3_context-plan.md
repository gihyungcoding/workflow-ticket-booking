---
status: ACTIVE
saved_at: 2026-11-13T00:00:00Z
summary: "PLAN_TASK-023.json — route Backend, flows 11건(F1~F10 이 AC 12건을 덮고 F11 은 AC 밖 문서 §3-6). input_fields 7. 변경 대상 15(신규 6)."

next_steps:
  - "Phase 2a — coverage_matrix.py --skeleton 으로 칸을 닫는다. unresolved 의 승인 테스트 수정 2건(PerformanceOwnerMigrationTest 버전 \"7\"→\"8\", ZoneRegistrationApiTest:173 의 등록 후 관객 조회)을 시나리오 회귀 행으로 명시해 2b 승인에 올린다"
  - "2a 에서 경계 판단 3건 확정 — 취소 vs 잠금 우선순위, 이미 공개 + now>=openAt 의 publish(409), 취소 + 공개 + now>=openAt 의 수정(PERFORMANCE_CANCELLED 우선)"
  - "Postgres 마이그레이션 테스트(V8)를 Testcontainers 로 필수 포함"

decisions:
  - decision: "Performance 기존 생성자는 published=true, 서비스 등록 경로만 false 를 명시"
    rationale: "new Performance(...) 를 쓰는 승인 테스트 9개 파일의 픽스처가 관객 조회에서 사라지는 것을 막는다"
  - decision: "F11 — 등록·수정·취소 응답을 OrganizerPerformanceResponse 로 (AC 밖, 문서 §3-6 기준)"
    rationale: "기능 문서가 Task B 산출물로 정했다. 사용자가 원치 않으면 F11 만 뺀다"
  - decision: "SecurityConfig 에 /api/organizer/** hasRole(ORGANIZER) 추가 — tasks.json 의 implementation_spec.paths 에 없던 파일"
    rationale: "없으면 anyRequest().authenticated() 로 관객 토큰이 200 을 받는다"
  - decision: "주최자 목록 응답은 신규 OrganizerPerformanceListResponse({items,...})"
    rationale: "관객 목록 record 는 content 키라 재사용 불가 (문서 §3-6)"
  - decision: "취소를 시각 규칙보다 먼저 검사하고, 잠금(409)을 멱등(200)보다 먼저 검사"
    rationale: "AC 의 두 문장이 now>=openAt 에서 충돌하는 칸을 한쪽으로 고정 — 2a 에서 시나리오로 확정"

attempts: []
---
