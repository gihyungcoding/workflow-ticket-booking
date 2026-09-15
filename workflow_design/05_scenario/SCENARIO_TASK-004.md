# TASK-004 시나리오

> **재시도 이력**
> - **attempt 2** (2026-09-15) — Phase 4 1차 검증 FAIL(`VERIFY_TASK-004.json`
>   code_review.findings)에 따른 `RETRY_SCENARIO`. 좌석 상한(AC4)이 역방향 행
>   범위·정수 오버플로로 완전히 우회되고 필수 필드 누락이 500으로 새는 결함을
>   막기 위해 SC-16~SC-21을 추가했다.
> - **attempt 3** (2026-09-15) — Phase 4 2차 검증(재검증)에서 code-reviewer가
>   재현한 잔여 결함 2건(title/venue 200자 초과, sections 배열 null 원소)을 막기
>   위해 SC-22~SC-23을 추가했다. code-reviewer가 title/venue 길이 결함이 PUT(수정)
>   경로도 공유한다고 확인해 SC-24를 추가로 확정했다.
> - 기존 SC-01~13/15(attempt 1)는 그대로 두고 손대지 않는다(이미 검증·승인됨).

## 확정한 정의 (Plan의 unresolved 해소)

- **행 범위 겹침**: 두 구역의 `[rowStart, rowEnd]` 를 알파벳 구간으로 보고, 이 구간이
  교집합을 가지면 겹침으로 판정한다. 좌석 번호(seatsPerRow) 조합은 보지 않는다.
- **행 표기**: `rowStart`/`rowEnd` 는 A~Z 단일 대문자만 지원한다. 26행을 넘는 구역은
  이번 범위 밖이며, 5,000석 상한(SC-04)과 함께 실무상 26행 조합으로 충분하다고 본다.

> **철회된 시나리오**: SC-14(regression, "기존 GET 조회 404 처리가 그대로 동작한다")를
> HITL#1 승인 이후 Phase 2b에서 제거했다. 이 태스크가 건드리지 않는 기존 코드 경로를
> 검증하는 시나리오라 지금 이미 통과해버려 "새 테스트는 반드시 실패한다"는 Red 요구사항과
> 구조적으로 맞지 않는다. 이 회귀 방지는 기존 `PerformanceApiTest`/`PerformanceConstraintTest`
> 전체 실행(Phase 3에서 반드시 확인)이 담당한다. 번호 14는 재사용하지 않는다.

## SC-01 (happy) 구역 2개로 등록하면 구역별 좌석이 생성되고 총수가 합산된다

- **Given** 등록 요청에 구역 VIP(행 A~B, 행당 10석, 가격 120000)와 구역 R(행 C~E, 행당 20석, 가격 80000)가 있다
- **And** 시각은 openAt <= closeAt <= startAt 을 만족한다
- **When** POST /api/performances 를 호출한다
- **Then** 201이 반환된다
- **And** 응답의 totalSeats/availableSeats 는 80(=2행×10 + 3행×20)이다
- **And** performance_id 로 seat 를 조회하면 80건이 조회된다
- **And** 조회된 seat 에 "VIP-A1", "VIP-B10", "R-C1", "R-E20" 라벨이 모두 존재한다

covers: 구역 1개 이상으로 POST /api/performances 호출 시 201과 함께 각 구역의 rowStart~rowEnd × seatsPerRow 만큼 seat 행이 생성된다; 생성된 performance.total_seats/available_seats 가 생성된 좌석 총수와 같다
flow: F1

## SC-02 (error) openAt이 closeAt보다 늦으면 등록이 거부된다

- **Given** 등록 요청의 openAt이 closeAt보다 늦다
- **And** sections는 유효한 구역 1개를 포함한다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_TIME_ORDER 이다
- **And** seat 테이블에 새로 생성된 행이 없다 (요청 전후 전체 seat 개수가 그대로다)

