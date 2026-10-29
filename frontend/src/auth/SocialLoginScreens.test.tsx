import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { StrictMode } from 'react'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter, useLocation, useNavigate } from 'react-router-dom'
import App from '../App'
import { theme } from '../theme'
import { AuthProvider } from './AuthContext'

// TASK-013 시나리오 SC-01 ~ SC-14. SoT: workflow_design/05_scenario/SCENARIO_TASK-013.md
// 서버는 두지 않는다 — fetch 응답(상태·본문)만 대체하고 그 위의 코드는 실제로 통과시킨다.
// 표로 묶은 시나리오는 test.each 한 줄(= 시나리오 한 건)이고 행마다 케이스가 하나씩 돈다.
// 앱 전체(App)를 MemoryRouter 로 렌더한다 — 새 모듈을 직접 import 하지 않으므로 컴파일 스켈레톤이 필요 없다.

const MSG = {
  banner: '로그인이 취소되었거나 실패했습니다',
  expired: '로그인 정보가 만료되었습니다. 다시 로그인해 주세요',
  completeFail: '가입을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요',
  alreadyJoined: '이미 가입된 계정입니다. 로그인해 주세요',
  writeRejected: '쓰기 권한 확인에 실패했습니다. 관리자에게 문의해 주세요',
  roleRequired: '역할을 선택해 주세요',
  generic: '소셜 계정으로 가입합니다',
}
const PROVIDERS = [
  { name: '카카오', id: 'kakao' },
  { name: '네이버', id: 'naver' },
  { name: '구글', id: 'google' },
  { name: '애플', id: 'apple' },
]

const performance = {
  id: 5,
  title: '가을 재즈 콘서트',
  venue: 'OO홀',
  startAt: '2026-10-01T19:00:00+09:00',
  openAt: '2026-09-10T10:00:00+09:00',
  closeAt: '2026-09-30T23:59:59+09:00',
  totalSeats: 20,
  availableSeats: 20,
  status: 'UPCOMING',
  sections: [{ grade: 'VIP', price: 120000, seatCount: 20 }],
}
const USER = { id: 1, email: 'hong@example.com', name: '홍길동', role: 'AUDIENCE' }
const OLD_USER = { id: 9, email: 'old@example.com', name: '옛사용자', role: 'AUDIENCE' }
const LOGIN_RESP = { accessToken: 'tok-1', tokenType: 'Bearer', expiresInSeconds: 86400, user: USER }
const COMPLETE_USER = { id: 2, email: 'hong@gmail.com', name: '홍길동', role: 'AUDIENCE' }
const completeResp = (accessToken: string, role: 'AUDIENCE' | 'ORGANIZER' = 'AUDIENCE') => ({
  accessToken,
  tokenType: 'Bearer',
  expiresInSeconds: 86400,
  user: { ...COMPLETE_USER, role },
})

// ---------- 가짜 서버 ----------

type Responder = (init: RequestInit) => Response | Promise<Response>
interface Call {
  key: string
  init: RequestInit
}
let calls: Call[] = []

function res(status: number, body?: unknown): Response {
  const text = body === undefined ? '' : JSON.stringify(body)
  return new Response(text, { status, headers: { 'Content-Type': 'application/json' } })
}
function text(status: number, body: string): Response {
  return new Response(body, { status, headers: { 'Content-Type': 'text/plain' } })
}
function networkError(): Response {
  throw new TypeError('Failed to fetch')
}

function stubServer(routes: Record<string, Responder> = {}) {
  calls = []
  const table: Record<string, Responder> = {
    'GET /api/performances': () => res(200, { content: [performance], page: 0, size: 20, totalElements: 1 }),
    'POST /api/auth/signup': () => res(201, { id: 1, email: 'hong@example.com', name: '홍길동', role: 'ORGANIZER' }),
    'POST /api/auth/login': () => res(200, LOGIN_RESP),
    ...routes,
  }
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) => {
      const path = String(input).split('?')[0]
      const key = `${(init.method ?? 'GET').toUpperCase()} ${path}`
      calls.push({ key, init })
      const respond = table[key]
      return respond ? respond(init) : res(404, { code: 'NOT_FOUND' })
    }),
  )
}

const callsTo = (key: string) => calls.filter((c) => c.key === key)
const authCalls = () => calls.filter((c) => c.key.includes('/api/auth/'))
const bodyOf = (call: Call) => JSON.parse(String(call.init.body)) as Record<string, unknown>
const bearerOf = (init: RequestInit) => (init.headers as Record<string, string> | undefined)?.Authorization

