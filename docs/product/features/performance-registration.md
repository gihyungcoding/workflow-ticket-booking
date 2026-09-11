# [주최자] 공연 등록

**상태**: 초안
**최종 갱신**: 2026-09-11

---

## 1. 요구사항

### 1-1. 배경

소규모 공연 주최자는 좌석 배정을 수기(전화·메신저·스프레드시트)로 관리해 같은
좌석을 두 번 파는 일이 생긴다(`docs/product/product.md` §2). [공연 목록과 예매
상태](performance-availability.md) 기능은 `performance` 테이블과 조회 API를
만들었지만 쓰기 API가 없어, 지금 데이터는 테스트 픽스처로만 채워진다 — 실제
주최자는 여전히 시스템에 직접 공연을 등록할 방법이 없다.

이 기능은 주최자가 공연 기본 정보와 좌석 구성(구역·행·열·가격)을 화면에서
직접 등록하는 첫 쓰기 경로를 만든다. 좌석은 개별 행으로 생성되어, 이후 좌석
선점 기능이 좌석 단위로 예매 상태를 매핑할 수 있는 기반이 된다(`docs/architecture/
architecture.md` §4).

| AS-IS | TO-BE |
|---|---|
| 주최자가 좌석 배정을 수기로 관리해 중복 판매가 발생한다 | 주최자가 구역별 행·열·가격을 입력하면 시스템이 개별 좌석을 생성한다 |
| `performance` 데이터는 테스트 픽스처로만 존재한다 | 주최자가 화면에서 등록·수정·취소할 수 있다 |

### 1-2. 태스크 분리 (필수)

| Task | 범위 | 산출물 | 포함하지 않음 | 선행 | 추정 | 태스크 ID |
|---|---|---|---|---|---|---|
| A. 백엔드 — 공연 등록/수정/취소 API | `seat` 테이블 신설, 구역(행×열) 입력으로 좌석 생성, `POST/PUT /api/performances`, 취소 API, 유효성 검증 | `seat` 마이그레이션, `Seat` 도메인, `PerformanceService`/`Repository` 확장, 3개 엔드포인트 | 주최자 인증/소유권, 좌석 선점·예매, 등록 후 좌석 구성 변경 | — | 14h | |
| B. 프론트 — 공연 등록/수정 화면 | 등록 폼(기본 정보 + 구역 반복 입력 + 생성될 좌석 수 미리보기), 수정 폼(기본 정보만), 취소 액션 | 등록·수정 컴포넌트, API 연동, 기본/로딩/검증오류/제출오류 상태 | 인터랙티브 좌석 맵 드래그 편집기 | A | 12h | |

**분리 이유**: 좌석 생성 규칙(구역→행×열→개별 좌석, 총 좌석 수 상한)과 시각 순서
검증(`openAt <= closeAt <= startAt`)이 백엔드 핵심이라 프론트와 독립적으로
검증되어야 한다. 프론트는 구역 입력값과 미리보기 개수를 그대로 보내면 되므로
백엔드 완료 후 착수한다.

```
흐름
  A(백엔드 API) ──► B(프론트 화면)
```

### 1-3. 상세

**구역(section) → 좌석 생성 규칙**

주최자는 등록 시 구역을 1개 이상 입력한다. 구역마다:

| 필드 | 설명 | 예 |
|---|---|---|
| `grade` | 구역/등급 이름 | `VIP`, `R`, `S` |
| `price` | 좌석 1석 가격(원) | `120000` |
| `rowStart` / `rowEnd` | 행 범위(대문자 알파벳 1자) | `A` ~ `E` |
| `seatsPerRow` | 행당 좌석 수 | `20` |

서버는 구역마다 `rowStart`~`rowEnd` 각 행에 대해 `1`~`seatsPerRow` 번호로 좌석을
생성한다. 좌석 라벨은 `{grade}-{행}{번호}` (예: `VIP-A1`). `(performance_id, 행,
번호)` 조합은 유일해야 한다 — 구역 간 행 범위가 겹치면 409로 거부한다.

`performance.total_seats` = 등록된 모든 구역의 좌석 수 합. `available_seats` =
등록 시점에는 `total_seats` 와 동일 — 좌석별 선점/예매는 이 기능의 범위가
아니므로(§7) 등록 직후 모든 좌석은 "가용"으로 취급한다. 좌석 선점 기능이
이 값의 갱신 방식을 다시 정의한다(`docs/product/product.md` §7 열린 질문).

