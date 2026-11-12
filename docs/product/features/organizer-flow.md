# [주최자] 주최자 흐름 — 진입·내 공연 관리·공개 전환

**상태**: 초안
**최종 갱신**: 2026-11-11

---

## 1. 요구사항

### 1-1. 배경

주최자로 가입해도 공연을 등록할 길이 화면에 없다. [공연 등록](performance-registration.md)
(TASK-005)이 등록·수정 화면을 만들었지만 그 화면으로 가는 링크가 어디에도 없고, 가입 완료
화면은 역할과 무관하게 관객용 공연 목록(`/`)으로 보낸다. 등록 화면이 인증 기능
(TASK-010·013)보다 먼저 만들어져 역할로 갈라 보여줄 근거가 없었고, 이후 시안
(`out/prd.md` 부록 A)은 관객 화면만 다뤘다.

서버도 같다. JWT 에 `role` 클레임이 있지만 공연 쓰기 API 는 `permitAll` 이고 쓰기 키
(ADR-0017) 하나로만 막혀 있으며, `performance` 에 소유자가 없어 "내 공연"을 가를 수 없다.
등록하는 즉시 관객에게 노출되므로 준비 중인 공연을 숨겨 둘 방법도 없다.

| AS-IS | TO-BE |
|---|---|
| 주최자가 가입 후 관객 목록에 떨어지고, 등록 화면은 URL 을 알아야 들어간다 | 주최자는 가입·로그인 후 「내 공연」으로 가고, 거기서 등록·수정·취소·공개를 한다 |
| 누구나(쓰기 키만 있으면) 아무 공연이나 수정·취소한다 | 주최자 역할만 등록하고, 자기 공연만 수정·취소·공개 전환한다 |
| 등록 즉시 관객에게 보인다 | 등록은 비공개(초안)로 시작하고, 주최자가 공개해야 관객에게 보인다 |
| 주최자가 자기 공연의 좌석 구성·잔여를 볼 화면이 없다 | 내 공연 상세에서 등급별 좌석 수·가격과 전체 잔여를 본다 |

### 1-2. 태스크 분리 (필수)

| Task | 범위 | 산출물 | 포함하지 않음 | 선행 | 추정 | 태스크 ID |
|---|---|---|---|---|---|---|
| A. 백엔드 — 공연 소유자·역할 인가·쓰기 가드 제거 | `owner_id` 추가, JWT `role` → `ROLE_*` 권한, 공연 쓰기 API 주최자 전용(401/403), 등록 시 소유자 지정, 수정·취소 소유권 검사(404), 쓰기 가드·CORS 키 헤더 제거, 기존 쓰기 테스트에 주최자 토큰 픽스처, 관련 문서 갱신 | `V7__add_performance_owner.sql`, `AccessClaims`, `SecurityConfig`·`JwtAuthenticationFilter`·`PerformanceService` 변경, `WriteGuard*` 삭제 | 비공개 상태, 주최자 조회 API, 공개 전환, 입금 확인 API 보호 방식 | — | 14h | TASK-022 |
| B. 백엔드 — 비공개 상태·주최자 조회·공개 전환 API | `published` 추가, 관객 조회 3경로 공개만, 주최자 목록(필터·정렬)·단건(좌석 요약 포함), `publish`·`unpublish`, 비공개 수정 시각 제한 해제, 취소된 공연 409, `editable`·`visibilityChangeable` 서버 계산 | `V8__add_performance_published.sql`, `OrganizerPerformanceResponse`, `OrganizerPerformanceController`, `GET /api/organizer/performances[/{id}]`, `POST /api/performances/{id}/publish·unpublish` | 등급별 잔여, 매출·예매자, 공개 예약, 좌석 구성 변경 | A | 12h | TASK-023 |
| C. 프론트 — 토큰 전송·주최자 가드 | `request()` Bearer 주입·쓰기 키 제거, 401/403 오류 구분, `RequireOrganizer` 로 등록·수정 화면 보호(비로그인 → 로그인 후 복귀, 관객 → 안내), 쓰기 키 거부 안내 분기·환경변수 제거 | `http.ts`·`performances.ts` 변경, `RequireOrganizer.tsx`, `App.tsx` 라우트 | 내 공연 화면, 진입 링크, 등록 후 이동 경로 변경 | A | 6h | TASK-024 |
| D. 프론트 — 내 공연 목록·진입 경로 | `/organizer` 목록(필터 칩·배지·빈/필터 0건/로딩/오류), 가입 완료·로그인 후 역할별 이동, `AppHeader` 「내 공연」 | `OrganizerPerformanceListPage.tsx`, `api/organizer.ts`, `JoinedPage`·`LoginPage`·`AppHeader` 변경 | 내 공연 상세, 공개 전환, 등록·수정 복귀 경로 | B, C | 8h | TASK-025 |
| E. 프론트 — 내 공연 상세·공개 전환·등록/수정 시안 적용 | `/organizer/performances/:id`(진행 타임라인·공개 전환·좌석 요약·정보·수정/취소/관객 목록 보기), 공개 전환과 409 안내, 404 안내, 등록·수정 폼을 시안 구조·문구로, 등록 성공 → 내 공연 상세, 수정 화면 값 로드를 주최자 조회로 | `OrganizerPerformanceDetailPage.tsx`, `PerformanceRegisterPage`·`PerformanceEditPage` 변경 | 좌석 맵, 등급별 잔여, 좌석 구성 수정, 포스터·구역 위치 입력 | D | 12h | TASK-026 |

[태스크 생성 2026-11-11]

**분리 이유**: 보안 경계(A: 누가 쓰기를 할 수 있나)와 노출 규칙(B: 무엇이 관객에게 보이나)은
실패했을 때의 피해가 달라 따로 검증한다 — A 는 거부 경로의 "DB 변경 없음"이, B 는 관객 조회
3경로의 누락 없는 필터가 핵심이다. 프론트는 기존 화면의 토큰·가드 배선(C)을 새 화면(D·E)과
떼어 C 만으로도 A 의 서버 변경 뒤 등록·수정이 계속 동작하게 한다. D·E 는 주최자 시안
(`out/screens/organizer*.html`, 2026-11-11 추가)을 따른다 (§8).

