---
status: ACTIVE
saved_at: 2026-11-12T00:00:00Z
summary: "SCENARIO_TASK-024 — 15건(happy 3 / error 4 / boundary 5 / regression 3), AC 6/6, 입력 경계 34칸(covered 29 · n/a 5). 독립검증 대기"

next_steps:
  - "scenario-validator 1회차 → VALIDATION_TASK-024.json 원문 저장"

decisions:
  - decision: "새로 쓴다 — 기존 http.test·WriteKeyRejected.test 시나리오를 확장하지 않고 SC-01·02·09·10·12 로 대체한다"
    rationale: "쓰기 키 전제가 사라져 옛 시나리오의 Given 이 성립하지 않는다"
  - decision: "PLAN 입력 제약 보완 — stored_token 은 앞뒤 공백을 걷어 싣고, Headers 가 거부하는 값(NUL·CR/LF·Latin-1 밖)은 Authorization 없이 보낸다"
    rationale: "Node Headers 프로브: ' tok-1 ' → 'Bearer  tok-1'(가운데 공백 2개), 'tok\\u00001'·'토큰' → TypeError. 요청 자체를 잃지 않게 한다. 분류 보완이라 ROLLBACK_PLAN 아님"
  - decision: "SC-11 의 '화면 떠나기' 는 로그아웃(→ 가드가 화면을 내림) 후 재로그인으로 만든다"
    rationale: "전역 헤더에 다른 화면 링크가 없다(AppHeader.tsx). 재로그인 뒤 늦은 401 이 새 세션을 지우지 않는지가 관찰 가능한 차이다"
  - decision: "기능 문서 §2-5 상태 대조 — '등록·수정: 비로그인'→SC-05·06, '관객 역할'→SC-07·06, '남의 공연(수정)'→범위 밖(TASK-026, SC-12 d 가 현재 문구 유지를 고정)"
    rationale: "Task C AC 에 남의 공연 상태가 없다(PLAN unresolved #4)"
  - decision: "PLAN inputs 제약 대조 — stored_token(SC-02), init Authorization(SC-03), method(SC-01), auth_state(SC-04·05·07·08), protected_path(SC-06), from(SC-05·09), write_response 401/403 상태 판별(SC-09·10 d~f)"
    rationale: "모든 제약이 Then 에서 단언된다"
  - decision: "기존 승인 테스트 6파일 변경을 SC-13·SC-14 와 '기존 승인 테스트 변경' 표로 명시 — App.test SC-10·AuthScreens SC-28·task018 SC-36 은 Phase 1 이 놓친 충돌(비로그인 등록 화면)이라 추가"
    rationale: "가드가 생기면 비로그인으로 /performances/new 를 열어 폼을 단언하던 테스트가 깨진다"

attempts: []
---

# CP-2.2 시나리오 — TASK-024

- `workflow_design/05_scenario/SCENARIO_TASK-024.md`(SoT) · `.json`
- 승인 테스트 변경: http · WriteKeyRejected(삭제) · AuthScreens · task018 · SocialLoginScreens · App.test