공연 하나의 좌석 총수는 5,000석을 넘을 수 없다 — 실수로 큰 숫자를 입력해
대량의 행이 생성되는 것을 막는 안전장치다. 초과 시 400.

**시각 순서 검증**: `openAt <= closeAt <= startAt` 을 만족해야 한다(서비스
계층 검증, DB 제약은 아니다 — 기존 `performance_seats_check` 제약은 `open_at
<= close_at` 만 다룬다). 위반 시 400.

**수정 범위**: 수정 API는 `title`/`venue`/`startAt`/`openAt`/`closeAt` 만
바꾼다. 좌석 구성(구역·가격)은 등록 후 변경할 수 없다(§7) — 이미 생성된
`seat` 행을 다시 계산하는 로직이 없기 때문이다. 수정은 `now < openAt` 일
때만 허용한다(오픈 이후에는 이미 노출된 정보라 변경하면 관객에게 혼란을
준다) — 위반 시 409.

**취소**: `cancelled` 플래그를 `true` 로 바꾼다. 시각·좌석 상태와 무관하게
언제든 가능하다(공연 자체 취소는 오픈 이후에도 발생할 수 있는 정상 흐름).
이미 취소된 공연을 다시 취소하면 멱등하게 200을 반환한다.

---

## 2. 화면 설계

**UI 라이브러리**: MUI (Material UI) — ADR-0002
**디자인 정본**: `docs/product/design.md` · `design-tokens.css`
**와이어프레임**: 없음 — 2-4 로 대신한다

### 2-1. 이 화면에서 사용자가 하려는 일

주최자가 공연 기본 정보와 구역별 좌석 구성을 입력해, 시스템이 실제 판매
가능한 개별 좌석을 만들어내는 것을 확인한다.

### 2-2. 참조

Eventbrite의 예약 좌석(Reserved Seating) 등록 흐름을 확인했다 — 구역
(section)을 만들고 각 구역에 행·좌석을 채운 뒤 구역별 가격 등급(ticket
tier)을 매기는 순서다.

| 서비스 | 이 화면에 해당하는 부분 | 우리에게 없는 것 |
|---|---|---|
| Eventbrite (Reserved Seating) | 구역 생성 → 행/좌석 추가 → 구역별 가격 등급 지정 → 인터랙티브 좌석 맵으로 미리보기 | 드래그로 배치하는 인터랙티브 좌석 맵, 통로/장애인석 지정, 좌석 맵 프리셋 |

**대조 결과**: 구역→행×열→가격의 입력 순서는 이번 범위에 그대로 반영한다.
드래그 기반 인터랙티브 좌석 맵과 통로/장애인석 지정은 폼 하나로 다루기엔
범위가 크므로 §7 범위 외로 둔다 — 이번엔 구역/행/열 숫자를 입력하면 좌석
목록을 텍스트로 미리보여주는 것으로 대신한다.

### 2-3. 담기는 것

**등록 폼**

| 우선 | 요소 | 왜 필요한가 | 데이터 출처 |
|---|---|---|---|
| 1 | 공연명·장소·공연일시 | 공연을 식별하는 기본 정보 | `performance.title`/`venue`/`start_at` |
| 2 | 오픈/마감 시각 | 예매 가능 기간을 정한다(§1-3) | `performance.open_at`/`close_at` |
| 3 | 구역 입력(반복 가능): 등급명·가격·행 범위·행당 좌석 수 | 개별 좌석 생성의 입력값(§1-3) | ⚠ 없음 — `seat` 테이블 신설, 구역 입력은 폼 전용 상태(저장되지 않음) |
| 4 | 생성될 좌석 수 미리보기(구역별·총합) | 실수로 큰 숫자를 입력했을 때 제출 전에 알아챈다 | 클라이언트 계산값(구역 입력을 그대로 합산) |
| 5 | 제출 버튼 | 등록을 확정한다 | — |

**수정 폼**

| 우선 | 요소 | 왜 필요한가 | 데이터 출처 |
|---|---|---|---|
| 1 | 공연명·장소·공연일시·오픈/마감 시각 (수정 가능) | §1-3 수정 범위 | `performance.*` |
| 2 | 좌석 구성 요약(읽기 전용): 구역별 등급·가격·좌석 수 | 이미 만들어진 좌석 구성을 확인만 한다(수정 불가, §1-3) | `seat` 집계 |
| 3 | 취소 버튼 | §1-3 취소 흐름 | `performance.cancelled` |

