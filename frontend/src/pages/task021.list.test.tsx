import { act, fireEvent, screen } from '@testing-library/react'
import {
  CHIPS,
  L1,
  L2,
  L3,
  L4,
  L5,
  L6,
  L7,
  LIST_ERROR,
  LIST_FAILS,
  LIST_KEY,
  MORE_FAIL,
  NEXT_KEY,
  RETRY,
  SIAN,
  TIME,
  TITLE_L4,
  cardLinks,
  cardTitles,
  chip,
  click,
  cvListRows,
  deferred,
  eyebrow,
  flush,
  heroSkeleton,
  heroText,
  jsonWithInfinityResponse,
  listBody,
  loginNow,
  logoutNow,
  navigateTo,
  numbered,
  openHome,
  pathAndQuery,
  pressed,
  queryOf,
  range,
  res,
  skeletons,
  startHome,
  timeRoute,
  urlsOf,
  within,
} from '../test/task021'
import type { Item, Responder } from '../test/task021'
import { calls, css, resetDocumentVisibility, squash, where } from '../test/task020'

// TASK-021 시나리오 — 홈 목록(칩·더 보기·빈 목록·로딩·오류·계약 위반·비동기). SoT: workflow_design/05_scenario/SCENARIO_TASK-021.md
// 표로 묶은 시나리오는 test.each 한 줄(= 시나리오 한 건)이고 행마다 케이스가 하나씩 돈다.

const HERO_L4 = `오늘 오후 6:00에 ${TITLE_L4} 예매가 열려요`
const GENERAL = '다가오는 공연을 골라 보세요'
const MORE = '더 보기'

beforeEach(() => {
  localStorage.clear()
})
afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  resetDocumentVisibility()
  localStorage.clear()
})

const moreButton = () => screen.queryByRole('button', { name: MORE })
const titlesAre = (items: Item[]) => items.forEach((item, i) => expect(cardTitles()[i]).toContain(String(item.title)))
const lastUrl = () => pathAndQuery(urlsOf(calls, LIST_KEY).at(-1)!)
const noMoreFail = () => expect(screen.queryByText(MORE_FAIL)).not.toBeInTheDocument()

/** 칩별 응답 — 홈 기본(전체)과 같은 시안 목록에서 상태가 맞는 공연만 */
const BY_CHIP: Record<string, Item[]> = { 예정: [L3, L4, L6], '예매 가능': [L1], 매진: [L5], 마감: [L2], 취소: [L7] }
const chipList = () => ({ '': listBody(SIAN), ...Object.fromEntries(Object.entries(BY_CHIP).map(([name, items]) => [queryOf(name), listBody(items)])) })
/** 20개 + 5개 목록 — 전체·칩별 */
const paged = (query = '') => ({
  [query]: listBody(range(1, 20), 25),
  [query ? `${query}&page=1` : 'page=1']: listBody(range(21, 25), 25),
})

