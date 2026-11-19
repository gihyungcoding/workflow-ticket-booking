import { screen, waitFor, within } from '@testing-library/react'
import { css, isTokenColor, tokenColor } from '../test/task020'
import { L1, L2, L3, L5, L7, cardLinks, inCard, tokenValue } from '../test/task021'
import { token as themeToken } from '../theme/tokens'
import {
  BADGES,
  C,
  CHIPS,
  D,
  DC,
  DO,
  EMPTY_SUB,
  EMPTY_TITLE,
  FIRST_REGISTER,
  HOME_LIST,
  LIST_ERROR,
  LIST_KEY,
  LIST_PATH,
  O,
  ONLY_TITLE,
  ORG,
  QUERY_OF,
  S,
  SEE_ALL,
  U,
  Z,
  badgesIn,
  bearerOf,
  callsTo,
  chip,
  chipGroup,
  chipNames,
  click,
  deferred,
  errorShown,
  fab,
  fromState,
  hasLine,
  heroTitles,
  jsonWithInfinity,
  listUrls,
  loginNow,
  logoutNow,
  many,
  moreButton,
  moreFailButton,
  navigateTo,
  networkError,
  openOrganizer,
  orgBody,
  patched,
  pause,
  pressedChips,
  raw,
  renderApp,
  res,
  retryButton,
  rowIds,
  rowLinks,
  rowOf,
  serve,
  skeletons,
  token,
  where,
} from '../test/task025'
import type { Item, Responder } from '../test/task025'

// TASK-025 시나리오 SC-06~SC-13, SC-15~SC-19, SC-23 — 내 공연 목록 화면.
// SoT: workflow_design/05_scenario/SCENARIO_TASK-025.md
// 표로 묶은 시나리오는 test.each 한 줄(= 시나리오 한 건)이고 행마다 케이스가 하나씩 돈다.
// 로딩 자리표시는 data-testid="organizer-row-skeleton" 으로 찾는다(홈의 performance-card-skeleton 관례).

