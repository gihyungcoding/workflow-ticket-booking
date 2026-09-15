# TASK-004 시나리오

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

- **Given** 등록 요청의 sections 가 빈 배열이다
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
flow: F3
