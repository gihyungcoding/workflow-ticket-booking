import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter, useLocation } from 'react-router-dom'
import App from '../App'
import { theme } from '../theme'
import { request } from '../api/http'
import { AuthProvider } from './AuthContext'

// TASK-024 시나리오 SC-01 ~ SC-16 (SC-13·SC-14 는 기존 승인 테스트의 행을 바꾼다 — 이 파일에 없다).
// SoT: workflow_design/05_scenario/SCENARIO_TASK-024.md
// 서버는 두지 않는다 — fetch 응답만 대체하고 앱 전체(App)를 MemoryRouter + AuthProvider 로 지난다.
// 표로 묶은 시나리오는 test.each 한 줄(= 시나리오 한 건)이고 행마다 케이스가 하나씩 돈다.

const MSG = {
  onlyTitle: '주최자만 이용할 수 있어요',
  onlySub: '공연 등록과 관리는 주최자 계정에서 할 수 있어요',
  browse: '공연 둘러보기',
  oldRejected: '쓰기 권한 확인에 실패했습니다. 관리자에게 문의해 주세요',
  registerFailed: '등록하지 못했습니다. 다시 시도해 주세요',
  timeOrder: '예매 오픈·마감·공연 일시 순서가 올바르지 않습니다',
  alreadyOpen: '이미 오픈된 공연은 기본 정보를 수정할 수 없습니다',
  saveFailed: '저장하지 못했습니다. 다시 시도해 주세요',
  cancelFailed: '취소하지 못했습니다. 다시 시도해 주세요',
  cancelDialog: '이 공연을 취소하시겠습니까?',
  loginHero: '쓰던 계정으로 바로 들어와요',
}

const ORG = { id: 1, email: 'org@ticket.on', name: '주최자', role: 'ORGANIZER', handle: 'org01' }
const AUD = { id: 2, email: 'aud@ticket.on', name: '관객', role: 'AUDIENCE', handle: 'aud01' }
const loginBody = (accessToken: string, user: Record<string, unknown>) => ({
  accessToken,
  tokenType: 'Bearer',
  expiresInSeconds: 86400,
  user,
})
const P5 = {
  id: 5,
  title: '가을 재즈 콘서트',
  venue: 'OO홀',
  startAt: '2099-10-01T19:00:00+09:00',
  openAt: '2099-09-10T10:00:00+09:00',
  closeAt: '2099-09-30T23:59:59+09:00',
  totalSeats: 20,
  availableSeats: 20,
  status: 'UPCOMING',
  sections: [{ grade: 'VIP', price: 120000, seatCount: 20 }],
}
const P3 = { ...P5, id: 3 }
const CREATED = { ...P5, id: 42, sections: undefined }

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
const ME = 'GET /api/auth/me'
const LOGIN = 'POST /api/auth/login'
const REGISTER = 'POST /api/performances'
const UPDATE = 'PUT /api/performances/5'
const CANCEL = 'POST /api/performances/5/cancel'

function stubServer(routes: Record<string, Responder> = {}) {
  calls = []
  const table: Record<string, Responder> = {
    'GET /api/performances': () => res(200, { content: [P3], page: 0, size: 20, totalElements: 1 }),
    'GET /api/performances/3': () => res(200, P3),
    'GET /api/performances/5': () => res(200, P5),
    [LOGIN]: () => res(200, loginBody('tok-org', ORG)),
    [REGISTER]: () => res(201, CREATED),
    [UPDATE]: () => res(200, { ...P5, sections: undefined }),
    [CANCEL]: () => res(200, { ...P5, status: 'CANCELLED', sections: undefined }),
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
const headersOf = (call: Call) => new Headers(call.init.headers)

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
  const from = (l.state as { from?: unknown } | null)?.from
  return (
    <>
      <div data-testid="where">{l.pathname + l.search + l.hash}</div>
      <div data-testid="from">{typeof from === 'string' ? from : ''}</div>
    </>
  )
}
const where = () => screen.getByTestId('where').textContent
const fromState = () => screen.getByTestId('from').textContent

function renderAt(entry: string) {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[entry]}>
        <AuthProvider>
          <App />
          <Where />
        </AuthProvider>
      </MemoryRouter>
    </ThemeProvider>,
  )
}

const token = () => localStorage.getItem('accessToken')
const settle = () =>
  act(async () => {
    await new Promise((r) => setTimeout(r, 30))
  })

// 로그인 상태로 시작 — 저장된 토큰 + /api/auth/me 가 그 사용자
function signedInAs(user: Record<string, unknown>, tok: string): Record<string, Responder> {
  localStorage.setItem('accessToken', tok)
  return { [ME]: () => res(200, user) }
}

const formShown = () => screen.queryByLabelText(/^공연명/) !== null
const noticeShown = () => screen.queryByText(MSG.onlyTitle) !== null

// 「공연 둘러보기」 는 링크·버튼 어느 쪽으로 그려도 된다 — 역할을 둘 다 찾는다
const browseControl = () => screen.queryByRole('link', { name: MSG.browse }) ?? screen.getByRole('button', { name: MSG.browse })

