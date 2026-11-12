---
status: ARCHIVED
saved_at: 2026-11-11T00:00:00Z
summary: "PLAN_TASK-022.json — route Backend, flows 8건이 AC 8건을 전부 덮음. input_fields 3. 변경 대상 17(신규 2·삭제 7)."

next_steps:
  - "Phase 2a — coverage_matrix.py --skeleton 으로 칸을 닫는다. unresolved 의 승인 테스트 수정 5건(AuthRegression·ErrorResponseRegression·등록/구역/조회 API·ZoneMigration SC-14·CorsFilter)을 시나리오의 회귀 행으로 명시해 2b 승인에 함께 올린다"

decisions:
  - decision: "verifyAccessToken() 의 반환 타입(Optional<Long>)은 유지하고 verifyAccessClaims() 를 추가한다"
    rationale: "JwtTokenFormatTest·OAuth2SocialLoginCallbackTest 가 Optional<Long> 을 단언 — 승인 테스트 수정을 줄인다. 기능 문서 §6 의 '반환 타입 변경' 과 다름"
  - decision: "role 클레임이 누락·정의 밖이면 토큰 무효(401)"
    rationale: "역할 없는 인증 주체를 만들지 않는다. 403 은 '정상 토큰 + 역할 불일치' 에만"
  - decision: "소유권 위반은 기존 PerformanceNotFoundException(id) 을 그대로 던진다"
    rationale: "ADR-0029 §3 — 남의 공연·소유자 NULL·없는 id 가 같은 404 본문"
  - decision: "owner_id 는 엔티티에 Long 컬럼(User 연관 없음), FK ON DELETE SET NULL"
    rationale: "도메인 간 결합을 만들지 않고, 사용자 삭제 시 공연이 '소유자 없음(아무도 수정 못 함)' 으로 남는다"
  - decision: "Performance 기존 생성자는 유지하고 ownerId 를 받는 생성자를 추가"
    rationale: "new Performance(…) 를 쓰는 승인 테스트 9개 파일을 고치지 않는다"
  - decision: "first-deploy-checklist 는 이 태스크에서 쓰기 키 변수 전체(VITE_WRITE_KEY 포함)를 정리한다"
    rationale: "배포 절차 문서는 한 번에 일관돼야 한다. 프론트 코드 제거는 TASK-024"

attempts: []
---

# CP-1.3 Phase 1 설계 정규화 — TASK-022

- 산출물: `workflow_design/04_plan/PLAN_TASK-022.json`
- 참조 ADR: 0029·0013·0017·0016·0006·0005
- unresolved 11건 — 그중 승인 테스트 수정 5건은 2b 승인에 함께 올린다
