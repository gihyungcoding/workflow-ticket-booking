import { act, fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
  DETAIL,
  P_CANCEL,
  P_CLOSED,
  P_OPEN,
  P_SOLD,
  P_UP,
  SECTIONS_P,
  TIME,
  TITLE,
  callsTo,
  calls,
  css,
  deferred,
  isTokenColor,
  jsonWithInfinity,
  networkError,
  raw,
  renderAt,
  res,
  resetDocumentVisibility,
  squash,
  stubServer,
  where,
} from '../test/task020'
import type { Perf, Section } from '../test/task020'

// TASK-020 시나리오 — 공연 상세(실시간 타이머). SoT: workflow_design/05_scenario/SCENARIO_TASK-020.md
// 표로 묶은 시나리오는 test.each 한 줄(= 시나리오 한 건)이고 행마다 케이스가 하나씩 돈다.
// 시간이 걸린 시나리오(카운트다운·재측정)는 task020.clock.test.tsx 가 가짜 타이머로 다룬다.
// 포스터 칸은 data-testid="poster" 를 단다고 약속한다(SC-17·33).

const ERROR_TEXT = '공연 정보를 불러오지 못했어요'
const NOT_FOUND_TEXT = '찾는 공연이 없어요'

beforeEach(() => {
  localStorage.clear()
})
afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  resetDocumentVisibility()
  localStorage.clear()
})

