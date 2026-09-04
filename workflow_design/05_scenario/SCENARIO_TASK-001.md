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
- **Then** DataIntegrityViolationException(또는 그 하위 타입)이 발생해 저장이 거부된다

V1 마이그레이션의 `title VARCHAR(200) NOT NULL` 이 엔티티 애노테이션에도 표현되어
있는지 확인한다 (title 은 대표 예시 — 나머지 NOT NULL 필드도 같은 방식으로 선언되어
있어야 하지만 코드 결함 관점에서 이 필드 하나가 나머지와 다른 메커니즘으로 깨질
이유는 없어 하나로 대표한다).

예외 타입을 `DataIntegrityViolationException`으로 좁힌 이유(재시도 시 반영):
1차 구현에서 `Exception.class` 로만 단언했더니 제약이 전혀 구현되지 않았거나
매핑이 잘못돼도 (예: 필드명 오타로 인한 다른 예외) 통과해버릴 수 있다는 지적을
Phase 4 code_review에서 받았다. Spring Data 리포지토리는 영속성 예외를
`DataAccessException` 계열로 변환하므로, 이 정도로 좁혀도 "구체 타입을 못박지
않는다"는 원래 취지(Hibernate 세부 구현에 종속되지 않음)는 유지된다.

"DB에 실제로 반영되는 시점까지" 라고 못박은 이유: ORM 구현에 따라 저장 호출 자체는
지연 쓰기로 즉시 반영되지 않을 수 있어(예: JPA의 flush/commit), 그 시점 이전에만
예외 여부를 확인하면 거짓 통과가 나올 수 있다. Phase 2b는 이 경계를 실제 구현에
맞는 지점(예: JPA라면 flush)으로 옮겨 테스트해야 한다.

covers: performance 테이블의 NOT NULL/CHECK 제약이 엔티티 애노테이션으로도 표현되어 있고, 위반 시 저장이 거부된다
flow: F7

---

## SC-11 (error) availableSeats 가 totalSeats 보다 크면 저장이 거부된다

- **Given** 공연 데이터의 totalSeats = 10, availableSeats = 11 이다 (그 외 필드는 유효)
- **When** 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
- **Then** DataIntegrityViolationException(또는 그 하위 타입)이 발생해 저장이 거부된다

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

## SC-12 (boundary) status=UPCOMING 필터가 정확히 적용된다

- **Given** 공연 A는 now < openAt 이다 (계산상 UPCOMING)
- **And** 공연 B는 openAt <= now <= closeAt 이고 availableSeats > 0 이다 (계산상 OPEN — 양성 대조군)
- **When** GET /api/performances?status=UPCOMING 를 호출한다
- **Then** content 에는 공연 A만 포함된다
- **And** totalElements 는 1이다
- **And** content 의 공연 A 항목의 status 는 "UPCOMING" 이다

covers: now < openAt 인 공연은 status: UPCOMING 으로 반환된다
flow: F3

마지막 Then("status는 UPCOMING이다")을 추가한 이유(독립검증 V3 지적):
SOLD_OUT/CANCELLED는 SC-03/SC-04가 상세 조회로 status 값 자체를 이미 단언하고
있어 SC-13/SC-14는 필터링 결과(멤버십)만 확인해도 됐다. UPCOMING은 그런 대응
시나리오가 없어, 필터만 맞고 응답의 status 필드가 다른 값을 반환해도 SC-12가
그대로 통과할 수 있었다 — 값 단언을 직접 추가해 이 공백을 막는다.

이 시나리오를 시나리오 보강 중 추가한 이유: UPCOMING은 기능 문서 §1-3 표에
있는 5개 상태 중 하나인데 §4 완료 조건에는 항목으로 없었다 — 원래 목록의
누락이라 이번에 acceptance_criteria에 추가했다(Plan amendments 참고).

---

## SC-13 (boundary) status=SOLD_OUT 필터가 정확히 적용된다