```
흐름
  A(소유자·인가) ──► B(비공개·주최자 API) ──┐
       │                                    ├──► D(내 공연 목록·진입) ──► E(상세·공개 전환·폼 시안)
       └──────────► C(토큰·가드) ───────────┘
                    [주최자 시안 out/screens/organizer*.html — 완료 2026-11-11]
```

### 1-3. 상세

데이터·API 규칙은 §3 이 정본이다. 요점만:

- **역할** — 공연 쓰기와 `/api/organizer/**` 는 주최자만 (§3-3). 판단은 토큰의 `role` 클레임
- **소유권** — 자기 공연만 쓰고 본다. 위반은 404 로 숨긴다 (§3-4)
- **공개** — 새 공연은 비공개. 공개·비공개 전환은 오픈 전에만, 양방향 (§3-4)
- **수정** — 비공개면 시각 제한 없음, 공개면 기존대로 `now < openAt`. 좌석 구성은 여전히 불변
- **기존 데이터** — `owner_id` NULL·`published` true. 관객에겐 그대로 보이고 아무도 쓰지 못한다

---

## 2. 화면 설계

**UI 라이브러리**: MUI (Material UI) — ADR-0002
**디자인 정본**: `docs/product/design.md` · `design-tokens.css`
**시안** [2026-11-11]: `out/screens/organizer.html`·`organizer--empty`·`organizer-new`(+`--error`)·`organizer-show`(+`--done`)·`organizer-edit`(+`--error`)·`organizer-live`·`organizer-cancel-sheet`·`organizer-cancelled`·`organizer-only` — 흐름은 `out/index.html` 의 「주최자 — 내 공연 관리」 묶음. 화면은 시안과 같아야 한다 (ADR-0022). 아래 2-4 ASCII 는 시안 이전의 초안이고, 다르면 시안이 이긴다

### 2-1. 이 화면에서 사용자가 하려는 일

주최자가 가입한 바로 그 자리에서 자기 공연을 등록하고, 등록한 공연이 지금 관객에게 보이는지·
예매가 어떤 상태인지·좌석이 얼마나 남았는지를 연락 없이 확인하고 관리한다.

### 2-2. 참조

공개 도움말 기준. 로그인 뒤 실제 화면은 열어 보지 못했다.

| 서비스 | 이 화면에 해당하는 부분 | 우리에게 없는 것 |
|---|---|---|
| Eventbrite | 로그인 후 「Manage my events」→ 내 이벤트 목록. 상태(Draft·Live·Started·Ended·Canceled)로 거르고, 행마다 날짜·판매 수·매출. 이벤트별 대시보드에 판매 리포트, 지정석 맵(판매·홀드·잔여) | 소유자, 초안 상태, 매출, 판매 리포트, 좌석 맵 |
| 이벤터스 호스트센터 | 참가자 명단 조회, 이메일·문자 발송, 출석 체크, 신청 퍼널 통계 | 예매자 명단, 메시지 발송, 출석 체크, 통계 |

두 서비스 모두 주최자의 첫 화면이 **「내가 연 행사 목록 + 행사마다 상태·판매 현황」**이다.

**대조 결과** [확정 2026-11-11]: 소유자·내 공연 목록·상태별 필터·초안(비공개) 상태·공연별
좌석 현황(등급별 좌석 수·가격 + 전체 잔여)을 넣는다. 매출·예매자 명단·판매 리포트는 예매
테이블이 없어 만들 수 없고, 좌석 맵·메시지 발송·출석 체크·통계는 §7 범위 외로 둔다.

### 2-3. 담기는 것

**내 공연 목록**

| 우선 | 요소 | 왜 필요한가 | 데이터 출처 |
|---|---|---|---|
| 1 | 공개 여부 (「비공개」 배지 `is-draft` — 7번째 의미색, design.md §5) | 관객에게 보이는지가 주최자의 첫 질문 | ⚠ 없음 — `performance.published` 신규 |
| 2 | 예매 상태 배지 | 예정·예매 가능·매진·마감 배지, 취소는 평문 「공연 취소」 — 관객 시안과 같은 이름 (design.md §5) | 계산값 `PerformanceStatusRules.of()` |
| 3 | 공연명 | 식별 | `performance.title` |
| 4 | 공연 일시 · 장소 | 식별 | `performance.start_at` · `venue` |
| 5 | 예매 오픈 일시 | 공개·수정 가능 기한이 여기서 정해진다 | `performance.open_at` |
| 6 | 잔여 / 총 좌석 | 판매 현황의 대체값 (예매 테이블이 없다) | `available_seats` · `total_seats` |
| 7 | 포스터 썸네일 | 식별 보조 | `performance.poster_url` (선택) |
| — | 상태 필터 (전체·비공개·예정·예매 가능·매진·마감·취소) | 공연이 쌓이면 찾기 | 계산값 + `published` |
| — | 「공연 등록」 떠 있는 버튼 (목록은 첫 화면이라 하단 고정 버튼을 두지 않는다) | 새 공연을 만드는 주 행동 | — |
| — | **소유자로 걸러냄** | 「내」 공연 | ⚠ 없음 — `performance.owner_id` 신규 |

**내 공연 상세 (관리)**

| 우선 | 요소 | 왜 필요한가 | 데이터 출처 |
|---|---|---|---|
| 1 | 공개 여부 + 공개/비공개 전환 버튼 | 2-1 의 첫 질문과 그 행동 | ⚠ `performance.published` 신규 |
| 2 | 예매 상태 배지 · 오픈/마감 일시 | | 계산값 · `open_at` · `close_at` |
| 3 | 잔여 / 총 좌석 | | `available_seats` · `total_seats` |
| 4 | 등급별 좌석 수 · 가격 · 구역 | 좌석 구성 확인 (등록 후 변경 불가) | `seat` 집계 `SeatSectionCount` + `performance_zone` |
| 5 | 기본 정보 (공연명·장소·공연 일시·포스터) | | `performance.*` |
| 6 | 수정 · 공연 취소 버튼 | 관리 행동 | 가능 여부는 계산값 (§3) |
| — | 관객 화면에서 보기 링크 | 공개 후 관객에게 어떻게 보이는지 | 공개일 때만 |