### 2-4. 배치

세로 폼 — 등록·수정 모두 "채워 넣고 제출하는" 단일 작업이라 처리할 항목
목록(그리드)이 아니라 순서대로 채우는 폼이 맞다.

```
등록 폼
┌─────────────────────────────────────────┐
│ 공연명 [__________________]              │
│ 장소   [__________________]              │
│ 공연일시 [____]  오픈 [____]  마감 [____] │
├─────────────────────────────────────────┤
│ 구역 1: 등급[VIP] 가격[______]            │
│         행[A]~[E]  행당 좌석수[20]        │
│         [+ 구역 추가]                    │
├─────────────────────────────────────────┤
│ 생성될 좌석: VIP 100석 · 총 100석 (미리보기)│
├─────────────────────────────────────────┤
│                          [등록]          │
└─────────────────────────────────────────┘

수정 폼
┌─────────────────────────────────────────┐
│ 공연명 [__________________]              │
│ 장소   [__________________]              │
│ 공연일시 [____]  오픈 [____]  마감 [____] │
├─────────────────────────────────────────┤
│ 좌석 구성 (읽기 전용)                     │
│  VIP · 120,000원 · 100석                 │
├─────────────────────────────────────────┤
│                    [저장]   [공연 취소]  │
└─────────────────────────────────────────┘
```

**이 배치인 이유**: 등록·수정 모두 순서가 있는 입력(기본 정보 → 좌석 구성 →
확정)이라 세로 폼으로 그린다. 구역은 반복 추가되는 항목이라 카드형 리스트로
쌓는다.

### 2-5. 상태별로 무엇이 보이나

| 화면 | 상태 |
|---|---|
| 공연 등록 | 기본 · 제출 중 · 검증 오류 · 제출 실패 · 성공 |
| 공연 수정 | 기본 · 로딩 · 저장 중 · 검증 오류 · 저장 실패 · 취소 확인 · 취소 완료 |

- **기본** — 등록: 빈 폼(구역 1개 기본 행). 수정: 기존 값으로 채워진 폼 + 좌석
  구성 요약
- **로딩(수정만)** — 공연 정보를 불러오는 동안 폼 자리에 스켈레톤
- **제출 중/저장 중** — 제출 버튼이 로딩 상태로 바뀌고 중복 클릭을 막는다
- **검증 오류** — `openAt <= closeAt <= startAt` 위반, 좌석 총수 5,000 초과,
  구역 행 범위 중복 등을 필드 아래 인라인 메시지로 표시한다
- **제출/저장 실패** — 폼 상단에 "등록하지 못했습니다. 다시 시도해 주세요"
  + `[다시 시도]`
- **성공(등록)** — 등록된 공연 상세 화면으로 이동
- **취소 확인** — "이 공연을 취소하시겠습니까?" 확인 다이얼로그
- **취소 완료** — 공연 상태가 취소로 바뀐 것을 화면에 반영

**이 상태 목록이 그대로 §5 완료 조건이 된다.**

**2-3 의 「데이터 출처」가 그대로 §3 API 명세가 된다.**

---

## 3. 데이터·API 명세

### 테이블: seat (신규)

```sql
CREATE TABLE seat (
    id              BIGSERIAL PRIMARY KEY,
    performance_id  BIGINT NOT NULL REFERENCES performance(id),
    grade           VARCHAR(50) NOT NULL,
    seat_row        VARCHAR(10) NOT NULL,
    seat_number     INT NOT NULL CHECK (seat_number > 0),
    seat_label      VARCHAR(30) NOT NULL,
    price           INT NOT NULL CHECK (price >= 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (performance_id, seat_row, seat_number)
);

CREATE INDEX idx_seat_performance_id ON seat (performance_id);
```

`performance` 테이블 스키마는 바뀌지 않는다 — `total_seats`/`available_seats`
는 이미 있는 컬럼을 그대로 쓰되, 값의 출처가 "테스트 픽스처가 직접 지정"에서
"등록 시 좌석 생성 결과로 계산"으로 바뀐다(§1-3).

### API

**POST /api/performances** — 공연 등록 (구역 → 좌석 생성 포함)

요청:

```json
{
  "title": "가을 재즈 콘서트",
  "venue": "OO홀",
  "startAt": "2026-10-01T19:00:00+09:00",
  "openAt": "2026-09-10T10:00:00+09:00",
  "closeAt": "2026-09-30T23:59:59+09:00",
  "sections": [
    { "grade": "VIP", "price": 120000, "rowStart": "A", "rowEnd": "B", "seatsPerRow": 10 },
    { "grade": "R",   "price": 80000,  "rowStart": "C", "rowEnd": "E", "seatsPerRow": 20 }
  ]
}
```

