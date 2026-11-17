import { act, fireEvent, screen, within } from '@testing-library/react'
import {
  L1,
  L2,
  L3,
  L4,
  L5,
  L6,
  L7,
  LIST_KEY,
  NEXT_KEY,
  S0,
  SIAN,
  TIME,
  TITLE_L4,
  cardLinks,
  cardOf,
  cardTitles,
  click,
  cvNextRows,
  deferred,
  eyebrow,
  flush,
  hero,
  heroText,
  inCard,
  listBody,
  moreLink,
  networkError,
  openHome,
  pathAndQuery,
  pressed,
  raw,
  res,
  startHome,
  timeRoute,
  urlsOf,
} from '../test/task021'
import type { Item, Responder } from '../test/task021'
import { P_OPEN, calls, css, isTokenColor, resetDocumentVisibility, setDocumentVisibility, squash, where } from '../test/task020'

// TASK-021 시나리오 — 홈(머리글·기준 시각·카드·포스터). SoT: workflow_design/05_scenario/SCENARIO_TASK-021.md
// 표로 묶은 시나리오는 test.each 한 줄(= 시나리오 한 건)이고 행마다 케이스가 하나씩 돈다.
// 화면은 App + MemoryRouter + AuthProvider 로 실제 라우트를 지나고 시간은 가짜 타이머다(flush 가 응답·효과를 처리한다).

const HERO_L4 = `오늘 오후 6:00에 ${TITLE_L4} 예매가 열려요`
const GENERAL = '다가오는 공연을 골라 보세요'

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

const never: Responder = () => new Promise<Response>(() => {})
const status = (code: number, body = ''): Responder => () => new Response(body, { status: code })