async function expectNotice() {
  expect(await screen.findByText(MSG.onlyTitle)).toBeInTheDocument()
  expect(screen.getByText(MSG.onlySub)).toBeInTheDocument()
  expect(browseControl()).toBeInTheDocument()
  expect(formShown()).toBe(false)
}

async function fillRegister() {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText(/^공연명/), '가을 재즈 콘서트')
  await user.type(screen.getByLabelText(/^장소/), 'OO홀')
  fireEvent.change(screen.getByLabelText(/^공연일시/), { target: { value: '2099-10-01T19:00' } })
  fireEvent.change(screen.getByLabelText(/^오픈/), { target: { value: '2099-09-10T10:00' } })
  fireEvent.change(screen.getByLabelText(/^마감/), { target: { value: '2099-09-30T23:59' } })
  await user.type(screen.getAllByLabelText(/^등급/)[0], 'VIP')
  await user.type(screen.getAllByLabelText(/^가격/)[0], '120000')
  await user.type(screen.getAllByLabelText(/^시작 행/)[0], 'A')
  await user.type(screen.getAllByLabelText(/^종료 행/)[0], 'B')
  await user.type(screen.getAllByLabelText(/^행당 좌석수/)[0], '10')
  return user
}

type WriteAction = 'register' | 'save' | 'cancel'
const SCREEN_OF: Record<WriteAction, string> = {
  register: '/performances/new',
  save: '/performances/5/edit',
  cancel: '/performances/5/edit',
}
const KEY_OF: Record<WriteAction, string> = { register: REGISTER, save: UPDATE, cancel: CANCEL }

// 화면이 폼을 그린 뒤 쓰기 동작을 한다 (취소는 확인 대화상자의 「확인」까지)
async function doWrite(action: WriteAction) {
  const user = action === 'register' ? await fillRegister() : userEvent.setup()
  if (action === 'register') {
    await user.click(screen.getByRole('button', { name: '등록' }))
  } else if (action === 'save') {
    await screen.findByDisplayValue('가을 재즈 콘서트')
    await user.click(screen.getByRole('button', { name: '저장' }))
  } else {
    await screen.findByDisplayValue('가을 재즈 콘서트')
    await user.click(screen.getByRole('button', { name: '공연 취소' }))
    await user.click(await screen.findByRole('button', { name: '확인' }))
  }
}

async function loginWith(email = 'org@ticket.on', password = 'password1') {
  fireEvent.change(screen.getByLabelText(/^이메일$/), { target: { value: email } })
  fireEvent.change(screen.getByLabelText(/^비밀번호$/), { target: { value: password } })
  fireEvent.click(screen.getByRole('button', { name: '로그인하기' }))
}

beforeEach(() => {
  localStorage.clear()
})
afterEach(() => {
  vi.unstubAllEnvs()
  vi.unstubAllGlobals()
  localStorage.clear()
})

// ---------- F1 요청 헤더 ----------

