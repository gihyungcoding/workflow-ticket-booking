import { cleanup, screen } from '@testing-library/react'
import { getNextOpening, getPerformances } from '../api/performances'
import {
  L1,
  L4,
  LIST_KEY,
  NEXT_KEY,
  SIAN,
  chip,
  cardLinks,
  click,
  cvNextRows,
  eyebrow,
  flush,
  listBody,
  loginNow,
  moreLink,
  openHome,
  pathAndQuery,
  pressed,
  res,
  tokenValue,
  within,
} from '../test/task021'
import type { Item } from '../test/task021'
import { P_OPEN, TITLE, calls, css, hasGlobalHeader, isTokenColor, navigateTo, raw, renderAt, resetDocumentVisibility, squash, stubServer, where } from '../test/task020'

// TASK-021 시나리오 — 전역 헤더·렌더 값·정적 제약·Poster 추출·API 클라이언트·계약 밖 필드.
// SoT: workflow_design/05_scenario/SCENARIO_TASK-021.md

/** 저장소 루트 — vitest 의 URL 해석은 /@fs 접두를 붙이므로 문자열로 자른다 (task020.static.test.ts 와 같은 사정) */
const REPO = decodeURIComponent(import.meta.url.replace('file://', '').split('/frontend/')[0])
const CHILD_PROCESS: string = 'node:child_process'
interface ConstraintResult {
  id: string
  violations?: { file?: string; path?: string }[]
}

/** 소스를 문자열로 읽는다 — 파일이 없으면 빈 객체(없다는 사실이 단언을 실패시킨다) */
const sources = import.meta.glob(
  ['../pages/PerformanceListPage.tsx', '../components/PerformanceCard.tsx', '../components/StatusChips.tsx', '../components/Poster.tsx', '../pages/PerformanceDetailPage.tsx'],
  { query: '?raw', import: 'default', eager: true },
) as Record<string, string>
const source = (name: string) => Object.entries(sources).find(([path]) => path.endsWith(`/${name}`))?.[1]

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

const detailRoute = { 'GET /api/performances/7': () => res(200, P_OPEN()) }