describe('TASK-021 — 홈 (머리글·기준 시각·카드·포스터)', () => {
  test('SC-01 (happy) 홈이 로딩 뒤에 시안 구조로 보인다', async () => {
    // Given 후보 응답이 L4(200), 시각 응답이 S0, 목록 응답이 시안 목록이다
    // When 홈(/)을 연다
    await openHome()

    // Then h1 은 「오늘 오후 6:00에 …」이고 그 위 줄은 「티켓온 · 11월 11일(수) 오후 5:50」이다
    expect(heroText()).toBe(HERO_L4)
    expect(eyebrow()).toBe('티켓온 · 11월 11일(수) 오후 5:50')
    // And 「… 공연 보기」 링크가 있고 h2 「다가오는 공연」 아래에 칩 6개가 있다 — 「전체」만 aria-pressed="true" 다
    expect(moreLink()).toHaveAttribute('href', '/performances/7')
    expect(moreLink()).toHaveAccessibleName(`${TITLE_L4} 공연 보기`)
    expect(screen.getByRole('heading', { level: 2, name: '다가오는 공연' })).toBeInTheDocument()
    expect(pressed()).toEqual(['전체'])
    // And 카드는 6개이고 순서는 L1~L6 다, 「더 보기」는 없다
    const titles = cardTitles()
    expect(titles).toHaveLength(6)
    SIAN.forEach((item, i) => expect(titles[i]).toContain(String(item.title)))
    expect(screen.queryByRole('button', { name: '더 보기' })).not.toBeInTheDocument()
    // And 목록 호출 수 1·후보 호출 수 1·시각 호출 수 3이고 목록 요청 URL 에 쿼리 문자열이 없다
    expect(urlsOf(calls, LIST_KEY)).toHaveLength(1)
    expect(urlsOf(calls, NEXT_KEY)).toHaveLength(1)
    expect(urlsOf(calls, TIME)).toHaveLength(3)
    expect(pathAndQuery(urlsOf(calls, LIST_KEY)[0])).toBe('/api/performances')
  })

  const HEADLINES: Array<[string, Item, string, string | undefined, string]> = [
    ['a', L4, S0, undefined, HERO_L4],
    ['b', L3, S0, undefined, '11월 14일(토) 오후 2:00에 재즈 나잇 인 서울 예매가 열려요'],
    ['c', { ...L4, openAt: '2026-11-11T00:00:00Z' }, S0, undefined, `오늘 오전 9:00에 ${TITLE_L4} 예매가 열려요`],
    ['d', { ...L4, openAt: '2026-11-11T14:59:59Z' }, '2026-11-11T14:59:59Z', undefined, `오늘 오후 11:59에 ${TITLE_L4} 예매가 열려요`],
    ['e', { ...L4, openAt: '2026-11-11T15:00:00Z' }, '2026-11-11T14:59:59Z', undefined, `11월 12일(목) 오전 12:00에 ${TITLE_L4} 예매가 열려요`],
    ['f', { ...L4, openAt: '2026-11-11T14:59:59Z' }, '2026-11-11T15:00:00Z', undefined, `11월 11일(수) 오후 11:59에 ${TITLE_L4} 예매가 열려요`],
    ['g', L4, S0, '2026-11-11T15:30:00Z', HERO_L4],
  ]
  test.each(HEADLINES)('SC-02 (happy) 머리글은 오픈 날짜가 오늘이면 「오늘 …」, 아니면 날짜로 쓴다 — 행 %s', async (_row, candidate, server, device, expected) => {
    // Given 후보 응답이 표의 공연이고 시각 응답이 표의 기준 시각이다(DEVICE 는 표가 정한다 — 없으면 기준 시각과 같다)
    const deviceAt = device ?? server
    const skew = Date.parse(deviceAt) - Date.parse(server)
    // When 홈을 열고 기준 시각 측정이 끝나기를 기다린다
    await openHome({ device: deviceAt, skew, next: candidate })
    // Then h1 은 표의 문구다
    expect(heroText()).toBe(expected)
  })

  test.each([
    ['a', '/performances/7', '루미너스 콘서트 〈WAVELENGTH〉 서울 공연 보기', L4, TITLE_L4],
    ['b', '/performances/11', 'card:소리풍경 밴드 나이트', L4, '소리풍경 밴드 나이트'],
    ['c', '/performances/7', 'card:루미너스 콘서트', L4, TITLE_L4],
    ['d', '/performances/13', '재즈 나잇 인 서울 공연 보기', L3, '재즈 나잇 인 서울'],
  ] as Array<[string, string, string, Item, string]>)('SC-03 (happy) 「공연 보기」와 카드를 누르면 그 상세로 간다 — 행 %s', async (_row, path, target, next, expectedTitle) => {
    // Given 후보 응답이 L4(행 d 는 L3), 목록 응답이 시안 목록이다. 상세는 표의 공연을 돌려준다
    const detail = (itemId: number): Responder => () => res(200, { ...P_OPEN(), id: itemId, title: SIAN.concat([L4]).find((l) => l.id === itemId)?.title ?? '재즈 나잇 인 서울' })
    await openHome({ next, routes: { 'GET /api/performances/7': detail(7), 'GET /api/performances/11': detail(11), 'GET /api/performances/13': detail(13) } })
    // When 표의 링크를 누른다
    const link = target.startsWith('card:') ? cardOf(target.slice(5)) : screen.getByRole('link', { name: target })
    await click(link)
    // Then 주소가 표의 경로다
    expect(where()).toBe(path)
    // And 상세 화면이 그 공연의 h2 를 보인다(상세 호출 수 1)
    expect(screen.getByRole('heading', { level: 2, name: expectedTitle })).toBeInTheDocument()
    expect(urlsOf(calls, `GET /api${path}`)).toHaveLength(1)
  })

  const NEXT_FAILS: Array<[string, Responder]> = [
    ['a', status(204)],
    ['b', () => res(404, { code: 'NOT_FOUND' })],
    ['c', () => res(404, { code: 'SOMETHING_ELSE' })],
    ['d', () => res(500, { code: 'INTERNAL' })],
    ['e', status(503)],
    ['f', () => raw(502, '<html>Bad Gateway</html>')],
    ['g', status(200)],
    ['h', () => raw(200, '<html>ok</html>')],
    ['i', () => networkError()],
    ['j', () => res(401, { code: 'UNAUTHORIZED' })],
  ]
  test.each(NEXT_FAILS)('SC-04 (error) 후보가 없거나 못 가져오면 일반 문구만 보이고 목록은 그대로다 — 행 %s', async (_row, next) => {
    // Given 후보 응답이 표와 같고 시각·목록 응답은 정상이다
    // When 홈을 연다
    await openHome({ next })
    // Then h1 은 「다가오는 공연을 골라 보세요」이고 「… 공연 보기」 링크는 없다
    expect(heroText()).toBe(GENERAL)
    expect(moreLink()).not.toBeInTheDocument()
    // And 카드 6개와 칩 6개는 그대로 보이고 「공연 목록을 불러오지 못했어요」는 없다
    expect(cardLinks()).toHaveLength(6)
    expect(pressed()).toEqual(['전체'])
    expect(screen.queryByText('공연 목록을 불러오지 못했어요')).not.toBeInTheDocument()
    // And 「티켓온 · 11월 11일(수) 오후 5:50」 줄은 있다
    expect(eyebrow()).toBe('티켓온 · 11월 11일(수) 오후 5:50')
  })

  test.each(cvNextRows())('SC-05 (error) 후보 응답이 계약을 어기면 후보 없음으로 본다 — %s', async (_row, body) => {
    // Given 후보 응답이 200 으로 L4 에서 CV 표의 값만 바꾼 본문이다
    const errors = vi.spyOn(console, 'error').mockImplementation(() => {})
    // When 홈을 연다
    await openHome({ next: body })
    // Then h1 은 「다가오는 공연을 골라 보세요」이고 「… 공연 보기」 링크는 없다
    expect(heroText()).toBe(GENERAL)
    expect(moreLink()).not.toBeInTheDocument()
    // And 화면이 비지 않고 예외로 멈추지 않는다(콘솔 오류 0건), 카드 6개가 그대로 보인다
    expect(errors).not.toHaveBeenCalled()
    expect(cardLinks()).toHaveLength(6)
  })

  test('SC-06 (boundary) 공연명이 아주 길어도 잘리지 않은 전체가 화면에 있다', async () => {
    // Given 후보 L4 의 title 이 한글 201자(서버 상한 200자 + 1), 목록의 첫 공연 L1 의 title·venue 도 각각 201자다
    const longTitle = '가'.repeat(201)
    const longVenue = '나'.repeat(201)
    const first = { ...L1, title: longTitle, venue: longVenue }
    // When 홈을 연다
    await openHome({ next: { ...L4, title: longTitle }, list: { '': listBody([first, ...SIAN.slice(1)]) } })
    // Then 오류 화면이 아니라 홈이 보인다 — h1 의 글자는 전체이고 줄여지지 않았다
    expect(screen.queryByText('공연 목록을 불러오지 못했어요')).not.toBeInTheDocument()
    expect(heroText()).toBe(`오늘 오후 6:00에 ${longTitle} 예매가 열려요`)
    // And 첫 카드의 글자에 201자 공연명과 201자 장소가 전체로 들어 있다
    const card = cardLinks()[0]
    expect(card.textContent).toContain(longTitle)
    expect(card.textContent).toContain(longVenue)
    // And 긴 낱말이 칸을 밀지 않는다 — h1 과 카드 공연명·장소 요소의 overflow-wrap 이 anywhere 또는 break-word 다
    const wraps = [hero()!, within(card).getByText(longTitle), within(card).getByText((c) => c.includes(longVenue))]
    for (const el of wraps) expect(['anywhere', 'break-word']).toContain(css(el, 'overflow-wrap'))
  })

  const LINES: Array<[string, string, string, string?]> = [
    ['a', S0, 'S0 · 기기=서버', undefined],
    ['b', S0, '기기가 3초 빠르다', '2026-11-11T08:50:03Z'],
    ['c', S0, '기기가 3시간 빠르다', '2026-11-11T11:50:00Z'],
    ['d', '2026-11-11T15:00:00Z', '한국 자정', undefined],
  ]
  test.each(LINES)('SC-07 (happy) 머리글 위에 기준 시각이 한국 시간으로 보이고 시간이 흐르면 갱신된다 — 행 %s (%s)', async (row, server, _label, device) => {
    // Given 시각 응답이 표처럼 정해진다. 후보·목록은 정상이다
    const deviceAt = device ?? server
    const expected = row === 'd' ? '티켓온 · 11월 12일(목) 오전 12:00' : '티켓온 · 11월 11일(수) 오후 5:50'
    // When 홈을 열고 측정이 끝나기를 기다린다
    await openHome({ device: deviceAt, skew: Date.parse(deviceAt) - Date.parse(server) })
    // Then 머리글 위 줄은 표의 문구다
    expect(eyebrow()).toBe(expected)
    // And 행 a 에서 시계를 61초 흘리면 줄이 「티켓온 · 11월 11일(수) 오후 5:51」로 바뀐다
    if (row === 'a') {
      await flush(61_000)
      expect(eyebrow()).toBe('티켓온 · 11월 11일(수) 오후 5:51')
    }
  })

  const TIME_FAILS: Array<[string, Responder]> = [
    ['a', never],
    ['b', () => res(404, { code: 'NOT_FOUND' })],
    ['c', () => res(500, { code: 'OTHER' })],
    ['d', status(200)],
    ['e', () => raw(200, '<html>ok</html>')],
    ['f', () => networkError()],
    ['g', () => res(200, {})],
    ['h', () => res(200, { now: null })],
    ['i', () => res(200, { now: 1731315600000 })],
    ['j', () => res(200, { now: '내일' })],
    ['k', () => res(200, { now: '2026-11-11T08:50:00' })],
    ['l', (init) => new Promise<Response>((_resolve, reject) => init.signal?.addEventListener('abort', () => reject(new DOMException('aborted', 'AbortError'))))],
  ]
  test.each(TIME_FAILS)('SC-08 (error) 기준 시각을 못 맞췄거나 맞추는 중이면 시각 줄이 없고 나머지는 그대로다 — 행 %s', async (row, time) => {
    // Given 시각 응답이 표와 같다. 후보는 L4·목록은 시안 목록이다
    // When 홈을 열고 측정이 끝나기를 기다린다(행 l 은 표본 시간 초과까지 시계를 흘린다)
    await openHome({ time })
    if (row === 'l') await flush(16_000)
    // Then 「티켓온 ·」으로 시작하는 줄이 없다
    expect(eyebrow()).toBeNull()
    // And h1·칩·카드는 그대로 보이고 「공연 목록을 불러오지 못했어요」는 없다
    expect(pressed()).toEqual(['전체'])
    expect(cardLinks()).toHaveLength(6)
    expect(screen.queryByText('공연 목록을 불러오지 못했어요')).not.toBeInTheDocument()
    // And 「기준 시각」 안내·「다시 맞추기」 버튼은 홈에 없다
    expect(screen.queryByText(/기준 시각/)).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '다시 맞추기' })).not.toBeInTheDocument()
    // And h1 은 기기 날짜로 가른 「오늘 오후 6:00에 …」다 — 후보 문구는 사라지지 않는다
    expect(heroText()).toBe(HERO_L4)
  })

  test('SC-09 (boundary) 기준 시각이 정해지기 전 「오늘」 문구는 기기 날짜로, 정해진 뒤에는 기준 시각 날짜로 바뀐다', async () => {
    // Given DEVICE = 2026-11-11T15:30:00Z(기기의 한국 날짜는 11월 12일), 서버 시각은 S0 이고 응답을 아직 붙들고 있다
    const gate = deferred<void>()
    const skew = Date.parse('2026-11-11T15:30:00Z') - Date.parse(S0)
    const time: Responder = async (init, url) => {
      await gate.promise
      return timeRoute({ skew })(init, url)
    }
    // And 후보 L4·목록은 시안 목록이다
    startHome({ device: '2026-11-11T15:30:00Z', time })
    await flush()
    // When 시각 응답이 오기 전과 온 뒤를 각각 본다
    // Then 오기 전: h1 은 「11월 11일(수) 오후 6:00에 …」이고 L4 카드 한 줄은 「고척스카이돔 · 11월 11일(수) 오후 6:00 오픈」이다
    expect(heroText()).toBe(`11월 11일(수) 오후 6:00에 ${TITLE_L4} 예매가 열려요`)
    expect(inCard('루미너스').getByText('고척스카이돔 · 11월 11일(수) 오후 6:00 오픈')).toBeInTheDocument()
    // And 온 뒤: h1 은 「오늘 오후 6:00에 …」, L4 카드 한 줄은 「고척스카이돔 · 오늘 오후 6:00 오픈」으로 바뀐다
    await act(async () => gate.resolve())
    await flush()
    expect(heroText()).toBe(HERO_L4)
    expect(inCard('루미너스').getByText('고척스카이돔 · 오늘 오후 6:00 오픈')).toBeInTheDocument()
    // And 이 바뀜 동안 목록 호출 수·후보 호출 수는 늘지 않는다
    expect(urlsOf(calls, LIST_KEY)).toHaveLength(1)
    expect(urlsOf(calls, NEXT_KEY)).toHaveLength(1)
  })

  test.each([
    ['a', async () => flush(600_000), 3],
    ['b', async () => act(async () => setDocumentVisibility('hidden')), 3],
    ['c', async () => { await act(async () => setDocumentVisibility('visible')); await flush() }, 6],
    ['d', async () => { await act(async () => setDocumentVisibility('prerender')); await flush() }, 3],
    ['e', async () => { await act(async () => setDocumentVisibility('visible')); await flush() }, 6],
  ] as Array<[string, () => Promise<unknown>, number]>)('SC-10 (boundary) 시간이 흘러도 목록·후보를 다시 부르지 않고, 탭이 다시 보일 때만 시각을 다시 잰다 — 행 %s', async (row, operate, timeCalls) => {
    // Given 홈이 시안 목록으로 보이고 측정이 끝났다(시각 호출 수 3, 목록·후보 호출 수 1) — 행 e 는 첫 3회 뒤의 측정이 모두 실패한다
    let n = 0
    const time: Responder = (init, url) => (row === 'e' && n++ >= 3 ? networkError() : timeRoute()(init, url))
    await openHome({ time })
    expect(urlsOf(calls, TIME)).toHaveLength(3)
    // When 시계를 흘리거나 탭 상태를 표처럼 바꾼다
    await operate()
    await flush()
    // Then 목록·후보 호출 수는 그대로 1이고 시각 호출 수는 표와 같다
    expect(urlsOf(calls, LIST_KEY)).toHaveLength(1)
    expect(urlsOf(calls, NEXT_KEY)).toHaveLength(1)
    expect(urlsOf(calls, TIME)).toHaveLength(timeCalls)
    // And 카드 6개와 칩의 aria-pressed 는 그대로다(행 e 는 시각 줄이 이전 값에서 계속 흐르고 사라지지 않는다)
    expect(cardLinks()).toHaveLength(6)
    expect(pressed()).toEqual(['전체'])
    if (row === 'e') expect(eyebrow()).not.toBeNull()
  })

  type Row = [string, Item, string, string, string, { device?: string }?]
  const CARD_ROWS: Row[] = [
    ['a', L1, '예매 가능', '11월 21일(토) 오후 7:00', '롤링홀 · 남은 좌석 37석'],
    ['b', { ...L1, availableSeats: 1 }, '예매 가능', '11월 21일(토) 오후 7:00', '롤링홀 · 남은 좌석 1석'],
    ['c', { ...L1, availableSeats: 0 }, '예매 가능', '11월 21일(토) 오후 7:00', '롤링홀 · 남은 좌석 0석'],
    ['d', L4, '예정', '12월 19일(토) 오후 6:00', '고척스카이돔 · 오늘 오후 6:00 오픈'],
    ['e', L3, '예정', '12월 5일(토) 오후 7:00', '세종문화회관 · 11월 14일(토) 오후 2:00 오픈'],
    ['f', { ...L4, openAt: S0 }, '예정', '12월 19일(토) 오후 6:00', '고척스카이돔 · 오늘 오후 5:50 오픈'],
    ['g', L5, '매진', '12월 24일(목) 오후 7:00', '예술의전당 오페라극장'],
    ['h', L2, '마감', '11월 28일(토) 오후 5:00', '금호아트홀'],
    ['i', L7, '공연 취소', '12월 12일(토) 오후 7:00', '모모홀'],
    ['j', L6, '예정', '1월 9일(토) 오후 6:00', '샤롯데씨어터 · 11월 18일(수) 오후 8:00 오픈'],
    ['k', { ...L4, openAt: '2026-11-11T15:00:00Z' }, '예정', '12월 19일(토) 오후 6:00', '고척스카이돔 · 11월 12일(목) 오전 12:00 오픈', { device: '2026-11-11T14:59:59Z' }],
    ['l', { ...L4, openAt: '2026-11-11T14:59:59Z' }, '예정', '12월 19일(토) 오후 6:00', '고척스카이돔 · 11월 11일(수) 오후 11:59 오픈', { device: '2026-11-11T15:00:00Z' }],
  ]
  test.each(CARD_ROWS)('SC-11 (happy) 카드는 상태별로 배지·공연명·일시·장소 줄이 시안대로 보인다 — 행 %s', async (row, item, badge, startsAt, line, options) => {
    // Given 목록 응답이 표의 공연 하나이고 시각 응답이 기준 시각이다(표가 달리 적지 않으면 S0)
    // When 홈을 연다
    await openHome({ list: { '': listBody([item]) }, ...options })
    // Then 그 카드 안에 배지·공연명·시작 일시·한 줄이 표와 같다
    const card = inCard(String(item.title))
    expect(card.getAllByText(badge)).toHaveLength(1) // And 카드 안 배지 글자는 하나뿐이다
    expect(card.getByText(String(item.title))).toBeInTheDocument()
    // And 시작 일시는 어느 행에서나 날짜와 시각을 함께 보인다
    expect(card.getByText(startsAt)).toBeInTheDocument()
    expect(card.getByText(line)).toBeInTheDocument()
    // And 행 g·h·i 는 장소 줄에 「남은 좌석」과 「오픈」이 없다
    if (['g', 'h', 'i'].includes(row)) expect(cardOf(String(item.title)).textContent).not.toMatch(/남은 좌석|오픈/)
  })

  const POSTERS: Array<[string, unknown, 'img' | 'icon', string]> = [
    ['a', 'https://cdn.example.com/sori.jpg', 'img', '이미지가 로드된다 (기준 행)'],
    ['b', undefined, 'icon', '키 없음'],
    ['c', null, 'icon', 'null'],
    ['d', '', 'icon', '빈 문자열'],
    ['e', '   ', 'icon', '공백만'],
    ['f', 'https://cdn.example.com/sori.jpg', 'icon', 'error 이벤트'],
    ['g', 123, 'icon', '숫자'],
    ['h', {}, 'icon', '객체'],
    ['i', 'javascript:alert(1)', 'icon', 'javascript 스킴'],
    ['j', 'data:image/png;base64,AAAA', 'icon', 'data 스킴'],
    ['k', 'ftp://cdn.example.com/sori.jpg', 'icon', 'ftp 스킴'],
    ['l', 'http://cdn.example.com/sori.jpg', 'img', 'http'],
    ['m', 'HTTPS://cdn.example.com/sori.jpg', 'img', '스킴 대소문자'],
    ['n', ' https://cdn.example.com/sori.jpg ', 'img', '앞뒤 공백'],
  ]
  test.each(POSTERS)('SC-12 (boundary) 포스터가 없거나 불러오지 못하면 같은 크기의 회색 면과 아이콘이 보인다 — 행 %s (%s)', async (row, posterUrl, kind) => {
    // Given 목록 응답이 첫 공연 L1 의 posterUrl 만 표와 같게 바꾼 것(나머지는 시안 목록)이다
    const first = { ...L1, posterUrl }
    // When 홈을 열고 표의 조건을 만든다
    await openHome({ list: { '': listBody([first, ...SIAN.slice(1)]) } })
    if (row === 'f') {
      await act(async () => {
        fireEvent.error(within(cardLinks()[0]).getByRole('img', { name: '소리풍경 밴드 나이트 포스터' }))
      })
    }
    // Then 첫 카드에 이름이 「소리풍경 밴드 나이트 포스터」 인 이미지 역할 요소가 하나 있다
    const card = within(cardLinks()[0])
    const poster = card.getByRole('img', { name: '소리풍경 밴드 나이트 포스터' })
    // And 그 포스터 칸의 aspect-ratio 는 1 / 1, 배경색은 --c-bg 다 — 포스터가 있을 때와 같다
    const box = card.getByTestId('poster')
    expect(squash(css(box, 'aspect-ratio'))).toBe('1/1')
    expect(isTokenColor(css(box, 'background-color'), '--c-bg')).toBe(true)
    // And 포스터가 없는 행은 그 요소가 <img> 가 아니라 아이콘 면이다
    expect(poster.tagName === 'IMG').toBe(kind === 'img')
    })
})

