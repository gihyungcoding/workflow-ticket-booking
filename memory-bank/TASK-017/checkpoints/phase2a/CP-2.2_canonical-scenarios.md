---
status: ARCHIVED
saved_at: 2026-11-05T02:30:00Z
summary: "18건 — 독립검증 수렴 안 됨, 사용자 게이트 예외로 2b 진행 (SCENARIO json gate_exception)."

next_steps:
  - "Phase 2b — Red, 기존 테스트 기대값 교체, HITL#1·2 에서 시나리오 전체 승인"

decisions:
  - decision: "새로 작성"
    rationale: "테마·부품은 새 구현. 옛 테마 테스트(TASK-003)의 기대값은 2b 에서 교체"
  - decision: "서체 로드·375px 가로 넘침은 '(화면 확인)' Then 으로 — jsdom 이 계산하지 않는다"
    rationale: "테스트는 그 상태를 만드는 설정(링크·#root 스타일)을 확인, 실제 결과는 Phase 4 브라우저 증거"
  - decision: "neutral 배지 바탕은 #F1F2F6 (흰 블록 위 --c-fill)"
    rationale: "시안의 배지가 모두 흰 블록·행 위에 놓인다"

  - decision: "1회차 경고 반영: SC-05 Then 을 '렌더가 예외 없이 끝나고 그린 영역이 비어 있다'로(V5), PLAN 에 토스트 빈 문구·onClose·onBack·Sheet onClose 명시(V8)"
    rationale: "검증자 V5·V8"
  - decision: "보류: V6(시간 진행·설정 켬/끔을 표 행으로 둔 것), V7(테마 키·CSS 변수명 — 이 태스크는 테마 자체가 대상), V9(SC-03·04 색 중복 — 매핑 vs 색)"
    rationale: "2b 통합 HITL 에서 판단"

attempts:
  - round: 1
    result: FAIL
    fail: ["V11 — Toast 언마운트 정리·문구 변경 재시작·긴 문구, Field 오류 꺼짐·빈 문구"]
    fix: "SC-14(토스트 타이머·긴 문구), SC-15(Field 오류 꺼짐·빈 문구 + 같은 종류인 Sheet 닫힘). PLAN inputs 에 토스트·Field 오류 제약"
---
  - round: 2
    result: FAIL
    fail: ["V11 — Field 오류 공백만, 토스트 표시 중 빈 문구 전환, 빈 문구의 onClose"]
    fix: "SC-07 에 onClose 미호출, SC-14 에 빈 문구·공백 전환 2행, SC-15 에 Field 공백만·Sheet 배경/Esc/안쪽 클릭. 경고 반영: PLAN 에 정의 밖 상태값·Sheet 닫기 규칙(V8), SC-13 을 목록+상세 표로·메타 Then 제거(V5·V10), Field 아이콘 aria-hidden 단언(V5), 기존 테스트 기대값 교체·화면 확인 증거 방법을 「쓰지 않은 것」에(V5). 같은 종류 대조: 켬→꺼짐 부품은 Field·Toast·Sheet 셋"
  - round: 3
    result: FAIL → Phase 1 롤백 1/2 (사용자 결정)
    fail: ["V11 — StatusBadge·Toast·Field null/undefined, Toast 빈→유효, Sheet 닫힌 뒤·언마운트 뒤 Esc"]
    fix: "PLAN design.prop_schema. SC-05·07 을 표(정의 밖·빈·null·undefined)로, SC-13 에 공연 수정 화면, SC-16(prop_schema 잔여 칸 전수: Toast 무효→유효·같은 문구 재렌더·onClose 없음, Field null/undefined→유효·label, Sheet 닫힌 뒤/언마운트 뒤 Esc·onClose 없음, AppBar onBack/title 없음, CtaBar disabled, Badge 빈 문구). 검증 회차는 롤백 후 1회차부터"
  - round: "롤백 후 1"
    result: FAIL
    fail: ["V11 — Toast·Field 유효↔무효 전환 10칸"]
    fix: "SC-17: 무효 4값 × 두 방향 × Toast·Field = 16행 전수. 경고 반영: SC-13 에 공연 등록·App 첫 화면·createAppTheme 호출, 문구 '표의 모든 화면'(V10). 보류: V5·V6·V7, 시나리오 17건(복잡 기준선 초과)은 2b 통합 HITL 에서 알림"
  - round: "롤백 후 2"
    result: FAIL
    fail: ["V11 — Field.label 무효, Field.error·AppBar.title·Badge 긴 문구, AppBar.title 빈·공백"]
    fix: "PLAN prop_schema 에 길이 규칙(Field.error 줄바꿈, AppBar.title 줄바꿈·버튼 칸 불침범, Badge nowrap+말줄임)과 label·title 무효 규칙. SC-18 추가. SC-13 App 행 기대값을 구체 문구로(V5). 같은 종류 대조: 사람이 넣는 문구 prop 은 Toast.message·Field.label·Field.error·AppBar.title·Badge.children·CtaBar label — CtaBar label 은 고정 문구라 시안 문구만"
  - round: "롤백 후 3"
    result: FAIL (missing 1칸 — StatusBadge 공백만)
    fix: "SC-05 표에 공백만 행. 사용자 예외 승인(2026-11-05, AskUserQuestion: '예외 승인 — 한 칸 채우고 4회차 검증')으로 재시도 상한 초과 후 같은 Phase 에서 4회차 — PLAN 변경 없음"
  - round: "예외 4"
    result: FAIL (Field.label 긴 값, Toast 지속 시간 prop)
    fix: "Toast 지속 시간 고정 상수 확정, SC-18 label 100자. 사용자 게이트 예외(2026-11-05) — 검증자 재호출 없이 2b. 보상: HITL#1·2 에서 시나리오 전체 승인"
