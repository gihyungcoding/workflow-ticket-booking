---
status: ACTIVE
saved_at: 2026-11-13T00:00:00Z
summary: "시나리오 39건, 입력 경계 칸 120/120(n/a 11). 독립검증 2회차 PASS(경고 6) — Phase 2b 진입 가능"

next_steps:
  - "Phase 2b — wf-red. 프론트 가짜 타이머 선례가 없다(userEvent 는 advanceTimers 옵션) — 사용법을 먼저 정한다. 승인 테스트 8행 교체·수정 포함"
  - "통합 HITL 요약에 아래 decisions 와 보류한 경고를 보여준다 — 특히 SC-04(계약 위반 → 오류 화면, startAt 행 au~aw 는 PLAN 이 표시 전용으로 둔 값이라 범위 판단 필요)·SC-30 d·SC-39·카운트다운 경계 표기"

decisions:
  - decision: "새로 작성(기존 확장 아님). MD·JSON·행렬을 한 데이터 스크립트에서 생성해 일치시킨다"
    rationale: "시나리오 39건·칸 120개라 손으로 맞추면 V1(MD↔JSON) 어긋남이 난다. 이후 수정은 스크립트 데이터를 고쳐 다시 생성"
  - decision: "응답 계약 위반(status·시각·availableSeats·sections 원소)은 오류 화면 — SC-04"
    rationale: "PLAN 이 입력으로 올렸으나 동작을 정하지 않았다. 흰 화면·예외 대신 기존 오류 화면을 쓴다. sections 키 없음·null·[] 는 오류가 아니라 가격 줄·구역 행 숨김(SC-18)"
  - decision: "표시 문자열 공백 판정은 ADR-0027(유니코드 공백·NBSP·U+200B) — grade·area 공백은 계약 위반, areaNote 공백은 설명 줄 없음, AppBar title 공백은 h1 없음"
    rationale: "현재 isBlank 는 trim() 이라 U+200B 를 못 거른다 — 구현이 ADR-0027 판정을 따라야 한다(SC-04 note)"
  - decision: "카운트다운 경계 표기: 두 단위를 보이고 작은 단위가 0 이어도 적는다('1일 0시간'·'1시간 0분'·'1분 0초'), 초 이하는 버린다"
    rationale: "시안·기능 문서는 예 네 개만 정한다(SC-08). 2b 승인에서 사람이 본다"
  - decision: "화면 복귀 재측정이 전부 실패하면 이전 오프셋을 유지한다(SC-30 d)"
    rationale: "일시적 실패가 잘 돌던 카운트다운을 죽이지 않게. 기기 시계 대체가 아니므로 기능 문서 §8 과 충돌하지 않는다. 처음 측정이 전부 실패한 경우만 '기준 시각을 맞추지 못했어요'(SC-10)"
  - decision: "배치도 접근성 이름은 일반 규칙('<공연장> 구역 위치. 무대 바로 앞부터 <첫 묶음 등급> 순서이고, 바깥으로 <묶음 이름들> 순서예요')"
    rationale: "시안 문장은 한 공연에 맞춘 것이고 조사(이/가)를 임의 이름에 맞출 수 없다"
  - decision: "묶음 계산: stageOrder 숫자 오름차순(같은 값은 응답 순서), 이어진 같은 area 만 한 묶음, 묶음 안 가격이 다르면 '최소~최대원'. 배치도 라벨은 12자 초과 시 11자 + '…'"
    rationale: "PLAN unresolved — 문서에 없는 규칙을 이 문서가 정한다(SC-22·23·24)"
  - decision: "id 는 요청 경로에 encodeURIComponent 로 끼운다(SC-06). 상세 앱바 뒤로 → '/', 대기열 뒤로·공연으로 돌아가기 → /performances/:id"
    rationale: "id 에 ?·#·/ 가 있으면 요청이 갈라진다. 뒤로 목적지는 시안 링크(home.html)를 따른다"
  - decision: "AC-14(375px 시안 대조)는 jsdom 이 계산하는 스타일(SC-33)과 Phase 4 브라우저 증거로 나눈다"
    rationale: "레이아웃·겹침은 jsdom 이 계산하지 않는다"
  - decision: "상태 표 대조 — 상세 12상태(예정·0초 전환·예매 가능·매진·마감·취소·로딩·없는 공연·오류·시각 실패·포스터 없음·구역 1개)와 시트 3상태(기본·미등록·묶음 5개 이상)가 모두 시나리오에 걸린다"
    rationale: "§2-5 표 한 행씩 대조: SC-07·09·13·15·01·02·03·10·17·18·19·22·21"
  - decision: "경고 반영 — SC-14 오타(행 h)·SC-22 f 를 구역 둘로·SC-23 라벨 규칙(접미사 제외, areaNote 는 배치도에 없음)·SC-11 실패 표본 지연·SC-30 b 수치·When 단일화(SC-07·10·37)·SC-28 마지막 Then 을 SC-29 e 로·SC-04 에 closeAt/startAt 해석 불가 행·SC-17 에 문자열 아닌 posterUrl·SC-14 i 에 30일(타이머 상한)"
    rationale: "1회차 V5·V6·V12 경고 중 값싸게 고칠 수 있는 것. 보류: V7 구현 누출(SC-31·33 은 정적·스타일 점검이라 규약 표로 근거를 단다)·V9 중복·V12 (4)(5) 왕복 음수·서버 불일치"
  - decision: "보류한 경고 — V8: SC-04 '계약 위반 → 오류 화면'·SC-30 d '재측정 전부 실패 시 이전 오프셋 유지'·SC-39 '상세 뒤로 → /' 는 PLAN 이 정하지 않은 시나리오 단계의 결정이다 (PLAN 결정이라 쓴 규약 표 문구는 고쳤다)"
    rationale: "2b 통합 HITL 요약에서 사람이 판단한다. SC-30 d 는 AC-4(처음 측정 실패)와 다른 경우(이미 성공한 뒤 재측정 실패)라 충돌하지 않는다고 본다"

  - decision: "2회차 PASS 의 새 경고 — [신규] SC-04 행 au~aw(startAt 없음·null·해석 불가)가 PLAN 의 'startAt 은 표시 전용·입력표에서 제외'와 충돌한다는 지적"
    rationale: "1회차 V12 가 startAt 의 시각 파서 RangeError 위험을 제안해 넣었다. 유지하되 2b 통합 HITL 에서 사람이 판단한다(빼면 formatDateTime 이 해석 불가 값에 예외를 던질 수 있다)"
  - decision: "남은 경고(V5 콘솔 오류 감시 방법·V6 SC-05 e·f·SC-30 b·c·V7·V9·V12 posterUrl 형식·왕복 음수·title/venue 키 없음)는 재검증 없이 보류"
    rationale: "경고만이면 재검증하지 않는다(wf-scenario Step 4). 콘솔 감시는 2b 에서 정한다"

attempts:
  - n: 1
    outcome: "독립검증 1회차 FAIL — V11 false_cover price@F7:integer.zero (SC-22 행 f 가 구역 1개라 SC-18 d 에 따라 시트를 열 수 없음)"
    back_to: "2a"
---