등급별 **잔여**는 넣지 않는다 — `seat` 에 좌석별 상태 컬럼이 없다(선점 기능이 아직 없다).
등급별로는 좌석 수만, 잔여는 공연 전체 값만 보인다.

**진입 경로** (화면이 아니라 링크)

| 위치 | 요소 | 조건 |
|---|---|---|
| 가입 완료 `JoinedPage` 「시작하기」 | 주최자면 `/organizer` 로 | `user.role === 'ORGANIZER'` |
| 로그인 직후 | `from` 이 없으면 주최자는 `/organizer` 로 | 같음 |
| `AppHeader` | 「내 공연」 링크 | 주최자로 로그인했을 때만 |

### 2-4. 배치

모바일 한 열 (기존 시안이 폰 기준이다).

```
/organizer — 내 공연                        /organizer/performances/:id — 내 공연 상세
┌──────────────────────────────┐           ┌──────────────────────────────┐
│ ← 내 공연            [로그아웃]│           │ ← 내 공연                     │
│ [전체][비공개][예매 전][예매 중]│→가로스크롤 │ [비공개] [예매 전]             │
│ [매진][종료][취소]             │           │ 공연명 (크게)                  │
├──────────────────────────────┤           │ 2026.12.24 19:00 · 장소        │
│ [썸] 공연명                   │           │ ┌──────────────────────────┐ │
│ [네] [비공개][예매 전]          │           │ │ 관객에게 보이지 않아요       │ │
│ [일] 12.24 19:00 · 장소        │           │ │ 오픈 12.01 20:00 전까지 공개 │ │
│      오픈 12.01 20:00          │           │ │ [ 공개하기 ]  (주 버튼)     │ │
│      잔여 480 / 500            │           │ └──────────────────────────┘ │
├──────────────────────────────┤           │ 좌석   잔여 480 / 500 (숫자 크게)│
│ [썸] 공연명                   │           │ ─ VIP  1층 앞  100석 150,000원│
│ ...                           │           │ ─ R    1층 뒤  200석 120,000원│
├──────────────────────────────┤           │ ─ S    2층     200석  90,000원│
│                               │           │ 기본 정보                     │
│ [      + 공연 등록       ]    │ 하단 고정  │ 장소 / 공연 일시 / 마감 / 포스터│
└──────────────────────────────┘           │ [ 수정 ]   [ 공연 취소 ]       │
                                           │ 관객 화면에서 보기 →           │
                                           └──────────────────────────────┘
```

**이 배치인 이유**: 주최자에게 공연은 고르는 상품이 아니라 처리할 항목이라 그리드가 아닌
세로 행 목록이다. 상세는 2-1 순서대로 공개 여부 → 좌석 숫자 → 정보 → 행동.

등록(`/performances/new`)·수정(`/performances/:id/edit`) 화면은 TASK-005 의 폼을 유지하고,
이번에는 **가드와 진입·복귀 경로만** 바꾼다. 등록 성공 후에는 관객 상세가 아니라 내 공연
상세로 간다 (새 공연은 비공개라 관객 상세에서 404 다).

### 2-5. 상태별로 무엇이 보이나

| 화면 | 상태 |
|---|---|
| 내 공연 목록 | 기본 · 로딩 · 빈 목록(공연 0건) · 필터 결과 0건 · 오류 · 비로그인 · 관객 역할 |
| 내 공연 상세 | 기본(비공개/공개) · 로딩 · 공개 전환 중 · 전환 거부(오픈 지남) · 취소된 공연 · 남의 공연/없는 공연 · 오류 |
| 등록 · 수정 | 기존 상태 + 비로그인 · 관객 역할 · 남의 공연(수정) |

- **기본** — 목록은 공연 일시가 늦은 순(다가올 공연이 위). 비공개 공연은 「비공개」 배지가 상태 배지 앞에 붙는다
- **로딩** — 목록 자리에 행 모양 자리표시 3개. 상세는 머리 자리표시
- **빈 목록** — 「아직 등록한 공연이 없어요」 + 「첫 공연 등록하기」 주 버튼
- **필터 결과 0건** — 「<필터명> 공연이 없어요」 + 「전체 보기」. 등록 버튼으로 바꾸지 않는다
- **오류** — 「공연 목록을 불러오지 못했어요」 + 「다시 시도」
- **비로그인** — `/organizer/**`·등록·수정 진입 시 `/login` 으로 보내고 `from` 을 넘긴다. 로그인 뒤 원래 화면으로 돌아온다
- **관객 역할** — 「주최자만 이용할 수 있어요」 + 「공연 둘러보기」(→ `/`). 폼이나 목록을 그리지 않는다
- **공개 전환 중** — 버튼이 진행 표시로 바뀌고 다시 누를 수 없다
- **전환 거부(409, 오픈 지남)** — 카드 안에 「예매가 열린 뒤에는 공개 여부를 바꿀 수 없어요」. 버튼은 사라진다
- **공개 상태 카드** — 공개면 「관객에게 보이고 있어요」 + 「비공개로 돌리기」(오픈 전만, 보조 버튼)
- **취소된 공연** — 「취소」 배지만, 공개 전환·수정·취소 버튼 없음. 정보는 읽기 전용
- **수정 버튼 숨김 규칙** — 공개 공연은 오픈 이후 수정 버튼이 없다. 비공개 공연은 시각과 무관하게 있다
- **남의 공연 / 없는 공연** — 둘 다 「공연을 찾을 수 없어요」 + 「내 공연으로」. 남의 공연이 있다는 사실을 드러내지 않는다
- **아주 긴 공연명** — 목록 2줄 말줄임, 상세는 전부 표시
- **포스터 없음** — 썸네일 자리에 기본 무늬 (관객 목록과 같은 규칙)