응답 201: 공연 상세와 동일한 구조(`performance-availability.md` §2 참조) +
`totalSeats`(생성된 좌석 총수).

응답 400 — 아래 중 하나라도 위반 시 `{"code": "...", "message": "..."}`:

| code | 조건 |
|---|---|
| `INVALID_TIME_ORDER` | `openAt <= closeAt <= startAt` 위반 |
| `SEAT_LIMIT_EXCEEDED` | 좌석 총수 > 5,000 |
| `EMPTY_SECTIONS` | `sections` 가 비어 있음 |

응답 409 — `DUPLICATE_SEAT_RANGE`: 구역 간 `(seat_row, seat_number)` 범위가 겹침

**PUT /api/performances/{id}** — 공연 수정 (기본 정보만)

요청: `title`/`venue`/`startAt`/`openAt`/`closeAt` (구역/좌석 필드 없음)

응답 200: 수정된 공연 상세
응답 404: `PERFORMANCE_NOT_FOUND`
응답 409: `REGISTRATION_ALREADY_OPEN` — `now >= openAt` 일 때
응답 400: `INVALID_TIME_ORDER`

**POST /api/performances/{id}/cancel** — 공연 취소

응답 200: 갱신된 공연 상세 (`cancelled: true`). 이미 취소된 공연도 200 반환(멱등).
응답 404: `PERFORMANCE_NOT_FOUND`

---

## 4. 흐름

```
주최자 ──► POST /api/performances { sections: [...] }
             │
             ▼
        PerformanceController
             │
             ▼
        PerformanceService ──► 시각 순서 검증 → 구역별 좌석 생성(seat_row × seat_number)
             │                                 → 좌석 총수 집계 → 5,000석 상한 검증
             ▼
        PerformanceRepository / SeatRepository ──► PostgreSQL (performance, seat)

주최자 ──► PUT /api/performances/{id}  (now < openAt 검증) ──► 동일 경로
주최자 ──► POST /api/performances/{id}/cancel ──► cancelled = true 갱신
```

---

## 5. 완료 조건

### Task A (백엔드)

- [ ] 구역 1개 이상으로 `POST /api/performances` 호출 시 201과 함께 각 구역의
      `rowStart`~`rowEnd` × `seatsPerRow` 만큼 `seat` 행이 생성된다
- [ ] 생성된 `performance.total_seats`/`available_seats` 가 생성된 좌석 총수와
      같다
- [ ] `openAt > closeAt` 또는 `closeAt > startAt` 이면 400과 `INVALID_TIME_ORDER`
      가 반환된다
- [ ] 좌석 총수가 5,000을 넘으면 400과 `SEAT_LIMIT_EXCEEDED` 가 반환된다
- [ ] 두 구역의 행 범위가 겹치면 409와 `DUPLICATE_SEAT_RANGE` 가 반환된다
- [ ] `now < openAt` 인 공연을 `PUT /api/performances/{id}` 로 수정하면 200과
      변경된 필드가 반영된 상세가 반환된다
- [ ] `now >= openAt` 인 공연을 수정하려 하면 409와 `REGISTRATION_ALREADY_OPEN`
      이 반환된다
- [ ] 존재하지 않는 id 로 수정/취소를 호출하면 404와 `PERFORMANCE_NOT_FOUND`
      가 반환된다
- [ ] `POST /api/performances/{id}/cancel` 호출 시 `cancelled` 가 `true` 로
      바뀌고, 이미 취소된 공연을 다시 호출해도 200이 반환된다(멱등)

### Task B (프론트)

- [ ] 등록 폼에서 기본 정보 + 구역 1개 이상을 입력하고 제출하면 공연이
      생성되고 상세 화면으로 이동한다
- [ ] 구역을 추가하면 "생성될 좌석 수" 미리보기가 즉시 갱신된다
- [ ] 시각 순서 위반(400) 응답을 받으면 관련 필드 아래 인라인 오류가 표시된다
- [ ] 좌석 총수 초과(400) 응답을 받으면 해당 구역 아래 오류 메시지가
      표시된다
- [ ] 등록 API 호출이 실패(5xx/네트워크)하면 폼 상단에 오류와
      `[다시 시도]` 가 표시된다
