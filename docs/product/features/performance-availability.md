# [관객] 공연 목록과 예매 상태

**상태**: 확정
**최종 갱신**: 2026-09-02

---

## 1. 요구사항

### 1-1. 배경

관객이 지금 예매 가능한 공연이 있는지 확인하려면 주최자에게 직접 연락해야 한다
(`docs/product/product.md` §2). 이 기능은 그 확인 과정을 화면으로 대체하는
첫걸음이다 — 관객이 공연 목록을 조회하고, 각 공연이 지금 예매 가능한지·매진인지를
문의 없이 바로 확인할 수 있게 한다.

좌석 단위 선택·선점은 이 기능의 범위가 아니다(§6). 이번 기능은 "무엇이 열려 있는가"
까지만 답한다 — "그중 어떤 좌석이 비어 있는가"는 다음 기능(좌석 선점)의 몫이다.

| AS-IS | TO-BE |
|---|---|
| 관객이 전화·메신저로 예매 가능 여부를 문의한다 | 화면에서 공연 목록과 상태(예매가능/매진/예정)를 바로 본다 |
| 주최자가 문의마다 응대한다 | 주최자 응대 없이 관객이 스스로 확인한다 |

### 1-2. 태스크 분리 (필수)

| Task | 범위 | 산출물 | 포함하지 않음 | 선행 | 추정 | 태스크 ID |
|---|---|---|---|---|---|---|
| A. 백엔드 — 공연 목록/상세 조회 API | `performance` 테이블, 상태 계산(Clock 기반), 목록(필터·페이지네이션)/상세 API | `performance` 마이그레이션, `PerformanceController`/`Service`/`Repository`, `GET /api/performances`, `GET /api/performances/{id}` | 공연 등록/수정 API(쓰기), 좌석 단위 데이터·API | — | 10h | TASK-001 |
| B. 프론트 — 공연 목록/상세 화면 | 목록 화면(카드 목록, 상태 배지, 필터), 상세 화면(공연 정보 + 상태) | 목록·상세 컴포넌트, API 연동, 기본/로딩/빈 목록/오류 상태 | 좌석 선택 UI, 결제 진입 | A | 8h | TASK-002 |

**분리 이유**: 백엔드는 상태 계산 규칙(§1-3)과 시각 처리(Clock)가 핵심이라 프론트와
독립적으로 검증 가능해야 한다. 프론트는 API 응답의 `status` 값을 그대로 배지로
표시하면 되므로 백엔드 완료 후 착수한다.

```
흐름
  A(백엔드 API) ──► B(프론트 화면)
```

[태스크 생성 2026-09-03]

### 1-3. 상세

공연(Performance)마다 예매 상태를 아래 5가지로 계산해 보여준다. DB 컬럼이 아니라
시각과 좌석 수로 계산되는 값이다 (단, 취소는 명시적 플래그).

| 상태 | 조건 |
|---|---|
| 예정 (UPCOMING) | `now < openAt` |
| 예매가능 (OPEN) | `openAt <= now <= closeAt` 이고 `availableSeats > 0` |
| 매진 (SOLD_OUT) | `openAt <= now <= closeAt` 이고 `availableSeats == 0` |
| 마감 (CLOSED) | `now > closeAt` |
| 취소 (CANCELLED) | `cancelled == true` (시각·좌석 수와 무관하게 우선 적용) |

`availableSeats` 를 누가 언제 줄이는지는 이 기능의 범위가 아니다(§6) — 다음 기능
(좌석 선점)이 좌석 단위로 관리하게 되면 이 필드의 갱신 방식도 함께 재검토한다
(→ `docs/product/product.md` §7 열린 질문).

목록은 페이지네이션과 상태 필터를 지원한다. 공연 시작이 임박한 순(start_at 오름차순)
으로 보인다. 목록은 항상 `start_at >= now` 인 공연만 포함한다 (상태 필터와 무관하게
적용되는 조건) — 지난 공연은 예매 문의 대상이 아니므로 이 기능에서 다루지 않는다.
지난 공연 조회(이력 조회)는 범위 밖이다(§6).

`status=CLOSED` 필터는 "지난 공연"과 다르다 — 공연은 아직 열리지 않았어도
(`start_at` 이 미래여도) 마감 시각이 지나 CLOSED 일 수 있다(조기 마감). 위 `start_at
>= now` 조건과 별개로 함께 적용된다.

