---
status: ARCHIVED
saved_at: 2026-11-11T00:00:00Z
summary: "PLAN_TASK-019.json — route Backend, flows 7건이 AC 7건을 전부 덮음. input_fields 7 → 칸 82. 변경 대상 19(신규 9)."

next_steps:
  - "Phase 2a — coverage_matrix.py --skeleton 으로 82칸을 1차 작성에서 전부 닫는다. unresolved #1(no-store 가짜 Red)·#7(posterUrl JSON 타입) 판정을 시나리오에 반영"

decisions:
  - decision: "기존 공연(zone 행 없음)은 기존 정렬(price DESC, grade ASC)을 유지하고 그 순서로 stageOrder 1..n, area=grade. V6 는 백필하지 않는다"
    rationale: "AC7 '기존 공연의 응답이 바뀌지 않는다' + feature §3-1 '기존 공연은 zone 행이 없다'"
  - decision: "새 등록은 zones 유무와 상관없이 등급마다 zone 행을 저장한다 — zones 가 null·빈 배열이면 sections 첫 등장 순서로 기본값"
    rationale: "AC2 '등록 순서대로 stageOrder' 를 조회 시 seat 순서 추론 없이 보장. 빈 배열은 '주지 않음' 과 같게"
  - decision: "GET /api/time 의 Cache-Control 은 컨트롤러가 CacheControl.noStore() 로 직접 설정하고, 테스트는 값이 정확히 'no-store' 인지 본다"
    rationale: "Security 기본 헤더가 이미 no-store 를 포함해 '포함' 단언은 구현 없이 통과한다"
  - decision: "posterUrl 은 Bean Validation 이 아니라 Service 에서 판정(INVALID_POSTER_URL). 빈 문자열도 같은 코드"
    rationale: "@Size 로 두면 501자가 INVALID_REQUEST 가 되어 AC4 와 어긋난다"
  - decision: "구역 오류는 펼친 grades 를 순서대로 훑어 처음 걸린 것(UNKNOWN 또는 DUPLICATED)을 낸다. 포스터 검증이 구역 검증보다 먼저, 둘 다 기존 서비스 검증 뒤"
    rationale: "기존 오류 응답 회귀 표를 바꾸지 않고, 동시 위반에 결정적인 코드를 준다"
  - decision: "Performance 기존 8인자 생성자는 유지하고 posterUrl 을 받는 생성자를 추가"
    rationale: "new Performance(…) 를 쓰는 승인 테스트 7개 파일을 고치지 않기 위해"
  - decision: "posterUrl 은 목록·상세 외에 등록·수정·취소·next-opening 응답에도 실린다(같은 toResponse) — 넓힌 범위로 기록"
    rationale: "응답 조립이 한 곳이라 따로 빼면 오히려 분기가 생긴다. 프론트(TASK-021 머리글)가 next-opening 의 posterUrl 을 쓴다"
  - decision: "area·note 에만 NoLoneSurrogate(ADR-0019). 기존 title·venue 미적용은 drift 로 남기고 손대지 않는다"
    rationale: "AC 밖. 회고에서 drift 로 올린다"

attempts: []
---

# CP-1.3 Phase 1 설계 정규화 — TASK-019

- 산출물: `workflow_design/04_plan/PLAN_TASK-019.json`
- 게이트: `check_plan.py` 통과(체크포인트 외 위반 0)
- 참조 ADR: 0003·0005·0006·0010·0019·0024·0026
- unresolved 9건 — PLAN 참조