describe('TASK-024 F1 — 요청 헤더', () => {
  const SC01 = [
    { row: 'a', method: undefined },
    { row: 'b', method: 'POST' },
    { row: 'c', method: 'PUT' },
    { row: 'd', method: 'PATCH' },
    { row: 'e', method: 'DELETE' },
    { row: 'f', method: 'post' },
  ]
  test.each(SC01)(
    'SC-01 (happy) 저장된 토큰이 있으면 요청에 Bearer 가 실리고, 쓰기 키는 어디에도 없다 — $row $method',
    async ({ method }) => {
      // Given 저장된 토큰 tok-org, 옛 코드라면 키를 실었을 VITE_WRITE_KEY
      localStorage.setItem('accessToken', 'tok-org')
      vi.stubEnv('VITE_WRITE_KEY', 'k-123')
      stubServer({ 'GET /api/x': () => res(200), 'POST /api/x': () => res(200), 'PUT /api/x': () => res(200), 'PATCH /api/x': () => res(200), 'DELETE /api/x': () => res(200) })

      // When
      await request('/api/x', method === undefined ? {} : { method })

      // Then Authorization: Bearer tok-org
      expect(calls).toHaveLength(1)
      expect(headersOf(calls[0]).get('Authorization')).toBe('Bearer tok-org')
      // And X-Write-Key 없음
      expect(headersOf(calls[0]).has('X-Write-Key')).toBe(false)
      // And 주소는 apiUrl 규칙 그대로
      expect((vi.mocked(fetch).mock.calls[0] as unknown[])[0]).toBe('/api/x')
    },
  )

  const SC02: { row: string; stored: string | null | 'throws'; expected: string | null }[] = [
    { row: 'a 키 없음', stored: null, expected: null },
    { row: 'b 빈 값', stored: '', expected: null },
    { row: 'c 공백만', stored: '   ', expected: null },
    { row: 'd 저장소 예외', stored: 'throws', expected: null },
    { row: 'e 앞뒤 공백', stored: ' tok-1 ', expected: 'Bearer tok-1' },
    { row: 'f 끝 개행', stored: 'tok-1\n', expected: 'Bearer tok-1' },
    { row: 'g NUL', stored: 'tok\u00001', expected: null },
    { row: 'h 헤더 바이트 밖', stored: '토큰', expected: null },
    { row: 'i Latin-1', stored: 'tök', expected: 'Bearer tök' },
    { row: 'j 접두사까지 저장', stored: 'Bearer tok-1', expected: 'Bearer Bearer tok-1' },
  ]
  test.each(SC02)(
    'SC-02 (boundary) 토큰이 없거나 헤더에 실을 수 없으면 Authorization 없이 요청은 나간다 — $row',
    async ({ stored, expected }) => {
      // Given
      if (stored === 'throws') {
        vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
          throw new Error('storage blocked')
        })
      } else if (stored !== null) {
        localStorage.setItem('accessToken', stored)
      }
      stubServer({ [REGISTER]: () => res(201, CREATED) })

      // When
      try {
        await request('/api/performances', { method: 'POST' })
      } finally {
        vi.restoreAllMocks()
      }

      // Then 정확히 1번 나간다
      expect(calls).toHaveLength(1)
      // And Authorization 은 표의 값
      const headers = headersOf(calls[0])
      if (expected === null) expect(headers.has('Authorization')).toBe(false)
      else expect(headers.get('Authorization')).toBe(expected)
      // And X-Write-Key 없음
      expect(headers.has('X-Write-Key')).toBe(false)
    },
  )

  const SC03: { row: string; stored: string | null; caller: string | null; expected: string | null }[] = [
    { row: 'a 둘 다', stored: 'tok-A', caller: 'Bearer tok-B', expected: 'Bearer tok-B' },
    { row: 'b 호출부만', stored: null, caller: 'Bearer tok-B', expected: 'Bearer tok-B' },
    { row: 'c 저장만', stored: 'tok-A', caller: null, expected: 'Bearer tok-A' },
    { row: 'd 둘 다 없음', stored: null, caller: null, expected: null },
  ]
  test.each(SC03)(
    'SC-03 (boundary) 호출부가 Authorization 을 실었으면 저장된 토큰으로 덮어쓰지 않는다 — $row',
    async ({ stored, caller, expected }) => {
      // Given
      if (stored !== null) localStorage.setItem('accessToken', stored)
      stubServer({ [ME]: () => res(200, ORG) })

      // When
      await request('/api/auth/me', caller === null ? {} : { headers: { Authorization: caller } })

      // Then
      const headers = headersOf(calls[0])
      if (expected === null) expect(headers.has('Authorization')).toBe(false)
      else expect(headers.get('Authorization')).toBe(expected)
    },
  )
})

// ---------- F2 가드 ----------

