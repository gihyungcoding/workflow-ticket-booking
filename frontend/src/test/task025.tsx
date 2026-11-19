import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { StrictMode } from 'react'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter, useLocation, useNavigate } from 'react-router-dom'
import type { NavigateFunction } from 'react-router-dom'
import App from '../App'
import { AuthProvider, useAuth } from '../auth/AuthContext'
import { theme } from '../theme'
import { callsTo, deferred, jsonWithInfinity, networkError, raw, res, stubServer } from './task020'
import type { Responder } from './task020'
import { listRoute } from './task021'

// TASK-025 테스트 공용 — SoT: workflow_design/05_scenario/SCENARIO_TASK-025.md 「규약」
// 서버는 fetch 스텁(task020 의 stubServer — 등록하지 않은 키는 404)이고 화면은 App + MemoryRouter + AuthProvider 로 실제 라우트를 지난다.

export { callsTo, deferred, jsonWithInfinity, networkError, raw, res, stubServer }
export type { Responder }

export const ME = 'GET /api/auth/me'
export const LOGIN = 'POST /api/auth/login'
export const LIST_KEY = 'GET /api/organizer/performances'
export const HOME_LIST = 'GET /api/performances'

// ---------- 사용자 (규약) ----------

export type User = Record<string, unknown>
export const ORG: User = { id: 1, email: 'org@ticket.on', name: '한도윤', role: 'ORGANIZER', handle: 'org01' }
export const AUD: User = { id: 2, email: 'aud@ticket.on', name: '관객', role: 'AUDIENCE', handle: 'aud01' }
export const tokenOf = (user: User) => (user.role === 'ORGANIZER' ? 'tok-org' : 'tok-aud')
export const loginBody = (accessToken: string, user: User) => ({ accessToken, tokenType: 'Bearer', expiresInSeconds: 86400, user })

// ---------- 공연 (규약 표) ----------

export type Item = Record<string, unknown>

export const D: Item = { id: 11, title: '소리풍경 밴드 나이트 앙코르', venue: '롤링홀', startAt: '2026-12-26T10:00:00Z', openAt: '2026-12-01T11:00:00Z', closeAt: '2026-12-26T09:00:00Z', totalSeats: 300, availableSeats: 300, status: 'UPCOMING', published: false }
export const O: Item = { id: 12, title: '소리풍경 밴드 나이트', venue: '롤링홀', startAt: '2026-11-21T10:00:00Z', openAt: '2026-11-01T11:00:00Z', closeAt: '2026-11-21T09:00:00Z', totalSeats: 300, availableSeats: 37, status: 'OPEN', published: true, posterUrl: 'https://img.ticket.on/p12.jpg' }
export const C: Item = { id: 13, title: '소리풍경 어쿠스틱 라이브', venue: '벨로주 홍대', startAt: '2026-10-10T09:00:00Z', openAt: '2026-09-01T11:00:00Z', closeAt: '2026-10-10T08:00:00Z', totalSeats: 300, availableSeats: 250, status: 'CANCELLED', published: true }
export const Z: Item = { id: 14, title: '소리풍경 밴드 여름 단독', venue: '롤링홀', startAt: '2026-08-22T10:00:00Z', openAt: '2026-07-01T11:00:00Z', closeAt: '2026-08-22T09:00:00Z', totalSeats: 300, availableSeats: 12, status: 'CLOSED', published: true }
export const U: Item = { id: 15, title: '가을 재즈 콘서트', venue: '블루노트 서울', startAt: '2026-12-05T11:00:00Z', openAt: '2026-11-20T11:00:00Z', closeAt: '2026-12-05T10:00:00Z', totalSeats: 200, availableSeats: 200, status: 'UPCOMING', published: true }
export const S: Item = { id: 16, title: '겨울 클래식 갈라', venue: '예술의전당', startAt: '2026-12-19T10:00:00Z', openAt: '2026-11-10T11:00:00Z', closeAt: '2026-12-19T09:00:00Z', totalSeats: 400, availableSeats: 0, status: 'SOLD_OUT', published: true }
export const DO: Item = { ...D, id: 18, title: '열린 뒤에도 비공개', status: 'OPEN', totalSeats: 100, availableSeats: 100 }
export const DC: Item = { ...D, id: 17, title: '취소된 비공개', status: 'CANCELLED' }
export const P5 = { id: 5, title: '가을 재즈 콘서트', venue: 'OO홀', startAt: '2099-10-01T19:00:00+09:00', openAt: '2099-09-10T10:00:00+09:00', closeAt: '2099-09-30T23:59:59+09:00', totalSeats: 20, availableSeats: 20, status: 'UPCOMING', sections: [{ grade: 'VIP', price: 120000, seatCount: 20, area: 'VIP', stageOrder: 1 }] }

