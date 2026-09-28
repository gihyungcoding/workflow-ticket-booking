# TASK-006 시나리오

## SC-01 (error) price에 소수를 보내면 등록이 거부된다

- **Given** 공연 등록 요청에 구역 하나(price=100.5, 나머지 필드는 유효)가 있다
- **When** POST /api/performances 를 호출한다
- **Then** 응답 상태는 400이다
- **And** 응답 코드는 INVALID_SECTION이다
- **And** seat 테이블에 새로 생성된 행이 없다 (요청 전후 전체 seat 개수가 그대로다)

covers: section.price가 정수가 아닌 값(예: -0.5)이면 400과 INVALID_SECTION이 반환되고 좌석이 생성되지 않는다
flow: F1

> price 예시값은 AC 원문의 -0.5 대신 100.5를 쓴다 — -0.5는 "정수가 아님"과 "0 미만"을 동시에
> 위반해 어느 검사가 걸렸는지 응답 코드만으로 구분되지 않는다. 100.5는 0 이상이므로
> 새로 추가하는 정수 검증만을 분리해서 확인한다 (독립검증 V3 지적 반영)

## SC-02 (error) seatsPerRow에 소수를 보내면 등록이 거부된다

- **Given** 공연 등록 요청에 구역 하나(seatsPerRow=1.9, 나머지 필드는 유효)가 있다
- **When** POST /api/performances 를 호출한다
- **Then** 응답 상태는 400이다
- **And** 응답 코드는 INVALID_SECTION이다
- **And** seat 테이블에 새로 생성된 행이 없다 (요청 전후 전체 seat 개수가 그대로다)

covers: section.seatsPerRow가 정수가 아닌 값(예: 1.9)이면 400과 INVALID_SECTION이 반환되고 좌석이 생성되지 않는다
flow: F2

## SC-03 (happy) 정수 price/seatsPerRow는 이전과 동일하게 성공한다

- **Given** 공연 등록 요청에 구역 하나(price=120000, rowStart="A", rowEnd="A", seatsPerRow=10)가 있다
- **When** POST /api/performances 를 호출한다
- **Then** 응답 상태는 201이다
- **And** 생성된 좌석 수는 10건이다 (rowStart~rowEnd 1개 행 × seatsPerRow 10)

covers: 정수 price/seatsPerRow를 보내는 기존 등록 요청은 이전과 동일하게 201로 성공한다
flow: F3

> rowStart/rowEnd를 1개 행으로 고정해 기대 좌석 수를 시나리오 안에서 바로 계산 가능하게
> 했다 (독립검증 V5 지적 반영). type을 error/regression과 구분되는 happy로 분류한다 —
> 이 태스크의 "정상 동작"이 곧 "정수 요청이 그대로 성공하는 것"이라 별도 happy를 새로
> 만들지 않는다 (독립검증 V2 지적 반영).
>
> 기존 PerformanceRegistrationApiTest의 다른 테스트와 입력 형태가 비슷하지만, 그 테스트는
> `SectionRequest`가 `Integer` 타입이던 시절에 통과했던 것이다. 이 태스크는 그 필드 타입을
> `BigDecimal`로 바꾸므로, 같은 입력이 새 타입·새 정수 검증 로직을 거쳐도 여전히 201을
> 반환하는지는 별도로 확인해야 한다 — 기존 테스트가 green이라는 사실만으로는 이 태스크가
> 만드는 변환 경로(BigDecimal → 정수 확인 → intValueExact)가 옳다는 것을 보장하지 않는다
> (독립검증 V9 지적 반영, 의도적으로 유지)
