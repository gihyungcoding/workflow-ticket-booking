---
status: ARCHIVED
saved_at: 2026-11-12T00:00:00Z
summary: "SCENARIO_TASK-024 — 17건(happy 3 / error 4 / boundary 6 / regression 4), AC 6/6, 입력 경계 40칸. 독립검증 3회차 PASS(경고 8) — Phase 4 RC-1 롤백 반영"

next_steps:
  - "Phase 2b — SC-17 Red(구현이 있는 상태 — 지금 구현에서 실제로 실패해야 한다) → 통합 HITL 재승인"

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

  - decision: "검증 경고 반영 — V5: SC-13 f 를 구체 문구로. V8: PLAN auth_state 제약에 AuthProvider 복원 규칙(401 만 토큰 삭제, 두 역할 밖 role 은 user 없음)을 근거로 적음. V11: leave n/a 사유를 PLAN 출처로. V12-1: SC-16(무효 토큰 + 공개 조회) 추가. V12-4: SC-10 에 취소 대화상자 닫힘. V12-5: SC-02 j('Bearer tok-1' 저장값) 추가 → prefix_only covered"
    rationale: "싸게 고칠 수 있고 관찰 가능한 단언이 생긴다"
  - decision: "검증 경고 보류 — V6(다중 When: 한 흐름의 순차 단계, 쪼개면 같은 Given 을 반복), V7(SC-01~03 은 http.ts 단위 동작이라 함수 호출이 입구, SC-13 의 MSG 상수는 기존 테스트 파일의 이름), V9(본문 무관 판별을 못 박으려는 의도적 행), V12-2·3·6(연속 클릭 401·수정 화면 GET 실패·다른 탭 — 기존 동작이거나 Task C 범위 밖), V4(SC-04 flow 'F1, F2' 표기)"
    rationale: "HITL 에서 사람이 판단"

  - decision: "RC-1 롤백(RETRY_SCENARIO 2/3) — 요청 토큰의 정본을 화면 세션으로. 다른 탭의 저장소 변경은 이 탭 요청을 바꾸지 않는다(SC-17 e·f)"
    rationale: "저장소 우선이면 다른 탭의 다른 계정 로그인이 이 탭 요청 명의를 바꾼다 — 화면과 요청이 갈라지는 같은 뿌리"
  - decision: "3회차 경고 반영 — V1 머리말(RequireOrganizer·AuthContext), V8 PLAN session_token source 에 소셜 콜백 경로·storage_token 이름 주석, V12-1 SC-17 행 g(정상 로그아웃). 보류 — V5·V6·V7·V9(a·b 는 예외 종류만 다름 — 쿼터를 명시해 두는 의도), V12-2·3(재로그인 순간·복원 중 요청은 SC-11·SC-08 범위)"
    rationale: "싸게 고칠 수 있는 것만"

attempts:
  - n: 1
    result: PASS
    note: "fail 0 · warn 7 — 재검증 없이 경고 반영"
  - n: 2
    result: PASS
    note: "2b 에서 RETRY_SCENARIO 1/3 — SC-09 Then '전역 헤더 비로그인 모양' 이 /login(인증 화면, 헤더 없음)에서 관찰 불가 → 로그인 화면 제목으로. fail 0 · warn 5(V5·V6·V7·V9·V12)"
  - n: 3
    result: PASS
    note: "Phase 4 1회차 RC-1 → RETRY_SCENARIO 2/3. SC-17 추가. fail 0 · warn 8"

---

# CP-2.2 시나리오 — TASK-024

- `workflow_design/05_scenario/SCENARIO_TASK-024.md`(SoT) · `.json`
- 승인 테스트 변경: http · WriteKeyRejected(삭제) · AuthScreens · task018 · SocialLoginScreens · App.test
