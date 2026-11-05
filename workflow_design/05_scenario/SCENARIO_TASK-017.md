# TASK-017 시나리오

대상: `frontend/src/theme/`(tokens.ts · index.ts · GlobalTokens.tsx), `frontend/src/components/ui/`(AppBar · Page · Block ·
Row · Badge · CtaBar · Field · Toast · Sheet), `frontend/src/components/StatusBadge.tsx`, `frontend/index.html`.

**jsdom 은 레이아웃·서체 로드를 계산하지 않는다.** 서체가 실제로 로드되는지와 375px 가로 넘침은 Phase 4 화면 확인
(브라우저)이 증거다. 아래에서 「(화면 확인)」 표시가 붙은 Then 이 그것이다. 테스트는 그 상태를 만드는 설정을 확인한다.

## 규약 (정본)

| 항목 | 값 |
|---|---|
| 토큰 정본 | `docs/product/design-tokens.css` 의 `:root` 선언 전부와 `.text-large` 선언 전부 |
| 배지 뜻 (design.md §5) | OPEN → `예매 가능` · brand / UPCOMING → `예정` · info / SOLD_OUT → `매진` · danger / CLOSED → `마감` · neutral / CANCELLED → `공연 취소` · neutral / (Badge 로만) `입금 대기` · warning / `예매 확정` · success |
| 배지 색 (tone → 바탕 / 글자) | brand → `#EBEDFE` / `#4154FF` · info → `#F0EEFE` / `#6647FF` · danger → `#FCEFF0` / `#D61E24` · warning → `#FBF4E5` / `#916500` · success → `#E7F4ED` / `#187B43` · neutral → `#F1F2F6` / `#4C4F66` (시안의 배지는 흰 블록·행 위에 놓여 --c-fill 이 --c-bg 가 된다) |
| 토스트 | role=status, 2,500ms 뒤 사라진다 |
| 한 열 최대 폭 | 375px (시안 뷰포트) |
| 서체 | Pretendard 1.3.9 (`cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css`) |

## SC-01 (happy) tokens.ts 의 값이 design-tokens.css 와 하나도 다르지 않다

- **Given** `docs/product/design-tokens.css` 를 읽어 `:root` 와 `.text-large` 의 모든 `--이름: 값` 쌍을 모았다
- **When** `frontend/src/theme/tokens.ts` 가 내보내는 토큰 표와 대조한다
- **Then** 정본의 모든 이름이 토큰 표에 있고 값이 글자까지 같다 (색은 대소문자 무시)
- **And** 토큰 표에 정본에 없는 이름이 없다 (부품 치수는 별도 표로 둔다)

covers: `frontend/src/theme/tokens.ts` 의 값이 `docs/product/design-tokens.css` 와 하나도 다르지 않다 (테스트로 대조)
flow: F1

## SC-02 (happy) 테마가 토큰 값으로 만들어진다

- **Given** 새 테마를 만든다
- **When** 테마의 팔레트·서체·모양을 읽는다
- **Then** 아래 표와 같다

| 테마 값 | 기대 |
|---|---|
| primary.main | `#4154FF` |
| background.default / paper | `#F1F2F6` / `#FEFEFE` |
| text.primary / secondary | `#1A1C28` / `#4C4F66` |
| error.main | `#E1292F` |
| fontFamily | `Pretendard` 로 시작 |
| h1(제목) 크기·굵기 | 28px · 700 |
| body1 크기·행간 | 16px · 1.55 |
| shape.borderRadius | 12 (--r-m) |

covers: `frontend/src/theme/tokens.ts` 의 값이 `docs/product/design-tokens.css` 와 하나도 다르지 않다 (테스트로 대조)
flow: F1, F2

## SC-03 (happy) 공연 상태 배지가 design.md §5 의 뜻·문구·색으로 나온다

- **Given** 새 테마를 적용했다
- **When** 아래 표의 상태로 StatusBadge 를 그린다
- **Then** 문구와 바탕색·글자색이 표와 같다

| 상태 | 문구 | tone |
|---|---|---|
| OPEN | 예매 가능 | brand |
| UPCOMING | 예정 | info |
| SOLD_OUT | 매진 | danger |
| CLOSED | 마감 | neutral |
| CANCELLED | 공연 취소 | neutral |

- **And** 옛 문구(`예매가능` `예매예정` `예매마감` `공연취소`)는 어느 행에도 없다

covers: 상태 배지가 design.md §5 의 6가지 뜻·문구·클래스로만 나온다 (예매 가능·예정·매진·마감·입금 대기·예매 확정, 공연 취소는 마감과 같은 회색)
flow: F5

## SC-04 (happy) Badge 의 tone 여섯 가지가 서로 다른 색이다

- **Given** 새 테마를 적용했다
- **When** Badge 를 tone 별로 그린다 — brand `예매 가능` · info `예정` · danger `매진` · neutral `마감` · warning `입금 대기` · success `예매 확정`
- **Then** 각 배지의 바탕색·글자색이 규약 표와 같다
- **And** 여섯 배지의 (바탕, 글자) 쌍이 서로 모두 다르다 — 한 색은 한 뜻