describe('TASK-021 — 홈 목록 (칩·더 보기·상태별 화면)', () => {
  const CHIP_ROWS: Array<[string, string]> = [['a', '예정'], ['b', '예매 가능'], ['c', '매진'], ['d', '마감'], ['e', '취소'], ['f', '전체'], ['g', '예정']]
  const CV_CHIP_ROWS = cvListRows([L3]).map(([name, body]) => ['cv ' + name, body] as [string, unknown])
  test.each<[string, unknown]>([...CHIP_ROWS, ...CV_CHIP_ROWS])('SC-13 (happy) 칩을 누르면 그 상태로 첫 페이지를 다시 불러오고 누른 칩만 눌린 상태다 — 행 %s', async (row, target) => {
    // Given 홈이 시안 목록으로 보인다. 목록 스텁은 status 쿼리별 응답을 돌려준다 (행 f 는 「예정」을 누른 뒤의 「전체」, 행 g 는 같은 칩 다시 누르기)
    const isCv = row.startsWith('cv ')
    const name = isCv ? '예정' : (target as string)
    const gate = deferred<Response>()
    let allCalls = 0
    const list: Record<string, Responder> = {
      '': () => (name !== '전체' || ++allCalls === 1 ? res(200, listBody(SIAN)) : gate.promise),
      ...Object.fromEntries(Object.entries(BY_CHIP).map(([n, items]) => [queryOf(n), (): Response | Promise<Response> => (row === 'f' || n !== name ? res(200, listBody(items)) : gate.promise)])),
    }
    await openHome({ list })
    if (row === 'f') {
      await click(chip('예정'))
      expect(pressed()).toEqual(['예정'])
    }
    // When 표의 칩을 누른다
    await click(chip(name))
    // Then 누른 칩만 aria-pressed="true" 이고 응답 전에는 카드 자리 4개가 보이고 이전 카드는 없다
    expect(pressed()).toEqual([name])
    expect(skeletons()).toHaveLength(4)
    expect(cardLinks()).toHaveLength(0)
    // And 응답 뒤에는 그 응답의 카드만 있다
    await act(async () => gate.resolve(isCv ? jsonWithInfinityResponse(target) : res(200, listBody(name === '전체' ? SIAN : BY_CHIP[name]))))
    await flush()
    if (isCv) {
      // And 칩 응답이 CV 표를 어기면 오류 화면이고 누른 칩은 눌린 채이며 이전 칩의 카드는 없다
      expect(screen.getByText(LIST_ERROR)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: RETRY })).toBeInTheDocument()
      expect(pressed()).toEqual([name])
      expect(cardLinks()).toHaveLength(0)
      return
    }
    const query = queryOf(name)
    expect(lastUrl()).toBe(query ? `/api/performances?${query}` : '/api/performances')
    const expected = name === '전체' ? SIAN : BY_CHIP[name]
    expect(cardTitles()).toHaveLength(expected.length)
    titlesAre(expected)
    // And 후보 호출 수·시각 호출 수는 늘지 않는다(머리글은 다시 부르지 않는다)
    expect(urlsOf(calls, NEXT_KEY)).toHaveLength(1)
    expect(urlsOf(calls, TIME)).toHaveLength(3)
    // And 선택된 칩을 다시 누르면 목록을 다시 부르지 않는다
    const count = urlsOf(calls, LIST_KEY).length
    await click(chip(name))
    expect(urlsOf(calls, LIST_KEY)).toHaveLength(count)
  })

  const RACES: Array<[string, string]> = [
    ['a', '늦게 온 이전 칩의 성공 응답은 버려진다'],
    ['b', '늦게 온 이전 칩의 실패 응답은 오류 화면을 띄우지 않는다'],
    ...LIST_FAILS.map(([k]) => [`c-${k}`, '칩 응답 실패 뒤 다시 시도는 같은 status 를 다시 부른다'] as [string, string]),
    ['d', '응답 전에 다른 화면으로 가면 상태가 갱신되지 않는다'],
    ['e', '응답 전에 로그인해도 응답이 반영되고 다시 부르지 않는다'],
  ]
  test.each(RACES)('SC-14 (boundary) 칩을 바꾸거나 화면을 떠나기 전에 오던 응답은 버려진다 — 행 %s (%s)', async (row) => {
    // Given 홈이 시안 목록으로 보인다. 「예정」·「마감」 칩의 응답은 테스트가 풀어 줄 때까지 붙들려 있다
    const up = deferred<Response>()
    const closed = deferred<Response>()
    let closedResponder: Responder = () => closed.promise
    const errors = vi.spyOn(console, 'error').mockImplementation(() => {})
    await openHome({ list: { '': listBody(SIAN), [queryOf('예정')]: () => up.promise, [queryOf('마감')]: (init: RequestInit, url: string) => closedResponder(init, url) } })
    const okUp = res(200, listBody(BY_CHIP['예정']))
    const okClosed = () => res(200, listBody(BY_CHIP['마감']))
    // When 표의 순서로 칩을 누르고 응답을 푼다
    if (row === 'a' || row === 'b') {
      await click(chip('예정'))
      await click(chip('마감'))
      await act(async () => closed.resolve(okClosed()))
      await flush()
      await act(async () => (row === 'a' ? up.resolve(okUp) : up.resolve(res(500, { code: 'INTERNAL' }))))
      await flush()
      // Then 「마감」 칩만 눌려 있고 카드는 「마감」 응답의 것이다 — 늦게 온 응답은 버려진다(오류 화면도 없다)
      expect(pressed()).toEqual(['마감'])
      expect(cardTitles()).toHaveLength(1)
      titlesAre(BY_CHIP['마감'])
      expect(screen.queryByText(LIST_ERROR)).not.toBeInTheDocument()
    } else if (row.startsWith('c-')) {
      const fail = LIST_FAILS.find(([k]) => k === row.slice(2))![1]
      up.resolve(okUp)
      await click(chip('예정'))
      closedResponder = fail
      await click(chip('마감'))
      // Then 「공연 목록을 불러오지 못했어요」+「다시 시도」, 「마감」 칩이 눌려 있고 「예정」 카드는 없다
      expect(screen.getByText(LIST_ERROR)).toBeInTheDocument()
      expect(pressed()).toEqual(['마감'])
      expect(cardLinks()).toHaveLength(0)
      // And 「다시 시도」는 ?status=CLOSED 를 다시 부른다
      closedResponder = okClosed
      await click(screen.getByRole('button', { name: RETRY }))
      expect(lastUrl()).toBe('/api/performances?status=CLOSED')
      titlesAre(BY_CHIP['마감'])
    } else if (row === 'd') {
      await click(chip('예정'))
      await navigateTo('/login')
      await act(async () => up.resolve(okUp))
      await flush()
      // Then 상태 갱신 경고(콘솔 오류) 0건, /login 화면이 그대로다
      expect(errors).not.toHaveBeenCalled()
      expect(where()).toBe('/login')
    } else {
      await click(chip('예정'))
      const count = urlsOf(calls, LIST_KEY).length
      await loginNow()
      await act(async () => up.resolve(okUp))
      await flush()
      // Then 「예정」 카드가 보이고 목록 호출 수는 그대로다(세션이 바뀌어도 다시 부르지 않는다)
      titlesAre(BY_CHIP['예정'])
      expect(urlsOf(calls, LIST_KEY)).toHaveLength(count)
    }
  })

  test.each<[string, string | null]>([['전체', null], ['예정', 'UPCOMING'], ['예매 가능', 'OPEN'], ['매진', 'SOLD_OUT'], ['마감', 'CLOSED'], ['취소', 'CANCELLED']])(
    'SC-15 (happy) 「더 보기」를 누르면 다음 20개가 아래에 붙고 다 불러오면 버튼이 없어진다 — 칩 %s',
    async (name) => {
      // Given 목록 첫 응답이 「공연 1」~「공연 20」·totalElements 25, ?page=1 응답이 「공연 21」~「공연 25」다
      const query = queryOf(name)
      await openHome({ list: { ...chipList(), ...paged(query) } })
      // And 홈이 열려 있다 — 카드 20개와 「더 보기」가 보인다 (칩 행은 그 칩을 누른 뒤)
      if (query) await click(chip(name))
      expect(cardLinks()).toHaveLength(20)
      expect(moreButton()).toBeInTheDocument()
      // When 「더 보기」를 누른다
      await click(moreButton()!)
      // Then 카드는 25개이며 순서는 「공연 1」~「공연 25」다(앞 20개는 그대로)
      expect(cardLinks()).toHaveLength(25)
      cardLinks().forEach((card, i) => expect(within(card).getByText(`공연 ${i + 1}`)).toBeInTheDocument())
      // And 「더 보기」는 없다. 목록 호출 수는 (칩 행은 칩을 누른 호출을 더해) 2 이고 두 번째 요청 URL 은 ?page=1 이다
      expect(moreButton()).not.toBeInTheDocument()
      const lists = urlsOf(calls, LIST_KEY).map(pathAndQuery)
      expect(lists.at(-1)).toBe(query ? `/api/performances?${query}&page=1` : '/api/performances?page=1')
      expect(lists.at(-2)).toBe(query ? `/api/performances?${query}` : '/api/performances')
      expect(lists).toHaveLength(query ? 3 : 2)
    },
  )

  type Spec = [string, { first: Item[]; total: number; page1?: Item[]; page2?: Item[] }, { more: boolean; afterClick?: { cards: number; more: boolean }; text?: string }]
  const MORE_ROWS: Spec[] = [
    ['a', { first: range(1, 20), total: 20 }, { more: false }],
    ['b', { first: range(1, 20), total: 21 }, { more: true }],
    ['c', { first: range(1, 20), total: 19 }, { more: false }],
    ['d', { first: range(1, 21), total: 21 }, { more: false }],
    ['e', { first: range(1, 20), total: 40, page1: range(21, 40) }, { more: true, afterClick: { cards: 40, more: false } }],
    ['f', { first: range(1, 20), total: 25, page1: [] }, { more: true, afterClick: { cards: 20, more: false } }],
    ['g', { first: [], total: 0 }, { more: false, text: '다가오는 공연이 없어요' }],
    ['h', { first: range(1, 20), total: 41, page1: range(21, 41) }, { more: true, afterClick: { cards: 41, more: false } }],
  ]
  test.each(MORE_ROWS)('SC-16 (boundary) 「더 보기」는 불러온 개수가 전체보다 적을 때만 보인다 — 행 %s', async (_row, spec, expected) => {
    // Given 목록 첫 응답이 표의 content·totalElements 다
    const list: Record<string, unknown> = { '': listBody(spec.first, spec.total) }
    if (spec.page1) list['page=1'] = listBody(spec.page1, spec.total)
    // When 홈을 연다 — 행 e·f·h 는 이어서 「더 보기」를 한 번 누른다
    await openHome({ list })
    // Then 「더 보기」 표시는 표와 같다
    expect(Boolean(moreButton())).toBe(expected.more)
    expect(cardLinks()).toHaveLength(spec.first.length)
    if (expected.text) expect(screen.getByText(expected.text)).toBeInTheDocument()
    if (expected.afterClick) {
      await click(moreButton()!)
      // (행 e·f·h 는 누른 뒤)
      expect(cardLinks()).toHaveLength(expected.afterClick.cards)
      expect(Boolean(moreButton())).toBe(expected.afterClick.more)
      expect(screen.queryByText(LIST_ERROR)).not.toBeInTheDocument()
      noMoreFail()
    }
  })

  const cvMore = cvListRows([numbered(21)]).map(([name, body]) => ['cv ' + name, body] as [string, unknown])
  test.each<[string, unknown]>([...LIST_FAILS.map(([k, r]) => [k, r] as [string, unknown]), ...cvMore])('SC-17 (error) 더 보기가 실패하면 이미 보인 카드는 그대로이고 그 자리에 「불러오지 못했어요 · 다시 시도」가 보인다 — 행 %s', async (_row, failure) => {
    // Given 첫 페이지가 「공연 1」~「공연 20」·totalElements 25 이고 ?page=1 응답이 표와 같다
    let failing = true
    const page1: Responder = (init, url) => {
      if (!failing) return res(200, listBody(range(21, 25), 25))
      return typeof failure === 'function' ? (failure as Responder)(init, url) : jsonWithInfinityResponse(failure)
    }
    // And 홈이 열려 있다
    await openHome({ list: { '': listBody(range(1, 20), 25), 'page=1': page1 } })
    // When 「더 보기」를 누른다
    await click(moreButton()!)
    // Then 카드 20개가 그대로이고 「더 보기」 자리에 「불러오지 못했어요 · 다시 시도」 버튼이 있다
    expect(cardLinks()).toHaveLength(20)
    expect(screen.getByRole('button', { name: MORE_FAIL })).toBeInTheDocument()
    // And 전체 오류 화면과 「더 보기」는 없다
    expect(screen.queryByText(LIST_ERROR)).not.toBeInTheDocument()
    expect(moreButton()).not.toBeInTheDocument()
    // And 「불러오지 못했어요 · 다시 시도」를 누르면 ?page=1 을 다시 부르고, 이번에 성공하면 카드 25개이고 그 버튼이 없다
    failing = false
    await click(screen.getByRole('button', { name: MORE_FAIL }))
    expect(urlsOf(calls, LIST_KEY).map(pathAndQuery).filter((u) => u === '/api/performances?page=1')).toHaveLength(2)
    expect(cardLinks()).toHaveLength(25)
    noMoreFail()
  })

  const DUP_ROWS: Array<[string, string]> = [
    ['a', '연달아 두 번 누른다'], ['b', '앞 페이지와 같은 id 가 온다'], ['h', '24<25 에서 다시 누르면 page=2'], ['c', '겹쳐도 25 서로 다른 공연이면 끝'],
    ['d', '더 보기 중 칩 전환 — 성공 응답'], ['e', '더 보기 중 칩 전환 — 실패 응답'], ['f', '더 보기 중 다른 화면으로'], ['g', '더 보기 중 로그인'],
  ]
  test.each(DUP_ROWS)('SC-18 (boundary) 더 보기 중에는 중복 요청이 없고, 같은 공연은 한 번만 보이고, 오래된 응답은 버려진다 — 행 %s (%s)', async (row) => {
    // Given 첫 페이지가 「공연 1」~「공연 20」·totalElements 25 이고 ?page=1 응답은 테스트가 풀 때까지 붙들려 있다
    const gate = deferred<Response>()
    const errors = vi.spyOn(console, 'error').mockImplementation(() => {})
    let page1: Responder = () => gate.promise
    await openHome({
      list: { '': listBody(range(1, 20), 25), 'page=1': (init: RequestInit, url: string) => page1(init, url), 'page=2': listBody([], 25), [queryOf('예정')]: listBody(BY_CHIP['예정']) },
    })
    // And 홈이 열려 있다
    // When 표의 조작을 한다
    const more = moreButton()!
    if (row === 'a') {
      await act(async () => {
        fireEvent.click(more)
        fireEvent.click(more)
      })
      await flush()
      // Then ?page=1 호출은 1회 — 진행 중에는 버튼이 눌려도 새 요청이 없다
      expect(urlsOf(calls, LIST_KEY).map(pathAndQuery).filter((u) => u === '/api/performances?page=1')).toHaveLength(1)
    } else if (row === 'b' || row === 'h') {
      page1 = () => res(200, listBody(range(20, 24), 25))
      await click(more)
      // Then 「공연 20」은 한 번만 보이고(먼저 보인 쪽 유지) 카드는 24개이며 「더 보기」가 남아 있다
      expect(cardLinks().filter((card) => within(card).queryByText('공연 20') !== null)).toHaveLength(1)
      expect(cardLinks()).toHaveLength(24)
      expect(moreButton()).toBeInTheDocument()
      if (row === 'h') {
        // 다시 누르면 ?page=2 를 요청한다 — 그 응답의 content 가 [] 이면 「더 보기」가 사라진다
        await click(moreButton()!)
        expect(lastUrl()).toBe('/api/performances?page=2')
        expect(moreButton()).not.toBeInTheDocument()
        expect(cardLinks()).toHaveLength(24)
      }
    } else if (row === 'c') {
      page1 = () => res(200, listBody(range(20, 25), 25))
      await click(more)
      expect(cardLinks()).toHaveLength(25)
      expect(moreButton()).not.toBeInTheDocument()
    } else if (row === 'd' || row === 'e') {
      await click(more)
      await click(chip('예정'))
      await act(async () => gate.resolve(row === 'd' ? res(200, listBody(range(21, 25), 25)) : res(500, { code: 'INTERNAL' })))
      await flush()
      // Then 카드는 「예정」 응답의 것이다(d) / 「불러오지 못했어요 · 다시 시도」가 뜨지 않는다(e)
      expect(pressed()).toEqual(['예정'])
      titlesAre(BY_CHIP['예정'])
      expect(cardLinks()).toHaveLength(BY_CHIP['예정'].length)
      noMoreFail()
    } else if (row === 'f') {
      await click(more)
      await navigateTo('/login')
      await act(async () => gate.resolve(res(200, listBody(range(21, 25), 25))))
      await flush()
      expect(errors).not.toHaveBeenCalled()
      expect(where()).toBe('/login')
    } else {
      await click(more)
      await loginNow()
      await act(async () => gate.resolve(res(200, listBody(range(21, 25), 25))))
      await flush()
      // Then 응답이 정상 반영돼 카드 25개이고 ?page=1 호출은 그대로 1회다
      expect(cardLinks()).toHaveLength(25)
      expect(urlsOf(calls, LIST_KEY).map(pathAndQuery).filter((u) => u === '/api/performances?page=1')).toHaveLength(1)
    }
  })

  const EMPTY_ROWS: Array<[string, string, string, boolean]> = [
    ['a', '전체', '다가오는 공연이 없어요', false],
    ['b', '예정', '예정 공연이 없어요', true],
    ['c', '예매 가능', '예매 가능 공연이 없어요', true],
    ['d', '매진', '매진 공연이 없어요', true],
    ['e', '마감', '마감 공연이 없어요', true],
    ['f', '취소', '취소 공연이 없어요', true],
    ['g', '전체', '다가오는 공연이 없어요', false],
  ]
  test.each(EMPTY_ROWS)('SC-19 (boundary) 빈 목록은 전체면 「다가오는 공연이 없어요」, 칩이면 「<칩> 공연이 없어요」와 「전체 보기」다 — 행 %s', async (row, name, text, hasAll) => {
    // Given 목록 응답이 표와 같다(칩이 표의 칩일 때의 응답)
    const empty = row === 'g' ? { content: [], page: 0, size: 20, totalElements: 3 } : listBody([], 0)
    // When 홈을 열고(칩 행은 그 칩을 누르고)
    await openHome({ list: { '': row === 'a' || row === 'g' ? empty : listBody(SIAN), [queryOf(name)]: empty } })
    if (row !== 'a' && row !== 'g') await click(chip(name))
    // Then 표의 문구가 보이고 카드·「더 보기」는 없다
    expect(screen.getByText(text)).toBeInTheDocument()
    expect(cardLinks()).toHaveLength(0)
    expect(moreButton()).not.toBeInTheDocument()
    expect(Boolean(screen.queryByRole('button', { name: '전체 보기' }))).toBe(hasAll)
    // And 머리글·칩 6개는 그대로 보인다
    expect(heroText()).toBe(HERO_L4)
    CHIPS.forEach((c) => expect(chip(c)).toBeInTheDocument())
    // And 「전체 보기」를 누르면 「전체」 칩이 눌리고 /api/performances(쿼리 없음)를 다시 부른다. 그 응답이 시안 목록이면 카드 6개가 보인다
    if (hasAll) {
      await click(screen.getByRole('button', { name: '전체 보기' }))
      expect(pressed()).toEqual(['전체'])
      expect(lastUrl()).toBe('/api/performances')
      expect(cardLinks()).toHaveLength(6)
    }
  })

  test.each([['a'], ['b'], ['c']])('SC-20 (boundary) 로딩 중에는 카드 자리 4개와 머리글 자리가 보인다 — 행 %s', async (row) => {
    // Given 목록·후보 응답이 표처럼 아직 오지 않았다 (a: 둘 다 대기, b: 후보만 응답, c: 목록만 응답)
    const listGate = deferred<Response>()
    const nextGate = deferred<Response>()
    startHome({
      list: { '': row === 'c' ? listBody(SIAN) : () => listGate.promise },
      next: row === 'b' ? L4 : () => nextGate.promise,
    })
    // When 홈을 연다
    await flush()
    // Then 표의 요소가 보인다
    CHIPS.forEach((c) => expect(chip(c)).toBeInTheDocument())
    expect(screen.queryByText(LIST_ERROR)).not.toBeInTheDocument()
    expect(screen.queryByText('다가오는 공연이 없어요')).not.toBeInTheDocument()
    if (row === 'a') {
      expect(skeletons()).toHaveLength(4)
      expect(heroSkeleton()).toBeInTheDocument()
      expect(heroText()).toBeNull()
      expect(cardLinks()).toHaveLength(0)
    } else if (row === 'b') {
      expect(skeletons()).toHaveLength(4)
      expect(heroText()).toBe(HERO_L4)
      expect(heroSkeleton()).not.toBeInTheDocument()
    } else {
      expect(cardLinks()).toHaveLength(6)
      expect(heroSkeleton()).toBeInTheDocument()
      expect(heroText()).toBeNull()
    }
    // And 카드 자리 하나하나의 aspect-ratio 는 1 / 1 이다(포스터 비율 유지)
    skeletons().forEach((el) => expect(squash(css(el, 'aspect-ratio'))).toBe('1/1'))
    // And 응답이 오면 카드 자리와 머리글 자리가 사라진다
    await act(async () => {
      listGate.resolve(res(200, listBody(SIAN)))
      nextGate.resolve(res(200, L4))
    })
    await flush()
    expect(skeletons()).toHaveLength(0)
    expect(heroSkeleton()).not.toBeInTheDocument()
  })

  test.each([...LIST_FAILS, ['k', (): Response => res(500, { code: 'INTERNAL' })]])('SC-21 (error) 목록 조회가 실패하면 「공연 목록을 불러오지 못했어요」와 「다시 시도」가 보이고, 다시 시도하면 다시 부른다 — 행 %s', async (row, fail) => {
    // Given 목록 응답이 표와 같다. 후보·시각은 정상이다 (행 k 는 「예정」 칩을 누른 뒤에 실패한다)
    let ok = row === 'k'
    const responder: Responder = (init, url) => (ok ? res(200, listBody(url.includes('status=') ? BY_CHIP['예정'] : SIAN)) : (fail as Responder)(init, url))
    await openHome({ list: { '': responder, [queryOf('예정')]: responder } })
    // When 홈을 연다
    if (row === 'k') {
      ok = false
      await click(chip('예정'))
    }
    // Then 「공연 목록을 불러오지 못했어요」와 「다시 시도」가 보이고 카드·「다가오는 공연이 없어요」는 없다
    expect(screen.getByText(LIST_ERROR)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: RETRY })).toBeInTheDocument()
    expect(cardLinks()).toHaveLength(0)
    expect(screen.queryByText('다가오는 공연이 없어요')).not.toBeInTheDocument()
    // And 머리글(h1)과 칩 6개는 그대로 보이고 누른 칩이 눌려 있다
    expect(heroText()).toBe(HERO_L4)
    expect(pressed()).toEqual([row === 'k' ? '예정' : '전체'])
    // And 「다시 시도」를 누르면 목록을 다시 부르고 이번 응답이 정상이면 카드가 보인다(후보 호출 수는 그대로 1)
    ok = true
    await click(screen.getByRole('button', { name: RETRY }))
    expect(lastUrl()).toBe(row === 'k' ? '/api/performances?status=UPCOMING' : '/api/performances')
    expect(cardLinks()).toHaveLength(row === 'k' ? BY_CHIP['예정'].length : 6)
    expect(urlsOf(calls, NEXT_KEY)).toHaveLength(1)
  })

  test.each(cvListRows(SIAN))('SC-22 (error) 목록 응답이 계약을 어기면 화면이 깨지지 않고 오류 화면이 된다 — %s', async (_row, body) => {
    // Given 목록 응답이 200 으로 시안 목록에서 CV 표의 값만 바꾼 본문이다
    const errors = vi.spyOn(console, 'error').mockImplementation(() => {})
    // When 홈을 연다
    await openHome({ list: { '': body } })
    // Then 「공연 목록을 불러오지 못했어요」와 「다시 시도」가 보인다
    expect(screen.getByText(LIST_ERROR)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: RETRY })).toBeInTheDocument()
    // And 화면이 비지 않고 예외로 멈추지 않는다(콘솔 오류 0건), 카드는 한 장도 보이지 않는다
    expect(errors).not.toHaveBeenCalled()
    expect(cardLinks()).toHaveLength(0)
    // And 「다시 시도」를 누르면 목록을 다시 부른다
    await click(screen.getByRole('button', { name: RETRY }))
    expect(urlsOf(calls, LIST_KEY)).toHaveLength(2)
  })

  test.each([['a'], ['b'], ['c'], ['d'], ['e'], ['f'], ['g'], ['h']])('SC-23 (boundary) 응답이 오는 사이 화면이 바뀌거나 세션이 바뀌어도 홈이 깨지지 않는다 — 행 %s', async (row) => {
    // Given 세 응답(목록·후보·시각)이 표처럼 늦게 풀린다
    const listGate = deferred<Response>()
    const nextGate = deferred<Response>()
    const timeGate = deferred<void>()
    const signals: Array<AbortSignal | null | undefined> = []
    const errors = vi.spyOn(console, 'error').mockImplementation(() => {})
    const time: Responder = row === 'c'
      ? (init) => { signals.push(init.signal); return new Promise<Response>((_r, reject) => init.signal?.addEventListener('abort', () => reject(new DOMException('aborted', 'AbortError')))) }
      : async (init, url) => {
          await timeGate.promise
          return row === 'h' ? res(500, { code: 'INTERNAL' }) : timeRoute()(init, url)
        }
    if (row === 'd') {
      // StrictMode — 효과가 두 번 돈다. 요청이 중복돼도 화면은 하나다(응답마다 새 Response 라 두 번 읽히지 않는다)
      startHome({ strict: true })
      await flush()
      // Then 머리글 h1 하나·카드는 응답마다 한 번씩만 보인다(중복 카드 없음)
      expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1)
      expect(cardLinks()).toHaveLength(6)
      return
    }
    startHome({ list: { '': () => listGate.promise }, next: () => nextGate.promise, time })
    await flush()
    const release = async (listBody_: Response, nextBody: Response) => {
      await act(async () => {
        listGate.resolve(listBody_)
        nextGate.resolve(nextBody)
        timeGate.resolve()
      })
      await flush()
    }
    // When 표의 조작을 한다
    if (row === 'a' || row === 'b') {
      await navigateTo('/login')
      await release(row === 'a' ? res(200, listBody(SIAN)) : res(500, { code: 'INTERNAL' }), row === 'a' ? res(200, L4) : res(500, { code: 'INTERNAL' }))
      // Then 콘솔 오류 0건, /login 화면이 그대로다
      expect(errors).not.toHaveBeenCalled()
      expect(where()).toBe('/login')
    } else if (row === 'c') {
      await navigateTo('/login')
      await flush(1000)
      // Then 붙들린 요청의 AbortSignal 이 중단되고 그 뒤 시각 호출이 더 나가지 않는다
      expect(signals[0]?.aborted).toBe(true)
      expect(urlsOf(calls, TIME)).toHaveLength(1)
    } else if (row === 'e' || row === 'f') {
      await loginNow()
      if (row === 'f') await logoutNow()
      await release(res(200, listBody(SIAN)), res(200, L4))
      // Then 세 응답이 정상 반영되고 호출 수는 늘지 않는다
      expect(heroText()).toBe(HERO_L4)
      expect(cardLinks()).toHaveLength(6)
      expect(eyebrow()).not.toBeNull()
      expect(urlsOf(calls, LIST_KEY)).toHaveLength(1)
      expect(urlsOf(calls, NEXT_KEY)).toHaveLength(1)
      expect(urlsOf(calls, TIME)).toHaveLength(3)
    } else if (row === 'g') {
      await release(res(200, listBody(SIAN)), res(500, { code: 'INTERNAL' }))
      // Then 카드는 그대로이고 h1 은 「다가오는 공연을 골라 보세요」다
      expect(cardLinks()).toHaveLength(6)
      expect(heroText()).toBe(GENERAL)
    } else {
      await release(res(200, listBody(SIAN)), res(200, L4))
      // Then h1·카드는 그대로이고 시각 줄만 없다
      expect(heroText()).toBe(HERO_L4)
      expect(cardLinks()).toHaveLength(6)
      expect(eyebrow()).toBeNull()
    }
  })
})
