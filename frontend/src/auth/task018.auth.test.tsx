import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { StrictMode } from 'react'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter, useLocation, useNavigate } from 'react-router-dom'
import App from '../App'
import { theme } from '../theme'
import { Field } from '../components/ui'
import { token as designToken } from '../theme/tokens'
import type { TokenName } from '../theme/tokens'
import { AuthProvider, useAuth } from './AuthContext'

// TASK-018 시나리오 SC-01 ~ SC-35. SoT: workflow_design/05_scenario/SCENARIO_TASK-018.md
// 서버는 두지 않는다 — fetch 응답(상태·본문)만 대체하고 앱 전체(App)를 MemoryRouter 로 지난다.
// 표로 묶은 시나리오는 test.each 한 줄(= 시나리오 한 건)이고 행마다 케이스가 하나씩 돈다.

const MSG = {
  heroLogin: '쓰던 계정으로 바로 들어와요',
  badCredentials: '이메일 또는 비밀번호가 올바르지 않아요',
  socialFailed: '로그인이 취소됐거나 실패했어요',
  joinedThenLogin: '가입은 끝났어요. 로그인해 주세요',
  loginFailed: '로그인하지 못했어요. 잠시 후 다시 시도해 주세요',
  signUpFailed: '가입하지 못했어요. 잠시 후 다시 시도해 주세요',
  // 옛 쓰기 키 거부 문구 — 쓰기 가드가 사라져 어떤 화면에도 나오지 않는다 (TASK-024)
  oldWriteRejected: '쓰기 권한 확인에 실패했습니다. 관리자에게 문의해 주세요',
  pwHelp: '8자 이상 72자 이하로 정해요',
  mismatch: '비밀번호가 일치하지 않아요',
  duplicate: '이미 가입된 이메일이에요',
  emailRequired: '이메일을 입력해 주세요',
  emailMax: '이메일은 255자 이하로 입력해 주세요',
  passwordRequired: '비밀번호를 입력해 주세요',
  confirmRequired: '비밀번호를 한 번 더 입력해 주세요',
  nameRequired: '이름을 입력해 주세요',
  nameMax: '이름은 100자 이하로 입력해 주세요',
  roleRequired: '역할을 골라 주세요',
  pendingLoading: '가입 정보를 불러오고 있어요',
  pendingFailed: '가입 정보를 불러오지 못했어요',
  roleNotice: '5분 안에 골라 주세요. 가입한 뒤에는 역할을 바꿀 수 없어요',
  expiredTitle: '가입 시간이 지났어요',
  expiredBody: '5분이 지나 역할을 고를 수 없어요. 다시 로그인해 주세요',
  completeFailed: '가입을 끝내지 못했어요. 잠시 후 다시 시도해 주세요',
  alreadyJoined: '이미 가입된 계정이에요',
  copied: '아이디를 복사했어요 — 일행에게 붙여넣어 보내세요',
  copyFallback: '길게 눌러 복사해 주세요',
}
const PROVIDERS = [
  { id: 'kakao', upper: 'KAKAO', name: '카카오' },
  { id: 'naver', upper: 'NAVER', name: '네이버' },
  { id: 'google', upper: 'GOOGLE', name: '구글' },
  { id: 'apple', upper: 'APPLE', name: '애플' },
] as const

const HANDLE = 'wave7302'
const USER = { id: 1, email: 'a@b.com', name: '김하린', role: 'AUDIENCE', handle: HANDLE }
const loginBody = (accessToken = 'tok-1', user: Record<string, unknown> = USER) => ({
  accessToken,
  tokenType: 'Bearer',
  expiresInSeconds: 86400,
  user,
})
const PENDING = {
  provider: 'KAKAO',
  email: 'harin.kim@kakao.com',
  name: '김하린',
  expiresAt: '',
}
const pendingBody = (over: Record<string, unknown> = {}) => ({
  ...PENDING,
  expiresAt: new Date(Date.now() + 300_000).toISOString(),
  ...over,
})
const performance = {
  id: 1,
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

// ---------- 가짜 서버 ----------

type Responder = (init: RequestInit) => Response | Promise<Response>
interface Call {
  key: string
  init: RequestInit
}
let calls: Call[] = []

// 요청마다 새 Response 를 만든다 — 본문은 한 번만 읽힌다.
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
    'GET /api/performances/1': () => res(200, performance),
    'POST /api/auth/signup': () => res(201, { id: 1, email: 'a@b.com', name: '김하린', role: 'AUDIENCE', handle: HANDLE }),
    'POST /api/auth/login': () => res(200, loginBody()),
    'GET /api/auth/me': () => res(200, USER),
    'POST /api/auth/social/pending': () => res(200, pendingBody()),
    'POST /api/auth/social/complete': () => res(200, loginBody('tok-5')),
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
const bodyOf = (call: Call) => JSON.parse(String(call.init.body)) as Record<string, unknown>
const LOGIN = 'POST /api/auth/login'
const SIGNUP = 'POST /api/auth/signup'
const PENDING_KEY = 'POST /api/auth/social/pending'
const COMPLETE = 'POST /api/auth/social/complete'
const ME = 'GET /api/auth/me'

function deferred() {
  let resolve!: (r: Response) => void
  let reject!: (e: unknown) => void
  const promise = new Promise<Response>((ok, fail) => {
    resolve = ok
    reject = fail
  })
  return { promise, resolve, reject }
}

// ---------- 렌더 ----------

function Where() {
  const l = useLocation()
  return <div data-testid="where">{l.pathname + l.search + l.hash}</div>
}
const where = () => screen.getByTestId('where').textContent

// 앱 밖에서 일으키는 일 — 다른 화면으로 이동, 다른 계정으로 로그인, 로그아웃.
function Outside({ to }: { to?: string }) {
  const navigate = useNavigate()
  const auth = useAuth()
  return (
    <div>
      {to && <button onClick={() => navigate(to)}>테스트용 이동</button>}
      <button onClick={() => auth.login('tok-0', { ...USER, id: 9, name: '다른사람', handle: 'echo0001' } as never)}>
        테스트용 로그인
      </button>
      <button onClick={() => auth.logout()}>테스트용 로그아웃</button>
    </div>
  )
}

type Entry = string | { pathname: string; state?: unknown }
function renderAt(entry: Entry, options: { strict?: boolean; navTo?: string } = {}) {
  const tree = (
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[entry]}>
        <AuthProvider>
          <App />
          <Where />
          <Outside to={options.navTo} />
        </AuthProvider>
      </MemoryRouter>
    </ThemeProvider>
  )
  return render(options.strict ? <StrictMode>{tree}</StrictMode> : tree)
}

const token = () => localStorage.getItem('accessToken')
const settle = () =>
  act(async () => {
    await new Promise((r) => setTimeout(r, 30))
  })
const outside = (name: '테스트용 이동' | '테스트용 로그인' | '테스트용 로그아웃') =>
  fireEvent.click(screen.getByRole('button', { name }))

// ---------- 폼 ----------

const LABEL = {
  email: /^이메일$/,
  password: /^비밀번호$/,
  confirm: /^비밀번호 확인$/,
  name: /^이름$/,
}
const input = (label: RegExp) => screen.getByLabelText(label) as HTMLInputElement
function setField(label: RegExp, value: string) {
  fireEvent.change(input(label), { target: { value } })
}
// 칸의 aria-describedby 가 가리키는 글자 — 「그 칸 아래」 를 확인한다.
function describedText(label: RegExp): string {
  const ids = (input(label).getAttribute('aria-describedby') ?? '').split(/\s+/).filter(Boolean)
  return ids.map((id) => document.getElementById(id)?.textContent ?? '').join(' ')
}
const invalid = (label: RegExp) => input(label).getAttribute('aria-invalid') === 'true'

interface SignUpValues {
  email: string
  password: string
  confirm: string
  name: string
  role: '관객' | '주최자' | null
}
const SIGNUP_OK: SignUpValues = { email: 'a@b.com', password: 'password1', confirm: 'password1', name: '김하린', role: '관객' }
function fillSignUp(over: Partial<SignUpValues> = {}): SignUpValues {
  const v = { ...SIGNUP_OK, ...over }
  setField(LABEL.email, v.email)
  setField(LABEL.password, v.password)
  setField(LABEL.confirm, v.confirm)
  setField(LABEL.name, v.name)
  if (v.role) fireEvent.click(screen.getByRole('radio', { name: new RegExp(`^${v.role}`) }))
  return v
}
function fillLogin(email = 'a@b.com', password = 'password1') {
  setField(LABEL.email, email)
  setField(LABEL.password, password)
}
const click = (name: string) => userEvent.click(screen.getByRole('button', { name }))
const roleRow = (name: '관객' | '주최자') => screen.getByRole('radio', { name: new RegExp(`^${name}`) })
const NUL = '\u0000'
const LONE_LOW = '\uDC00'
const LONE_HIGH = '\uD800'
const NFD_E = 'é'
const headerGone = () => {
  expect(screen.queryByRole('link', { name: '가입' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: '로그아웃' })).not.toBeInTheDocument()
}

// 역할 선택 화면에 pendingToken 을 들고 온다 (콜백이 넘긴 라우트 state).
async function arriveAtRole(pendingToken: unknown = 'pend-1', options: { strict?: boolean; navTo?: string } = {}) {
  renderAt({ pathname: '/oauth2/role', state: { pendingToken } }, options)
}
async function roleReady() {
  await screen.findByRole('button', { name: '계속하기' })
}

beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()
})
afterEach(() => {
  cleanup()
  vi.useRealTimers()
  vi.unstubAllGlobals()
  vi.unstubAllEnvs()
  vi.restoreAllMocks()
  localStorage.clear()
  sessionStorage.clear()
})