---

## 2. 데이터·API 명세

### 테이블: performance (신규)

```sql
CREATE TABLE performance (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    venue            VARCHAR(200) NOT NULL,
    start_at         TIMESTAMPTZ NOT NULL,
    open_at          TIMESTAMPTZ NOT NULL,
    close_at         TIMESTAMPTZ NOT NULL,
    total_seats      INT NOT NULL CHECK (total_seats > 0),
    available_seats  INT NOT NULL CHECK (available_seats >= 0 AND available_seats <= total_seats),
    cancelled        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_performance_start_at ON performance (start_at);
```

이번 기능은 이 테이블에 대한 쓰기 API 를 만들지 않는다 — 초기 데이터는 테스트
픽스처/시드로 넣는다. 주최자 등록 API 는 별도 기능 문서에서 다룬다(§6).

**기준 시각**: 상태 판정(§1-3)과 목록 필터링(`start_at >= now`)의 기준 시각은
애플리케이션이 주입하는 `java.time.Clock` 으로 얻는다. 쿼리에 `now()` 를 직접
쓰지 않는다 — DB 의 `now()` 를 쓰면 오픈 정각·마감 1초 전 같은 경계 시나리오를
테스트에서 고정된 시각으로 재현할 수 없다. `PerformanceService` 는 `Clock` 을
주입받고, 테스트는 `Clock.fixed(...)` 로 대체한다. (근거: ADR-0005)

### API

**GET /api/performances** — 공연 목록

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| status | string | 아니오 | `UPCOMING`\|`OPEN`\|`SOLD_OUT`\|`CLOSED`\|`CANCELLED` 중 하나로 필터 |
| page | int | 아니오 | 기본 0 |
| size | int | 아니오 | 기본 20, 최대 100 |

응답 200:

```json
{
  "content": [
    {
      "id": 1,
      "title": "가을 재즈 콘서트",
      "venue": "OO홀",
      "startAt": "2026-10-01T19:00:00+09:00",
      "openAt": "2026-09-10T10:00:00+09:00",
      "closeAt": "2026-09-30T23:59:59+09:00",
      "totalSeats": 200,
      "availableSeats": 37,
      "status": "OPEN"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

**GET /api/performances/{id}** — 공연 상세

응답 200: 목록의 항목과 동일한 필드 구조 하나.
응답 404: 해당 id 가 없을 때 — `{"code": "PERFORMANCE_NOT_FOUND", "message": "..."}`

---

## 3. 흐름

```
관객 ──► GET /api/performances(?status,page,size)
           │
           ▼
        PerformanceController
           │
           ▼
        PerformanceService ──► 상태 계산 (openAt/closeAt/availableSeats/cancelled)
           │
           ▼
        PerformanceRepository ──► PostgreSQL (performance)