**이 상태 목록이 그대로 §5 완료 조건이 된다.**

---

## 3. 데이터·API 명세

### 3-1. 마이그레이션 (신규 2건 — 태스크 A·B 가 하나씩 갖는다)

```sql
-- V7__add_performance_owner.sql (Task A)
-- 기존 공연은 소유자 없음(NULL) — 어느 주최자의 목록에도 없고 쓰기는 404 (§3-4)
ALTER TABLE performance ADD COLUMN owner_id BIGINT REFERENCES users(id);
CREATE INDEX idx_performance_owner_id ON performance (owner_id);

-- V8__add_performance_published.sql (Task B)
-- 기존 공연은 지금처럼 관객에게 보이게 TRUE 로 채우고, 이후 기본값을 FALSE 로 바꾼다
ALTER TABLE performance ADD COLUMN published BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE performance ALTER COLUMN published SET DEFAULT FALSE;
```

`owner_id` 는 DB 에서 NULL 을 허용하지만, 새 등록은 서비스가 항상 채운다 (NOT NULL 은 기존
행 때문에 걸 수 없다). `Performance` 엔티티에 `ownerId`(Long)·`published`(boolean) 를 추가한다.

### 3-2. 인증 주체에 역할 싣기

| 지금 | 바뀜 |
|---|---|
| `JwtTokenProvider.verifyAccessToken()` → `Optional<Long>` (userId) | userId + `role` 클레임을 함께 돌려준다 (`AccessClaims` record 신규) |
| `JwtAuthenticationFilter` → 권한 `List.of()` | 권한 `ROLE_<role>` 하나 (`ROLE_ORGANIZER` / `ROLE_AUDIENCE`). principal 은 그대로 userId |

역할은 가입 시 정해지고 바뀌지 않는다(user-authentication §역할 불변성) — 그래서 요청마다
DB 를 다시 읽지 않고 토큰 클레임을 믿는다.

### 3-3. 접근 정책 — `SecurityConfig` (ADR-0013 단일 지점)

| 경로 | 지금 | 바뀜 |
|---|---|---|
| `GET /api/performances/**` | permitAll | permitAll (유지) |
| `POST·PUT /api/performances/**` | permitAll + 쓰기 가드 | `hasRole('ORGANIZER')` |
| `/api/organizer/**` | (없음) | `hasRole('ORGANIZER')` |

- 토큰 없음/만료 → **401** (기존 `unauthorizedEntryPoint`)
- 관객 토큰 → **403** `{"code":"FORBIDDEN_ROLE","message":"주최자만 이용할 수 있습니다"}` (accessDeniedHandler 신규)
- **쓰기 가드 완전 제거** [확정 2026-11-11] — `WriteGuardFilter`·`WriteGuardConfig`·
  `WRITE_GUARD_ENABLED`/`WRITE_API_KEY`, `CorsFilter` 의 `X-Write-Key` 허용 헤더를 지운다.
  지금 가드는 경로 구분 없이 모든 쓰기(가입·로그인 포함)에 걸려 있다. ADR-0017 이 정한 제거
  조건(쓰기 API 에 인증·소유권 검사)이 이 기능으로 충족된다 → ADR-0029. ADR-0025 의 입금 확인
  내부 API 는 운영자 역할 + JWT 방향으로 바뀌고, 방식은 그 기능에서 정한다. 제약 AUTH-001

### 3-4. 소유권 규칙 — `PerformanceService`

| 동작 | 규칙 | 위반 |
|---|---|---|
| 등록 | `ownerId` = 요청자, `published = false` | — |
| 수정·취소·공개 전환·주최자 조회 | `performance.ownerId == 요청자` | **404** `PerformanceNotFoundException` — 남의 공연·소유자 없는 공연·없는 공연을 구분하지 않는다 |
| 수정 | 비공개: 시각 제한 없음 / 공개: `now < openAt` | 공개 + 오픈 이후 → **409** `RegistrationAlreadyOpenException` (기존) |
| 수정·공개 전환 | 취소되지 않은 공연 | **409** `PerformanceCancelledException` (신규) |
| 공개 전환 (양방향) | `now < openAt` | **409** `VisibilityLockedException` (신규, `code: VISIBILITY_LOCKED`) |
| 공개 전환 | 이미 그 상태면 변경 없이 200 (멱등) | — |
| 취소 | 기존 규칙 그대로 + 소유권 | — |

시각 판단은 서버 `Clock` 만 쓴다(ADR-0026). 화면은 아래 `editable`·`visibilityChangeable`
플래그를 그대로 따른다 — 프론트에서 `openAt` 과 현재 시각을 비교하지 않는다.

### 3-5. 관객 조회 — 공개된 것만

`PerformanceRepositoryImpl.findVisiblePerformances()`·`findNextOpening()` 과
`PerformanceService.getPerformance()` 에 `published = true` 조건을 더한다. 비공개 공연의
`GET /api/performances/{id}` 는 **404**. 공개 후 취소된 공연은 지금처럼 「취소」로 보인다.

### 3-6. API

**기존 — 동작만 바뀜**

| 메서드 · 경로 | 바뀜 |
|---|---|
| `POST /api/performances` | 주최자만. 응답 201 은 `OrganizerPerformanceResponse` (`published: false`) |
| `PUT /api/performances/{id}` | 주최자 + 소유권 + §3-4 수정 규칙. 응답 `OrganizerPerformanceResponse` |
| `POST /api/performances/{id}/cancel` | 주최자 + 소유권. 응답 `OrganizerPerformanceResponse` |
| `GET /api/performances`, `/next-opening`, `/{id}` | 공개된 것만 (§3-5). 응답 형태 그대로 |

**신규**

