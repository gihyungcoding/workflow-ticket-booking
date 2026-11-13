import { act, render, screen } from '@testing-library/react'
import { StrictMode } from 'react'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter, useLocation, useNavigate } from 'react-router-dom'
import type { NavigateFunction } from 'react-router-dom'
import App from '../App'
import { AuthProvider, useAuth } from '../auth/AuthContext'
import { theme } from '../theme'
import { token } from '../theme/tokens'
import type { TokenName } from '../theme/tokens'

// TASK-020 테스트 공용 — SoT: workflow_design/05_scenario/SCENARIO_TASK-020.md 「규약」
// 서버는 fetch 스텁이다(응답마다 새 Response). 화면은 App + MemoryRouter + AuthProvider 로 실제 라우트를 지난다.

// ---------- 공연 P (규약 표) ----------

export const T_OPEN = '2026-11-11T09:00:00Z' // 한국 11월 11일(수) 오후 6:00
export const T_SERVER = '2026-11-11T08:55:28Z' // 기준 시각 예 — openAt 까지 272초
export const TITLE = '루미너스 콘서트 〈WAVELENGTH〉 서울'

export interface Section {
  grade: string
  price: number
  seatCount: number
  area: string
  areaNote?: string
  stageOrder: number
}

const floor = (grade: string, stageOrder: number): Section => ({
  grade,
  price: 150000,
  seatCount: 100,
  area: '플로어',
  areaNote: '무대 앞 바닥, F1이 가장 앞',
  stageOrder,
})

export const SECTIONS_P: Section[] = [
  floor('F1', 1),
  floor('F2', 2),
  floor('F3', 3),
  floor('F4', 4),
  { grade: '1층', price: 132000, seatCount: 300, area: '1층 지정석', areaNote: '플로어를 둘러싼 아래층', stageOrder: 5 },
  { grade: '2층', price: 110000, seatCount: 400, area: '2층 지정석', areaNote: '가장 바깥 위층', stageOrder: 6 },
]

export type Perf = Record<string, unknown>

export function perf(over: Perf = {}): Perf {
  return {
    id: 7,
    title: TITLE,
    venue: '고척스카이돔',
    startAt: '2026-12-19T09:00:00Z',
    openAt: T_OPEN,
    closeAt: '2026-11-18T14:59:00Z',
    totalSeats: 21000,
    availableSeats: 37,
    status: 'OPEN',
    posterUrl: 'https://cdn.example.com/luminous.jpg',
    sections: SECTIONS_P,
    ...over,
  }
}
export const P_OPEN = (over: Perf = {}) => perf(over)
export const P_UP = (over: Perf = {}) => perf({ status: 'UPCOMING', availableSeats: 21000, ...over })
export const P_SOLD = (over: Perf = {}) => perf({ status: 'SOLD_OUT', availableSeats: 0, ...over })
export const P_CLOSED = (over: Perf = {}) => perf({ status: 'CLOSED', availableSeats: 0, ...over })
export const P_CANCEL = (over: Perf = {}) => perf({ status: 'CANCELLED', availableSeats: 37, ...over })

// ---------- 가짜 서버 ----------

export type Responder = (init: RequestInit, url: string) => Response | Promise<Response>
export interface Call {
  key: string
  url: string
  init: RequestInit
}

export function res(status: number, body?: unknown): Response {
  const text = body === undefined ? '' : typeof body === 'string' ? body : JSON.stringify(body)
  return new Response(text, { status, headers: { 'Content-Type': 'application/json' } })
}
export function raw(status: number, text: string): Response {
  return new Response(text, { status, headers: { 'Content-Type': 'text/plain' } })
}
export function networkError(): Response {
  throw new TypeError('Failed to fetch')
}

/** JSON 직렬화는 Infinity 를 null 로 바꾼다 — 표시 문자열 '__INF__' 를 1e400 로 바꿔 파서가 Infinity 로 읽게 한다 */
export function jsonWithInfinity(obj: unknown): string {
  return JSON.stringify(obj).replace(/"__INF__"/g, '1e400')
}

export let calls: Call[] = []
export const callsTo = (key: string) => calls.filter((c) => c.key === key)

export const DETAIL = 'GET /api/performances/7'
export const TIME = 'GET /api/time'

export function stubServer(routes: Record<string, Responder> = {}) {
  calls = []
  const table: Record<string, Responder> = { ...routes }
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) => {
      const url = String(input)
      const path = url.split('?')[0]
      const key = `${(init.method ?? 'GET').toUpperCase()} ${path}`
      calls.push({ key, url, init })
      const respond = table[key]
      return respond ? respond(init, url) : res(404, { code: 'NOT_FOUND' })
    }),
  )
}

/** 서버 시각 = 응답 시점의 기기 시계 − skew. rtt 만큼 응답이 늦는다(가짜 타이머) */
export function timeRoute(options: { skew?: number; rtt?: number } = {}): Responder {
  const { skew = 0, rtt = 0 } = options
  return async () => {
    if (rtt) await new Promise((r) => setTimeout(r, rtt))
    return res(200, { now: new Date(Date.now() - skew).toISOString() })
  }
}

