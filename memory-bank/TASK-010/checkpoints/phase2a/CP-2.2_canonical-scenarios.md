---
status: ARCHIVED
saved_at: 2026-10-22T03:40:00Z
summary: "[Phase 4 1차 FAIL 롤백 재진입 · 6차 독립검증 PASS(경고 4) · HITL#1 승인됨 2026-10-22] RC-1(복원 응답 경쟁) 변종 5건을 SC-33~35 로 추가해 31건. 경고 반영: V11 통과 경계·UTF-16 이모지 행·역할 매핑 표·이름 공백 보존, V5 SC-29 시점. 보류: V6 SC-28 When 다중, V9 SC-32 중복. 승인 전 상태. [이전] [HITL#1 승인] 5차 독립검증 PASS (경고 3) SC-28 개 28건, AC 6/6. 경고: JSON notes 동기화(반영), SC-28 When 다중·SC-16/32 중복(보류). [5차 작성 — 사용자 예외 승인으로 한 번 더 검증] 4차 V11(로그인 길이 초과 미정) 반영: Plan 문장 정정, SC-32 추가, SC-05→SC-29 병합, When 에서 서버 응답 설정을 Given 으로, JSON 에 표 포함. 28건. 5차 검증 대기. [4차 검증 FAIL — 예외 검증 소진] V11: 로그인 길이 초과(이메일 256자·비밀번호 73자) 기대 동작 미정(Plan 문장이 모호). 사용자 결정 대기. [4차 작성 — 사용자 예외 승인으로 추가 검증 1회] 3차 V11 두 건(로그인 비밀번호 공백만, 가입 확인 공백만) 반영, SC-06·14·18 을 병합해 29건. 4차 검증 대기. [3차 검증 FAIL — 재검증 3/3 소진] V11: 로그인 비밀번호 공백만·가입 비밀번호 확인 공백만 경계 2건 누락. 사용자 결정 대기(Phase 1 롤백 제안 vs 2건 보완 후 예외 재검증). [3차 작성] 2차 독립검증 FAIL(V11: 빈 토큰·문자열 전수 전송·201 본문) 반영 — 31건, 마지막 재검증(3/3) 대기. [2차 작성] 1차 독립검증 FAIL(V11: 재제출 시 오류 초기화·72바이트 경계 누락) 반영 — 28건(happy 9 / error 8 / boundary 10 / regression 1), When 단일화, Plan 에 trim·순환방지·복원실패 보존 규칙 반영. 재검증 대기. [1차] 20건 작성 → FAIL 1·WARN 5."

next_steps:
  - "HITL#1 승인 후 Phase 2b — 기존 테스트에 SC-33~35 와 SC-03·07·08·29 보강 행 추가(전체 재작성 아님), Red 확인(구현이 이미 있어 변이 검증 필요: 복원 무효화를 제거해 테스트가 실패하는지)"
  - "Phase 2b — wf-red 로 Red 테스트 작성 (권고 3건: 통과 경계 8·72·100·255자, 이메일 trim 후 길이, AUDIENCE 매핑을 테스트 행에 포함)"
  - "HITL#1 승인 후 Phase 2b"