describe('TASK-018 인증 화면 5종을 시안대로', () => {
  // ---------- SC-01 ----------
  test('SC-01 (happy) 로그인 화면은 시안의 요소를 보인다', async () => {
    // Given 로그인하지 않았다
    stubServer()

    // When /login 을 연다
    renderAt('/login')

    // Then 제목과 보조 문구
    expect(screen.getByRole('heading', { name: MSG.heroLogin })).toBeInTheDocument()
    expect(screen.getByText('카카오·네이버·구글·애플 계정이면 처음이어도 버튼 한 번으로 시작해요')).toBeInTheDocument()
    // And 칸과 로그인하기 버튼
    expect(input(LABEL.email)).toBeInTheDocument()
    expect(input(LABEL.password)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그인하기' })).toBeInTheDocument()
    // And 소셜 로그인 묶음 안 네 링크가 이 순서
    const links = within(screen.getByRole('group', { name: '소셜 로그인' })).getAllByRole('link')
    expect(links.map((l) => l.textContent)).toEqual(PROVIDERS.map((p) => p.name))
    // And 계정이 없나요? 줄의 이메일로 가입 링크 → /signup
    expect(screen.getByText('계정이 없나요?')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('link', { name: '이메일로 가입' }))
    expect(where()).toBe('/signup')
    // And 전역 헤더가 없다
    headerGone()
  })

  // ---------- SC-02 ----------
  test('SC-02 (error) 로그인 401 INVALID_CREDENTIALS 면 두 칸이 함께 오류가 된다', async () => {
    // Given 서버가 401 INVALID_CREDENTIALS
    stubServer({ [LOGIN]: () => res(401, { code: 'INVALID_CREDENTIALS', message: 'x' }) })
    renderAt('/login')

    // When
    fillLogin('a@b.com', 'wrongpass1')
    await click('로그인하기')

    // Then 두 칸이 모두 aria-invalid
    await waitFor(() => expect(invalid(LABEL.email)).toBe(true))
    expect(invalid(LABEL.password)).toBe(true)
    // And 비밀번호 칸 아래 문구가 한 번만
    expect(screen.getAllByText(MSG.badCredentials)).toHaveLength(1)
    expect(describedText(LABEL.password)).toContain(MSG.badCredentials)
    // And 경로 그대로, 토큰 없음
    expect(where()).toBe('/login')
    expect(token()).toBeNull()
  })

  // ---------- SC-03 ----------
  const SC03: { label: string; respond: Responder; message: string }[] = [
    { label: '401 WRITE_KEY_INVALID(옛 쓰기 가드 코드)', respond: () => res(401, { code: 'WRITE_KEY_INVALID' }), message: MSG.loginFailed },
    { label: '401 다른 code', respond: () => res(401, { code: 'UNAUTHORIZED' }), message: MSG.loginFailed },
    { label: '401 빈 본문', respond: () => res(401), message: MSG.loginFailed },
    { label: '500 비 JSON', respond: () => text(500, '<html>oops</html>'), message: MSG.loginFailed },
    { label: '네트워크 오류', respond: networkError, message: MSG.loginFailed },
    {
      label: '200 handle 없음',
      respond: () => res(200, loginBody('tok-1', { id: 1, email: 'a@b.com', name: '김하린', role: 'AUDIENCE' })),
      message: MSG.loginFailed,
    },
    { label: '200 handle ""', respond: () => res(200, loginBody('tok-1', { ...USER, handle: '' })), message: MSG.loginFailed },
    { label: '200 handle 공백만', respond: () => res(200, loginBody('tok-1', { ...USER, handle: '  ' })), message: MSG.loginFailed },
    { label: '200 handle 숫자', respond: () => res(200, loginBody('tok-1', { ...USER, handle: 7302 })), message: MSG.loginFailed },
    { label: '200 handle null', respond: () => res(200, loginBody('tok-1', { ...USER, handle: null })), message: MSG.loginFailed },
    { label: '200 빈 본문', respond: () => res(200), message: MSG.loginFailed },
  ]
  test.each(SC03)('SC-03 (error) 로그인 응답이 판정 코드가 아니면 일반 실패 문구를 보인다 — $label', async ({ respond, message }) => {
    // Given 서버가 표의 응답
    stubServer({ [LOGIN]: respond })
    renderAt('/login')

    // When
    fillLogin('a@b.com', 'password1')
    await click('로그인하기')

    // Then 표의 문구, 두 칸은 오류가 아니다
    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(screen.queryByText(MSG.oldWriteRejected)).not.toBeInTheDocument()
    expect(invalid(LABEL.email)).toBe(false)
    expect(invalid(LABEL.password)).toBe(false)
    // And 경로 그대로, 토큰 없음
    expect(where()).toBe('/login')
    expect(token()).toBeNull()
  })

  // ---------- SC-04 ----------
  const SC04: { label: string; state: unknown; to: string }[] = [
    { label: 'state 없음', state: undefined, to: '/' },
    { label: 'from null', state: { from: null }, to: '/' },
    { label: '숫자', state: { from: 42 }, to: '/' },
    { label: '객체', state: { from: { pathname: '/x' } }, to: '/' },
    { label: '앱 안 경로', state: { from: '/performances/7?tab=seat#top' }, to: '/performances/7?tab=seat#top' },
    { label: '프로토콜 상대', state: { from: '//evil.example' }, to: '/' },
    { label: '절대 URL', state: { from: 'https://evil.example/x' }, to: '/' },
    { label: '역슬래시', state: { from: '/\\evil.example' }, to: '/' },
    { label: '앞 공백', state: { from: ' /performances/7' }, to: '/' },
    { label: '끝 LF', state: { from: '/performances/7\n' }, to: '/performances/7\n' },
    { label: '/login 순환', state: { from: '/login?x=1' }, to: '/' },
    { label: '/signup 순환', state: { from: '/signup' }, to: '/' },
  ]
  test.each(SC04)('SC-04 (happy) 로그인에 성공하면 원래 가려던 화면으로 돌아간다 — $label', async ({ state, to }) => {
    // Given 서버가 200, /login 의 state.from 은 표의 값
    stubServer({ 'GET /api/performances/7': () => res(200, { ...performance, id: 7 }) })
    renderAt({ pathname: '/login', state })

    // When
    fillLogin()
    await click('로그인하기')

    // Then 토큰 저장
    await waitFor(() => expect(token()).toBe('tok-1'))
    // And 표의 이동 위치
    await waitFor(() => expect(where()).toBe(to))
  })

  // ---------- SC-05 ----------
  const SC05: { label: string; email: string; password: string; blocked?: string; sent?: { email: string; password: string } }[] = [
    { label: '이메일 빈 값', email: '', password: 'password1', blocked: MSG.emailRequired },
    { label: '이메일 공백만', email: '   ', password: 'password1', blocked: MSG.emailRequired },
    { label: '비밀번호 빈 값', email: 'a@b.com', password: '', blocked: MSG.passwordRequired },
    { label: '비밀번호 공백만', email: 'a@b.com', password: '   ', sent: { email: 'a@b.com', password: '   ' } },
    {
      label: '이메일 앞뒤 공백 · 비밀번호 NUL',
      email: '  a@b.com  ',
      password: `pass${NUL}word`,
      sent: { email: 'a@b.com', password: `pass${NUL}word` },
    },
    { label: '이메일 NUL', email: `a${NUL}@b.com`, password: 'password1', sent: { email: `a${NUL}@b.com`, password: 'password1' } },
    {
      label: '짝 없는 서로게이트',
      email: `a${LONE_LOW}@b.com`,
      password: `${LONE_HIGH}abcdefgh`,
      sent: { email: `a${LONE_LOW}@b.com`, password: `${LONE_HIGH}abcdefgh` },
    },
    {
      label: 'NFD · 80 유닛',
      email: `café@b.com`,
      password: NFD_E.repeat(40),
      sent: { email: `café@b.com`, password: NFD_E.repeat(40) },
    },
  ]
  test.each(SC05)('SC-05 (error) 로그인 칸이 비면 요청하지 않고, 값은 바꾸지 않고 보낸다 — $label', async (row) => {
    // Given 로그인 화면
    stubServer()
    renderAt('/login')

    // When 표의 값으로 로그인하기
    fillLogin(row.email, row.password)
    await click('로그인하기')

    // Then 표의 결과
    if (row.blocked) {
      const field = row.blocked === MSG.emailRequired ? LABEL.email : LABEL.password
      expect(await screen.findByText(row.blocked)).toBeInTheDocument()
      expect(describedText(field)).toContain(row.blocked)
      expect(callsTo(LOGIN)).toHaveLength(0)
    } else {
      await waitFor(() => expect(callsTo(LOGIN)).toHaveLength(1))
      expect(bodyOf(callsTo(LOGIN)[0])).toEqual(row.sent)
    }
  })

  // ---------- SC-06 ----------
  test.each([
    { label: '200', second: () => res(200, loginBody()), after: 'ok' as const },
    { label: '500 빈 본문', second: () => res(500), after: 'fail' as const },
  ])('SC-06 (error) 로그인 오류는 다시 제출하면 사라진다 — $label', async ({ second, after }) => {
    // Given 첫 요청 401, 둘째 요청은 붙들어 둔다
    const held = deferred()
    let n = 0
    stubServer({ [LOGIN]: () => (++n === 1 ? res(401, { code: 'INVALID_CREDENTIALS' }) : held.promise) })
    renderAt('/login')
    fillLogin('a@b.com', 'wrongpass1')
    await click('로그인하기')
    expect(await screen.findByText(MSG.badCredentials)).toBeInTheDocument()

    // When 비밀번호를 고쳐 다시 제출
    setField(LABEL.password, 'password1')
    await click('로그인하기')

    // Then 응답 전에 오류가 사라진다
    await waitFor(() => expect(callsTo(LOGIN)).toHaveLength(2))
    expect(screen.queryByText(MSG.badCredentials)).not.toBeInTheDocument()
    expect(invalid(LABEL.email)).toBe(false)
    expect(invalid(LABEL.password)).toBe(false)
    // And 둘째 응답 뒤 표의 상태
    held.resolve(second())
    if (after === 'ok') {
      await waitFor(() => expect(where()).toBe('/'))
      expect(screen.queryByText(MSG.badCredentials)).not.toBeInTheDocument()
    } else {
      expect(await screen.findByText(MSG.loginFailed)).toBeInTheDocument()
      expect(invalid(LABEL.email)).toBe(false)
      expect(invalid(LABEL.password)).toBe(false)
    }
  })

  // ---------- SC-07 ----------
  test.each([
    { query: '?error=social', shown: true },
    { query: '', shown: false },
    { query: '?error=', shown: false },
    { query: '?error=SOCIAL', shown: false },
    { query: '?error=other', shown: false },
    { query: '?error=social%20', shown: false },
  ])('SC-07 (happy) ?error=social 일 때만 맨 위에 소셜 실패 안내가 나온다 — "$query"', ({ query, shown }) => {
    // Given
    stubServer()

    // When
    renderAt(`/login${query}`)

    // Then
    if (shown) expect(screen.getByText(MSG.socialFailed)).toBeInTheDocument()
    else expect(screen.queryByText(MSG.socialFailed)).not.toBeInTheDocument()
  })

  // ---------- SC-08 ----------
  test('SC-08 (happy) 가입 화면은 시안의 요소를 보인다', async () => {
    // Given
    stubServer()

    // When
    renderAt('/signup')

    // Then 제목과 안내
    expect(screen.getByRole('heading', { name: '이메일로 가입' })).toBeInTheDocument()
    expect(screen.getByText('이메일과 역할은 가입한 뒤에 바꿀 수 없어요')).toBeInTheDocument()
    // And 네 칸, 비밀번호 칸 아래 안내(오류 아님)
    for (const l of [LABEL.email, LABEL.password, LABEL.confirm, LABEL.name]) expect(input(l)).toBeInTheDocument()
    expect(describedText(LABEL.password)).toBe(MSG.pwHelp)
    expect(invalid(LABEL.password)).toBe(false)
    // And 역할 묶음, 아무것도 고르지 않음
    const group = screen.getByRole('radiogroup', { name: '역할' })
    const radios = within(group).getAllByRole('radio')
    expect(radios.map((r) => r.textContent)).toEqual(['관객', '주최자'])
    radios.forEach((r) => expect(r).not.toBeChecked())
    // And 이미 계정이 있나요? 로그인 → /login
    expect(screen.getByText('이미 계정이 있나요?')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '로그인' })).toHaveAttribute('href', '/login')
    // And 가입하기 버튼, 소셜 묶음·전역 헤더 없음
    expect(screen.getByRole('button', { name: '가입하기' })).toBeInTheDocument()
    expect(screen.queryByRole('group', { name: '소셜 로그인' })).not.toBeInTheDocument()
    headerGone()
  })

  // ---------- SC-09 ----------
  const same = (pw: string): Partial<SignUpValues> => ({ password: pw, confirm: pw })
  const SC09: { label: string; over: Partial<SignUpValues>; field: RegExp | null; message?: string; pass?: boolean }[] = [
    { label: '이메일 빈 값', over: { email: '' }, field: LABEL.email, message: MSG.emailRequired },
    { label: '이메일 공백만', over: { email: '   ' }, field: LABEL.email, message: MSG.emailRequired },
    { label: '이메일 256자', over: { email: 'a'.repeat(250) + '@b.com' }, field: LABEL.email, message: MSG.emailMax },
    { label: '비밀번호 빈 값', over: same(''), field: LABEL.password, message: MSG.passwordRequired },
    { label: '비밀번호 공백 8', over: same(' '.repeat(8)), field: LABEL.password, message: MSG.passwordRequired },
    { label: '비밀번호 7자', over: same('pass123'), field: LABEL.password, message: MSG.pwHelp },
    { label: '비밀번호 73자', over: same('a'.repeat(73)), field: LABEL.password, message: MSG.pwHelp },
    { label: '비밀번호 NFD 74 유닛', over: same(NFD_E.repeat(37)), field: LABEL.password, message: MSG.pwHelp },
    { label: '확인 빈 값', over: { confirm: '' }, field: LABEL.confirm, message: MSG.confirmRequired },
    { label: '확인 공백만', over: { confirm: '   ' }, field: LABEL.confirm, message: MSG.mismatch },
    { label: '확인 다름', over: { confirm: 'password2' }, field: LABEL.confirm, message: MSG.mismatch },
    { label: '확인 79자', over: { confirm: 'password1' + 'a'.repeat(70) }, field: LABEL.confirm, message: MSG.mismatch },
    { label: '이름 빈 값', over: { name: '' }, field: LABEL.name, message: MSG.nameRequired },
    { label: '이름 공백만', over: { name: '   ' }, field: LABEL.name, message: MSG.nameRequired },
    { label: '이름 101자', over: { name: '가'.repeat(101) }, field: LABEL.name, message: MSG.nameMax },
    { label: '역할 미선택', over: { role: null }, field: null, message: MSG.roleRequired },
    { label: '통과: 비밀번호 8자', over: same('pass1234'), field: null, pass: true },
    { label: '통과: 비밀번호 72자', over: same('a'.repeat(72)), field: null, pass: true },
    { label: '통과: 비밀번호 NFD 72 유닛', over: same(NFD_E.repeat(36)), field: null, pass: true },
    { label: '통과: 이메일 255자', over: { email: 'a'.repeat(249) + '@b.com' }, field: null, pass: true },
    { label: '통과: 이름 100자', over: { name: '가'.repeat(100) }, field: null, pass: true },
  ]
  test.each(SC09)('SC-09 (error) 가입 칸이 규칙을 벗어나면 그 칸 아래 오류가 나오고 요청하지 않는다 — $label', async (row) => {
    // Given 나머지 칸은 유효 — 통과 행은 가입 응답을 붙들어 둔다(성공하면 화면을 떠나 칸을 볼 수 없다)
    stubServer(row.pass ? { [SIGNUP]: () => deferred().promise } : {})
    renderAt('/signup')
    fillSignUp(row.over)

    // When
    await click('가입하기')

    if (row.pass) {
      // 경계 안쪽 짝 — 오류 없이 요청이 간다
      await waitFor(() => expect(callsTo(SIGNUP)).toHaveLength(1))
      for (const l of [LABEL.email, LABEL.password, LABEL.confirm, LABEL.name]) expect(invalid(l)).toBe(false)
      return
    }
    // Then 그 칸이 aria-invalid 이고 칸 아래 문구
    if (row.field) {
      await waitFor(() => expect(invalid(row.field!)).toBe(true))
      expect(describedText(row.field)).toContain(row.message!)
    } else {
      expect(await screen.findByText(row.message!)).toBeInTheDocument()
    }
    // 안내 문구가 오류로 바뀐 행은 같은 문구가 한 번만 있다
    if (row.message === MSG.pwHelp) expect(screen.getAllByText(MSG.pwHelp)).toHaveLength(1)
    // And 요청 없음
    expect(callsTo(SIGNUP)).toHaveLength(0)
  })

  // ---------- SC-10 ----------
  const SC10: { label: string; over: Partial<SignUpValues>; expect: Record<string, string> }[] = [
    { label: '이메일 앞뒤 공백', over: { email: '  a@b.com  ' }, expect: { email: 'a@b.com' } },
    { label: '이메일 NUL', over: { email: `a${NUL}@b.com` }, expect: { email: `a${NUL}@b.com` } },
    { label: '이메일 서로게이트', over: { email: `a${LONE_LOW}@b.com` }, expect: { email: `a${LONE_LOW}@b.com` } },
    { label: '이메일 NFD', over: { email: `café@b.com` }, expect: { email: `café@b.com` } },
    { label: '비밀번호 NUL', over: same(`pass${NUL}word`), expect: { password: `pass${NUL}word`, passwordConfirm: `pass${NUL}word` } },
    {
      label: '비밀번호 서로게이트',
      over: same(`${LONE_HIGH}abcdefgh`),
      expect: { password: `${LONE_HIGH}abcdefgh`, passwordConfirm: `${LONE_HIGH}abcdefgh` },
    },
    {
      label: '비밀번호 NFD 72 유닛',
      over: same(NFD_E.repeat(36)),
      expect: { password: NFD_E.repeat(36), passwordConfirm: NFD_E.repeat(36) },
    },
    { label: '이름 NUL', over: { name: `김${NUL}하린` }, expect: { name: `김${NUL}하린` } },
    { label: '이름 서로게이트', over: { name: `김${LONE_LOW}` }, expect: { name: `김${LONE_LOW}` } },
    {
      label: '이름 NFD 한글',
      over: { name: '하린' },
      expect: { name: '하린' },
    },
  ]
  test.each(SC10)('SC-10 (happy) 가입 값은 바꾸지 않고 보낸다 (이메일 앞뒤 공백만 걷는다) — $label', async (row) => {
    // Given 201·200
    stubServer()
    renderAt('/signup')

    // When 표의 값 + 관객
    fillSignUp({ ...row.over, role: '관객' })
    await click('가입하기')

    // Then signup 본문
    await waitFor(() => expect(callsTo(SIGNUP)).toHaveLength(1))
    const base = { email: 'a@b.com', password: 'password1', passwordConfirm: 'password1', name: '김하린', role: 'AUDIENCE' }
    expect(bodyOf(callsTo(SIGNUP)[0])).toEqual({ ...base, ...row.expect })
  })

  // ---------- SC-11 ----------
  test('SC-11 (error) 이미 가입된 이메일이면 이메일 칸에서 알리고 로그인 링크를 준다', async () => {
    // Given 첫 요청 409, 둘째 요청은 붙들어 둔다
    const held = deferred()
    let n = 0
    stubServer({ [SIGNUP]: () => (++n === 1 ? res(409, { code: 'EMAIL_ALREADY_EXISTS' }) : held.promise) })
    renderAt('/signup')
    fillSignUp()

    // When
    await click('가입하기')

    // Then 이메일 칸 오류 + 칸 아래 문구와 링크
    await waitFor(() => expect(invalid(LABEL.email)).toBe(true))
    expect(describedText(LABEL.email)).toContain(MSG.duplicate)
    const message = screen.getByText(MSG.duplicate, { exact: false })
    const link = within(message.closest('[id]') ?? message).getByRole('link', { name: '로그인하기' })
    expect(link).toHaveAttribute('href', '/login')
    // And 아래 줄이 없다
    expect(screen.queryByText('이미 계정이 있나요?')).not.toBeInTheDocument()
    // And 이메일을 바꿔 다시 제출하면 응답 전에 중복 문구가 사라지고 줄이 돌아온다
    setField(LABEL.email, 'c@d.com')
    await click('가입하기')
    await waitFor(() => expect(callsTo(SIGNUP)).toHaveLength(2))
    expect(screen.queryByText(MSG.duplicate, { exact: false })).not.toBeInTheDocument()
    expect(screen.getByText('이미 계정이 있나요?')).toBeInTheDocument()
    held.resolve(res(500))
    // And 링크는 /login 으로 간다 (새로 그린 화면에서 확인)
    cleanup()
    stubServer({ [SIGNUP]: () => res(409, { code: 'EMAIL_ALREADY_EXISTS' }) })
    renderAt('/signup')
    fillSignUp()
    await click('가입하기')
    await userEvent.click(await screen.findByRole('link', { name: '로그인하기' }))
    expect(where()).toBe('/login')
  })

  // ---------- SC-12 ----------
  const SC12: { label: string; respond: Responder; message: string }[] = [
    { label: '409 다른 code', respond: () => res(409, { code: 'SOCIAL_ACCOUNT_ALREADY_LINKED' }), message: MSG.signUpFailed },
    { label: '401 WRITE_KEY_INVALID(옛 쓰기 가드 코드)', respond: () => res(401, { code: 'WRITE_KEY_INVALID' }), message: MSG.signUpFailed },
    { label: '400', respond: () => res(400, { code: 'VALIDATION_FAILED' }), message: MSG.signUpFailed },
    { label: '200 빈 본문', respond: () => res(200), message: MSG.signUpFailed },
    { label: '500 빈 본문', respond: () => res(500), message: MSG.signUpFailed },
    { label: '502 비 JSON', respond: () => text(502, 'Bad Gateway'), message: MSG.signUpFailed },
    { label: '네트워크 오류', respond: networkError, message: MSG.signUpFailed },
  ]
  test.each(SC12)('SC-12 (error) 가입 응답이 201 도 중복도 아니면 일반 실패 문구를 보인다 — $label', async ({ respond, message }) => {
    // Given 첫 요청은 표의 응답, 다시 누른 요청은 붙들어 둔다
    const held = deferred()
    let n = 0
    stubServer({ [SIGNUP]: (init) => (++n === 1 ? respond(init) : held.promise) })
    renderAt('/signup')
    fillSignUp()

    // When
    await click('가입하기')

    // Then 표의 문구, 이메일 칸은 오류 아님
    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(screen.queryByText(MSG.oldWriteRejected)).not.toBeInTheDocument()
    expect(invalid(LABEL.email)).toBe(false)
    // And login 요청 없음, 경로 그대로
    expect(callsTo(LOGIN)).toHaveLength(0)
    expect(where()).toBe('/signup')
    // 다시 누르면 응답 전에 문구가 사라진다
    await click('가입하기')
    await waitFor(() => expect(callsTo(SIGNUP)).toHaveLength(2))
    expect(screen.queryByText(message)).not.toBeInTheDocument()
  })

  // ---------- SC-13 ----------
  test.each([
    { label: '관객', role: '관객' as const, code: 'AUDIENCE', shown: '이메일 · 관객' },
    { label: '주최자', role: '주최자' as const, code: 'ORGANIZER', shown: '이메일 · 주최자' },
    { label: '공백 포함 비밀번호', role: '관객' as const, code: 'AUDIENCE', shown: '이메일 · 관객', password: ' pass word1 ' },
  ])('SC-13 (happy) 가입에 성공하면 같은 자격 증명으로 로그인해 가입 완료 화면으로 간다 — $label', async (row) => {
    // Given 201 · 200(tok-9, 그 역할)
    stubServer({ [LOGIN]: () => res(200, loginBody('tok-9', { ...USER, role: row.code })) })
    renderAt('/signup')
    const password = row.password ?? 'password1'

    // When
    fillSignUp({ email: '  a@b.com ', password, confirm: password, name: '김하린', role: row.role })
    await click('가입하기')

    // Then signup 의 role, login 본문
    await waitFor(() => expect(callsTo(LOGIN)).toHaveLength(1))
    expect(bodyOf(callsTo(SIGNUP)[0]).role).toBe(row.code)
    expect(bodyOf(callsTo(LOGIN)[0])).toEqual({ email: 'a@b.com', password })
    // And 토큰 저장, /joined
    await waitFor(() => expect(where()).toBe('/joined'))
    expect(token()).toBe('tok-9')
    // And 윗줄·제목·아이디
    expect(screen.getByText(row.shown)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '가입이 끝났어요' })).toBeInTheDocument()
    expect(screen.getByText('내 티켓온 아이디')).toBeInTheDocument()
    expect(screen.getByText(HANDLE)).toBeInTheDocument()
  })

  // ---------- SC-14 ----------
  const SC14: { label: string; respond: Responder }[] = [
    { label: '500 빈 본문', respond: () => res(500) },
    { label: '503 비 JSON', respond: () => text(503, 'down') },
    { label: '401 INVALID_CREDENTIALS', respond: () => res(401, { code: 'INVALID_CREDENTIALS' }) },
    { label: '401 WRITE_KEY_INVALID(옛 쓰기 가드 코드)', respond: () => res(401, { code: 'WRITE_KEY_INVALID' }) },
    { label: '네트워크 오류', respond: networkError },
    { label: '200 빈 본문', respond: () => res(200) },
    {
      label: '200 handle 없음',
      respond: () => res(200, loginBody('tok-9', { id: 1, email: 'a@b.com', name: '김하린', role: 'AUDIENCE' })),
    },
    { label: '200 handle null', respond: () => res(200, loginBody('tok-9', { ...USER, handle: null })) },
    { label: '200 handle ""', respond: () => res(200, loginBody('tok-9', { ...USER, handle: '' })) },
    { label: '200 handle 공백만', respond: () => res(200, loginBody('tok-9', { ...USER, handle: '   ' })) },
    { label: '200 handle 숫자', respond: () => res(200, loginBody('tok-9', { ...USER, handle: 7302 })) },
  ]
  test.each(SC14)('SC-14 (error) 가입 뒤 자동 로그인이 실패하면 로그인 화면에서 안내한다 — $label', async ({ respond }) => {
    // Given 가입 201, 자동 로그인은 표의 응답 (그 화면에서 다시 누른 로그인은 붙들어 둔다)
    const held = deferred()
    let n = 0
    stubServer({ [LOGIN]: (init) => (++n === 1 ? respond(init) : held.promise) })
    renderAt('/signup')
    fillSignUp({ email: 'a@b.com' })

    // When
    await click('가입하기')

    // Then /login 과 맨 위 안내
    await waitFor(() => expect(where()).toBe('/login'))
    expect(screen.getByText(MSG.joinedThenLogin)).toBeInTheDocument()
    // And 이메일 칸 채움, 비밀번호 비움
    expect(input(LABEL.email)).toHaveValue('a@b.com')
    expect(input(LABEL.password)).toHaveValue('')
    // And 토큰 없음
    expect(token()).toBeNull()
    // And 로그인하기를 누르면 안내가 사라진다
    setField(LABEL.password, 'password1')
    await click('로그인하기')
    await waitFor(() => expect(callsTo(LOGIN)).toHaveLength(2))
    expect(screen.queryByText(MSG.joinedThenLogin)).not.toBeInTheDocument()
  })

  // ---------- SC-15 ----------
  type Sc15 = {
    label: string
    path: '/login' | '/signup'
    hold: 'login' | 'signup' | 'auto-login'
    before: 'move' | 'login-other' | 'logout'
    respond: () => Response | 'network'
    expectFn: () => Promise<void>
    strict?: boolean
  }
  const SC15: Sc15[] = [
    {
      label: '로그인 · 이동 · 200',
      path: '/login',
      hold: 'login',
      before: 'move',
      respond: () => res(200, loginBody('tok-1')),
      expectFn: async () => {
        expect(token()).toBeNull()
        expect(where()).toBe('/performances/1')
      },
    },
    {
      label: '로그인 · 이동 · 500',
      path: '/login',
      hold: 'login',
      before: 'move',
      respond: () => res(500),
      expectFn: async () => {
        expect(screen.queryByText(MSG.loginFailed)).not.toBeInTheDocument()
        expect(where()).toBe('/performances/1')
      },
    },
    {
      label: '로그인 · 다른 계정 로그인 · 200',
      path: '/login',
      hold: 'login',
      before: 'login-other',
      respond: () => res(200, loginBody('tok-1')),
      expectFn: async () => {
        await waitFor(() => expect(token()).toBe('tok-1'))
        await waitFor(() => expect(where()).toBe('/'))
      },
    },
    {
      label: '로그인 · 로그아웃 · 401',
      path: '/login',
      hold: 'login',
      before: 'logout',
      respond: () => res(401, { code: 'INVALID_CREDENTIALS' }),
      expectFn: async () => {
        await waitFor(() => expect(invalid(LABEL.email)).toBe(true))
        expect(invalid(LABEL.password)).toBe(true)
      },
    },
    {
      label: '가입 · 이동 · 201',
      path: '/signup',
      hold: 'signup',
      before: 'move',
      respond: () => res(201),
      expectFn: async () => {
        expect(callsTo(LOGIN)).toHaveLength(0)
        expect(where()).toBe('/performances/1')
      },
    },
    {
      label: '가입 · 이동 · 500',
      path: '/signup',
      hold: 'signup',
      before: 'move',
      respond: () => res(500),
      expectFn: async () => {
        expect(screen.queryByText(MSG.signUpFailed)).not.toBeInTheDocument()
        expect(where()).toBe('/performances/1')
      },
    },
    {
      label: '가입 · 다른 계정 로그인 · 201→200',
      path: '/signup',
      hold: 'signup',
      before: 'login-other',
      respond: () => res(201),
      expectFn: async () => {
        await waitFor(() => expect(token()).toBe('tok-9'))
        await waitFor(() => expect(where()).toBe('/joined'))
      },
    },
    {
      label: '자동 로그인 · 이동 · 200',
      path: '/signup',
      hold: 'auto-login',
      before: 'move',
      respond: () => res(200, loginBody('tok-9')),
      expectFn: async () => {
        expect(token()).toBeNull()
        expect(where()).toBe('/performances/1')
      },
    },
    {
      label: '자동 로그인 · 이동 · 500',
      path: '/signup',
      hold: 'auto-login',
      before: 'move',
      respond: () => res(500),
      expectFn: async () => {
        expect(where()).toBe('/performances/1')
      },
    },
    {
      label: '자동 로그인 · 로그아웃 · 200',
      path: '/signup',
      hold: 'auto-login',
      before: 'logout',
      respond: () => res(200, loginBody('tok-9')),
      expectFn: async () => {
        await waitFor(() => expect(token()).toBe('tok-9'))
        await waitFor(() => expect(where()).toBe('/joined'))
      },
    },
    {
      label: 'StrictMode 가입 201→200',
      path: '/signup',
      hold: 'signup',
      before: 'move',
      strict: true,
      respond: () => res(201),
      expectFn: async () => {
        expect(callsTo(SIGNUP)).toHaveLength(1)
      },
    },
  ]
  test.each(SC15)(
    'SC-15 (boundary) 화면을 떠난 뒤 온 응답은 쓰지 않고, 사이에 바뀐 세션은 성공 응답이 덮는다 — $label',
    async (row) => {
      // Given 표의 요청이 응답을 기다린다
      const held = deferred()
      const routes: Record<string, Responder> = {}
      if (row.hold === 'login') routes[LOGIN] = () => held.promise
      if (row.hold === 'signup') {
        routes[SIGNUP] = () => held.promise
        routes[LOGIN] = () => res(200, loginBody('tok-9'))
      }
      if (row.hold === 'auto-login') routes[LOGIN] = () => held.promise
      stubServer(routes)
      renderAt(row.path, { navTo: '/performances/1', strict: row.strict })
      if (row.path === '/login') fillLogin()
      else fillSignUp()
      await click(row.path === '/login' ? '로그인하기' : '가입하기')
      await waitFor(() => expect(callsTo(row.hold === 'signup' ? SIGNUP : LOGIN)).toHaveLength(1))

      if (row.strict) {
        // StrictMode: 이중 마운트에서도 요청은 각각 한 번, 응답 뒤 /joined
        held.resolve(row.respond() as Response)
        await waitFor(() => expect(where()).toBe('/joined'))
        expect(callsTo(SIGNUP)).toHaveLength(1)
        expect(callsTo(LOGIN)).toHaveLength(1)
        return
      }

      // When 표의 일이 일어난 뒤 응답이 온다
      if (row.before === 'move') outside('테스트용 이동')
      if (row.before === 'login-other') outside('테스트용 로그인')
      if (row.before === 'logout') outside('테스트용 로그아웃')
      const r = row.respond()
      if (r === 'network') held.reject(new TypeError('Failed to fetch'))
      else held.resolve(r)
      await settle()

      // Then 표의 결과
      await row.expectFn()
    },
  )

  // ---------- SC-16 ----------
  test.each([
    { label: '/login', path: '/login', button: '로그인하기', key: LOGIN, enter: false },
    { label: '/login Enter', path: '/login', button: '로그인하기', key: LOGIN, enter: true },
    { label: '/signup', path: '/signup', button: '가입하기', key: SIGNUP, enter: false },
    { label: '/signup Enter', path: '/signup', button: '가입하기', key: SIGNUP, enter: true },
    { label: '/oauth2/role', path: '/oauth2/role', button: '계속하기', key: COMPLETE, enter: false },
  ])('SC-16 (happy) 제출 중에는 주 버튼을 다시 누를 수 없다 — $label', async (row) => {
    // Given 응답이 오지 않는다
    const held = deferred()
    stubServer({ [row.key]: () => held.promise })
    if (row.path === '/oauth2/role') {
      await arriveAtRole()
      await roleReady()
      await userEvent.click(roleRow('관객'))
    } else {
      renderAt(row.path)
      if (row.path === '/login') fillLogin()
      else fillSignUp()
    }

    // When 두 번 제출
    if (row.enter) {
      const field = input(LABEL.email)
      fireEvent.submit(field.closest('form')!)
      fireEvent.submit(field.closest('form')!)
    } else {
      await click(row.button)
      await userEvent.setup({ pointerEventsCheck: 0 }).click(screen.getByRole('button', { name: row.button }))
    }

    // Then 비활성, 요청 한 번
    await waitFor(() => expect(screen.getByRole('button', { name: row.button })).toBeDisabled())
    expect(callsTo(row.key)).toHaveLength(1)
    // And 실패 응답이 오면 다시 누를 수 있다
    held.resolve(res(500))
    await waitFor(() => expect(screen.getByRole('button', { name: row.button })).toBeEnabled())
  })

  // ---------- SC-17 ----------
  test.each([...PROVIDERS.map((p) => ({ ...p, blocked: false })), { ...PROVIDERS[0], blocked: true }])(
    'SC-17 (happy) 소셜 버튼을 누르면 제공자를 기록하고 인가 주소로 간다 — $name (저장소 막힘 $blocked)',
    async ({ id, name, blocked }) => {
      // Given 로그인 화면, 기준 주소 미설정
      stubServer()
      renderAt('/login')
      if (blocked) {
        vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
          throw new Error('denied')
        })
      }
      // jsdom 은 링크 이동을 하지 않는다 — 기본 동작이 막혔는지만 기록하고 마지막에 막는다.
      let prevented: boolean | null = null
      const record = (e: Event) => {
        prevented = e.defaultPrevented
        e.preventDefault()
      }
      document.addEventListener('click', record)

      // When 그 버튼을 누른다
      const link = screen.getByRole('link', { name })
      fireEvent.click(link)
      document.removeEventListener('click', record)

      // Then href
      expect(link).toHaveAttribute('href', `/oauth2/authorization/${id}`)
      // And 기록 (막힌 행은 화면이 그대로이고 이동이 막히지 않는다)
      if (blocked) {
        expect(screen.getByRole('button', { name: '로그인하기' })).toBeInTheDocument()
      } else {
        expect(sessionStorage.getItem('socialProvider')).toBe(id)
      }
      expect(prevented).toBe(false)
    },
  )

  // ---------- SC-18 ----------
  const SC18: { label: string; stored: string | null | 'throw'; message: string }[] = [
    ...PROVIDERS.map((p) => ({ label: p.id, stored: p.id as string, message: `${p.name} 계정을 확인하고 있어요` })),
    { label: '없음', stored: null, message: '소셜 계정을 확인하고 있어요' },
    { label: '빈 값', stored: '', message: '소셜 계정을 확인하고 있어요' },
    { label: '대문자', stored: 'KAKAO', message: '소셜 계정을 확인하고 있어요' },
    { label: '모르는 값', stored: 'line', message: '소셜 계정을 확인하고 있어요' },
    { label: '읽기 예외', stored: 'throw', message: '소셜 계정을 확인하고 있어요' },
  ]
  test.each(SC18)('SC-18 (happy) 콜백 화면은 기록된 제공자 이름으로 확인 중임을 알린다 — $label', async ({ stored, message }) => {
    // Given 기록과 붙들린 /me
    const held = deferred()
    stubServer({ [ME]: () => held.promise })
    if (stored === 'throw') {
      vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
        throw new Error('denied')
      })
    } else if (stored !== null) {
      sessionStorage.setItem('socialProvider', stored)
    }

    // When
    renderAt('/oauth2/redirect?token=tok-1')

    // Then 진행 표시와 문구
    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(screen.getByRole('progressbar')).toBeInTheDocument()
  })

  // ---------- SC-19 ----------
  test.each([
    { query: '?token=tok-1', to: '/' },
    { query: '?pendingToken=pend-1', to: '/oauth2/role' },
    { query: '', to: '/login?error=social' },
    { query: '?token=a&pendingToken=b', to: '/login?error=social' },
  ])('SC-19 (regression) 콜백의 토큰 처리는 바뀌지 않는다 — "$query"', async ({ query, to }) => {
    // Given /me 200 (handle 포함)
    stubServer()

    // When
    renderAt(`/oauth2/redirect${query}`)

    // Then
    await waitFor(() => expect(where()).toBe(to))
    if (to === '/') await waitFor(() => expect(token()).toBe('tok-1'))
  })

  // ---------- SC-20 ----------
  test.each(PROVIDERS)('SC-20 (happy) 역할 선택 화면은 서버의 대기 정보로 어느 계정으로 가입 중인지 보인다 — $upper', async ({ upper, name }) => {
    // Given 대기 정보 응답은 붙들어 둔다
    const held = deferred()
    stubServer({ [PENDING_KEY]: () => held.promise })
    await arriveAtRole('pend-1')

    // 응답 전 — 진행 표시만, 계속하기 없음
    expect(await screen.findByRole('progressbar', { name: MSG.pendingLoading })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '계속하기' })).not.toBeInTheDocument()

    // When 응답을 받는다
    held.resolve(res(200, pendingBody({ provider: upper })))
    await roleReady()

    // Then 요청 본문
    expect(callsTo(PENDING_KEY)).toHaveLength(1)
    expect(bodyOf(callsTo(PENDING_KEY)[0])).toEqual({ pendingToken: 'pend-1' })
    // And 윗줄·제목
    expect(screen.getByText(`${name}로 처음 가입`)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'harin.kim@kakao.com(김하린)으로 가입하고 있어요' })).toBeInTheDocument()
    // And 역할 묶음, 안내, 계속하기
    const group = screen.getByRole('radiogroup', { name: '역할' })
    expect(within(group).getByRole('radio', { name: /^관객/ })).toHaveTextContent('공연을 찾고 예매해요')
    expect(within(group).getByRole('radio', { name: /^주최자/ })).toHaveTextContent('공연을 등록하고 관리해요')
    expect(screen.getByText(MSG.roleNotice)).toBeInTheDocument()
  })

  // ---------- SC-21 ----------
  const SC21: { label: string; state: unknown; sent?: string }[] = [
    { label: 'state 없음', state: undefined },
    { label: 'null', state: { pendingToken: null } },
    { label: '빈 값', state: { pendingToken: '' } },
    { label: '공백만', state: { pendingToken: '   ' } },
    { label: '숫자', state: { pendingToken: 123 } },
    { label: '앞 공백', state: { pendingToken: ' pend-1' }, sent: ' pend-1' },
    { label: '끝 LF', state: { pendingToken: 'pend-1\n' }, sent: 'pend-1\n' },
    { label: '=', state: { pendingToken: 'pend-1=' }, sent: 'pend-1=' },
    { label: 'NUL', state: { pendingToken: `pend-${NUL}-1` }, sent: `pend-${NUL}-1` },
    { label: '서로게이트', state: { pendingToken: `pend-${LONE_LOW}` }, sent: `pend-${LONE_LOW}` },
    { label: '비 ASCII', state: { pendingToken: 'pénd-1' }, sent: 'pénd-1' },
  ]
  test.each(SC21)('SC-21 (error) pendingToken 이 없으면 역할 선택을 열지 않고, 있으면 바꾸지 않고 보낸다 — $label', async ({ state, sent }) => {
    // Given
    stubServer()

    // When
    renderAt({ pathname: '/oauth2/role', state })

    // Then
    if (sent === undefined) {
      await waitFor(() => expect(where()).toBe('/login'))
      await settle()
      expect(callsTo(PENDING_KEY)).toHaveLength(0)
      return
    }
    await roleReady()
    await userEvent.click(roleRow('관객'))
    await click('계속하기')
    await waitFor(() => expect(callsTo(COMPLETE)).toHaveLength(1))
    expect(callsTo(PENDING_KEY)).toHaveLength(1)
    expect(bodyOf(callsTo(PENDING_KEY)[0]).pendingToken).toBe(sent)
    expect(bodyOf(callsTo(COMPLETE)[0]).pendingToken).toBe(sent)
  })

  // ---------- SC-22 ----------
  const fieldRows = (field: string, values: unknown[]) =>
    values.map((v) => ({
      label: `${field} ${v === undefined ? '없음' : JSON.stringify(v)}`,
      respond: () => {
        const body: Record<string, unknown> = pendingBody()
        if (v === undefined) delete body[field]
        else body[field] = v
        return res(200, body)
      },
    }))
  const SC22 = [
    ...fieldRows('provider', [undefined, null, 1, 'LINE', 'kakao']),
    ...fieldRows('email', [undefined, null, 42, '', '   ']),
    ...fieldRows('name', [undefined, null, {}, '', '   ']),
    ...fieldRows('expiresAt', [undefined, null, 1762850400, '내일', '2026-11-11T08:40:00']),
    { label: '200 빈 본문', respond: () => res(200) },
    { label: '200 비 JSON', respond: () => text(200, 'ok') },
    { label: '401 다른 code', respond: () => res(401, { code: 'UNAUTHORIZED' }) },
    { label: '401 빈 본문', respond: () => res(401) },
    { label: '401 WRITE_KEY_INVALID(옛 쓰기 가드 코드)', respond: () => res(401, { code: 'WRITE_KEY_INVALID' }) },
    { label: '500 빈 본문', respond: () => res(500) },
    { label: '502 비 JSON', respond: () => text(502, '<html>') },
    { label: '네트워크 오류', respond: networkError },
  ]
  test.each(SC22)(
    'SC-22 (error) 대기 정보가 올바르지 않거나 가져오지 못하면 실패 안내와 다시 시도를 보인다 — $label',
    async ({ respond }) => {
      // Given 첫 응답은 표, 다시 시도는 정상
      let n = 0
      stubServer({ [PENDING_KEY]: () => (++n === 1 ? respond() : res(200, pendingBody())) })
      await arriveAtRole('pend-1')

      // When / Then 실패 안내와 다시 시도하기, 역할 행·계속하기 없음
      expect(await screen.findByText(MSG.pendingFailed)).toBeInTheDocument()
      expect(screen.queryByText(MSG.oldWriteRejected)).not.toBeInTheDocument()
      expect(screen.getByRole('button', { name: '다시 시도하기' })).toBeInTheDocument()
      expect(screen.queryAllByRole('radio')).toHaveLength(0)
      expect(screen.queryByRole('button', { name: '계속하기' })).not.toBeInTheDocument()
      // And 다시 시도하면 같은 본문으로 다시 요청하고 정상이면 역할 선택
      await click('다시 시도하기')
      await roleReady()
      expect(callsTo(PENDING_KEY)).toHaveLength(2)
      expect(bodyOf(callsTo(PENDING_KEY)[1])).toEqual(bodyOf(callsTo(PENDING_KEY)[0]))
      expect(screen.getByText('카카오로 처음 가입')).toBeInTheDocument()
    },
  )

  // ---------- SC-23 ----------
  const NOW = new Date('2026-11-11T08:35:00.000Z')
  const SC23: { label: string; expiresAt: string; checks: [number, 'role' | 'expired'][] }[] = [
    { label: 'Z 경계-1ms', expiresAt: '2026-11-11T08:40:00Z', checks: [[299_999, 'role']] },
    { label: 'Z 경계', expiresAt: '2026-11-11T08:40:00Z', checks: [[300_000, 'expired']] },
    { label: '오프셋 표기', expiresAt: '2026-11-11T17:40:00+09:00', checks: [[299_999, 'role'], [1, 'expired']] },
    { label: '나노초', expiresAt: '2026-11-11T08:40:00.123456789Z', checks: [[300_122, 'role'], [1, 'expired']] },
    { label: '= 지금', expiresAt: '2026-11-11T08:35:00Z', checks: [[0, 'expired']] },
    { label: '이미 지남', expiresAt: '2026-11-11T08:30:00Z', checks: [[0, 'expired']] },
    { label: '30일 뒤', expiresAt: '2026-12-11T08:35:00Z', checks: [[0, 'role'], [2_151_360_000, 'role']] },
  ]
  test.each(SC23)('SC-23 (boundary) 화면에 머물러 있어도 expiresAt 이 되는 순간 만료 상태로 바뀐다 — $label', async ({ expiresAt, checks }) => {
    // Given 가짜 타이머, 지금 08:35, 대기 정보의 expiresAt 은 표의 원문
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval', 'Date'] })
    vi.setSystemTime(NOW)
    stubServer({ [PENDING_KEY]: () => res(200, pendingBody({ expiresAt })) })
    await arriveAtRole('pend-1')
    // fetch → json → 상태 반영까지 흘린다 (시간은 흐르지 않는다)
    for (let i = 0; i < 5; i++) await act(() => vi.advanceTimersByTimeAsync(0))

    // When 표의 시간만큼 흐른다 / Then 표의 화면
    for (const [ms, state] of checks) {
      await act(() => vi.advanceTimersByTimeAsync(ms))
      if (state === 'role') {
        expect(screen.getByRole('button', { name: '계속하기' })).toBeInTheDocument()
        expect(screen.queryByText(MSG.expiredTitle)).not.toBeInTheDocument()
      } else {
        expect(screen.getByRole('heading', { name: MSG.expiredTitle })).toBeInTheDocument()
        expect(screen.getByText(MSG.expiredBody)).toBeInTheDocument()
        expect(screen.getByRole('button', { name: '다시 로그인하기' })).toBeInTheDocument()
        expect(screen.queryAllByRole('radio')).toHaveLength(0)
        expect(screen.queryByRole('button', { name: '계속하기' })).not.toBeInTheDocument()
      }
    }
    // And 만료 상태의 다시 로그인하기 → /login
    if (checks[checks.length - 1][1] === 'expired') {
      fireEvent.click(screen.getByRole('button', { name: '다시 로그인하기' }))
      expect(where()).toBe('/login')
    }
  })

  // ---------- SC-24 ----------
  const SC24: { label: string; on: 'pending' | 'complete'; respond: Responder; expired: boolean; other?: string }[] = [
    { label: 'pending 만료', on: 'pending', respond: () => res(401, { code: 'PENDING_SIGNUP_EXPIRED' }), expired: true },
    { label: 'complete 만료', on: 'complete', respond: () => res(401, { code: 'PENDING_SIGNUP_EXPIRED' }), expired: true },
    { label: 'pending 다른 code', on: 'pending', respond: () => res(401, { code: 'UNAUTHORIZED' }), expired: false, other: MSG.pendingFailed },
    { label: 'pending 빈 본문', on: 'pending', respond: () => res(401), expired: false, other: MSG.pendingFailed },
    { label: 'pending 비 JSON', on: 'pending', respond: () => text(401, 'expired'), expired: false, other: MSG.pendingFailed },
    { label: 'complete 다른 code', on: 'complete', respond: () => res(401, { code: 'UNAUTHORIZED' }), expired: false, other: MSG.completeFailed },
    { label: 'complete 빈 본문', on: 'complete', respond: () => res(401), expired: false, other: MSG.completeFailed },
    { label: 'complete 비 JSON', on: 'complete', respond: () => text(401, 'expired'), expired: false, other: MSG.completeFailed },
  ]
  test.each(SC24)('SC-24 (error) 서버가 PENDING_SIGNUP_EXPIRED 로 답하면 같은 만료 상태가 된다 — $label', async (row) => {
    // Given 표의 요청에 표의 응답
    stubServer({ [row.on === 'pending' ? PENDING_KEY : COMPLETE]: row.respond })
    await arriveAtRole('pend-1')

    // When 응답을 받는다
    if (row.on === 'complete') {
      await roleReady()
      await userEvent.click(roleRow('관객'))
      await click('계속하기')
    }

    // Then
    if (row.expired) {
      expect(await screen.findByRole('heading', { name: MSG.expiredTitle })).toBeInTheDocument()
      expect(screen.getByText(MSG.expiredBody)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '다시 로그인하기' })).toBeInTheDocument()
      expect(screen.queryAllByRole('radio')).toHaveLength(0)
    } else {
      expect(await screen.findByText(row.other!)).toBeInTheDocument()
      expect(screen.queryByText(MSG.expiredTitle)).not.toBeInTheDocument()
    }
  })

  // ---------- SC-25 ----------
  test.each([
    { provider: 'KAKAO', role: '관객' as const, code: 'AUDIENCE', shown: '카카오 · 관객' },
    { provider: 'NAVER', role: '주최자' as const, code: 'ORGANIZER', shown: '네이버 · 주최자' },
    { provider: 'GOOGLE', role: '관객' as const, code: 'AUDIENCE', shown: '구글 · 관객' },
    { provider: 'APPLE', role: '주최자' as const, code: 'ORGANIZER', shown: '애플 · 주최자' },
  ])('SC-25 (happy) 역할을 고르고 계속하면 가입 완료 화면으로 간다 — $provider $role', async (row) => {
    // Given 대기 정보 provider, complete 200(tok-5, 그 역할, aria0042)
    stubServer({
      [PENDING_KEY]: () => res(200, pendingBody({ provider: row.provider })),
      [COMPLETE]: () => res(200, loginBody('tok-5', { ...USER, role: row.code, handle: 'aria0042' })),
    })
    await arriveAtRole('pend-1')
    await roleReady()

    // 고르지 않고 누르면 오류, 요청 없음
    await click('계속하기')
    expect(await screen.findByText(MSG.roleRequired)).toBeInTheDocument()
    expect(callsTo(COMPLETE)).toHaveLength(0)

    // When 표의 역할을 골라 계속하기
    await userEvent.click(roleRow(row.role))
    await click('계속하기')

    // Then 본문
    await waitFor(() => expect(callsTo(COMPLETE)).toHaveLength(1))
    expect(bodyOf(callsTo(COMPLETE)[0])).toEqual({ pendingToken: 'pend-1', role: row.code })
    // And 토큰·경로
    await waitFor(() => expect(where()).toBe('/joined'))
    expect(token()).toBe('tok-5')
    // And 윗줄·아이디
    expect(screen.getByText(row.shown)).toBeInTheDocument()
    expect(screen.getByText('aria0042')).toBeInTheDocument()
  })

  // ---------- SC-26 ----------
  const handleRows = [undefined, null, '', '   ', 42].map((h) => ({
    label: `200 handle ${h === undefined ? '없음' : JSON.stringify(h)}`,
    respond: () => {
      const user: Record<string, unknown> = { ...USER }
      if (h === undefined) delete user.handle
      else user.handle = h
      return res(200, loginBody('tok-5', user))
    },
    message: MSG.completeFailed,
    link: false,
  }))
  const SC26: { label: string; respond: Responder; message: string; link: boolean }[] = [
    { label: '409 이메일', respond: () => res(409, { code: 'EMAIL_ALREADY_EXISTS' }), message: MSG.alreadyJoined, link: true },
    { label: '409 소셜 연결', respond: () => res(409, { code: 'SOCIAL_ACCOUNT_ALREADY_LINKED' }), message: MSG.alreadyJoined, link: true },
    { label: '409 다른 code', respond: () => res(409, { code: 'OTHER' }), message: MSG.completeFailed, link: false },
    { label: '401 WRITE_KEY_INVALID(옛 쓰기 가드 코드)', respond: () => res(401, { code: 'WRITE_KEY_INVALID' }), message: MSG.completeFailed, link: false },
    { label: '500 빈 본문', respond: () => res(500), message: MSG.completeFailed, link: false },
    { label: '502 비 JSON', respond: () => text(502, '<html>'), message: MSG.completeFailed, link: false },
    { label: '네트워크 오류', respond: networkError, message: MSG.completeFailed, link: false },
    { label: '200 빈 본문', respond: () => res(200), message: MSG.completeFailed, link: false },
    ...handleRows,
  ]
  test.each(SC26)('SC-26 (error) 가입 완료 요청이 실패하면 문구를 보이고, 다시 누르면 사라진다 — $label', async (row) => {
    // Given 관객을 골랐고 첫 요청은 표의 응답, 다시 누른 요청은 붙들어 둔다
    const held = deferred()
    let n = 0
    stubServer({ [COMPLETE]: (init) => (++n === 1 ? row.respond(init) : held.promise) })
    await arriveAtRole('pend-1')
    await roleReady()
    await userEvent.click(roleRow('관객'))

    // When
    await click('계속하기')

    // Then 표의 문구, 경로·토큰
    const message = await screen.findByText(row.message)
    expect(screen.queryByText(MSG.oldWriteRejected)).not.toBeInTheDocument()
    expect(where()).toBe('/oauth2/role')
    expect(token()).toBeNull()
    const links = within(message.parentElement!).queryAllByRole('link', { name: '로그인하기' })
    if (row.link) {
      expect(links).toHaveLength(1)
      expect(links[0]).toHaveAttribute('href', '/login')
    } else {
      expect(links).toHaveLength(0)
    }
    // And 고른 역할 유지, 다시 누르면 응답 전에 문구가 사라진다
    expect(roleRow('관객')).toBeChecked()
    await click('계속하기')
    await waitFor(() => expect(callsTo(COMPLETE)).toHaveLength(2))
    expect(screen.queryByText(row.message)).not.toBeInTheDocument()
  })

  // ---------- SC-27 ----------
  type Sc27 =
    | 'pending-leave-ok'
    | 'pending-leave-500'
    | 'strict'
    | 'shown-leave-5min'
    | 'complete-leave-ok'
    | 'complete-leave-500'
    | 'inflight-expiry-ok'
    | 'inflight-expiry-401'
    | 'inflight-expiry-network'
    | 'complete-login-other'
    | 'complete-logout'
  const SC27: Sc27[] = [
    'pending-leave-ok',
    'pending-leave-500',
    'strict',
    'shown-leave-5min',
    'complete-leave-ok',
    'complete-leave-500',
    'inflight-expiry-ok',
    'inflight-expiry-401',
    'inflight-expiry-network',
    'complete-login-other',
    'complete-logout',
  ]
  test.each(SC27)('SC-27 (boundary) 역할 선택의 늦은 응답 — 떠난 뒤엔 쓰지 않고, 가는 중엔 만료를 서버에 맡긴다 — %s', async (kind) => {
    // Given 가짜 타이머, expiresAt 은 지금+5분
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval', 'Date'] })
    vi.setSystemTime(NOW)
    const flush = async () => {
      for (let i = 0; i < 5; i++) await act(() => vi.advanceTimersByTimeAsync(0))
    }
    const expiresAt = '2026-11-11T08:40:00Z'
    const pendingHeld = deferred()
    const completeHeld = deferred()
    let pendingCount = 0
    stubServer({
      [PENDING_KEY]: () => {
        pendingCount++
        return kind.startsWith('pending-') ? pendingHeld.promise : res(200, pendingBody({ expiresAt }))
      },
      [COMPLETE]: () => completeHeld.promise,
    })
    // When / Then
    if (kind === 'strict') {
      await arriveAtRole('pend-1', { strict: true })
      await flush()
      expect(pendingCount).toBeGreaterThanOrEqual(1)
      expect(screen.getAllByRole('radio', { name: /^관객/ })).toHaveLength(1)
      expect(screen.getAllByRole('button', { name: '계속하기' })).toHaveLength(1)
      return
    }

    await arriveAtRole('pend-1', { navTo: '/' })
    if (kind.startsWith('pending-')) {
      outside('테스트용 이동')
      if (kind === 'pending-leave-ok') pendingHeld.resolve(res(200, pendingBody({ expiresAt })))
      else pendingHeld.resolve(res(500))
      await flush()
      await act(() => vi.advanceTimersByTimeAsync(300_000))
      expect(where()).toBe('/')
      expect(screen.queryByText(MSG.pendingFailed)).not.toBeInTheDocument()
      return
    }

    await flush()
    expect(screen.getByRole('button', { name: '계속하기' })).toBeInTheDocument()
    if (kind === 'shown-leave-5min') {
      outside('테스트용 이동')
      await act(() => vi.advanceTimersByTimeAsync(300_000))
      expect(where()).toBe('/')
      return
    }

    fireEvent.click(roleRow('관객'))
    if (kind.startsWith('inflight-')) await act(() => vi.advanceTimersByTimeAsync(299_000))
    fireEvent.click(screen.getByRole('button', { name: '계속하기' }))
    await flush()
    expect(callsTo(COMPLETE)).toHaveLength(1)

    if (kind === 'complete-leave-ok' || kind === 'complete-leave-500') {
      outside('테스트용 이동')
      completeHeld.resolve(kind === 'complete-leave-ok' ? res(200, loginBody('tok-5')) : res(500))
      await flush()
      expect(token()).toBeNull()
      expect(where()).toBe('/')
      expect(screen.queryByText(MSG.completeFailed)).not.toBeInTheDocument()
      return
    }
    if (kind === 'complete-login-other' || kind === 'complete-logout') {
      outside(kind === 'complete-login-other' ? '테스트용 로그인' : '테스트용 로그아웃')
      completeHeld.resolve(res(200, loginBody('tok-5')))
      await flush()
      expect(token()).toBe('tok-5')
      expect(where()).toBe('/joined')
      return
    }
    // inflight: 응답 전에 5분이 지난다
    await act(() => vi.advanceTimersByTimeAsync(2_000))
    expect(screen.queryByText(MSG.expiredTitle)).not.toBeInTheDocument()
    if (kind === 'inflight-expiry-ok') completeHeld.resolve(res(200, loginBody('tok-5')))
    if (kind === 'inflight-expiry-401') completeHeld.resolve(res(401, { code: 'PENDING_SIGNUP_EXPIRED' }))
    if (kind === 'inflight-expiry-network') completeHeld.reject(new TypeError('Failed to fetch'))
    await flush()
    if (kind === 'inflight-expiry-ok') {
      expect(where()).toBe('/joined')
    } else {
      expect(screen.getByRole('heading', { name: MSG.expiredTitle })).toBeInTheDocument()
    }
  })

  // ---------- SC-28 ~ SC-31 공용 ----------
  // state 를 넘기지 않았을 때만 기본값 — undefined 를 넘기면 그대로 '없음' 이다
  async function arriveAtJoined(...args: [state?: unknown, me?: Responder]) {
    const state = args.length >= 1 ? args[0] : { via: 'email' }
    const me = args[1] ?? (() => res(200, USER))
    localStorage.setItem('accessToken', 'tok-1')
    stubServer({ [ME]: me })
    renderAt({ pathname: '/joined', state })
  }
  function stubClipboard(writeText: ((t: string) => Promise<void>) | null) {
    Object.defineProperty(navigator, 'clipboard', {
      value: writeText ? { writeText: vi.fn(writeText) } : undefined,
      configurable: true,
    })
    return (navigator as unknown as { clipboard?: { writeText: ReturnType<typeof vi.fn> } }).clipboard
  }

  // ---------- SC-28 ----------
  test('SC-28 (happy) 가입 완료 화면에서 아이디를 복사하면 토스트가 나온다', async () => {
    // Given wave7302 로 로그인, /joined, 클립보드 성공
    const clip = stubClipboard(async () => {})
    await arriveAtJoined()
    await screen.findByText(HANDLE)
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })

    // When
    fireEvent.click(screen.getByRole('button', { name: '아이디 복사' }))
    await act(() => vi.advanceTimersByTimeAsync(0))

    // Then writeText 가 아이디로 한 번
    expect(clip!.writeText).toHaveBeenCalledTimes(1)
    expect(clip!.writeText).toHaveBeenCalledWith(HANDLE)
    // And 토스트
    expect(screen.getByRole('status')).toHaveTextContent(MSG.copied)
    // And 2,500ms 뒤 사라진다
    await act(() => vi.advanceTimersByTimeAsync(2_500))
    expect(screen.queryByText(MSG.copied)).not.toBeInTheDocument()
  })

  // ---------- SC-29 ----------
  test.each([
    { label: '거부', clip: () => stubClipboard(() => Promise.reject(new DOMException('no', 'NotAllowedError'))) },
    {
      label: '늦은 거부',
      clip: () =>
        stubClipboard(
          () => new Promise((_, reject) => setTimeout(() => reject(new DOMException('no', 'NotAllowedError')), 100)),
        ),
    },
    { label: '클립보드 없음', clip: () => stubClipboard(null) },
    { label: '떠난 뒤 거부', clip: () => stubClipboard(() => new Promise((_, reject) => setTimeout(() => reject(new Error('late')), 100))), leave: true },
    { label: '떠난 뒤 성공', clip: () => stubClipboard(() => new Promise((ok) => setTimeout(() => ok(), 100))), leave: true },
  ])('SC-29 (error) 복사하지 못하면 아이디를 선택해 두고 길게 눌러 복사하라고 안내한다 — $label', async (row) => {
    // Given /joined, 클립보드는 표
    row.clip()
    await arriveAtJoined()
    await screen.findByText(HANDLE)

    // When
    fireEvent.click(screen.getByRole('button', { name: '아이디 복사' }))
    if ('leave' in row && row.leave) {
      fireEvent.click(screen.getByRole('button', { name: '시작하기' }))
      await act(async () => {
        await new Promise((r) => setTimeout(r, 150))
      })
      expect(where()).toBe('/')
      expect(screen.queryByText(MSG.copyFallback)).not.toBeInTheDocument()
      return
    }
    await act(async () => {
      await new Promise((r) => setTimeout(r, 150))
    })

    // Then 안내, 토스트 없음
    expect(screen.getByText(MSG.copyFallback)).toBeInTheDocument()
    expect(screen.queryByText(MSG.copied)).not.toBeInTheDocument()
    // And 선택 영역이 아이디
    expect(window.getSelection()?.toString()).toBe(HANDLE)
  })

  // ---------- SC-30 ----------
  test('SC-30 (happy) 시작하기를 누르면 홈으로 간다', async () => {
    // Given
    await arriveAtJoined()
    await screen.findByText(HANDLE)

    // When
    await click('시작하기')

    // Then
    expect(where()).toBe('/')
    expect(token()).toBe('tok-1')
  })

  // ---------- SC-31 ----------
  const meWith = (over: Record<string, unknown>, drop?: string) => () => {
    const user: Record<string, unknown> = { ...USER, ...over }
    if (drop) delete user[drop]
    return res(200, user)
  }
  const SC31: { label: string; state: unknown; me?: Responder; loggedOut?: boolean; restoring?: boolean; top?: string; to?: string }[] = [
    { label: 'email', state: { via: 'email' }, top: '이메일 · 관객' },
    { label: 'email 주최자', state: { via: 'email' }, me: meWith({ role: 'ORGANIZER' }), top: '이메일 · 주최자' },
    ...PROVIDERS.map((p) => ({ label: p.upper, state: { via: p.upper }, top: `${p.name} · 관객` })),
    { label: 'state 없음', state: undefined, top: '관객' },
    { label: 'via null', state: { via: null }, top: '관객' },
    { label: 'via LINE', state: { via: 'LINE' }, top: '관객' },
    { label: 'via kakao', state: { via: 'kakao' }, top: '관객' },
    { label: 'via 1', state: { via: 1 }, top: '관객' },
    { label: '비로그인', state: { via: 'email' }, loggedOut: true, to: '/login' },
    { label: '복원 중', state: { via: 'email' }, restoring: true, top: '이메일 · 관객' },
    ...[undefined, null, '', '   ', 7302].map((h) => ({
      label: `복원 handle ${h === undefined ? '없음' : JSON.stringify(h)}`,
      state: { via: 'email' },
      me: h === undefined ? meWith({}, 'handle') : meWith({ handle: h }),
      to: '/login',
    })),
  ]
  test.each(SC31)('SC-31 (boundary) 가입 완료 화면의 윗줄과 접근 조건 — $label', async (row) => {
    if (row.loggedOut) {
      // Given 비로그인
      stubServer()
      renderAt({ pathname: '/joined', state: row.state })
      // Then /login
      await waitFor(() => expect(where()).toBe('/login'))
      return
    }
    if (row.restoring) {
      // Given 복원 중 — 응답 전에는 아이디도 이동도 없다
      const held = deferred()
      await arriveAtJoined(row.state, () => held.promise)
      await settle()
      expect(screen.queryByText(HANDLE)).not.toBeInTheDocument()
      expect(where()).toBe('/joined')
      held.resolve(res(200, USER))
      expect(await screen.findByText(HANDLE)).toBeInTheDocument()
      expect(screen.getByText(row.top!)).toBeInTheDocument()
      return
    }
    // Given 로그인(복원 응답은 표) · state 는 표
    await arriveAtJoined(row.state, row.me)

    // Then
    if (row.to) {
      await waitFor(() => expect(where()).toBe(row.to))
      return
    }
    expect(await screen.findByText(HANDLE)).toBeInTheDocument()
    expect(screen.getByText(row.top!, { exact: true })).toBeInTheDocument()
  })

  // ---------- SC-32 ----------
  test.each([
    { path: '/', header: true },
    { path: '/performances/1', header: true },
    { path: '/login', header: false },
    { path: '/signup', header: false },
    { path: '/oauth2/redirect', header: false },
    { path: '/oauth2/role', header: false },
    { path: '/joined', header: false },
  ])('SC-32 (regression) 전역 헤더는 인증 화면에서만 사라진다 — $path', async ({ path, header }) => {
    // Given 로그인하지 않았다
    stubServer()

    // When
    renderAt(path)

    // Then
    if (header) {
      expect(await screen.findByRole('link', { name: '가입' })).toBeInTheDocument()
      expect(screen.getAllByRole('link', { name: '로그인' }).length).toBeGreaterThan(0)
    } else {
      expect(screen.queryByRole('link', { name: '가입' })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: '로그아웃' })).not.toBeInTheDocument()
    }
  })

  // ---------- SC-33 ----------
  test.each([
    { label: '안내만', help: MSG.pwHelp, error: undefined, kind: 'help' as const },
    { label: '같은 문구 오류', help: MSG.pwHelp, error: MSG.pwHelp, kind: 'error' as const },
    { label: '다른 문구 오류', help: MSG.pwHelp, error: MSG.passwordRequired, kind: 'error' as const },
    { label: '빈 안내', help: '', error: undefined, kind: 'none' as const },
    { label: '공백 안내', help: '   ', error: undefined, kind: 'none' as const },
    { label: '긴 안내', help: '가'.repeat(300), error: undefined, kind: 'help' as const },
  ])('SC-33 (boundary) 공용 Field 의 안내 자리는 오류가 나면 같은 자리가 오류로 바뀐다 — $label', ({ help, error, kind }) => {
    // Given / When
    render(
      <ThemeProvider theme={theme}>
        <Field label="비밀번호" help={help} error={error} />
      </ThemeProvider>,
    )
    const field = screen.getByLabelText('비밀번호')
    const described = (field.getAttribute('aria-describedby') ?? '').split(/\s+/).filter(Boolean)

    // Then
    if (kind === 'help') {
      expect(described).toHaveLength(1)
      expect(document.getElementById(described[0])?.textContent).toBe(help)
      expect(field).not.toHaveAttribute('aria-invalid', 'true')
    } else if (kind === 'error') {
      expect(field).toHaveAttribute('aria-invalid', 'true')
      expect(screen.getAllByText(error!)).toHaveLength(1)
      expect(document.querySelectorAll('[data-field-error]')).toHaveLength(1)
      if (error !== help) expect(screen.queryByText(help)).not.toBeInTheDocument()
      expect(described).toHaveLength(1)
    } else {
      expect(field).not.toHaveAttribute('aria-describedby')
    }
  })

  // ---------- SC-34 ----------
  test.todo('SC-34 (happy) 다섯 화면과 오류 상태 3종을 375px 로 시안과 나란히 비교한다 — Phase 4 화면 확인')

  // ---------- SC-35 ----------
  type Flow = 'F1' | 'F3' | 'F7' | 'F8'
  const variants: { label: string; mutate: (b: Record<string, unknown>) => void; flows: Flow[] }[] = []
  const setUser = (k: string, v: unknown) => (b: Record<string, unknown>) => {
    const u = { ...(b.user as Record<string, unknown>) }
    if (v === undefined) delete u[k]
    else u[k] = v
    b.user = u
  }
  for (const v of [undefined, null, 42, '', '   ']) {
    variants.push({
      label: `accessToken ${v === undefined ? '없음' : JSON.stringify(v)}`,
      mutate: (b) => {
        if (v === undefined) delete b.accessToken
        else b.accessToken = v
      },
      flows: ['F1', 'F3', 'F7'],
    })
  }
  for (const v of [undefined, null]) {
    variants.push({
      label: `user ${v === undefined ? '없음' : 'null'}`,
      mutate: (b) => {
        if (v === undefined) delete b.user
        else b.user = v
      },
      flows: ['F1', 'F3', 'F7', 'F8'],
    })
  }
  const userVariants: [string, unknown[]][] = [
    ['name', [undefined, null, 7, '', '   ']],
    ['email', [undefined, null, 7]],
    ['role', [undefined, null, 1, 'ADMIN', 'audience']],
    ['handle', [undefined, null, 7302, '', '   ']],
  ]
  for (const [k, vs] of userVariants) {
    for (const v of vs) {
      variants.push({
        label: `user.${k} ${v === undefined ? '없음' : JSON.stringify(v)}`,
        mutate: setUser(k, v),
        flows: ['F1', 'F3', 'F7', 'F8'],
      })
    }
  }
  const okVariants = [
    { label: '짝: role AUDIENCE', mutate: setUser('role', 'AUDIENCE') },
    { label: '짝: role ORGANIZER', mutate: setUser('role', 'ORGANIZER') },
    { label: '짝: email ""', mutate: setUser('email', '') },
  ]
  const SC35 = [
    ...variants.flatMap((v) => v.flows.map((flow) => ({ label: `${flow} · ${v.label}`, flow, mutate: v.mutate, ok: false }))),
    ...okVariants.flatMap((v) =>
      (['F1', 'F3', 'F7', 'F8'] as Flow[]).map((flow) => ({ label: `${flow} · ${v.label}`, flow, mutate: v.mutate, ok: true })),
    ),
  ]
  test.each(SC35)('SC-35 (error) 로그인·가입 완료·복원 응답의 본문이 규칙에 맞지 않으면 성공으로 보지 않는다 — $label', async (row) => {
    // Given 정상 본문에서 표의 필드만 바꾼다
    const body = loginBody('tok-new') as Record<string, unknown>
    row.mutate(body)
    const respond = () => res(200, body)

    // When 그 흐름의 동작 / Then 흐름의 결과
    if (row.flow === 'F1') {
      stubServer({ [LOGIN]: respond })
      renderAt('/login')
      fillLogin()
      await click('로그인하기')
      if (row.ok) {
        await waitFor(() => expect(where()).toBe('/'))
        expect(token()).toBe('tok-new')
      } else {
        expect(await screen.findByText(MSG.loginFailed)).toBeInTheDocument()
        expect(where()).toBe('/login')
        expect(token()).toBeNull()
      }
    } else if (row.flow === 'F3') {
      stubServer({ [LOGIN]: respond })
      renderAt('/signup')
      fillSignUp()
      await click('가입하기')
      if (row.ok) {
        await waitFor(() => expect(where()).toBe('/joined'))
        expect(token()).toBe('tok-new')
      } else {
        await waitFor(() => expect(where()).toBe('/login'))
        expect(screen.getByText(MSG.joinedThenLogin)).toBeInTheDocument()
        expect(token()).toBeNull()
      }
    } else if (row.flow === 'F7') {
      stubServer({ [COMPLETE]: respond })
      await arriveAtRole('pend-1')
      await roleReady()
      await userEvent.click(roleRow('관객'))
      await click('계속하기')
      if (row.ok) {
        await waitFor(() => expect(where()).toBe('/joined'))
        expect(token()).toBe('tok-new')
      } else {
        expect(await screen.findByText(MSG.completeFailed)).toBeInTheDocument()
        expect(where()).toBe('/oauth2/role')
        expect(token()).toBeNull()
      }
    } else {
      // F8 — /api/auth/me 본문이 곧 user
      localStorage.setItem('accessToken', 'tok-1')
      stubServer({ [ME]: () => res(200, body.user) })
      renderAt({ pathname: '/joined', state: { via: 'email' } })
      if (row.ok) {
        expect(await screen.findByText(HANDLE)).toBeInTheDocument()
      } else {
        await waitFor(() => expect(where()).toBe('/login'))
      }
    }
  })
})