covers: openAt > closeAt 또는 closeAt > startAt 이면 400과 INVALID_TIME_ORDER 가 반환된다
flow: F2

## SC-03 (error) closeAt이 startAt보다 늦으면 등록이 거부된다

- **Given** 등록 요청의 openAt <= closeAt 이지만 closeAt이 startAt보다 늦다
- **And** sections는 유효한 구역 1개를 포함한다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_TIME_ORDER 이다

covers: openAt > closeAt 또는 closeAt > startAt 이면 400과 INVALID_TIME_ORDER 가 반환된다
flow: F2

## SC-04 (error) 구역 좌석 합이 5,000을 넘으면 등록이 거부된다

- **Given** 등록 요청에 구역 1개(행 A 하나, 행당 5,001석)가 있어 합산 좌석 수가 정확히 5,001이다
- **And** 시각은 openAt <= closeAt <= startAt 을 만족한다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 SEAT_LIMIT_EXCEEDED 이다
- **And** seat 테이블에 새로 생성된 행이 없다 (요청 전후 전체 seat 개수가 그대로다)

covers: 좌석 총수가 5,000을 넘으면 400과 SEAT_LIMIT_EXCEEDED 가 반환된다
flow: F3

## SC-05 (error) 두 구역의 행 범위가 겹치면 등록이 거부된다

- **Given** 등록 요청에 구역 VIP(행 A~E)와 구역 R(행 C~F)가 있다 (C~E 구간이 겹친다)
- **And** 시각은 openAt <= closeAt <= startAt 을 만족한다
- **When** POST /api/performances 를 호출한다
- **Then** 409가 반환된다
- **And** 응답 코드는 DUPLICATE_SEAT_RANGE 이다
- **And** seat 테이블에 새로 생성된 행이 없다 (요청 전후 전체 seat 개수가 그대로다)

covers: 두 구역의 행 범위가 겹치면 409와 DUPLICATE_SEAT_RANGE 가 반환된다
flow: F4

## SC-06 (happy) 오픈 전인 공연을 수정하면 필드가 반영된다

- **Given** 공연 P가 등록되어 있고 now < P.openAt 이다
- **When** PUT /api/performances/{P.id} 로 title/venue/startAt/openAt/closeAt 변경을 요청한다
- **Then** 200이 반환된다
- **And** 응답의 title/venue/startAt/openAt/closeAt 이 요청한 값으로 바뀌어 있다

covers: now < openAt 인 공연을 PUT /api/performances/{id} 로 수정하면 200과 변경된 필드가 반영된 상세가 반환된다
flow: F5

## SC-07 (error) 오픈 이후인 공연을 수정하려 하면 거부된다

- **Given** 공연 P가 등록되어 있고 now >= P.openAt 이다
- **When** PUT /api/performances/{P.id} 로 수정을 요청한다
- **Then** 409가 반환된다
- **And** 응답 코드는 REGISTRATION_ALREADY_OPEN 이다
- **And** P를 다시 조회하면 title/venue/startAt/openAt/closeAt 이 요청 이전 값과 동일하다

covers: now >= openAt 인 공연을 수정하려 하면 409와 REGISTRATION_ALREADY_OPEN 이 반환된다
flow: F6

## SC-08 (error) 수정 요청 자체가 시각 순서를 어기면 거부된다

- **Given** 공연 P가 등록되어 있고 now < P.openAt 이다
- **And** 수정 요청의 openAt이 closeAt보다 늦다
- **When** PUT /api/performances/{P.id} 로 수정을 요청한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_TIME_ORDER 이다
- **And** P를 다시 조회하면 title/venue/startAt/openAt/closeAt 이 요청 이전 값과 동일하다

covers: (acceptance_criteria 미대응 — PLAN_TASK-004.json F5 단계 "openAt<=closeAt<=startAt 재검증"을 검증한다. now<openAt 이어도 시각 순서 자체가 깨진 수정은 통과시키지 않는다.)
flow: F5

