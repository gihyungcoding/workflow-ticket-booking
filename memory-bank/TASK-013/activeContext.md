---
task_id: TASK-013
title: "소셜 로그인 버튼·역할 선택·콜백 처리"
phase: "4"
phase_name: "Phase 4 완료 (HITL#3 예외 승인, 5 진입 대기)"
status: ACTIVE
track: standard
created_at: 2026-10-29
last_updated: 2026-10-29

primary_category: Frontend
sub_categories: [audience, organizer]
target_repo: "."
branch: "feature/task-013-social-login-frontend"

last_checkpoint: "CP-4.2"
artifacts:
  plan: "workflow_design/04_plan/PLAN_TASK-013.json"
  scenario: "workflow_design/05_scenario/SCENARIO_TASK-013.md"
  validation: "workflow_design/05_scenario/validator/VALIDATION_TASK-013.json"
  red: "workflow_design/05_scenario/TEST_TASK-013.json"
  dev: "workflow_design/06_dev/DEV_TASK-013.json"
  verify: "workflow_design/07_verify/VERIFY_TASK-013.json"
---

## 지금 무엇을 하고 있나

TASK-013 Phase 1(Plan) 완료. flows F1~F5 가 AC 5개를 덮는다. 선행 TASK-011·TASK-012 는 develop 에 머지됨. 프론트 골격 확인 완료 — `npm test` 176/176 통과, `npm run lint` error 0(기존 warning 3건).

## 다음 한 걸음

`wf-scenario`(Phase 2a) — (Phase 1 에서 한 일: 코드베이스 조사(기존 `LoginPage`·`SignUpPage`·`AuthContext`·`api/auth.ts`·라우터), 백엔드 콜백·`POST /api/auth/social/complete` 응답 계약을 **백엔드 소스에서 직접** 확인, 응답 필드를 feature 문서 문장으로 가정하지 않음). 2a 확정 6건은 CP-1.3 next_steps 참고.

## 알아둬야 할 것

- 완료 조건 5개: 4개 제공자 버튼(`/oauth2/authorization/{provider}` 이동) · `token` 쿼리 즉시 로그인 후 홈 · `pendingToken` 쿼리면 역할 선택 화면, 제출 후 로그인 · pendingToken 만료 오류 시 안내 + 로그인 화면 이동 · 제공자 취소/실패 시 로그인 화면 복귀 + 오류 메시지
- 출처: `docs/product/features/user-authentication.md` §2-5(상태별 표 — 시나리오 covers 와 한 행씩 대조), §5 Task E, §6, §7(범위 외)
- **결정(사용자, 2026-10-29)**: 토큰은 **query(`?token=`)** 기준으로 만든다. ADR-0021(fragment) 전환은 후속 태스크에서 프론트도 함께 바꾼다. 콜백 페이지가 파싱 위치를 한 곳에 모으도록 설계해 전환 비용을 줄인다
- 미해결 위험 R-1(TASK-012 인계): 인가 요청 state 가 HttpSession 에 있고 JSESSIONID 에 SameSite/Secure 없음 — 애플 `form_post` 교차 사이트 콜백이 실제 브라우저에서 실패할 수 있다. 이 태스크가 소셜 로그인을 브라우저로 처음 확인하는 지점. 이 태스크 범위는 아님(백엔드 후속), 확인되면 기록한다
- 소셜 최초 가입 이메일/이름 표시: 역할 선택 화면은 "소셜 계정의 이메일/이름을 안내 문구로 보여준다"(§2-5). 이를 어디서 얻는지(pendingToken 페이로드 디코드 vs API)는 Phase 1 에서 백엔드로 확인
- 회고 지침(develop 에 반영 완료): wf-plan — 외부 서버 응답도 `design.inputs` 에 · 운영 엔진 통합 테스트 · 외부 신원 선점 가능성. wf-scenario — 상태 표를 covers 와 대조, 정책이 바뀐 롤백이면 기존 시나리오 전수 대조. **미머지**: `chore/retro-wf-plan-external-response` 브랜치(wf-plan 의 "외부 서버 응답" 한 문단)는 로컬 커밋만 있고 develop 에 없다
- 환경: node 는 `PATH="$HOME/.nvm/versions/node/v22.23.1/bin:$PATH"` 로 지정해 실행(`~/.nvm/nvm.sh` 없음)
- 크기: 12h, 정상 WRU 범위(4~16h)