| 메서드 · 경로 | 요청 | 응답 |
|---|---|---|
| `GET /api/organizer/performances` | `?filter=ALL\|DRAFT\|UPCOMING\|OPEN\|SOLD_OUT\|CLOSED\|CANCELLED&page=0&size=20` | 200 `{ items: OrganizerPerformanceResponse[], page, size, totalElements }` |
| `GET /api/organizer/performances/{id}` | — | 200 `OrganizerPerformanceResponse` (+ `sections`) / 404 |
| `POST /api/performances/{id}/publish` | — | 200 `OrganizerPerformanceResponse` / 404 / 409 |
| `POST /api/performances/{id}/unpublish` | — | 같음 |

- 목록은 요청자 소유만, `start_at DESC, id DESC`. `filter` 기본값 `ALL`
- `DRAFT` = `published = false AND cancelled = false`. 나머지 상태 필터는 공개 여부와 무관하게
  `PerformanceStatusRules` 와 같은 조건 (공개 여부는 배지로 따로 보인다)
- 공개 전환 경로는 기존 `/{id}/cancel` 과 같은 동사형 POST — `/api/performances/**` 쓰기 규칙을 그대로 탄다

```java
// service/OrganizerPerformanceResponse.java (신규) — PerformanceResponse 와 별개.
// 관객 응답에 주최자 전용 필드가 새지 않게 타입을 나눈다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrganizerPerformanceResponse(
    Long id,
    String title,
    String venue,
    Instant startAt,
    Instant openAt,
    Instant closeAt,
    Integer totalSeats,
    Integer availableSeats,
    PerformanceStatus status,
    String posterUrl,
    boolean published,
    boolean editable,              // §3-4 수정 규칙을 지금 통과하는가
    boolean visibilityChangeable,  // §3-4 공개 전환 규칙을 지금 통과하는가
    List<SectionSummaryResponse> sections) {}  // 단건 조회에서만
```

### 3-7. 프론트 API 클라이언트

| 파일 | 바뀜 |
|---|---|
| `frontend/src/api/http.ts` `request()` | `X-Write-Key` 주입 제거. 저장된 토큰(`ACCESS_TOKEN_KEY`)이 있으면 `Authorization: Bearer` 를 붙인다 |
| `frontend/src/api/performances.ts` | `WRITE_KEY_INVALID`·`WRITE_REJECTED_MESSAGE`·`isWriteKeyRejected` 제거. 401/403 을 `UnauthorizedError`/`ForbiddenRoleError` 로 구분 |
| `frontend/src/api/organizer.ts` (신규) | `getMyPerformances(filter)`, `getMyPerformance(id)`, `publishPerformance(id)`, `unpublishPerformance(id)` |
| `LoginPage`·`SignUpPage`·`RoleSelectionPage`·`PerformanceRegisterPage`·`PerformanceEditPage` | 쓰기 키 거부 안내(`WRITE_REJECTED_MESSAGE`) 분기 제거 |
| `frontend/.env.example`·`frontend/src/vite-env.d.ts`·배포 설정 | `VITE_WRITE_KEY` 제거 |

---

## 4. 흐름

**진입**

```
가입 완료(JoinedPage)·로그인 성공
   │
   ├─ from 있음 ───────────────► from (로그인 전 보던 화면)
   ├─ role = ORGANIZER ────────► /organizer
   └─ role = AUDIENCE ─────────► /
```

**화면 이동 (주최자)**

```
/organizer (내 공연 목록) ──「공연 등록」──► /performances/new ──201──► /organizer/performances/:id
     │                                                                   │
     └──── 행 선택 ─────────────────────────────────────────────────────►│
                                                                         ├─「수정」──► /performances/:id/edit ──200──┐
                                                                         │◄──────────────────────────────────────────┘
                                                                         ├─「공개하기」/「비공개로」 (같은 화면에서 갱신)
                                                                         ├─「공연 취소」 (확인 후 같은 화면에서 갱신)
                                                                         └─「관객 화면에서 보기」──► /performances/:id (공개일 때만)
```

**가드 (프론트, `RequireOrganizer` 신규)**

```
/organizer/** · /performances/new · /performances/:id/edit
   │
   ├─ restoring ──────► 아무것도 그리지 않음 (토큰 복원 대기)
   ├─ user 없음 ──────► /login (state.from = 현재 경로)
   ├─ role ≠ ORGANIZER ► 「주최자만 이용할 수 있어요」
   └─ 통과 ───────────► 화면. 이후 API 가 401 이면 로그아웃 후 /login, 403 이면 위 안내
```

프론트 가드는 화면을 가르는 편의일 뿐이다 — 최종 판단은 서버(§3-3·§3-4)가 한다.

**서버 (쓰기 요청)**

```
요청 ──► JwtAuthenticationFilter (userId + ROLE_*) ──► SecurityConfig
            토큰 없음 401 ◄─┤          관객 403 ◄─┤
                                                 ▼
          PerformanceController ──► PerformanceService
                                       ├─ findById → ownerId ≠ 요청자 → 404
                                       ├─ 취소됨 → 409 / 시각 규칙 → 409
                                       └─ 변경 → OrganizerPerformanceResponse
```

---

## 5. 완료 조건

### Task A (백엔드 — 소유자·역할 인가·쓰기 가드 제거)

- [ ] 마이그레이션 후 기존 `performance` 행의 `owner_id` 가 NULL 이고, 기존 관객 조회 API 응답이 마이그레이션 전과 같다
- [ ] 주최자 토큰으로 `POST /api/performances` 를 보내면 201 이고, 저장된 행의 `owner_id` 가 그 사용자 id 다 (비트랜잭션 조회로 확인)
- [ ] 토큰 없이 `POST`·`PUT /api/performances/**` 를 보내면 401, 관객 토큰이면 403 `FORBIDDEN_ROLE` 이고 DB 가 바뀌지 않는다
- [ ] 다른 주최자의 공연, 소유자가 NULL 인 공연, 없는 id 에 수정·취소를 보내면 모두 같은 형태의 404 이고 DB 가 바뀌지 않는다
- [ ] 자기 공연의 수정·취소는 지금 규칙(수정은 `now < openAt`) 그대로 동작하고 DB 에 반영된다
- [ ] `X-Write-Key` 없이 가입·로그인·소셜 가입 완료가 성공한다. `WRITE_GUARD_ENABLED=true` 를 줘도 쓰기 요청이 키 때문에 거부되지 않는다
- [ ] `WriteGuardFilter`·`WriteGuardConfig` 와 그 테스트가 저장소에 없고, CORS 허용 헤더에 `X-Write-Key` 가 없다
- [ ] `GET /api/performances/**` 는 토큰 없이 지금처럼 200 이다