describe('TASK-024 F2 — 주최자 가드', () => {
  test('SC-04 (happy) 주최자는 등록 화면에서 공연을 등록하고, 요청에 자기 토큰이 실린다', async () => {
    // Given ORG 로그인 상태, 등록 201 { id: 42 }
    stubServer({ ...signedInAs(ORG, 'tok-org') })

    // When /performances/new 를 연다
    renderAt('/performances/new')

    // Then 폼이 보인다(제출 전)
    expect(await screen.findByLabelText(/^공연명/)).toBeInTheDocument()

    // When 유효한 값을 채워 제출한다
    await doWrite('register')

    // Then 등록 요청에 Bearer tok-org, X-Write-Key 없음
    await waitFor(() => expect(callsTo(REGISTER)).toHaveLength(1))
    expect(headersOf(callsTo(REGISTER)[0]).get('Authorization')).toBe('Bearer tok-org')
    expect(headersOf(callsTo(REGISTER)[0]).has('X-Write-Key')).toBe(false)
    // And /performances/42
    await waitFor(() => expect(where()).toBe('/performances/42'))
  })

  const SC05 = [
    { row: 'a', path: '/performances/new', user: ORG, tok: 'tok-org', shows: 'form' },
    { row: 'b', path: '/performances/5/edit', user: ORG, tok: 'tok-org', shows: 'form-p5' },
    { row: 'c', path: '/performances/5/edit?tab=info#top', user: ORG, tok: 'tok-org', shows: 'form' },
    { row: 'd', path: '/performances/new', user: AUD, tok: 'tok-aud', shows: 'notice' },
  ] as const
  test.each(SC05)(
    'SC-05 (error) 비로그인으로 보호 경로에 들어가면 로그인으로 가고, 로그인 후 원래 경로로 돌아온다 — $row $path',
    async ({ path, user, tok, shows }) => {
      // Given 비로그인, 로그인은 표의 사용자
      stubServer({ [LOGIN]: () => res(200, loginBody(tok, user)) })

      // When 표의 주소를 연다
      renderAt(path)

      // Then /login, 로그인 화면
      await waitFor(() => expect(where()).toBe('/login'))
      expect(screen.getByRole('heading', { name: MSG.loginHero })).toBeInTheDocument()
      // And 폼은 그려진 적이 없다 — 지금도 없다
      expect(formShown()).toBe(false)
      // And 수정 경로면 공연 조회를 부르지 않았다
      expect(callsTo('GET /api/performances/5')).toHaveLength(0)

      // When 로그인한다
      await loginWith()

      // Then 원래 주소(경로·쿼리·해시)로 돌아온다
      await waitFor(() => expect(where()).toBe(path))
      // And 표의 화면
      if (shows === 'notice') {
        await expectNotice()
      } else {
        expect(await screen.findByLabelText(/^공연명/)).toBeInTheDocument()
        if (shows === 'form-p5') expect(await screen.findByDisplayValue('가을 재즈 콘서트')).toBeInTheDocument()
      }
    },
  )

  const SC06 = [
    { row: 'a', path: '/performances/new/' },
    { row: 'b', path: '/Performances/NEW' },
    { row: 'c', path: '/performances/%6Eew' },
    { row: 'd', path: '/performances/5/edit/' },
    { row: 'e', path: '/PERFORMANCES/5/Edit' },
    { row: 'f', path: '/performances/5/%65dit' },
    { row: 'g', path: '/performances/%E0/edit' },
  ]
  test.each(SC06)(
    'SC-06 (boundary) 표기가 달라도 라우터가 등록·수정 화면으로 고르는 주소면 가드를 지난다 — $row $path',
    async ({ path }) => {
      // Given 비로그인
      stubServer()

      // When 표의 주소를 연다
      const first = renderAt(path)

      // Then /login, from 은 연 주소 그대로
      await waitFor(() => expect(where()).toBe('/login'))
      expect(fromState()).toBe(path)
      // And 폼은 없다
      expect(formShown()).toBe(false)
      first.unmount()
      cleanup()

      // Given AUD 로그인 상태
      stubServer({ ...signedInAs(AUD, 'tok-aud') })

      // When 같은 주소를 연다
      renderAt(path)

      // Then 주최자 전용 안내, 폼 없음
      await expectNotice()
    },
  )

  const SC07 = [
    { row: 'a', path: '/performances/new' },
    { row: 'b', path: '/performances/5/edit' },
  ]
  test.each(SC07)(
    'SC-07 (error) 관객이 보호 경로에 들어가면 폼 대신 주최자 전용 안내가 보인다 — $row $path',
    async ({ path }) => {
      // Given AUD 로그인 상태
      stubServer({ ...signedInAs(AUD, 'tok-aud') })

      // When 표의 주소를 연다
      renderAt(path)

      // Then 주최자 전용 안내(제목·부제·「공연 둘러보기」)
      await expectNotice()
      // And 폼이 없고 주소는 그대로
      expect(formShown()).toBe(false)
      expect(where()).toBe(path)
      // And 수정 경로면 공연 조회를 부르지 않았다
      await settle()
      expect(callsTo('GET /api/performances/5')).toHaveLength(0)

      // When 「공연 둘러보기」 를 누른다
      fireEvent.click(browseControl())

      // Then /
      await waitFor(() => expect(where()).toBe('/'))
    },
  )

  type MeResult = 'org' | 'aud' | '401' | '500' | 'network' | 'admin'
  const SC08: { row: string; path: string; me: MeResult; result: 'form' | 'form-p5' | 'notice' | 'login'; tokenAfter: string | null }[] = [
    { row: 'a', path: '/performances/new', me: 'org', result: 'form', tokenAfter: 'tok-org' },
    { row: 'b', path: '/performances/5/edit', me: 'org', result: 'form-p5', tokenAfter: 'tok-org' },
    { row: 'c', path: '/performances/new', me: 'aud', result: 'notice', tokenAfter: 'tok-org' },
    { row: 'd', path: '/performances/new', me: '401', result: 'login', tokenAfter: null },
    { row: 'e', path: '/performances/new', me: '500', result: 'login', tokenAfter: 'tok-org' },
    { row: 'f', path: '/performances/new', me: 'network', result: 'login', tokenAfter: 'tok-org' },
    { row: 'g', path: '/performances/new', me: 'admin', result: 'login', tokenAfter: 'tok-org' },
  ]
  test.each(SC08)(
    'SC-08 (boundary) 토큰을 복원하는 동안 가드는 아무것도 그리지 않고, 결과에 따라 갈린다 — $row $me',
    async ({ path, me, result, tokenAfter }) => {
      // Given 저장된 토큰, /api/auth/me 를 붙들어 둔다
      localStorage.setItem('accessToken', 'tok-org')
      const held = deferred()
      stubServer({ [ME]: () => held.promise })

      // When 표의 주소를 연다
      renderAt(path)
      await settle()

      // Then 아무것도 그리지 않는다
      expect(formShown()).toBe(false)
      expect(noticeShown()).toBe(false)
      expect(where()).toBe(path)
      // And 수정 경로면 공연 조회를 아직 부르지 않았다
      expect(callsTo('GET /api/performances/5')).toHaveLength(0)

      // When 붙들어 둔 /api/auth/me 를 끝낸다
      await act(async () => {
        if (me === 'org') held.resolve(res(200, ORG))
        else if (me === 'aud') held.resolve(res(200, AUD))
        else if (me === '401') held.resolve(res(401, { code: 'UNAUTHORIZED' }))
        else if (me === '500') held.resolve(res(500))
        else if (me === 'admin') held.resolve(res(200, { ...ORG, role: 'ADMIN' }))
        else held.reject(new TypeError('Failed to fetch'))
      })

      // Then 표의 결과
      if (result === 'notice') {
        await expectNotice()
      } else if (result === 'login') {
        await waitFor(() => expect(where()).toBe('/login'))
        expect(fromState()).toBe('/performances/new')
      } else {
        expect(await screen.findByLabelText(/^공연명/)).toBeInTheDocument()
        if (result === 'form-p5') expect(await screen.findByDisplayValue('가을 재즈 콘서트')).toBeInTheDocument()
      }
      expect(token()).toBe(tokenAfter)
    },
  )
})

