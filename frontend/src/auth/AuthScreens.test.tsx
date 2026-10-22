import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { StrictMode } from 'react'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { registerPerformance, WRITE_REJECTED_MESSAGE } from '../api/performances'
import App from '../App'
import { theme } from '../theme'
import { AuthProvider } from './AuthContext'

// TASK-010 시나리오 SC-01 ~ SC-32. SoT: workflow_design/05_scenario/SCENARIO_TASK-010.md
// 서버는 두지 않는다 — fetch 응답(상태·본문)만 대체하고 그 위의 코드는 실제로 통과시킨다.
// 표로 묶은 시나리오는 test.each 한 줄(= 시나리오 한 건)이고 행마다 케이스가 하나씩 돈다.

const MSG = {
  mismatch: '비밀번호가 일치하지 않습니다',
  signupFail: '가입하지 못했습니다. 잠시 후 다시 시도해 주세요',
  loginFail: '로그인하지 못했습니다. 잠시 후 다시 시도해 주세요',
  badCredentials: '이메일 또는 비밀번호가 올바르지 않습니다',
  emailRequired: '이메일을 입력해 주세요',
  passwordRequired: '비밀번호를 입력해 주세요',
  confirmRequired: '비밀번호를 한 번 더 입력해 주세요',
  nameRequired: '이름을 입력해 주세요',
  roleRequired: '역할을 선택해 주세요',
  pwMin: '비밀번호는 8자 이상이어야 합니다',
  pwMax: '비밀번호는 72자 이하여야 합니다',
  nameMax: '이름은 100자 이하여야 합니다',
  emailMax: '이메일은 255자 이하여야 합니다',
}
// 링크가 문장 안에 들어가므로 앞부분만 본다.
const DUP = /이미 가입된 이메일입니다\./
const ALL_ERRORS = [...Object.values(MSG), DUP, WRITE_REJECTED_MESSAGE]

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
const LOGIN_RESP = { accessToken: 'tok-1', tokenType: 'Bearer', expiresInSeconds: 86400, user: USER }
const SIGNUP_RESP = { id: 1, email: 'hong@example.com', name: '홍길동', role: 'ORGANIZER' }

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
    'GET /api/performances/3': () => res(200, { ...performance, id: 3 }),
    'POST /api/auth/signup': () => res(201, SIGNUP_RESP),
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

function deferred() {
  let resolve!: (r: Response) => void
  let reject!: (e: unknown) => void
  const promise = new Promise<Response>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

// ---------- 렌더 ----------

function Where() {
  const l = useLocation()
  return <div data-testid="where">{l.pathname + l.search + l.hash}</div>
}
const where = () => screen.getByTestId('where').textContent

function renderAt(entry: string | { pathname: string; state?: unknown }, options: { strict?: boolean } = {}) {
  const tree = (
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[entry]}>
        <AuthProvider>
          <App />
          <Where />
        </AuthProvider>
      </MemoryRouter>
    </ThemeProvider>
  )
  return render(options.strict ? <StrictMode>{tree}</StrictMode> : tree)
}

// ---------- 폼 ----------

const LABEL = {
  email: /^이메일/,
  password: /^비밀번호(?! *확인)/,
  confirm: /^비밀번호 확인/,
  name: /^이름/,
}
function setField(label: RegExp, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
}

interface SignUpValues {
  email: string
  password: string
  confirm: string
  name: string
  role: '관객' | '주최자' | null
}
const SIGNUP_OK: SignUpValues = {
  email: 'hong@example.com',
  password: 'password1!',
  confirm: 'password1!',
  name: '홍길동',
  role: '주최자',
}
function fillSignUp(over: Partial<SignUpValues> = {}): SignUpValues {
  const v = { ...SIGNUP_OK, ...over }
  setField(LABEL.email, v.email)
  setField(LABEL.password, v.password)
  setField(LABEL.confirm, v.confirm)
  setField(LABEL.name, v.name)
  if (v.role) fireEvent.click(screen.getByRole('radio', { name: v.role }))
  return v
}
// 비밀번호 항목을 바꿀 때는 확인란도 같은 값으로 둔다 — 그 위반만 남기려는 것.
const samePw = (pw: string): Partial<SignUpValues> => ({ password: pw, confirm: pw })

interface LoginValues {
  email: string
  password: string
}
const LOGIN_OK: LoginValues = { email: 'hong@example.com', password: 'password1!' }
function fillLogin(over: Partial<LoginValues> = {}): LoginValues {
  const v = { ...LOGIN_OK, ...over }
  setField(LABEL.email, v.email)
  setField(LABEL.password, v.password)
  return v
}

const clickButton = (name: string) => userEvent.click(screen.getByRole('button', { name }))
const token = () => localStorage.getItem('accessToken')
const NUL = '\u0000'
const LONE = '\ud800'
const emailOf = (len: number) => 'a'.repeat(len - '@example.com'.length) + '@example.com'

function expectNoErrorTexts() {
  expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  for (const m of ALL_ERRORS) expect(screen.queryByText(m)).not.toBeInTheDocument()
}

beforeEach(() => {
  localStorage.clear()
})
afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  localStorage.clear()
})