covers: 상태 배지가 design.md §5 의 6가지 뜻·문구·클래스로만 나온다 (예매 가능·예정·매진·마감·입금 대기·예매 확정, 공연 취소는 마감과 같은 회색)
flow: F5

## SC-05 (error) 알 수 없는 상태값이면 배지를 그리지 않는다

- **Given** 서버가 정의 밖 상태 `POSTPONED` 를 돌려줬다
- **When** 그 값으로 StatusBadge 를 그린다
- **Then** 렌더가 예외 없이 끝나고 그린 영역이 비어 있다

covers: 상태 배지가 design.md §5 의 6가지 뜻·문구·클래스로만 나온다 (예매 가능·예정·매진·마감·입금 대기·예매 확정, 공연 취소는 마감과 같은 회색)
flow: F5

## SC-06 (happy) 토스트가 문구를 띄우고 스스로 사라진다

- **Given** 가짜 타이머를 켰다
- **When** 문구 `아이디를 복사했어요 — 일행에게 붙여넣어 보내세요` 로 Toast 를 연다
- **Then** role=status 영역에 그 문구가 보인다
- **And** 2,499ms 가 지나도 보인다
- **And** 2,500ms 가 지나면 사라지고 닫힘 콜백이 한 번 불린다

covers: `data-stay` 에 해당하는 토스트 부품이 문구를 띄우고 스스로 사라진다
flow: F6

## SC-07 (error) 빈 문구로는 토스트가 뜨지 않는다

- **Given** 가짜 타이머를 켰다
- **When** 문구 `` 또는 공백만(`   `)으로 Toast 를 연다
- **Then** role=status 영역에 보이는 문구가 없다

covers: `data-stay` 에 해당하는 토스트 부품이 문구를 띄우고 스스로 사라진다
flow: F6

## SC-08 (happy) 움직임 줄이기 설정이면 토스트에 전환 효과가 없다

- **Given** `prefers-reduced-motion: reduce` 가 켜져 있다
- **When** Toast 를 연다
- **Then** 토스트 요소의 transition 이 `none` 이다
- **And** 설정이 꺼져 있으면 transition 이 `none` 이 아니다

covers: `data-stay` 에 해당하는 토스트 부품이 문구를 띄우고 스스로 사라진다
flow: F6

## SC-09 (happy) 공통 부품이 토큰 값으로 그려진다

- **Given** 새 테마를 적용했다
- **When** 아래 표의 부품을 그린다
- **Then** 표의 스타일 값이 그 토큰과 같다

| 부품 | 스타일 | 기대 (토큰) |
|---|---|---|
| AppBar | 높이 · 제목 굵기 | 56px (--h-appbar) · 600 (--fw-strong) |
| AppBar 뒤로 버튼 | 너비·높이 | 44px (부품 치수 터치) |
| Block | 바탕 · 모서리 · 안쪽 여백 | `#FEFEFE` · 20px (--r-l) · 24px (--block-pad) |
| Row | 최소 높이 | 64px (--h-row) |
| CtaBar 주 버튼 | 높이 · 바탕 · 글자 · 모서리 | 56px (--h-btn) · `#4154FF` · `#FFFFFF` · 12px (--r-m) |
| Field 입력 (오류) | 테두리 · 바탕 | `#E1292F` · `#FCEFF0` |
| Field 오류 문구 | 글자색 · 아이콘 있음 | `#E1292F` · 문구 앞에 아이콘 |
| Sheet | 모서리 | 20px 20px 0 0 |

- **And** Field 입력은 오류일 때 `aria-invalid="true"` 이고 오류 문구와 `aria-describedby` 로 이어진다
- **And** AppBar 뒤로 버튼은 `aria-label="뒤로"` 이고 누르면 뒤로 가기 콜백이 한 번 불린다

covers: 테마 밖 색상 리터럴·매직 치수가 없다 (`check_architecture.py` DESIGN-001·002 통과, 003 위반 신규 0)
flow: F2

## SC-10 (happy) 테마 밖 색·매직 치수가 없다

- **Given** 이 태스크의 변경이 적용된 저장소다
- **When** `python3 scripts/check_architecture.py --json` 을 실행한다
- **Then** DESIGN-001·DESIGN-002 위반이 0건이다
- **And** DESIGN-003 위반은 변경 전과 같은 3건(기존 파일)뿐이고 `frontend/src/theme/`·`frontend/src/components/` 의 위반은 0건이다
- **And** 세 제약 모두 `no_target` 에 없다

covers: 테마 밖 색상 리터럴·매직 치수가 없다 (`check_architecture.py` DESIGN-001·002 통과, 003 위반 신규 0)
flow: F2

## SC-11 (happy) Pretendard 를 불러오고 옛 서체를 불러오지 않는다

