---
task_id: TASK-012
title: "애플 로그인 연동"
phase: "5"
phase_name: "Phase 5 - Reflect (HITL#4 승인, 태스크 종료)"
status: DONE
track: standard
created_at: 2026-10-27
last_updated: 2026-10-27

primary_category: Backend
sub_categories: []
target_repo: "."
branch: "feature/task-012-apple-login-oauth2"

last_checkpoint: "CP-5.2"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-012.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-012.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-012.json"
  red: "workflow_design/05_scenario/TEST_TASK-012.json"
  dev: "workflow_design/06_dev/DEV_TASK-012.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-012.json"
  reflect: "workflow_design/08_reflect/REFLECT_TASK-012.json"
---

## 지금 무엇을 하고 있나

TASK-012 를 종료했다(DONE). Phase 1~5 완료: 시나리오 12건, 테스트 13건(승인 12 + RC-1 회귀 1), H2 146/146 · PostgreSQL 114/114, Phase 4 FAIL 1회(RC-1) 후 예외 승인(R-1). 승인한 코드는 verified_commit 8714a94. 회고 승인(HITL#4) — 승인 + 반복 제안 지금 반영.

## 다음 한 걸음

`/wf-ship` 으로 머지를 준비한다(승인한 코드 신선도 대조 → PR 생성, 머지는 사람이). 후속 태스크 후보: R-1(세션 쿠키 SameSite), ADR-0021 fragment 리다이렉트, user-authentication.md §1-3 갱신, architecture.md 에 config 계층 행.

## 알아둬야 할 것

- 범위: `AppleClientSecretGenerator`(ES256 서명 Client Secret), `form_post` 콜백 처리, 최초 1회만 오는 이름(`user` 파라미터) 캡처, `CustomOAuth2UserService` 의 애플 분기. 프론트(TASK-013)는 제외
- 관련 ADR: ADR-0013(Spring Security OAuth2 Login), ADR-0014→ADR-0020(자동 연결은 이메일 소유가 확인된 계정만), ADR-0019(문자열 입력 서버 정본), ADR-0021(토큰은 URL fragment — 핸들러는 아직 query, 후속 태스크 대상)
- **TASK-011 인계(Phase 4 VERIFY RC-1 변종, `deferred_to: TASK-012`)**: 애플의 `email_verified` 는 불리언이 아니라 문자열 `"true"` 로 올 수 있다. `SocialProfile.emailVerified` 에 true 를 넣는 기준을 엄격히 정해야 한다(잘못 true 가 되면 계정 선점 경로가 열린다). 시나리오 변종을 이 태스크가 전부 가져온다
- **문자열 규칙 재사용**: 외부 문자열(이메일·이름·providerUserId)은 `SocialInputRules` 한 곳을 거친다. 애플 `id_token` 클레임과 `user` 파라미터의 이름도 같은 규칙(이름 100자, 제어문자·서로게이트 거부)을 통과해야 한다
- 새 지침(회고 제안 반영, PR #23 미머지 — 수동 적용): wf-plan — 운영 엔진(PostgreSQL) 통합 테스트를 test_hints 에 필수로 적거나 사유를 unresolved 에, 외부 신원 연결 설계는 선점 가능성을 unresolved 에. wf-scenario — feature 문서 상태 표를 covers 와 대조, 정책이 바뀐 롤백이면 기존 시나리오 전수 대조
- 문서 갱신 필요(이 태스크 범위 밖 보고): `user-authentication.md` §1-3 계정 연결 정책 문장은 ADR-0020 이전 내용이고, `tasks.json` 의 TASK-011 AC 문구도 어긋난다
- 열린 회고 개선안 6건은 서로 다른 대상이라 시작을 막지 않는다
- 크기: 12h 로 정상 WRU 범위(4~16h). 시나리오가 기준선(8~12)을 넘으면 2a 에서 사용자에게 알린다