- **Given** 공연 A는 openAt <= now <= closeAt 이고 availableSeats = 0 이다 (계산상 SOLD_OUT)
- **And** 공연 B는 openAt <= now <= closeAt 이고 availableSeats > 0 이다 (계산상 OPEN — 양성 대조군)
- **When** GET /api/performances?status=SOLD_OUT 를 호출한다
- **Then** content 에는 공연 A만 포함된다
- **And** totalElements 는 1이다

covers: availableSeats == 0 인 공연은 status: SOLD_OUT 으로 반환된다
flow: F3

---

## SC-14 (boundary) status=CANCELLED 필터가 정확히 적용된다

- **Given** 공연 A는 cancelled = true 이고, openAt <= now <= closeAt 이며 availableSeats > 0 이다 (조건만 보면 OPEN처럼 보인다)
- **And** 공연 B는 cancelled = false 이고, openAt <= now <= closeAt 이며 availableSeats > 0 이다 (계산상 OPEN — 양성 대조군)
- **When** GET /api/performances?status=CANCELLED 를 호출한다
- **Then** content 에는 공연 A만 포함된다
- **And** totalElements 는 1이다

covers: cancelled == true 인 공연은 시각·좌석 수와 무관하게 status: CANCELLED 로 반환된다
flow: F3

---

SC-12/SC-13/SC-14를 추가한 이유(Phase 4 REJECT, RETRY_SCENARIO/COVERAGE_INSUFFICIENT):
1차 구현까지 `statusSpecification()`의 5개 분기 중 OPEN(SC-02)과 CLOSED(SC-06)만
필터 경로로 실행됐고 UPCOMING/SOLD_OUT/CANCELLED는 어떤 시나리오도 실행하지
않았다 — 이 공백에서 CLOSED 분기의 실제 결함(SC-15 참고)이 발견 없이 넘어갔다.
세 시나리오 모두 "필터 대상이 아닌 것도 존재하는 상태에서 필터링이 정확히
좁혀지는가"를 양성 대조군으로 확인한다 (SC-02와 같은 패턴).

---

## SC-15 (error) openAt 가 closeAt 보다 늦으면 저장이 거부된다

- **Given** 공연 데이터의 openAt 이 closeAt 보다 1일 늦다 (그 외 필드는 유효)
- **When** 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
- **Then** DataIntegrityViolationException(또는 그 하위 타입)이 발생해 저장이 거부된다

covers: openAt 가 closeAt 보다 늦은 공연은 저장이 거부된다
flow: F8

이 시나리오를 추가한 이유: Phase 4에서 `PerformanceService.statusOf()`와
`statusSpecification()`의 CLOSED 분기가 `openAt>closeAt`인 데이터에서 서로 다른
결과를 내는 결함이 발견됐다 (같은 행이 `?status=UPCOMING`과 `?status=CLOSED`
양쪽에 모순되게 잡힘). 근본 원인은 코드 실수가 아니라 "예매 오픈이 마감보다
늦을 수 없다"는 도메인 불변조건이 스키마 어디에도 없었다는 것이었다 — 이
불변조건이 성립하면 두 메서드는 수학적으로 동치가 된다(F8 참고). 그래서
CLOSED 분기 코드를 직접 고치는 대신 이 불변조건을 저장 시점에 강제하는
시나리오를 추가한다 (사용자 승인).

---

## SC-16 (error) title 이 200자를 초과하면 저장이 거부된다

- **Given** 공연 데이터의 title 이 201자이다 (그 외 필드는 유효)
- **When** 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
- **Then** DataIntegrityViolationException(또는 그 하위 타입)이 발생해 저장이 거부된다

covers: performance 테이블의 컬럼 길이 제약(title/venue VARCHAR(200))이 엔티티에도 표현되어 있고, 위반 시 저장이 거부된다
flow: F7

이 시나리오를 추가한 이유: Phase 4에서 `Performance.title`/`venue`에 길이
지정이 없어 JPA 기본값 255가 적용되고, V1 마이그레이션은 VARCHAR(200)이라는
어긋남이 발견됐다 — H2 테스트 스키마는 애노테이션에서 생성되므로(ADR-0006)
201~255자 title은 테스트는 통과하고 운영 PostgreSQL에서만 실패한다. SC-10
(NOT NULL)과 분리한 이유: 길이 제약은 컬럼 타입 자체의 속성이라 `@Column
(nullable=false)`가 아니라 `@Column(length=200)`이라는 별도 애노테이션 속성이
필요해 다른 구현 경로를 탄다. venue는 title과 같은 메커니즘(둘 다 VARCHAR(200))
이라 대표해서 하나로 다룬다.

