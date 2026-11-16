import { act, fireEvent, screen, within } from '@testing-library/react'
import {
  TIME,
  USER,
  authApi,
  deferred,
  jsonWithInfinity,
  navigateTo,
  networkError,
  raw,
  renderAt,
  res,
  settle,
  startFakeClock,
  stubServer,
  timeRoute,
} from './task020'
import type { Responder } from './task020'
import { token } from '../theme/tokens'
import type { TokenName } from '../theme/tokens'

// TASK-021 테스트 공용 — SoT: workflow_design/05_scenario/SCENARIO_TASK-021.md 「규약」
// 서버는 fetch 스텁(task020 의 stubServer — 등록하지 않은 키는 404)이고 시간은 가짜 타이머다.
// 화면은 App + MemoryRouter + AuthProvider 로 실제 라우트를 지난다.

export { TIME, USER, authApi, deferred, navigateTo, networkError, raw, renderAt, res, settle, startFakeClock, stubServer, timeRoute }
export type { Responder }

export const LIST_KEY = 'GET /api/performances'
export const NEXT_KEY = 'GET /api/performances/next-opening'

/** 기준 시각 S0 — 한국 11월 11일(수) 오후 5:50 */
export const S0 = '2026-11-11T08:50:00Z'

export type Item = Record<string, unknown>

const COMMON = { closeAt: '2026-12-31T14:59:00Z', totalSeats: 1000 }

// ---------- 공연 L1~L7 (규약 표 — 시안 home.html 의 데이터) ----------
export const L1: Item = { ...COMMON, id: 11, title: '소리풍경 밴드 나이트', venue: '롤링홀', status: 'OPEN', availableSeats: 37, startAt: '2026-11-21T10:00:00Z', openAt: '2026-11-01T01:00:00Z', posterUrl: 'https://cdn.example.com/sori.jpg' }
export const L2: Item = { ...COMMON, id: 12, title: '클래식 〈겨울 나그네〉', venue: '금호아트홀', status: 'CLOSED', availableSeats: 0, startAt: '2026-11-28T08:00:00Z', openAt: '2026-10-20T01:00:00Z' }
export const L3: Item = { ...COMMON, id: 13, title: '재즈 나잇 인 서울', venue: '세종문화회관', status: 'UPCOMING', availableSeats: 800, startAt: '2026-12-05T10:00:00Z', openAt: '2026-11-14T05:00:00Z' }
export const L4: Item = { ...COMMON, id: 7, title: '루미너스 콘서트 〈WAVELENGTH〉 서울', venue: '고척스카이돔', status: 'UPCOMING', availableSeats: 21000, startAt: '2026-12-19T09:00:00Z', openAt: '2026-11-11T09:00:00Z' }
export const L5: Item = { ...COMMON, id: 15, title: '발레 〈호두까기 인형〉', venue: '예술의전당 오페라극장', status: 'SOLD_OUT', availableSeats: 0, startAt: '2026-12-24T10:00:00Z', openAt: '2026-11-02T01:00:00Z' }
export const L6: Item = { ...COMMON, id: 16, title: '뮤지컬 〈별의 정원〉', venue: '샤롯데씨어터', status: 'UPCOMING', availableSeats: 1200, startAt: '2027-01-09T09:00:00Z', openAt: '2026-11-18T11:00:00Z' }
export const L7: Item = { ...COMMON, id: 17, title: '취소된 공연', venue: '모모홀', status: 'CANCELLED', availableSeats: 100, startAt: '2026-12-12T10:00:00Z', openAt: '2026-11-05T01:00:00Z' }
export const SIAN: Item[] = [L1, L2, L3, L4, L5, L6]
export const TITLE_L4 = String(L4.title)