## SC-09 (error) 존재하지 않는 공연을 수정하려 하면 404

- **Given** id 99999 에 해당하는 공연이 존재하지 않는다
- **When** PUT /api/performances/99999 로 수정을 요청한다
- **Then** 404가 반환된다
- **And** 응답 코드는 PERFORMANCE_NOT_FOUND 이다

covers: 존재하지 않는 id 로 수정/취소를 호출하면 404와 PERFORMANCE_NOT_FOUND 가 반환된다
flow: F7

## SC-10 (error) 존재하지 않는 공연을 취소하려 하면 404

- **Given** id 99999 에 해당하는 공연이 존재하지 않는다
- **When** POST /api/performances/99999/cancel 을 호출한다
- **Then** 404가 반환된다
- **And** 응답 코드는 PERFORMANCE_NOT_FOUND 이다

covers: 존재하지 않는 id 로 수정/취소를 호출하면 404와 PERFORMANCE_NOT_FOUND 가 반환된다
flow: F7

## SC-11 (happy) 공연을 취소하면 cancelled가 true로 바뀐다

- **Given** 공연 P가 등록되어 있고 cancelled=false 이다
- **When** POST /api/performances/{P.id}/cancel 을 호출한다
- **Then** 200이 반환된다
- **And** 응답의 status 는 CANCELLED 이다

covers: POST /api/performances/{id}/cancel 호출 시 cancelled 가 true 로 바뀌고, 이미 취소된 공연을 다시 호출해도 200이 반환된다(멱등)
flow: F8
note: PerformanceResponse에는 cancelled 필드가 없고 status로 노출된다 — PerformanceStatusRules는 cancelled=true를 시각·좌석 수와 무관하게 최우선으로 CANCELLED 처리한다 (도메인 근거이지 단언 대상이 아니다)

## SC-12 (boundary) 이미 취소된 공연을 다시 취소해도 200이 반환된다

- **Given** 공연 P가 등록되어 있고 cancelled=true 이다 (이미 취소됨)
- **When** POST /api/performances/{P.id}/cancel 을 다시 호출한다
- **Then** 200이 반환된다
- **And** 응답의 status 는 CANCELLED 로 유지된다

covers: POST /api/performances/{id}/cancel 호출 시 cancelled 가 true 로 바뀌고, 이미 취소된 공연을 다시 호출해도 200이 반환된다(멱등)
flow: F8

## SC-13 (error) 구역이 하나도 없는 등록 요청은 거부된다

- **Given** 등록 요청의 sections 가 빈 배열 `[]` 이다 (필드 자체가 없는 null 이 아니다 — null 이면 SC-16의 INVALID_REQUEST)
- **And** 시각은 openAt <= closeAt <= startAt 을 만족한다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 EMPTY_SECTIONS 이다

covers: (acceptance_criteria 미대응 — performance-registration.md §3 API 명세에 명시된 오류 코드. PLAN_TASK-004.json F9)
flow: F9

## SC-15 (boundary) 정확히 5,000석이면 등록이 성공한다

- **Given** 등록 요청에 구역 1개(행 A 하나, 행당 5,000석)가 있어 합산 좌석 수가 정확히 5,000이다
- **And** 시각은 openAt <= closeAt <= startAt 을 만족한다
- **When** POST /api/performances 를 호출한다
- **Then** 201이 반환된다
- **And** 응답의 totalSeats/availableSeats 는 5,000이다

covers: (acceptance_criteria 미대응 — SC-04의 반대쪽 경계. "5,000을 넘으면" 거부라는 AC4 문구가 5,000 자체는 포함임을 확인한다.)
flow: F1

## SC-16 (error) 필수 필드가 없으면 등록이 거부된다

- **Given** 등록 요청에 sections 필드 자체가 없다(JSON에 없음, null — 빈 배열 `[]` 이 아니다)
- **And** 나머지 필드(title/venue/시각)는 유효하다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_REQUEST 이다