describe('TASK-021 — 홈 (회귀·API 클라이언트·계약 밖 필드)', () => {
  test('SC-24 (regression) 「내 예매」 행은 없고 전역 헤더는 홈에 남는다', async () => {
    // Given 시안 목록이 보이는 홈이다
    // When 비로그인·로그인 상태로 각각 홈을 연다
    await openHome({ routes: detailRoute })
    // Then 「내 예매」 글자·링크·/my-bookings 로 가는 링크가 없다
    expect(screen.queryByText(/내 예매/)).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /내 예매/ })).not.toBeInTheDocument()
    expect(document.querySelector('a[href*="my-bookings"]')).toBeNull()
    // And 비로그인이면 「로그인」·「가입」 링크가 있고 「로그인」을 누르면 주소가 /login 이다
    expect(screen.getByRole('link', { name: '가입' })).toBeInTheDocument()
    await click(screen.getByRole('link', { name: '로그인' }))
    expect(where()).toBe('/login')
    cleanup()
    // And 로그인 상태면 이름 「홍길동」과 「로그아웃」 버튼이 있고 「로그인」·「가입」 링크는 없다
    await openHome({ routes: detailRoute })
    await loginNow()
    await flush()
    expect(screen.getByText('홍길동')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그아웃' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: '로그인' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: '가입' })).not.toBeInTheDocument()
    // And 같은 앱에서 상세(/performances/7)에는 전역 헤더가 없다(TASK-020 그대로)
    await navigateTo('/performances/7')
    await flush()
    expect(hasGlobalHeader()).toBe(false)
  })

  test('SC-25 (regression) 렌더된 값이 시안과 같다 — 칩·격자·카드 글자', async () => {
    // Given 시안 목록이 보이는 홈이다(후보 L4·기준 시각 S0)
    await openHome()
    const card = within(cardLinks()[0])
    const first = String(L1.title)
    // When 요소의 계산된 스타일을 읽는다
    // Then 값은 표와 같다
    // a 선택된 칩(전체)
    const on = chip('전체')
    expect(isTokenColor(css(on, 'background-color'), '--c-text-1')).toBe(true)
    expect(isTokenColor(css(on, 'color'), '--c-surface')).toBe(true)
    expect(css(on, 'min-height')).toBe('44px')
    expect(css(on, 'border-radius')).toBe(tokenValue('borderRadius', '--r-pill'))
    // b 선택 안 된 칩
    const off = chip('예정')
    expect(isTokenColor(css(off, 'background-color'), '--c-surface')).toBe(true)
    expect(isTokenColor(css(off, 'color'), '--c-text-2')).toBe(true)
    expect(css(off, 'min-height')).toBe('44px')
    // c 칩 줄
    const row = on.parentElement as HTMLElement
    expect(css(row, 'display')).toBe('flex')
    expect(css(row, 'overflow-x')).toBe('auto')
    // d 카드 격자
    const grid = cardLinks()[0].parentElement as HTMLElement
    expect(css(grid, 'display')).toBe('grid')
    expect(css(grid, 'grid-template-columns')).toBe('repeat(2, 1fr)')
    // e 카드 안 공연명
    const title = card.getByText(first)
    expect(css(title, 'font-weight')).toBe('700')
    expect(css(title, 'font-size')).toBe(tokenValue('fontSize', '--fs-caption'))
    expect(isTokenColor(css(title, 'color'), '--c-text-1')).toBe(true)
    // f 카드 안 시작 일시
    const startsAt = card.getByText('11월 21일(토) 오후 7:00')
    expect(css(startsAt, 'font-size')).toBe(tokenValue('fontSize', '--fs-sub'))
    expect(isTokenColor(css(startsAt, 'color'), '--c-text-2')).toBe(true)
    // g 카드 안 장소 줄
    const meta = card.getByText('롤링홀 · 남은 좌석 37석')
    expect(css(meta, 'font-size')).toBe(tokenValue('fontSize', '--fs-caption'))
    expect(isTokenColor(css(meta, 'color'), '--c-text-3')).toBe(true)
    // h 카드 포스터 칸
    const poster = card.getByTestId('poster')
    expect(squash(css(poster, 'aspect-ratio'))).toBe('1/1')
    expect(css(poster, 'border-radius')).toBe(tokenValue('borderRadius', '--r-m'))
    // i 「… 공연 보기」 링크
    expect(isTokenColor(css(moreLink()!, 'color'), '--c-brand-text')).toBe(true)
    expect(css(moreLink()!, 'min-height')).toBe('44px')
    // j 머리글 위 줄
    const line = screen.getByText(/^티켓온 ·/)
    expect(css(line, 'font-size')).toBe(tokenValue('fontSize', '--fs-sub'))
    expect(isTokenColor(css(line, 'color'), '--c-text-2')).toBe(true)
    // k 소스의 색·치수 — 홈 소스에 색상 리터럴과 숫자 치수가 없다
    const files = ['PerformanceListPage.tsx', 'PerformanceCard.tsx', 'StatusChips.tsx']
    for (const name of files) {
      const text = source(name)
      expect(text, `${name} 이 있다`).toBeDefined()
      expect(text).not.toMatch(/#[0-9a-fA-F]{3,8}\b|rgba?\(|hsla?\(/)
      expect(text).not.toMatch(/\b(height|width|fontSize|padding|margin|borderRadius)=\{[0-9]+\}/)
    }
  })

  test('SC-26 (regression) 디자인·배포 제약이 이번 변경으로 새로 어겨지지 않는다', async () => {
    // Given 이 태스크의 변경이 frontend/src 에 반영돼 있다
    const { execFileSync } = (await import(/* @vite-ignore */ CHILD_PROCESS)) as {
      execFileSync: (cmd: string, args: string[], options: { cwd: string; encoding: 'utf8' }) => string
    }
    // When python3 scripts/check_architecture.py --json 을 실행한다
    const output = execFileSync('python3', ['scripts/check_architecture.py', '--json'], { cwd: REPO, encoding: 'utf8' })
    const report = JSON.parse(output) as { results: ConstraintResult[]; no_target: string[] }
    const files = (id: string) => (report.results.find((r) => r.id === id)?.violations ?? []).map((v) => v.file ?? v.path ?? '')
    // Then DESIGN-001·DESIGN-002·DEPLOY-001 위반이 0건이다
    expect(files('DESIGN-001')).toEqual([])
    expect(files('DESIGN-002')).toEqual([])
    expect(files('DEPLOY-001')).toEqual([])
    // And DESIGN-003 위반 파일은 PerformanceEditPage.tsx 하나뿐이고 theme·components 아래 위반은 0건이다
    expect(files('DESIGN-003').sort()).toEqual(['frontend/src/pages/PerformanceEditPage.tsx'])
    expect(files('DESIGN-003').filter((f) => /frontend\/src\/(theme|components)\//.test(f))).toEqual([])
    // And no_target 에 DESIGN-001·002·003 이 없다
    for (const id of ['DESIGN-001', 'DESIGN-002', 'DESIGN-003']) expect(report.no_target).not.toContain(id)
  })

  test('SC-27 (regression) 포스터를 공용으로 꺼낸 뒤에도 상세의 포스터는 그대로다', async () => {
    // Given components/Poster.tsx 가 있고 PerformanceDetailPage.tsx 가 그것을 쓴다(자기 안의 Poster 는 없다)
    const poster = source('Poster.tsx')
    const detail = source('PerformanceDetailPage.tsx')
    expect(poster, 'components/Poster.tsx 가 있다').toBeDefined()
    expect(detail).toMatch(/from '\.\.\/components\/Poster'/)
    expect(detail).not.toMatch(/function Poster\b/)
    // When /performances/7(P_OPEN)을 연다
    stubServer({ 'GET /api/performances/7': () => res(200, { ...P_OPEN(), posterUrl: 'https://cdn.example.com/luminous.jpg' }) })
    renderAt('/performances/7')
    // Then 이름이 「<공연명> 포스터」 인 이미지가 있고 상세의 포스터 칸은 aspect-ratio 4 / 3·배경 --c-bg 다 — TASK-020 SC-17·33 의 값이 그대로다
    const image = await screen.findByRole('img', { name: `${TITLE} 포스터` })
    const box = screen.getByTestId('poster')
    expect(image.tagName).toBe('IMG')
    expect(squash(css(box, 'aspect-ratio'))).toBe('4/3')
    expect(isTokenColor(css(box, 'background-color'), '--c-bg')).toBe(true)
  })

  const fetchOk = () => stubServer({ [LIST_KEY]: () => res(200, listBody([L1])), [NEXT_KEY]: () => res(200, L4) })
  const lastRequest = () => pathAndQuery(calls.at(-1)!.url)
  const API_ROWS: Array<[string, () => Promise<void>]> = [
    ['a', async () => { fetchOk(); await getPerformances(); expect(lastRequest()).toBe('/api/performances') }],
    ['b', async () => { fetchOk(); await getPerformances(undefined, 0); expect(lastRequest()).toBe('/api/performances') }],
    ['c', async () => { fetchOk(); await getPerformances('OPEN'); expect(lastRequest()).toBe('/api/performances?status=OPEN') }],
    ['d', async () => { fetchOk(); await getPerformances(undefined, 1); expect(lastRequest()).toBe('/api/performances?page=1') }],
    ['e', async () => { fetchOk(); await getPerformances('SOLD_OUT', 2); expect(lastRequest()).toBe('/api/performances?status=SOLD_OUT&page=2') }],
    ['f', async () => { fetchOk(); await getPerformances(null); expect(lastRequest()).toBe('/api/performances') }],
    ['g', async () => {
      for (const status of ['UPCOMING', 'OPEN', 'SOLD_OUT', 'CLOSED', 'CANCELLED'] as const) {
        fetchOk()
        await getPerformances(status)
        expect(lastRequest()).toBe(`/api/performances?status=${status}`)
      }
    }],
    ['h', async () => {
      for (const bad of ['PAUSED', 1, {}]) {
        fetchOk()
        await expect((getPerformances as (...args: unknown[]) => Promise<unknown>)(bad)).rejects.toBeDefined()
        expect(calls).toHaveLength(0)
      }
    }],
    ['i', async () => {
      for (const bad of [-1, 1.9, Number.NaN, Number.POSITIVE_INFINITY, Number('1e400'), Number.MAX_SAFE_INTEGER + 2, '1']) {
        fetchOk()
        await expect((getPerformances as (...args: unknown[]) => Promise<unknown>)(undefined, bad)).rejects.toBeDefined()
        expect(calls).toHaveLength(0)
      }
    }],
    ['j', async () => { fetchOk(); await expect(getNextOpening()).resolves.toEqual(L4) }],
    ['k', async () => { stubServer({ [NEXT_KEY]: () => new Response(null, { status: 204 }) }); await expect(getNextOpening()).resolves.toBeNull() }],
    ['l', async () => {
      // 스켈레톤이 던지기만 해도 거부 단언이 통과하지 않게 — 정상 응답은 먼저 성공해야 한다
      fetchOk()
      await expect(getNextOpening()).resolves.toEqual(L4)
      const failures: Array<() => Response> = [() => res(404, { code: 'NOT_FOUND' }), () => res(500, { code: 'INTERNAL' }), () => new Response('', { status: 200 }), () => raw(200, '<html>ok</html>')]
      for (const failure of failures) {
        stubServer({ [NEXT_KEY]: failure })
        await expect(getNextOpening()).rejects.toBeDefined()
      }
      for (const [name, body] of cvNextRows()) {
        stubServer({ [NEXT_KEY]: () => new Response(JSON.stringify(body).replace(/"__INF__"/g, '1e400'), { status: 200 }) })
        await expect(getNextOpening(), `CV ${name}`).rejects.toBeDefined()
      }
    }],
  ]
  test.each(API_ROWS)('SC-28 (boundary) API 클라이언트는 상태·페이지를 쿼리로 싣고 잘못된 인자는 요청 전에 거른다 — 행 %s', async (_row, run) => {
    // Given fetch 가 호출 기록을 남기는 스텁이다(응답은 정상 200)
    // When 표의 호출을 한다
    // Then 결과는 표와 같다
    await run()
  })

  const IGNORED: Array<[string, (items: Item[]) => unknown]> = [
    ['a', (items) => listBody([{ ...items[0], closeAt: undefined }, ...items.slice(1)])],
    ['b', (items) => listBody([{ ...items[0], closeAt: null }, ...items.slice(1)])],
    ['c', (items) => listBody([{ ...items[0], totalSeats: null }, ...items.slice(1)])],
    ['d', (items) => listBody([{ ...items[0], sections: 'x' }, ...items.slice(1)])],
    ['e', (items) => listBody([{ ...items[0], sections: [null] }, ...items.slice(1)])],
    ['f', (items) => listBody([{ ...items[0], extra: 1 }, ...items.slice(1)])],
    ['g', (items) => ({ content: items, totalElements: items.length })],
    ['h', (items) => ({ ...listBody(items), size: 'x' })],
    ['i', (items) => listBody([{ ...items[0], availableSeats: 2000 }, ...items.slice(1)])],
  ]
  test.each(IGNORED)('SC-29 (boundary) 홈이 쓰지 않는 필드가 이상해도 홈은 그대로 보인다 — 행 %s', async (_row, make) => {
    // Given 목록 응답이 200 으로 시안 목록에서 표의 값만 바꾼 본문이다
    // When 홈을 연다
    await openHome({ list: { '': make(SIAN) } })
    // Then 「공연 목록을 불러오지 못했어요」가 아니라 카드 6개가 보인다
    expect(screen.queryByText('공연 목록을 불러오지 못했어요')).not.toBeInTheDocument()
    expect(cardLinks()).toHaveLength(6)
    expect(pressed()).toEqual(['전체'])
    expect(eyebrow()).not.toBeNull()
  })
})