// Phase 4 롤백 (2026-11-09) — VERIFY_TASK-018 RC-2·RC-3 의 변종을 시나리오로 옮긴 것. SC-36 ~ SC-39.
describe('TASK-018 Phase 4 롤백 — 경로 표기·시안 값·만료 구성', () => {
  // 토큰 색을 jsdom 이 계산한 값으로 — 테스트에 색 리터럴을 두지 않는다 (DESIGN-002)
  function colorOf(name: TokenName): string {
    const probe = document.createElement('span')
    probe.style.color = designToken(name)
    document.body.appendChild(probe)
    const color = getComputedStyle(probe).color
    probe.remove()
    return color
  }
  // 시안 .title 자간 -0.03em — jsdom 은 em 을 px 로 계산한다 (28px × -0.03)
  const TITLE_TRACKING_PX = '-0.84px'

  // ---------- SC-36 ----------
  type Screen36 = 'login' | 'signup' | 'joined' | 'role' | 'callback' | 'list' | 'register' | 'none'
  const SC36: { path: string; screen: Screen36; loggedIn?: boolean; organizer?: boolean; state?: unknown; header: boolean }[] = [
    { path: '/login/', screen: 'login', header: false },
    { path: '/Login', screen: 'login', header: false },
    { path: '/LOGIN', screen: 'login', header: false },
    { path: '/%6Cogin', screen: 'login', header: false },
    { path: '/signup/', screen: 'signup', header: false },
    { path: '/Signup', screen: 'signup', header: false },
    { path: '/%73ignup', screen: 'signup', header: false },
    { path: '/joined/', screen: 'joined', loggedIn: true, header: false },
    { path: '/Joined', screen: 'joined', loggedIn: true, header: false },
    { path: '/oauth2/role/', screen: 'role', state: { pendingToken: 'pend-1' }, header: false },
    { path: '/oauth2/redirect/?token=tok-1', screen: 'callback', header: false },
    { path: '/oauth2/Redirect?token=tok-1', screen: 'callback', header: false },
    { path: '/', screen: 'list', header: true },
    // 등록 화면은 주최자만 그린다 (TASK-024 가드) — 주최자로 로그인해 같은 단언을 지킨다
    { path: '/performances/new', screen: 'register', loggedIn: true, organizer: true, header: true },
    { path: '/%', screen: 'none', header: true },
    { path: '/%E0%A4%A', screen: 'none', header: true },
    { path: '/', screen: 'list', loggedIn: true, header: true },
  ]
  test.each(SC36)(
    'SC-36 (boundary) 라우터가 인증 화면으로 그리는 주소면 표기가 달라도 전역 헤더가 없다 — $path (로그인 $loggedIn)',
    async (row) => {
      // Given 표의 로그인 상태 (콜백의 /me 는 붙들어 화면에 머문다)
      if (row.loggedIn) localStorage.setItem('accessToken', 'tok-1')
      stubServer(
        row.screen === 'callback'
          ? { [ME]: () => deferred().promise }
          : row.organizer
            ? { [ME]: () => res(200, { ...USER, role: 'ORGANIZER' }) }
            : {},
      )

      // When 표의 주소를 연다
      const [pathname, search = ''] = row.path.split('?')
      renderAt(row.state ? { pathname, state: row.state } : search ? `${pathname}?${search}` : pathname)

      // Then 표의 화면
      if (row.screen === 'login') expect(await screen.findByRole('heading', { name: '쓰던 계정으로 바로 들어와요' })).toBeInTheDocument()
      if (row.screen === 'signup') expect(await screen.findByRole('heading', { name: '이메일로 가입' })).toBeInTheDocument()
      if (row.screen === 'joined') expect(await screen.findByText(HANDLE)).toBeInTheDocument()
      if (row.screen === 'role') expect(await screen.findByRole('button', { name: '계속하기' })).toBeInTheDocument()
      if (row.screen === 'callback') expect(await screen.findByText('소셜 계정을 확인하고 있어요')).toBeInTheDocument()
      if (row.screen === 'register') expect(await screen.findByLabelText(/^공연명/)).toBeInTheDocument()
      await settle()
      // And 전역 헤더
      const headerShown = row.loggedIn
        ? screen.queryByRole('button', { name: '로그아웃' }) !== null
        : screen.queryByRole('link', { name: '가입' }) !== null
      expect(headerShown).toBe(row.header)
    },
  )

  test('SC-36 (boundary) 표기가 다른 로그인 주소에서도 소셜 실패 안내가 나온다 — /Login?error=social', () => {
    stubServer()
    renderAt('/Login?error=social')
    expect(screen.getByText(MSG.socialFailed)).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: '가입' })).not.toBeInTheDocument()
  })

  // ---------- SC-37 ----------
  test.each([
    { from: '/login/', to: '/' },
    { from: '/Login', to: '/' },
    { from: '/LOGIN?x=1', to: '/' },
    { from: '/login/#a', to: '/' },
    { from: '/%6Cogin', to: '/' },
    { from: '/signup/', to: '/' },
    { from: '/Signup', to: '/' },
    { from: '/%73ignup', to: '/' },
    { from: '/performances/%37', to: '/performances/%37' },
  ])('SC-37 (error) 로그인 뒤 이동할 곳이 로그인·가입 화면이면 표기가 달라도 홈으로 간다 — $from', async ({ from, to }) => {
    // Given 서버 200, state.from 은 표의 값
    stubServer({ 'GET /api/performances/7': () => res(200, { ...performance, id: 7 }) })
    renderAt({ pathname: '/login', state: { from } })

    // When
    fillLogin()
    await click('로그인하기')

    // Then
    await waitFor(() => expect(token()).toBe('tok-1'))
    await waitFor(() => expect(where()).toBe(to))
  })

  // ---------- SC-38 ----------
  test.each([
    { label: '/signup 앱바', open: () => renderAt('/signup') },
    { label: '역할 선택 앱바', open: () => renderAt({ pathname: '/oauth2/role', state: { pendingToken: 'pend-1' } }), wait: '계속하기' },
  ])('SC-38 (happy) 인증 화면의 모양 값은 시안과 같다 — $label 바탕이 흰 화면과 같다', async ({ open, wait }) => {
    stubServer()
    open()
    if (wait) await screen.findByRole('button', { name: wait })
    const bar = screen.getByRole('button', { name: '뒤로' }).closest('header')!
    const page = screen.getByRole('main')
    expect(getComputedStyle(page).backgroundColor).toBe(colorOf('--c-surface'))
    expect(getComputedStyle(bar).backgroundColor).toBe(getComputedStyle(page).backgroundColor)
  })

  test.each([
    { label: '/login 제목', path: '/login', name: '쓰던 계정으로 바로 들어와요' },
    { label: '/signup 제목', path: '/signup', name: '이메일로 가입' },
    { label: '콜백 제목', path: '/oauth2/redirect?token=tok-1', name: '소셜 계정을 확인하고 있어요' },
  ])('SC-38 (happy) 인증 화면의 모양 값은 시안과 같다 — $label 자간', async ({ path, name }) => {
    stubServer({ [ME]: () => deferred().promise })
    renderAt(path)
    expect(getComputedStyle(await screen.findByRole('heading', { name })).letterSpacing).toBe(TITLE_TRACKING_PX)
  })

  test('SC-38 (happy) 인증 화면의 모양 값은 시안과 같다 — 역할 행 일러스트 36px', async () => {
    stubServer()
    await arriveAtRole('pend-1')
    const img = (await screen.findByRole('radio', { name: /^관객/ })).querySelector('img')!
    expect(getComputedStyle(img).width).toBe('36px')
    expect(getComputedStyle(img).height).toBe('36px')
  })

  test.each(['만료', '역할 미선택'])('SC-38 (happy) 인증 화면의 모양 값은 시안과 같다 — %s 오류 아이콘 18px', async (state) => {
    stubServer(state === '만료' ? { [PENDING_KEY]: () => res(401, { code: 'PENDING_SIGNUP_EXPIRED' }) } : {})
    await arriveAtRole('pend-1')
    if (state === '역할 미선택') {
      await roleReady()
      await click('계속하기')
    }
    const message = state === '만료' ? await screen.findByRole('alert') : (await screen.findByText(MSG.roleRequired)).closest('p')!
    const icon = message.querySelector('svg')!
    expect(icon.getAttribute('width')).toBe('18px')
    expect(icon.getAttribute('height')).toBe('18px')
  })

  test("SC-38 (happy) 인증 화면의 모양 값은 시안과 같다 — '이메일로 가입' 화살표 18px · 간격 2px", () => {
    stubServer()
    renderAt('/login')
    const link = screen.getByRole('link', { name: '이메일로 가입' })
    expect(link.querySelector('svg')!.getAttribute('width')).toBe('18px')
    expect(getComputedStyle(link).gap).toBe('2px')
  })

  test('SC-38 (happy) 인증 화면의 모양 값은 시안과 같다 — 가입 완료 아이디 줄 높이·자간', async () => {
    localStorage.setItem('accessToken', 'tok-1')
    stubServer()
    renderAt({ pathname: '/joined', state: { via: 'email' } })
    const id = await screen.findByText(HANDLE)
    expect(getComputedStyle(id).lineHeight).toBe('1.15')
    expect(getComputedStyle(id).letterSpacing).not.toBe('')
    expect(getComputedStyle(id).letterSpacing).not.toBe('normal')
  })

  test('SC-38 (happy) 인증 화면의 모양 값은 시안과 같다 — 콜백 진행 표시 32px·테두리 3px·트랙·호', async () => {
    stubServer({ [ME]: () => deferred().promise })
    renderAt('/oauth2/redirect?token=tok-1')
    const spin = await screen.findByRole('progressbar')
    const style = getComputedStyle(spin)
    expect(style.width).toBe('32px')
    expect(style.height).toBe('32px')
    expect(style.borderTopWidth).toBe('3px')
    expect(style.borderRightColor).toBe(colorOf('--c-line'))
    expect(style.borderTopColor).toBe(colorOf('--c-brand'))
  })

  // ---------- SC-39 ----------
  test.each(['timer', 'complete-401', 'pending-401'] as const)(
    'SC-39 (boundary) 화면에 머물다 만료되면 어느 계정이었는지 남긴다 — %s',
    async (path) => {
      // Given 대기 정보 응답
      const pending = { provider: 'KAKAO', email: 'harin.kim@kakao.com', name: '김하린', expiresAt: '2026-11-11T08:40:00Z' }
      if (path === 'timer') {
        vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval', 'Date'] })
        vi.setSystemTime(new Date('2026-11-11T08:35:00.000Z'))
      }
      stubServer({
        [PENDING_KEY]: () => (path === 'pending-401' ? res(401, { code: 'PENDING_SIGNUP_EXPIRED' }) : res(200, path === 'timer' ? pending : { ...pending, expiresAt: new Date(Date.now() + 300_000).toISOString() })),
        [COMPLETE]: () => res(401, { code: 'PENDING_SIGNUP_EXPIRED' }),
      })

      // When 표의 경로로 만료
      await arriveAtRole('pend-1')
      if (path === 'timer') {
        for (let i = 0; i < 5; i++) await act(() => vi.advanceTimersByTimeAsync(0))
        await act(() => vi.advanceTimersByTimeAsync(300_000))
      } else if (path === 'complete-401') {
        await roleReady()
        await userEvent.click(roleRow('관객'))
        await click('계속하기')
      }

      // Then 제목·라벨·아이콘 붙은 오류 안내
      const heading = path === 'timer' ? screen.getByRole('heading', { name: MSG.expiredTitle }) : await screen.findByRole('heading', { name: MSG.expiredTitle })
      expect(heading).toBeInTheDocument()
      expect(screen.getByText('역할')).toBeInTheDocument()
      const alert = screen.getByRole('alert')
      expect(alert).toHaveTextContent(MSG.expiredBody)
      expect(alert.querySelector('svg')).not.toBeNull()
      // And 윗줄·이메일(이름)
      if (path === 'pending-401') {
        expect(screen.queryByText('카카오로 처음 가입')).not.toBeInTheDocument()
        expect(screen.queryByText('harin.kim@kakao.com(김하린)')).not.toBeInTheDocument()
      } else {
        expect(screen.getByText('카카오로 처음 가입')).toBeInTheDocument()
        expect(screen.getByText('harin.kim@kakao.com(김하린)')).toBeInTheDocument()
      }
    },
  )
})
