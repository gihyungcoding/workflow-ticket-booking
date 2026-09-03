# TASK-001 시나리오

기준 시각(now)은 모든 시나리오에서 `Clock.fixed(T0, ...)` 로 고정된 값이다.
공연의 시각 필드(startAt/openAt/closeAt)는 이 T0을 기준으로 상대적으로 서술한다.

---

## SC-01 (happy) 목록이 기본 페이지 형식으로 반환된다

- **Given** 공연 A, B가 등록되어 있고 둘 다 startAt >= now 이다
- **When** GET /api/performances 를 파라미터 없이 호출한다
- **Then** 200이 반환된다
- **And** 응답 바디의 최상위 필드는 content/page/size/totalElements 정확히 4개뿐이고 그 외 필드는 없다
- **And** content 에는 A, B 가 모두 포함된다
- **And** totalElements 는 2이다

covers: GET /api/performances 호출 시 200과 함께 content/page/size/totalElements 형식으로 목록이 반환된다
flow: F1

---

## SC-02 (happy) status=OPEN 필터는 계산된 상태를 기준으로 적용된다

- **Given** 공연 A는 openAt <= now <= closeAt 이고 availableSeats = 5 이다 (계산상 OPEN)
- **And** 공연 B는 openAt <= now <= closeAt 이고 availableSeats = 0 이다 (계산상 SOLD_OUT)
- **When** GET /api/performances?status=OPEN 을 호출한다
- **Then** content 에는 공연 A만 포함된다
- **And** totalElements 는 1이다

covers: status=OPEN 필터 시 openAt <= now <= closeAt 이고 availableSeats > 0 인 공연만 반환된다
flow: F2, F3

---

## SC-03 (happy) 매진된 공연은 SOLD_OUT 으로 표시된다

- **Given** 공연 C는 openAt <= now <= closeAt 이고 availableSeats = 0 이다
- **When** GET /api/performances/{C.id} 를 호출한다
- **Then** 200이 반환된다
- **And** 응답의 status 는 "SOLD_OUT" 이다

covers: availableSeats == 0 인 공연은 status: SOLD_OUT 으로 반환된다
flow: F2

---

## SC-04 (happy) 취소된 공연은 시각·좌석 수와 무관하게 CANCELLED 로 표시된다

- **Given** 공연 D는 cancelled = true 이다
- **And** 공연 D의 openAt <= now <= closeAt 이고 availableSeats = 10 이다 (조건만 보면 OPEN처럼 보인다)
- **When** GET /api/performances/{D.id} 를 호출한다
- **Then** 응답의 status 는 "CANCELLED" 이다

covers: cancelled == true 인 공연은 시각·좌석 수와 무관하게 status: CANCELLED 로 반환된다
flow: F2

---

## SC-05 (boundary) 지난 공연은 필터 없는 목록에서 제외된다

- **Given** 공연 E의 startAt < now 이다 (지난 공연)
- **And** 공연 F의 startAt >= now 이다
- **When** GET /api/performances 를 파라미터 없이 호출한다
- **Then** content 에는 공연 F만 포함된다
- **And** 공연 E는 content 에 없다

covers: start_at < now 인 공연은 상태 필터와 무관하게 목록에서 제외된다
flow: F4

---

## SC-06 (boundary) status 필터가 일치해도 지난 공연은 여전히 제외된다

- **Given** 공연 E의 startAt < now 이고 now > closeAt 이다 (지난 공연이면서 마감도 지났다)
- **And** 공연 F의 startAt >= now 이고 now > closeAt 이다 (아직 시작 전이지만 마감 시각은 지난 "조기 마감" 케이스 — §1-3)
- **When** GET /api/performances?status=CLOSED 를 호출한다
- **Then** content 에는 공연 F만 포함된다
- **And** 공연 E는 content 에 없다

공연 F(양성 대조)가 없으면 필터가 완전히 깨져 빈 목록이 와도 이 시나리오는 통과해버린다 —
F를 포함시켜 "필터 조건 자체는 맞지만 지난 공연만 추가로 걸러진다"는 것을 검증한다.

covers: start_at < now 인 공연은 상태 필터와 무관하게 목록에서 제외된다
flow: F3, F4

---

## SC-07 (boundary) 오픈 정각에 상태가 OPEN 으로 전환된다