// ---------- F3 쓰기 API 401/403 ----------

describe('TASK-024 F3 — 쓰기 API 401/403', () => {
  const SC09: { row: string; path: string; action: WriteAction; respond: Responder }[] = [
    { row: 'a', path: '/performances/new', action: 'register', respond: () => res(401, { code: 'UNAUTHORIZED', message: '인증이 필요합니다. 토큰이 없거나 올바르지 않습니다' }) },
    { row: 'b', path: '/performances/5/edit', action: 'save', respond: () => res(401, { code: 'UNAUTHORIZED' }) },
    { row: 'c', path: '/performances/5/edit', action: 'cancel', respond: () => res(401, { code: 'UNAUTHORIZED' }) },
    { row: 'd 빈 본문', path: '/performances/new', action: 'register', respond: () => res(401) },
    { row: 'e 비 JSON', path: '/performances/new', action: 'register', respond: () => text(401, '<html>') },
    { row: 'f 옛 코드', path: '/performances/new', action: 'register', respond: () => res(401, { code: 'WRITE_KEY_INVALID' }) },
    { row: 'g 쿼리', path: '/performances/5/edit?tab=info', action: 'save', respond: () => res(401, { code: 'UNAUTHORIZED' }) },
  ]
  test.each(SC09)(
    'SC-09 (error) 쓰기 API 가 401 이면 로그아웃되고, 로그인으로 갔다가 같은 화면으로 돌아온다 — $row $action',
    async ({ path, action, respond }) => {
      // Given ORG 로그인 상태로 그 화면, 쓰기는 401. 다시 로그인하면 tok-org
      stubServer({ ...signedInAs(ORG, 'tok-org'), [KEY_OF[action]]: respond, [LOGIN]: () => res(200, loginBody('tok-org', ORG)) })
      renderAt(path)
      await screen.findByLabelText(/^공연명/)

      // When 쓰기 동작
      await doWrite(action)

      // Then 저장된 토큰이 지워진다
      await waitFor(() => expect(token()).toBeNull())
      // And /login, from = 그 화면의 주소
      await waitFor(() => expect(where()).toBe('/login'))
      expect(fromState()).toBe(path)
      // And 로그인 화면이 보인다
      expect(screen.getByRole('heading', { name: MSG.loginHero })).toBeInTheDocument()

      // When ORG 로 다시 로그인한다
      await loginWith()

      // Then 그 화면으로 돌아오고 폼이 보인다
      await waitFor(() => expect(where()).toBe(path))
      expect(await screen.findByLabelText(/^공연명/)).toBeInTheDocument()
    },
  )

  const SC10: { row: string; path: string; action: WriteAction; respond: Responder }[] = [
    { row: 'a', path: '/performances/new', action: 'register', respond: () => res(403, { code: 'FORBIDDEN_ROLE', message: '주최자만 이용할 수 있습니다' }) },
    { row: 'b', path: '/performances/5/edit', action: 'save', respond: () => res(403, { code: 'FORBIDDEN_ROLE' }) },
    { row: 'c', path: '/performances/5/edit', action: 'cancel', respond: () => res(403, { code: 'FORBIDDEN_ROLE' }) },
    { row: 'd 빈 본문', path: '/performances/new', action: 'register', respond: () => res(403) },
    { row: 'e 비 JSON', path: '/performances/new', action: 'register', respond: () => text(403, 'Forbidden') },
    { row: 'f 옛 코드', path: '/performances/new', action: 'register', respond: () => res(403, { code: 'WRITE_KEY_INVALID' }) },
  ]
  test.each(SC10)(
    'SC-10 (error) 쓰기 API 가 403 이면 폼 대신 주최자 전용 안내가 보이고, 로그인은 유지된다 — $row $action',
    async ({ path, action, respond }) => {
      // Given ORG 로그인 상태로 그 화면, 쓰기는 403
      stubServer({ ...signedInAs(ORG, 'tok-org'), [KEY_OF[action]]: respond })
      renderAt(path)
      await screen.findByLabelText(/^공연명/)

      // When 쓰기 동작
      await doWrite(action)

      // Then 주최자 전용 안내, 폼 없음
      await expectNotice()
      // And 주소 그대로
      expect(where()).toBe(path)
      // And 토큰 그대로
      expect(token()).toBe('tok-org')
      // And 옛 쓰기 키 문구 없음
      expect(screen.queryByText(MSG.oldRejected)).not.toBeInTheDocument()
      // And 취소 확인 대화상자는 닫혀 있다
      await waitFor(() => expect(screen.queryByText(MSG.cancelDialog)).not.toBeInTheDocument())
    },
  )

  type Late = '401' | '403' | '500' | 'network'
  const SC11: { row: string; relogin: boolean; late: Late }[] = [
    { row: 'a', relogin: true, late: '401' },
    { row: 'b', relogin: true, late: '403' },
    { row: 'c', relogin: true, late: '500' },
    { row: 'd', relogin: true, late: 'network' },
    { row: 'e', relogin: false, late: '403' },
    { row: 'f', relogin: false, late: '401' },
  ]
  test.each(SC11)(
    'SC-11 (boundary) 화면을 떠난 뒤 도착한 쓰기 응답은 로그인 상태·주소·화면을 바꾸지 않는다 — $row 재로그인 $relogin · $late',
    async ({ relogin, late }) => {
      // Given ORG 로그인 상태로 등록 화면, 등록 응답을 붙들어 둔다. 다시 로그인하면 tok-org2
      const held = deferred()
      stubServer({ ...signedInAs(ORG, 'tok-org'), [REGISTER]: () => held.promise, [LOGIN]: () => res(200, loginBody('tok-org2', ORG)) })
      renderAt('/performances/new')
      await screen.findByLabelText(/^공연명/)

      // When 등록을 제출하고, 응답 전에 로그아웃한다
      await doWrite('register')
      await waitFor(() => expect(callsTo(REGISTER)).toHaveLength(1))
      fireEvent.click(screen.getByRole('button', { name: '로그아웃' }))

      // Then /login (가드가 보냈다)
      await waitFor(() => expect(where()).toBe('/login'))

      // When 그다음
      if (relogin) {
        await loginWith()
        await waitFor(() => expect(where()).toBe('/performances/new'))
        await screen.findByLabelText(/^공연명/)
      }
      // And 붙들어 둔 응답을 끝낸다
      await act(async () => {
        if (late === '401') held.resolve(res(401, { code: 'UNAUTHORIZED' }))
        else if (late === '403') held.resolve(res(403, { code: 'FORBIDDEN_ROLE' }))
        else if (late === '500') held.resolve(res(500))
        else held.reject(new TypeError('Failed to fetch'))
      })
      await settle()

      // Then 남는 상태
      if (relogin) {
        expect(where()).toBe('/performances/new')
        expect(formShown()).toBe(true)
        expect(token()).toBe('tok-org2')
      } else {
        expect(where()).toBe('/login')
        expect(token()).toBeNull()
      }
      // And 안내·옛 문구·등록 실패 문구가 없다
      expect(noticeShown()).toBe(false)
      expect(screen.queryByText(MSG.oldRejected)).not.toBeInTheDocument()
      expect(screen.queryByText(MSG.registerFailed)).not.toBeInTheDocument()
    },
  )

  const SC12: { row: string; action: WriteAction; respond: Responder; message: string }[] = [
    { row: 'a', action: 'register', respond: () => res(500), message: MSG.registerFailed },
    { row: 'b', action: 'register', respond: () => res(400, { code: 'INVALID_TIME_ORDER' }), message: MSG.timeOrder },
    { row: 'c', action: 'save', respond: () => res(409, { code: 'REGISTRATION_ALREADY_OPEN' }), message: MSG.alreadyOpen },
    { row: 'd', action: 'save', respond: () => res(404, { code: 'PERFORMANCE_NOT_FOUND' }), message: MSG.saveFailed },
    { row: 'e', action: 'cancel', respond: () => res(500), message: MSG.cancelFailed },
  ]
  test.each(SC12)(
    'SC-12 (regression) 401·403 이 아닌 쓰기 실패는 지금 문구 그대로이고, 다시 제출하면 꺼진다 — $row $action',
    async ({ row, action, respond, message }) => {
      // Given ORG 로그인 상태로 그 화면, 쓰기는 표의 응답
      let n = 0
      const path = SCREEN_OF[action]
      stubServer({
        ...signedInAs(ORG, 'tok-org'),
        [KEY_OF[action]]: (init) => (row === 'a' && ++n > 1 ? res(201, CREATED) : respond(init)),
      })
      renderAt(path)
      await screen.findByLabelText(/^공연명/)

      // When 쓰기 동작
      await doWrite(action)

      // Then 표의 문구, 폼은 그대로
      expect(await screen.findByText(message)).toBeInTheDocument()
      expect(formShown()).toBe(true)
      // And 토큰·주소 그대로
      expect(token()).toBe('tok-org')
      expect(where()).toBe(path)

      if (row === 'a') {
        // When 다시 제출한다(서버는 201)
        fireEvent.click(screen.getByRole('button', { name: '등록' }))
        // Then 문구가 없어지고 /performances/42
        await waitFor(() => expect(where()).toBe('/performances/42'))
        expect(screen.queryByText(message)).not.toBeInTheDocument()
      }
    },
  )
})