```

상세 조회(`GET /api/performances/{id}`)도 동일한 경로를 거치되 단건 조회 후
없으면 404 를 반환한다.

---

## 4. 완료 조건

### Task A (백엔드)

- [ ] `GET /api/performances` 호출 시 200과 함께 `content`/`page`/`size`/`totalElements`
      형식으로 목록이 반환된다
- [ ] `status=OPEN` 필터 시 `openAt <= now <= closeAt` 이고 `availableSeats > 0` 인
      공연만 반환된다
- [ ] `availableSeats == 0` 인 공연은 `status: "SOLD_OUT"` 으로 반환된다
- [ ] `cancelled == true` 인 공연은 시각·좌석 수와 무관하게 `status: "CANCELLED"` 로
      반환된다
- [ ] `start_at < now` 인 공연은 상태 필터와 무관하게 목록에서 제외된다
- [ ] 존재하지 않는 id 로 `GET /api/performances/{id}` 호출 시 404 와
      `PERFORMANCE_NOT_FOUND` 코드가 반환된다
- [ ] `Clock` 을 오픈 정각·마감 1초 전으로 고정한 테스트에서 경계값이 올바른
      `status` 를 반환한다 (SQL `now()` 를 쓰지 않았는지 리뷰로 확인)

### Task B (프론트)

- [ ] 목록 화면 진입 시 API 응답의 공연이 카드로 표시되고 각 카드에 상태 배지가
      보인다
- [ ] 로딩 중에는 카드 자리에 스켈레톤이 표시된다
- [ ] 목록이 빈 배열이면 "예정된 공연이 없습니다" 안내가 표시된다
- [ ] 목록 API 호출이 실패하면 오류 메시지와 `[다시 시도]` 버튼이 표시된다
- [ ] 상세 화면에서 공연명·장소·일시·오픈/마감 시각·잔여 좌석·상태 배지가
      표시된다
- [ ] 존재하지 않는 공연 상세로 진입(404)하면 "존재하지 않는 공연입니다" 안내와
      목록으로 돌아가는 버튼이 표시된다

---

## 5. 구현 참고

- `backend/src/main/resources/db/migration/V1__create_performance.sql` — `performance`
  테이블 마이그레이션 (신규, 마이그레이션 도구 미정 — Flyway 권장)
- `backend/src/main/java/com/ticketbooking/repository/PerformanceRepository.java` —
  조회 쿼리 (신규)
- `backend/src/main/java/com/ticketbooking/service/PerformanceService.java` —
  `Clock` 주입, 상태 계산 로직 (신규)
- `backend/src/main/java/com/ticketbooking/api/PerformanceController.java` —
  `GET /api/performances`, `GET /api/performances/{id}` (신규)
- `frontend/src/api/performances.ts` — API 클라이언트 (신규)
- `frontend/src/pages/PerformanceListPage.tsx` — 목록 화면 (신규)
- `frontend/src/pages/PerformanceDetailPage.tsx` — 상세 화면 (신규)

### 화면 명세

**UI 라이브러리**: MUI (Material UI) — ADR-0002
**시안**: 없음

| 화면 | 상태 |
|---|---|
| 공연 목록 | 기본 · 로딩 · 빈 목록 · 오류 |
| 공연 상세 | 기본 · 로딩 · 오류(404) |

#### 상태별로 무엇이 보이나

**공연 목록**

- **기본** — 공연이 카드 목록으로. 각 카드에 제목 · 장소 · 공연 일시 · 상태 배지
  (예매가능/매진/예정/마감/취소, 색으로 구분)
- **로딩** — 카드 자리에 스켈레톤 3~6개
- **빈 목록** — "예정된 공연이 없습니다"
- **오류** — "목록을 불러오지 못했습니다" + `[다시 시도]` 버튼

**공연 상세**

- **기본** — 공연명 · 장소 · 공연 일시 · 예매 오픈/마감 시각 · 잔여 좌석 수 ·
  상태 배지
- **로딩** — 상세 영역에 스켈레톤
- **오류(404)** — "존재하지 않는 공연입니다" + `[목록으로]` 버튼

**이 상태 목록이 그대로 §4 완료 조건이 된다.** 적어놓고 완료 조건에 없으면 검증되지 않는다.

**이 화면이 필요로 하는 데이터가 §2 API 명세가 된다.** 화면을 먼저 적고 API 를 도출하면
"만들고 보니 이 필드가 없다"를 피할 수 있다.

---

## 6. 범위 외

- 좌석 단위 선택·선점 — 다음 기능. 이번 기능은 공연 전체의 집계 상태만 다룬다
- 결제 — `docs/product/product.md` §4 범위 외 (이번 단계 전체 제외)
- 공연 등록·수정 API(주최자) — 별도 기능 문서. 이번 기능은 읽기 전용
- 지난 공연 이력 조회 — `start_at < now` 인 공연은 이번 기능에서 조회할 방법을
  만들지 않는다
- 텍스트 검색(제목·장소 키워드) — 이번엔 상태 필터만 지원. 검색은 나중에 할 수
  있으나 이번 단계에는 없다

---

## 7. 주요 결정 사항

| 결정 | 이유 | 범위 | ADR |
|---|---|---|---|
| 예매 상태(UPCOMING/OPEN/SOLD_OUT/CLOSED/CANCELLED)를 DB 컬럼이 아니라 시각·좌석 수로 계산 | 시각이 지나도 컬럼을 갱신하지 않으면 실제 상태와 어긋난다 | 이 기능만 | — |
| 시각 기준 판정에 `java.time.Clock` 주입을 쓰고 SQL `now()` 를 쓰지 않는다 | 오픈 정각·마감 1초 전 같은 경계 시나리오를 테스트에서 재현 가능하게 하기 위함 | **전체** — 좌석 선점 만료 등 다른 시각 기반 판정에도 동일하게 적용될 원칙 | ADR-0005 |
