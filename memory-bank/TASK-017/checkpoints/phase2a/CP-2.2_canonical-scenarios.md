---
status: ACTIVE
saved_at: 2026-11-05T02:30:00Z
summary: "SCENARIO_TASK-017 15건. 1회차 V11 FAIL → SC-14·15 추가, 2회차 검증 대기."

next_steps:
  - "scenario-validator 독립검증"

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