// ---------- F1 요청 토큰의 정본 (Phase 4 1회차 RC-1) ----------

describe('TASK-024 F1 — 요청 토큰의 정본은 화면의 로그인 세션', () => {
  type Storage17 = 'setItem-throws' | 'quota' | 'set-remove-throw' | 'ok'
  const SC17: { row: string; storage: Storage17; then: string; expected: string | null; shows: 'organizer' | 'anon' }[] = [
    { row: 'a 저장 실패 · 이메일 로그인 → 등록', storage: 'setItem-throws', then: 'email-register', expected: 'Bearer tok-mem', shows: 'organizer' },
    { row: 'b 쿼터 초과 · 이메일 로그인', storage: 'quota', then: 'email', expected: 'Bearer tok-mem', shows: 'organizer' },
    { row: 'c 저장 실패 · 소셜 콜백', storage: 'setItem-throws', then: 'social', expected: 'Bearer tok-soc', shows: 'organizer' },
    { row: 'd 저장·삭제 실패 · 로그아웃', storage: 'set-remove-throw', then: 'logout', expected: null, shows: 'anon' },
    { row: 'e 다른 탭이 저장소를 다른 토큰으로', storage: 'ok', then: 'other-tab-set', expected: 'Bearer tok-org', shows: 'organizer' },
    { row: 'f 다른 탭이 저장소를 지움', storage: 'ok', then: 'other-tab-remove', expected: 'Bearer tok-org', shows: 'organizer' },
    { row: 'g 정상 로그아웃', storage: 'ok', then: 'logout', expected: null, shows: 'anon' },
  ]
  test.each(SC17)(
    'SC-17 (boundary) 저장소와 화면의 로그인 상태가 갈라져도 요청에는 화면의 로그인 상태가 실린다 — $row',
    async ({ storage, then, expected, shows }) => {
      // Given 표의 저장소 상태와 로그인 경로
      const throwOn = (method: 'setItem' | 'removeItem', error: Error) =>
        vi.spyOn(Storage.prototype, method).mockImplementation(() => {
          throw error
        })
      try {
        if (then === 'email-register' || then === 'email') {
          stubServer({ [LOGIN]: () => res(200, loginBody('tok-mem', ORG)) })
        } else if (then === 'social') {
          stubServer({ [ME]: () => res(200, ORG) })
        } else if (storage === 'set-remove-throw') {
          localStorage.setItem('accessToken', 'tok-old')
          stubServer({ [ME]: () => res(200, ORG) })
        } else {
          stubServer({ ...signedInAs(ORG, 'tok-org') })
        }
        if (storage === 'setItem-throws') throwOn('setItem', new Error('storage blocked'))
        if (storage === 'quota') throwOn('setItem', new DOMException('quota exceeded', 'QuotaExceededError'))
        if (storage === 'set-remove-throw') {
          throwOn('setItem', new Error('storage blocked'))
          throwOn('removeItem', new Error('storage blocked'))
        }

        // When 그다음
        if (then === 'email-register') {
          renderAt('/performances/new')
          await waitFor(() => expect(where()).toBe('/login'))
          await loginWith()
          await waitFor(() => expect(where()).toBe('/performances/new'))
          await screen.findByLabelText(/^공연명/)
          await doWrite('register')
          await waitFor(() => expect(callsTo(REGISTER)).toHaveLength(1))
        } else if (then === 'email') {
          renderAt('/login')
          await loginWith()
          await waitFor(() => expect(where()).toBe('/'))
        } else if (then === 'social') {
          renderAt('/oauth2/redirect?token=tok-soc')
          await waitFor(() => expect(where()).toBe('/'))
        } else {
          renderAt('/')
          const logoutButton = await screen.findByRole('button', { name: '로그아웃' })
          if (then === 'logout') fireEvent.click(logoutButton)
          // 다른 탭의 일 — 이 탭 화면을 거치지 않고 저장소만 바뀐다
          if (then === 'other-tab-set') localStorage.setItem('accessToken', 'tok-aud')
          if (then === 'other-tab-remove') localStorage.removeItem('accessToken')
        }
        // 행 a 는 등록 요청 자체, 나머지는 그 뒤 request()
        const sent = then === 'email-register' ? callsTo(REGISTER)[0] : (await request('/api/performances', { method: 'POST' }), calls[calls.length - 1])

        // Then 실린 Authorization
        const headers = headersOf(sent)
        if (expected === null) expect(headers.has('Authorization')).toBe(false)
        else expect(headers.get('Authorization')).toBe(expected)
        if (then === 'email-register') await waitFor(() => expect(where()).toBe('/performances/42'))
        // And 화면의 로그인 상태
        if (shows === 'organizer') {
          expect(screen.getByRole('button', { name: '로그아웃' })).toBeInTheDocument()
          expect(screen.getByText('주최자')).toBeInTheDocument()
        } else {
          expect(screen.getByRole('link', { name: '로그인' })).toBeInTheDocument()
        }
        if (storage === 'set-remove-throw') {
          vi.restoreAllMocks()
          expect(localStorage.getItem('accessToken')).toBe('tok-old')
        }
      } finally {
        vi.restoreAllMocks()
      }
    },
  )
})