---

## SC-17 (boundary) title 이 정확히 200자면 저장된다

- **Given** 공연 데이터의 title 이 정확히 200자이다 (그 외 필드는 유효)
- **When** 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
- **Then** 예외 없이 저장된다
- **And** 저장된 데이터를 다시 조회하면 title 이 200자 그대로 보존되어 있다

covers: performance 테이블의 컬럼 길이 제약(title/venue VARCHAR(200))이 엔티티에도 표현되어 있고, 위반 시 저장이 거부된다
flow: F7

SC-16의 양성 대조군이다(2차 독립검증 V10 지적으로 마지막 Then 추가). SC-16만
있으면 `length=199` 처럼 과도하게 좁은 제약을 넣어도(200자가 거부돼야 하는데
안 하는 게 아니라, 199자 초과부터 막아버리는 off-by-one) 통과해버린다 — 정확히
200자가 "거부되지 않아야 한다"는 것까지 확인해야 경계가 정확히 200인지
검증된다. "예외 없이 저장된다"만으로는 DB가 값을 조용히 잘라 저장해도
(truncation) 통과해버릴 수 있어, 재조회로 200자가 그대로 보존됐는지까지
확인한다.

---

## SC-18 (boundary) openAt 이 closeAt 과 같으면 저장된다

- **Given** 공연 데이터의 openAt 과 closeAt 이 정확히 같은 시각이다 (그 외 필드는 유효)
- **When** 이 데이터의 저장을 끝까지 완료하려고 시도한다 (DB에 실제로 반영되는 시점까지)
- **Then** 예외 없이 저장된다
- **And** 저장된 데이터를 다시 조회하면 openAt 과 closeAt 이 입력한 값 그대로 보존되어 있다

covers: openAt 가 closeAt 보다 늦은 공연은 저장이 거부된다
flow: F8

SC-15의 양성 대조군이다(독립검증 V10 지적). "openAt 가 closeAt 보다 늦으면
거부된다"를 `CHECK (open_at < close_at)`(등호 없이) 처럼 과도하게 좁게
구현해도 SC-15만으로는 안 걸린다 — openAt==closeAt(예매 오픈과 마감이 같은
순간)까지는 유효해야 한다는 것을 이 시나리오가 명시적으로 보장한다. 이 값도
F2/F3의 CLOSED·OPEN 경계 판정(`openAt<=now<=closeAt`)과 일관된 선택이다.

**F8 추가가 기존 시나리오를 깨지 않는지(독립검증 V10 지적)**: SC-06(CLOSED
필터)의 픽스처를 다시 확인한 결과 공연 E(openAt=now-10일, closeAt=now-6일),
F(openAt=now-10일, closeAt=now-1일) 모두 이미 openAt<=closeAt를 만족한다.
SC-01~SC-09 전체 픽스처를 재확인해도 위반하는 값이 없다 — F8을 추가해도 새로
깨지는 기존 시나리오는 없다. 새 회귀 시나리오를 별도로 추가하지 않고, 전체
테스트 스위트 재실행(Phase 2b/3)으로 이를 확인하는 것으로 충분하다고 판단했다.

---

## 커버리지