/** 고쳐 쓴 원소 — undefined 는 키 없음(JSON 직렬화가 떨군다) */
export const patched = (base: Item, patch: Item): Item => ({ ...base, ...patch })

/** 응답 본문 `BODY(items, total)` */
export const orgBody = (items: unknown, total: unknown = Array.isArray(items) ? items.length : 0) => ({ items, page: 0, size: 20, totalElements: total })

/** 서버 순서 그대로 id 1000+n 인 예매 가능 공연 n..m */
export const many = (from: number, to: number): Item[] =>
  Array.from({ length: to - from + 1 }, (_, i) => {
    const n = from + i
    return { id: 1000 + n, title: `공연 ${n}`, venue: 'OO홀', startAt: '2026-12-01T10:00:00Z', openAt: '2026-11-01T01:00:00Z', closeAt: '2026-12-01T09:00:00Z', totalSeats: 300, availableSeats: 10, status: 'OPEN', published: true }
  })

// ---------- 가짜 서버 ----------

type Value = Responder | unknown

/** 로그인 상태로 시작 — 저장된 토큰 + /api/auth/me 가 그 사용자 */
export function signedIn(user: User): Record<string, Responder> {
  localStorage.setItem('accessToken', tokenOf(user))
  return { [ME]: () => res(200, user) }
}

export interface ServeOptions {
  /** null 이면 비로그인 */
  user?: User | null
  /** 쿼리 문자열('' · 'filter=DRAFT' · 'page=1' …)별 응답. 함수면 그대로 응답기 */
  list?: Record<string, Value> | Responder
  routes?: Record<string, Responder>
}

export function serve(options: ServeOptions = {}) {
  const user = options.user === undefined ? ORG : options.user
  const list = options.list ?? { '': orgBody([O]) }
  stubServer({
    ...(user ? signedIn(user) : {}),
    [HOME_LIST]: () => res(200, { content: [], page: 0, size: 20, totalElements: 0 }),
    [LIST_KEY]: typeof list === 'function' ? list : listRoute(list),
    ...(options.routes ?? {}),
  })
}

// ---------- 렌더 ----------

function Where() {
  const l = useLocation()
  const from = (l.state as { from?: unknown } | null)?.from
  return (
    <>
      <div data-testid="where">{l.pathname + l.search + l.hash}</div>
      <div data-testid="from">{typeof from === 'string' ? from : ''}</div>
    </>
  )
}
export const where = () => screen.getByTestId('where').textContent
export const fromState = () => screen.getByTestId('from').textContent

let authRef: ReturnType<typeof useAuth> | null = null
function AuthProbe() {
  authRef = useAuth()
  return null
}
/** 화면이 떠 있는 채로 로그인·로그아웃을 부른다(세션 변경) */
export const authApi = () => authRef!

let navigateRef: NavigateFunction | null = null
function NavProbe() {
  navigateRef = useNavigate()
  return null
}
export async function navigateTo(path: string) {
  await act(async () => {
    navigateRef!(path)
  })
}

type Entry = string | { pathname: string; state?: unknown }