covers: (acceptance_criteria 미대응 — Phase 4 검증에서 code-reviewer가 재현: sections/시각/title/venue 등 최상위 필수 필드가 없으면 NPE 또는 DB NOT NULL 위반으로 500이 새던 결함. title/venue/시각 누락도 같은 코드 경로(INVALID_REQUEST, F10 — 등록/수정이 공유하는 단일 null 검증 지점)로 처리되므로 대표적으로 sections 누락 하나만 시나리오로 둔다 — coverage-policy §3 "같은 코드 경로는 하나로 묶는다". 구역 내부 필드(price 등)의 누락/형식 오류는 F11/INVALID_SECTION 소관이라 여기 포함하지 않는다 — SC-17~19 참고)
flow: F10

## SC-17 (error) 구역의 rowStart가 rowEnd보다 뒤 알파벳이면 등록이 거부된다

- **Given** 등록 요청에 구역 하나(rowStart="E", rowEnd="C")가 있다 (역방향 범위)
- **And** 시각은 openAt <= closeAt <= startAt 을 만족한다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_SECTION 이다
- **And** seat 테이블에 새로 생성된 행이 없다 (요청 전후 전체 seat 개수가 그대로다)

covers: (acceptance_criteria 미대응 — Phase 4 검증에서 code-reviewer가 재현: 역방향 행 범위가 음수 좌석수로 계산돼 SEAT_LIMIT_EXCEEDED(AC4)와 DUPLICATE_SEAT_RANGE(AC5) 검사를 모두 우회하던 결함. 이 검사(F11)가 그 두 검사(F3/F4)보다 먼저 실행되어야 한다)
flow: F11

## SC-18 (error) 구역의 seatsPerRow가 0 이하이면 등록이 거부된다

- **Given** 등록 요청에 구역 하나(seatsPerRow=0)가 있다
- **And** 시각은 openAt <= closeAt <= startAt 을 만족한다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_SECTION 이다

covers: (acceptance_criteria 미대응 — Phase 4 검증에서 code-reviewer가 재현: seatsPerRow가 1 미만(0 또는 음수)이면 좌석수가 0 이하이거나 음수가 되어 다른 구역의 좌석수를 상쇄하는 등 상한 검사를 무력화할 수 있던 결함. 0은 "1 이상" 규칙의 경계값이다)
flow: F11

## SC-19 (error) 구역의 rowStart가 빈 문자열이면 등록이 거부된다

- **Given** 등록 요청에 구역 하나(rowStart="")가 있다
- **And** 시각은 openAt <= closeAt <= startAt 을 만족한다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_SECTION 이다

covers: (acceptance_criteria 미대응 — Phase 4 검증에서 code-reviewer가 재현: rowStart가 빈 문자열이면 charAt(0)에서 StringIndexOutOfBoundsException으로 500이 나던 결함)
flow: F11

## SC-20 (boundary) 구역의 seatsPerRow가 매우 큰 값이면 좌석 상한 초과로 거부된다

- **Given** 등록 요청에 서로 겹치지 않는 구역 2개 — 구역 A(행 A~A, seatsPerRow=1,200,000,000)와 구역 B(행 B~B, seatsPerRow=1,200,000,000) — 가 있다
- **And** 시각은 openAt <= closeAt <= startAt 을 만족한다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 SEAT_LIMIT_EXCEEDED 이다
- **And** seat 테이블에 새로 생성된 행이 없다 (요청 전후 전체 seat 개수가 그대로다)