- **Given** `frontend/index.html` 이다
- **When** 문서의 스타일시트 링크를 읽는다
- **Then** 규약 표의 Pretendard 스타일시트를 불러온다
- **And** `Noto Serif KR`·`IBM Plex Sans KR` 을 불러오지 않는다
- **And** (화면 확인) 앱을 띄우면 `document.fonts` 에 Pretendard 가 `loaded` 로 있고 본문 계산 서체가 Pretendard 다

covers: Pretendard 가 실제로 로드된다 (`document.fonts` 확인 또는 네트워크 요청)
flow: F3

## SC-12 (happy) 앱은 375px 한 열로 가운데 놓인다

- **Given** 전역 토큰 스타일을 적용했다
- **When** `#root` 의 계산된 스타일을 읽는다
- **Then** 최대 폭 375px, 좌우 바깥 여백 auto 다
- **And** body 바탕이 `#F1F2F6`, 가로 넘침이 숨겨지지 않고(overflow-x 가 hidden 이 아니다) 한국어 줄바꿈이 `keep-all` 이다
- **And** (화면 확인) 375px 뷰포트에서 `document.documentElement.scrollWidth` ≤ 375, 1280px 뷰포트에서 내용 열의 왼쪽·오른쪽 여백이 같다

covers: 375px 폭에서 가로 스크롤이 없고, 넓은 화면에서는 가운데 한 열로 보인다
flow: F4

## SC-13 (regression) 기존 공연 목록·상세 화면이 새 배지 문구로 나온다

- **Given** 공연 API 가 상태별 공연 다섯 개(OPEN·UPCOMING·SOLD_OUT·CLOSED·CANCELLED)를 돌려준다
- **When** 공연 목록 화면을 그린다
- **Then** `예매 가능` `예정` `매진` `마감` `공연 취소` 배지가 각각 하나씩 있다
- **And** 옛 문구는 없다
- **And** 기존 목록·상세·공연 등록·수정 화면 테스트가 옛 배지 문구·옛 토큰 값 단언을 새 값으로 바꾼 뒤 모두 통과한다 (단언 수는 줄지 않는다)

covers: 기존 화면(목록·상세·공연 등록·수정) 테스트가 새 배지 문구로 고친 뒤 모두 통과한다
flow: F7

## SC-14 (happy) 토스트 타이머는 문구가 바뀌면 새로 세고, 사라지기 전에 닫히면 정리된다

- **Given** 가짜 타이머를 켰다
- **When** 아래 표의 순서로 Toast 를 다룬다
- **Then** 결과가 표와 같다

| 순서 | 결과 |
|---|---|
| 문구 A 로 연다 → 2,000ms → 문구 B 로 바꾼다 → 2,499ms | 문구 B 가 보이고 닫힘 콜백은 불리지 않았다 |
| 위에 이어 1ms 더 (문구 B 로 바꾼 뒤 2,500ms) | 사라지고 닫힘 콜백이 한 번 불린다 |
| 문구 A 로 연다 → 1,000ms → 언마운트 → 5,000ms | 닫힘 콜백이 불리지 않고 남은 타이머가 없다 |
| 길이 300자의 공백 없는 문구로 연다 | 문구가 줄을 바꿔 보인다 (토스트의 overflow-wrap 이 anywhere) · (화면 확인) 375px 에서 가로 넘침 없음 |

covers: `data-stay` 에 해당하는 토스트 부품이 문구를 띄우고 스스로 사라진다
flow: F6

## SC-15 (happy) 켜졌던 오류·시트가 꺼지면 흔적이 남지 않는다

- **Given** 새 테마를 적용했다
- **When** 아래 표대로 부품을 다시 그린다
- **Then** 결과가 표와 같다

| 부품 | 처음 → 다시 | 다시 그린 뒤 |
|---|---|---|
| Field | 오류 문구 `이메일을 입력해 주세요` → 오류 없음 | 오류 문구 없음, `aria-invalid` 없음, `aria-describedby` 없음, 입력 테두리가 오류색이 아님 |
| Field | 오류 문구 `` (빈 문자열) | 오류 모양 아님, `aria-invalid` 없음, `aria-describedby` 없음 |
| Sheet | open → 닫힘 | 시트가 화면에 없다 |

covers: 테마 밖 색상 리터럴·매직 치수가 없다 (`check_architecture.py` DESIGN-001·002 통과, 003 위반 신규 0)
flow: F2

---

## 시나리오로 쓰지 않은 것

- **화면 재배치(목록·상세를 시안 구조로)** — 흐름 2. 이 태스크는 새 테마로 색·서체가 바뀌는 것까지
- **AppHeader 교체** — AC 밖(PLAN unresolved #5). AppBar 부품만 SC-09 에서 확인
- **입금 대기·예매 확정 배지를 쓰는 화면** — 예매 모델이 흐름 4·5. 이 태스크는 Badge tone 만(SC-04)