- [ ] 수정 화면 진입 시 기존 값이 폼에 채워지고 좌석 구성 요약이 읽기
      전용으로 표시된다
- [ ] 오픈 이후 공연을 수정하려 하면(409) "이미 오픈된 공연은 기본 정보를
      수정할 수 없습니다" 안내가 표시된다
- [ ] 취소 버튼 클릭 시 확인 다이얼로그가 뜨고, 확인하면 상태가 취소로
      반영된다

---

## 6. 구현 참고

- `backend/src/main/resources/db/migration/V2__create_seat.sql` — `seat` 테이블
  마이그레이션 (신규)
- `backend/src/main/java/com/example/ticket_booking/domain/Seat.java` — 좌석
  도메인 (신규)
- `backend/src/main/java/com/example/ticket_booking/repository/SeatRepository.java`
  — 좌석 저장/집계 (신규)
- `backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java`
  — 등록/수정/취소 로직, 구역→좌석 생성, 시각 순서 검증 추가 (기존 확장)
- `backend/src/main/java/com/example/ticket_booking/api/PerformanceController.java`
  — `POST /api/performances`, `PUT /api/performances/{id}`,
  `POST /api/performances/{id}/cancel` 추가 (기존 확장)
- `backend/src/main/java/com/example/ticket_booking/api/PerformanceExceptionHandler.java`
  — `INVALID_TIME_ORDER`/`SEAT_LIMIT_EXCEEDED`/`DUPLICATE_SEAT_RANGE`/
  `REGISTRATION_ALREADY_OPEN` 매핑 추가 (기존 확장)
- `frontend/src/api/performances.ts` — 등록/수정/취소 API 클라이언트 함수 추가
  (기존 확장)
- `frontend/src/pages/PerformanceRegisterPage.tsx` — 등록 폼 (신규)
- `frontend/src/pages/PerformanceEditPage.tsx` — 수정 폼 (신규)

---

## 7. 범위 외

- 주최자 인증/소유권 확인 — 인증 시스템이 아직 없다. 누구나 등록·수정·취소
  가능(`docs/product/product.md` §7 열린 질문, 무검증 등록 상태 유지)
- 인터랙티브 좌석 맵(드래그 배치), 통로·장애인석 지정 — Eventbrite 참조에서
  확인했으나(§2-2) 폼 기반 입력으로 대신한다. 필요해지면 별도 기능
- 좌석별 선점·예매 — 다음 기능. 이번 기능은 좌석을 "생성"만 하고 판매
  가능 상태로 둔다
- 등록 후 좌석 구성(구역·가격·좌석 수) 변경 — 이미 생성된 `seat` 를
  재계산하는 로직을 만들지 않는다. 공연 자체를 취소하고 다시 등록하는
  것으로 대신한다
- 자유석/스탠딩(좌석 미지정) 공연 — `docs/product/product.md` §7 열린 질문,
  이번 기능은 좌석 지정 공연만 다룬다
- 결제 — `docs/product/product.md` §4 범위 외

---

## 8. 주요 결정 사항

| 결정 | 이유 | 범위 | ADR |
|---|---|---|---|
| 주최자 인증 없이 등록/수정/취소를 허용한다 | 인증 시스템이 아직 없고, product.md §7 이 아직 열린 질문으로 남겨둔 사안이라 이번 기능에서 선제적으로 결정하지 않는다 | 이 기능만 — 인증 도입 시 재검토 | — |
| 좌석을 구역(등급)×행×열 입력으로 개별 `seat` 행 생성한다(좌석표 단위, 좌표/드래그 배치 아님) | 좌석 선점 기능이 좌석 단위 매핑을 쓰려면(architecture.md §4) 좌석이 개별 행으로 존재해야 한다. 인터랙티브 좌표 배치는 1인 개발 범위(product.md §6)에 비해 과함 | **전체** — 이후 좌석 선점 기능의 데이터 모델 기반이 된다 | ADR 승격 후보 |
| `total_seats`/`available_seats` 는 등록 시 좌석 생성 결과로 계산하고, 좌석별 선점 전까지는 `available_seats == total_seats` 로 유지한다 | 두 값이 손으로 관리되던 것(픽스처)에서 좌석 테이블 파생값으로 바뀌는 지점을 명시해야 좌석 선점 기능이 이 값의 갱신 책임을 이어받을 때 혼란이 없다 | 이 기능만 — 좌석 선점 기능이 갱신 규칙을 다시 정의 | — |