// Phase 3 자체 리뷰 보강 (RC-1 later 변종) — 규약표 '요청 토큰의 정본': AuthProvider 밖에서는 저장소 값을 쓴다
describe('TASK-024 F1 — AuthProvider 가 내려간 뒤의 요청 토큰', () => {
  test('SC-17 보강 (boundary) AuthProvider 가 내려가면 세션이 끝나 요청은 저장소 값을 쓴다', async () => {
    // Given 주최자로 로그인한 화면(tok-mem — 저장에 실패해 세션에만 있다)
    stubServer({ [LOGIN]: () => res(200, loginBody('tok-mem', ORG)) })
    const spy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('storage blocked')
    })
    const view = renderAt('/login')
    await loginWith()
    await waitFor(() => expect(where()).toBe('/'))
    spy.mockRestore()

    // When AuthProvider 가 내려가고, 저장소에 tok-x 가 있을 때 요청을 보낸다
    view.unmount()
    localStorage.setItem('accessToken', 'tok-x')
    await request('/api/performances', { method: 'POST' })

    // Then 세션이 아니라 저장소 값이 실린다
    expect(headersOf(calls[calls.length - 1]).get('Authorization')).toBe('Bearer tok-x')
  })
})

// ---------- F4 쓰기 키 흔적 ----------