export function renderApp(entry: Entry | Entry[], options: { strict?: boolean } = {}) {
  const entries = Array.isArray(entry) ? entry : [entry]
  const tree = (
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={entries} initialIndex={entries.length - 1}>
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

/** `/organizer` 를 연다 */
export function openOrganizer(options: ServeOptions = {}, entry = '/organizer', strict = false) {
  serve(options)
  return renderApp(entry, { strict })
}

/** 응답·효과가 처리되도록 잠깐 기다린다(실제 타이머) */
export const pause = (ms = 40) =>
  act(async () => {
    await new Promise((r) => setTimeout(r, ms))
  })

// ---------- 질의 도우미 ----------

export const ROW_HREF = /^\/organizer\/performances\/(\d+)$/
export const rowLinks = () => screen.queryAllByRole('link').filter((a) => ROW_HREF.test(a.getAttribute('href') ?? ''))
export const rowIds = () => rowLinks().map((a) => Number(ROW_HREF.exec(a.getAttribute('href') ?? '')![1]))
export const rowOf = (id: number) => {
  const found = rowLinks().find((a) => a.getAttribute('href') === `/organizer/performances/${id}`)
  if (!found) throw new Error(`행이 없다: ${id}`)
  return found
}
/** 행 안에 글자가 정확히 그 줄(요소)로 있다 — 글자가 span 으로 쪼개져도 찾는다 */
export const hasLine = (root: Element, text: string) => Array.from(root.querySelectorAll('*')).some((el) => (el.textContent ?? '').trim() === text)
export const BADGES = ['비공개', '예정', '예매 가능', '매진', '마감'] as const
export const badgesIn = (root: Element) => BADGES.filter((b) => hasLine(root, b))

export const CHIPS = ['전체', '비공개', '예정', '예매 가능', '매진', '마감', '취소'] as const
export const QUERY_OF: Record<(typeof CHIPS)[number], string> = {
  전체: '',
  비공개: 'filter=DRAFT',
  예정: 'filter=UPCOMING',
  '예매 가능': 'filter=OPEN',
  매진: 'filter=SOLD_OUT',
  마감: 'filter=CLOSED',
  취소: 'filter=CANCELLED',
}
export const chipGroup = () => screen.queryByRole('group', { name: '상태로 걸러 보기' })
export const chip = (name: string) => within(screen.getByRole('group', { name: '상태로 걸러 보기' })).getByRole('button', { name })
export const chipNames = () => (chipGroup() ? within(chipGroup()!).getAllByRole('button').map((b) => b.textContent ?? '') : [])
export const pressedChips = () => CHIPS.filter((name) => chip(name).getAttribute('aria-pressed') === 'true')

export const LIST_ERROR = '공연 목록을 불러오지 못했어요'
export const RETRY = '다시 시도'
export const MORE = '더 보기'
export const MORE_FAIL = '불러오지 못했어요 · 다시 시도'
export const EMPTY_TITLE = '아직 등록한 공연이 없어요'
export const EMPTY_SUB = '등록하면 비공개로 저장돼요. 준비가 끝나면 공개하세요'
export const FIRST_REGISTER = '첫 공연 등록하기'
export const SEE_ALL = '전체 보기'
export const ONLY_TITLE = '주최자만 이용할 수 있어요'
export const ONLY_SUB = '공연 등록과 관리는 주최자 계정에서 할 수 있어요'
export const BROWSE = '공연 둘러보기'
export const LOGIN_HERO = '쓰던 계정으로 바로 들어와요'

export const skeletons = () => screen.queryAllByTestId('organizer-row-skeleton')
export const errorShown = () => screen.queryByText(LIST_ERROR) !== null
export const retryButton = () => screen.queryByRole('button', { name: RETRY })
export const moreButton = () => screen.queryByRole('button', { name: MORE })
export const moreFailButton = () => screen.queryByRole('button', { name: MORE_FAIL })
export const fab = () => screen.queryByRole('link', { name: '공연 등록' }) ?? screen.queryByRole('button', { name: '공연 등록' })
export const backButton = () => screen.queryByRole('button', { name: '뒤로' })
export const noticeShown = () => screen.queryByText(ONLY_TITLE) !== null
export const heroTitles = () => screen.queryAllByRole('heading', { level: 1 }).map((h) => h.textContent ?? '')
export const listUrls = () => callsTo(LIST_KEY).map((c) => c.url.replace(/^https?:\/\/[^/]+/, ''))
export const LIST_PATH = '/api/organizer/performances'
export const bearerOf = (init: RequestInit) => new Headers(init.headers).get('Authorization')
export const token = () => localStorage.getItem('accessToken')
export const click = async (element: Element) => {
  await act(async () => {
    fireEvent.click(element)
  })
}

/** 로그인 폼을 채워 제출한다 */
export async function loginWith(email = 'org@ticket.on', password = 'password1') {
  fireEvent.change(screen.getByLabelText(/^이메일$/), { target: { value: email } })
  fireEvent.change(screen.getByLabelText(/^비밀번호$/), { target: { value: password } })
  fireEvent.click(screen.getByRole('button', { name: '로그인하기' }))
}

export async function logoutNow() {
  await act(async () => authApi().logout())
}
export async function loginNow(user: User) {
  await act(async () => authApi().login(tokenOf(user), user as never))
}

beforeEach(() => {
  localStorage.clear()
})
afterEach(() => {
  vi.unstubAllEnvs()
  vi.unstubAllGlobals()
  localStorage.clear()
})