| acceptance_criteria | 시나리오 |
|---|---|
| GET /api/performances 호출 시 200과 함께 content/page/size/totalElements 형식으로 목록이 반환된다 | SC-01 |
| status=OPEN 필터 시 openAt <= now <= closeAt 이고 availableSeats > 0 인 공연만 반환된다 | SC-02 |
| availableSeats == 0 인 공연은 status: SOLD_OUT 으로 반환된다 | SC-03, SC-13 |
| cancelled == true 인 공연은 시각·좌석 수와 무관하게 status: CANCELLED 로 반환된다 | SC-04, SC-14 |
| start_at < now 인 공연은 상태 필터와 무관하게 목록에서 제외된다 | SC-05, SC-06 |
| 존재하지 않는 id 로 GET /api/performances/{id} 호출 시 404와 PERFORMANCE_NOT_FOUND 코드가 반환된다 | SC-09 |
| Clock 을 오픈 정각·마감 1초 전으로 고정한 테스트에서 경계값이 올바른 status 를 반환한다 | SC-07, SC-08 |
| performance 테이블의 NOT NULL/CHECK 제약이 엔티티 애노테이션으로도 표현되어 있고, 위반 시 저장이 거부된다 | SC-10, SC-11 |
| openAt 가 closeAt 보다 늦은 공연은 저장이 거부된다 | SC-15, SC-18(양성 대조) |
| performance 테이블의 컬럼 길이 제약(title/venue VARCHAR(200))이 엔티티에도 표현되어 있고, 위반 시 저장이 거부된다 | SC-16, SC-17(양성 대조) |
| now < openAt 인 공연은 status: UPCOMING 으로 반환된다 | SC-12 |

SC-05/SC-06을 하나로 묶지 않은 이유: SC-05는 "필터가 아예 없을 때"의 기본 조회 경로를,
SC-06은 "status 필터 조건이 우연히 일치해도"의 필터링 경로를 검증한다. 필터 쿼리를
구현하며 `start_at >= now` 조건을 빠뜨리는 결함은 SC-05만으로는 잡히지 않고 SC-06에서만
드러난다 (F3+F4 상호작용, `references/coverage-policy.md` §3 기준).

SC-07/SC-08을 하나로 묶지 않은 이유: 오픈 경계(`now >= openAt`)와 마감 경계
(`now <= closeAt`)는 서로 다른 비교 연산이라, 한쪽만 `<=` 대신 `<` 로 잘못 구현해도
다른 한쪽 시나리오는 통과한다 — 병합하면 그 결함을 놓친다.

SC-03/SC-13, SC-04/SC-14를 각각 하나로 묶지 않은 이유: SC-03/SC-04는 상세
조회(단건, 필터 없음) 경로에서 상태 계산 자체를 검증하고, SC-13/SC-14는
목록의 status 쿼리 파라미터가 그 계산된 상태로 정확히 필터링하는지(F3)를
검증한다 — 서로 다른 코드 경로다. 상태 계산은 맞는데 필터 조건(Specification)
에서 그 상태를 잘못 옮기는 결함은 SC-03/SC-04만으로는 잡히지 않는다. 실제로
CLOSED에서 이 유형의 결함이 있었다(SC-15로 근본 해결).

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

## Phase 2b 인벤토리 예외 — GWT 시나리오에 대응하지 않는 테스트 1건

2차 독립검증 V10(CLOSED/UPCOMING 상호배타성이 API 경계에서 재현 불가 —
F8이 성립하면 그 데이터 자체가 존재할 수 없어 구조적으로 테스트 불가)에 대해,
HITL#1 재승인 시 사용자가 **순수 로직 분리 + 단위 테스트**로 처리하기로
승인했다. `PerformanceStatusRules`(F9, Plan 참고)의 `of()`/`matches()`가
5개 상태에 대해 대표 경계값마다 상호배타적인지 검증하는 단위 테스트를
Phase 2b가 추가하지만, 이 테스트는 위 SC-01~18 중 어느 것에도 1:1 대응하지
않는다 — Spring 컨텍스트도, HTTP 요청도, Given/When/Then으로 자연스럽게
쓸 수 있는 사용자 시나리오도 아닌 순수 구현 안전장치이기 때문이다.

**`wf-red`의 "시나리오 하나당 테스트 함수 하나" 인벤토리 검사 시 이 사실을
반드시 반영한다** — 시나리오 18건 + 이 예외 1건 = 테스트 함수 19개가 정상이며,
불일치로 보고 시나리오를 다시 세지 않는다. 근거는 `PLAN_TASK-001.json.amendments`
(attempt 2, HITL#1 재승인 시점 항목)에도 남아 있다.
