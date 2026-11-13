import { act, fireEvent, screen } from '@testing-library/react'
import {
  DETAIL,
  P_CANCEL,
  P_CLOSED,
  P_OPEN,
  P_SOLD,
  P_UP,
  T_OPEN,
  T_SERVER,
  TIME,
  TITLE,
  at,
  authApi,
  callsTo,
  cta,
  deferred,
  h1,
  hasGlobalHeader,
  navigateTo,
  networkError,
  raw,
  renderAt,
  res,
  resetDocumentVisibility,
  setDocumentVisibility,
  settle,
  stubServer,
  timeRoute,
  timeSamples,
  timer,
  timerOrNull,
  startFakeClock,
  USER,
  where,
} from '../test/task020'
import type { Responder, Sample } from '../test/task020'

// TASK-020 시나리오 — 기준 시각·카운트다운·0초 전환(가짜 타이머). SoT: workflow_design/05_scenario/SCENARIO_TASK-020.md
// 「시계」 규약: 기기 시계 DEVICE 를 정하고 settle(ms) 로 흘린다. 서버 시각 = DEVICE − SKEW.
// 프론트에 가짜 타이머 선례가 없다 — 응답·효과는 settle() 이 act 안에서 처리하고, findBy 는 쓰지 않는다.

const FAIL_TEXT = '기준 시각을 맞추지 못했어요'
const UP_CTA = '예매하기 · 오후 6:00에 열려요'
const LEAD_TAIL = ' · 폰과 PC가 같은 순간에 열려요'

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

/** 기기 시계를 server+skew 로 두고 P_UP 상세를 연 뒤 응답·측정이 끝나게 한다 */
async function openUp(options: {
  server?: string
  skew?: number
  time?: Responder
  perf?: Record<string, unknown>
  strict?: boolean
} = {}) {
  const { server = T_SERVER, skew = 0, time, perf, strict } = options
  startFakeClock(at(server, skew))
  stubServer({ [DETAIL]: () => res(200, P_UP(perf)), [TIME]: time ?? timeRoute({ skew }) })
  renderAt('/performances/7', { strict })
  await settle(0)
}

const click = async (el: HTMLElement) => {
  await act(async () => {
    fireEvent.click(el)
  })
  await settle(0)
}