describe('TASK-010 이메일 회원가입/로그인 화면', () => {
  test('SC-01 (happy) 가입 화면의 기본 상태', async () => {
    // Given 로그인하지 않았고 localStorage 에 토큰이 없다
    stubServer()

    // When /signup 을 연다
    renderAt('/signup')

    // Then 입력란·역할 라디오·버튼이 있다
    expect(screen.getByLabelText(LABEL.email)).toBeInTheDocument()
    expect(screen.getByLabelText(LABEL.password)).toBeInTheDocument()
    expect(screen.getByLabelText(LABEL.confirm)).toBeInTheDocument()
    expect(screen.getByLabelText(LABEL.name)).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: '관객' })).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: '주최자' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '가입하기' })).toBeInTheDocument()
    // And 라디오는 선택되지 않았고 오류 문구는 없다
    expect(screen.getByRole('radio', { name: '관객' })).not.toBeChecked()
    expect(screen.getByRole('radio', { name: '주최자' })).not.toBeChecked()
    expectNoErrorTexts()
    // And 이메일 입력란에 포커스가 있다
    expect(screen.getByLabelText(LABEL.email)).toHaveFocus()
  })

  test('SC-02 (happy) 로그인 화면의 기본 상태', async () => {
    // Given
    stubServer()

    // When
    renderAt('/login')

    // Then
    expect(screen.getByLabelText(LABEL.email)).toBeInTheDocument()
    expect(screen.getByLabelText(LABEL.password)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그인' })).toBeInTheDocument()
    expectNoErrorTexts()
    // And
    expect(screen.getByLabelText(LABEL.email)).toHaveFocus()
  })

  test.each([
    { role: '주최자' as const, code: 'ORGANIZER' },
    { role: '관객' as const, code: 'AUDIENCE' },
  ])('SC-03 (happy) 가입에 성공하면 역할($role→$code) 포함해 요청하고 로그인 화면으로 이동한다', async ({ role, code }) => {
    // Given 올바르게 입력했고 서버가 201 로 응답한다
    stubServer()
    renderAt('/signup')
    fillSignUp({ role })

    // When
    await clickButton('가입하기')

    // Then 요청 본문
    await waitFor(() => expect(callsTo('POST /api/auth/signup')).toHaveLength(1))
    expect(bodyOf(callsTo('POST /api/auth/signup')[0])).toEqual({
      email: 'hong@example.com',
      password: 'password1!',
      passwordConfirm: 'password1!',
      name: '홍길동',
      role: code,
    })
    // And /login 으로 이동
    await waitFor(() => expect(where()).toBe('/login'))
    expect(screen.getByRole('button', { name: '로그인' })).toBeInTheDocument()
    // And 가입은 로그인이 아니다
    expect(token()).toBeNull()
  })

  test('SC-04 (error) 비밀번호 확인이 다르면 제출 전에 인라인 오류를 보인다', async () => {
    // Given
    stubServer()
    renderAt('/signup')
    fillSignUp({ password: 'password1!', confirm: 'password2!' })

    // When
    await clickButton('가입하기')

    // Then 확인란 아래 오류와 aria-invalid
    expect(await screen.findByText(MSG.mismatch)).toBeInTheDocument()
    expect(screen.getByLabelText(LABEL.confirm)).toHaveAttribute('aria-invalid', 'true')
    // And 요청 없음, 화면 유지
    expect(authCalls()).toHaveLength(0)
    expect(where()).toBe('/signup')
  })

  // 경계: 통과 행(pass)은 서버로 요청이 간다. 길이는 UTF-16 코드 유닛 수, 이메일은 trim 뒤 길이.
  interface Sc07Row {
    label: string
    screen: '가입' | '로그인'
    signUp?: Partial<SignUpValues>
    login?: Partial<LoginValues>
    field?: RegExp
    message?: string
    pass?: boolean
  }
  const SC07_ROWS: Sc07Row[] = [
    { label: '가입 이메일 빈 값', screen: '가입', signUp: { email: '' }, field: LABEL.email, message: MSG.emailRequired },
    { label: '가입 이메일 공백만', screen: '가입', signUp: { email: '   ' }, field: LABEL.email, message: MSG.emailRequired },
    { label: '가입 비밀번호 빈 값', screen: '가입', signUp: samePw(''), field: LABEL.password, message: MSG.passwordRequired },
    {
      label: '가입 비밀번호 공백만 8자',
      screen: '가입',
      signUp: samePw(' '.repeat(8)),
      field: LABEL.password,
      message: MSG.passwordRequired,
    },
    { label: '가입 비밀번호 7자', screen: '가입', signUp: samePw('abcdefg'), field: LABEL.password, message: MSG.pwMin },
    {
      label: '가입 비밀번호 73자',
      screen: '가입',
      signUp: samePw('a'.repeat(73)),
      field: LABEL.password,
      message: MSG.pwMax,
    },
    { label: '가입 확인 빈 값', screen: '가입', signUp: { confirm: '' }, field: LABEL.confirm, message: MSG.confirmRequired },
    {
      label: '가입 확인 공백만 (불일치로 본다)',
      screen: '가입',
      signUp: { confirm: '   ' },
      field: LABEL.confirm,
      message: MSG.mismatch,
    },
    { label: '가입 이름 빈 값', screen: '가입', signUp: { name: '' }, field: LABEL.name, message: MSG.nameRequired },
    { label: '가입 이름 공백만', screen: '가입', signUp: { name: '   ' }, field: LABEL.name, message: MSG.nameRequired },
    { label: '가입 이름 101자', screen: '가입', signUp: { name: '가'.repeat(101) }, field: LABEL.name, message: MSG.nameMax },
    {
      label: '가입 이름 이모지 51개 (102 UTF-16 유닛)',
      screen: '가입',
      signUp: { name: '😀'.repeat(51) },
      field: LABEL.name,
      message: MSG.nameMax,
    },
    {
      label: '가입 이메일 256자',
      screen: '가입',
      signUp: { email: emailOf(256) },
      field: LABEL.email,
      message: MSG.emailMax,
    },
    {
      label: '가입 이메일 trim 뒤 256자 (앞뒤 공백 포함)',
      screen: '가입',
      signUp: { email: `  ${emailOf(256)}  ` },
      field: LABEL.email,
      message: MSG.emailMax,
    },
    { label: '가입 역할 미선택', screen: '가입', signUp: { role: null }, message: MSG.roleRequired },
    { label: '로그인 이메일 빈 값', screen: '로그인', login: { email: '' }, field: LABEL.email, message: MSG.emailRequired },
    { label: '로그인 이메일 공백만', screen: '로그인', login: { email: '   ' }, field: LABEL.email, message: MSG.emailRequired },
    { label: '로그인 비밀번호 빈 값', screen: '로그인', login: { password: '' }, field: LABEL.password, message: MSG.passwordRequired },
    // 통과 경계
    { label: '통과: 가입 비밀번호 8자', screen: '가입', signUp: samePw('abcdefgh'), pass: true },
    { label: '통과: 가입 비밀번호 72자', screen: '가입', signUp: samePw('a'.repeat(72)), pass: true },
    { label: '통과: 가입 이름 이모지 50개 (100 UTF-16 유닛)', screen: '가입', signUp: { name: '😀'.repeat(50) }, pass: true },
    { label: '통과: 가입 이름 100자', screen: '가입', signUp: { name: '가'.repeat(100) }, pass: true },
    { label: '통과: 가입 이메일 255자', screen: '가입', signUp: { email: emailOf(255) }, pass: true },
    {
      label: '통과: 가입 이메일 trim 뒤 255자 (앞뒤 공백 포함 259자)',
      screen: '가입',
      signUp: { email: `  ${emailOf(255)}  ` },
      pass: true,
    },
    { label: '통과: 한글 25자 비밀번호 (72바이트 초과여도 클라이언트는 막지 않는다)', screen: '가입', signUp: samePw('가'.repeat(25)), pass: true },
    { label: '통과: 로그인 비밀번호 공백만 8자', screen: '로그인', login: { password: ' '.repeat(8) }, pass: true },
  ]
  test.each(SC07_ROWS)('SC-07 (error) 필수 입력과 길이 위반은 서버로 보내지 않고 필드 아래에 알린다 — $label', async (row) => {
    // Given 위반 하나만 있는 폼
    stubServer()
    renderAt(row.screen === '가입' ? '/signup' : '/login')
    if (row.screen === '가입') fillSignUp(row.signUp)
    else fillLogin(row.login)

    // When
    await clickButton(row.screen === '가입' ? '가입하기' : '로그인')

    if (row.pass) {
      // 경계 통과: 요청이 간다
      await waitFor(() => expect(authCalls()).toHaveLength(1))
      return
    }
    // Then 필드 아래 문구 (+ 필드가 있으면 aria-invalid)
    expect(await screen.findByText(row.message!)).toBeInTheDocument()
    if (row.field) expect(screen.getByLabelText(row.field)).toHaveAttribute('aria-invalid', 'true')
    // And 요청 없음
    expect(authCalls()).toHaveLength(0)
  })

  interface Sc08Row {
    field: 'email' | 'password' | 'name'
    input: string
    sent: string
  }
  const SC08_ROWS: Sc08Row[] = [
    { field: 'email', input: '  hong@example.com  ', sent: 'hong@example.com' },
    { field: 'email', input: `a${NUL}b@example.com`, sent: `a${NUL}b@example.com` },
    { field: 'email', input: `a${LONE}b@example.com`, sent: `a${LONE}b@example.com` },
    { field: 'password', input: ' pass word1 ', sent: ' pass word1 ' },
    { field: 'password', input: `pass${NUL}word1`, sent: `pass${NUL}word1` },
    { field: 'password', input: `pass${LONE}word1`, sent: `pass${LONE}word1` },
    { field: 'name', input: `홍${NUL}길동😀`, sent: `홍${NUL}길동😀` },
    { field: 'name', input: `홍${LONE}길동`, sent: `홍${LONE}길동` },
    { field: 'name', input: ' 홍길동 ', sent: ' 홍길동 ' },
  ]
  test.each(SC08_ROWS)(
    'SC-08 (boundary) 가입 입력은 이메일 앞뒤 공백만 제거하고 나머지는 변환 없이 보낸다 — $field',
    async ({ field, input, sent }) => {
      // Given
      stubServer()
      renderAt('/signup')
      const over: Partial<SignUpValues> =
        field === 'password' ? samePw(input) : field === 'email' ? { email: input } : { name: input }
      fillSignUp(over)

      // When
      await clickButton('가입하기')

      // Then 해당 필드는 전송 값과 정확히 같고 다른 필드는 올바른 값 그대로
      await waitFor(() => expect(callsTo('POST /api/auth/signup')).toHaveLength(1))
      const expected: Record<string, unknown> = {
        email: 'hong@example.com',
        password: 'password1!',
        passwordConfirm: 'password1!',
        name: '홍길동',
        role: 'ORGANIZER',
      }
      if (field === 'password') {
        expected.password = sent
        expected.passwordConfirm = sent
      } else {
        expected[field] = sent
      }
      expect(bodyOf(callsTo('POST /api/auth/signup')[0])).toEqual(expected)
    },
  )

  interface Sc09Row {
    field: 'email' | 'password'
    input: string
    sent: string
  }
  const SC09_ROWS: Sc09Row[] = [
    { field: 'email', input: '  hong@example.com  ', sent: 'hong@example.com' },
    { field: 'email', input: `a${NUL}b@example.com`, sent: `a${NUL}b@example.com` },
    { field: 'email', input: `a${LONE}b@example.com`, sent: `a${LONE}b@example.com` },
    { field: 'password', input: ' pass word1 ', sent: ' pass word1 ' },
    { field: 'password', input: `pass${NUL}word1`, sent: `pass${NUL}word1` },
    { field: 'password', input: `pass${LONE}word1`, sent: `pass${LONE}word1` },
  ]
  test.each(SC09_ROWS)(
    'SC-09 (boundary) 로그인 입력도 이메일 앞뒤 공백만 제거하고 나머지는 변환 없이 보낸다 — $field',
    async ({ field, input, sent }) => {
      // Given
      stubServer()
      renderAt('/login')
      fillLogin({ [field]: input })

      // When
      await clickButton('로그인')

      // Then
      await waitFor(() => expect(callsTo('POST /api/auth/login')).toHaveLength(1))
      const body = bodyOf(callsTo('POST /api/auth/login')[0])
      expect(body[field]).toBe(sent)
      // And 키는 email, password 둘뿐
      expect(Object.keys(body).sort()).toEqual(['email', 'password'])
    },
  )

  test('SC-10 (error) 이메일이 중복이면 전용 메시지와 로그인 링크를 보인다', async () => {
    // Given
    stubServer({ 'POST /api/auth/signup': () => res(409, { code: 'EMAIL_ALREADY_EXISTS', message: 'dup' }) })
    renderAt('/signup')
    const v = fillSignUp()

    // When
    await clickButton('가입하기')

    // Then 전용 문구 + 그 안의 로그인 링크
    expect(await screen.findByText(DUP)).toBeInTheDocument()
    const link = screen
      .getAllByRole('link', { name: '로그인' })
      .find((a) => a.parentElement?.textContent?.includes('이미 가입된 이메일입니다.'))
    expect(link).toBeDefined()
    expect(link).toHaveAttribute('href', '/login')
    // And 입력값 유지
    expect(screen.getByLabelText(LABEL.email)).toHaveValue(v.email)
    expect(screen.getByLabelText(LABEL.name)).toHaveValue(v.name)
    expect(screen.getByRole('radio', { name: '주최자' })).toBeChecked()
    expect(screen.getByLabelText(LABEL.password)).toHaveValue(v.password)
    expect(screen.getByLabelText(LABEL.confirm)).toHaveValue(v.confirm)
    // And 화면 유지, 일반 문구 없음
    expect(where()).toBe('/signup')
    expect(screen.queryByText(MSG.signupFail)).not.toBeInTheDocument()
  })

  interface Sc11Row {
    label: string
    respond: Responder
    message: string
    input?: Partial<SignUpValues>
  }
  const SC11_ROWS: Sc11Row[] = [
    { label: '409 code 다름', respond: () => res(409, { code: 'SOMETHING_ELSE' }), message: MSG.signupFail },
    { label: '409 빈 바디', respond: () => res(409), message: MSG.signupFail },
    {
      label: '400 72바이트 초과 (한글 25자)',
      respond: () => res(400, { code: 'INVALID_REQUEST', message: 'password: UTF-8 기준 72바이트를 넘을 수 없습니다' }),
      message: MSG.signupFail,
      input: samePw('가'.repeat(25)),
    },
    {
      label: '400 이름 NUL',
      respond: () => res(400, { code: 'INVALID_REQUEST', message: 'name: ...' }),
      message: MSG.signupFail,
      input: { name: `a${NUL}b` },
    },
    {
      label: '400 이름 짝 없는 서로게이트',
      respond: () => res(400, { code: 'INVALID_REQUEST', message: 'name: ...' }),
      message: MSG.signupFail,
      input: { name: `a${LONE}b` },
    },
    {
      label: '400 이메일 형식 오류',
      respond: () => res(400, { code: 'INVALID_REQUEST', message: 'email: ...' }),
      message: MSG.signupFail,
      input: { email: 'abc' },
    },
    {
      label: '400 이메일 NUL',
      respond: () => res(400, { code: 'INVALID_REQUEST', message: 'email: ...' }),
      message: MSG.signupFail,
      input: { email: `a${NUL}b@example.com` },
    },
    {
      label: '400 이메일 짝 없는 서로게이트',
      respond: () => res(400, { code: 'INVALID_REQUEST', message: 'email: ...' }),
      message: MSG.signupFail,
      input: { email: `a${LONE}b@example.com` },
    },
    { label: '500 비 JSON', respond: () => text(500, 'Internal Server Error'), message: MSG.signupFail },
    { label: '임의 JSON + 400', respond: () => res(400, { foo: 1 }), message: MSG.signupFail },
    { label: '네트워크 오류', respond: networkError, message: MSG.signupFail },
    {
      label: '401 WRITE_KEY_INVALID (쓰기 가드)',
      respond: () => res(401, { code: 'WRITE_KEY_INVALID', message: 'X-Write-Key' }),
      message: WRITE_REJECTED_MESSAGE,
    },
  ]
  test.each(SC11_ROWS)('SC-11 (error) 중복이 아닌 가입 실패는 일반 문구로 보이고 중복 문구가 새지 않는다 — $label', async (row) => {
    // Given
    stubServer({ 'POST /api/auth/signup': row.respond })
    renderAt('/signup')
    const v = fillSignUp(row.input)

    // When
    await clickButton('가입하기')

    // Then 표의 문구, 중복 문구는 없다
    expect(await screen.findByText(row.message)).toBeInTheDocument()
    expect(screen.queryByText(DUP)).not.toBeInTheDocument()
    // And 화면 유지, 토큰 없음
    expect(where()).toBe('/signup')
    expect(token()).toBeNull()
    // And 입력값 유지, 버튼은 다시 눌러진다
    expect(screen.getByLabelText(LABEL.email)).toHaveValue(v.email)
    expect(screen.getByLabelText(LABEL.name)).toHaveValue(v.name)
    expect(screen.getByRole('radio', { name: '주최자' })).toBeChecked()
    expect(screen.getByLabelText(LABEL.password)).toHaveValue(v.password)
    expect(screen.getByLabelText(LABEL.confirm)).toHaveValue(v.confirm)
    expect(screen.getByRole('button', { name: '가입하기' })).toBeEnabled()
  })

  test.each([
    { screen: '가입' as const, path: '/signup', button: '가입하기', key: 'POST /api/auth/signup' },
    { screen: '로그인' as const, path: '/login', button: '로그인', key: 'POST /api/auth/login' },
  ])('SC-12 (boundary) 제출 중에는 버튼이 비활성이고 중복 요청이 가지 않는다 — $screen', async (row) => {
    // Given 올바르게 채운 폼, 응답은 아직 오지 않는다
    const pending = deferred()
    stubServer({ [row.key]: () => pending.promise })
    renderAt(row.path)
    if (row.screen === '가입') fillSignUp()
    else fillLogin()

    // When 두 번 연속 누른다
    await clickButton(row.button)
    // 비활성 버튼은 pointer-events: none 이라 user-event 가 기본으로 거부한다 — 둘째 클릭만 검사를 끈다.
    await userEvent.setup({ pointerEventsCheck: 0 }).click(screen.getByRole('button', { name: row.button }))

    // Then 요청은 한 번뿐, 응답 전까지 disabled
    expect(callsTo(row.key)).toHaveLength(1)
    expect(screen.getByRole('button', { name: row.button })).toBeDisabled()
    // And 오류 응답이 오면 다시 활성화
    pending.resolve(res(500))
    await waitFor(() => expect(screen.getByRole('button', { name: row.button })).toBeEnabled())
  })

  test('SC-13 (happy) 헤더 링크로 온 로그인에 성공하면 토큰을 저장하고 이전 화면으로 이동한다', async () => {
    // Given /performances/3 에서 헤더의 로그인 링크로 /login 에 왔다
    stubServer()
    renderAt('/performances/3')
    await userEvent.click(await screen.findByRole('link', { name: '로그인' }))
    await waitFor(() => expect(where()).toBe('/login'))
    fillLogin()

    // When
    await clickButton('로그인')

    // Then 요청 본문
    await waitFor(() => expect(callsTo('POST /api/auth/login')).toHaveLength(1))
    expect(bodyOf(callsTo('POST /api/auth/login')[0])).toEqual({ email: 'hong@example.com', password: 'password1!' })
    // And 토큰 저장, 이전 화면으로 이동
    await waitFor(() => expect(where()).toBe('/performances/3'))
    expect(token()).toBe('tok-1')
  })

  interface Sc15Row {
    label: string
    state: unknown
    to: string
  }
  const SC15_ROWS: Sc15Row[] = [
    { label: '이전 화면 정보 없음', state: undefined, to: '/' },
    { label: '앱 안의 경로', state: { from: '/performances/3?tab=a#x' }, to: '/performances/3?tab=a#x' },
    { label: '절대 URL', state: { from: 'https://evil.example/' }, to: '/' },
    { label: '프로토콜 상대 URL', state: { from: '//evil.example/' }, to: '/' },
    { label: 'javascript: 스킴', state: { from: 'javascript:alert(1)' }, to: '/' },
    { label: '/login 순환', state: { from: '/login' }, to: '/' },
    { label: '/signup 순환', state: { from: '/signup' }, to: '/' },
    { label: '빈 문자열', state: { from: '' }, to: '/' },
    { label: '숫자', state: { from: 3 }, to: '/' },
    { label: '객체', state: { from: { pathname: '/performances/3' } }, to: '/' },
  ]
  test.each(SC15_ROWS)('SC-15 (boundary) 이전 화면은 앱 안의 경로만 믿는다 — $label', async ({ state, to }) => {
    // Given
    stubServer()
    renderAt({ pathname: '/login', state })
    fillLogin()

    // When
    await clickButton('로그인')

    // Then
    await waitFor(() => expect(callsTo('POST /api/auth/login')).toHaveLength(1))
    await waitFor(() => expect(where()).toBe(to))
  })

  test.each([
    { label: '일반 비밀번호', password: 'password1!' },
    { label: '한글 25자 (UTF-8 75바이트)', password: '가'.repeat(25) },
    { label: '공백만 8자', password: ' '.repeat(8) },
  ])('SC-16 (error) 자격 증명이 틀리면 고정 문구를 보이고 토큰을 저장하지 않는다 — $label', async ({ password }) => {
    // Given 서버가 401 INVALID_CREDENTIALS
    stubServer({
      'POST /api/auth/login': () => res(401, { code: 'INVALID_CREDENTIALS', message: '서버가 준 다른 문구' }),
    })
    renderAt('/login')
    fillLogin({ password })

    // When
    await clickButton('로그인')

    // Then 요청이 가고 고정 문구, 서버 message 는 나오지 않는다
    expect(await screen.findByText(MSG.badCredentials)).toBeInTheDocument()
    expect(callsTo('POST /api/auth/login')).toHaveLength(1)
    expect(screen.queryByText('서버가 준 다른 문구')).not.toBeInTheDocument()
    // And 토큰 없음, 화면 유지, 이메일 유지
    expect(token()).toBeNull()
    expect(where()).toBe('/login')
    expect(screen.getByLabelText(LABEL.email)).toHaveValue('hong@example.com')
  })

  interface Sc17Row {
    label: string
    respond: Responder
    message: string
  }
  const SC17_ROWS: Sc17Row[] = [
    { label: '401 code 다름', respond: () => res(401, { code: 'UNAUTHORIZED' }), message: MSG.loginFail },
    { label: '401 빈 바디', respond: () => res(401), message: MSG.loginFail },
    { label: '401 비 JSON', respond: () => text(401, 'nope'), message: MSG.loginFail },
    { label: '400 INVALID_REQUEST', respond: () => res(400, { code: 'INVALID_REQUEST', message: 'email: ...' }), message: MSG.loginFail },
    { label: '500', respond: () => res(500), message: MSG.loginFail },
    { label: '네트워크 오류', respond: networkError, message: MSG.loginFail },
    {
      label: '200 accessToken 없음',
      respond: () => res(200, { tokenType: 'Bearer', user: USER }),
      message: MSG.loginFail,
    },
    {
      label: '200 accessToken 숫자',
      respond: () => res(200, { ...LOGIN_RESP, accessToken: 123 }),
      message: MSG.loginFail,
    },
    {
      label: '200 accessToken 빈 문자열',
      respond: () => res(200, { ...LOGIN_RESP, accessToken: '' }),
      message: MSG.loginFail,
    },
    { label: '200 비 JSON', respond: () => text(200, 'ok'), message: MSG.loginFail },
    { label: '200 user 없음', respond: () => res(200, { accessToken: 'tok-1' }), message: MSG.loginFail },
    {
      label: '200 user.name 없음',
      respond: () => res(200, { ...LOGIN_RESP, user: { id: 1, email: 'hong@example.com', role: 'AUDIENCE' } }),
      message: MSG.loginFail,
    },
    {
      label: '401 WRITE_KEY_INVALID (쓰기 가드)',
      respond: () => res(401, { code: 'WRITE_KEY_INVALID', message: 'X-Write-Key' }),
      message: WRITE_REJECTED_MESSAGE,
    },
  ]
  test.each(SC17_ROWS)('SC-17 (error) 자격 증명 실패가 아닌 로그인 이상 응답은 일반 문구로 보인다 — $label', async (row) => {
    // Given
    stubServer({ 'POST /api/auth/login': row.respond })
    renderAt('/login')
    fillLogin()

    // When
    await clickButton('로그인')

    // Then 표의 문구, 자격 증명 실패 문구는 없다
    expect(await screen.findByText(row.message)).toBeInTheDocument()
    expect(screen.queryByText(MSG.badCredentials)).not.toBeInTheDocument()
    // And 토큰 없음, 화면 유지
    expect(token()).toBeNull()
    expect(where()).toBe('/login')
  })

  interface Sc19Row {
    label: string
    screen: '가입' | '로그인'
    first: Responder
    firstText: string | RegExp
    next: Responder
    nextText: string | RegExp | null
  }
  const SC19_ROWS: Sc19Row[] = [
    {
      label: '가입 409 → 500',
      screen: '가입',
      first: () => res(409, { code: 'EMAIL_ALREADY_EXISTS' }),
      firstText: DUP,
      next: () => res(500),
      nextText: MSG.signupFail,
    },
    {
      label: '가입 500 → 409',
      screen: '가입',
      first: () => res(500),
      firstText: MSG.signupFail,
      next: () => res(409, { code: 'EMAIL_ALREADY_EXISTS' }),
      nextText: DUP,
    },
    {
      label: '가입 쓰기 거부 → 201',
      screen: '가입',
      first: () => res(401, { code: 'WRITE_KEY_INVALID' }),
      firstText: WRITE_REJECTED_MESSAGE,
      next: () => res(201, SIGNUP_RESP),
      nextText: null,
    },
    {
      label: '로그인 401 → 500',
      screen: '로그인',
      first: () => res(401, { code: 'INVALID_CREDENTIALS' }),
      firstText: MSG.badCredentials,
      next: () => res(500),
      nextText: MSG.loginFail,
    },
    {
      label: '로그인 500 → 401',
      screen: '로그인',
      first: () => res(500),
      firstText: MSG.loginFail,
      next: () => res(401, { code: 'INVALID_CREDENTIALS' }),
      nextText: MSG.badCredentials,
    },
  ]
  test.each(SC19_ROWS)('SC-19 (boundary) 서버 오류 문구는 재제출을 시작하면 지워지고 새 오류로 바뀐다 — $label', async (row) => {
    // Given 첫 오류가 보이고, 다음 응답은 아직 오지 않는다
    const key = row.screen === '가입' ? 'POST /api/auth/signup' : 'POST /api/auth/login'
    const button = row.screen === '가입' ? '가입하기' : '로그인'
    const second = deferred()
    let count = 0
    stubServer({ [key]: (init) => (++count === 1 ? row.first(init) : second.promise) })
    renderAt(row.screen === '가입' ? '/signup' : '/login')
    if (row.screen === '가입') fillSignUp()
    else fillLogin()
    await clickButton(button)
    expect(await screen.findByText(row.firstText)).toBeInTheDocument()

    // When 다시 누른다
    await clickButton(button)

    // Then 응답이 오기 전에 첫 오류가 사라져 있다
    expect(screen.queryByText(row.firstText)).not.toBeInTheDocument()
    // And 다음 응답의 결과만 보인다
    second.resolve(await row.next({}))
    if (row.nextText) {
      expect(await screen.findByText(row.nextText)).toBeInTheDocument()
    } else {
      await waitFor(() => expect(where()).toBe('/login'))
    }
    expect(screen.queryByText(row.firstText)).not.toBeInTheDocument()
  })

  // ---------- 헤더·세션 ----------

  function loggedInServer() {
    localStorage.setItem('accessToken', 'tok-1')
    stubServer({ 'GET /api/auth/me': () => res(200, USER) })
  }

  test.each(['/', '/performances/3'])('SC-20 (happy) 로그인 상태의 헤더에는 이름과 로그아웃 버튼이 보인다 — %s', async (path) => {
    // Given 홍길동으로 로그인한 상태
    loggedInServer()

    // When 어느 화면이든 헤더를 본다
    renderAt(path)

    // Then 이름과 로그아웃 버튼
    expect(await screen.findByText('홍길동')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그아웃' })).toBeInTheDocument()
    // And 로그인·가입 링크는 없다
    expect(screen.queryByRole('link', { name: '로그인' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: '가입' })).not.toBeInTheDocument()
  })

  test('SC-21 (happy) 로그아웃하면 토큰이 삭제되고 비로그인 상태가 된다', async () => {
    // Given
    loggedInServer()
    renderAt('/')
    await screen.findByText('홍길동')
    const before = calls.length

    // When
    await clickButton('로그아웃')

    // Then 토큰 삭제
    expect(token()).toBeNull()
    // And 헤더가 비로그인으로
    expect(screen.queryByText('홍길동')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '로그아웃' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: '로그인' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '가입' })).toBeInTheDocument()
    // And 서버 요청 없음
    expect(calls).toHaveLength(before)
  })

  test.each([
    { link: '로그인', path: '/login', button: '로그인' },
    { link: '가입', path: '/signup', button: '가입하기' },
  ])('SC-22 (happy) 비로그인 헤더의 링크가 로그인·가입 화면으로 이어진다 — $link', async ({ link, path, button }) => {
    // Given
    stubServer()
    renderAt('/performances/3')

    // When
    await userEvent.click(await screen.findByRole('link', { name: link }))

    // Then
    await waitFor(() => expect(where()).toBe(path))
    expect(screen.getByRole('button', { name: button })).toBeInTheDocument()
  })

  test('SC-23 (happy) 새로고침 후 저장된 토큰으로 로그인 상태가 복원된다', async () => {
    // Given 응답이 아직 오지 않는다
    localStorage.setItem('accessToken', 'tok-1')
    const me = deferred()
    stubServer({ 'GET /api/auth/me': () => me.promise })

    // When 앱을 처음 연다
    renderAt('/')

    // Then me 요청이 한 번, Authorization 이 Bearer tok-1
    await waitFor(() => expect(callsTo('GET /api/auth/me')).toHaveLength(1))
    expect(new Headers(callsTo('GET /api/auth/me')[0].init.headers).get('Authorization')).toBe('Bearer tok-1')
    // And 응답 전에는 아무것도 그리지 않는다
    expect(screen.queryByRole('link', { name: '로그인' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: '가입' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '로그아웃' })).not.toBeInTheDocument()
    // And 응답 뒤에 이름과 로그아웃
    me.resolve(res(200, USER))
    expect(await screen.findByText('홍길동')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그아웃' })).toBeInTheDocument()
  })

  test.each([
    { label: '키 자체가 없다', value: null },
    { label: '빈 문자열', value: '' },
    { label: '공백만', value: '   ' },
  ])('SC-24 (boundary) 저장된 토큰이 없거나 비어 있으면 복원 요청 없이 비로그인으로 보인다 — $label', ({ value }) => {
    // Given
    if (value !== null) localStorage.setItem('accessToken', value)
    stubServer()

    // When
    renderAt('/')

    // Then 요청 없음, 곧바로 비로그인 헤더
    expect(callsTo('GET /api/auth/me')).toHaveLength(0)
    expect(screen.getByRole('link', { name: '로그인' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '가입' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '로그아웃' })).not.toBeInTheDocument()
  })

  interface Sc25Row {
    label: string
    respond: Responder
    kept: boolean
  }
  const SC25_ROWS: Sc25Row[] = [
    { label: '401 UNAUTHORIZED', respond: () => res(401, { code: 'UNAUTHORIZED' }), kept: false },
    { label: '401 빈 바디', respond: () => res(401), kept: false },
    { label: '401 비 JSON', respond: () => text(401, 'nope'), kept: false },
    { label: '500', respond: () => res(500), kept: true },
    { label: '네트워크 오류', respond: networkError, kept: true },
    { label: '200 비 JSON', respond: () => text(200, 'ok'), kept: true },
    { label: '200 name 없는 JSON', respond: () => res(200, { id: 1, email: 'hong@example.com' }), kept: true },
  ]
  test.each(SC25_ROWS)('SC-25 (error) 토큰 복원이 실패하면 비로그인으로 보이고 무효 토큰만 지운다 — $label', async (row) => {
    // Given
    localStorage.setItem('accessToken', 'tok-old')
    stubServer({ 'GET /api/auth/me': row.respond })

    // When
    renderAt('/')

    // Then 비로그인 헤더
    expect(await screen.findByRole('link', { name: '로그인' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '가입' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '로그아웃' })).not.toBeInTheDocument()
    // And 토큰은 401 일 때만 삭제된다
    expect(token()).toBe(row.kept ? 'tok-old' : null)
  })

  test('SC-26 (boundary) 브라우저 저장소를 읽지 못해도 앱이 죽지 않는다', async () => {
    // Given localStorage 읽기가 예외를 던진다
    stubServer()
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('denied')
    })

    // When
    renderAt('/')

    // Then 비로그인 헤더
    expect(await screen.findByRole('link', { name: '로그인' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '가입' })).toBeInTheDocument()
    // And me 요청 없음
    expect(callsTo('GET /api/auth/me')).toHaveLength(0)
  })

  test('SC-27 (boundary) 저장소에 쓰지 못해도 이번 방문 동안은 로그인 상태가 유지된다', async () => {
    // Given localStorage 쓰기만 예외를 던진다
    stubServer()
    renderAt('/login')
    fillLogin()
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('quota')
    })

    // When
    await clickButton('로그인')

    // Then 이전 화면(없으면 홈)으로 이동
    await waitFor(() => expect(where()).toBe('/'))
    // And 헤더에 이름과 로그아웃
    expect(await screen.findByText('홍길동')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그아웃' })).toBeInTheDocument()
  })

  test('SC-28 (regression) 헤더와 새 라우트가 기존 공연 화면을 해치지 않는다', async () => {
    // Given 로그인하지 않았고 서버가 공연 목록·상세를 준다
    stubServer({ 'POST /api/performances': () => res(201, performance) })

    // When / Then 기존 화면 세 곳과 헤더
    renderAt('/')
    expect(await screen.findByText('가을 재즈 콘서트')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '로그인' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '가입' })).toBeInTheDocument()
    cleanup()

    renderAt('/performances/new')
    expect(screen.getByLabelText(/^공연명/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '로그인' })).toBeInTheDocument()
    cleanup()

    renderAt('/performances/3')
    await waitFor(() => expect(callsTo('GET /api/performances/3').length).toBeGreaterThan(0))
    expect(await screen.findByText('가을 재즈 콘서트')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '로그인' })).toBeInTheDocument()
    cleanup()

    // And /login, /signup 은 공연 조회를 하지 않는다
    calls.length = 0
    renderAt('/login')
    expect(await screen.findByRole('button', { name: '로그인' })).toBeInTheDocument()
    cleanup()
    renderAt('/signup')
    expect(await screen.findByRole('button', { name: '가입하기' })).toBeInTheDocument()
    expect(calls.filter((c) => c.key.includes('/api/performances'))).toHaveLength(0)

    // And 공연 등록 요청에는 Authorization 이 붙지 않는다
    await registerPerformance({
      title: '가을 재즈 콘서트',
      venue: 'OO홀',
      startAt: '2026-10-01T19:00:00+09:00',
      openAt: '2026-09-10T10:00:00+09:00',
      closeAt: '2026-09-30T23:59:59+09:00',
      sections: [{ grade: 'VIP', price: 120000, rowStart: 'A', rowEnd: 'B', seatsPerRow: 10 }],
    })
    const post = callsTo('POST /api/performances')
    expect(post).toHaveLength(1)
    expect(new Headers(post[0].init.headers).has('Authorization')).toBe(false)
  })

  test.each([
    { label: '확인란을 비밀번호에 맞췄다', fix: () => setField(LABEL.confirm, 'password1!') },
    { label: '비밀번호를 확인란에 맞췄다', fix: () => setField(LABEL.password, 'password2!') },
  ])('SC-29 (boundary) 불일치를 고쳐 맞추면 오류가 사라지고 제출이 가능하다 — $label', async ({ fix }) => {
    // Given SC-04 처럼 불일치 오류가 보이던 폼
    stubServer()
    renderAt('/signup')
    fillSignUp({ password: 'password1!', confirm: 'password2!' })
    await clickButton('가입하기')
    expect(await screen.findByText(MSG.mismatch)).toBeInTheDocument()

    // When 고친다
    fix()

    // Then 고친 직후 이미 오류가 사라져 있다
    expect(screen.queryByText(MSG.mismatch)).not.toBeInTheDocument()
    expect(screen.getByLabelText(LABEL.confirm)).not.toHaveAttribute('aria-invalid', 'true')
    // And 제출하면 요청이 한 번 가고 두 비밀번호가 같다
    await clickButton('가입하기')
    await waitFor(() => expect(callsTo('POST /api/auth/signup')).toHaveLength(1))
    const body = bodyOf(callsTo('POST /api/auth/signup')[0])
    expect(body.password).toBe(body.passwordConfirm)
    expect(screen.queryByText(MSG.mismatch)).not.toBeInTheDocument()
  })

  test('SC-30 (happy) 중복 안내의 로그인 링크는 로그인 화면으로 이동한다', async () => {
    // Given SC-10 처럼 중복 안내와 링크가 보인다
    stubServer({ 'POST /api/auth/signup': () => res(409, { code: 'EMAIL_ALREADY_EXISTS' }) })
    renderAt('/signup')
    fillSignUp()
    await clickButton('가입하기')
    await screen.findByText(DUP)
    const link = screen
      .getAllByRole('link', { name: '로그인' })
      .find((a) => a.parentElement?.textContent?.includes('이미 가입된 이메일입니다.'))!

    // When
    await userEvent.click(link)

    // Then
    await waitFor(() => expect(where()).toBe('/login'))
    expect(screen.getByRole('button', { name: '로그인' })).toBeInTheDocument()
  })

  test.each([
    { label: '빈 바디', respond: () => res(201) },
    { label: '비 JSON 텍스트', respond: () => text(201, 'created') },
    { label: '{}', respond: () => res(201, {}) },
  ])('SC-31 (boundary) 가입 성공 판정은 응답 본문이 아니라 상태 201 로 한다 — $label', async ({ respond }) => {
    // Given
    stubServer({ 'POST /api/auth/signup': respond })
    renderAt('/signup')
    fillSignUp()

    // When
    await clickButton('가입하기')

    // Then
    await waitFor(() => expect(where()).toBe('/login'))
    // And 실패 문구 없음, 토큰 없음
    expect(screen.queryByText(MSG.signupFail)).not.toBeInTheDocument()
    expect(token()).toBeNull()
  })

  test.each([
    {
      label: '이메일 256자',
      input: { email: emailOf(256) },
      respond: () => res(400, { code: 'INVALID_REQUEST', message: 'email: ...' }),
      message: MSG.loginFail,
    },
    {
      label: '비밀번호 73자',
      input: { password: 'a'.repeat(73) },
      respond: () => res(401, { code: 'INVALID_CREDENTIALS' }),
      message: MSG.badCredentials,
    },
    {
      label: '비밀번호 1000자',
      input: { password: 'a'.repeat(1000) },
      respond: () => res(401, { code: 'INVALID_CREDENTIALS' }),
      message: MSG.badCredentials,
    },
  ])('SC-32 (boundary) 로그인은 길이 상한을 클라이언트가 막지 않고 서버 판정에 맡긴다 — $label', async (row) => {
    // Given
    stubServer({ 'POST /api/auth/login': row.respond })
    renderAt('/login')
    const v = fillLogin(row.input)

    // When
    await clickButton('로그인')

    // Then 요청이 가고 본문이 입력과 정확히 같다
    await waitFor(() => expect(callsTo('POST /api/auth/login')).toHaveLength(1))
    expect(bodyOf(callsTo('POST /api/auth/login')[0])).toEqual(v)
    // And 표의 문구, 토큰 없음
    expect(await screen.findByText(row.message)).toBeInTheDocument()
    expect(token()).toBeNull()
  })
  // ---------- 복원 응답 경쟁 (Phase 4 RC-1) ----------

  const B_LOGIN = {
    accessToken: 'tok-new',
    tokenType: 'Bearer',
    expiresInSeconds: 86400,
    user: { id: 2, email: 'kim@example.com', name: '김철수', role: 'AUDIENCE' },
  }
  const LATE_RESTORE: { label: string; settle: (d: ReturnType<typeof deferred>) => void }[] = [
    { label: '200 + 홍길동', settle: (d) => d.resolve(res(200, USER)) },
    { label: '401 UNAUTHORIZED', settle: (d) => d.resolve(res(401, { code: 'UNAUTHORIZED' })) },
    { label: '401 빈 바디', settle: (d) => d.resolve(res(401)) },
    { label: '500', settle: (d) => d.resolve(res(500)) },
    { label: '네트워크 오류', settle: (d) => d.reject(new TypeError('Failed to fetch')) },
    { label: '200 비 JSON', settle: (d) => d.resolve(text(200, 'ok')) },
  ]
  // 늦게 온 응답이 처리될 시간을 준다 — 아무 일도 일어나지 않아야 하는 단언 전용.
  const settleLate = (d: ReturnType<typeof deferred>, settle: (d: ReturnType<typeof deferred>) => void) =>
    act(async () => {
      settle(d)
      await new Promise((r) => setTimeout(r, 30))
    })

  async function loginWhileRestoring() {
    localStorage.setItem('accessToken', 'tok-old')
    const me = deferred()
    stubServer({ 'GET /api/auth/me': () => me.promise, 'POST /api/auth/login': () => res(200, B_LOGIN) })
    renderAt('/login')
    fillLogin({ email: 'kim@example.com' })
    await clickButton('로그인')
    return me
  }

  test.each(LATE_RESTORE)('SC-33 (boundary) 복원이 끝나기 전에 로그인하면 늦게 온 복원 응답이 덮어쓰지 않는다 — $label', async ({ settle }) => {
    // Given 복원 중(tok-old)이고 로그인 응답이 김철수다 / When 로그인을 누른다
    const me = await loginWhileRestoring()

    // Then 복원 응답이 오기 전에 이미 헤더에 김철수와 로그아웃이 보인다
    expect(await screen.findByText('김철수')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그아웃' })).toBeInTheDocument()
    // And 늦은 복원 응답이 도착한 뒤에도 김철수, 토큰은 tok-new
    await settleLate(me, settle)
    expect(screen.getByText('김철수')).toBeInTheDocument()
    expect(token()).toBe('tok-new')
    // And 복원 응답의 사용자는 나타나지 않는다
    expect(screen.queryByText('홍길동')).not.toBeInTheDocument()
  })

  test.each(LATE_RESTORE.filter((r) => ['200 + 홍길동', '401 UNAUTHORIZED', '500'].includes(r.label)))(
    'SC-34 (boundary) 복원 중 로그인했다 로그아웃하면 늦게 온 복원 응답이 다시 로그인시키지 않는다 — $label',
    async ({ settle }) => {
      // Given 복원 중에 김철수로 로그인했고 헤더에 로그아웃이 보인다
      const me = await loginWhileRestoring()
      await screen.findByText('김철수')

      // When 로그아웃을 누른다
      await clickButton('로그아웃')

      // Then 늦은 복원 응답이 도착한 뒤에도 로그인·가입 링크만 있다
      await settleLate(me, settle)
      expect(screen.getByRole('link', { name: '로그인' })).toBeInTheDocument()
      expect(screen.getByRole('link', { name: '가입' })).toBeInTheDocument()
      expect(screen.queryByText('김철수')).not.toBeInTheDocument()
      expect(screen.queryByText('홍길동')).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: '로그아웃' })).not.toBeInTheDocument()
      // And accessToken 이 없다
      expect(token()).toBeNull()
    },
  )

  const STRICT_ROWS = [
    { label: '200·200', first: () => res(200, USER), second: () => res(200, USER), user: true, tokenKept: true },
    { label: '401·401', first: () => res(401), second: () => res(401), user: false, tokenKept: false },
    { label: '401(취소된 요청)·200', first: () => res(401), second: () => res(200, USER), user: true, tokenKept: true },
  ]
  test.each(STRICT_ROWS)('SC-35 (boundary) 개발 모드의 이중 마운트에서도 복원 결과가 맞다 — $label', async (row) => {
    // Given tok-1 이 있고 StrictMode 안에서 렌더되며, me 는 요청 순서대로 응답한다
    localStorage.setItem('accessToken', 'tok-1')
    let n = 0
    stubServer({ 'GET /api/auth/me': () => (++n === 1 ? row.first() : row.second()) })

    // When 앱을 처음 연다
    renderAt('/', { strict: true })

    // Then 표의 헤더
    if (row.user) {
      expect(await screen.findByText('홍길동')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '로그아웃' })).toBeInTheDocument()
    } else {
      expect(await screen.findByRole('link', { name: '로그인' })).toBeInTheDocument()
      expect(screen.getByRole('link', { name: '가입' })).toBeInTheDocument()
    }
    // And 토큰
    await act(async () => {
      await new Promise((r) => setTimeout(r, 30))
    })
    expect(token()).toBe(row.tokenKept ? 'tok-1' : null)
  })
})