/** 「공연 N」 — id 100+N, OPEN, 10석, N분씩 늦은 시작 */
export function numbered(n: number): Item {
  return { ...COMMON, id: 100 + n, title: `공연 ${n}`, venue: 'OO홀', status: 'OPEN', availableSeats: 10, startAt: new Date(Date.parse('2026-12-01T10:00:00Z') + n * 60_000).toISOString(), openAt: '2026-11-01T01:00:00Z' }
}
export const range = (from: number, to: number): Item[] => Array.from({ length: to - from + 1 }, (_, i) => numbered(from + i))

/** 목록 응답 본문 `LIST(items, total)` */
export const listBody = (items: unknown, total: unknown = Array.isArray(items) ? items.length : 0) => ({ content: items, page: 0, size: 20, totalElements: total })

type Value = Responder | unknown
/** 값이 함수면 그대로 응답기, 아니면 200 JSON — 요청마다 새 Response (본문은 한 번만 읽힌다) */
const asResponder = (v: Value): Responder => (typeof v === 'function' ? (v as Responder) : () => jsonWithInfinityResponse(v))
export function jsonWithInfinityResponse(body: unknown): Response {
  return new Response(jsonWithInfinity(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
}

/** 쿼리 문자열(`status=OPEN&page=1`, 없으면 '')별로 응답한다. 모르는 쿼리는 404 */
export function listRoute(table: Record<string, Value>): Responder {
  return (init, url) => {
    const query = url.includes('?') ? url.split('?')[1] : ''
    const respond = Object.hasOwn(table, query) ? asResponder(table[query]) : undefined
    return respond ? respond(init, url) : res(404, { code: 'NOT_FOUND' })
  }
}
export const sianList = () => ({ '': listBody(SIAN) })

export interface HomeOptions {
  /** 기기 시계 — 기본 S0 */
  device?: string
  /** 서버 시각 = 응답 시점 기기 시계 − skew. 기본은 기기 시계와 같다 */
  skew?: number
  list?: Record<string, Value>
  next?: Value
  time?: Responder
  routes?: Record<string, Responder>
  strict?: boolean
}

/** 홈을 연다 — 가짜 타이머로 기기 시계를 고정하고 서버는 스텁. 응답이 처리될 때까지 기다리지는 않는다 */
export function startHome(options: HomeOptions = {}) {
  startFakeClock(options.device ?? S0)
  stubServer({
    [LIST_KEY]: listRoute(options.list ?? sianList()),
    [NEXT_KEY]: asResponder(options.next ?? L4),
    [TIME]: options.time ?? timeRoute({ skew: options.skew ?? 0 }),
    ...(options.routes ?? {}),
  })
  return renderAt('/', { strict: options.strict })
}

/** 응답·효과를 모두 처리한다 */
export async function flush(ms = 0) {
  await settle(ms)
  await settle(0)
  await settle(0)
}

export async function openHome(options: HomeOptions = {}) {
  const view = startHome(options)
  await flush()
  return view
}

export async function click(element: Element) {
  await act(async () => {
    fireEvent.click(element)
  })
  await flush()
}

// ---------- 질의 도우미 ----------

export const hero = () => screen.queryByRole('heading', { level: 1 })
export const heroText = () => hero()?.textContent ?? null
export const eyebrow = () => screen.queryByText(/^티켓온 ·/)?.textContent ?? null
export const moreLink = () => screen.queryByRole('link', { name: /공연 보기$/ })
export const chip = (name: string) => screen.getByRole('button', { name })
export const CHIPS = ['전체', '예정', '예매 가능', '매진', '마감', '취소'] as const
export const pressed = () => CHIPS.filter((name) => chip(name).getAttribute('aria-pressed') === 'true')
export const STATUS_OF_CHIP: Record<string, string | undefined> = { 전체: undefined, 예정: 'UPCOMING', '예매 가능': 'OPEN', 매진: 'SOLD_OUT', 마감: 'CLOSED', 취소: 'CANCELLED' }
export const queryOf = (chipName: string) => {
  const status = STATUS_OF_CHIP[chipName]
  return status ? `status=${status}` : ''
}

/** 카드 = /performances/{id} 로 가는 링크(머리글의 「공연 보기」 제외) */
export const cardLinks = () =>
  screen.queryAllByRole('link').filter((a) => /^\/performances\/\d+$/.test(a.getAttribute('href') ?? '') && !/공연 보기$/.test(a.textContent ?? ''))
export const cardTitles = () => cardLinks().map((a) => a.textContent ?? '')
export const cardOf = (title: string) => {
  const found = cardLinks().find((a) => (a.textContent ?? '').includes(title))
  if (!found) throw new Error(`카드가 없다: ${title}`)
  return found
}
export const inCard = (title: string) => within(cardOf(title))
export const skeletons = () => screen.queryAllByTestId('performance-card-skeleton')
export const heroSkeleton = () => screen.queryByTestId('hero-skeleton')

export const LIST_ERROR = '공연 목록을 불러오지 못했어요'
export const RETRY = '다시 시도'
export const MORE_FAIL = '불러오지 못했어요 · 다시 시도'

export const listCalls = (calls: { key: string; url: string }[]) => calls.filter((c) => c.key === LIST_KEY)
export const urlsOf = (calls: { key: string; url: string }[], key: string) => calls.filter((c) => c.key === key).map((c) => c.url)
export const pathAndQuery = (url: string) => url.replace(/^https?:\/\/[^/]+/, '')

/** 화면이 떠 있는 채로 로그인한다(세션 변경) */
export async function loginNow() {
  await act(async () => authApi().login('tok-1', USER as never))
}
export async function logoutNow() {
  await act(async () => authApi().logout())
}

// ---------- 계약 위반 표 CV (규약) ----------

const inf = '__INF__'
/** 값 하나만 바꾼 원소 행 — undefined 는 키 없음 */
export const CV_ELEMENT: Array<[string, Item]> = [
  ['id-a', { id: undefined }], ['id-b', { id: null }], ['id-c', { id: '11' }], ['id-d', { id: '../7' }], ['id-e', { id: 11.5 }], ['id-f', { id: 0 }], ['id-g', { id: -1 }],
  ['id-h', { id: inf }], ['id-i', { id: ' 11' }], ['id-j', { id: '11\n' }], ['id-k', { id: '11\u0000' }], ['id-l', { id: '１１' }], ['id-m', { id: '11?x=1' }],
  ['ti-a', { title: undefined }], ['ti-b', { title: null }], ['ti-c', { title: 7 }], ['ti-d', { title: ['x'] }], ['ti-e', { title: '' }], ['ti-f', { title: '   ' }],
  ['ti-g', { title: '　' }], ['ti-h', { title: ' ' }], ['ti-i', { title: '​' }],
  ['ve-a', { venue: undefined }], ['ve-b', { venue: null }], ['ve-c', { venue: 7 }], ['ve-d', { venue: '' }], ['ve-e', { venue: '   ' }], ['ve-f', { venue: '　' }],
  ['ve-g', { venue: ' ' }], ['ve-h', { venue: '​' }],
  ['st-a', { startAt: undefined }], ['st-b', { startAt: null }], ['st-c', { startAt: 1731315600000 }], ['st-d', { startAt: '내일' }], ['st-e', { startAt: '2026-12-19T18:00:00' }], ['st-f', { startAt: '2026-12-19' }],
  ['op-a', { openAt: undefined }], ['op-b', { openAt: null }], ['op-c', { openAt: 1731315600000 }], ['op-d', { openAt: '내일' }], ['op-e', { openAt: '2026-11-11T18:00:00' }], ['op-f', { openAt: '2026-11-11' }],
  ['ss-a', { status: undefined }], ['ss-b', { status: null }], ['ss-c', { status: 1 }], ['ss-d', { status: 'PAUSED' }],
  ['av-a', { availableSeats: undefined }], ['av-b', { availableSeats: null }], ['av-c', { availableSeats: '37' }], ['av-d', { availableSeats: -1 }], ['av-e', { availableSeats: 1.9 }],
  ['av-f', { availableSeats: inf }], ['av-g', { availableSeats: Number.MAX_SAFE_INTEGER + 2 }],
]

/** 응답 수준 행 — 정상 응답(items 또는 시안 목록)을 받아 값 하나만 바꾼 본문 */
export const CV_RESPONSE: Array<[string, (items: Item[]) => unknown]> = [
  ['el-a', (items) => listBody([items[0], null], 2)],
  ['el-b', (items) => listBody([items[0], 'x'], 2)],
  ['el-c', (items) => listBody([items[0], []], 2)],
  ['co-a', (items) => ({ ...listBody(items), content: undefined })],
  ['co-b', (items) => ({ ...listBody(items), content: null })],
  ['co-c', (items) => ({ ...listBody(items), content: 'x' })],
  ['co-d', (items) => ({ ...listBody(items), content: {} })],
  ['to-a', (items) => ({ ...listBody(items), totalElements: undefined })],
  ['to-b', (items) => ({ ...listBody(items), totalElements: null })],
  ['to-c', (items) => ({ ...listBody(items), totalElements: '6' })],
  ['to-d', (items) => ({ ...listBody(items), totalElements: -1 })],
  ['to-e', (items) => ({ ...listBody(items), totalElements: 1.5 })],
  ['to-f', (items) => ({ ...listBody(items), totalElements: inf })],
  ['to-g', (items) => ({ ...listBody(items), totalElements: Number.MAX_SAFE_INTEGER + 2 })],
]

/** 원소 행을 items[index] 에 적용한 목록 본문 */
export const withPatch = (items: Item[], patch: Item, index = 0): unknown => listBody(items.map((it, i) => (i === index ? { ...it, ...patch } : it)))

/** CV 전체 — [행 이름, 첫 응답 본문] 목록. 원소 행은 items 의 첫 원소에 적용한다 */
export const cvListRows = (items: Item[]): Array<[string, unknown]> => [
  ...CV_ELEMENT.map(([name, patch]) => [name, withPatch(items, patch)] as [string, unknown]),
  ...CV_RESPONSE.map(([name, make]) => [name, make(items)] as [string, unknown]),
]

/** 후보 본문 행 — 원소 행과 본문 자체가 null·'x'·[] 인 행 */
export const cvNextRows = (): Array<[string, unknown]> => [
  ...CV_ELEMENT.map(([name, patch]) => [name, { ...L4, ...patch }] as [string, unknown]),
  ['body-null', null],
  ['body-x', 'x'],
  ['body-array', []],
]

/** SC-17·21 표 — 목록 조회 실패 응답 a~j (같은 오류 화면) */
export const LIST_FAILS: Array<[string, Responder]> = [
  ['a', () => res(500, { code: 'INTERNAL' })],
  ['b', () => res(500, { code: 'OTHER' })],
  ['c', () => new Response('', { status: 503 })],
  ['d', () => raw(502, '<html>Bad Gateway</html>')],
  ['e', () => new Response('', { status: 200 })],
  ['f', () => raw(200, '<html>ok</html>')],
  ['g', () => networkError()],
  ['h', () => res(404, { code: 'NOT_FOUND' })],
  ['i', () => res(401, { code: 'UNAUTHORIZED' })],
  ['j', () => res(400, { code: 'INVALID_PAGE' })],
]

/** 토큰 값을 칠한 탐침 요소의 계산된 값 — 매직 넘버 대신 토큰과 대조한다 */
export function tokenValue(property: 'fontSize' | 'borderRadius' | 'minHeight', name: TokenName): string {
  const probe = document.createElement('div')
  probe.style[property] = token(name)
  document.body.appendChild(probe)
  const kebab = property.replace(/[A-Z]/g, (c) => `-${c.toLowerCase()}`)
  const value = getComputedStyle(probe).getPropertyValue(kebab)
  probe.remove()
  return value
}

export { fireEvent, within }
