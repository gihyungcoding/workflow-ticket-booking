# TASK-005 시나리오

## SC-01 (happy) 등록 폼 제출 시 공연이 생성되고 상세 화면으로 이동한다

- **Given** 등록 화면(`/performances/new`)이 열려 있다
- **And** 공연명 "가을 재즈 콘서트", 장소 "OO홀", 공연일시 "2026-10-01T19:00",
  오픈 "2026-09-10T10:00", 마감 "2026-09-30T23:59" 이 입력되어 있다
- **And** 구역 1개(등급 "VIP", 가격 120000, 행 A~B, 행당 좌석수 10)가 입력되어 있다
- **And** `POST /api/performances` 가 201과 `{ id: 1, ...totalSeats: 20 }` 을 반환하도록
  되어 있다
- **When** [등록] 버튼을 클릭한다
- **Then** `POST /api/performances` 요청 본문에 입력한 기본 정보와 `sections: [{ grade:
  "VIP", price: 120000, rowStart: "A", rowEnd: "B", seatsPerRow: 10 }]` 가 담겨 전송된다
- **And** 화면이 `/performances/1` 로 이동한다

covers: 등록 폼에서 기본 정보 + 구역 1개 이상을 입력하고 제출하면 공연이 생성되고 상세 화면으로 이동한다
flow: F1

## SC-02 (happy) 구역을 추가하면 생성될 좌석 수 미리보기가 즉시 갱신된다

- **Given** 등록 화면이 열려 있다
- **And** 구역 1개(등급 "VIP", 행 A~B, 행당 좌석수 10 → 20석)가 입력되어 있다
- **And** 좌석 수 미리보기에 총 20이 표시되어 있다
- **And** [+ 구역 추가] 버튼을 눌러 빈 구역 카드가 하나 더 추가되어 있다(등급/행/행당
  좌석수는 아직 비어 있음)
- **When** 새 구역 카드에 등급 "R", 행 C~E, 행당 좌석수 20을 입력한다
- **Then** API 호출 없이 좌석 수 미리보기의 총합이 80(VIP 20 + R 60)으로 갱신된다
- **And** 구역별 미리보기에 VIP 20, R 60 이 각각 표시된다

covers: 구역을 추가하면 "생성될 좌석 수" 미리보기가 즉시 갱신된다
flow: F1

## SC-03 (error) 시각 순서 위반 응답을 받으면 필드 아래 인라인 오류가 표시된다

- **Given** 등록 화면에 기본 정보 + 구역 1개가 입력되어 있다
- **And** `POST /api/performances` 가 400과 `{ code: "INVALID_TIME_ORDER" }` 를 반환하도록
  되어 있다
- **When** [등록] 버튼을 클릭한다
- **Then** 시각(공연일시/오픈/마감) 입력 영역 아래에 "예매 오픈·마감·공연 일시 순서가
  올바르지 않습니다" 오류 메시지가 표시된다
- **And** 화면은 여전히 등록 폼이다(다른 라우트로 이동하지 않는다)

covers: 시각 순서 위반(400) 응답을 받으면 관련 필드 아래 인라인 오류가 표시된다
flow: F2

## SC-04 (error) 좌석 총수 초과 응답을 받으면 해당 구역 아래 오류가 표시된다

- **Given** 등록 화면에 기본 정보 + 구역 1개(등급 "R", 행 A~Z, 행당 좌석수 200 → 5,200석)가
  입력되어 있다
- **And** `POST /api/performances` 가 400과 `{ code: "SEAT_LIMIT_EXCEEDED" }` 를 반환하도록
  되어 있다
- **When** [등록] 버튼을 클릭한다
- **Then** 그 구역 카드 아래에 "좌석 총수는 5,000석을 넘을 수 없습니다" 오류 메시지가
  표시된다

covers: 좌석 총수 초과(400) 응답을 받으면 해당 구역 아래 오류 메시지가 표시된다
flow: F3

## SC-05 (error) 등록 API 호출이 실패하면 폼 상단에 오류와 다시 시도가 표시된다

- **Given** 등록 화면에 기본 정보 + 구역 1개가 입력되어 있다
- **And** `POST /api/performances` 요청이 네트워크 오류로 거부되도록 되어 있다
- **When** [등록] 버튼을 클릭한다
- **Then** 폼 상단에 "등록하지 못했습니다. 다시 시도해 주세요" 오류 메시지가 표시된다
  (문구는 feature 문서 §2-5 그대로)
- **And** [다시 시도] 버튼이 표시된다

