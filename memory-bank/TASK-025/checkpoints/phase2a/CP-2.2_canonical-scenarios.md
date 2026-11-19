---
status: ARCHIVED
saved_at: 2026-11-17T00:00:00Z
summary: "SCENARIO_TASK-025 — 시나리오 23건(happy 11·error 7·boundary 4·regression 1), AC 10/10 매핑, 입력 경계 칸 140/140, 독립검증 2회차 PASS(경고 5건). 사람 승인은 2b 끝 통합 HITL."

next_steps:
  - "Phase 2b — wf-red. 승인된 테스트 변경(task024.organizer.test.tsx SC-17·SC-17 보강)을 Red 와 함께 HITL#1·2 통합 승인에 올린다. 통합 HITL 요약에 반영·보류 경고(decisions)와 검증 회차(attempts)를 쓴다. SC-23 행 c 는 `--c-fill` 값으로 정했다"

decisions:
  - decision: "시나리오 23건 — 정책 기준선(보통 4~7건)을 넘는다"
    rationale: "입력 경계 칸이 115개이고 AC 10건 + 사용자 확정 확장 2건(더 보기·복원 중 로그아웃)이 있다. 같은 검사는 표로 묶었다(SC-08 응답 계약 위반 100여 행, SC-10 HTTP 결과, SC-13 칩 7종, SC-15 0건 안내). 분할 검토 신호이지만 AC 는 화면 하나(내 공연 목록)와 진입 경로로 한 덩어리다"
  - decision: "PLAN 분류 보완 1건 — 주최자 전용 안내의 앱바는 /organizer 에서만"
    rationale: "PLAN F11 은 등록·수정 가드 화면도 같은 컴포넌트를 쓴다고 했으나 그 화면들은 전역 헤더가 있어 앱바를 더하면 헤더가 둘이 된다. PLAN F11·RequireOrganizer 변경 문구를 고쳤다(ROLLBACK_PLAN 아님 — 설계 변경이 아니라 범위 안의 구체화)"
  - decision: "행 하나에 배지 하나 — 시안 우선 (기능 문서 §2-5 와 어긋남)"
    rationale: "design.md: 시안이 이긴다. 비공개(취소 아님)면 「비공개」만, 취소면 배지 없음, 그 외 상태 배지. 비공개이면서 OPEN(DO) 행으로 고정했다"
  - decision: "머리글 N 은 불러온 행에서 센다"
    rationale: "서버가 비공개 총수를 주지 않는다(응답에 없음). 더 보기로 행이 늘면 N 이 는다 — SC-17 행 k 가 지킨다"
  - decision: "시안의 취소·마감 비링크 행은 따르지 않고 모든 행을 링크로 한다"
    rationale: "AC 8(행 선택 → 상세)과 상세 시안(organizer-cancelled·show--done)이 있다. Phase 4 시안 대조에서 의도된 변종으로 적는다"
  - decision: "승인된 테스트 변경 1파일 — task024.organizer.test.tsx SC-17 표의 email·social 행과 SC-17 보강"
    rationale: "주최자 로그인 직후 기대 주소가 `/` 에서 `/organizer` 로 바뀐다(SC-02 행 a·SC-03 행 a). 다른 파일은 Grep 으로 해당 없음 확인"
  - decision: "PLAN 분류 보완 2번째 — input_fields 에 list_response.item.id·more_page 추가 (1회차 V11 plan_gap)"
    rationale: "design.inputs 에는 있었으나 input_fields 에서 빠졌다. 칸 25개가 늘어 140개. ROLLBACK_PLAN 으로 세지 않는다(분류 보완)"
  - decision: "1회차 경고 5건 중 반영 — V5 의 모호한 Then 3건(SC-06 배지 문구·SC-17 앞 행 순서·SC-22 영구 null), V12 제안 4건(SC-17 행 l·m·n, SC-07 행 n)"
    rationale: "Then 을 단언 가능한 문장으로 바꾸고, 필터가 바뀔 때 쪽 번호가 처음부터 세어지는지·필터 사이 행이 섞이지 않는지·`items` 가 총수보다 많을 때·이름의 HTML 특수문자를 시나리오로 가져왔다"
  - decision: "1회차 경고 중 보류 — V6(When 둘)·V7(구현 이름)·V9(중복)·V12 (4)·(6)·(7)"
    rationale: "V6: 지연 응답과 복원 흐름은 한 시나리오의 시간 순서라 쪼개면 같은 Given 을 반복한다. V7: SC-14 는 API 함수 경계(화면이 만들지 않는 인자)를 지키는 시나리오라 함수명이 곧 입구다 — SC-22 의 logout 은 앱 안에서 로그아웃을 부르는 유일한 입구다(화면에 로그아웃 단추가 /organizer 에 없다). SC-23 의 CSS 는 regression·prop_schema 규약. V9: 행 b 는 같은 상태 다른 code 라는 의도가 있다. V12 (4) 연타 이중 이동·(6) N 이 큰 경우·(7) 복원 중 401 겹침은 PLAN 입력이 아니고 기존 SC-19·SC-20 이 같은 세대 규칙으로 닫는다. 사람 판단 항목은 통합 HITL 에 올린다"
  - decision: "n/a 칸 22건 — 근거는 PLAN·JSON.parse 표기·상한 없음"
    rationale: "role 정의 밖 값(UserRole 이 둘뿐) 5, F11 from 앞뒤 문자 1, totalElements·좌석 수·id·more_page 의 integral_fraction·exponent(JSON.parse 가 같은 수로 읽음)·max+1(상한 없음)"

attempts:
  - attempt: 2
    result: PASS
    reason: "plan_gap 2건 해소, 140칸 false_cover·bad_na·plan_gap 없음. 경고 5건(V5 SC-23 행 c 부정형·V6·V7·V9·V12)"
  - attempt: 1
    result: FAIL
    reason: "V11 plan_gap 2건 — 응답 항목 id·더 보기 쪽 번호가 PLAN input_fields 에 없다. 경고 5건(V5·V6·V7·V9·V12)"
---

# CP-2.2 시나리오 — TASK-025

- 산출물: `workflow_design/05_scenario/SCENARIO_TASK-025.md`·`.json`
- 독립검증 결과: `workflow_design/05_scenario/validator/VALIDATION_TASK-025.json` (2회차 PASS)
- V5 경고 중 SC-23 행 c 는 반영(토큰 `--c-fill` 값으로 단언). 재검증하지 않았다