const P = LIST_PATH
const waitIds = (ids: number[]) => waitFor(() => expect(rowIds()).toEqual(ids))
const seeAll = () => screen.queryByRole('button', { name: SEE_ALL }) ?? screen.queryByRole('link', { name: SEE_ALL })
const idsOf = (items: Item[]) => items.map((i) => i.id as number)
const leaf = (root: Element, text: string) => {
  const found = Array.from(root.querySelectorAll('*')).filter((el) => (el.textContent ?? '').trim() === text)
  return found[found.length - 1]
}
const posterImgs = () => Array.from(document.querySelectorAll('img')).filter((img) => /^https?:\/\//.test(img.getAttribute('src') ?? ''))
const errorScreen = () => {
  expect(errorShown()).toBe(true)
  expect(retryButton()).not.toBeNull()
  expect(rowLinks()).toHaveLength(0)
}

// ---------- F5 행 표시 ----------

describe('TASK-025 F5 — 내 공연 행', () => {
  const ROWS06: { id: number; badge: string | null; title: string; l3: string; l4: string; poster: string | null }[] = [
    { id: 11, badge: '비공개', title: '소리풍경 밴드 나이트 앙코르', l3: '12월 26일(토) 오후 7:00 · 롤링홀', l4: '12월 1일(화) 오후 8:00 오픈 · 300석', poster: null },
    { id: 12, badge: '예매 가능', title: '소리풍경 밴드 나이트', l3: '11월 21일(토) 오후 7:00 · 롤링홀', l4: '남은 좌석 37 / 300석', poster: 'https://img.ticket.on/p12.jpg' },
    { id: 13, badge: null, title: '소리풍경 어쿠스틱 라이브', l3: '10월 10일(토) 오후 6:00 · 벨로주 홍대', l4: '공연 취소', poster: null },
    { id: 14, badge: '마감', title: '소리풍경 밴드 여름 단독', l3: '8월 22일(토) 오후 7:00 · 롤링홀', l4: '남은 좌석 12 / 300석', poster: null },
    { id: 15, badge: '예정', title: '가을 재즈 콘서트', l3: '12월 5일(토) 오후 8:00 · 블루노트 서울', l4: '11월 20일(금) 오후 8:00 오픈 · 200석', poster: null },
    { id: 16, badge: '매진', title: '겨울 클래식 갈라', l3: '12월 19일(토) 오후 7:00 · 예술의전당', l4: '남은 좌석 0 / 400석', poster: null },
    { id: 18, badge: '비공개', title: '열린 뒤에도 비공개', l3: '12월 26일(토) 오후 7:00 · 롤링홀', l4: '12월 1일(화) 오후 8:00 오픈 · 100석', poster: null },
    { id: 17, badge: null, title: '취소된 비공개', l3: '12월 26일(토) 오후 7:00 · 롤링홀', l4: '공연 취소', poster: null },
  ]
  test('SC-06 (happy) 내 공연 행은 배지·공연명·일시·장소·둘째 줄·포스터를 시안대로 보여준다', async () => {
    // Given ORG 로그인 상태이고 LIST 가 BODY([D, O, C, Z, U, S, DO, DC]) 를 200 으로 준다
    // When /organizer 를 연다
    openOrganizer({ list: { '': orgBody([D, O, C, Z, U, S, DO, DC]) } })

    // Then 행이 8개이고 서버가 준 순서 그대로다
    await waitIds([11, 12, 13, 14, 15, 16, 18, 17])
    // And LIST 가 정확히 1번, 쿼리 없이 나가고 Authorization 이 Bearer tok-org 다
    expect(listUrls()).toEqual([P])
    expect(bearerOf(callsTo(LIST_KEY)[0].init)).toBe('Bearer tok-org')
    // And 행마다 표와 같다 — 배지는 표의 것 하나뿐이고 취소 행은 배지가 없다
    for (const r of ROWS06) {
      const row = rowOf(r.id)
      expect(badgesIn(row)).toEqual(r.badge ? [r.badge] : [])
      expect(hasLine(row, r.title)).toBe(true)
      expect(hasLine(row, r.l3)).toBe(true)
      expect(hasLine(row, r.l4)).toBe(true)
      // And 포스터는 행 12 에만 이미지가 있고 src 가 posterUrl 이다
      const imgs = Array.from(row.querySelectorAll('img')).filter((img) => /^https?:\/\//.test(img.getAttribute('src') ?? ''))
      if (r.poster) expect(imgs.map((i) => i.getAttribute('src'))).toEqual([r.poster])
      else expect(imgs).toHaveLength(0)
    }
    // And 배지 문구는 표의 글자 그대로다
    expect(BADGES).toEqual(['비공개', '예정', '예매 가능', '매진', '마감'])
    // And 「공연 등록」 버튼이 하나 있다
    expect(fab()).not.toBeNull()
  })
  const SC07: { row: string; name: string; chip?: string; first: Item[]; chipItems?: Item[]; eyebrow: string | null; title: string }[] = [
    { row: 'a', name: '한도윤', first: [], eyebrow: '한도윤 님', title: '첫 공연을 등록해 보세요' },
    { row: 'b', name: '한도윤', first: [D], eyebrow: '한도윤 님', title: '비공개 공연이 1건 있어요' },
    { row: 'c', name: '한도윤', first: [D, DO, O], eyebrow: '한도윤 님', title: '비공개 공연이 2건 있어요' },
    { row: 'd', name: '한도윤', first: [O, Z], eyebrow: '한도윤 님', title: '내 공연' },
    { row: 'e', name: '한도윤', first: [DC, C], eyebrow: '한도윤 님', title: '내 공연' },
    { row: 'f', name: '한도윤', chip: '비공개', first: [D, O], chipItems: [D], eyebrow: '한도윤 님', title: '비공개 공연이 1건 있어요' },
    { row: 'g', name: '한도윤', chip: '예매 가능', first: [O], chipItems: [], eyebrow: '한도윤 님', title: '내 공연' },
    { row: 'h 빈 이름', name: '', first: [O], eyebrow: null, title: '내 공연' },
    { row: 'i 공백만', name: '   ', first: [O], eyebrow: null, title: '내 공연' },
    { row: 'j 전각 공백·U+200B', name: '　​', first: [O], eyebrow: null, title: '내 공연' },
    { row: 'k NBSP', name: ' ', first: [O], eyebrow: null, title: '내 공연' },
    { row: 'l 줄 구분자', name: ' ', first: [O], eyebrow: null, title: '내 공연' },
    { row: 'm 300자', name: '가'.repeat(300), first: [O], eyebrow: `${'가'.repeat(300)} 님`, title: '내 공연' },
    { row: 'n HTML 문자', name: '<b>한도윤</b> &amp; "x"', first: [O], eyebrow: '<b>한도윤</b> &amp; "x" 님', title: '내 공연' },
  ]
  test.each(SC07)('SC-07 (boundary) 머리글은 이름과 상태별 제목을 보여주고, 이름이 비어 있으면 작은 글을 그리지 않는다 — $row', async ({ name, chip: chipName, first, chipItems, eyebrow, title }) => {
    // Given 로그인한 ORG 의 이름과 LIST 응답이 표와 같다 (빈·공백 이름은 /me 검증을 지나지 못해 세션 API 로 로그인한다)
    const list: Record<string, unknown> = { '': orgBody(first) }
    if (chipName) list[QUERY_OF[chipName as (typeof CHIPS)[number]]] = orgBody(chipItems ?? [])
    serve({ user: null, list })
    renderApp('/login')
    await loginNow({ ...ORG, name })

    // When /organizer 를 열고 표의 칩을 누른다
    await navigateTo('/organizer')
    await waitFor(() => expect(listUrls().length).toBeGreaterThan(0))
    if (chipName) {
      await waitFor(() => expect(chip(chipName).getAttribute('aria-pressed')).toBe('false'))
      await click(chip(chipName))
      await waitFor(() => expect(listUrls()).toHaveLength(2))
    }
    await pause()

    // Then 작은 글과 제목이 표와 같다
    if (eyebrow === null) expect(screen.queryByText(/님$/)).toBeNull()
    else {
      expect(screen.getByText(eyebrow)).toBeInTheDocument()
      // And HTML 문자는 글자 그대로다(태그로 해석되지 않는다)
      expect(document.querySelector('b')).toBeNull()
    }
    expect(heroTitles()).toContain(title)
  })

  // ---------- 응답 계약 위반 (SC-08) ----------
  const INF = '__INF__'
  const BIGNUM = Number.MAX_SAFE_INTEGER + 2
  const one = (patch: Item) => orgBody([patched(O, patch)])
  const SC08: [string, unknown][] = [
    ['본문이 배열', []],
    ['본문이 문자열', 'x'],
    ['본문이 null', null],
    ['items 없음', { page: 0, size: 20, totalElements: 1 }],
    ['items 객체', { ...orgBody([O]), items: {} }],
    ['items 문자열', { ...orgBody([O]), items: 'x' }],
    ['items null', { ...orgBody([O]), items: null }],
    ['items [null]', orgBody([null], 1)],
    ['items [O, null]', orgBody([O, null], 2)],
    ['items ["x"]', orgBody(['x'], 1)],
    ['items [[]]', orgBody([[]], 1)],
    ['totalElements 없음', { ...orgBody([O]), totalElements: undefined }],
    ['totalElements 문자열', orgBody([O], '20')],
    ['totalElements null', orgBody([O], null)],
    ['totalElements -1', orgBody([O], -1)],
    ['totalElements 1.5', orgBody([O], 1.5)],
    ['totalElements 1e400', orgBody([O], INF)],
    ['totalElements 2^53+1', orgBody([O], BIGNUM)],
    ['totalElements {}', orgBody([O], {})],
    ['id 0', one({ id: 0 })],
    ['id "12"', one({ id: '12' })],
    ['id 없음', one({ id: undefined })],
    ['id 1.5', one({ id: 1.5 })],
    ['id -1', one({ id: -1 })],
    ['id 1e400', one({ id: INF })],
    ['id 2^53+1', one({ id: BIGNUM })],
    ['id null', one({ id: null })],
    ...(
      [
        ['빈 문자열', ''],
        ['공백만', '   '],
        ['전각 공백', '　'],
        ['NBSP', ' '],
        ['U+200B', '​'],
        ['줄 구분자', ' '],
        ['숫자 12', 12],
        ['null', null],
        ['키 없음', undefined],
      ] as [string, unknown][]
    ).map(([label, v]): [string, unknown] => [`title ${label}`, one({ title: v })]),
    ...(
      [
        ['빈 문자열', ''],
        ['공백만', '   '],
        ['전각 공백', '　'],
        ['숫자 7', 7],
        ['null', null],
        ['키 없음', undefined],
      ] as [string, unknown][]
    ).map(([label, v]): [string, unknown] => [`venue ${label}`, one({ venue: v })]),
    ...(['startAt', 'openAt'] as const).flatMap((field) =>
      (
        [
          ['빈 문자열', ''],
          ['공백만', '   '],
          ['전각 공백', '　'],
          ['soon', 'soon'],
          ['날짜만', '2026-12-26'],
          ['오프셋 없음', '2026-12-26T19:00:00'],
          ['없는 날짜', '2026-02-30T10:00:00Z'],
          ['24시', '2026-12-26T24:00:00Z'],
          ['숫자', 1766743200000],
          ['null', null],
          ['키 없음', undefined],
          ['2001자', 'x'.repeat(2001)],
        ] as [string, unknown][]
      ).map(([label, v]): [string, unknown] => [`${field} ${label}`, one({ [field]: v })]),
    ),
    ...(
      [
        ['PAUSED', 'PAUSED'],
        ['소문자 open', 'open'],
        ['숫자 5', 5],
        ['null', null],
        ['배열', ['OPEN']],
        ['키 없음', undefined],
      ] as [string, unknown][]
    ).map(([label, v]): [string, unknown] => [`status ${label}`, one({ status: v })]),
    ...(['totalSeats', 'availableSeats'] as const).flatMap((field) =>
      (
        [
          ['-1', -1],
          ['1.5', 1.5],
          ['문자열', '300'],
          ['1e400', INF],
          ['2^53+1', BIGNUM],
          ['null', null],
          ['{}', {}],
          ['키 없음', undefined],
        ] as [string, unknown][]
      ).map(([label, v]): [string, unknown] => [`${field} ${label}`, one({ [field]: v })]),
    ),
    ['availableSeats 301 > totalSeats 300', one({ totalSeats: 300, availableSeats: 301 })],
    ...(
      [
        ['문자열 true', 'true'],
        ['숫자 1', 1],
        ['null', null],
        ['키 없음', undefined],
      ] as [string, unknown][]
    ).map(([label, v]): [string, unknown] => [`published ${label}`, one({ published: v })]),
  ]
  test.each(SC08)('SC-08 (error) 응답이 계약을 어기면 일부만 그리지 않고 목록 전체를 오류로 본다 — %s', async (_label, body) => {
    // Given ORG 로그인 상태이고 LIST 가 200 으로 한 곳만 어긋난 본문을 준다
    // When /organizer 를 연다
    openOrganizer({ list: { '': body } })

    // Then 오류 화면이고 공연 행이 하나도 없다
    await screen.findByText(LIST_ERROR)
    errorScreen()
    // And 빈 안내는 보이지 않는다
    expect(screen.queryByText(EMPTY_TITLE)).toBeNull()
    expect(screen.queryByText(/ 공연이 없어요$/)).toBeNull()
    // And 칩 7개와 「공연 등록」 버튼은 보인다
    expect(chipNames()).toEqual([...CHIPS])
    expect(fab()).not.toBeNull()
    // And LIST 는 1번만 나갔다(자동으로 다시 부르지 않는다)
    await pause()
    expect(listUrls()).toHaveLength(1)
  })

  // ---------- 계약 안의 값 (SC-09) ----------
  const url2000 = `https://img.ticket.on/${'a'.repeat(2000)}`
  const SC09: { row: string; items: Item[]; total?: number; check: () => void }[] = [
    { row: 'a 공연명 300자', items: [patched(O, { title: '가'.repeat(300) })], check: () => expect(hasLine(rowOf(12), '가'.repeat(300))).toBe(true) },
    { row: 'b 장소 300자', items: [patched(O, { venue: '나'.repeat(300) })], check: () => expect(hasLine(rowOf(12), `11월 21일(토) 오후 7:00 · ${'나'.repeat(300)}`)).toBe(true) },
    { row: 'c posterUrl 없음', items: [patched(O, { posterUrl: undefined })], check: () => expect(posterImgs()).toHaveLength(0) },
    { row: 'd posterUrl null', items: [patched(O, { posterUrl: null })], check: () => expect(posterImgs()).toHaveLength(0) },
    { row: 'e posterUrl 빈 문자열', items: [patched(O, { posterUrl: '' })], check: () => expect(posterImgs()).toHaveLength(0) },
    { row: 'f posterUrl 공백', items: [patched(O, { posterUrl: '   ' })], check: () => expect(posterImgs()).toHaveLength(0) },
    { row: 'g posterUrl 전각 공백', items: [patched(O, { posterUrl: '　' })], check: () => expect(posterImgs()).toHaveLength(0) },
    { row: 'h posterUrl 숫자', items: [patched(O, { posterUrl: 12 })], check: () => expect(posterImgs()).toHaveLength(0) },
    { row: 'i posterUrl 2000자', items: [patched(O, { posterUrl: url2000 })], check: () => expect(posterImgs().map((i) => i.getAttribute('src'))).toEqual([url2000]) },
    { row: 'j 좌석 0 / 0', items: [patched(S, { totalSeats: 0, availableSeats: 0 })], check: () => expect(hasLine(rowOf(16), '남은 좌석 0 / 0석')).toBe(true) },
    { row: 'k 좌석 300 / 300', items: [patched(O, { availableSeats: 300 })], check: () => expect(hasLine(rowOf(12), '남은 좌석 300 / 300석')).toBe(true) },
    { row: 'l closeAt 없음·이상', items: [patched(O, { closeAt: undefined }), patched(U, { closeAt: '??' })], check: () => expect(rowIds()).toEqual([12, 15]) },
    { row: 'm 모르는 키', items: [patched(O, { sections: [], editable: true, visibilityChangeable: false, extra: 1 })], check: () => expect(rowIds()).toEqual([12]) },
    { row: 'n 101개', items: many(1, 101), total: 101, check: () => {
        expect(rowLinks()).toHaveLength(101)
        expect(moreButton()).toBeNull()
      } },
    { row: 'o 같은 id 둘', items: [O, O], check: () => expect(rowIds()).toEqual([12]) },
    { row: 'p 연도 0099', items: [patched(O, { startAt: '0099-12-26T10:00:00Z' })], check: () => expect(rowIds()).toEqual([12]) },
  ]
  test.each(SC09)('SC-09 (boundary) 계약 안의 극단 값과 화면이 읽지 않는 값은 오류가 아니라 그대로 그려진다 — $row', async ({ items, total, check }) => {
    // Given ORG 로그인 상태이고 LIST 가 200 으로 표의 변형을 가진 BODY 를 준다
    // When /organizer 를 연다
    openOrganizer({ list: { '': orgBody(items, total) } })

    // Then 오류 문구가 없고 표의 결과가 맞다
    await waitFor(() => expect(rowLinks().length).toBeGreaterThan(0))
    expect(errorShown()).toBe(false)
    check()
  })
})

// ---------- F5·F8 HTTP 결과 ----------

describe('TASK-025 F5·F8 — 서버 결과', () => {
  type Kind = 'error' | 'login' | 'notice'
  const SC10: { row: string; resp: () => Response; kind: Kind }[] = [
    { row: 'a 500', resp: () => res(500, {}), kind: 'error' },
    { row: 'b 500 다른 code', resp: () => res(500, { code: 'INTERNAL' }), kind: 'error' },
    { row: 'c 404', resp: () => res(404, { code: 'NOT_FOUND' }), kind: 'error' },
    { row: 'd 400', resp: () => res(400, { code: 'INVALID_REQUEST' }), kind: 'error' },
    { row: 'e 200 빈 본문', resp: () => res(200), kind: 'error' },
    { row: 'f 200 비 JSON', resp: () => raw(200, 'OK'), kind: 'error' },
    { row: 'g 200 null', resp: () => res(200, 'null'), kind: 'error' },
    { row: 'h 502 HTML', resp: () => raw(502, '<html>Bad Gateway</html>'), kind: 'error' },
    { row: 'i 네트워크', resp: () => networkError(), kind: 'error' },
    { row: 'j 401', resp: () => res(401, { code: 'UNAUTHORIZED' }), kind: 'login' },
    { row: 'k 401 다른 code', resp: () => res(401, { code: 'TOKEN_EXPIRED' }), kind: 'login' },
    { row: 'l 401 빈 본문', resp: () => res(401), kind: 'login' },
    { row: 'm 403', resp: () => res(403, { code: 'FORBIDDEN_ROLE' }), kind: 'notice' },
    { row: 'n 403 다른 code', resp: () => res(403, { code: 'OTHER' }), kind: 'notice' },
    { row: 'o 403 빈 본문', resp: () => res(403), kind: 'notice' },
    { row: 'p 403 비 JSON', resp: () => raw(403, 'Forbidden'), kind: 'notice' },
  ]
  test.each(SC10)('SC-10 (error) 서버가 실패하거나 권한을 거부하면 오류·로그인·주최자 전용 안내로 갈린다(상태로만 판별한다) — $row', async ({ resp, kind }) => {
    // Given ORG 로그인 상태이고 LIST 가 표의 응답을 준다
    // When /organizer 를 연다
    openOrganizer({ list: () => resp() })

    // Then 화면이 표와 같다
    if (kind === 'error') {
      await screen.findByText(LIST_ERROR)
      errorScreen()
    } else if (kind === 'login') {
      // 저장된 토큰이 지워지고 /login 으로 간다, from 이 /organizer 다
      await waitFor(() => expect(where()).toBe('/login'))
      expect(fromState()).toBe('/organizer')
      expect(token()).toBeNull()
    } else {
      // 주최자 전용 안내(토큰은 그대로, 주소는 /organizer)
      expect(await screen.findByText(ONLY_TITLE)).toBeInTheDocument()
      expect(token()).toBe('tok-org')
      expect(where()).toBe('/organizer')
    }
  })

  test('SC-11 (happy) 불러오는 동안에는 행 모양 자리표시 3개가 보이고 칩·떠 있는 버튼은 남는다', async () => {
    // Given ORG 로그인 상태이고 LIST 응답을 붙들어 둔다
    const held = deferred()
    openOrganizer({ list: () => held.promise })

    // When /organizer 를 연다 → Then 행 자리표시가 3개 보인다
    await waitFor(() => expect(skeletons()).toHaveLength(3))
    // And 공연 행·오류 문구·빈 문구가 없다
    expect(rowLinks()).toHaveLength(0)
    expect(errorShown()).toBe(false)
    expect(screen.queryByText(EMPTY_TITLE)).toBeNull()
    // And 칩 7개가 보이고 「전체」가 선택돼 있다
    expect(chipNames()).toEqual([...CHIPS])
    expect(pressedChips()).toEqual(['전체'])
    // And 「공연 등록」 버튼이 보인다
    expect(fab()).not.toBeNull()
    // And LIST 는 1번 나갔다
    expect(listUrls()).toHaveLength(1)

    // And 붙들어 둔 응답을 BODY([O]) 로 보내면 자리표시가 사라지고 행 1개가 보인다
    held.resolve(res(200, orgBody([O])))
    await waitIds([12])
    expect(skeletons()).toHaveLength(0)
  })

  // ---------- 다시 시도 (SC-12) ----------
  const SC12: { row: string; chip: (typeof CHIPS)[number]; after: 'ok' | '500' | '401' }[] = [
    { row: 'a 전체', chip: '전체', after: 'ok' },
    { row: 'b 매진', chip: '매진', after: 'ok' },
    { row: 'c 비공개', chip: '비공개', after: 'ok' },
    { row: 'd 한 번 더 500', chip: '전체', after: '500' },
    { row: 'e 401', chip: '전체', after: '401' },
  ]
  test.each(SC12)('SC-12 (error) 오류에서 「다시 시도」를 누르면 같은 필터로 다시 불러온다 — $row', async ({ chip: chipName, after }) => {
    // Given 표의 칩이 선택된 채 LIST 가 500 을 줘 오류 화면이 보인다
    const target = QUERY_OF[chipName]
    const held = deferred()
    let hits = 0
    const responder: Responder = (_init, url) => {
      const query = url.split('?')[1] ?? ''
      if (query !== target) return res(200, orgBody([O, Z]))
      hits += 1
      if (hits === 1) return res(500, { code: 'INTERNAL' })
      if (after === '500') return res(500, { code: 'INTERNAL' })
      if (after === '401') return res(401, { code: 'UNAUTHORIZED' })
      return held.promise
    }
    openOrganizer({ list: responder })
    if (chipName !== '전체') {
      await waitIds([12, 14])
      await click(chip(chipName))
    }
    await screen.findByText(LIST_ERROR)

    // When 「다시 시도」를 누른다
    await click(retryButton()!)

    // Then LIST 가 같은 쿼리로 한 번 더 나간다
    await waitFor(() => expect(listUrls().filter((u) => u === `${P}${target ? `?${target}` : ''}`)).toHaveLength(2))
    if (after === 'ok') {
      // And 다시 불러오는 동안 행 자리표시가 보이고 오류 문구는 없다
      expect(skeletons()).toHaveLength(3)
      expect(errorShown()).toBe(false)
      // And 이번 응답이 BODY([O]) 면 행 1개가 보이고 오류 문구가 사라진다
      held.resolve(res(200, orgBody([O])))
      await waitIds([12])
      expect(errorShown()).toBe(false)
    } else if (after === '500') {
      // 오류 화면이 그대로이고 「다시 시도」가 다시 눌린다
      await screen.findByText(LIST_ERROR)
      expect(retryButton()).not.toBeNull()
    } else {
      // 로그아웃되고 /login 이다
      await waitFor(() => expect(where()).toBe('/login'))
      expect(token()).toBeNull()
    }
  })
})

// ---------- F6 칩 ----------

describe('TASK-025 F6 — 칩', () => {
  const ITEMS_OF: Record<string, Item[]> = { 비공개: [D], 예정: [U], '예매 가능': [O], 매진: [S], 마감: [Z], 취소: [C] }
  const SC13: { row: string; kind: 'chip' | 'back-to-all' | 'same-all' | 'same-twice'; chip?: string }[] = [
    { row: 'b 비공개', kind: 'chip', chip: '비공개' },
    { row: 'c 예정', kind: 'chip', chip: '예정' },
    { row: 'd 예매 가능', kind: 'chip', chip: '예매 가능' },
    { row: 'e 매진', kind: 'chip', chip: '매진' },
    { row: 'f 마감', kind: 'chip', chip: '마감' },
    { row: 'g 취소', kind: 'chip', chip: '취소' },
    { row: 'h 매진 뒤 전체', kind: 'back-to-all' },
    { row: 'i 선택된 전체 다시', kind: 'same-all' },
    { row: 'j 매진 두 번', kind: 'same-twice' },
  ]
  test.each(SC13)('SC-13 (happy) 칩을 누르면 그 filter 로 첫 쪽을 다시 불러오고 선택이 바뀐다 — $row', async ({ kind, chip: chipName }) => {
    // Given ORG 로그인 상태이고 /organizer 가 BODY([O, Z]) 로 열려 있다(처음 LIST 1번)
    const held = deferred()
    const table: Record<string, unknown> = { '': orgBody([O, Z]) }
    for (const [name, items] of Object.entries(ITEMS_OF)) table[QUERY_OF[name as (typeof CHIPS)[number]]] = orgBody(items)
    const responder: Responder = (_init, url) => {
      const query = url.split('?')[1] ?? ''
      if (kind === 'chip' && query === QUERY_OF[chipName as (typeof CHIPS)[number]]) return held.promise
      return res(200, table[query])
    }
    openOrganizer({ list: responder })
    await waitIds([12, 14])
    expect(listUrls()).toEqual([P])

    if (kind === 'chip') {
      // When 표의 칩을 누른다
      await click(chip(chipName!))
      // Then LIST 가 표의 쿼리로 나간다
      await waitFor(() => expect(listUrls()).toEqual([P, `${P}?${QUERY_OF[chipName as (typeof CHIPS)[number]]}`]))
      // And 누른 칩만 aria-pressed=true 다
      expect(pressedChips()).toEqual([chipName])
      // And 누르는 순간 행 자리표시가 보이고 이전 행은 남지 않는다
      expect(skeletons()).toHaveLength(3)
      expect(rowLinks()).toHaveLength(0)
      // And 응답이 오면 행이 표의 items 의 공연으로 바뀐다
      held.resolve(res(200, orgBody(ITEMS_OF[chipName!])))
      await waitIds(idsOf(ITEMS_OF[chipName!]))
      // And 칩은 이 7개가 이 순서로 있다
      expect(chipNames()).toEqual([...CHIPS])
      expect(screen.getByRole('group', { name: '상태로 걸러 보기' })).toBeInTheDocument()
    } else if (kind === 'back-to-all') {
      await click(chip('매진'))
      await waitIds([16])
      await click(chip('전체'))
      // Then 두 번째 요청은 쿼리가 없다
      await waitFor(() => expect(listUrls()).toEqual([P, `${P}?filter=SOLD_OUT`, P]))
      await waitIds([12, 14])
    } else if (kind === 'same-all') {
      await click(chip('전체'))
      await pause()
      // Then LIST 가 더 나가지 않는다
      expect(listUrls()).toEqual([P])
    } else {
      await click(chip('매진'))
      await waitIds([16])
      await click(chip('매진'))
      await pause()
      // Then 두 번째 누름은 요청을 만들지 않는다(총 2번)
      expect(listUrls()).toEqual([P, `${P}?filter=SOLD_OUT`])
    }
  })

  // ---------- 빈 목록·필터 0건 (SC-15) ----------
  const SC15: { row: string; chip: (typeof CHIPS)[number]; body?: unknown }[] = [
    { row: 'a 전체 0건', chip: '전체' },
    { row: 'b 비공개', chip: '비공개' },
    { row: 'c 예정', chip: '예정' },
    { row: 'd 예매 가능', chip: '예매 가능' },
    { row: 'e 매진', chip: '매진' },
    { row: 'f 마감', chip: '마감' },
    { row: 'g 취소', chip: '취소' },
    { row: 'h 전체 · total 5 인데 items 빈 배열', chip: '전체', body: orgBody([], 5) },
    { row: 'i 전체 · total 0', chip: '전체', body: orgBody([], 0) },
  ]
  test.each(SC15)('SC-15 (boundary) 공연이 없으면 안내가 보이고, 필터 결과가 없으면 필터 이름이 담긴 안내가 보인다 — $row', async ({ chip: chipName, body }) => {
    // Given ORG 로그인 상태이고 표의 칩이 선택돼 LIST 가 표의 응답을 200 으로 준다
    const list: Record<string, unknown> = { '': body ?? (chipName === '전체' ? orgBody([]) : orgBody([O])) }
    if (chipName !== '전체') list[QUERY_OF[chipName]] = orgBody([])
    openOrganizer({ list })

    // When /organizer 를 열고 표의 칩을 누른다(전체 행은 누르지 않는다)
    if (chipName !== '전체') {
      await waitIds([12])
      await click(chip(chipName))
    }

    // Then 화면이 표와 같다
    if (chipName === '전체') {
      expect(await screen.findByText(EMPTY_TITLE)).toBeInTheDocument()
      expect(screen.getByText(EMPTY_SUB)).toBeInTheDocument()
      expect(screen.getByText(FIRST_REGISTER)).toBeInTheDocument()
      expect(heroTitles()).toContain('첫 공연을 등록해 보세요')
      // 칩 묶음과 「공연 등록」 떠 있는 버튼은 없다
      expect(chipGroup()).toBeNull()
      expect(fab()).toBeNull()
    } else {
      expect(await screen.findByText(`${chipName} 공연이 없어요`)).toBeInTheDocument()
      expect(seeAll()).not.toBeNull()
      // 칩 7개가 남고 선택돼 있다, 「공연 등록」 버튼이 있다, 「첫 공연 등록하기」는 없다
      expect(chipNames()).toEqual([...CHIPS])
      expect(pressedChips()).toEqual([chipName])
      expect(fab()).not.toBeNull()
      expect(screen.queryByText(FIRST_REGISTER)).toBeNull()
      expect(screen.queryByText(EMPTY_TITLE)).toBeNull()
    }
  })
})

// ---------- F10 이동 ----------

describe('TASK-025 F10 — 화면 간 이동', () => {
  type Row = { row: string; run: () => Promise<void> }
  const rowTo = (id: number, item: Item): Row => ({
    row: `${id} 행 선택`,
    run: async () => {
      openOrganizer({ list: { '': orgBody([item]) } })
      await waitIds([id])
      await click(rowOf(id))
      await waitFor(() => expect(where()).toBe(`/organizer/performances/${id}`))
    },
  })
  const allRow = (chipName: (typeof CHIPS)[number]): Row => ({
    row: `「${chipName}」 칩 0건 → 전체 보기`,
    run: async () => {
      openOrganizer({ list: { '': orgBody([O]), [QUERY_OF[chipName]]: orgBody([]) } })
      await waitIds([12])
      await click(chip(chipName))
      await screen.findByText(`${chipName} 공연이 없어요`)
      await click(seeAll()!)
      // 주소는 /organizer 그대로, LIST 가 쿼리 없이 나가고 「전체」가 선택된다
      await waitFor(() => expect(listUrls().slice(-1)).toEqual([P]))
      expect(where()).toBe('/organizer')
      await waitIds([12])
      expect(pressedChips()).toEqual(['전체'])
    },
  })
  const SC16: Row[] = [
    {
      row: 'a 공연 등록',
      run: async () => {
        openOrganizer({ list: { '': orgBody([O]) } })
        await waitIds([12])
        await click(fab()!)
        await waitFor(() => expect(where()).toBe('/performances/new'))
        expect(await screen.findByLabelText(/^공연명/)).toBeInTheDocument()
      },
    },
    {
      row: 'b 첫 공연 등록하기',
      run: async () => {
        openOrganizer({ list: { '': orgBody([]) } })
        await click(await screen.findByText(FIRST_REGISTER))
        await waitFor(() => expect(where()).toBe('/performances/new'))
        expect(await screen.findByLabelText(/^공연명/)).toBeInTheDocument()
      },
    },
    rowTo(11, D),
    rowTo(12, O),
    rowTo(13, C),
    rowTo(14, Z),
    rowTo(15, U),
    rowTo(16, S),
    allRow('비공개'),
    allRow('예정'),
    allRow('예매 가능'),
    allRow('매진'),
    allRow('마감'),
    allRow('취소'),
    {
      row: 'o 행은 링크다',
      run: async () => {
        const items = [D, O, C, Z, U, S]
        openOrganizer({ list: { '': orgBody(items) } })
        await waitIds(idsOf(items))
        for (const it of items) {
          const a = rowOf(it.id as number)
          expect(a.getAttribute('href')).toBe(`/organizer/performances/${it.id}`)
          expect(a.textContent ?? '').toContain(String(it.title))
        }
      },
    },
  ]
  test.each(SC16)('SC-16 (happy) 떠 있는 버튼·빈 화면 버튼·행·「전체 보기」가 각자의 목적지로 간다 — $row', async ({ run }) => {
    await run()
  })
})

// ---------- F9 더 보기 ----------

describe('TASK-025 F9 — 더 보기', () => {
  const P1 = `${P}?page=1`
  const P2 = `${P}?page=2`
  const first = many(1, 20)
  type Row = { row: string; run: () => Promise<void> }
  const SC17: Row[] = [
    {
      row: 'a 전체 20/25',
      run: async () => {
        openOrganizer({ list: { '': orgBody(first, 25), 'page=1': orgBody(many(21, 25), 25) } })
        await waitIds(idsOf(first))
        expect(moreButton()).not.toBeNull()
        await click(moreButton()!)
        // 요청이 ?page=1, 5건이 이어 붙어 행 25개, 「더 보기」가 사라진다
        await waitIds(idsOf(many(1, 25)))
        expect(listUrls()).toEqual([P, P1])
        expect(moreButton()).toBeNull()
      },
    },
    {
      row: 'b 매진 20/25',
      run: async () => {
        openOrganizer({ list: { '': orgBody([O]), 'filter=SOLD_OUT': orgBody(first, 25), 'filter=SOLD_OUT&page=1': orgBody(many(21, 25), 25) } })
        await waitIds([12])
        await click(chip('매진'))
        await waitIds(idsOf(first))
        await click(moreButton()!)
        await waitIds(idsOf(many(1, 25)))
        expect(listUrls().slice(-1)).toEqual([`${P}?filter=SOLD_OUT&page=1`])
      },
    },
    {
      row: 'c total 20',
      run: async () => {
        openOrganizer({ list: { '': orgBody(first, 20) } })
        await waitIds(idsOf(first))
        await pause()
        expect(moreButton()).toBeNull()
      },
    },
    {
      row: 'd total 3',
      run: async () => {
        openOrganizer({ list: { '': orgBody(many(1, 3), 3) } })
        await waitIds(idsOf(many(1, 3)))
        await pause()
        expect(moreButton()).toBeNull()
      },
    },
    {
      row: 'e total 45 두 번',
      run: async () => {
        openOrganizer({ list: { '': orgBody(first, 45), 'page=1': orgBody(many(21, 40), 45), 'page=2': orgBody(many(41, 45), 45) } })
        await waitIds(idsOf(first))
        await click(moreButton()!)
        await waitIds(idsOf(many(1, 40)))
        await click(moreButton()!)
        await waitIds(idsOf(many(1, 45)))
        expect(listUrls()).toEqual([P, P1, P2])
      },
    },
    {
      row: 'f 같은 id 2건 포함',
      run: async () => {
        // 둘째 쪽 5건 중 앞 쪽과 같은 id(1019·1020) 2건
        openOrganizer({ list: { '': orgBody(first, 25), 'page=1': orgBody(many(19, 23), 25) } })
        await waitIds(idsOf(first))
        await click(moreButton()!)
        // 행은 23개다(같은 id 는 한 번만), 「더 보기」는 사라진다
        await waitIds(idsOf(many(1, 23)))
        await pause()
        expect(moreButton()).toBeNull()
      },
    },
    {
      row: 'g 중복 뒤 다음 쪽 번호',
      run: async () => {
        openOrganizer({ list: { '': orgBody(first, 45), 'page=1': orgBody(many(19, 23), 45), 'page=2': orgBody(many(41, 45), 45) } })
        await waitIds(idsOf(first))
        await click(moreButton()!)
        await waitIds(idsOf(many(1, 23)))
        await click(moreButton()!)
        // 다음 요청은 ?page=2 다(불러온 개수가 아니라 성공한 횟수로 센다)
        await waitFor(() => expect(listUrls()).toEqual([P, P1, P2]))
      },
    },
    {
      row: 'h 연달아 두 번',
      run: async () => {
        const held = deferred()
        openOrganizer({ list: (_i, url) => (url.endsWith('page=1') ? held.promise : res(200, orgBody(first, 25))) })
        await waitIds(idsOf(first))
        const button = moreButton()!
        await click(button)
        await click(button)
        await pause()
        // ?page=1 요청은 1번뿐이다, 응답을 기다리는 동안 버튼은 눌리지 않는다
        expect(listUrls().filter((u) => u === P1)).toHaveLength(1)
        expect(moreButton()?.hasAttribute('disabled') ?? true).toBe(true)
        held.resolve(res(200, orgBody(many(21, 25), 25)))
        await waitIds(idsOf(many(1, 25)))
      },
    },
    {
      row: 'i 빈 쪽',
      run: async () => {
        openOrganizer({ list: { '': orgBody(first, 30), 'page=1': orgBody([], 30) } })
        await waitIds(idsOf(first))
        await click(moreButton()!)
        await waitFor(() => expect(moreButton()).toBeNull())
        await pause()
        expect(listUrls()).toEqual([P, P1])
      },
    },
    {
      row: 'j 빈 쪽 total 0',
      run: async () => {
        openOrganizer({ list: { '': orgBody(first, 25), 'page=1': orgBody([], 0) } })
        await waitIds(idsOf(first))
        await click(moreButton()!)
        await waitFor(() => expect(moreButton()).toBeNull())
        await pause()
        expect(listUrls()).toEqual([P, P1])
      },
    },
    {
      row: 'k 둘째 쪽에 비공개',
      run: async () => {
        openOrganizer({ list: { '': orgBody(first, 25), 'page=1': orgBody([D, ...many(21, 24)], 25) } })
        await waitIds(idsOf(first))
        expect(heroTitles()).toContain('내 공연')
        await click(moreButton()!)
        await waitFor(() => expect(heroTitles()).toContain('비공개 공연이 1건 있어요'))
      },
    },
    {
      row: 'l 칩을 바꾸면 쪽 번호를 처음부터 센다',
      run: async () => {
        openOrganizer({
          list: {
            '': orgBody(first, 25),
            'page=1': orgBody(many(21, 25), 25),
            'filter=SOLD_OUT': orgBody(many(31, 50), 25),
            'filter=SOLD_OUT&page=1': orgBody(many(51, 55), 25),
          },
        })
        await waitIds(idsOf(first))
        await click(moreButton()!)
        await waitIds(idsOf(many(1, 25)))
        await click(chip('매진'))
        await waitIds(idsOf(many(31, 50)))
        await click(moreButton()!)
        await waitIds(idsOf([...many(31, 50), ...many(51, 55)]))
        expect(listUrls().slice(-2)).toEqual([`${P}?filter=SOLD_OUT`, `${P}?filter=SOLD_OUT&page=1`])
      },
    },
    {
      row: 'm 필터 사이 행이 섞이지 않는다',
      run: async () => {
        openOrganizer({ list: { '': orgBody([O, S]), 'filter=OPEN': orgBody([O, Z]) } })
        await waitIds([12, 16])
        await click(chip('예매 가능'))
        await waitIds([12, 14])
      },
    },
    {
      row: 'n items 20 > total 5',
      run: async () => {
        openOrganizer({ list: { '': orgBody(first, 5) } })
        await waitIds(idsOf(first))
        await pause()
        expect(moreButton()).toBeNull()
      },
    },
  ]
  test.each(SC17)('SC-17 (happy) 「더 보기」는 20건씩 이어 붙이고, 불러온 개수가 아니라 성공 횟수로 다음 쪽을 센다 — $row', async ({ run }) => {
    await run()
  })

  const INF = '__INF__'
  const BIGNUM = Number.MAX_SAFE_INTEGER + 2
  const SC18: { row: string; resp: () => Response; then: 'fail' | 'retry' | 'login' | 'notice' }[] = [
    { row: 'a 500', resp: () => res(500, { code: 'INTERNAL' }), then: 'fail' },
    { row: 'b 네트워크', resp: () => networkError(), then: 'fail' },
    { row: 'c 비 JSON', resp: () => raw(200, '<html>ok</html>'), then: 'fail' },
    { row: 'd 빈 본문', resp: () => res(200), then: 'fail' },
    { row: "e totalElements '30'", resp: () => res(200, orgBody(many(21, 25), '30')), then: 'fail' },
    { row: 'f totalElements -1', resp: () => res(200, orgBody(many(21, 25), -1)), then: 'fail' },
    { row: 'g totalElements 1.5', resp: () => res(200, orgBody(many(21, 25), 1.5)), then: 'fail' },
    { row: 'h totalElements 1e400', resp: () => new Response(jsonWithInfinity(orgBody(many(21, 25), INF)), { status: 200 }), then: 'fail' },
    { row: 'i totalElements 2^53+1', resp: () => res(200, orgBody(many(21, 25), BIGNUM)), then: 'fail' },
    { row: 'j totalElements 없음', resp: () => res(200, { ...orgBody(many(21, 25)), totalElements: undefined }), then: 'fail' },
    { row: 'k items [null]', resp: () => res(200, orgBody([null], 45)), then: 'fail' },
    { row: "l title ''", resp: () => res(200, orgBody([patched(many(21, 21)[0], { title: '' })], 45)), then: 'fail' },
    { row: 'm items {}', resp: () => res(200, { ...orgBody([]), items: {} }), then: 'fail' },
    { row: 'n 다시 시도', resp: () => res(500, { code: 'INTERNAL' }), then: 'retry' },
    { row: 'o 401', resp: () => res(401, { code: 'UNAUTHORIZED' }), then: 'login' },
    { row: 'p 403', resp: () => res(403, { code: 'FORBIDDEN_ROLE' }), then: 'notice' },
  ]
  test.each(SC18)('SC-18 (error) 더 보기가 실패하거나 계약을 어기면 앞 목록은 지키고 다시 시도할 수 있다 — $row', async ({ resp, then }) => {
    // Given ORG 로그인 상태이고 첫 쪽 20건(totalElements 45)이 보인다
    let hits = 0
    const responder: Responder = (_i, url) => {
      if (!url.endsWith('page=1')) return res(200, orgBody(first, 45))
      hits += 1
      return then === 'retry' && hits > 1 ? res(200, orgBody(many(21, 25), 45)) : resp()
    }
    openOrganizer({ list: responder })
    await waitIds(idsOf(first))

    // When 「더 보기」를 누르고 다음 쪽 응답으로 표의 것이 온다
    await click(moreButton()!)

    if (then === 'login') {
      await waitFor(() => expect(where()).toBe('/login'))
      expect(fromState()).toBe('/organizer')
      return
    }
    if (then === 'notice') {
      expect(await screen.findByText(ONLY_TITLE)).toBeInTheDocument()
      return
    }
    // Then 「불러오지 못했어요 · 다시 시도」가 보이고 앞의 20행은 그대로 남는다
    await waitFor(() => expect(moreFailButton()).not.toBeNull())
    expect(rowIds()).toEqual(idsOf(first))
    // And 목록 전체 오류 화면으로 바뀌지 않는다
    expect(errorShown()).toBe(false)
    // And LIST 는 ?page=1 로 한 번 나갔다
    expect(listUrls()).toEqual([P, P1])

    if (then === 'retry') {
      // When 「불러오지 못했어요 · 다시 시도」를 누른다 → Then 같은 ?page=1 로 한 번 더 나가고 행이 이어 붙는다
      await click(moreFailButton()!)
      await waitIds(idsOf(many(1, 25)))
      expect(listUrls()).toEqual([P, P1, P1])
    }
  })
})

// ---------- 응답이 오는 사이 (SC-19) ----------

describe('TASK-025 F5·F6·F9 — 늦은 응답', () => {
  type Late = 'ok' | '401' | '403' | '500' | 'network' | 'non-json'
  type Settler = { resolve: (r: Response) => void; reject: (e: unknown) => void }
  const lateOf = (late: Late, held: Settler, items: Item[] = [D]) => {
    if (late === 'ok') held.resolve(res(200, orgBody(items)))
    else if (late === '401') held.resolve(res(401, { code: 'UNAUTHORIZED' }))
    else if (late === '403') held.resolve(res(403, { code: 'FORBIDDEN_ROLE' }))
    else if (late === '500') held.resolve(res(500, { code: 'INTERNAL' }))
    else if (late === 'network') held.reject(new TypeError('Failed to fetch'))
    else held.resolve(raw(200, '<html>ok</html>'))
  }
  const first = many(1, 20)
  const stale = (late: Late): { row: string; run: () => Promise<void> } => ({
    row: `칩을 눌러 이전 요청이 낡았다 · ${late}`,
    run: async () => {
      const held = deferred()
      let n = 0
      openOrganizer({ list: (_i, url) => ((url.split('?')[1] ?? '') === '' && (n += 1) === 1 ? held.promise : res(200, orgBody([S]))) })
      await waitFor(() => expect(skeletons()).toHaveLength(3))
      await click(chip('매진'))
      await waitIds([16])
      lateOf(late, held)
      await pause(60)
      // 현재 칩의 결과가 정상이고 로그아웃·이동·오류가 없다
      expect(rowIds()).toEqual([16])
      expect(pressedChips()).toEqual(['매진'])
      expect(where()).toBe('/organizer')
      expect(token()).toBe('tok-org')
      expect(errorShown()).toBe(false)
      expect(screen.queryByText(ONLY_TITLE)).toBeNull()
    },
  })
  const SC19: { row: string; run: () => Promise<void> }[] = [
    {
      row: 'a 첫 응답 전 칩 변경',
      run: async () => {
        const held = deferred()
        openOrganizer({ list: (_i, url) => ((url.split('?')[1] ?? '') === '' ? held.promise : res(200, orgBody([S]))) })
        await waitFor(() => expect(skeletons()).toHaveLength(3))
        await click(chip('매진'))
        await waitIds([16])
        held.resolve(res(200, orgBody([D])))
        await pause(60)
        expect(rowIds()).toEqual([16])
        expect(pressedChips()).toEqual(['매진'])
      },
    },
    {
      row: 'b 첫 응답 전 화면 이동',
      run: async () => {
        const held = deferred()
        openOrganizer({ list: () => held.promise })
        await waitFor(() => expect(skeletons()).toHaveLength(3))
        await navigateTo('/')
        held.resolve(res(200, orgBody([O])))
        await pause(60)
        expect(where()).toBe('/')
        expect(rowLinks().filter((a) => a.getAttribute('href')?.startsWith('/organizer'))).toHaveLength(0)
        expect(errorShown()).toBe(false)
      },
    },
    {
      row: 'c 첫 응답 전 로그아웃',
      run: async () => {
        const held = deferred()
        openOrganizer({ list: () => held.promise })
        await waitFor(() => expect(skeletons()).toHaveLength(3))
        await logoutNow()
        held.resolve(res(200, orgBody([O])))
        await pause(60)
        expect(where()).toBe('/login')
        expect(rowLinks()).toHaveLength(0)
      },
    },
    stale('401'),
    stale('403'),
    stale('500'),
    stale('network'),
    stale('non-json'),
    {
      row: 'i 더 보기 응답 전 칩 변경',
      run: async () => {
        const held = deferred()
        openOrganizer({ list: (_i, url) => (url.endsWith('page=1') ? held.promise : url.includes('SOLD_OUT') ? res(200, orgBody([S])) : res(200, orgBody(first, 45))) })
        await waitIds(idsOf(first))
        await click(moreButton()!)
        await click(chip('매진'))
        await waitIds([16])
        held.resolve(res(200, orgBody(many(21, 25), 45)))
        await pause(60)
        // 새 칩의 목록에 붙지 않는다
        expect(rowIds()).toEqual([16])
      },
    },
    {
      row: 'j 더 보기 응답 전 화면 이동',
      run: async () => {
        const held = deferred()
        openOrganizer({ list: (_i, url) => (url.endsWith('page=1') ? held.promise : res(200, orgBody(first, 45))) })
        await waitIds(idsOf(first))
        await click(moreButton()!)
        await navigateTo('/')
        held.resolve(res(200, orgBody(many(21, 25), 45)))
        await pause(60)
        expect(where()).toBe('/')
        expect(errorShown()).toBe(false)
      },
    },
    {
      row: 'k 더 보기 응답 전 칩 변경 · 401',
      run: async () => {
        const held = deferred()
        openOrganizer({ list: (_i, url) => (url.endsWith('page=1') ? held.promise : url.includes('SOLD_OUT') ? res(200, orgBody([S])) : res(200, orgBody(first, 45))) })
        await waitIds(idsOf(first))
        await click(moreButton()!)
        await click(chip('매진'))
        await waitIds([16])
        lateOf('401', held)
        await pause(60)
        expect(where()).toBe('/organizer')
        expect(token()).toBe('tok-org')
      },
    },
    {
      row: 'l 더 보기 응답 전 칩 변경 · 500',
      run: async () => {
        const held = deferred()
        openOrganizer({ list: (_i, url) => (url.endsWith('page=1') ? held.promise : url.includes('SOLD_OUT') ? res(200, orgBody([S])) : res(200, orgBody(first, 45))) })
        await waitIds(idsOf(first))
        await click(moreButton()!)
        await click(chip('매진'))
        await waitIds([16])
        lateOf('500', held)
        await pause(60)
        expect(moreFailButton()).toBeNull()
        expect(rowIds()).toEqual([16])
      },
    },
    {
      row: 'm 다시 시도 응답 전 다른 칩',
      run: async () => {
        const held = deferred()
        let n = 0
        openOrganizer({
          list: (_i, url) => {
            if (url.includes('SOLD_OUT')) return res(200, orgBody([S]))
            n += 1
            return n === 1 ? res(500, { code: 'INTERNAL' }) : held.promise
          },
        })
        await screen.findByText(LIST_ERROR)
        await click(retryButton()!)
        await click(chip('매진'))
        await waitIds([16])
        held.resolve(res(200, orgBody([O])))
        await pause(60)
        expect(rowIds()).toEqual([16])
      },
    },
    {
      row: 'n StrictMode 이중 마운트',
      run: async () => {
        openOrganizer({ list: { '': orgBody([O]) } }, '/organizer', true)
        await waitIds([12])
        await pause(60)
        expect(rowIds()).toEqual([12])
        expect(errorShown()).toBe(false)
      },
    },
    {
      row: 'o 로그아웃 뒤 다시 로그인',
      run: async () => {
        const held = deferred()
        let n = 0
        openOrganizer({ list: () => ((n += 1) === 1 ? held.promise : res(200, orgBody([Z]))) })
        await waitFor(() => expect(skeletons()).toHaveLength(3))
        await logoutNow()
        await loginNow(ORG)
        await navigateTo('/organizer')
        held.resolve(res(200, orgBody([D])))
        await waitIds([14])
        await pause(60)
        // 다시 연 화면에서 새로 부른 결과만 보인다
        expect(rowIds()).toEqual([14])
      },
    },
  ]
  test.each(SC19)('SC-19 (boundary) 응답이 오는 사이 칩·화면·세션이 바뀌면 늦은 응답은 현재 상태를 덮어쓰지 않는다 — $row', async ({ run }) => {
    await run()
  })
})

// ---------- SC-23 렌더 결과 ----------

describe('TASK-025 F5·F10 — 렌더된 모양', () => {
  const rowsBody = orgBody([D, O, Z])
  type Row = { row: string; run: () => Promise<void> }
  const open = async () => {
    openOrganizer({ list: { '': rowsBody } })
    await waitIds([11, 12, 14])
  }
  const style = (row: string, prop: string, el: () => Element, expected: (v: string) => void): Row => ({
    row,
    run: async () => {
      await open()
      expected(css(el(), prop))
    },
  })
  const SC23: Row[] = [
    {
      row: 'a 비공개 배지 색',
      run: async () => {
        await open()
        const badge = leaf(rowOf(11), '비공개')
        expect(isTokenColor(css(badge, 'background-color'), '--c-draft-tint')).toBe(true)
        expect(isTokenColor(css(badge, 'color'), '--c-draft-tint-text')).toBe(true)
      },
    },
    style('b 비공개 배지 flex-shrink', 'flex-shrink', () => leaf(rowOf(11), '비공개'), (v) => expect(v).toBe('0')),
    {
      row: 'c 마감 배지 배경',
      run: async () => {
        await open()
        const bg = css(leaf(rowOf(14), '마감'), 'background-color')
        // Badge 의 중립 배지는 var(--c-fill) 을 그대로 쓴다(jsdom 은 var 를 해석하지 않는다)
        expect(bg).toBe('var(--c-fill)')
        expect(bg).not.toBe(tokenColor('--c-draft-tint'))
      },
    },
    {
      row: 'd 예매 가능 배지 색',
      run: async () => {
        await open()
        const badge = leaf(rowOf(12), '예매 가능')
        expect(isTokenColor(css(badge, 'background-color'), '--c-brand-tint')).toBe(true)
        expect(isTokenColor(css(badge, 'color'), '--c-brand-tint-text')).toBe(true)
      },
    },
    style('e 버튼 position', 'position', () => fab()!, (v) => expect(v).toBe('fixed')),
    {
      row: 'f 버튼 z-index·height·min-width',
      run: async () => {
        await open()
        expect(css(fab()!, 'z-index')).toBe('6')
        expect(css(fab()!, 'height')).toBe('52px')
        expect(css(fab()!, 'min-width')).toBe('52px')
      },
    },
    style('g 버튼 border-radius', 'border-radius', () => fab()!, (v) => expect(v).toBe(tokenValue('borderRadius', '--r-pill'))),
    {
      row: 'h 버튼 색',
      run: async () => {
        await open()
        expect(isTokenColor(css(fab()!, 'background-color'), '--c-brand')).toBe(true)
        expect(isTokenColor(css(fab()!, 'color'), '--c-on-brand')).toBe(true)
      },
    },
    style('i 버튼 pointer-events', 'pointer-events', () => fab()!, (v) => expect(v).not.toBe('none')),
    style('j 버튼 bottom', 'bottom', () => fab()!, (v) => expect(v).toContain('env(safe-area-inset-bottom)')),
    style('k 버튼 right', 'right', () => fab()!, (v) => expect(v).toBe(themeToken('--page-x'))),
    style('l 칩 flex', 'flex', () => chip('비공개'), (v) => expect(v).toBe('0 0 auto')),
    style('l2 칩 min-height', 'min-height', () => chip('비공개'), (v) => expect(v).toBe('44px')),
    style('m 칩 묶음 overflow-x', 'overflow-x', () => chipGroup()!, (v) => expect(v).toBe('auto')),
    {
      row: 'n 선택된 칩 색',
      run: async () => {
        await open()
        expect(isTokenColor(css(chip('전체'), 'background-color'), '--c-text-1')).toBe(true)
        expect(isTokenColor(css(chip('전체'), 'color'), '--c-surface')).toBe(true)
      },
    },
    {
      row: 'o 썸네일 flex',
      run: async () => {
        await open()
        const row = rowOf(12)
        let el: Element | null = row.querySelector('img[src^="https://"]')
        let found = false
        while (el && el !== row) {
          if (css(el, 'flex') === '0 0 auto') found = true
          el = el.parentElement
        }
        expect(found).toBe(true)
      },
    },
    {
      row: 'p 행 본문 min-width',
      run: async () => {
        await open()
        const row = rowOf(12)
        let el: Element | null = leaf(row, '소리풍경 밴드 나이트')
        let found = false
        while (el && el !== row) {
          if (css(el, 'min-width') === '0px') found = true
          el = el.parentElement
        }
        expect(found).toBe(true)
      },
    },
    {
      row: 'q 홈 상태 칩 회귀',
      run: async () => {
        serve({ user: null, routes: { [HOME_LIST]: (_i, url) => res(200, { content: url.includes('UPCOMING') ? [L3] : [L1, L3], page: 0, size: 20, totalElements: 2 }) } })
        renderApp('/')
        const group = await screen.findByRole('group', { name: '상태로 걸러 보기' })
        expect(within(group).getAllByRole('button').map((b) => b.textContent)).toEqual(['전체', '예정', '예매 가능', '매진', '마감', '취소'])
        await click(within(group).getByRole('button', { name: '예정' }))
        await waitFor(() => expect(callsTo(HOME_LIST).map((c) => c.url)).toContain('/api/performances?status=UPCOMING'))
      },
    },
    {
      row: 'r 홈 상태 배지 회귀',
      run: async () => {
        serve({ user: null, routes: { [HOME_LIST]: () => res(200, { content: [L1, L2, L3, L5, L7], page: 0, size: 20, totalElements: 5 }) } })
        renderApp('/')
        await waitFor(() => expect(cardLinks()).toHaveLength(5))
        expect(inCard(String(L1.title)).getByText('예매 가능')).toBeInTheDocument()
        expect(inCard(String(L2.title)).getByText('마감')).toBeInTheDocument()
        expect(inCard(String(L3.title)).getByText('예정')).toBeInTheDocument()
        expect(inCard(String(L5.title)).getByText('매진')).toBeInTheDocument()
        expect(inCard(String(L7.title)).getByText('공연 취소')).toBeInTheDocument()
      },
    },
  ]
  test.each(SC23)('SC-23 (regression) 새 부품의 모양과 기존 칩·배지가 렌더 결과로 시안과 같다 — $row', async ({ run }) => {
    await run()
  })
})