function deferred() {
  let resolve!: (r: Response) => void
  let reject!: (e: unknown) => void
  const promise = new Promise<Response>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

// 붙들어 둔 응답을 늦게 보낸다 — 표의 '늦은 응답' 종류.
type Late = 'ok' | '401' | '401-expired' | '409' | '500' | 'network' | 'non-json'
function settleLate(d: ReturnType<typeof deferred>, late: Late, okBody: unknown) {
  switch (late) {
    case 'ok':
      d.resolve(res(200, okBody))
      break
    case '401':
      d.resolve(res(401, { code: 'UNAUTHORIZED' }))
      break
    case '401-expired':
      d.resolve(res(401, { code: 'PENDING_SIGNUP_EXPIRED', message: 'x' }))
      break
    case '409':
      d.resolve(res(409, { code: 'EMAIL_ALREADY_EXISTS' }))
      break
    case '500':
      d.resolve(text(500, 'boom'))
      break
    case 'network':
      d.reject(new TypeError('Failed to fetch'))
      break
    case 'non-json':
      d.resolve(text(200, '<html>not json</html>'))
      break
  }
}

// ---------- 렌더 ----------

function Where() {
  const l = useLocation()
  return <div data-testid="where">{l.pathname + l.search + l.hash}</div>
}
const where = () => screen.getByTestId('where').textContent

// 테스트가 앱 밖에서 일으키는 이동 — 외부에서 도착하거나 뒤로 가기를 누른 것을 흉내 낸다.
function Nav({ to }: { to?: string }) {
  const navigate = useNavigate()
  return (
    <div>
      {to && <button onClick={() => navigate(to)}>테스트용 이동</button>}
      <button onClick={() => navigate(-1)}>테스트용 뒤로</button>
    </div>
  )
}

type Entry = string | { pathname: string; state?: unknown }
function renderEntries(
  entries: Entry[],
  options: { strict?: boolean; index?: number; navTo?: string } = {},
) {
  const tree = (
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={entries} initialIndex={options.index ?? entries.length - 1}>
        <AuthProvider>
          <App />
          <Where />
          <Nav to={options.navTo} />
        </AuthProvider>
      </MemoryRouter>
    </ThemeProvider>
  )
  return render(options.strict ? <StrictMode>{tree}</StrictMode> : tree)
}
const renderAt = (entry: Entry, options: { strict?: boolean } = {}) => renderEntries([entry], options)

const token = () => localStorage.getItem('accessToken')
const settle = () =>
  act(async () => {
    await new Promise((r) => setTimeout(r, 30))
  })
const clickButton = (name: string) => userEvent.click(screen.getByRole('button', { name }))
const goBack = () => clickButton('테스트용 뒤로')

// pendingToken — 서명은 검증하지 않으므로 모양만 JWT 와 같게 만든다.
const b64url = (s: string) =>
  btoa(String.fromCharCode(...new TextEncoder().encode(s)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
const PENDING_PAYLOAD = {
  email: 'hong@gmail.com',
  name: '홍길동',
  provider: 'KAKAO',
  providerUserId: 'k-1',
  emailVerified: true,
  typ: 'pending',
}
const pendingToken = (payload: unknown = PENDING_PAYLOAD) =>
  `${b64url('{"alg":"HS256","typ":"JWT"}')}.${b64url(typeof payload === 'string' ? payload : JSON.stringify(payload))}.c2ln`
const PT = pendingToken()
const callbackWith = (t: string) => `/oauth2/redirect?pendingToken=${encodeURIComponent(t)}`

const roleRadio = (name: '관객' | '주최자') => screen.getByRole('radio', { name })

// 콜백으로 역할 선택 화면에 도착한다.
async function arriveAtRoleSelection(t: string = PT) {
  renderAt(callbackWith(t))
  await screen.findByRole('button', { name: '계속하기' })
}

function expectLoggedOutHeader() {
  expect(screen.getByRole('link', { name: '로그인' })).toBeInTheDocument()
  expect(screen.getByRole('link', { name: '가입' })).toBeInTheDocument()
}

beforeEach(() => {
  localStorage.clear()
})
afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  vi.unstubAllEnvs()
  vi.restoreAllMocks()
  localStorage.clear()
})

describe('TASK-013 소셜 로그인 버튼·콜백·역할 선택', () => {
  // ---------- SC-01 ----------
  const BUTTON_ROWS: { label: string; path: string; env: string | undefined; base: string }[] = [
    { label: '/login · 미설정', path: '/login', env: undefined, base: '' },
    { label: '/signup · 미설정', path: '/signup', env: undefined, base: '' },
    { label: '/login · 빈 문자열', path: '/login', env: '', base: '' },
    { label: '/login · 공백만', path: '/login', env: '   ', base: '' },
    { label: '/login · 주소', path: '/login', env: 'https://api.example.test', base: 'https://api.example.test' },
    { label: '/login · 끝 슬래시', path: '/login', env: 'https://api.example.test/', base: 'https://api.example.test' },
    { label: '/signup · 연속 슬래시', path: '/signup', env: 'https://api.example.test//', base: 'https://api.example.test' },
  ]
  test.each(BUTTON_ROWS)(
    'SC-01 (happy) 로그인·가입 화면에 소셜 버튼 네 개가 보이고 각각 인가 주소로 이동한다 — $label',
    ({ path, env, base }) => {
      // Given 로그인하지 않았고 VITE_API_BASE_URL 은 표의 값이다
      stubServer()
      if (env !== undefined) vi.stubEnv('VITE_API_BASE_URL', env)

      // When 표의 화면을 연다
      renderAt(path)

      // Then 소셜 로그인 묶음 안에 네 링크가 이 순서로 있다
      const group = screen.getByRole('group', { name: '소셜 로그인' })
      const links = within(group).getAllByRole('link')
      expect(links.map((l) => l.textContent)).toEqual(PROVIDERS.map((p) => p.name))
      // And 각 링크의 이동 주소
      expect(links.map((l) => l.getAttribute('href'))).toEqual(
        PROVIDERS.map((p) => `${base}/oauth2/authorization/${p.id}`),
      )
      // And 이메일 폼과 소셜 버튼 사이에 구분선이 있다
      expect(screen.getByText('또는')).toBeInTheDocument()
      // And 이메일 폼의 입력란·제출 버튼은 그대로 있다
      expect(screen.getByLabelText(/^이메일/)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: path === '/login' ? '로그인' : '가입하기' })).toBeInTheDocument()
    },
  )

  // ---------- SC-02 ----------
  const TOKEN_ROWS: { label: string; strict?: boolean; start: 'anon' | 'old'; query: string; decoded: string }[] = [
    { label: '일반', start: 'anon', query: '?token=tok-1', decoded: 'tok-1' },
    { label: 'StrictMode', strict: true, start: 'anon', query: '?token=tok-1', decoded: 'tok-1' },
    { label: '퍼센트 인코딩', start: 'anon', query: '?token=ab%2Bcd%3D%2F', decoded: 'ab+cd=/' },
    { label: '모르는 파라미터 동반', start: 'anon', query: '?token=tok-1&foo=bar', decoded: 'tok-1' },
    { label: '앞뒤 공백', start: 'anon', query: '?token=%20tok-1%20', decoded: 'tok-1' },
    { label: 'pendingToken 빈 값 동반', start: 'anon', query: '?token=tok-1&pendingToken=', decoded: 'tok-1' },
    { label: 'pendingToken 공백만 동반', start: 'anon', query: '?token=tok-1&pendingToken=%20', decoded: 'tok-1' },
    { label: '이미 로그인한 상태', start: 'old', query: '?token=tok-1', decoded: 'tok-1' },
  ]
  test.each(TOKEN_ROWS)(
    'SC-02 (happy) 콜백에 token 이 오면 사용자 정보를 얻어 로그인하고 홈으로 이동한다 — $label',
    async (row) => {
      // Given 시작 상태는 표와 같고, 서버는 디코드된 토큰이면 홍길동으로 응답한다(응답은 붙들어 둔다)
      const held = deferred()
      stubServer({
        'GET /api/auth/me': (init) => {
          const bearer = bearerOf(init)
          if (bearer === 'Bearer old-tok') return res(200, OLD_USER)
          return bearer === `Bearer ${row.decoded}` ? held.promise : res(401, { code: 'UNAUTHORIZED' })
        },
      })
      const callback = `/oauth2/redirect${row.query}`

      // When 콜백을 연다
      if (row.start === 'old') {
        localStorage.setItem('accessToken', 'old-tok')
        renderEntries(['/'], { navTo: callback })
        expect(await screen.findByText('옛사용자')).toBeInTheDocument()
        await clickButton('테스트용 이동')
      } else {
        renderEntries(['/login', callback], { strict: row.strict })
      }

      // Then 응답을 기다리는 동안 스피너가 보이고 입력란·역할 라디오는 없다
      expect(await screen.findByRole('progressbar', { name: '로그인 처리 중' })).toBeInTheDocument()
      expect(screen.queryAllByRole('textbox')).toHaveLength(0)
      expect(screen.queryAllByRole('radio')).toHaveLength(0)
      // And GET /api/auth/me 가 디코드된 토큰으로 간다
      await waitFor(() =>
        expect(callsTo('GET /api/auth/me').map((c) => bearerOf(c.init))).toContain(`Bearer ${row.decoded}`),
      )
      // And 응답이 오면 localStorage 의 accessToken 이 디코드된 토큰이다
      held.resolve(res(200, USER))
      await waitFor(() => expect(token()).toBe(row.decoded))
      // And 홈으로 이동하고 헤더에 홍길동과 로그아웃이 보인다
      await waitFor(() => expect(where()).toBe('/'))
      expect(await screen.findByText('홍길동')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '로그아웃' })).toBeInTheDocument()
      // And 주소에 token 이 남지 않고, 뒤로 가기로 콜백(토큰이 든 주소)에 돌아가지 않는다
      expect(where()).not.toContain('token')
      await goBack()
      await settle()
      expect(where()).toBe(row.start === 'old' ? '/' : '/login')
    },
  )

  // ---------- SC-03 ----------
  const BAD_CALLBACK_ROWS: { label: string; query: string; me: Responder | null }[] = [
    { label: '쿼리 없음', query: '', me: null },
    { label: 'token 빈 값', query: '?token=', me: null },
    { label: 'token 공백만', query: '?token=%20', me: null },
    { label: 'pendingToken 빈 값', query: '?pendingToken=', me: null },
    { label: 'pendingToken 공백만', query: '?pendingToken=%20', me: null },
    { label: '둘 다 빈 값', query: '?token=&pendingToken=', me: null },
    { label: '둘 다 공백만', query: '?token=%20&pendingToken=%20', me: null },
    { label: '둘 다 값이 있음', query: '?token=tok-1&pendingToken=pt-1', me: null },
    { label: '모르는 파라미터만', query: '?foo=bar', me: null },
    { label: 'me 401', query: '?token=tok-1', me: () => res(401, { code: 'UNAUTHORIZED' }) },
    { label: 'me 500 비 JSON', query: '?token=tok-1', me: () => text(500, 'boom') },
    { label: 'me 네트워크 오류', query: '?token=tok-1', me: () => networkError() },
    { label: 'me 200 빈 바디', query: '?token=tok-1', me: () => res(200) },
    { label: 'me 200 비 JSON', query: '?token=tok-1', me: () => text(200, '<html>no</html>') },
    { label: 'me 200 이름 빈 값', query: '?token=tok-1', me: () => res(200, { email: 'a@b.c', name: '' }) },
  ]
  test.each(BAD_CALLBACK_ROWS)(
    'SC-03 (error) 콜백이 정상이 아니면 로그인 화면으로 돌아가 오류 배너를 보인다 — $label',
    async ({ query, me }) => {
      // Given 로그인하지 않았고 토큰이 없으며, 서버가 표의 응답을 한다
      stubServer(me ? { 'GET /api/auth/me': me } : {})

      // When 콜백을 표의 쿼리로 연다
      renderEntries(['/login', `/oauth2/redirect${query}`])

      // Then /login?error=social 로 이동한다
      await waitFor(() => expect(where()).toBe('/login?error=social'))
      // And 배너가 보이고 헤더는 비로그인이다
      expect(screen.getByText(MSG.banner)).toBeInTheDocument()
      expectLoggedOutHeader()
      // And 토큰이 저장되지 않는다
      await settle()
      expect(token()).toBeNull()
      // And 응답이 '—' 인 행은 서버로 어떤 요청도 가지 않는다
      if (!me) expect(calls).toHaveLength(0)
      // And 뒤로 가기로 콜백 주소에 돌아가지 않는다
      await goBack()
      await settle()
      expect(where()).toBe('/login')
    },
  )

  // ---------- SC-04 ----------
  const PENDING_ROWS: { label: string; strict?: boolean; query: (t: string) => string }[] = [
    { label: '일반', query: (t) => `?pendingToken=${encodeURIComponent(t)}` },
    { label: 'StrictMode', strict: true, query: (t) => `?pendingToken=${encodeURIComponent(t)}` },
    { label: 'token 빈 값', query: (t) => `?token=&pendingToken=${encodeURIComponent(t)}` },
    { label: 'token 공백만', query: (t) => `?token=%20&pendingToken=${encodeURIComponent(t)}` },
  ]
  test.each(PENDING_ROWS)(
    'SC-04 (happy) 콜백에 pendingToken 이 오면 역할 선택 화면으로 이동한다 — $label',
    async ({ strict, query }) => {
      // Given 로그인하지 않았고 pendingToken 의 페이로드는 표준 값이다
      stubServer()

      // When 콜백을 표의 방식·쿼리로 연다
      renderEntries(['/login', `/oauth2/redirect${query(PT)}`], { strict })

      // Then /oauth2/role 로 이동하고 제목과 안내가 보인다
      await waitFor(() => expect(where()).toBe('/oauth2/role'))
      expect(screen.getByRole('heading', { name: '거의 다 됐어요' })).toBeInTheDocument()
      expect(screen.getByText('홍길동님, hong@gmail.com 으로 가입합니다')).toBeInTheDocument()
      // And 역할 라디오는 둘 다 선택되어 있지 않고 계속하기 버튼이 있다
      expect(roleRadio('관객')).not.toBeChecked()
      expect(roleRadio('주최자')).not.toBeChecked()
      expect(screen.getByRole('button', { name: '계속하기' })).toBeInTheDocument()
      // And 주소 어디에도 pendingToken 이 남지 않는다
      expect(decodeURIComponent(where() ?? '')).not.toContain(PT)
      expect(where()).not.toContain('pendingToken')
      // And 서버로 어떤 요청도 가지 않고 토큰이 없다
      expect(calls).toHaveLength(0)
      expect(token()).toBeNull()
      // And 뒤로 가기로 콜백 주소에 돌아가지 않는다
      await goBack()
      await settle()
      expect(where()).toBe('/login')
    },
  )

  // ---------- SC-05 ----------
  const PAYLOAD_ROWS: { label: string; token: () => string; guide: string; noBold?: boolean }[] = [
    { label: '한글 이름', token: () => pendingToken(), guide: '홍길동님, hong@gmail.com 으로 가입합니다' },
    {
      label: '4바이트 문자 이름',
      token: () => pendingToken({ email: 'hong@gmail.com', name: '😀길동' }),
      guide: '😀길동님, hong@gmail.com 으로 가입합니다',
    },
    {
      label: 'base64url 의 - _ 포함',
      token: () => {
        const t = pendingToken({ email: 'a+b@example.com', name: 'ÿÿÿ' })
        // 하네스 가정: 이 토큰의 페이로드에 base64url 전용 문자(- 또는 _)가 실제로 들어 있다
        expect(t.split('.')[1]).toMatch(/[-_]/)
        return t
      },
      guide: 'ÿÿÿ님, a+b@example.com 으로 가입합니다',
    },
    {
      label: 'HTML 형태 이름',
      token: () => pendingToken({ email: 'hong@gmail.com', name: '<b>길동</b>' }),
      guide: '<b>길동</b>님, hong@gmail.com 으로 가입합니다',
      noBold: true,
    },
    { label: 'email 누락', token: () => pendingToken({ name: '홍길동' }), guide: MSG.generic },
    { label: 'name 누락', token: () => pendingToken({ email: 'hong@gmail.com' }), guide: MSG.generic },
    { label: 'name 빈 문자열', token: () => pendingToken({ email: 'hong@gmail.com', name: '' }), guide: MSG.generic },
    { label: 'name 숫자', token: () => pendingToken({ email: 'hong@gmail.com', name: 123 }), guide: MSG.generic },
    { label: 'email 숫자', token: () => pendingToken({ email: 123, name: '홍길동' }), guide: MSG.generic },
    { label: '페이로드가 JSON 아님', token: () => pendingToken('not-json'), guide: MSG.generic },
    { label: '페이로드가 JSON 배열', token: () => pendingToken('["a","b"]'), guide: MSG.generic },
    { label: 'base64 깨짐', token: () => 'aaa.!!!.ccc', guide: MSG.generic },
    { label: '점 없는 임의 문자열', token: () => 'pt-opaque', guide: MSG.generic },
  ]
  test.each(PAYLOAD_ROWS)(
    'SC-05 (boundary) pendingToken 페이로드가 달라도 역할 선택은 동작하고 안내 문구만 바뀐다 — $label',
    async ({ token: makeToken, guide, noBold }) => {
      // Given 로그인하지 않았고 표의 토큰으로 역할 선택 화면에 도착한다
      stubServer()

      // When 역할 선택 화면이 열린다
      await arriveAtRoleSelection(makeToken())

      // Then 안내 문구가 표의 값이다
      expect(screen.getByText(guide)).toBeInTheDocument()
      // And 라디오와 계속하기 버튼이 있다
      expect(roleRadio('관객')).toBeInTheDocument()
      expect(roleRadio('주최자')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '계속하기' })).toBeInTheDocument()
      // And 이름·이메일은 글자 그대로 보이고 HTML 로 해석되지 않는다
      if (noBold) expect(document.querySelector('b')).toBeNull()
    },
  )

  // ---------- SC-06 ----------
  test.each([
    { label: '관객', radio: '관객' as const, role: 'AUDIENCE' as const },
    { label: '주최자', radio: '주최자' as const, role: 'ORGANIZER' as const },
  ])(
    'SC-06 (happy) 역할을 골라 제출하면 가입을 완료하고 로그인해 홈으로 이동한다 — $label',
    async ({ radio, role }) => {
      // Given 역할 선택 화면에 pendingToken 으로 도착했고, 표의 역할을 골랐다
      const held = deferred()
      stubServer({ 'POST /api/auth/social/complete': () => held.promise })
      await arriveAtRoleSelection(PT)
      await userEvent.click(roleRadio(radio))

      // When 계속하기를 두 번 연달아 누른다
      await userEvent.click(screen.getByRole('button', { name: '계속하기' }))
      await userEvent.click(screen.getByRole('button', { name: '계속하기' }))

      // Then 응답을 기다리는 동안 계속하기가 비활성이고 요청은 한 번만 간다
      expect(screen.getByRole('button', { name: '계속하기' })).toBeDisabled()
      await waitFor(() => expect(callsTo('POST /api/auth/social/complete')).toHaveLength(1))
      // And 요청 본문
      expect(bodyOf(callsTo('POST /api/auth/social/complete')[0])).toEqual({ pendingToken: PT, role })
      // And 응답이 오면 토큰이 저장된다
      held.resolve(res(200, completeResp('tok-2', role)))
      await waitFor(() => expect(token()).toBe('tok-2'))
      // And 홈으로 이동하고 헤더에 홍길동과 로그아웃이 보인다
      await waitFor(() => expect(where()).toBe('/'))
      expect(await screen.findByText('홍길동')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '로그아웃' })).toBeInTheDocument()
    },
  )

  // ---------- SC-07 ----------
  test('SC-07 (error) 역할을 고르지 않고 제출하면 인라인 오류를 보이고 요청하지 않는다', async () => {
    // Given 역할 선택 화면에 도착했고 역할을 고르지 않았다
    stubServer()
    await arriveAtRoleSelection()

    // When 계속하기를 누른다
    await clickButton('계속하기')

    // Then 역할 선택 아래에 오류가 보인다
    expect(screen.getByText(MSG.roleRequired)).toBeInTheDocument()
    // And 서버로 어떤 요청도 가지 않고 /oauth2/role 에 머문다
    expect(calls).toHaveLength(0)
    expect(where()).toBe('/oauth2/role')
    // And 역할 라디오 하나를 고르면 오류가 사라진다
    await userEvent.click(roleRadio('관객'))
    expect(screen.queryByText(MSG.roleRequired)).not.toBeInTheDocument()
  })

  // ---------- SC-08 ----------
  test('SC-08 (error) pendingToken 이 만료되면 안내와 로그인 화면으로 가는 버튼을 보인다', async () => {
    // Given 역할 선택 화면에서 관객을 골랐고, 서버가 401 PENDING_SIGNUP_EXPIRED 로 응답한다
    stubServer({
      'POST /api/auth/social/complete': () => res(401, { code: 'PENDING_SIGNUP_EXPIRED', message: '...' }),
    })
    await arriveAtRoleSelection()
    await userEvent.click(roleRadio('관객'))

    // When 계속하기를 누른다
    await clickButton('계속하기')

    // Then 만료 안내와 버튼이 보인다
    expect(await screen.findByText(MSG.expired)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그인 화면으로' })).toBeInTheDocument()
    // And 더 제출할 수 없다
    expect(screen.queryByRole('button', { name: '계속하기' })).not.toBeInTheDocument()
    expect(screen.queryAllByRole('radio')).toHaveLength(0)
    // And 토큰이 없다
    expect(token()).toBeNull()
    // And 버튼을 누르면 /login 으로 이동하고 배너는 없다
    await clickButton('로그인 화면으로')
    expect(where()).toBe('/login')
    expect(screen.queryByText(MSG.banner)).not.toBeInTheDocument()
  })

  // ---------- SC-09 ----------
  const FAIL_ROWS: { label: string; resp: Responder; message: string; link: boolean }[] = [
    { label: '400', resp: () => res(400, { code: 'VALIDATION_ERROR' }), message: MSG.completeFail, link: false },
    { label: '401 다른 code', resp: () => res(401, { code: 'UNAUTHORIZED' }), message: MSG.completeFail, link: false },
    { label: '401 빈 바디', resp: () => res(401), message: MSG.completeFail, link: false },
    { label: '500 비 JSON', resp: () => text(500, 'boom'), message: MSG.completeFail, link: false },
    { label: '네트워크 오류', resp: () => networkError(), message: MSG.completeFail, link: false },
    { label: '200 accessToken 없음', resp: () => res(200, { user: COMPLETE_USER }), message: MSG.completeFail, link: false },
    {
      label: '200 이름 빈 값',
      resp: () => res(200, { accessToken: 'tok-2', user: { ...COMPLETE_USER, name: '' } }),
      message: MSG.completeFail,
      link: false,
    },
    { label: '쓰기 가드', resp: () => res(403, { code: 'WRITE_KEY_INVALID' }), message: MSG.writeRejected, link: false },
    { label: '409 이메일', resp: () => res(409, { code: 'EMAIL_ALREADY_EXISTS' }), message: MSG.alreadyJoined, link: true },
    {
      label: '409 소셜 연결',
      resp: () => res(409, { code: 'SOCIAL_ACCOUNT_ALREADY_LINKED' }),
      message: MSG.alreadyJoined,
      link: true,
    },
  ]
  test.each(FAIL_ROWS)(
    'SC-09 (error) 만료가 아닌 가입 완료 실패는 문구를 보이고 입력을 유지한 채 다시 시도할 수 있다 — $label',
    async ({ resp, message, link }) => {
      // Given 역할 선택 화면에서 주최자를 골랐고, 서버가 표의 응답을 한다
      stubServer({ 'POST /api/auth/social/complete': resp })
      await arriveAtRoleSelection()
      await userEvent.click(roleRadio('주최자'))

      // When 계속하기를 누른다
      await clickButton('계속하기')

      // Then 표의 문구가 보이고 만료 안내는 보이지 않는다
      const alert = await screen.findByRole('alert')
      expect(alert.textContent).toBe(message)
      expect(screen.queryByText(MSG.expired)).not.toBeInTheDocument()
      // And 주최자가 선택된 채 계속하기가 다시 활성이다
      expect(roleRadio('주최자')).toBeChecked()
      expect(screen.getByRole('button', { name: '계속하기' })).toBeEnabled()
      // And 토큰이 없고 /oauth2/role 에 머문다
      expect(token()).toBeNull()
      expect(where()).toBe('/oauth2/role')
      // And 로그인 링크 열
      const links = within(alert).queryAllByRole('link')
      if (link) {
        expect(links).toHaveLength(1)
        expect(links[0].textContent).toBe('로그인')
        expect(links[0].getAttribute('href')).toBe('/login')
      } else {
        expect(links).toHaveLength(0)
      }
    },
  )

  // ---------- SC-10 ----------
  const RETRY_ROWS = [
    { label: '500 비 JSON', first: () => text(500, 'boom'), gone: MSG.completeFail },
    { label: '409 이메일', first: () => res(409, { code: 'EMAIL_ALREADY_EXISTS' }), gone: MSG.alreadyJoined },
    { label: '쓰기 가드', first: () => res(403, { code: 'WRITE_KEY_INVALID' }), gone: MSG.writeRejected },
  ]
  test.each(RETRY_ROWS)(
    'SC-10 (error) 가입 완료가 실패한 뒤 다시 제출하면 이전 문구가 사라지고 성공하면 로그인한다 — $label',
    async ({ first, gone }) => {
      // Given 관객을 골랐고 첫 제출이 표의 실패로 끝나 그 문구가 보인다
      const held = deferred()
      let n = 0
      stubServer({ 'POST /api/auth/social/complete': () => (++n === 1 ? first() : held.promise) })
      await arriveAtRoleSelection()
      await userEvent.click(roleRadio('관객'))
      await clickButton('계속하기')
      expect((await screen.findByRole('alert')).textContent).toBe(gone)

      // When 계속하기를 다시 누른다
      await clickButton('계속하기')

      // Then 응답을 기다리는 동안 이전 실패 문구가 보이지 않는다
      await waitFor(() => expect(callsTo('POST /api/auth/social/complete')).toHaveLength(2))
      expect(screen.queryByText(gone)).not.toBeInTheDocument()
      // And 응답이 오면 토큰이 저장되고 홈으로 이동한다
      held.resolve(res(200, completeResp('tok-3')))
      await waitFor(() => expect(token()).toBe('tok-3'))
      await waitFor(() => expect(where()).toBe('/'))
    },
  )

  // ---------- SC-11 ----------
  const BANNER_ROWS = [
    { label: 'error=social', query: '?error=social', shown: true },
    { label: 'error=other', query: '?error=other', shown: false },
    { label: 'error= (빈 값)', query: '?error=', shown: false },
    { label: 'error=SOCIAL (대소문자)', query: '?error=SOCIAL', shown: false },
    { label: '쿼리 없음', query: '', shown: false },
  ]
  test.each(BANNER_ROWS)(
    'SC-11 (error) 로그인 화면은 error=social 일 때만 실패 배너를 보인다 — $label',
    ({ query, shown }) => {
      // Given 로그인하지 않았다
      stubServer()

      // When /login 을 표의 쿼리로 연다
      renderAt(`/login${query}`)

      // Then 배너의 표시 여부가 표와 같다
      if (shown) expect(screen.getByText(MSG.banner)).toBeInTheDocument()
      else expect(screen.queryByText(MSG.banner)).not.toBeInTheDocument()
      // And 폼·소셜 링크가 있고 주소의 쿼리가 지워지지 않는다
      expect(screen.getByLabelText(/^이메일/)).toBeInTheDocument()
      expect(screen.getByLabelText(/^비밀번호/)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '로그인' })).toBeInTheDocument()
      expect(within(screen.getByRole('group', { name: '소셜 로그인' })).getAllByRole('link')).toHaveLength(4)
      expect(where()).toBe(`/login${query}`)
    },
  )

  // ---------- SC-12 ----------
  const STATE_ROWS: { label: string; state: unknown }[] = [
    { label: '없음', state: undefined },
    { label: '빈 문자열', state: { pendingToken: '' } },
    { label: '공백만', state: { pendingToken: '  ' } },
    { label: '문자열이 아님', state: { pendingToken: 123 } },
  ]
  test.each(STATE_ROWS)(
    'SC-12 (boundary) 역할 선택 화면은 이동할 때 넘어온 pendingToken 이 없으면 열리지 않는다 — $label',
    async ({ state }) => {
      // Given 로그인하지 않았다
      stubServer()

      // When /oauth2/role 을 표의 값으로 직접 연다
      renderAt({ pathname: '/oauth2/role', state })

      // Then /login 으로 이동하고 배너는 없다
      await waitFor(() => expect(where()).toBe('/login'))
      expect(screen.queryByText(MSG.banner)).not.toBeInTheDocument()
      // And 서버로 어떤 요청도 가지 않는다
      expect(calls).toHaveLength(0)
    },
  )

  // ---------- SC-13 ----------
  type RaceKind = 'restore' | 'callback-nav' | 'callback-back' | 'role-nav'
  const lateRows = (kind: RaceKind, lates: Late[]) => lates.map((late) => ({ kind, late, label: `${kind} · ${late}` }))
  const RACE_ROWS = [
    ...lateRows('restore', ['ok', '401', '500', 'network', 'non-json']),
    ...lateRows('callback-nav', ['ok', '401', '500', 'network', 'non-json']),
    ...lateRows('callback-back', ['ok', '401']),
    ...lateRows('role-nav', ['ok', '401-expired', '409', '500', 'network', 'non-json']),
  ]
  test.each(RACE_ROWS)(
    'SC-13 (boundary) 응답이 오기 전에 상태가 이미 바뀌었으면 늦은 응답은 그 상태를 덮어쓰지 않는다 — $label',
    async ({ kind, late }) => {
      const held = deferred()
      if (kind === 'restore') {
        // Given 저장된 old-tok 로 복원이 진행 중이고, 그동안 new-tok 콜백 로그인이 끝났다
        localStorage.setItem('accessToken', 'old-tok')
        stubServer({
          'GET /api/auth/me': (init) => (bearerOf(init) === 'Bearer old-tok' ? held.promise : res(200, USER)),
        })
        renderAt('/oauth2/redirect?token=new-tok')
        await waitFor(() => expect(token()).toBe('new-tok'))
        await waitFor(() => expect(where()).toBe('/'))

        // When 붙들어 둔 복원 응답을 늦게 보낸다
        settleLate(held, late, OLD_USER)
        await settle()

        // Then 헤더가 새 사용자이고 저장 토큰이 new-tok 이다
        expect(await screen.findByText('홍길동')).toBeInTheDocument()
        expect(screen.queryByText('옛사용자')).not.toBeInTheDocument()
        expect(token()).toBe('new-tok')
        return
      }

      if (kind === 'callback-nav') {
        // Given 콜백의 사용자 조회가 진행 중이고, 사용자가 헤더의 가입 링크로 /signup 에 갔다
        stubServer({ 'GET /api/auth/me': () => held.promise })
        renderAt('/oauth2/redirect?token=tok-1')
        await screen.findByRole('progressbar', { name: '로그인 처리 중' })
        await userEvent.click(screen.getByRole('link', { name: '가입' }))
        expect(where()).toBe('/signup')
        settleLate(held, late, USER)
      } else if (kind === 'callback-back') {
        // Given 콜백의 사용자 조회가 진행 중이고, 사용자가 브라우저 뒤로 가기로 이전 화면에 갔다
        stubServer({ 'GET /api/auth/me': () => held.promise })
        renderEntries(['/', '/oauth2/redirect?token=tok-1'])
        await screen.findByRole('progressbar', { name: '로그인 처리 중' })
        await goBack()
        expect(where()).toBe('/')
        settleLate(held, late, USER)
      } else {
        // Given 역할 선택의 가입 완료가 진행 중이고, 사용자가 헤더의 가입 링크로 /signup 에 갔다
        stubServer({ 'POST /api/auth/social/complete': () => held.promise })
        await arriveAtRoleSelection()
        await userEvent.click(roleRadio('관객'))
        await clickButton('계속하기')
        await waitFor(() => expect(callsTo('POST /api/auth/social/complete')).toHaveLength(1))
        await userEvent.click(screen.getByRole('link', { name: '가입' }))
        expect(where()).toBe('/signup')
        settleLate(held, late, completeResp('tok-2'))
      }

      // When 붙들어 둔 응답을 늦게 보냈다
      await settle()

      // Then 도착한 화면에 머물고 헤더가 비로그인이며 토큰이 없다
      expect(where()).toBe(kind === 'callback-back' ? '/' : '/signup')
      expectLoggedOutHeader()
      expect(token()).toBeNull()
      // And 도착한 화면에 실패 배너·가입 완료 실패 문구가 없다
      expect(screen.queryByText(MSG.banner)).not.toBeInTheDocument()
      expect(screen.queryByText(MSG.completeFail)).not.toBeInTheDocument()
    },
  )

  // ---------- SC-14 ----------
  const REGRESSION_ROWS = [
    { label: '/login 로그인', path: '/login', submit: '로그인', key: 'POST /api/auth/login', after: '/', stored: 'tok-1' },
    { label: '/signup 가입하기', path: '/signup', submit: '가입하기', key: 'POST /api/auth/signup', after: '/login', stored: null },
  ]
  test.each(REGRESSION_ROWS)(
    'SC-14 (regression) 소셜 버튼이 있어도 이메일 로그인·가입은 그대로 동작한다 — $label',
    async ({ path, submit, key, after, stored }) => {
      // Given 표의 화면에서 올바른 값을 모두 입력했고 서버가 성공으로 응답한다
      stubServer()
      renderAt(path)
      fireEvent.change(screen.getByLabelText(/^이메일/), { target: { value: 'hong@example.com' } })
      fireEvent.change(screen.getByLabelText(/^비밀번호(?! *확인)/), { target: { value: 'password1!' } })
      if (path === '/signup') {
        fireEvent.change(screen.getByLabelText(/^비밀번호 확인/), { target: { value: 'password1!' } })
        fireEvent.change(screen.getByLabelText(/^이름/), { target: { value: '홍길동' } })
        fireEvent.click(screen.getByRole('radio', { name: '주최자' }))
      }

      // When 표의 제출 버튼을 누른다
      await clickButton(submit)

      // Then 표의 요청이 한 번만 가고 다른 인증 경로·인가 주소로는 가지 않는다
      await waitFor(() => expect(callsTo(key)).toHaveLength(1))
      expect(authCalls().map((c) => c.key)).toEqual([key])
      expect(calls.filter((c) => c.key.includes('/oauth2/authorization/'))).toHaveLength(0)
      // And 표의 이동 결과
      await waitFor(() => expect(where()).toBe(after))
      expect(token()).toBe(stored)
      if (path === '/login') expect(await screen.findByText('홍길동')).toBeInTheDocument()
    },
  )
})
