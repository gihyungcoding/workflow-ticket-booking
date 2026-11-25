# [관객] 예매 흐름 최소 경로 — 좌석 선점 → 무통장 예약 → 입금 확인 → 확정 (REQ-002)

**상태**: 확정
**최종 갱신**: 2026-11-25
**계획**: `workflow_design/01_brief/BRIEF_REQ-002.json` (HITL#0 확정 2026-11-25, 결정 D1~D42 · 완전성 C1~C10 · AC-1~12)

---

## 1. 요구사항

### 1-1. 배경

공연 상세의 「예매하기」는 지금 대기열 자리표시 화면(`QueuePlaceholderPage`)으로 간다. 좌석을 고르고
잡는 길이 없고, 예매(`booking`) 테이블도 없다. 운영자 역할이 없어 입금 확인 API 를 누가 부를지도
정해지지 않았다(ADR-0029 의 미정 항목).

| AS-IS | TO-BE |
|---|---|
| 좌석을 고를 화면·API 가 없다 | 로그인한 관객이 구역 좌석도에서 1~4석을 골라 7분 동안 선점한다 |
| 예매 기록이 없다 | 선점한 좌석으로 무통장 예약 — 예매번호·입금 계좌·금액·입금 기한(예약 당일 오후 11:59) |
| 입금 확인 API·운영자 역할이 없다 | ADMIN 이 예매번호로 입금 확인을 기록하면 예매가 확정된다 (ADR-0036) |
| 만료를 처리할 장치가 없다 | 선점 7분·입금 기한 경과는 조회 시점에 판정해 좌석이 다시 판매된다 (ADR-0037) |
| 내 예매 화면이 없다 | 내 예매 목록·상세에서 입금 대기·예매 확정·취소를 본다 |

이 요청은 PRD 예매 흐름의 **최소 경로**다(D1). 대기열·연석 자동 선점·직접 취소·양도는 후속 요청(§7).

### 1-2. 태스크 분리 (필수)

| Task | 범위 | 산출물 | 포함하지 않음 | 선행 | 추정 | 태스크 ID |
|---|---|---|---|---|---|---|
| A1. 백엔드 — 좌석 잠금 스키마·선점·해제·좌석 조회 API (동시성, PostgreSQL 테스트 베이스) | `seat_lock` 스키마, 좌석 조회(로그인 관객만, 매처 순서), 선점 1~4석 전부-아니면-없음·입력 검증·오름차순·반환 수 대조, 사용자당 활성 선점 1개 대체, 공연별 1인 4석 합계, 선점 해제(본인·미예약만), 에러 코드·`ErrorResponse` 선택 필드, 마이그레이션 버전 단언 3개 갱신, Testcontainers 공용 베이스와 동시 선점 테스트, 설정값(7분·4석) | `seat_lock` 마이그레이션, `GET /api/performances/{id}/seats`, `POST /api/seat-holds`, `DELETE /api/seat-holds/{holdId}`, `SecurityConfig` 좌석 조회 매처, `BookingExceptionHandler`(신규), `ErrorResponse` 선택 필드 | 예약(`booking`)·예매번호, ADMIN 역할, 입금 확인, 대기열 입장권 검사, 매수 지정 연석 자동 선점 | — | 15h | TASK-032 |
| A2. 백엔드 — 무통장 예약·예매번호·ADMIN 역할·가입 차단·보안 매처 | `booking`·`booking_seat`·예매번호 시퀀스, 예약(선점 → 입금 대기, 조건부 전환, 같은 hold 재예약 200), 입금 기한 계산(23:59 Asia/Seoul), 응답에 예매번호·계좌·금액·기한, `UserRole.ADMIN`, 가입 API 두 곳 ADMIN 400, 프론트 `UserRole`·`ROLES` 에 `'ADMIN'`, `/api/admin/**` `hasRole(ADMIN)` 매처, 입금 계좌·기한 설정값 | `booking` 마이그레이션, `POST /api/bookings`, `UserRole.ADMIN`, `SignUpRequest`·`CompleteSocialSignUpRequest` 검증, `SecurityConfig` 관리자 매처, `frontend/src/api/auth.ts` 타입 | 입금 확인 API, 내 예매 조회 API, ADMIN 화면·계정 생성 화면, 카드·간편결제 | A1 | 10h | TASK-033 |
| B. 백엔드 — 입금 확인 API(ADMIN)·기한 판정·재판매·남은 좌석 조회 시점 계산 | 예매번호로 입금 확인(조건부 쓰기, 기한 뒤 409·쓰기 없음, 이미 확정이면 200), DB 역할 재확인·`confirmed_by`·`confirmed_at`, 예매 상태 파생(`CONFIRMED`·`PENDING_DEPOSIT`·`CANCELLED`), 내 예매 목록·상세 조회 API, 취소된 예약 좌석의 재판매, 공연의 남은 좌석 수를 조회 시점에 계산(전체 − 유효한 잠금)해 공연 상태(OPEN/SOLD_OUT)·공개 목록 매진 필터·주최자 응답에 반영 | `POST /api/admin/bookings/by-number/{bookingNumber}/confirm-deposit`, `GET /api/bookings/me`, `GET /api/bookings/{id}`, `PerformanceStatusRules`·`PerformanceRepositoryImpl`·`OrganizerPerformanceResponse` 의 남은 좌석 계산 | 은행 입금 자동 연동, 확정·취소 알림(메일·문자), 관객 직접 취소·환불, 운영자 화면 | A2 | 10.5h | TASK-034 |
| C. 프론트 — 좌석 선택(확대·미니맵)·예약·예약 완료 화면 | 좌석 화면(구역 전체 → 확대 44px·미니맵, 등급 전환, 1~4석 선택, 상태 6장), 선점 완료·예약 화면(남은 시간·다시 고르기 = 해제·떠나면 해제), 예약 완료 화면(입금 정보 묶음·계좌 복사), 모바일 폭 한 줄(데스크톱도 같음, D41), 공통 부품 `picker`·`picker-mini`·`due`, 공연 상세 「예매하기」 → 좌석 화면, 비로그인 → 로그인 | 모바일 `seats`·`seats--*`·`hold`·`reserved`, `components/ui` 공통 부품 3종 (§2), `api/` 의 선점·예약 호출 | 내 예매 목록·상세, 대기열 화면, 매수 지정 자동 배정, 카드·간편결제 수단 | A2 | 16h | TASK-035 |
| D. 프론트 — 내 예매 목록·상세 화면 | 내 예매 목록(입금 대기·확정·취소, 빈 상태), 예매 상세(진행 단계 선점 → 예약 → 입금 확인 → 확정, 입금 정보 묶음 상태별, 취소 화면 「다시 예매하기」), 모바일 폭 한 줄(D41), 공통 부품 `due` 재사용 | 모바일 `bookings`·`bookings--empty`·`bookings--confirmed`·`booking`·`booking--confirmed`·`booking--cancelled` (§2) | 직접 취소·양도 버튼(「아직 할 수 없어요」 한 줄만), 모바일 티켓, 알림 | B | 8h | TASK-036 |

[태스크 생성 2026-11-25]

**분리 이유**: 백엔드를 좌석 잠금(A1)과 예약·역할(A2)로 나눈 것은 D31 — 동시성·매처 순서·PostgreSQL
테스트 베이스를 갖는 A1 만으로 15h 라 예약·ADMIN 까지 합치면 상한을 넘는다. 입금 확인(B)은 기한 경계와
권한 재확인이 핵심이라 따로 검증한다. 프론트는 시안 묶음(좌석 선점·무통장 예약 / 내 예매)대로 둘이고,
C 는 예약 응답(A2)이, D 는 상태 파생·조회 API(B)가 있어야 화면을 만든다.

```
흐름
  A1(좌석 잠금·선점·조회) ──► A2(예약·ADMIN·매처) ──┬──► B(입금 확인·상태·조회) ──► D(내 예매)
                                                   └──► C(좌석·예약·예약 완료)
         [시안 workflow_design/01_brief/mockup/REQ-002/ — rev 6 승인 2026-11-25]
```

### 1-3. 상세

규칙의 정본은 BRIEF 결정 행과 ADR 이다. 요점만:

- **시간 값** — 선점 7분, 입금 기한 예약 당일 23:59:00 Asia/Seoul, 한 번에·1인 최대 4석. 설정 한 곳(D16)
- **유효** — 잠금 유효 = `now < expires_at`, 판매 확정 = `expires_at IS NULL`. `now` 는 앱 `Clock` (ADR-0037, D36)
- **동시성** — 유니크 제약 + 조건부 UPSERT 한 문장, 부분 선점 없음 (ADR-0004, D26·D27)
- **1인 상한** — 사용자당 활성 선점 1개(새 선점이 이전 미예약 선점을 대체) + 공연별 「입금 대기 + 확정」 합계 4석 (D32)
- **예매 상태** — 저장하지 않고 만료 시각에서 파생 (D15)
- **남은 좌석 수** — 저장하지 않고 조회 시점에 전체 − 유효한 잠금으로 계산. 공연 상태·공개 목록 매진 필터·주최자 응답이 쓴다 (D40, ADR-0037)
- **권한** — 좌석 조회·선점·예약·내 예매는 로그인 관객, 입금 확인은 ADMIN (ADR-0036, D24·D30·D34)
- **멱등** — 같은 hold 재예약은 기존 예약 200, 이미 확정된 예매의 입금 확인은 200 (D18·D37)
- **알려진 한계** — 동명이인·같은 금액 입금은 운영자가 예매번호로 구분한다 (D23)

---

## 2. 화면 설계

**UI 라이브러리**: MUI (Material UI) — ADR-0002
**디자인 정본**: `docs/product/design.md` · `design-tokens.css`
**시안** [rev 6 승인 2026-11-25]: 캔버스 `workflow_design/01_brief/mockup/REQ-002/index.html` — 화면 목록
`screens.json`, 여정 `journey.md`, 데이터 묶음 `layout.md`.
**구현 기준은 모바일 화면** `workflow_design/01_brief/mockup/REQ-002/screens/*.html` — 이 화면들의 정본이다(ADR-0038, D42).
앱은 데스크톱에서도 모바일 폭 한 줄(`appMaxWidth`)이라 데스크톱 시안 `screens/desktop/` 은 **보관(D41)** — 구현하지 않는다.
화면 코드는 그 화면의 정본 시안과 같아야 한다. 아래 ASCII 는 요약이고, 다르면 시안이 이긴다.
대체된 `out/` 화면(`seat`·`seat--error`·`checkout`·`booked`·`my-bookings`)과의 대응은 `design.md` §0

### 2-1. 이 화면에서 사용자가 하려는 일

관객이 원하는 좌석을 지금 내 것으로 잡고, 어디로 얼마를 언제까지 보내면 예매가 확정되는지 알고,
입금 뒤 연락 없이 확정을 확인한다.

### 2-2. 참조

이 요청에서는 상용 서비스를 따로 열어 대조하지 않았다 — 국내 공연 예매 앱의 일반 골격(좌석도 → 선점 →
결제 수단 → 예매 완료 → 내 예매)을 기본값으로 따랐다(`journey.md` 머리말).

| 서비스 | 이 화면에 해당하는 부분 | 우리에게 없는 것 |
|---|---|---|
| 국내 공연 예매 앱 일반 골격 | 좌석도 → 선점 → 결제 → 예매 완료 → 예매 내역 | 카드·간편결제, 매수 지정 자동 배정, 취소·양도, 모바일 티켓, 알림 |

**대조 결과**: 없는 것은 모두 범위 외(§7). 예매번호는 운영자가 입금과 예약을 맞추는 데 필요해 이번에 넣는다(D8).

### 2-3. 담기는 것

| 우선 | 요소 | 왜 필요한가 | 데이터 출처 |
|---|---|---|---|
| 1 | 좌석도 (고를 수 있음 · 고를 수 없음 · 내가 고름) | 고를 자리 | `seat` + ⚠ 신규 `seat_lock` 파생 `available` (`GET /api/performances/{id}/seats`) |
| 2 | 등급별 남은 석수 | 붙은 자리가 남았나 | ⚠ 신규 — 좌석 조회 응답 |
| 3 | 선점 남은 시간 | 7분 안에 예약 | ⚠ 신규 — 선점 응답의 만료 시각, 서버 시각 오프셋(`/api/time`, ADR-0024) |
| 4 | 금액 | 얼마를 보내나 | `seat.price` 합 |
| 5 | 입금 기한 | 언제까지 | ⚠ 신규 — 예약 응답 `depositDeadline` |
| 6 | 입금 계좌·예금주 | 어디로 | ⚠ 신규 — 서비스 고정 계좌 설정값(`journey.md` §6 가정) |
| 7 | 입금자명 | 누구 이름으로 | `users.name` (입금자명 칸 없음, D9) |
| 8 | 예매번호 | 문의·입금 대조 | ⚠ 신규 — `booking.booking_number` (DB 시퀀스, D37) |
| 9 | 예매 상태 | 확정됐나 | ⚠ 신규 — 만료 시각에서 파생 (D15) |
| 10 | 입금 확인 시각 | 언제 반영됐나 | ⚠ 신규 — `booking.confirmed_at` (D38) |
| 11 | 공연명·일시·장소 | 무엇을 예매했나 | `performance.title`·`start_at`·`venue` |

### 2-4. 배치

**화면 목록 — 태스크별** (`screens.json`)

| 태스크 | 묶음 | 모바일 (375) — 구현 기준 | 데스크톱 (1280) — 보관(D41) |
|---|---|---|---|
| C | 좌석 선점 | `seats--overview` · `seats` · `seats--taken` · `seats--limit` · `seats--expired` · `seats--released` | `d-seats` · `d-seats--taken` · `d-seats--limit` · `d-seats--expired` · `d-seats--released` |
| C | 무통장 예약 | `hold` · `reserved` | `d-hold` · `d-reserved` |
| D | 내 예매 | `bookings` · `bookings--empty` · `bookings--confirmed` · `booking` · `booking--confirmed` · `booking--cancelled` | `d-bookings` · `d-bookings--empty` · `d-bookings--confirmed` · `d-bookings--cancelled` |

좌석 화면 상태 6장은 「한 화면 최대 3장」 규칙의 예외다 — 모두 완료 조건 장면(D11).

**데이터 묶음** (`layout.md` 「데이터 묶음」 — 화면 사이에서 같은 순서·부품으로)

| 묶음 | 항목(순서) | 쓰는 화면 | 모양 |
|---|---|---|---|
| `payment` | amount → deadline → account | `reserved`·`booking`·`booking--confirmed`·`booking--cancelled` | 블록 「입금 정보」: 금액(제목 크기 숫자 + "R석 99,000원 × 3") → 기한 줄(키트 `due` — 대기 = 남은 시간(경고) + 오늘 오후 11:59까지, 확정 = `is-done` 입금 확인 시각, 취소 = `is-off` 기한 지남) → 계좌 행(은행 아이콘 + 은행·계좌번호 + 예금주·입금자명 + 복사). **확정·취소는 계좌 행을 뺀다**. 취소 화면 금액 이름표는 「예약 금액」 |
| `booking` | show → when → seats → number | 예약 완료·예매 상세·내 예매 목록 행 | 티켓 일러스트 행: 공연명 → 일시·장소 → 좌석 → 예매번호 |

**모바일 — 좌석 선점 (C)**
```
┌─────────────────────────────┐
│ 공연명 · 일시     [R석|S석]  │
│ 남은 좌석 R 52 · S 23        │
│ ┌ 미니맵 (구역·무대·보는 범위)┐│
│ └─────────────────────────┘ │
│   무대                        │
│ A □ □ ■ ■ □                   │  ← 처음은 구역 전체(누를 수 없음),
│ B ■ ▣ ▣ ▣ □   44px 좌석       │    누른 곳 주변을 확대 (D10)
│ (상태 안내 한 줄: 빠짐·한도·만료·해제)│
│ 고른 좌석 3 / 4 · 297,000원   │
│ [좌석 선점하기]               │
└─────────────────────────────┘
```

**데스크톱** — 보관(D41). 시안의 보조 열·목록·상세 나란히 배치(`layout.md` §3)는 구현하지 않는다 — 데스크톱에서도 위 모바일 화면이 한 줄로 보인다.
앱 전체 데스크톱 배치는 별도 요청.

**이 배치인 이유**: 좌석은 위치를 따라 놓이고(좌석도가 가장 큰 면적) 최대 4석·7분이라 선택 수·남은 시간·금액이
늘 보여야 한다. 관객 한 명의 예매는 몇 건이라 상태 보드 대신 목록 → 상세다.

### 2-5. 상태별로 무엇이 보이나

| 화면 | 상태 |
|---|---|
| 좌석 선택 (C) | 구역 전체 · 확대 고르기 · 먼저 잡힌 좌석 · 4석 한도 · 선점 만료 · 선점 해제 · 로딩 · 비로그인 |
| 선점 완료·예약 (C) | 기본(남은 시간) · 예약 중 · 시간 0 |
| 예약 완료 (C) | 기본 · 기한까지 1시간 미만 |
| 내 예매 목록 (D) | 기본 · 빈 목록 · 확정 반영 · 로딩 · 오류 |
| 예매 상세 (D) | 입금 대기 · 예매 확정 · 자동 취소 · 없는 예매 |

- **구역 전체** — 구역의 빈자리 분포만 보이고 좌석은 누를 수 없다. 누른 곳 주변이 44px 로 확대된다(D10)
- **먼저 잡힌 좌석** — 빠진 좌석 번호와 「아직 선점 전이에요」, 남은 고른 좌석으로 다시 선점(AC-2, D17). 부분 선점은 없다
- **4석 한도** — 5번째 좌석은 고르지 않고 4석 한도를 알린다(AC-3)
- **선점 만료** — 7분이 지나 선점이 풀렸고 다시 고를 수 있다(AC-8). 시간 0 에서 클라이언트가 전환하고 판정은 서버(ADR-0026)
- **선점 해제** — 「좌석 다시 고르기」·뒤로·다른 화면 이동은 선점을 바로 푼다(AC-10, D7·D12). 예약 화면에 이 안내 한 줄.
  **예약에 성공한 뒤의 이동은 해제를 보내지 않는다**(D35)
- **비로그인** — 좌석 조회가 401 이면 로그인으로 안내하고 돌아온다(AC-11, D30)
- **예약 완료** — 입금 정보 묶음(대기). 기한까지 1시간 미만이면 「입금 시간이 짧아요」(D22). 계좌 복사
- **입금 대기 상세** — 진행 단계, 입금 정보 묶음(대기), 「열 때마다 최신으로 확인돼요」(D20). 직접 취소·양도는 「아직 할 수 없어요」 한 줄(D6)
- **예매 확정** — 「예매 확정」 배지, 입금 정보 묶음(확정 — 입금 확인 시각, 계좌 없음)(AC-5, D21)
- **자동 취소** — 배지 없이 평문 「입금 기한이 지나 취소됨」, 금액 이름표 「예약 금액」, 「예매번호로 문의해 주세요」(D9), 「다시 예매하기」 → 구역 전체(D19)(AC-6)
- **빈 목록** — 아직 예매가 없다는 안내와 공연 보러 가기

**이 상태 목록이 그대로 §5 완료 조건이 된다.**

---

## 3. 데이터·API 명세

계약은 D13 이 정본이다. Phase 1 PLAN 이 계약표로 고정한다.

| 메서드 · 경로 | 누가 | 요청 | 응답 / 오류 | 태스크 |
|---|---|---|---|---|
| `GET /api/performances/{id}/seats` | 로그인 관객 | — | 좌석별 `available`, 등급별 남은 석수 / 401 | A1 |
| `POST /api/seat-holds` | 로그인 관객 | `performanceId`, `seatIds`(1~4) | `holdId`, 만료 시각 / `SEAT_UNAVAILABLE` 409(빠진 id 목록) · `SEAT_LIMIT_EXCEEDED` 400 | A1 |
| `DELETE /api/seat-holds/{holdId}` | 본인 | — | 204 (이미 예약된 hold 도 204 no-op) / `HOLD_NOT_OWNED` 404 | A1 |
| `POST /api/bookings` | 로그인 관객 | `holdId` | `bookingNumber`·계좌·금액·`depositDeadline` (같은 hold 재요청 200) / `HOLD_EXPIRED` 409 · `HOLD_NOT_OWNED` 404 | A2 |
| `POST /api/admin/bookings/by-number/{bookingNumber}/confirm-deposit` | ADMIN | — | 금액·예매자·좌석 (이미 확정 200) / `DEPOSIT_DEADLINE_PASSED` 409 · `BOOKING_NOT_FOUND` 404 | B |
| `GET /api/bookings/me` | 로그인 관객 | `page`/`size` (0/20) | 최신순 목록, 파생 상태 | B |
| `GET /api/bookings/{id}` | 본인 | — | 상세, 파생 상태 / `BOOKING_NOT_FOUND` 404 | B |

- camelCase, 시각은 ISO, 서버 시각(`/api/time`) 기준(D13)
- `ErrorResponse` 는 2-인자 생성자를 유지하고 선택 필드를 `@JsonInclude(NON_NULL)` 로 더한다(D28) — 추가형
- 테이블 모양은 `docs/architecture/architecture.md` §4-1 (`seat_lock`, `booking`, `booking_seat`)

---

## 4. 흐름

```
관객 ──► 좌석 조회 ──► 선점(7분) ──┬─► 다시 고르기 / 떠남 ──► 해제 ──► 좌석 조회
                                   ├─► 7분 경과 ──────────────► (조회 시점) 다시 판매
                                   └─► 무통장 예약 ──► 입금 대기(기한 당일 23:59)
                                                        │
운영자(ADMIN) ── 예매번호로 입금 확인 ──────────────────┤
                                                        ├─► 기한 안 ──► 예매 확정 (confirmed_by·at)
                                                        └─► 기한 뒤 ──► 409, 쓰기 없음 / (조회 시점) 취소·다시 판매
관객 ──► 내 예매 목록·상세 ──► 파생 상태(입금 대기 · 예매 확정 · 취소)
```

---

## 5. 완료 조건

### Task A1 (백엔드 — 좌석 잠금·선점·해제·좌석 조회)

- [ ] 관객이 좌석 1~4개를 선점하면 7분 동안 다른 관객의 같은 좌석 선점은 `SEAT_UNAVAILABLE` 409 로 거절된다 (AC-1)
- [ ] 이미 선점·판매된 좌석이 섞인 선점은 아무 좌석도 잡지 않고 409 와 빠진 좌석 id 목록을 돌려준다 (AC-2)
- [ ] 5석 이상 선점 요청은 `SEAT_LIMIT_EXCEEDED` 400 으로 거절된다. 중복 id·다른 공연 좌석·비공개 공연도 거절된다 (AC-3)
- [ ] 두 관객이 같은 좌석을 동시에 선점하면 한 명만 성공한다 — PostgreSQL(Testcontainers) 테스트 (AC-7)
- [ ] 선점 7분이 지난 좌석은 다른 관객이 선점할 수 있다 — `Clock` 을 옮겨 확인 (AC-8)
- [ ] 본인이 선점을 해제하면 다른 관객이 바로 선점할 수 있다. 남의 hold 해제는 404, 이미 예약된 hold 해제는 204 이고 잠금이 그대로다 (AC-10)
- [ ] 비로그인 좌석 조회는 401 이다 (AC-11)
- [ ] 새로 선점하면 같은 관객의 이전 미예약 선점이 풀린다. 공연별 입금 대기·확정 좌석과 합쳐 4석을 넘는 선점은 거절된다 (AC-12)
- [ ] 기존 마이그레이션 버전 단언 3개가 새 버전에서 통과한다 — 승인된 테스트 변경은 사람 승인 뒤 (D28)

### Task A2 (백엔드 — 무통장 예약·ADMIN·매처)

- [ ] 유효한 선점으로 예약하면 예매번호·입금 계좌·금액·입금 기한(예약 당일 23:59 Asia/Seoul)이 응답에 온다 (AC-4)
- [ ] 만료된 선점으로 예약하면 `HOLD_EXPIRED` 409 이고 예약 행이 생기지 않는다(비트랜잭션 확인). 같은 hold 로 다시 예약하면 기존 예약이 200 으로 온다
- [ ] 가입 API 두 곳에서 `role: ADMIN` 은 400 으로 거절되고 사용자 행이 생기지 않는다
- [ ] `/api/admin/**` 는 ADMIN 이 아니면 403 `FORBIDDEN_ROLE`, 비로그인은 401 이다

### Task B (백엔드 — 입금 확인·상태·조회)

- [ ] ADMIN 이 기한 안에 예매번호로 입금 확인을 부르면 예매가 확정되고 `confirmed_by`·`confirmed_at` 이 남는다. 다시 부르면 200 이고 바뀌지 않는다 (AC-5)
- [ ] 입금 기한이 지나면 예약은 취소 상태로 조회되고 그 좌석을 다른 관객이 선점할 수 있다 (AC-6)
- [ ] 기한 뒤 입금 확인은 `DEPOSIT_DEADLINE_PASSED` 409 이고 DB 가 바뀌지 않는다(비트랜잭션 확인). 예약은 취소 상태로 보인다 (AC-9)
- [ ] DB 에서 ADMIN 역할을 회수한 사용자의 입금 확인은 거절된다
- [ ] 내 예매 목록은 본인 것만 최신순으로, 남의 예매 상세는 404 다
- [ ] 예매 확정·만료가 공연의 남은 좌석 수와 매진 표시에 반영된다 — 마지막 좌석이 입금 대기·확정이면 공연이 매진(SOLD_OUT)으로 보이고 공개 목록 매진 필터에 잡히며, 그 예약이 기한을 넘기면 다시 판매 중으로 보인다. 주최자 응답의 남은 좌석 수도 같다 (AC-5·AC-6, D40)

### Task C (프론트 — 좌석·예약·예약 완료)

- [ ] 좌석 1~4개를 골라 선점하면 선점 완료 화면에 남은 시간·좌석·금액이 보인다 (AC-1)
- [ ] 선점이 409 면 빠진 좌석 안내와 다시 고르기가 보인다 (AC-2)
- [ ] 5번째 좌석을 누르면 고르지 않고 4석 한도 안내가 보인다 (AC-3)
- [ ] 예약하면 예약 완료 화면에 금액·입금 기한·입금 계좌가 데이터 묶음 순서로 보이고 계좌를 복사할 수 있다 (AC-4)
- [ ] 남은 시간이 0 이 되면 좌석 화면으로 전환되고 만료 안내가 보인다 (AC-8)
- [ ] 「좌석 다시 고르기」·뒤로는 해제 요청을 보내고 좌석 화면으로 간다. 예약 성공 뒤 이동은 해제를 보내지 않는다 (AC-10)
- [ ] 좌석 조회 401 이면 로그인으로 안내하고 로그인 뒤 돌아온다 (AC-11)
- [ ] 화면이 정본 시안(REQ-002 모바일 화면)과 같다. 데스크톱에서도 모바일 폭 한 줄이다 (Phase 4 화면 확인 — 상태를 실제로 만들어)

### Task D (프론트 — 내 예매)

- [ ] 확정된 예매는 목록·상세에 「예매 확정」과 입금 확인 시각이 보인다 (AC-5)
- [ ] 기한이 지난 예매는 「입금 기한이 지나 취소됨」과 「다시 예매하기」가 보인다 (AC-6)
- [ ] 예매가 없으면 빈 안내가 보인다. 조회 실패면 오류 문구가 보인다
- [ ] 화면이 정본 시안(REQ-002 모바일 화면)과 같다. 데스크톱에서도 모바일 폭 한 줄이다 (Phase 4 화면 확인 — 상태를 실제로 만들어)

---

## 6. 구현 참고

- `backend/src/main/java/com/example/ticket_booking/domain/Seat.java` — 좌석(`grade`·`seatRow`·`seatNumber`·`seatLabel`·`price`)
- `backend/src/main/java/com/example/ticket_booking/domain/UserRole.java` — `ADMIN` 추가 (A2)
- `backend/src/main/java/com/example/ticket_booking/config/SecurityConfig.java` — 매처 순서 (A1 좌석 조회, A2 `/api/admin/**`)
- `backend/src/main/java/com/example/ticket_booking/domain/PerformanceStatusRules.java` — OPEN/SOLD_OUT 판정이 남은 좌석 계산을 쓴다 (B)
- `backend/src/main/java/com/example/ticket_booking/repository/PerformanceRepositoryImpl.java` — 공개 목록 매진 필터가 지금 `availableSeats` 를 읽는다 (B)
- `backend/src/main/java/com/example/ticket_booking/service/OrganizerPerformanceResponse.java` · `PerformanceResponse.java` — `availableSeats` 응답 (B)
- `backend/src/main/java/com/example/ticket_booking/config/ClockConfig.java` — 주입 `Clock` (ADR-0005)
- `backend/src/main/java/com/example/ticket_booking/api/dto/ErrorResponse.java` — 선택 필드 추가 (A1)
- `backend/src/main/java/com/example/ticket_booking/api/dto/SignUpRequest.java` · `CompleteSocialSignUpRequest.java` — ADMIN 거절 (A2)
- `backend/src/main/java/com/example/ticket_booking/api/PerformanceExceptionHandler.java` — 예외 처리기 형태 참고, `BookingExceptionHandler` 는 신규
- `backend/src/main/resources/db/migration/` — 마지막 `V9__add_email_token.sql`. 새 마이그레이션 `V10__create_bookings.sql` (BRIEF scope.paths, 신규)
- `backend/src/test/java/com/example/ticket_booking/repository/PerformanceOwnerMigrationTest.java` · `PerformancePublishedMigrationTest.java` · `Task027EmailTokenMigrationTest.java` — 버전 '9' 단언 (A1, D28)
- `frontend/src/api/auth.ts` — `UserRole`·`ROLES` 에 `'ADMIN'` (A2)
- `frontend/src/pages/PerformanceDetailPage.tsx` — 「예매하기」가 지금 `/performances/:id/queue` 로 간다 → 좌석 화면으로 (C)
- `frontend/src/pages/QueuePlaceholderPage.tsx` · `frontend/src/App.tsx` — 라우트 (C·D)
- `frontend/src/hooks/useServerClock.ts` — 서버 시각 오프셋 (남은 시간, ADR-0024·0026)
- `frontend/src/components/ui/` — 공통 부품 자리(`index.ts` 로 내보냄). 새 공통 부품 3종은 여기에 만든다 — 화면 안 개별 구현 금지 (ADR-0038, D42)
  - `picker` — 좌석 칸 고르기(좌석 배치도·좌석 칸 상태: 고를 수 있음·고를 수 없음·내가 고름). 시안 키트 `components.css` 의 `.picker*`·`.seat` (C)
  - `picker-mini` — 미니맵(구역 모양·무대·보는 범위·고른 좌석) (C)
  - `due` — 기한 줄(대기 = 남은 시간 경고 + 기한, 확정 = `is-done` 입금 확인 시각, 취소 = `is-off` 기한 지남). 시안 키트 `.due*` (C 가 만들고 D 가 재사용)
- 데이터 묶음 (`workflow_design/01_brief/mockup/REQ-002/layout.md` 「데이터 묶음」, §2-4) — 화면마다 같은 순서·부품으로
  - `payment`: amount → deadline(`due`) → account (확정·취소는 account 없음) — `reserved`·`booking` 계열
  - `booking`: show → when → seats → number — 예약 완료·예매 상세·내 예매 목록 행
- `frontend/src/theme/tokens.ts` — `componentSizes.appMaxWidth`(375px), 모바일 폭 한 줄 (D41)
- 신규 화면·API 모듈의 파일 이름은 Phase 1 PLAN 에서 정한다

---

## 7. 범위 외

후속 요청에서 다룸 (하기로 했지만 이번이 아님):

- 대기열 (FR-6~9, ADR-0023 Redis) — 좌석 API 에 입장권 검사가 없다(architecture.md §7)
- 매수 지정 연석 자동 선점 (FR-11·12)
- 관객 직접 취소(FR-19)·양도(FR-20) — 화면에는 「아직 할 수 없어요」 한 줄만
- 모바일 티켓 — REQ-001 (이 요청이 끝난 뒤 재개)

하지 않기로 함:

- 카드·간편결제 (FR-18, ADR-0025)
- ADMIN 의 화면·가입 경로와 그 밖의 운영자 기능 (product.md §4)
- 확정·취소 알림(메일·문자) — 확정은 내 예매에서 확인한다(D9)
- 만료 정리 스케줄러 (ADR-0037)
- 주최자 화면의 새 요소 — 화면 구성은 그대로이고 남은 좌석 수 값만 예매에 따라 움직인다(D39·D40)
- 공연별·주최자별 입금 계좌 — 서비스 고정 계좌 하나(정산 문제)

---

## 8. 주요 결정 사항

| 결정 | 이유 | 범위 | ADR |
|---|---|---|---|
| 입금 확인 호출자는 최소 ADMIN 역할, 계정은 DB 로만 | 보호 방식을 JWT 역할 하나로, 확정자를 남긴다 (D2·D25·D38) | **전체** | ADR-0036 |
| 선점·예약 만료를 조회 시점에 판정, 스케줄러 없음 | 만료 지연 없이 Clock 으로 경계 검증 (D5·D15·D35·D36) | **전체** | ADR-0037 |
| 선점은 유니크 제약 + 조건부 UPSERT 한 문장, 부분 선점 없음 | ADR-0004 확장 (D4·D26·D27) | **전체** | ADR-0004 |
| 선점·동시성 테스트는 PostgreSQL(Testcontainers) 공용 베이스 | ADR-0006 의 전환 조건 충족 (D29) | **전체** | ADR-0006 |
| 선점·판매를 관객에게 구분하지 않는다 (`available` 하나) | 관객에게 구분할 이유가 없다 (D30) | 이 기능만 | — |
| 공연의 남은 좌석 수를 저장값이 아니라 조회 시점에 계산 (`available_seats` 컬럼은 더 쓰지 않음, 컬럼 처리는 PLAN) | 만료가 쓰기 사건이 아니라 저장값으로는 맞출 수 없다 — 선점·만료와 같은 기준 (D40) | **전체** | ADR-0037 |
| 예매번호를 보이고 DB 시퀀스로 만든다 | 운영자 입금 대조 (D8·D37) | 이 기능만 | — |
| 떠나면 선점 해제 | 선점이 남아 다른 관객을 막지 않게 (D7·D12) | 이 기능만 | — |
| 데스크톱도 모바일 폭 한 줄, 데스크톱 시안은 보관 | 기존 앱이 모바일 폭 한 줄이다 — 앱 전체 데스크톱 배치는 별도 요청 (D41) | **전체** | ADR-0038 |
| 시안 정본을 화면 단위로 — 이 요청 화면은 REQ-002 모바일 시안, 새 부품은 `components/ui` 공통 부품 | 같은 화면에 정본이 둘이면 Phase 4 판정이 성립하지 않는다 (D42) | **전체** | ADR-0038 |