export interface Sample {
  /** 응답이 오기까지 걸리는 시간(ms) */
  rtt: number
  /** 서버가 주는 시각(ISO). 실패 표본이면 쓰지 않는다 */
  now?: string
  /** 실패 표본 — 응답 대신 이 모양으로 실패한다 */
  fail?: (() => Response) | 'network'
  /** now 대신 본문 전체를 직접 준다 */
  body?: unknown
}

/** 호출 순서대로 표본을 준다 — 표본이 바닥나면 마지막 표본을 되풀이한다. log 에는 호출이 나간 기기 시각을 적는다 */
export function timeSamples(samples: Sample[], log: number[] = []): Responder {
  let i = 0
  return async () => {
    const s = samples[Math.min(i, samples.length - 1)]
    i += 1
    log.push(Date.now())
    if (s.rtt) await new Promise((r) => setTimeout(r, s.rtt))
    if (s.fail === 'network') return networkError()
    if (s.fail) return s.fail()
    if (s.body !== undefined) return res(200, s.body)
    return res(200, { now: s.now })
  }
}

export function deferred<T = Response>() {
  let resolve!: (v: T) => void
  let reject!: (e: unknown) => void
  const promise = new Promise<T>((a, b) => {
    resolve = a
    reject = b
  })
  return { promise, resolve, reject }
}

// ---------- 렌더 ----------

export function Where() {
  const l = useLocation()
  return <div data-testid="where">{l.pathname + l.search + l.hash}</div>
}
export const where = () => screen.getByTestId('where').textContent

export const USER = { id: 1, email: 'hong@example.com', name: '홍길동', role: 'AUDIENCE', handle: 'wave7302' }

let authRef: ReturnType<typeof useAuth> | null = null
function AuthProbe() {
  authRef = useAuth()
  return null
}
/** 화면이 떠 있는 채로 로그인·로그아웃이 일어나게 한다(세션 변경) */
export const authApi = () => authRef!

let navigateRef: NavigateFunction | null = null
function NavProbe() {
  navigateRef = useNavigate()
  return null
}
/** 브라우저 뒤로 가기 — MemoryRouter 의 history.go(-1) 와 같다 */
export async function browserBack() {
  await act(async () => {
    navigateRef!(-1)
  })
}
export async function navigateTo(path: string) {
  await act(async () => {
    navigateRef!(path)
  })
}

type Entry = string | { pathname: string; state?: unknown }

export function renderAt(
  entry: Entry | Entry[],
  options: { strict?: boolean; index?: number } = {},
) {
  const entries = Array.isArray(entry) ? entry : [entry]
  const tree = (
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={entries} initialIndex={options.index ?? entries.length - 1}>
        <AuthProvider>
          <App />
          <Where />
          <NavProbe />
          <AuthProbe />
        </AuthProvider>
      </MemoryRouter>
    </ThemeProvider>
  )
  return render(options.strict ? <StrictMode>{tree}</StrictMode> : tree)
}

// ---------- 가짜 타이머 ----------

/** 기기 시계(DEVICE)를 정하고 가짜 타이머를 켠다. 끄는 것은 afterEach 의 vi.useRealTimers() */
export function startFakeClock(deviceIso: string) {
  vi.useFakeTimers()
  vi.setSystemTime(new Date(deviceIso))
}

/** 시간을 ms 만큼 흘리고 그 사이 대기 중인 응답·효과를 모두 처리한다 */
export async function settle(ms = 0) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms)
  })
  await act(async () => {
    await vi.advanceTimersByTimeAsync(0)
  })
}

export const iso = (ms: number) => new Date(ms).toISOString()
export const at = (base: string, deltaMs: number) => iso(new Date(base).getTime() + deltaMs)

// ---------- 질의 도우미 ----------

export const h1 = () => screen.getByRole('heading', { level: 1 })
export const cta = (name: string | RegExp) => screen.getByRole('button', { name })
export const timer = () => screen.getByRole('timer')
export const timerOrNull = () => screen.queryByRole('timer')
export const globalHeaderLinks = () => screen.queryAllByRole('link', { name: /^(로그인|가입)$/ })
export const hasGlobalHeader = () => globalHeaderLinks().length > 0 || screen.queryByRole('button', { name: '로그아웃' }) !== null

export function setDocumentVisibility(state: string) {
  Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => state })
  document.dispatchEvent(new Event('visibilitychange'))
}
export function resetDocumentVisibility() {
  Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => 'visible' })
}

// ---------- 스타일 단언 (DESIGN-002 — 색 리터럴 대신 토큰을 칠한 탐침 요소의 계산값과 대조한다) ----------

/** 토큰 색을 칠한 탐침 요소의 계산된 색 */
export function tokenColor(name: TokenName): string {
  const probe = document.createElement('div')
  probe.style.color = token(name)
  document.body.appendChild(probe)
  const value = getComputedStyle(probe).color
  probe.remove()
  return value
}
/** 계산된 값이 그 토큰의 색이다 — jsdom 은 SVG 의 var(--x) 를 해석하지 않으므로 그 표기도 같은 색으로 본다 */
export const isTokenColor = (actual: string, name: TokenName) => actual === tokenColor(name) || actual === `var(${name})`
/** 공백 표기 차이를 없앤다 — jsdom 은 aspect-ratio 를 '4/3' 으로 준다 */
export const squash = (v: string) => v.replace(/\s+/g, '')
export const css = (el: Element, prop: string) => getComputedStyle(el).getPropertyValue(prop)