### Task B (백엔드 — 비공개 상태·주최자 조회·공개 전환)

- [ ] 마이그레이션 후 기존 행은 `published = true` 이고, 새로 등록한 공연은 `published = false` 다
- [ ] 비공개 공연은 `GET /api/performances` 목록·`/next-opening` 에 나오지 않고 `GET /api/performances/{id}` 는 404 다
- [ ] `GET /api/organizer/performances` 는 요청자 소유 공연만 `start_at DESC` 로 돌려주고, 다른 주최자 공연·소유자 NULL 공연은 없다
- [ ] `filter=DRAFT` 는 취소되지 않은 비공개 공연만, `filter=UPCOMING|OPEN|SOLD_OUT|CLOSED|CANCELLED` 는 공개 여부와 무관하게 해당 상태만 돌려준다
- [ ] `GET /api/organizer/performances/{id}` 는 자기 공연이면 `sections`·`published`·`editable`·`visibilityChangeable` 을 담고, 남의 공연·없는 공연은 404 다
- [ ] `now < openAt` 인 자기 공연에 `publish` 를 보내면 `published = true` 로 DB 에 반영되고 관객 목록에 나타난다. `unpublish` 는 반대
- [ ] `now >= openAt` 이면 `publish`·`unpublish` 가 409 `VISIBILITY_LOCKED` 이고 DB 가 바뀌지 않는다
- [ ] 취소된 공연에 `publish`·`unpublish`·수정을 보내면 409 이고 DB 가 바뀌지 않는다
- [ ] 이미 공개된 공연에 `publish` 를 보내면 200 이고 값이 그대로다
- [ ] 비공개 공연은 `now >= openAt` 이어도 수정이 200 이고 반영된다. 공개 공연은 기존처럼 409
- [ ] `editable`·`visibilityChangeable` 값이 같은 시각에 실제 수정·전환 요청의 성공 여부와 일치한다 (고정 `Clock` 으로 경계 시각 전후 확인)
- [ ] 관객 응답(`PerformanceResponse`) JSON 에 `published`·`editable`·`visibilityChangeable`·`ownerId` 키가 없다

### Task C (프론트 — 토큰 전송·역할 가드)

- [ ] 로그인 상태에서 공연 쓰기 요청에 `Authorization: Bearer <토큰>` 이 실리고, `X-Write-Key` 는 어떤 요청에도 없다
- [ ] 비로그인으로 `/performances/new`·`/performances/:id/edit` 에 들어가면 `/login` 으로 가고, 로그인 후 원래 경로로 돌아온다
- [ ] 관객으로 같은 경로에 들어가면 폼 대신 「주최자만 이용할 수 있어요」와 「공연 둘러보기」가 보인다
- [ ] 토큰 복원 중에는 가드가 안내·이동 없이 아무것도 그리지 않는다 (복원 후 주최자면 폼이 보인다)
- [ ] 쓰기 API 가 401 이면 로그아웃되고 `/login` 으로 간다. 403 이면 「주최자만 이용할 수 있어요」가 보인다
- [ ] `VITE_WRITE_KEY`·`WRITE_REJECTED_MESSAGE`·`isWriteKeyRejected` 가 `frontend/src` 와 `.env.example` 에 없다

### Task D (프론트 — 내 공연 목록·진입 경로)

- [ ] 주최자가 가입 완료 화면에서 「시작하기」를 누르면 `/organizer` 로, 관객은 `/` 로 간다
- [ ] 주최자가 `from` 없이 로그인하면 `/organizer` 로, `from` 이 있으면 그 경로로 간다
- [ ] 주최자로 로그인하면 `AppHeader` 에 「내 공연」 링크가 있고, 관객·비로그인에게는 없다
- [ ] `/organizer` 는 내 공연을 행마다 공연명·공연 일시·장소·오픈 일시 또는 남은/총 좌석·상태 배지로 보여주고, 비공개면 「비공개」 배지(`is-draft`)가, 취소면 배지 없이 「공연 취소」가 보인다
- [ ] 필터 칩(전체·비공개·예정·예매 가능·매진·마감·취소)을 누르면 해당 `filter` 로 다시 불러온다
- [ ] 공연 0건이면 「아직 등록한 공연이 없어요」와 「첫 공연 등록하기」가, 필터 결과 0건이면 「<필터명> 공연이 없어요」와 「전체 보기」가 보인다
- [ ] 불러오는 중에는 행 자리표시가, 실패하면 「공연 목록을 불러오지 못했어요」와 「다시 시도」가 보인다
- [ ] 떠 있는 「공연 등록」 버튼이 `/performances/new` 로, 행 선택이 `/organizer/performances/:id` 로 간다
- [ ] 비로그인·관객의 `/organizer` 진입은 Task C 의 가드 규칙대로 처리된다
- [ ] 화면 구조·문구가 `out/screens/organizer.html`·`organizer--empty.html`·`organizer-only.html` 과 같다

### Task E (프론트 — 내 공연 상세·공개 전환·등록/수정 복귀)