- **Given** Clock 이 공연 G의 openAt 과 정확히 같은 시각 T로 고정되어 있다 (now == openAt == T)
- **And** T는 실제 시스템 시각과 다르다 (예: 몇 년 뒤의 미래 시각으로 openAt/closeAt/T를 함께 잡는다)
- **And** 공연 G의 availableSeats >= 1 이고 now <= closeAt 이다
- **When** GET /api/performances/{G.id} 를 호출한다
- **Then** 응답의 status 는 "OPEN" 이다 (UPCOMING 이 아니다)

T를 실제 시각과 다르게 잡는 이유: 구현이 주입된 Clock 대신 SQL `now()`/시스템 실제
시각을 썼다면, 실제 시각은 openAt(T)에 한참 못 미치므로 status가 UPCOMING으로 나와
이 Then이 실패한다 — 즉 이 시나리오는 "SQL now() 미사용"도 간접적으로 관찰 가능한
결과(status 값)로 검증한다.

covers: Clock 을 오픈 정각·마감 1초 전으로 고정한 테스트에서 경계값이 올바른 status 를 반환한다 (SQL now() 미사용을 리뷰로 확인)
flow: F6

---

## SC-08 (boundary) 마감 1초 전까지는 CLOSED 로 전환되지 않는다

- **Given** Clock 이 공연 H의 closeAt 보다 1초 이른 시각 T로 고정되어 있다 (now == closeAt - 1s == T)
- **And** T는 실제 시스템 시각과 다르다 (SC-07과 같은 이유 — 미래의 임의 시각으로 openAt/closeAt/T를 함께 잡는다)
- **And** 공연 H의 availableSeats >= 1 이고 now >= openAt 이다
- **When** GET /api/performances/{H.id} 를 호출한다
- **Then** 응답의 status 는 "OPEN" 이다 (CLOSED 가 아니다)

covers: Clock 을 오픈 정각·마감 1초 전으로 고정한 테스트에서 경계값이 올바른 status 를 반환한다 (SQL now() 미사용을 리뷰로 확인)
flow: F6

---

## SC-09 (error) 존재하지 않는 공연을 조회하면 404 가 반환된다

- **Given** id 999 에 해당하는 공연이 존재하지 않는다
- **When** GET /api/performances/999 를 호출한다
- **Then** 404가 반환된다
- **And** 응답 바디의 code 는 "PERFORMANCE_NOT_FOUND" 이다

covers: 존재하지 않는 id 로 GET /api/performances/{id} 호출 시 404와 PERFORMANCE_NOT_FOUND 코드가 반환된다
flow: F5

---

## SC-10 (error) 필수 필드가 없는 공연은 저장이 거부된다

- **Given** 공연 데이터에서 title 이 null 이고, 나머지 필드(venue/startAt/openAt/closeAt/totalSeats/availableSeats)는 모두 유효하다
- **When** 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
- **Then** 예외가 발생해 저장이 거부된다

V1 마이그레이션의 `title VARCHAR(200) NOT NULL` 이 엔티티 애노테이션에도 표현되어
있는지 확인한다 (title 은 대표 예시 — 나머지 NOT NULL 필드도 같은 방식으로 선언되어
있어야 하지만 코드 결함 관점에서 이 필드 하나가 나머지와 다른 메커니즘으로 깨질
이유는 없어 하나로 대표한다).

"DB에 실제로 반영되는 시점까지" 라고 못박은 이유: ORM 구현에 따라 저장 호출 자체는
지연 쓰기로 즉시 반영되지 않을 수 있어(예: JPA의 flush/commit), 그 시점 이전에만
예외 여부를 확인하면 거짓 통과가 나올 수 있다. Phase 2b는 이 경계를 실제 구현에
맞는 지점(예: JPA라면 flush)으로 옮겨 테스트해야 한다 — 어떤 메커니즘을 쓰는지는
여기서 정하지 않는다. 예외의 구체적 타입(예: `DataIntegrityViolationException`)도
Then에서 못박지 않는다 — 구현 선택이며, Phase 2b가 실제 스택에 맞춰 정한다.

covers: performance 테이블의 NOT NULL/CHECK 제약이 엔티티 애노테이션으로도 표현되어 있고, 위반 시 저장이 거부된다
flow: F7

---

## SC-11 (error) availableSeats 가 totalSeats 보다 크면 저장이 거부된다

- **Given** 공연 데이터의 totalSeats = 10, availableSeats = 11 이다 (그 외 필드는 유효)
- **When** 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
- **Then** 예외가 발생해 저장이 거부된다

