---
status: ACTIVE
saved_at: 2026-11-12T00:00:00Z
summary: "PLAN_TASK-024.json — route Frontend, flows 4건이 AC 6건을 전부 덮음. input_fields 8. 변경 대상 17(신규 1·삭제 1, 승인 테스트 5파일 포함)."

next_steps:
  - "Phase 2a — coverage_matrix.py --skeleton 으로 칸을 닫는다. unresolved #1 의 승인 테스트 수정(http·WriteKeyRejected·AuthScreens·task018·SocialLoginScreens)을 시나리오의 회귀 행으로 명시해 2b 승인에 함께 올린다"

decisions:
  - decision: "401/403 은 상태로 판별한다(본문 무관)"
    rationale: "이 경로의 403 은 SecurityConfig 의 FORBIDDEN_ROLE 하나뿐이고, 본문 없는 프록시 응답도 같은 처리가 맞다. 코드로 판별하던 이유(쓰기 키 401 과 인증 401 구분)는 쓰기 키와 함께 사라진다"
  - decision: "Bearer 는 메서드와 무관하게 저장된 토큰이 있으면 싣고, 호출부가 실은 Authorization 은 덮어쓰지 않는다"
    rationale: "기능 문서 §3-7. 무효 토큰은 JwtAuthenticationFilter 가 무시해 공개 GET 이 401 이 되지 않는다. fetchMe 는 복원 중 토큰을 직접 싣는다"
  - decision: "주최자 전용 안내는 RequireOrganizer.tsx 안에 두고 가드와 쓰기 403 이 같은 컴포넌트를 쓴다"
    rationale: "같은 뜻에 같은 화면(design.md §5). 시안 appbar 「내 공연」은 /organizer 가 생기는 TASK-025 이후"
  - decision: "화면을 떠난 뒤 도착한 401/403 은 무시한다"
    rationale: "ADR-0018 — 늦은 응답이 상태를 바꾸지 않는다(LoginPage mounted 관례)"
  - decision: "AUTH-001 error 승격은 이 태스크에서 constraints.yaml 을 고치지 않고 Phase 5 로 넘긴다"
    rationale: "Phase 3·4 중 제약 파일 변경은 훅이 묻는다. 위반 0 을 Phase 4 에서 확인한 뒤 회고에서 승격을 제안한다"

attempts: []
---

# CP-1.3 Phase 1 설계 정규화 — TASK-024

- 산출물: `workflow_design/04_plan/PLAN_TASK-024.json`
- 참조 ADR: 0029·0015·0016·0017·0018·0022
- unresolved 8건 — #1 승인 테스트 수정 5파일은 2b 승인에 함께 올린다(task018·SocialLoginScreens 는 spec 밖 → scope_deviation)
- 시안: `out/screens/organizer-only.html`