covers: (acceptance_criteria 미대응 — Phase 4 검증에서 code-reviewer가 재현: 좌석수 합산에 int를 써서 오버플로로 음수가 되어 상한 검사를 우회하고 수억 개 Seat 객체 생성을 시도하던 보안 결함(DoS))
flow: F3
note: 구역을 서로 다른 행(A~A, B~B)으로 둔 것은 의도적이다 — 같은 행을 쓰면 DUPLICATE_SEAT_RANGE(409)가 먼저 발생해 이 시나리오가 검증하려는 SEAT_LIMIT_EXCEEDED 경로에 도달하지 못한다. "int 오버플로로 우회되지 않음"은 이 테스트가 통과해야 하는 이유(구현 근거)이지 Then 단언 대상이 아니다 — 관찰 가능한 단언은 400/SEAT_LIMIT_EXCEEDED/seat 0건뿐이다.

## SC-21 (error) 구역명(grade)이 20자를 넘으면 등록이 거부된다

- **Given** 등록 요청에 구역 하나(grade가 21자)가 있다
- **And** 시각은 openAt <= closeAt <= startAt 을 만족한다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_SECTION 이다
- **And** seat 테이블에 새로 생성된 행이 없다 (요청 전후 전체 seat 개수가 그대로다)

covers: (acceptance_criteria 미대응 — Phase 4 검증에서 code-reviewer가 재현: grade가 길면 seatLabel(grade+행+좌석번호)이 seat_label VARCHAR(30)을 넘어 insert 시 500이 나던 결함. grade를 20자로 제한해 어떤 좌석 번호 조합에서도 라벨이 30자를 넘지 않게 한다)
flow: F11

## SC-22 (error) title이 200자를 넘으면 등록이 거부된다

- **Given** 등록 요청의 title이 201자이다
- **And** 나머지 필드는 유효하다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_REQUEST 이다

covers: (acceptance_criteria 미대응 — Phase 4 2차 검증에서 code-reviewer가 재현: title/venue가 200자를 넘으면 performance.title VARCHAR(200) 제약 위반으로 500이 나던 결함. venue도 같은 코드 경로(validateRequired, F10)라 대표로 title 하나만 시나리오로 둔다 — coverage-policy §3)
flow: F10

## SC-23 (error) sections 배열에 null 원소가 있으면 등록이 거부된다

- **Given** 등록 요청의 sections 배열에 null 원소가 하나 있다 (예: `[null]`)
- **And** 나머지 필드는 유효하다
- **When** POST /api/performances 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_SECTION 이다

covers: (acceptance_criteria 미대응 — Phase 4 2차 검증에서 code-reviewer가 재현: sections 리스트 자체는 null이 아니지만 원소가 null이면 Controller의 SectionRequest→SectionSpec 변환에서 NPE로 500이 나던 결함. 원소를 그대로 Service에 넘기고 validateSection의 null 검사(F11)가 잡도록 한다)
flow: F11

## SC-24 (error) 수정 시 title이 200자를 넘으면 거부된다

- **Given** now < openAt 인 공연이 등록되어 있다
- **And** 수정 요청의 title이 201자이다
- **When** PUT /api/performances/{id} 를 호출한다
- **Then** 400이 반환된다
- **And** 응답 코드는 INVALID_REQUEST 이다

covers: (acceptance_criteria 미대응 — Phase 4 2차 검증에서 code-reviewer가 재현: registerPerformance/updatePerformance가 validateRequired를 공유하지만 두 개의 별도 Controller 엔드포인트라, 길이 검사를 등록 경로에만 붙이는 잘못된 수정이 들어가면 SC-22는 통과하면서 이 경로만 500을 내는 결함이 가능하다 — coverage-policy §3. 또한 code-reviewer는 PerformanceRegistrationApiTest의 클래스 레벨 @Transactional이 테스트 트랜잭션을 롤백해 PUT 경로의 DB 제약 위반(500)을 가린다는 것도 발견했다 — Phase 2b에서 이 시나리오를 Red로 옮길 때 HTTP 응답의 status/code 단언만으로 충분하지만, 혹시 응답이 200으로 나온다면 이 트랜잭션 격리 사각지대가 원인일 수 있으니 서비스 계층에서 예외가 던져지는지도 함께 확인한다)
flow: F10
