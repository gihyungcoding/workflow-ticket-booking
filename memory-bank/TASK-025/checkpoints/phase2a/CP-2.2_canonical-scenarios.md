---
status: ACTIVE
saved_at: 2026-11-17T00:00:00Z
summary: "SCENARIO_TASK-025 작성 — 시나리오 23건(happy 10·error 6·boundary 5·regression 1 + 일부 표), AC 10/10 매핑, 입력 경계 칸 115/115. 독립검증 대기."

next_steps:
  - "scenario-validator 독립검증(1회차) → 통과하면 Phase 2b"

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
  - decision: "n/a 칸 8건 — 근거는 PLAN·JSON.parse 표기·상한 없음"
    rationale: "role 정의 밖 값(UserRole 이 둘뿐) 5, F11 from 앞뒤 문자(RequireOrganizer 가 만든 값) 1, totalElements·좌석 수 integral_fraction·exponent(JSON.parse 가 같은 수로 읽음)·max+1(상한 없음)"

attempts:
  - attempt: 1
    result: FAIL
    reason: "V11 plan_gap 2건 — 응답 항목 id·더 보기 쪽 번호가 PLAN input_fields 에 없다. 경고 5건(V5·V6·V7·V9·V12)"
---

# CP-2.2 시나리오 — TASK-025

- 산출물: `workflow_design/05_scenario/SCENARIO_TASK-025.md`·`.json`
- 독립검증 결과: `workflow_design/05_scenario/validator/VALIDATION_TASK-025.json` (아직 없음)