- [ ] `/organizer/performances/:id` 가 공개 여부 카드·상태 배지·오픈/마감·잔여/총 좌석·등급별 좌석 수·가격·구역·기본 정보를 보여준다
- [ ] `visibilityChangeable` 이 true 인 비공개 공연에 「공개하기」, 공개 공연에 「비공개로 돌리기」가 보이고, 누르면 진행 중엔 다시 누를 수 없으며 성공 후 카드가 바뀐다
- [ ] 전환이 409 면 「예매가 열린 뒤에는 공개 여부를 바꿀 수 없어요」가 보이고 전환 버튼이 사라진다
- [ ] `editable` 이 false 면 「수정」 버튼이 없다. 취소된 공연은 전환·수정·취소 버튼이 모두 없다
- [ ] 「공연 취소」는 확인을 거친 뒤 요청하고, 성공하면 같은 화면이 「취소」 상태로 바뀐다
- [ ] 공개 공연에만 「관객 화면에서 보기」(→ `/performances/:id`)가 보인다
- [ ] 남의 공연·없는 공연이면 둘 다 「공연을 찾을 수 없어요」와 「내 공연으로」가 보인다
- [ ] 등록 성공 후 `/organizer/performances/<새 id>` 로 간다
- [ ] 수정 화면은 주최자 단건 조회(`getMyPerformance`)로 값을 채우고 — 비공개 공연도 열린다 — 저장 후 내 공연 상세로 돌아온다
- [ ] 화면 구조·문구가 `out/screens/organizer-show.html`·`organizer-show--done.html`·`organizer-live.html`·`organizer-cancel-sheet.html`·`organizer-cancelled.html` 과 같다
- [ ] 등록·수정 폼의 구조·문구(구역 블록, 「시작 열·끝 열·열당 좌석 수」, 만들어질 좌석 수, 오류 문구)가 `out/screens/organizer-new.html`(+`--error`)·`organizer-edit.html`(+`--error`) 과 같다

---

## 6. 구현 참고

**백엔드 (Task A)**

- `backend/src/main/resources/db/migration/V7__add_performance_owner.sql` (신규)
- `backend/src/main/java/com/example/ticket_booking/domain/Performance.java` — `ownerId` 필드
- `backend/src/main/java/com/example/ticket_booking/config/JwtTokenProvider.java:verifyAccessToken()` — `Optional<Long>` → `Optional<AccessClaims>` (userId + role)
- `backend/src/main/java/com/example/ticket_booking/config/JwtAuthenticationFilter.java:doFilterInternal()` — 권한 `ROLE_<role>` 부여
- `backend/src/main/java/com/example/ticket_booking/config/SecurityConfig.java:securityFilterChain()` — `/api/performances/**` permitAll 을 GET 으로 좁히고 쓰기에 `hasRole("ORGANIZER")`, accessDeniedHandler(403 `FORBIDDEN_ROLE`) 추가. 클래스 주석의 "소유권 검사는 범위 밖" 문구 갱신
- `backend/src/main/java/com/example/ticket_booking/api/PerformanceController.java:register()·update()·cancel()` — `Authentication` principal(userId)을 서비스로 넘김 (`AuthController.me()` 와 같은 방식)
- `backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java:registerPerformance()·updatePerformance()·cancelPerformance()` — 소유자 지정·검사
- 삭제: `config/WriteGuardFilter.java`, `config/WriteGuardConfig.java`, 테스트 `config/WriteGuardFilterTest.java`, `api/WriteGuardIntegrationTest.java`, `api/WriteGuardOffIntegrationTest.java`, `api/WriteGuardSocialCompleteTest.java`, `api/WriteGuardAuthApiTest.java`
- `backend/src/main/java/com/example/ticket_booking/config/CorsFilter.java:ALLOW_HEADERS` — `X-Write-Key` 제거 (`config/CorsFilterTest.java` 기대값 함께)
- 기존 쓰기 테스트 `api/PerformanceRegistrationApiTest.java`·`api/PerformanceZoneRegistrationApiTest.java`·`api/PerformanceApiTest.java`·`api/PerformanceErrorResponseRegressionTest.java` — 주최자 토큰 픽스처가 필요해진다

**백엔드 (Task B)**

- `backend/src/main/resources/db/migration/V8__add_performance_published.sql` (신규)
- `backend/src/main/java/com/example/ticket_booking/repository/PerformanceRepositoryImpl.java:findVisiblePerformances()·findNextOpening()` — `published = true` 조건
- `backend/src/main/java/com/example/ticket_booking/repository/PerformanceRepositoryCustom.java` — 소유자별·필터별 조회 메서드 추가
- `backend/src/main/java/com/example/ticket_booking/service/PerformanceService.java:getPerformance()` — 비공개 404. `publish()`·`unpublish()`·`getMyPerformances()`·`getMyPerformance()` 신규
- `backend/src/main/java/com/example/ticket_booking/domain/PerformanceStatusRules.java` — `editable`·`visibilityChangeable` 판정을 같은 클래스의 순수 함수로 둔다
- `backend/src/main/java/com/example/ticket_booking/service/OrganizerPerformanceResponse.java` (신규)
- `backend/src/main/java/com/example/ticket_booking/service/VisibilityLockedException.java`·`PerformanceCancelledException.java` (신규) — `api/PerformanceExceptionHandler.java` 에 409 매핑
- `backend/src/main/java/com/example/ticket_booking/api/OrganizerPerformanceController.java` (신규) — `GET /api/organizer/performances`, `/{id}`
- `PerformanceController` — `POST /{id}/publish`, `/{id}/unpublish`
- 좌석 집계는 기존 `repository/SeatRepository.java` 의 `SeatSectionCount` 투영과 `PerformanceZoneRepository` 를 재사용

**프론트 (Task C)**

- `frontend/src/api/http.ts:request()` — Bearer 주입, 쓰기 키 제거 (`api/http.test.ts` 기대값 함께)
- `frontend/src/api/performances.ts` — 쓰기 키 관련 export 제거, `UnauthorizedError`·`ForbiddenRoleError`
- `frontend/src/auth/RequireOrganizer.tsx` (신규) — `useAuth()` 의 `user`·`restoring` 사용
- `frontend/src/App.tsx` — `/performances/new`·`/performances/:id/edit` 를 가드로 감싼다
- 쓰기 키 분기 제거: `pages/LoginPage.tsx`, `pages/SignUpPage.tsx`, `pages/RoleSelectionPage.tsx`, `pages/PerformanceRegisterPage.tsx`, `pages/PerformanceEditPage.tsx`, 테스트 `pages/WriteKeyRejected.test.tsx`(삭제)·`auth/AuthScreens.test.tsx`
- `frontend/.env.example`, `frontend/src/vite-env.d.ts` — `VITE_WRITE_KEY` 제거