covers: 등록 API 호출이 실패(5xx/네트워크)하면 폼 상단에 오류와 [다시 시도] 가 표시된다
flow: F4

## SC-06 (happy) 수정 화면 진입 시 기존 값과 좌석 구성 요약이 표시된다

- **Given** `GET /api/performances/5` 가 200과 `{ id: 5, title: "가을 재즈 콘서트", venue:
  "OO홀", startAt: "2026-10-01T19:00:00+09:00", openAt: "2026-09-10T10:00:00+09:00",
  closeAt: "2026-09-30T23:59:59+09:00", sections: [{ grade: "VIP", price: 120000,
  seatCount: 20 }] }` 를 반환하도록 되어 있다
- **When** 수정 화면(`/performances/5/edit`)에 진입한다
- **Then** 공연명 입력란에 "가을 재즈 콘서트"가, 장소 입력란에 "OO홀"이 채워져 표시된다
- **And** 좌석 구성 요약 영역에 "VIP · 120,000원 · 20석" 텍스트가 표시된다
- **And** 그 영역에는 값을 고칠 수 있는 입력 요소(input/textarea 등)가 없다

covers: 수정 화면 진입 시 기존 값이 폼에 채워지고 좌석 구성 요약이 읽기 전용으로 표시된다
flow: F5

## SC-07 (error) 오픈 이후 수정하려 하면 전용 안내가 표시된다

- **Given** 수정 화면에 기존 값이 채워져 있다
- **And** `PUT /api/performances/5` 가 409와 `{ code: "REGISTRATION_ALREADY_OPEN" }` 를
  반환하도록 되어 있다
- **When** [저장] 버튼을 클릭한다
- **Then** "이미 오픈된 공연은 기본 정보를 수정할 수 없습니다" 안내가 표시된다

covers: 오픈 이후 공연을 수정하려 하면(409) "이미 오픈된 공연은 기본 정보를 수정할 수 없습니다" 안내가 표시된다
flow: F6

## SC-08 (happy) 취소 버튼을 클릭하면 확인 다이얼로그가 뜬다

- **Given** 수정 화면이 열려 있다
- **When** [공연 취소] 버튼을 클릭한다
- **Then** "이 공연을 취소하시겠습니까?" 확인 다이얼로그가 표시된다

covers: 취소 버튼 클릭 시 확인 다이얼로그가 뜨고, 확인하면 상태가 취소로 반영된다
flow: F7

## SC-09 (happy) 확인 다이얼로그에서 확인하면 취소 상태가 반영된다

- **Given** 수정 화면에서 [공연 취소] 버튼을 클릭해 확인 다이얼로그가 열려 있다
- **And** `POST /api/performances/5/cancel` 이 200과 `{ id: 5, ...status: "CANCELLED" }`
  를 반환하도록 되어 있다
- **When** 다이얼로그의 확인 버튼을 클릭한다
- **Then** `POST /api/performances/5/cancel` 요청이 전송된다
- **And** 화면의 상태 표시 영역에 "공연취소" 텍스트가 표시된다

covers: 취소 버튼 클릭 시 확인 다이얼로그가 뜨고, 확인하면 상태가 취소로 반영된다
flow: F7

## SC-10 (regression) 새 등록 라우트가 기존 상세 라우트를 가리지 않는다

- **Given** App 에 `/performances/new`(신규)와 `/performances/:id`(기존)가 함께 등록되어
  있다
- **When** `/performances/new` 로 진입한다
- **Then** 공연명 입력란(라벨 "공연명")이 화면에 표시된다 — 등록 폼에만 있고 상세
  화면(PerformanceDetailPage)에는 없는 요소다
- **And** `GET /api/performances/new` 요청이 발생하지 않는다(상세 페이지로 오인되지
  않았다는 증거)

covers: (acceptance_criteria 없음 — 회귀 검증, PLAN F8)
flow: F8

---

## 커버리지 메모

- SC-08/SC-09는 같은 acceptance_criteria(취소 버튼→확인 다이얼로그→확인→반영)를 나눠
  covers 한다. AC 원문 자체가 "확인 다이얼로그가 뜨고, 확인하면 반영된다"는 두 단계
  행위를 하나로 묶어 표현하고 있어, GWT의 "When은 하나" 규칙(gwt-canonical.md §1)을
  지키려면 시나리오를 분리해야 했다.