V1 마이그레이션의 `CHECK (available_seats >= 0 AND available_seats <= total_seats)`
가 엔티티 애노테이션(또는 동등한 제약)으로도 표현되어 있는지 확인한다. SC-10(NOT NULL)과
분리한 이유: NOT NULL 은 컬럼 정의 하나로 끝나지만 이 CHECK 는 두 필드 사이의 관계식이라
서로 다른 애노테이션/구현 경로를 타므로, 한쪽만 구현하고 다른 쪽을 빠뜨리는 결함을
각각 잡을 수 있어야 한다. "DB에 실제로 반영되는 시점까지" 표기 이유는 SC-10과 같되,
이 CHECK는 특히 DB 왕복 전에는 드러나지 않을 가능성이 크다.

covers: performance 테이블의 NOT NULL/CHECK 제약이 엔티티 애노테이션으로도 표현되어 있고, 위반 시 저장이 거부된다
flow: F7

---

**SC-10/SC-11의 성격에 대해**: 다른 시나리오(SC-01~09)는 공개 HTTP API를 경계로
삼지만, SC-10/SC-11은 영속성 계층(Repository/엔티티)을 경계로 삼는다 — 의도적이다.
"엔티티 애노테이션이 마이그레이션과 같은 제약을 표현하는가"는 애초에 HTTP로 관측할
방법이 없는 성질(스키마 일치)이라, 이 두 시나리오만 `@DataJpaTest` 수준에서
검증한다 (Plan test_hints에 반영).

---

## 커버리지

| acceptance_criteria | 시나리오 |
|---|---|
| GET /api/performances 호출 시 200과 함께 content/page/size/totalElements 형식으로 목록이 반환된다 | SC-01 |
| status=OPEN 필터 시 openAt <= now <= closeAt 이고 availableSeats > 0 인 공연만 반환된다 | SC-02 |
| availableSeats == 0 인 공연은 status: SOLD_OUT 으로 반환된다 | SC-03 |
| cancelled == true 인 공연은 시각·좌석 수와 무관하게 status: CANCELLED 로 반환된다 | SC-04 |
| start_at < now 인 공연은 상태 필터와 무관하게 목록에서 제외된다 | SC-05, SC-06 |
| 존재하지 않는 id 로 GET /api/performances/{id} 호출 시 404와 PERFORMANCE_NOT_FOUND 코드가 반환된다 | SC-09 |
| Clock 을 오픈 정각·마감 1초 전으로 고정한 테스트에서 경계값이 올바른 status 를 반환한다 | SC-07, SC-08 |
| performance 테이블의 NOT NULL/CHECK 제약이 엔티티 애노테이션으로도 표현되어 있고, 위반 시 저장이 거부된다 | SC-10, SC-11 |

SC-05/SC-06을 하나로 묶지 않은 이유: SC-05는 "필터가 아예 없을 때"의 기본 조회 경로를,
SC-06은 "status 필터 조건이 우연히 일치해도"의 필터링 경로를 검증한다. 필터 쿼리를
구현하며 `start_at >= now` 조건을 빠뜨리는 결함은 SC-05만으로는 잡히지 않고 SC-06에서만
드러난다 (F3+F4 상호작용, `references/coverage-policy.md` §3 기준).

SC-07/SC-08을 하나로 묶지 않은 이유: 오픈 경계(`now >= openAt`)와 마감 경계
(`now <= closeAt`)는 서로 다른 비교 연산이라, 한쪽만 `<=` 대신 `<` 로 잘못 구현해도
다른 한쪽 시나리오는 통과한다 — 병합하면 그 결함을 놓친다.

SC-10/SC-11(AC8, F7)은 HITL#1 검토 중 사용자 피드백으로 추가됐다 — Plan의
`codebase_analysis.unresolved`에 있던 "엔티티 애노테이션과 V1 마이그레이션 제약을
수동으로 동기화해야 한다"는 리스크를 방치하지 않고 `@DataJpaTest`로 실제 보호하기
위함이다 (`PLAN_TASK-001.json.amendments` 참고).

## 의도적으로 다루지 않는 것

독립검증(V8)에서 지적된 다음 항목은 `acceptance_criteria`(§4) 목록에 없어 이번
시나리오 범위에서 제외한다 — 필요하면 별도 기준으로 추가한 뒤 시나리오를 쓴다.

- `page`/`size` 파라미터의 기본값(0/20)·상한(100) 적용 동작 (Plan design.inputs에는
  있으나 §4 완료 조건에는 없음)
- `status` 에 정의된 5개 값 외의 문자열이 들어왔을 때의 동작 (Plan design.outputs에도
  대응하는 에러 응답이 없다 — 프레임워크 기본 동작에 맡긴다)