**프론트 (Task D·E)**

- `frontend/src/api/organizer.ts` (신규)
- `frontend/src/pages/OrganizerPerformanceListPage.tsx` (신규) · `frontend/src/pages/OrganizerPerformanceDetailPage.tsx` (신규)
- `frontend/src/pages/JoinedPage.tsx` — 「시작하기」 목적지를 역할로 가름 (지금 `navigate('/')`)
- `frontend/src/pages/LoginPage.tsx:safeDestination()` — `from` 없을 때 역할별 기본 경로
- `frontend/src/components/AppHeader.tsx` — 주최자에게 「내 공연」
- `frontend/src/theme/tokens.ts` — `--c-draft`·`--c-draft-tint`·`--c-draft-tint-text` 를 `docs/product/design-tokens.css` 에서 옮긴다 (Task D)
- `frontend/src/components/ui/Badge.tsx:BadgeTone` — `draft` 톤 추가 (Task D)
- `frontend/src/pages/PerformanceRegisterPage.tsx` — 성공 시 `navigate('/organizer/performances/${id}')` (지금 `/performances/${id}`)
- `frontend/src/pages/PerformanceEditPage.tsx` — 값 로드를 `getMyPerformance` 로
- 공통 부품 `frontend/src/components/ui/`(`Badge`·`Row`·`Block`·`Segmented`·`CtaBar`·`Sheet`)과 `components/StatusBadge.tsx` 재사용

**문서**

- `docs/architecture/architecture.md` §4 (입금 확인 `X-Write-Key` 문구), `docs/release/first-deploy-checklist.md` (쓰기 키 환경변수 단계) — Task A 에서 갱신

---

## 7. 범위 외

**하지 않기로 한 것**

- 매출·예매자 명단·판매 리포트 — 예매 테이블이 아직 없다. 예매 기능이 생긴 뒤 별도 문서
- 등급별 잔여 좌석 — `seat` 에 좌석별 상태가 없다. 좌석 선점 기능이 상태를 만든 뒤
- 좌석 맵 시각화, 메시지 발송, 출석 체크, 통계 (참조 대조 §2-2)
- 등록 후 좌석 구성(구역·가격) 변경 — 비공개여도 불가. ADR-0009 의 좌석 개별 행 생성 규칙을 다시 열어야 한다
- 소유자 없는 기존 공연의 소유자 지정 — 관객 화면에는 남고 쓰기는 막힌다. 필요하면 운영 SQL 로 한다
- 운영자 승인 후 공개 — 주최자가 스스로 공개한다 (product.md §7 열린 질문은 "승인 없음"으로 답한다, §8)
- 공연 복제, 공연 삭제

**나중에 할 수도 있는 것**

- 입금 확인 내부 API 의 보호 방식 — 그 기능(ADR-0025) 을 만들 때 정한다 (§8)
- 공개 예약(지정 시각에 자동 공개)
- 주최자 여러 명이 한 공연을 함께 관리

---

## 8. 주요 결정 사항

| 결정 | 이유 | 범위 | ADR |
|---|---|---|---|
| 쓰기 가드(`X-Write-Key`) 완전 제거, `/api/**` 쓰기는 JWT 역할 + 소유권으로 보호 | ADR-0017 의 제거 조건 충족. 키가 프론트 번들에 노출되는 한계(ADR-0016) 해소 | **전체** | ADR-0029 (ADR-0017 대체, ADR-0025 의 「쓰기 키 재사용」 무효화) |
| 입금 확인 내부 API 보호 수단은 미정으로 둔다 | 가드를 지우면 ADR-0025 의 근거가 사라진다. 그 API 는 아직 없다 | **전체** | ADR-0029 — 방향은 운영자(ADMIN) 역할 + JWT, 방식은 그 기능에서 |
| 역할 판정은 JWT `role` 클레임 → `ROLE_*` 권한, `SecurityConfig` 에서 경로별 `hasRole` | 역할 불변이라 DB 재조회 불필요. 접근 정책 단일 지점(ADR-0013) 유지 | **전체** | ADR-0029 |
| 소유권 위반·소유자 없음·없는 공연을 모두 404 로 | 남의 공연 존재를 드러내지 않는다 | **전체** (앞으로의 소유 자원 전부) | ADR-0029 |
| 공연은 비공개로 등록되고, 공개 전환은 양방향·오픈 전에만 | 준비 중 공연을 숨기되, 예매가 열린 뒤 사라지지 않게 | 이 기능만 | — |
| 운영자 승인 없이 주최자가 직접 공개 | 운영자 기능이 범위 밖(product.md §4) | 이 기능만 | — (product.md §7 열린 질문 갱신) |
| 주최자 응답은 `OrganizerPerformanceResponse` 로 타입 분리, 가능 여부 플래그는 서버 계산 | 관객 응답에 주최자 필드 누출 방지. 시각 판단은 서버(ADR-0026) | 이 기능만 | — |
| 주최자 화면 시안을 태스크 생성 전에 `out/screens/` 에 추가 [완료 2026-11-11, 12장] | 화면은 시안과 같아야 한다(ADR-0022) | 이 기능만 | — |
| 「비공개」는 7번째 의미 배지 `is-draft`(청록 `#0B8086`) | 주최자가 해야 할 일이라 회색 불가, 기존 6색은 모두 다른 뜻. 시안 검사기가 색 공유를 막는다 | **전체** (디자인 정본) | — design.md §4·§5 갱신 |
| 주최자 상태 이름은 관객과 같다 (예정·예매 가능·매진·마감·공연 취소) | 같은 공연을 두 이름으로 부르지 않는다 | 이 기능만 | — |
| 프론트 요청 토큰은 앱에 하나뿐인 AuthProvider 의 세션에서 읽는다 (TASK-024 Phase 4 RC-1) | 저장 실패·다른 탭에서 화면과 요청 토큰이 갈라지지 않게 | **전체** | ADR-0030 |