decisions:
  - decision: "[롤백 재진입] 복원 응답 경쟁(RC-1)을 SC-33(복원 중 로그인)·SC-34(복원 중 로그인 뒤 로그아웃)·SC-35(StrictMode)로 시나리오화. 복원 중에는 로그아웃 버튼이 없어 SC-34 는 로그인 후 로그아웃 순서로만 만들어진다"
    rationale: "Phase 4 VERIFY RC-1 변종 5건 전부를 한 번에 가져온다(root-cause-sweep). not_probed 2건(restoring 잔류, StrictMode 취소 요청 401)도 시나리오에 넣어 테스트가 프로브하게 한다"
  - decision: "검증자 V11 권고 3건과 이름 공백 보존을 시나리오 표로 올렸다 (2b 에서 이미 테스트 행이 있던 것을 시나리오가 뒤따라 정본화)"
    rationale: "경계는 산문이 아니라 단언 대상이어야 한다. SC-03 역할 표, SC-07 통과 경계 표·이모지 51개·trim 뒤 256자, SC-08 이름 공백"
  - decision: "로그인은 길이 상한을 클라이언트가 검증하지 않는다(SC-32). 가입의 길이 검증만 UTF-16 코드 유닛 수. Plan 의 모호한 문장을 바로잡았다"
    rationale: "VALIDATION 4차 V11 — 4차 보완 때 쓴 Plan 문장이 원인. 사용자가 '보완 후 한 번 더 검증'을 선택"
  - decision: "사용자가 재검증 3/3 소진 후 '두 건 보완 후 추가 검증'을 선택(AskUserQuestion)했다. 가입 확인란 공백만은 값이 있으므로 불일치 오류, 로그인 비밀번호 공백만은 제출(서버 판정), 길이는 UTF-16 코드 유닛 수"
    rationale: "3차 V11 + 서버 @Size(String.length) 와 일치"
  - decision: "가입 성공 판정은 상태 201 만 본다(본문 무시, SC-31). 빈 토큰·공백 토큰은 복원 요청 없이 비로그인(SC-24). 문자열 전수는 가입·로그인 전 필드 표로 전송 값 동일성을 단언(SC-08·09)"
    rationale: "VALIDATION 2차 V11"
  - decision: "SC-19 로 재제출 시 오류 문구 초기화·전환을 추가하고, 72바이트 경계는 클라이언트가 막지 않기로 확정(가입 서버 400, 로그인 서버 401 INVALID_CREDENTIALS)"
    rationale: "VALIDATION 1차 V11. 서버 구현(AuthService.exceedsBcryptLimit)으로 확인"
  - decision: "Plan inputs 에 이메일 trim·공백만 검증·from 순환 방지·복원 실패 시 401 만 토큰 삭제를 반영"
    rationale: "VALIDATION 1차 V8 — 시나리오가 Plan 밖 규칙을 쓰고 있었다"
  - decision: "PLAN unresolved 3건을 시나리오에서 정했다 — 가입 성공 안내 없음(이동만), 복원 중 헤더는 아무것도 그리지 않음, 72바이트 초과 비밀번호는 클라이언트가 검증하지 않고 서버 400 을 일반 문구로 표시"
    rationale: "AC 가 이동만 요구한다 / 깜빡임 방지 / 서버가 최종 판단(architecture.md §2)"
  - decision: "복원 실패 시 401 이면 토큰 삭제, 500·네트워크 오류·비정상 200 이면 토큰 유지"
    rationale: "일시 오류로 사용자를 로그아웃시키지 않는다. 401 은 토큰 무효의 증거"
  - decision: "화면 문구를 시나리오 상단 표를 정본으로 고정"
    rationale: "design.md §5 에 인증 문구가 없다. Red 테스트가 문구를 단언한다"
  - decision: "응답 이상 케이스(SC-08·13·18)는 표로 묶었다"
    rationale: "같은 응답 해석 경로, 같은 Then. TASK-009 회고의 '응답 code 분기' 경계값 행을 적용했다 (다른 code·빈 바디·비 JSON)"
  - decision: "문자열 필드 전수(NUL·서로게이트)는 프론트에서 재검증하지 않는다"
    rationale: "서버가 거부한다(TASK-009 RC-1). 화면은 입력을 변환하지 않고 보내며(SC-06) 400 은 일반 문구(SC-08·13)"

attempts:
  - phase: "2a"
    round: 5
    outcome: "PASS — 경고 3(V1 JSON notes 누락→반영, V6 SC-28 When 다중→보류, V9 SC-16/32 경로 겹침→보류). 권고 3(통과 경계 8·72·100·255자 Then 승격, trim 후 길이, AUDIENCE 매핑)은 Phase 2b 에서 테스트 케이스로 다룰지 HITL#1 에서 판단"
  - phase: "2a"
    round: 4
    outcome: "FAIL — V11(로그인 이메일 256자·비밀번호 73자 기대 동작 미정; 4차 보완에서 Plan 에 쓴 문장이 가입/로그인 길이 검증을 모호하게 함) · WARN 3(JSON 에 표 없음, When 에 서버 응답 설정, SC-05/29 겹침)"
    reject_type: "VALIDATOR_FAIL 4 (예외 1회 소진)"
  - phase: "2a"
    round: 3
    outcome: "FAIL — V11(로그인 비밀번호 공백만, 가입 비밀번호 확인 공백만) · WARN 4 · 31건은 복잡도 기준선(8~12) 초과"
    reject_type: "VALIDATOR_FAIL 3/3 (소진)"
  - phase: "2a"
    round: 2
    outcome: "FAIL — V11(accessToken 빈 문자열·공백, 문자열 전수 NUL·서로게이트 전송 단언, 가입 201 비정상 본문) · WARN 4(When 이중 SC-05·06·10·22, 오류 없이 모호, SC-13/15 겹침)"
    reject_type: "VALIDATOR_FAIL 2/3"
  - phase: "2a"
    round: 1
    outcome: "FAIL — V11(재제출 시 서버 오류 문구 초기화 시나리오 없음, 72바이트 한글 비밀번호 경계 미결정) · WARN 5(When 이중, 관찰 불가 Then, Plan 밖 규칙)"
    reject_type: "VALIDATOR_FAIL 1/3"
---