/** 저장소 루트 — vitest 의 URL 해석은 /@fs 접두를 붙이므로 문자열로 자른다 (task017.static.test.ts 관례) */
const REPO = decodeURIComponent(import.meta.url.replace('file://', '').split('/frontend/')[0])
const NODE_FS: string = 'node:fs'
const NODE_PATH: string = 'node:path'
const SELF = 'frontend/src/auth/task024.organizer.test.tsx'

describe('TASK-024 F4 — 쓰기 키 흔적', () => {
  test('SC-15 (happy) 쓰기 키 흔적이 소스와 환경 예시에 없다', async () => {
    // Given frontend/src/** (이 파일 제외) 와 frontend/.env.example
    const fs = (await import(/* @vite-ignore */ NODE_FS)) as {
      readdirSync: (p: string, o: { recursive: true }) => string[]
      readFileSync: (p: string, e: 'utf8') => string
      statSync: (p: string) => { isFile: () => boolean }
      existsSync: (p: string) => boolean
    }
    const path = (await import(/* @vite-ignore */ NODE_PATH)) as { join: (...p: string[]) => string }
    const srcRoot = path.join(REPO, 'frontend/src')
    const files = fs
      .readdirSync(srcRoot, { recursive: true })
      .map((rel) => `frontend/src/${rel}`)
      .filter((rel) => /\.(ts|tsx)$/.test(rel) && rel !== SELF)
      .filter((rel) => fs.statSync(path.join(REPO, rel)).isFile())
    const read = (rel: string) => fs.readFileSync(path.join(REPO, rel), 'utf8')
    const envExample = 'frontend/.env.example'
    const hits = (pattern: RegExp, targets: string[]) => targets.filter((rel) => pattern.test(read(rel)))

    // When / Then 세 이름은 어느 파일에도 없다(테스트 포함)
    expect(hits(/VITE_WRITE_KEY|WRITE_REJECTED_MESSAGE|isWriteKeyRejected/, [...files, envExample])).toEqual([])
    // And 테스트가 아닌 소스와 .env.example 에 X-Write-Key·WRITE_KEY_INVALID 가 없다
    const nonTest = files.filter((rel) => !/\.test\.(ts|tsx)$/.test(rel))
    expect(hits(/X-Write-Key|WRITE_KEY_INVALID/, [...nonTest, envExample])).toEqual([])
    // And WriteKeyRejected.test.tsx 가 없다
    expect(fs.existsSync(path.join(REPO, 'frontend/src/pages/WriteKeyRejected.test.tsx'))).toBe(false)
    // And 옛 쓰기 키 문구가 테스트가 아닌 소스에 없다
    expect(hits(new RegExp(MSG.oldRejected), nonTest)).toEqual([])
  })
})

// ---------- 공개 조회 회귀 ----------

describe('TASK-024 — 공개 조회 회귀', () => {
  const SC16 = [
    { row: 'a', path: '/', key: 'GET /api/performances' },
    { row: 'b', path: '/performances/3', key: 'GET /api/performances/3' },
  ]
  test.each(SC16)(
    'SC-16 (regression) 무효 토큰이 저장돼 있어도 공개 조회 화면은 지금처럼 열린다 — $row $path',
    async ({ path, key }) => {
      // Given 무효 토큰 tok-stale, /me 401, 공개 조회는 200
      localStorage.setItem('accessToken', 'tok-stale')
      stubServer({ [ME]: () => res(401, { code: 'UNAUTHORIZED' }) })

      // When 표의 주소를 연다
      renderAt(path)

      // Then 표의 화면
      expect(await screen.findByText('가을 재즈 콘서트')).toBeInTheDocument()
      await settle()
      // And 공연 조회는 그 주소의 것만, 오류 문구 없음
      expect(calls.filter((c) => c.key.startsWith('GET /api/performances')).map((c) => c.key)).toEqual([key])
      expect(screen.queryByRole('alert')).not.toBeInTheDocument()
      // And 전역 헤더는 비로그인 모양
      expect(within(document.body).getByRole('link', { name: '로그인' })).toBeInTheDocument()
    },
  )
})
