# TASK-008 시나리오

## SC-01 (happy) 상세 조회 시 구역별 좌석 요약이 포함된다

- **Given** 공연 P가 구역 2개로 등록되어 있다 — VIP(가격 120000원, 좌석 10석)와
  R(가격 80000원, 좌석 30석)
- **When** 사용자가 `GET /api/performances/{P.id}` 를 호출한다
- **Then** 응답 본문의 `sections` 배열에 `{grade: "VIP", price: 120000, seatCount: 10}`
  이 포함된다
- **And** 같은 배열에 `{grade: "R", price: 80000, seatCount: 30}` 이 포함된다

covers: 구역 2개(등급·가격이 다른)로 등록된 공연을 GET /api/performances/{id} 로 조회하면 응답의 sections 필드에 구역별 grade·price·좌석수가 각각 포함된다
flow: F1

## SC-02 (error) 존재하지 않는 공연 조회 시 404가 반환된다

- **Given** id=999 인 공연이 존재하지 않는다
- **When** 사용자가 `GET /api/performances/999` 를 호출한다
- **Then** 404가 반환된다
- **And** 응답 본문의 `code` 가 `"PERFORMANCE_NOT_FOUND"` 이다

covers: 존재하지 않는 공연 id로 GET /api/performances/{id} 를 호출하면 404와 PERFORMANCE_NOT_FOUND 가 반환된다 (기존 동작 유지)
flow: F2

## SC-03 (regression) 목록 조회 응답에는 sections 키가 없다

- **Given** 공연 P가 구역 1개 이상으로 등록되어 있고, `GET /api/performances` 결과 페이지에
  P가 포함된다
- **When** 사용자가 `GET /api/performances` 를 호출한다
- **Then** 응답 본문의 `content` 배열에서 P에 해당하는 항목에 `sections` 키가 없다

covers: GET /api/performances (목록) 응답은 sections 필드를 포함하지 않는다 — 상세 조회에만 추가한다
flow: F3

## SC-04 (boundary) 서로 다른 구역이 같은 등급·가격이면 좌석 수가 합산된다

- **Given** 공연 P가 구역 2개로 등록되어 있다 — 둘 다 등급 R·가격 80000원이지만 행 범위가
  달라 첫 구역은 좌석 10석, 둘째 구역은 좌석 20석이다
- **When** 사용자가 `GET /api/performances/{P.id}` 를 호출한다
- **Then** 응답 본문의 `sections` 배열에 `grade: "R", price: 80000` 인 항목이 하나만 있다
- **And** 그 항목의 `seatCount` 는 30이다

covers: 구역 2개(등급·가격이 다른)로 등록된 공연을 GET /api/performances/{id} 로 조회하면 응답의 sections 필드에 구역별 grade·price·좌석수가 각각 포함된다
flow: F1

---

## 커버리지 메모

- SC-04는 SC-01과 같은 acceptance_criteria/flow(F1)를 covers로 공유한다. PLAN의
  `unresolved` 항목("grade+price 조합이 같은 두 구역은 하나의 sections 행으로 합쳐진다")을
  명시적으로 검증하는 boundary 시나리오다 — SC-01(서로 다른 grade+price)만으로는 그룹핑
  키가 실제로 (grade, price) 조합인지, 혹은 구역 순서나 행 범위로 잘못 나뉘는지를
  구분하지 못한다.
- SC-03은 type을 regression으로 분류했다 — 목록 엔드포인트 자체는 이번 태스크가
  손대는 기능이 아니지만, PerformanceResponse에 필드를 추가하면서 목록 경로가
  실수로 영향받지 않는지 확인하는 성격이다.
