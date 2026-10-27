---
task_id: TASK-012
title: "애플 로그인 연동"
phase: "2a"
phase_name: "Phase 2a - Scenario (HITL#1 승인 완료)"
status: ACTIVE
track: standard
created_at: 2026-10-27
last_updated: 2026-10-27

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-012-apple-login-oauth2"

last_checkpoint: "CP-2.2"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-012.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-012.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-012.json"
---

## 지금 무엇을 하고 있나

Phase 2a (Scenario) 를 마쳤다 — 시나리오 12건(happy 7 / boundary 3 / error 1 / regression 1), AC 3/3, 독립검증 2회차 PASS(warn 1: V6), HITL#1 승인. CP-2.2 저장.
확정한 결정: 표시 이름 `firstName lastName` 공백 1칸 · 이름 없으면 `Apple 사용자` (모두 사용자 선택). 보류한 경고 V6(SC-02 When 에 교환 두 번)은 승인과 함께 확정.
설계 요지: AuthService 는 바꾸지 않는다. 애플 고유 관심사(Client Secret·토큰 교환·form_post·클레임 정규화·user 이름)는 config 에 두고 `SocialProfile` 만 만들어 기존 `loginWithSocial` 에 합류시킨다.

## 다음 한 걸음

Phase 2b — wf-red. 시나리오 12건을 실패하는 테스트로 옮긴다(스텁은 예외를 던지게, 실제 로직 금지). 가짜 애플 서버(JDK HttpServer)와 tokenUri/jwkSetUri 교체 방법을 2b 에서 정한다.

## 알아둬야 할 것

- 범위: `AppleClientSecretGenerator`(ES256 서명 Client Secret), `form_post` 콜백 처리, 최초 1회만 오는 이름(`user` 파라미터) 캡처, `CustomOAuth2UserService` 의 애플 분기. 프론트(TASK-013)는 제외
- 관련 ADR: ADR-0013(Spring Security OAuth2 Login), ADR-0014→ADR-0020(자동 연결은 이메일 소유가 확인된 계정만), ADR-0019(문자열 입력 서버 정본), ADR-0021(토큰은 URL fragment — 핸들러는 아직 query, 후속 태스크 대상)
- **TASK-011 인계(Phase 4 VERIFY RC-1 변종, `deferred_to: TASK-012`)**: 애플의 `email_verified` 는 불리언이 아니라 문자열 `"true"` 로 올 수 있다. `SocialProfile.emailVerified` 에 true 를 넣는 기준을 엄격히 정해야 한다(잘못 true 가 되면 계정 선점 경로가 열린다). 시나리오 변종을 이 태스크가 전부 가져온다
- **문자열 규칙 재사용**: 외부 문자열(이메일·이름·providerUserId)은 `SocialInputRules` 한 곳을 거친다. 애플 `id_token` 클레임과 `user` 파라미터의 이름도 같은 규칙(이름 100자, 제어문자·서로게이트 거부)을 통과해야 한다
- 새 지침(회고 제안 반영, PR #23 미머지 — 수동 적용): wf-plan — 운영 엔진(PostgreSQL) 통합 테스트를 test_hints 에 필수로 적거나 사유를 unresolved 에, 외부 신원 연결 설계는 선점 가능성을 unresolved 에. wf-scenario — feature 문서 상태 표를 covers 와 대조, 정책이 바뀐 롤백이면 기존 시나리오 전수 대조
- 문서 갱신 필요(이 태스크 범위 밖 보고): `user-authentication.md` §1-3 계정 연결 정책 문장은 ADR-0020 이전 내용이고, `tasks.json` 의 TASK-011 AC 문구도 어긋난다
- 열린 회고 개선안 6건은 서로 다른 대상이라 시작을 막지 않는다
- 크기: 12h 로 정상 WRU 범위(4~16h). 시나리오가 기준선(8~12)을 넘으면 2a 에서 사용자에게 알린다
