---
status: ACTIVE
saved_at: 2026-11-16T11:00:00Z
summary: "SCENARIO_TASK-021.md 29건(happy 7·error 6) 작성 — AC 9건 전부 매핑, 입력 경계 칸 152/152 판정(coverage_matrix 통과). 독립검증 전."

next_steps:
  - "scenario-validator 호출 → VALIDATION_TASK-021.json 에 그대로 저장. FAIL 이면 지적 kind(false_cover·bad_na·plan_gap)대로 고치고 같은 시나리오를 근거로 든 다른 칸도 전부 다시 본다"
  - "통과하면 Phase 2b — 승인 테스트 수정 7행(시나리오 맨 아래 표)을 2b 통합 승인(HITL#1·2)에 함께 올린다"

decisions:
  - decision: "새로 쓴 시나리오다(확장 아님). 규약 표 + 계약 위반 표 CV 를 정본으로 두고 후보·칩 응답·더 보기·첫 응답이 같은 표를 쓴다"
    rationale: "같은 검사를 시나리오마다 따로 쓰면 TASK-020 처럼 976줄이 된다. 칸은 SC 하나의 표로 묶는다(coverage-policy 「개수를 불리지 않는 법」)"
  - decision: "기준 시각이 없을 때(측정 중·실패) 「오늘」 문구는 기기 날짜로 가른다 (SC-08·09)"
    rationale: "상세 PerformanceDetailPage.tsx:249 의 선례. 문구용이고 켜는 동작이 아니다. 홈에는 카운트다운·CTA 가 없어 기기 시계를 믿어도 PRD 의 문제(기기별 어긋난 0초)가 생기지 않는다. 사람이 2b 승인에서 본다"
  - decision: "목록 응답의 원소 하나라도 계약을 어기면 응답 전체를 오류로 본다 — 정의 밖 status 포함 (SC-22)"
    rationale: "한 원소만 건너뛰면 totalElements 와 불러온 개수가 어긋나 더 보기가 영영 남는다. 상세 parseDetail 이 정의 밖 status 를 오류로 보는 것과 같다(StatusBadge 의 null 은 별개의 방어). 후보(next-opening) 위반은 후보 없음으로 본다(SC-05)"
  - decision: "목록 계약은 홈이 쓰는 필드(id·title·venue·startAt·openAt·status·availableSeats, content, totalElements)만이다 — closeAt·totalSeats·sections·page·size 는 계약이 아니다 (SC-29)"
    rationale: "AuthScreens·SocialLoginScreens 승인 테스트 픽스처가 목록에 sections 를 모양 없이 싣는다(area·stageOrder 없음). 쓰지 않는 값을 위반으로 보면 서버가 필드를 바꿀 때 멀쩡한 화면이 오류가 된다(TASK-020 SC-43 와 같은 이유)"
  - decision: "선택된 칩을 다시 누르면 다시 부르지 않는다 (SC-13). 칩을 눌러도 머리글(후보·시각)은 다시 부르지 않는다"
    rationale: "AC-3 은 「누르면 해당 status 로 첫 페이지를 다시 불러온다」 — 같은 칩 재클릭은 AC 가 말하지 않는다. 같은 요청을 반복하지 않는 쪽을 택했다. 2b 승인에서 사람이 본다"
  - decision: "더 보기 응답이 빈 content 면 더 보기를 감춘다(오류 아님), 같은 id 가 오면 먼저 보인 카드를 유지하고 한 번만 보인다 (SC-16 f·SC-18 b)"
    rationale: "빈 페이지를 계속 요청하는 루프를 막는다. 더 보기 판단은 서로 다른 공연 수 < totalElements"
  - decision: "다시 시도(목록)는 현재 칩의 status 로 page 0 을 다시 부른다. 로딩 중에도 칩 6개는 보인다 (SC-20·21)"
    rationale: "기능 문서 §2-5 가 로딩에서 카드 자리·머리글 자리만 말한다. 칩을 숨기면 눌려 있던 칩이 사라진다"
  - decision: "두 줄 말줄임(-webkit-line-clamp)과 375px 시안 나란히 대조는 jsdom 이 못 한다 — Phase 4 브라우저 증거 (SC-06·25)"
    rationale: "Then 에 관찰 불가한 것을 쓰지 않기 위해 데이터를 자르지 않음·overflow-wrap·계산된 값으로 나눴다"
  - decision: "PLAN acceptance_criteria 에 AC-n 접두사를 붙였다 (표기 정규화, 설계 변경 아님)"
    rationale: "게이트가 covers 를 PLAN acceptance_criteria 와 문자열 정확 일치로 대조한다. TASK-020 PLAN 과 같은 형식"
  - decision: "feature 문서 §2-5 홈 상태 9종을 한 행씩 대조했다 — 기본 SC-01·로딩 SC-20·빈 목록(칩별) SC-19·오류 SC-21·더 보기 실패 SC-17·머리글 후보 없음 SC-04·기준 시각 실패 SC-08·포스터 없음 SC-12·긴 공연명 SC-06"
    rationale: "상태 표 대조(wf-scenario Step 2)"
  - decision: "PLAN inputs.constraints 를 한 항목씩 대조했다 — 목록 content 원소 필드·sections 없음·totalElements SC-22·29·16, 실패는 오류 화면·더 보기 실패 유지 SC-21·17, 204·실패 SC-04, 기준 시각 실패 시 줄 숨김 SC-08, 칩 6개·aria-pressed SC-01·13, 20개씩 SC-15·16"
    rationale: "제약 없는 시나리오는 테스트가 없어 구현에서 지워도 실패하지 않는다"
  - decision: "Then 근거를 코드로 확인했다 — 스텁 미등록 키 404(test/task020.tsx stubServer), 홈이 전역 헤더를 그림(App.tsx:31-47), kstDay 비교(PerformanceDetailPage.tsx:250), 훅의 visible 재측정·오프셋 유지(useServerClock.ts), 서버 title 상한 200(RegisterPerformanceRequest.java:20), page×size int 한도(PerformanceService.java:100)"
    rationale: "기존 동작을 단언하는 행의 근거(TASK-022 교훈)"

attempts: []
---