describe('TASK-020 — 공연 상세 (실시간 타이머)', () => {
  test('SC-01 (happy) OPEN 공연 상세가 로딩 뒤에 시안 구조로 보인다', async () => {
    // Given P_OPEN 의 상세 응답이 아직 오지 않는다
    const pending = deferred()
    stubServer({ [DETAIL]: () => pending.promise })

    // When /performances/7 을 연다
    renderAt('/performances/7')

    // Then 응답 전에는 공연명과 「예매하기」가 없고 앱바의 「뒤로」 버튼은 있다
    expect(screen.queryByText(TITLE)).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '예매하기' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: '뒤로' })).toBeInTheDocument()
    // And 응답 뒤 h1 은 「지금 예매할 수 있어요」, 아랫줄은 마감 안내, h2 는 공연명, CTA 는 켜진 「예매하기」다
    await act(async () => {
      pending.resolve(res(200, P_OPEN()))
    })
    expect(await screen.findByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })).toBeInTheDocument()
    expect(screen.getByText('11월 18일(수) 오후 11:59에 마감돼요')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: TITLE })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '예매하기' })).toBeEnabled()
    // And 상세 호출 수는 1, 시각 호출 수는 0이다
    expect(callsTo(DETAIL)).toHaveLength(1)
    expect(callsTo(TIME)).toHaveLength(0)
  })

  test.each([
    ['a', '404 + code PERFORMANCE_NOT_FOUND', () => res(404, { code: 'PERFORMANCE_NOT_FOUND' })],
    ['b', '404 + 다른 code', () => res(404, { code: 'SOMETHING_ELSE' })],
    ['c', '404 + 빈 바디', () => res(404)],
    ['d', '404 + 비 JSON 본문', () => raw(404, '<html>Not Found</html>')],
  ])('SC-02 (error) 없는 공연이면 「찾는 공연이 없어요」와 「홈으로」가 보인다 — %s %s', async (_row, _label, respond) => {
    // Given GET /api/performances/999 가 표의 응답을 준다
    stubServer({ 'GET /api/performances/999': respond })

    // When /performances/999 를 연다
    renderAt('/performances/999')

    // Then 「찾는 공연이 없어요」와 「홈으로」가 보이고 「공연 정보를 불러오지 못했어요」는 없다
    expect(await screen.findByText(NOT_FOUND_TEXT)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '홈으로' })).toBeInTheDocument()
    expect(screen.queryByText(ERROR_TEXT)).not.toBeInTheDocument()
    // And 「홈으로」를 누르면 주소가 / 다
    await userEvent.click(screen.getByRole('link', { name: '홈으로' }))
    await waitFor(() => expect(where()).toBe('/'))
  })

  test.each([
    ['a', '500 + code INTERNAL', () => res(500, { code: 'INTERNAL' })],
    ['b', '500 + 다른 code', () => res(500, { code: 'OTHER' })],
    ['c', '503 + 빈 바디', () => res(503)],
    ['d', '502 + 비 JSON 본문', () => raw(502, '<html>Bad Gateway</html>')],
    ['e', '200 + 빈 바디', () => res(200)],
    ['f', '200 + 비 JSON 본문', () => raw(200, '<html>ok</html>')],
    ['g', '네트워크 오류', () => networkError()],
  ])('SC-03 (error) 그 밖의 실패면 「공연 정보를 불러오지 못했어요」와 「다시 시도」가 보인다 — %s %s', async (_row, _label, respond) => {
    // Given 상세 응답이 표처럼 실패한다
    stubServer({ [DETAIL]: respond })

    // When /performances/7 을 연다
    renderAt('/performances/7')

    // Then 「공연 정보를 불러오지 못했어요」와 「다시 시도」가 보이고 「찾는 공연이 없어요」는 없다
    expect(await screen.findByText(ERROR_TEXT)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeInTheDocument()
    expect(screen.queryByText(NOT_FOUND_TEXT)).not.toBeInTheDocument()
    // And 앱바의 「뒤로」 버튼은 그대로 있다
    expect(screen.getByRole('button', { name: '뒤로' })).toBeInTheDocument()
    // And 시각 호출 수는 0이다
    expect(callsTo(TIME)).toHaveLength(0)
  })

  // ---------- SC-04 — 계약 위반 ----------

  const first = (over: Record<string, unknown>) => (p: Perf): Perf => ({
    ...p,
    sections: [{ ...SECTIONS_P[0], ...over }, ...SECTIONS_P.slice(1)],
  })
  const firstWithout = (key: keyof Section) => (p: Perf): Perf => {
    const { [key]: _removed, ...rest } = SECTIONS_P[0]
    return { ...p, sections: [rest, ...SECTIONS_P.slice(1)] }
  }
  const top = (over: Perf) => (p: Perf): Perf => ({ ...p, ...over })
  const topWithout = (key: string) => (p: Perf): Perf => {
    const { [key]: _removed, ...rest } = p
    return rest
  }
  const INF = '__INF__'
  const VIOLATIONS: Array<[string, string, (p: Perf) => Perf]> = [
    ['a', 'status 키 없음', topWithout('status')],
    ['b', 'status null', top({ status: null })],
    ['c', "status 'PAUSED'", top({ status: 'PAUSED' })],
    ['d', 'status 1', top({ status: 1 })],
    ['e', 'openAt 키 없음', topWithout('openAt')],
    ['f', 'openAt null', top({ openAt: null })],
    ['g', 'openAt 숫자', top({ openAt: 1731315600000 })],
    ['h', "openAt '내일'", top({ openAt: '내일' })],
    ['i', 'closeAt 키 없음', topWithout('closeAt')],
    ['j', 'closeAt null', top({ closeAt: null })],
    ['k', 'closeAt 배열', top({ closeAt: ['2026-11-18T14:59:00Z'] })],
    ['l', 'availableSeats 키 없음', topWithout('availableSeats')],
    ['m', 'availableSeats null', top({ availableSeats: null })],
    ['n', "availableSeats '37'", top({ availableSeats: '37' })],
    ['o', 'availableSeats -1', top({ availableSeats: -1 })],
    ['p', 'availableSeats 1.9', top({ availableSeats: 1.9 })],
    ['q', 'availableSeats 1e400', top({ availableSeats: INF })],
    ['r', "sections 'x'", top({ sections: 'x' })],
    ['s', 'sections {}', top({ sections: {} })],
    ['t', 'sections [null]', top({ sections: [null] })],
    ['u', 'sections [첫 원소, null]', top({ sections: [SECTIONS_P[0], null] })],
    ['v', 'price 키 없음', firstWithout('price')],
    ['w', 'price null', first({ price: null })],
    ['x', "price '150000'", first({ price: '150000' })],
    ['y', 'price -1', first({ price: -1 })],
    ['z', 'price 1.9', first({ price: 1.9 })],
    ['aa', 'price 1e400', first({ price: INF })],
    ['ab', 'stageOrder 키 없음', firstWithout('stageOrder')],
    ['ac', 'stageOrder null', first({ stageOrder: null })],
    ['ad', 'stageOrder 1.5', first({ stageOrder: 1.5 })],
    ['ae', 'stageOrder 1e400', first({ stageOrder: INF })],
    ['af', 'grade 키 없음', firstWithout('grade')],
    ['ag', 'grade null', first({ grade: null })],
    ['ah', "grade ''", first({ grade: '' })],
    ['ai', "grade '   '", first({ grade: '   ' })],
    ['aj', 'grade 전각 공백', first({ grade: '　' })],
    ['ak', 'grade NBSP', first({ grade: ' ' })],
    ['al', 'grade 제로폭 공백', first({ grade: '​' })],
    ['am', 'area 키 없음', firstWithout('area')],
    ['an', 'area null', first({ area: null })],
    ['ao', "area ''", first({ area: '' })],
    ['ap', "area '   '", first({ area: '   ' })],
    ['aq', 'area 전각 공백', first({ area: '　' })],
    ['ar', 'area NBSP', first({ area: ' ' })],
    ['as', 'area 제로폭 공백', first({ area: '​' })],
    ['at', "closeAt '내일'", top({ closeAt: '내일' })],
    ['au', 'startAt 키 없음', topWithout('startAt')],
    ['av', 'startAt null', top({ startAt: null })],
    ['aw', "startAt '내일'", top({ startAt: '내일' })],
    // 시각은 ISO 순간(…T…Z 또는 ±hh:mm)만 받는다 — Date.parse 가 받아 주는 값도 계약 위반이다 (RC-4)
    ...(['openAt', 'closeAt', 'startAt'] as const).flatMap((field) =>
      (
        [
          ['ax', "'1'", '1'],
          ['ay', "'2026'", '2026'],
          ['az', "날짜만 '2026-11-11'", '2026-11-11'],
          ['ba', "오프셋 없음 '2026-11-11T09:00:00'", '2026-11-11T09:00:00'],
        ] as const
      ).map(([row, label, value]): [string, string, (p: Perf) => Perf] => [`${row}·${field}`, `${field} ${label}`, top({ [field]: value })]),
    ),
    ['bb', 'title 키 없음', topWithout('title')],
    ['bc', 'title null', top({ title: null })],
    ['bd', "title ''", top({ title: '' })],
    ['be', "title '   '", top({ title: '   ' })],
    ['bf', 'title 전각 공백', top({ title: '　' })],
    ['bg', 'title 제로폭 공백', top({ title: '​' })],
    ['bh', 'title 123', top({ title: 123 })],
    ['bi', 'venue 키 없음', topWithout('venue')],
    ['bj', 'venue null', top({ venue: null })],
    ['bk', "venue ''", top({ venue: '' })],
    ['bl', "venue '   '", top({ venue: '   ' })],
    ['bm', 'venue 전각 공백', top({ venue: '　' })],
    ['bn', 'venue 제로폭 공백', top({ venue: '​' })],
    ['bo', 'venue 123', top({ venue: 123 })],
    ['bp', 'totalSeats 키 없음', topWithout('totalSeats')],
    ['bq', 'totalSeats null', top({ totalSeats: null })],
    ['br', "totalSeats '21000'", top({ totalSeats: '21000' })],
    ['bs', 'totalSeats -1', top({ totalSeats: -1 })],
    ['bt', 'totalSeats 1.9', top({ totalSeats: 1.9 })],
    ['bu', 'totalSeats 1e400', top({ totalSeats: INF })],
    ['bv', 'totalSeats 100·availableSeats 101', top({ totalSeats: 100, availableSeats: 101 })],
  ]

  test.each(VIOLATIONS)('SC-04 (error) 응답이 계약을 어기면 화면이 깨지지 않고 오류 화면이 된다 — %s %s', async (_row, _label, mutate) => {
    // Given GET /api/performances/7 이 200 으로 P_UP 에서 표의 값만 바꾼 본문을 준다
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
    const body = jsonWithInfinity(mutate(P_UP()))
    stubServer({ [DETAIL]: () => res(200, body) })

    // When /performances/7 을 연다
    renderAt('/performances/7')

    // Then 「공연 정보를 불러오지 못했어요」와 「다시 시도」가 보인다
    expect(await screen.findByText(ERROR_TEXT)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeInTheDocument()
    // And 화면이 비지 않고 예외로 멈추지 않는다 (콘솔 오류 0건)
    expect(document.body.textContent).not.toBe('')
    expect(consoleError).not.toHaveBeenCalled()
    // And 시각 호출 수는 0이다
    expect(callsTo(TIME)).toHaveLength(0)
  })

  // ---------- SC-06 ----------

  test.each([
    ['a', '/performances/%207', '/api/performances/%207'],
    ['b', '/performances/7%20', '/api/performances/7%20'],
    ['c', '/performances/7%0A', '/api/performances/7%0A'],
    ['d', '/performances/7%00', '/api/performances/7%00'],
    ['e', '/performances/7%3Fx%3D1', '/api/performances/7%3Fx%3D1'],
    ['f', '/performances/7%23x', '/api/performances/7%23x'],
    ['g', '/performances/7%2F1', '/api/performances/7%2F1'],
    ['h', '/performances/%ED%95%9C', '/api/performances/%ED%95%9C'],
    ['i', '/performances/%E0%A4%A', '/api/performances/%25E0%25A4%25A'],
    ['j', '/performances/7%2F1/queue', '/performances/7%2F1'],
    ['k', '/performances/%ED%95%9C/queue', '/performances/%ED%95%9C'],
  ])('SC-06 (boundary) 공연 번호에 특수 문자가 있어도 요청 경로가 갈라지지 않고 없는 공연으로 보인다 — %s %s', async (row, address, expected) => {
    // Given 서버는 /api/performances/ 로 시작하는 알 수 없는 경로에 404 를 준다
    vi.spyOn(console, 'warn').mockImplementation(() => {})
    stubServer({})

    // When 표의 주소를 연다
    renderAt(address)

    if (row === 'j' || row === 'k') {
      // Then 대기열 임시 화면의 「공연으로 돌아가기」 링크와 앱바 「뒤로」 가 가리키는 주소는 표의 돌아갈 주소와 같다
      const link = await screen.findByRole('link', { name: '공연으로 돌아가기' })
      expect(link.getAttribute('href')).toBe(expected)
      await userEvent.click(screen.getByRole('button', { name: '뒤로' }))
      await waitFor(() => expect(where()).toBe(expected))
    } else {
      // Then 상세 요청의 주소(문자열 전체)가 표의 요청 경로와 정확히 같다 — 쿼리·조각·경로 단계가 새로 생기지 않는다
      expect(await screen.findByText(NOT_FOUND_TEXT)).toBeInTheDocument()
      const detailCalls = calls.filter((c) => c.url.startsWith('/api/performances/'))
      expect(detailCalls.map((c) => c.url)).toEqual([expected])
      // And 상세 화면은 「찾는 공연이 없어요」를 보인다
      expect(screen.getByRole('link', { name: '홈으로' })).toBeInTheDocument()
    }
  })

  // ---------- SC-15 ----------

  test.each([
    ['a', 'OPEN / 37', () => P_OPEN(), '지금 예매할 수 있어요', '11월 18일(수) 오후 11:59에 마감돼요', '예매하기', true, '예매 가능', 0],
    ['b', 'SOLD_OUT / 0', () => P_SOLD(), '모든 좌석이 팔렸어요', null, '매진', false, '매진', 1],
    ['c', 'CLOSED / 0', () => P_CLOSED(), '예매가 마감됐어요', null, '예매 마감', false, '마감', 0],
    ['d', 'CANCELLED / 37', () => P_CANCEL(), '공연이 취소됐어요', null, null, false, '공연 취소', 0],
  ])('SC-15 (happy) 서버가 준 상태가 머리글·아랫줄·CTA 로 보이고 배지는 없다 — %s %s', async (_row, _label, body, header, sub, ctaName, enabled, plain, plainCount) => {
    // Given 상세 응답이 표의 상태·좌석이다
    stubServer({ [DETAIL]: () => res(200, body()) })

    // When /performances/7 을 연다
    renderAt('/performances/7')

    // Then 표의 머리글(h1)·아랫줄·CTA 다
    expect(await screen.findByRole('heading', { level: 1, name: header })).toBeInTheDocument()
    if (sub) expect(screen.getByText(sub)).toBeInTheDocument()
    else expect(screen.queryByText(/마감돼요/)).not.toBeInTheDocument()
    if (ctaName) {
      const button = screen.getByRole('button', { name: ctaName })
      if (enabled) expect(button).toBeEnabled()
      else expect(button).toBeDisabled()
    } else {
      expect(screen.queryByRole('button', { name: /예매하기|매진|예매 마감/ })).not.toBeInTheDocument()
    }
    // And 시각 호출 수는 0이다
    expect(callsTo(TIME)).toHaveLength(0)
    // And 표의 「글자만 가진 요소」 개수가 그대로다 (상태 배지를 그리지 않는다)
    expect(screen.queryAllByText(plain as string)).toHaveLength(plainCount as number)
  })

  // ---------- SC-16 ----------

  test('SC-16 (happy) 공연 정보가 시안 구조로 보인다', async () => {
    // Given P_OPEN (sections 는 P 의 것)
    stubServer({ [DETAIL]: () => res(200, P_OPEN()) })

    // When /performances/7 을 열고 응답이 온다
    renderAt('/performances/7')
    await screen.findByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })

    // Then 이름이 「<공연명> 포스터」 인 이미지의 src 가 posterUrl 이다
    expect(screen.getByRole('img', { name: `${TITLE} 포스터` })).toHaveAttribute('src', 'https://cdn.example.com/luminous.jpg')
    // And 장소·좌석 수, 공연명, 가격, 공연 일시, 1인 최대 장수가 보인다
    expect(screen.getByText('고척스카이돔 · 21,000석')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: TITLE })).toBeInTheDocument()
    expect(screen.getByText('110,000~150,000원')).toBeInTheDocument()
    expect(screen.getByText('12월 19일(토) 오후 6:00')).toBeInTheDocument()
    expect(screen.getByText('1인 최대 4장')).toBeInTheDocument()
    // And 「열리면 이 순서로 골라요」 아래에 네 단계와 설명이 이 순서로 있다
    const steps = screen.getByRole('heading', { level: 2, name: '열리면 이 순서로 골라요' })
    const labels = ['1 · 회차', '2 · 구역', '3 · 좌석', '4 · 확인']
    const bodies = ['회차와 매수를 함께 골라요', '고른 매수만큼 붙어 남은 곳을 구역마다 보여 줘요', '시작 자리 하나를 누르면 옆자리까지 한 번에 잡혀요', '7분 안에 확인하고 무통장 입금으로 예약해요']
    const ordered = [steps, ...labels.flatMap((l, i) => [screen.getByText(l), screen.getByText(bodies[i])])]
    for (let i = 1; i < ordered.length; i += 1) {
      expect(ordered[i - 1].compareDocumentPosition(ordered[i]) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    }
    // And 「구역 위치 보기」 행이 있고 그 아랫줄은 「F1 무대 바로 앞 → F4 맨 뒤」다
    expect(screen.getByText('구역 위치 보기')).toBeInTheDocument()
    expect(screen.getByText('F1 무대 바로 앞 → F4 맨 뒤')).toBeInTheDocument()
    // And 이번에 두지 않기로 한 것이 없다
    expect(screen.queryByText(/1회 12월|2회 12월/)).not.toBeInTheDocument()
    expect(screen.queryByText(/오픈 5분 전부터는/)).not.toBeInTheDocument()
    expect(screen.queryByText(/잔여 좌석/)).not.toBeInTheDocument()
    expect(screen.queryAllByText('예매 가능')).toHaveLength(0)
  })

  // ---------- SC-17 ----------

  const POSTER = 'https://cdn.example.com/luminous.jpg'
  test.each([
    ['a', POSTER],
    ['b', undefined],
    ['c', null],
    ['d', ''],
    ['e', '   '],
    ['f', POSTER], // 이미지에서 error 이벤트가 난다
    ['g', 123],
    ['h', {}],
    ['i', 'javascript:alert(1)'],
    ['j', 'data:image/png;base64,AAAA'],
    ['k', 'ftp://cdn.example.com/luminous.jpg'],
    ['l', 'http://cdn.example.com/luminous.jpg'], // http 는 이미지로 보인다
  ])('SC-17 (boundary) 포스터가 없거나 불러오지 못하면 같은 크기의 회색 면과 아이콘이 보인다 — %s', async (row, posterUrl) => {
    // Given P_OPEN 이고 posterUrl 이 표와 같다
    const body = P_OPEN()
    if (posterUrl === undefined) delete body.posterUrl
    else body.posterUrl = posterUrl
    stubServer({ [DETAIL]: () => res(200, body) })

    // When /performances/7 을 열고 표의 조건을 만든다
    renderAt('/performances/7')
    await screen.findByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })
    if (row === 'f') {
      fireEvent.error(screen.getByRole('img', { name: `${TITLE} 포스터` }))
    }

    // Then 이름이 「<공연명> 포스터」 인 이미지 역할 요소가 하나 있다
    const poster = screen.getByRole('img', { name: `${TITLE} 포스터` })
    // And 포스터 칸의 aspect-ratio 는 4 / 3, 배경색은 --c-bg, 모서리는 --r-m 이다 — 포스터가 있을 때와 같다
    const frame = screen.getByTestId('poster')
    expect(squash(css(frame, 'aspect-ratio'))).toBe('4/3')
    expect(isTokenColor(css(frame, 'background-color'), '--c-bg')).toBe(true)
    // And 포스터가 없는 행은 그 요소가 <img> 가 아니라 아이콘 면이다
    if (row === 'a' || row === 'l') expect(poster.tagName).toBe('IMG')
    else expect(poster.tagName).not.toBe('IMG')
  })

  // ---------- SC-43 ----------

  test.each([
    ['a', 'seatCount 키 없음', firstWithout('seatCount')],
    ['b', 'seatCount null', first({ seatCount: null })],
    ['c', "seatCount '100'", first({ seatCount: '100' })],
    ['d', 'seatCount -1', first({ seatCount: -1 })],
    ['e', '계약에 없는 키 extra', top({ extra: 1 })],
    ['f', 'totalSeats 0·availableSeats 0 (status OPEN)', top({ totalSeats: 0, availableSeats: 0 })],
  ])('SC-43 (boundary) 화면이 쓰지 않는 필드가 이상해도 상세 화면은 그대로 보이고, 좌석 수가 0 이어도 계약 위반이 아니다 — %s %s', async (row, _label, mutate) => {
    // Given GET /api/performances/7 이 200 으로 P_OPEN 에서 표의 값만 바꾼 본문을 준다
    stubServer({ [DETAIL]: () => res(200, mutate(P_OPEN())) })

    // When /performances/7 을 연다
    renderAt('/performances/7')

    // Then 오류 화면이 아니라 상세 화면이 보인다 (머리글 「지금 예매할 수 있어요」)
    expect(await screen.findByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })).toBeInTheDocument()
    expect(screen.queryByText(ERROR_TEXT)).not.toBeInTheDocument()
    // And a~e: 구역 행이 그대로 보인다 / f: 「고척스카이돔 · 0석」이 보인다
    if (row === 'f') expect(screen.getByText('고척스카이돔 · 0석')).toBeInTheDocument()
    else expect(screen.getByText('구역 위치 보기')).toBeInTheDocument()
  })

  // ---------- SC-18 ----------

  const one = (over: Partial<Section> = {}): Section => ({ ...SECTIONS_P[0], ...over })
  test.each([
    ['a', 'sections 키 없음', undefined, false, false],
    ['b', 'sections null', null, false, false],
    ['c', 'sections []', [], false, false],
    ['d', '원소 하나', [one()], true, false],
    ['e', '원소 둘(같은 area)', [one(), one({ grade: 'F2', stageOrder: 2 })], true, true],
  ])('SC-18 (boundary) 구역 정보가 없거나 구역이 하나면 가격 줄·「구역 위치 보기」 행을 숨긴다 — %s %s', async (_row, _label, sections, priceLine, zoneRow) => {
    // Given P_OPEN 에서 sections 만 표처럼 바꾼다
    const body = P_OPEN()
    if (sections === undefined) delete body.sections
    else body.sections = sections
    stubServer({ [DETAIL]: () => res(200, body) })

    // When /performances/7 을 연다
    renderAt('/performances/7')

    // Then 공연명·일시·4단계 안내·켜진 「예매하기」는 그대로 보인다 (오류 화면이 아니다)
    expect(await screen.findByRole('heading', { level: 2, name: TITLE })).toBeInTheDocument()
    expect(screen.getByText('12월 19일(토) 오후 6:00')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: '열리면 이 순서로 골라요' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '예매하기' })).toBeEnabled()
    expect(screen.queryByText(ERROR_TEXT)).not.toBeInTheDocument()
    // And 표의 가격 줄·「구역 위치 보기」 행이다
    if (priceLine) expect(screen.getByText('150,000원')).toBeInTheDocument()
    else expect(screen.queryByText(/^[\d,]+(~[\d,]+)?원$/)).not.toBeInTheDocument()
    if (zoneRow) expect(screen.getByText('구역 위치 보기')).toBeInTheDocument()
    else expect(screen.queryByText('구역 위치 보기')).not.toBeInTheDocument()
  })

  // ---------- SC-35 ----------

  const priced = (prices: number[]): Section[] =>
    prices.map((price, i) => ({ grade: `G${i + 1}`, price, seatCount: 10, area: `A${i + 1}`, stageOrder: i + 1 }))
  test.each([
    ['a', [150000, 150000, 150000, 150000, 132000, 110000], '110,000~150,000원'],
    ['b', [150000], '150,000원'],
    ['c', [150000, 150000], '150,000원'],
    ['d', [5000, 3000], '3,000~5,000원'],
    ['e', [0], '0원'],
    ['f', [0, 1000], '0~1,000원'],
    ['g', [1000000], '1,000,000원'],
  ])('SC-35 (boundary) 가격은 최솟값~최댓값으로, 한 가격이면 한 값으로 천 단위 표기된다 — %s', async (_row, prices, line) => {
    // Given P_OPEN 에서 sections 의 price 만 표처럼 바꾼다 (응답 순서는 표 그대로)
    stubServer({ [DETAIL]: () => res(200, P_OPEN({ sections: priced(prices) })) })

    // When /performances/7 을 연다
    renderAt('/performances/7')

    // Then 가격 줄이 표의 문구다
    expect(await screen.findByText(line)).toBeInTheDocument()
  })

  // ---------- SC-36 ----------

  test.each([
    ['a', '200 P_OPEN', () => res(200, P_OPEN()), '지금 예매할 수 있어요'],
    ['b', '또 500', () => res(500, { code: 'INTERNAL' }), ERROR_TEXT],
    ['c', '404', () => res(404, { code: 'PERFORMANCE_NOT_FOUND' }), NOT_FOUND_TEXT],
  ])('SC-36 (happy) 「다시 시도」를 누르면 같은 공연을 다시 조회한다 — %s %s', async (_row, _label, second, expected) => {
    // Given GET /api/performances/7 이 500 으로 실패해 오류 화면이 보인다 (상세 호출 수 1)
    let n = 0
    stubServer({ [DETAIL]: () => (n++ === 0 ? res(500, { code: 'INTERNAL' }) : second()) })
    renderAt('/performances/7')
    const retry = await screen.findByRole('button', { name: '다시 시도' })
    expect(callsTo(DETAIL)).toHaveLength(1)

    // When 「다시 시도」를 누른다
    await userEvent.click(retry)

    // Then 상세 호출 수는 2다
    await waitFor(() => expect(callsTo(DETAIL)).toHaveLength(2))
    // And 표의 화면이 보인다
    if (expected === '지금 예매할 수 있어요') {
      expect(await screen.findByRole('heading', { level: 1, name: expected })).toBeInTheDocument()
    } else {
      expect(await screen.findByText(expected)).toBeInTheDocument()
      if (expected === ERROR_TEXT) expect(screen.getByRole('button', { name: '다시 시도' })).toBeInTheDocument()
      else expect(screen.getByRole('link', { name: '홈으로' })).toBeInTheDocument()
    }
  })

  // ---------- SC-38·39 ----------

  test.each([
    ['a', '「공연으로 돌아가기」 링크', () => userEvent.click(screen.getByRole('link', { name: '공연으로 돌아가기' }))],
    ['b', '앱바 「뒤로」 버튼', () => userEvent.click(screen.getByRole('button', { name: '뒤로' }))],
  ])('SC-38 (happy) 대기열 임시 화면에서 상세로 돌아간다 — %s %s', async (_row, _label, act2) => {
    // Given /performances/7/queue 의 대기열 임시 화면이다
    stubServer({ [DETAIL]: () => res(200, P_OPEN()) })
    renderAt('/performances/7/queue')
    await screen.findByRole('heading', { level: 1, name: '대기열을 준비하고 있어요' })
    expect(callsTo(DETAIL)).toHaveLength(0)

    // When 표의 방법으로 돌아간다
    await act2()

    // Then 주소가 /performances/7 이고 상세가 다시 그려진다 (GET /api/performances/7 을 새로 부른다)
    await waitFor(() => expect(where()).toBe('/performances/7'))
    expect(await screen.findByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })).toBeInTheDocument()
    expect(callsTo(DETAIL)).toHaveLength(1)
  })

  test('SC-39 (happy) 상세 앱바의 「뒤로」는 홈으로 간다', async () => {
    // Given P_OPEN 상세가 보인다 (직접 주소로 열었다)
    stubServer({
      [DETAIL]: () => res(200, P_OPEN()),
      'GET /api/performances': () =>
        res(200, { content: [{ ...P_OPEN(), id: 3, title: '가을 재즈 콘서트' }], page: 0, size: 20, totalElements: 1 }),
    })
    renderAt('/performances/7')
    await screen.findByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })

    // When 앱바의 「뒤로」 버튼을 누른다
    await userEvent.click(screen.getByRole('button', { name: '뒤로' }))

    // Then 주소가 / 이고 홈(공연 목록)이 보인다
    await waitFor(() => expect(where()).toBe('/'))
    expect(await screen.findByText('가을 재즈 콘서트')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })).not.toBeInTheDocument()
  })
})