- PLAN.unresolved에 남긴 대로, EMPTY_SECTIONS/INVALID_SECTION/INVALID_REQUEST/
  DUPLICATE_SEAT_RANGE 4개 에러 코드와 수정 화면의 404(PERFORMANCE_NOT_FOUND)는
  전용 시나리오를 두지 않는다 — acceptance_criteria에 없는 범위이며, 발생 시 SC-05와
  동일한 코드 경로(폼 상단 일반 실패 배너)를 타므로 SC-05가 그 경로 자체는 이미
  검증한다.
- SC-10(regression)을 추가했다 — scenario-validator V10이 두 차례 모두 지적한 대로,
  `App.tsx`에 정적 라우트(`/performances/new`)와 기존 동적 라우트(`/performances/:id`)가
  공존하게 된다. React Router v7(ADR-0008)은 정적 세그먼트를 동적 파라미터보다 항상
  우선 매칭하도록 라우트를 랭킹하므로 선언 순서와 무관하게 충돌 위험은 낮지만, 이를
  실제로 검증하는 시나리오가 없었다. 사용자 승인(HITL#1 사전 확인) 하에 PLAN에 F8을
  추가하고 SC-10으로 검증한다.
- `performances.ts`의 `PerformanceNotFoundError`/새 `ApiError` 공존이나 `Performance`
  타입에 `sections?` 옵셔널 필드를 추가하는 것 자체는 기존 함수 시그니처를 바꾸지
  않으므로 별도 회귀 시나리오를 두지 않는다 — 옵셔널 필드 추가는 하위 호환이다.

## 독립검증 반영 이력

3회 검증했다(VALIDATION_TASK-005.json은 3차 결과를 저장 — 1·2차는 재검증으로 대체됨,
overall.pass는 3회 모두 true).

**1차 → 2차 사이 반영**
- V5 (Then 관찰 가능성) — SC-03/SC-04/SC-05 의 Then에 구체적인 한국어 오류 문구를
  추가했다. SC-05는 feature 문서 §2-5의 문구를 그대로, SC-03/SC-04는 백엔드 예외
  메시지(`InvalidTimeOrderException`/`SeatLimitExceededException`)가 개발자 지향이라
  그대로 쓰지 않고 사용자 지향 문구로 다시 썼다 — 프론트가 `code`로 분기해 자체
  문구를 표시한다는 뜻이다.
- V6 (When 단일성) — SC-02의 "구역 추가+입력"을 "구역 추가(Given)"와 "새 구역에
  값 입력(When)"으로 분리했다.

**2차 → 3차 사이 반영**
- V1 (MD↔JSON 불일치) — SC-02/SC-05의 괄호 부연을 JSON에도 동일하게 반영했다.
- V5 잔여 — SC-06 "읽기 전용" 을 "입력 요소가 없다"로, SC-09 "상태가 반영된다"를
  "상태 표시 영역에 텍스트가 표시된다"로 관찰 가능하게 다시 썼다.
- V10 (회귀 누락) — 사용자에게 확인(AskUserQuestion)한 뒤 승인받아 PLAN에 F8(기존
  상세 라우트 보존) 을 추가하고 SC-10(regression) 을 신설했다. React Router v7
  (ADR-0008)은 정적 세그먼트를 동적 파라미터보다 항상 우선 매칭하도록 라우트를
  랭킹하므로 `/performances/new` 와 `/performances/:id` 공존 자체의 충돌 위험은
  낮지만, 이를 실제로 검증하는 시나리오가 없었다.

**3차에서 반영**
- V5/V7 (SC-10의 컴포넌트명 노출 + 관찰 불가) — Then을 "등록 폼(PerformanceRegisterPage)
  요소가 렌더된다"에서 "공연명 입력란(라벨 \"공연명\")이 화면에 표시된다 — 등록
  폼에만 있고 상세 화면에는 없는 요소다"로 다시 썼다.
- V8 (PLAN outputs 미정의) — SC-03/SC-04 오류 문구와 SC-06 좌석 요약 표기 형식
  (`{grade} · {price}원 · {seatCount}석`, feature 문서 §2-4 와이어프레임과 동일)을
  PLAN_TASK-005.json 의 `design.outputs` 에 추가해 시나리오와 PLAN을 다시 일치시켰다.
- V10 (문서 모순) — 이 섹션 자체가 2차 검증 시점 그대로 남아 있던 문제. 지금 이
  갱신으로 해소한다.

**반영하지 않음, 사람 판단으로 남김**
- V8의 "EMPTY_SECTIONS 등 4개 코드는 시나리오 없음" 관련 지적은 PLAN.unresolved에서
  이미 근거를 남긴 의도된 설계 결정이라 추가 반영하지 않았다.