describe('TASK-020 — 기준 시각·카운트다운·0초 전환', () => {
  test('SC-07 (happy) 기기 시계가 서버보다 3초 빨라도 카운트다운과 CTA 는 서버 시각을 따른다', async () => {
    // Given P_UP(openAt 11-11T09:00:00Z), 서버 시각 08:55:28Z, SKEW = 3000, 상세를 열어 측정을 마쳤다 (진입 직후 = 0초)
    await openUp({ skew: 3000 })

    // Then 0초 — 카운트다운 「4분 32초」, CTA 꺼짐 「예매하기 · 오후 6:00에 열려요」
    expect(timer()).toHaveTextContent('4분 32초')
    // And aria-label 은 「오픈까지 」 + 표시 문구다
    expect(timer()).toHaveAttribute('aria-label', '오픈까지 4분 32초')
    expect(cta(UP_CTA)).toBeDisabled()
    // And 「티켓온 기준 오후 5:55:28」 이 보인다 (기기 시각 오후 5:55:31 이 아니다)
    expect(screen.getByText(/티켓온 기준 오후 5:55:28/)).toBeInTheDocument()
    expect(screen.queryByText(/오후 5:55:31/)).not.toBeInTheDocument()
    // And GET /api/time 요청에는 cache: 'no-store' 가 있고 Authorization 헤더가 없다
    for (const call of callsTo(TIME)) {
      expect(call.init.cache).toBe('no-store')
      expect(new Headers(call.init.headers).has('Authorization')).toBe(false)
    }

    // When 271.999초가 흐른다 (기기 시계로는 openAt 을 이미 2.999초 넘긴 시점)
    await settle(271_999)
    // Then CTA 는 아직 꺼짐이다
    expect(cta(UP_CTA)).toBeDisabled()

    // When 272.000초가 된다
    await settle(1)
    // Then 카운트다운이 없고 머리글 「지금 예매할 수 있어요」, 켜진 「예매하기」다
    expect(timerOrNull()).not.toBeInTheDocument()
    expect(h1()).toHaveTextContent('지금 예매할 수 있어요')
    expect(cta('예매하기')).toBeEnabled()
  })

  const SEC = 1000
  const MIN = 60 * SEC
  const HOUR = 60 * MIN
  const DAY = 24 * HOUR
  test.each([
    ['a', 3 * DAY + 4 * HOUR + 30 * MIN, '3일 4시간'],
    ['b', 2 * HOUR + 15 * MIN + 20 * SEC, '2시간 15분'],
    ['c', 4 * MIN + 32 * SEC, '4분 32초'],
    ['d', 32 * SEC, '32초'],
    ['e', DAY, '1일 0시간'],
    ['f', 23 * HOUR + 59 * MIN + 59 * SEC, '23시간 59분'],
    ['g', HOUR, '1시간 0분'],
    ['h', 59 * MIN + 59_900, '59분 59초'],
    ['i', MIN, '1분 0초'],
    ['j', 59_900, '59초'],
    ['k', SEC, '1초'],
    ['l', 500, '0초'],
  ])('SC-08 (boundary) 남은 시간이 단위 경계에서 정해진 형식으로 표시된다 — %s', async (_row, remaining, text) => {
    // Given P_UP 이고 서버 시각 고정(SKEW = 0), openAt 은 서버 시각 + 표의 남은 시간이다
    await openUp({ perf: { openAt: at(T_SERVER, remaining) } })

    // Then role="timer" 요소가 정확히 하나이고 그 텍스트는 표의 문구다
    expect(screen.getAllByRole('timer')).toHaveLength(1)
    expect(timer()).toHaveTextContent(text)
    // And 그 요소의 aria-label 은 「오픈까지 」 + 같은 문구다
    expect(timer()).toHaveAttribute('aria-label', `오픈까지 ${text}`)
  })

  test.each([
    ['a', '2026-11-11T08:55:28Z', '2026-11-11T09:00:00Z', '11월 11일(수) 오후 6:00 예매 오픈', '오후 5:55:28', '예매하기 · 오후 6:00에 열려요'],
    ['b', '2026-11-11T08:55:28Z', '2026-11-14T05:00:00Z', '11월 14일(토) 오후 2:00 예매 오픈', '오후 5:55:28', '예매하기 · 11월 14일(토) 오후 2:00에 열려요'],
    ['c', '2026-11-11T14:59:59Z', '2026-11-11T15:00:00Z', '11월 12일(목) 오전 12:00 예매 오픈', '오후 11:59:59', '예매하기 · 11월 12일(목) 오전 12:00에 열려요'],
    ['d', '2026-11-11T15:00:00Z', '2026-11-12T09:00:00Z', '11월 12일(목) 오후 6:00 예매 오픈', '오전 12:00:00', '예매하기 · 오후 6:00에 열려요'],
  ])('SC-09 (happy) 예정 화면의 머리글·기준 시각·꺼진 CTA 문구가 한국 시간으로 보인다 — %s', async (_row, server, openAt, eyebrow, clock, ctaText) => {
    // Given P_UP(상세) 이고 서버 시각·openAt 은 표와 같다
    // When /performances/7 을 연다
    await openUp({ server, perf: { openAt } })

    // Then 표의 eyebrow·기준 시각 줄·CTA 문구가 보인다 (기기 시간대와 무관하다)
    expect(screen.getByText(eyebrow)).toBeInTheDocument()
    expect(screen.getByText(`티켓온 기준 ${clock}${LEAD_TAIL}`)).toBeInTheDocument()
    expect(cta(ctaText)).toBeDisabled()
    // And CTA 는 꺼져 있고 눌러도 주소가 바뀌지 않는다
    await click(cta(ctaText))
    expect(where()).toBe('/performances/7')
  })

  // ---------- SC-10 ----------

  const FAILURES: Array<[string, string, () => Response]> = [
    ['a', '503 + code UNAVAILABLE', () => res(503, { code: 'UNAVAILABLE' })],
    ['b', '503 + 다른 code', () => res(503, { code: 'OTHER' })],
    ['c', '404 + 빈 바디', () => res(404)],
    ['d', '200 + 빈 바디', () => res(200)],
    ['e', '200 + 비 JSON', () => raw(200, '<html>')],
    ['f', '네트워크 오류', () => networkError()],
    ['g', '200 + {}', () => res(200, {})],
    ['h', '200 + now null', () => res(200, { now: null })],
    ['i', '200 + now 숫자', () => res(200, { now: 1731315328000 })],
    ['j', '200 + now 배열', () => res(200, { now: ['2026-11-11T08:55:28Z'] })],
    ['k', '200 + now true', () => res(200, { now: true })],
    ['l', "200 + now '어제'", () => res(200, { now: '어제' })],
  ]
  test.each(FAILURES)('SC-10 (error) 기준 시각을 맞추지 못하면 카운트다운 대신 안내가 보이고 CTA 는 기기 시계로 켜지지 않는다 — %s %s', async (_row, _label, respond) => {
    // Given P_UP(openAt 은 서버 시각 + 272초), GET /api/time 이 세 번 모두 표의 응답을 준다, 상세를 열었다
    await openUp({ time: () => respond() })

    // When 기기 시계를 openAt 보다 5분 더 흘린다
    await settle(272_000 + 5 * 60_000)

    // Then 「기준 시각을 맞추지 못했어요」(머리글)와 「다시 맞추기」 버튼이 보이고 role="timer" 요소는 없다
    expect(h1()).toHaveTextContent(FAIL_TEXT)
    expect(screen.getByRole('button', { name: '다시 맞추기' })).toBeInTheDocument()
    expect(timerOrNull()).not.toBeInTheDocument()
    // And CTA 는 꺼짐 그대로이고 기기 시계가 openAt 을 넘어도 켜지지 않는다
    expect(cta(UP_CTA)).toBeDisabled()
    // And 시각 호출 수는 정확히 3이다 (한 번의 측정), 상세 호출 수는 1이다
    expect(callsTo(TIME)).toHaveLength(3)
    expect(callsTo(DETAIL)).toHaveLength(1)
  })

  // ---------- SC-11 ----------

  const D0 = '2026-11-11T08:55:00.000Z'
  const S = (t: string) => `2026-11-11T${t}Z`
  const fail503 = () => res(503, { code: 'UNAVAILABLE' })
  const SAMPLES: Array<[string, Sample[], number, string, number[]]> = [
    ['a', [{ rtt: 400, now: S('08:55:31.000') }, { rtt: 100, now: S('08:55:30.300') }, { rtt: 300, now: S('08:55:29.000') }], 800, '4분 29초', [0, 400, 500]],
    ['b', [{ rtt: 400, fail: fail503 }, { rtt: 100, now: S('08:55:30.300') }, { rtt: 300, now: S('08:55:29.000') }], 800, '4분 29초', [0, 400, 500]],
    ['c', [{ rtt: 400, now: S('08:55:31.000') }, { rtt: 100, fail: 'network' }, { rtt: 300, now: S('08:55:29.000') }], 800, '4분 30초', [0, 400, 500]],
    ['d', [{ rtt: 400, fail: () => raw(200, '<html>') }, { rtt: 100, fail: fail503 }, { rtt: 300, now: S('08:55:29.000') }], 800, '4분 30초', [0, 400, 500]],
    ['e', [{ rtt: 100, now: S('08:55:31.000') }, { rtt: 100, now: S('08:55:29.000') }, { rtt: 100, now: S('08:55:30.000') }], 300, '4분 28초', [0, 100, 200]],
  ]
  test.each(SAMPLES)('SC-11 (boundary) 왕복이 가장 짧은 표본의 오프셋을 쓰고 일부가 실패해도 성공한 표본으로 센다 — %s', async (_row, samples, total, text, starts) => {
    // Given P_UP, openAt 09:00:00Z, 측정 시작 때 기기 시계는 08:55:00.000Z 다 — 세 번이 표의 (왕복, now)를 순서대로 준다
    const log: number[] = []
    startFakeClock(D0)
    stubServer({ [DETAIL]: () => res(200, P_UP()), [TIME]: timeSamples(samples, log) })
    const t0 = Date.now()

    // When /performances/7 을 열고 세 번의 측정이 모두 끝난 직후를 본다
    renderAt('/performances/7')
    await settle(total)

    // Then 카운트다운 문구가 표의 값이다 — 왕복이 가장 짧은(같으면 먼저 잰) 표본의 오프셋
    expect(timer()).toHaveTextContent(text)
    // And 시각 호출 수는 정확히 3이다 — 두 번째 요청은 첫 응답이 도착한 뒤에 나간다
    expect(callsTo(TIME)).toHaveLength(3)
    expect(log.map((t) => t - t0)).toEqual(starts)
  })

  // ---------- SC-12 ----------

  test.each([
    ['a', 'visible', 6, '4분 29초'],
    ['b', 'hidden', 3, '4분 32초'],
    ['c', 'prerender', 3, '4분 32초'],
  ])('SC-12 (happy) 탭이 다시 보이면 기준 시각을 다시 재서 카운트다운에 반영한다 — %s %s', async (_row, state, expectedCalls, text) => {
    // Given P_UP 이고 SKEW = 3000 으로 측정을 마쳤다 (시각 호출 수 3)
    let skew = 3000
    await openUp({ skew, time: async () => res(200, { now: new Date(Date.now() - skew).toISOString() }) })
    expect(callsTo(TIME)).toHaveLength(3)
    expect(timer()).toHaveTextContent('4분 32초')
    // And 그 뒤 서버 시각 스텁의 SKEW 가 0 으로 바뀌었다
    skew = 0

    // When document.visibilityState 를 표의 값으로 바꾸고 visibilitychange 를 보낸다
    await act(async () => {
      setDocumentVisibility(state as string)
    })
    await settle(0)

    // Then 시각 호출 수는 표의 값이다
    expect(callsTo(TIME)).toHaveLength(expectedCalls as number)
    // And visible 행은 카운트다운이 새 오프셋을 반영한다 — 서버 시각이 3초 앞서 남은 시간이 3초 줄어든다
    expect(timer()).toHaveTextContent(text as string)
  })

  // ---------- SC-13·14 ----------

  test.each([
    ['a', 37, '지금 예매할 수 있어요', '11월 18일(수) 오후 11:59에 마감돼요', '예매하기', true],
    ['b', 0, '모든 좌석이 팔렸어요', null, '매진', false],
    ['c', 1, '지금 예매할 수 있어요', '11월 18일(수) 오후 11:59에 마감돼요', '예매하기', true],
  ])('SC-13 (happy) 기준 시각이 openAt 에 닿으면 다시 부르지 않고 예매 가능으로 바뀐다 — %s', async (_row, seats, header, sub, ctaName, enabled) => {
    // Given GET /api/time 이 서버 시각 = openAt − 5초 로 측정을 마쳤다 (시각 호출 수 3, 상세 호출 수 1)
    await openUp({ server: at(T_OPEN, -5000), perf: { availableSeats: seats } })
    expect(callsTo(TIME)).toHaveLength(3)
    expect(callsTo(DETAIL)).toHaveLength(1)

    // When 서버 시각이 openAt 이 될 때까지 5초를 흘린다
    await settle(5000)

    // Then 표의 머리글·아랫줄·CTA 로 바뀐다
    expect(h1()).toHaveTextContent(header as string)
    if (sub) expect(screen.getByText(sub as string)).toBeInTheDocument()
    else expect(screen.queryByText(/마감돼요/)).not.toBeInTheDocument()
    if (enabled) expect(cta(ctaName as string)).toBeEnabled()
    else expect(cta(ctaName as string)).toBeDisabled()
    // And role="timer" 요소가 사라진다
    expect(timerOrNull()).not.toBeInTheDocument()
    // And 상세 호출 수는 1 그대로이고 시각 호출 수도 3 그대로다 (그 순간 호출 0회 — ADR-0026)
    expect(callsTo(DETAIL)).toHaveLength(1)
    expect(callsTo(TIME)).toHaveLength(3)
  })

  const EARLY = 600_000
  test.each([
    ['a', '진입 때 openAt − 1ms'],
    ['b', '진입 때 openAt'],
    ['c', '진입 때 openAt + 1ms'],
    ['d', '타이머가 openAt − 1ms → openAt'],
    ['e', '숨겨진 사이 지난 뒤 재측정 ≥ openAt'],
    ['f', '재측정 < openAt'],
    ['g', '전환된 뒤 탭이 다시 보임'],
    ['h', 'openAt 에 좌석 0'],
    ['i', 'openAt 이 30일 뒤'],
  ])('SC-14 (boundary) 기준 시각이 openAt 과 같은 순간의 판정이 어느 경로에서든 같다 — %s %s', async (row, _label) => {
    // Given P_UP, availableSeats 37 (행 h 는 0), openAt 2026-11-11T09:00:00Z
    // When 표의 경로로 기준 시각이 표의 값에 놓인다 / Then 표의 화면 상태다
    const seats = row === 'h' ? 0 : 37
    if (row === 'a') {
      await openUp({ server: at(T_OPEN, -1), perf: { availableSeats: seats } })
      expect(timer()).toHaveTextContent('0초')
      expect(cta(UP_CTA)).toBeDisabled()
    } else if (row === 'b' || row === 'c' || row === 'h') {
      await openUp({ server: at(T_OPEN, row === 'c' ? 1 : 0), perf: { availableSeats: seats } })
      expect(timerOrNull()).not.toBeInTheDocument()
      if (row === 'h') {
        expect(h1()).toHaveTextContent('모든 좌석이 팔렸어요')
        expect(cta('매진')).toBeDisabled()
      } else {
        expect(h1()).toHaveTextContent('지금 예매할 수 있어요')
        expect(cta('예매하기')).toBeEnabled()
      }
    } else if (row === 'd') {
      await openUp({ server: at(T_OPEN, -5000) })
      await settle(4999)
      expect(h1()).not.toHaveTextContent('지금 예매할 수 있어요')
      expect(cta(UP_CTA)).toBeDisabled()
      await settle(1)
      expect(h1()).toHaveTextContent('지금 예매할 수 있어요')
      expect(cta('예매하기')).toBeEnabled()
    } else if (row === 'e' || row === 'f') {
      await openUp({ server: at(T_OPEN, -EARLY) })
      expect(callsTo(TIME)).toHaveLength(3)
      // 탭이 숨겨진 사이 기기 시계가 앞으로 갔다(타이머는 돌지 못했다)
      vi.setSystemTime(new Date(Date.now() + (row === 'e' ? EARLY + 60_000 : EARLY / 2)))
      await act(async () => {
        setDocumentVisibility('visible')
      })
      await settle(0)
      if (row === 'e') {
        expect(h1()).toHaveTextContent('지금 예매할 수 있어요')
        expect(timerOrNull()).not.toBeInTheDocument()
      } else {
        expect(timer()).toHaveTextContent('5분 0초')
        expect(cta(UP_CTA)).toBeDisabled()
      }
    } else if (row === 'g') {
      await openUp({ server: at(T_OPEN, -5000) })
      await settle(5000)
      expect(h1()).toHaveTextContent('지금 예매할 수 있어요')
      await act(async () => {
        setDocumentVisibility('visible')
      })
      await settle(0)
      expect(h1()).toHaveTextContent('지금 예매할 수 있어요')
      expect(callsTo(TIME)).toHaveLength(3)
    } else {
      // i — 타이머 지연 상한(2^31−1ms ≈ 24.8일)을 넘는 대기도 곧바로 전환되지 않는다
      await openUp({ server: at(T_OPEN, -30 * DAY) })
      expect(timer()).toHaveTextContent('30일 0시간')
      await settle(5000)
      expect(h1()).not.toHaveTextContent('지금 예매할 수 있어요')
      expect(cta(UP_CTA)).toBeDisabled()
    }
    // And 상태가 바뀌는 행에서도 상세 호출 수는 늘지 않는다
    expect(callsTo(DETAIL)).toHaveLength(1)
  })

  // ---------- SC-05 ----------

  test.each([
    ['a', '8 로 이동 — 7 의 늦은 성공', 'success'],
    ['b-1', '8 로 이동 — 7 의 늦은 500', 'fail0'],
    ['b-2', '8 로 이동 — 7 의 늦은 네트워크 오류', 'fail1'],
    ['b-3', '8 로 이동 — 7 의 늦은 비 JSON', 'fail2'],
    ['c', '/ 로 이동 — 7 의 늦은 성공(P_UP)', 'home'],
    ['d-1', 'StrictMode — 취소된 첫 요청이 먼저 도착', 'strict-first'],
    ['d-2', 'StrictMode — 취소된 첫 요청이 나중에 도착', 'strict-last'],
    ['e', '응답 전 로그인', 'login'],
    ['f', '로그인 상태로 응답 전 로그아웃', 'logout'],
  ])('SC-05 (boundary) 응답이 오기 전에 상황이 바뀌어도 늦게 온 응답이 화면을 덮어쓰지 않는다 — %s %s', async (_row, _label, kind) => {
    // Given /performances/7 의 상세 응답이 아직 오지 않는다 (/performances/8 은 즉시 200)
    startFakeClock(T_SERVER)
    const pending = [deferred(), deferred()]
    let n = 0
    const strict = (kind as string).startsWith('strict')
    if ((kind as string) === 'logout') {
      localStorage.setItem('accessToken', 'tok-1')
    }
    stubServer({
      [DETAIL]: () => pending[Math.min(n++, 1)].promise,
      'GET /api/performances/8': () => res(200, P_OPEN({ id: 8, title: '다른 공연' })),
      'GET /api/performances': () => res(200, { content: [], page: 0, size: 20, totalElements: 0 }),
      'GET /api/auth/me': () => res(200, USER),
      [TIME]: timeRoute(),
    })
    renderAt('/performances/7', { strict })
    await settle(0)

    // When 표의 일이 일어난 뒤 7 의 응답이 도착한다
    // 7 의 늦은 응답 — 실패는 요청을 거부하거나 본문이 깨진 응답으로 도착한다
    const arrive = async () => {
      await act(async () => {
        if (kind === 'fail1') pending[0].reject(new TypeError('Failed to fetch'))
        else if (kind === 'fail0') pending[0].resolve(res(500, { code: 'INTERNAL' }))
        else if (kind === 'fail2') pending[0].resolve(raw(200, '<html>'))
        else pending[0].resolve(res(200, kind === 'home' ? P_UP() : P_OPEN()))
      })
    }
    if (kind === 'success' || (kind as string).startsWith('fail')) {
      await navigateTo('/performances/8')
      await settle(0)
      expect(screen.getByRole('heading', { level: 2, name: '다른 공연' })).toBeInTheDocument()
      await arrive()
    } else if (kind === 'home') {
      await navigateTo('/')
      await settle(0)
      await arrive()
    } else if (strict) {
      const order = kind === 'strict-first' ? [0, 1] : [1, 0]
      for (const i of order) await act(async () => pending[i].resolve(res(200, P_UP())))
    } else {
      if (kind === 'login') await act(async () => authApi().login('tok-1', USER as never))
      else {
        await settle(0)
        await act(async () => authApi().logout())
      }
      await arrive()
    }
    await settle(0)

    // Then 표의 Then 열 그대로다
    if (kind === 'success' || (kind as string).startsWith('fail')) {
      expect(screen.getByRole('heading', { level: 2, name: '다른 공연' })).toBeInTheDocument()
      expect(screen.queryByText(TITLE)).not.toBeInTheDocument()
      expect(screen.queryByText('공연 정보를 불러오지 못했어요')).not.toBeInTheDocument()
    } else if (kind === 'home') {
      expect(screen.queryByText(TITLE)).not.toBeInTheDocument()
      expect(timerOrNull()).not.toBeInTheDocument()
    } else if (strict) {
      expect(screen.getAllByRole('timer')).toHaveLength(1)
    } else {
      expect(h1()).toHaveTextContent('지금 예매할 수 있어요')
      expect(callsTo(DETAIL)).toHaveLength(1)
    }
    // And 화면이 바뀐 뒤에도 늦은 응답 때문에 시각 호출이 생기지 않는다 (시각 호출 수 0 — StrictMode 의 P_UP 행은 제외)
    if (!strict) expect(callsTo(TIME)).toHaveLength(0)
  })

  // ---------- SC-25·26 ----------

  test.each([
    ['a', 'P_OPEN 으로 처음부터 예매 가능'],
    ['b', 'P_UP 에서 기준 시각이 openAt 에 닿아 전환된 뒤'],
  ])('SC-25 (happy) 켜진 「예매하기」를 누르면 대기열 임시 화면으로 간다 — %s %s', async (row, _label) => {
    // Given CTA 가 켜진 상세 화면이다
    if (row === 'a') {
      startFakeClock(T_SERVER)
      stubServer({ [DETAIL]: () => res(200, P_OPEN()) })
      renderAt('/performances/7')
      await settle(0)
    } else {
      await openUp({ server: at(T_OPEN, -5000) })
      await settle(5000)
    }
    const detailCalls = callsTo(DETAIL).length

    // When 「예매하기」를 누른다
    await click(cta('예매하기'))

    // Then 주소가 /performances/7/queue 다
    expect(where()).toBe('/performances/7/queue')
    // And h1 「대기열을 준비하고 있어요」와 링크 「공연으로 돌아가기」, 앱바 「뒤로」 버튼이 보인다
    expect(h1()).toHaveTextContent('대기열을 준비하고 있어요')
    expect(screen.getByRole('link', { name: '공연으로 돌아가기' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '뒤로' })).toBeInTheDocument()
    // And 전역 헤더가 없다
    expect(hasGlobalHeader()).toBe(false)
    // And 상세 호출 수가 늘지 않는다
    expect(callsTo(DETAIL)).toHaveLength(detailCalls)
  })

  test.each([
    ['a', 'P_UP — 카운트다운 중'],
    ['b', 'P_UP — 기준 시각 실패, 기기 시계는 openAt 을 넘었다'],
    ['c', 'P_SOLD'],
    ['d', 'P_CLOSED'],
    ['e', 'P_CANCEL'],
  ])('SC-26 (error) 꺼진 CTA 는 눌러도 이동하지 않고 취소된 공연에는 CTA 가 없다 — %s %s', async (row, _label) => {
    // Given 상세 화면이 표의 상태다
    const bodies = { c: P_SOLD(), d: P_CLOSED(), e: P_CANCEL() } as Record<string, Record<string, unknown>>
    if (row === 'a') await openUp()
    else if (row === 'b') {
      await openUp({ time: () => networkError() })
      await settle(272_000 + 60_000)
    } else {
      startFakeClock(T_SERVER)
      stubServer({ [DETAIL]: () => res(200, bodies[row]) })
      renderAt('/performances/7')
      await settle(0)
    }

    // When CTA 를 누른다 (없으면 CTA 를 찾는다)
    const name = row === 'c' ? '매진' : row === 'd' ? '예매 마감' : UP_CTA
    if (row !== 'e') await click(cta(name))

    // Then 주소는 /performances/7 그대로이고 「대기열을 준비하고 있어요」가 나타나지 않는다
    expect(where()).toBe('/performances/7')
    expect(screen.queryByText('대기열을 준비하고 있어요')).not.toBeInTheDocument()
    // And 행 e 는 예매하기·매진·예매 마감 어느 CTA 버튼도 없다
    if (row === 'e') expect(screen.queryByRole('button', { name: /예매하기|매진|예매 마감/ })).not.toBeInTheDocument()
  })

  // ---------- SC-30 ----------

  test.each([
    ['a', '첫 측정의 첫 응답 전에 홈으로 이동'],
    ['b', '재측정 중 또 재측정(구세대의 늦은 응답은 +60초)'],
    ['c-1', '신세대 성공 뒤 구세대의 늦은 503'],
    ['c-2', '신세대 성공 뒤 구세대의 늦은 네트워크 오류'],
    ['c-3', '신세대 성공 뒤 구세대의 늦은 비 JSON'],
    ['d', '측정을 마친 뒤 재측정이 세 번 모두 실패'],
    ['e', '측정 도중 로그인'],
    ['f', 'StrictMode'],
  ])('SC-30 (boundary) 기준 시각을 재는 도중 화면이 바뀌어도 오래된 결과가 상태를 덮어쓰지 않는다 — %s %s', async (row, _label) => {
    // Given P_UP 상세에서 표의 일이 일어난다
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
    const consoleWarn = vi.spyOn(console, 'warn').mockImplementation(() => {})
    let n = 0
    const lateFail: Record<string, () => Response> = { 'c-1': fail503, 'c-2': () => networkError(), 'c-3': () => raw(200, '<html>') }
    const route: Responder = async () => {
      const i = n++
      if (row === 'a') {
        await new Promise((r) => setTimeout(r, 1000))
      } else if (row === 'b' || row.startsWith('c')) {
        if (i === 3) {
          await new Promise((r) => setTimeout(r, 2000))
          if (row === 'b') return res(200, { now: new Date(Date.now() + 60_000).toISOString() })
          return lateFail[row]()
        }
      } else if (row === 'd') {
        if (i >= 3) return networkError()
      } else if (row === 'e') {
        await new Promise((r) => setTimeout(r, 500))
      }
      return res(200, { now: new Date(Date.now()).toISOString() })
    }
    startFakeClock(T_SERVER)
    stubServer({
      [DETAIL]: () => res(200, P_UP()),
      [TIME]: route,
      'GET /api/performances': () => res(200, { content: [], page: 0, size: 20, totalElements: 0 }),
    })
    renderAt('/performances/7', { strict: row === 'f' })

    // When 표의 일이 일어난 뒤 해당 시각 응답들이 도착한다
    if (row === 'a') {
      await settle(0)
      await navigateTo('/')
      await settle(5000)
    } else if (row === 'b' || row.startsWith('c')) {
      await settle(0)
      await act(async () => setDocumentVisibility('hidden'))
      await act(async () => setDocumentVisibility('visible')) // 재측정 #1 — 첫 응답이 2초 걸린다
      await settle(0)
      await act(async () => setDocumentVisibility('hidden'))
      await act(async () => setDocumentVisibility('visible')) // 재측정 #2 — 빠르다
      await settle(2000)
    } else if (row === 'd') {
      await settle(0)
      await act(async () => setDocumentVisibility('visible'))
      await settle(0)
    } else if (row === 'e') {
      await settle(600)
      await act(async () => authApi().login('tok-1', USER as never))
      await settle(1000)
    } else {
      await settle(0)
    }

    // Then 표의 Then 이다
    if (row === 'a') {
      // 첫 측정의 첫 호출만 나갔고 그 뒤로는 더 나가지 않는다
      expect(callsTo(TIME)).toHaveLength(1)
      expect(timerOrNull()).not.toBeInTheDocument()
      expect(screen.queryByText(TITLE)).not.toBeInTheDocument()
    } else if (row === 'b') {
      // 구세대(+60초)가 쓰였다면 3분 30초 — 나중에 시작한 재측정만 쓰이면 4분 30초
      expect(timer()).toHaveTextContent('4분 30초')
    } else if (row.startsWith('c')) {
      expect(screen.queryByText(FAIL_TEXT)).not.toBeInTheDocument()
      expect(timer()).toBeInTheDocument()
    } else if (row === 'd') {
      expect(screen.queryByText(FAIL_TEXT)).not.toBeInTheDocument()
      expect(timer()).toHaveTextContent('4분 32초')
      await settle(1000)
      expect(timer()).toHaveTextContent('4분 31초')
    } else if (row === 'e') {
      expect(callsTo(TIME)).toHaveLength(3)
      for (const call of callsTo(TIME)) expect(new Headers(call.init.headers).has('Authorization')).toBe(false)
      expect(timer()).toBeInTheDocument()
    } else {
      expect(screen.getAllByRole('timer')).toHaveLength(1)
      await settle(272_000)
      expect(h1()).toHaveTextContent('지금 예매할 수 있어요')
      expect(timerOrNull()).not.toBeInTheDocument()
    }
    // And 콘솔 오류·경고(상태 갱신 경고 포함)가 0건이다
    expect(consoleError).not.toHaveBeenCalled()
    expect(consoleWarn).not.toHaveBeenCalled()
  })

  // ---------- SC-34 ----------

  test.each([
    ['a', '정상 now', true],
    ['b', '세 번 모두 다시 실패', false],
  ])('SC-34 (happy) 「다시 맞추기」를 누르면 기준 시각을 다시 재고 성공하면 카운트다운이 돌아온다 — %s %s', async (_row, _label, healthy) => {
    // Given P_UP 이고 시각 호출 세 번이 모두 실패해 「기준 시각을 맞추지 못했어요」가 보인다 (시각 호출 수 3)
    let ok = false
    await openUp({ time: async () => (ok ? res(200, { now: new Date().toISOString() }) : networkError()) })
    expect(h1()).toHaveTextContent(FAIL_TEXT)
    expect(callsTo(TIME)).toHaveLength(3)
    ok = healthy as boolean

    // When 「다시 맞추기」를 누른다
    await click(screen.getByRole('button', { name: '다시 맞추기' }))

    // Then 시각 호출 수는 3 늘어난다
    expect(callsTo(TIME)).toHaveLength(6)
    // And 표의 결과가 보인다
    if (healthy) {
      expect(screen.queryByText(FAIL_TEXT)).not.toBeInTheDocument()
      expect(timer()).toHaveTextContent('4분 32초')
    } else {
      expect(h1()).toHaveTextContent(FAIL_TEXT)
      expect(screen.getByRole('button', { name: '다시 맞추기' })).toBeInTheDocument()
    }
    expect(cta(UP_CTA)).toBeDisabled()
    // And 상세 호출 수는 1 그대로다
    expect(callsTo(DETAIL)).toHaveLength(1)
  })
})
