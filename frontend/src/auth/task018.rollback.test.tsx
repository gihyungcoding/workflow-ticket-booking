import { act, cleanup, render, screen, within } from '@testing-library/react'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter } from 'react-router-dom'
import App from '../App'
import { theme } from '../theme'
import { componentSizes } from '../theme/tokens'
import { AuthProvider } from './AuthContext'
import authPartsSource from './AuthParts.tsx?raw'

// TASK-018 Phase 4 1회차 FAIL 의 뿌리·변종 (VERIFY_TASK-018.json root_causes RC-2·RC-3).
// 변종마다 실제 라우트(App + MemoryRouter)로 그려 확인한다. 시안 값의 출처는 out/components.css·out/screens/*.html.

// 시안 .title letter-spacing -0.03em — jsdom 은 em 을 px 로 계산한다 (--fs-title 28px × -0.03)
const TITLE_TRACKING_PX = '-0.84px'

const USER = { id: 1, email: 'harin.kim@kakao.com', name: '김하린', role: 'AUDIENCE', handle: 'wave7302' }

function json(status: number, body?: unknown) {
  return new Response(body === undefined ? '' : JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function stubServer(routes: Record<string, () => Response | Promise<Response>> = {}) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL, init: RequestInit = {}) => {
      const key = `${(init.method ?? 'GET').toUpperCase()} ${String(input).split('?')[0]}`
      const table: Record<string, () => Response | Promise<Response>> = {
        'GET /api/performances': () => json(200, { content: [], page: 0, size: 20, totalElements: 0 }),
        'GET /api/auth/me': () => json(200, USER),
        'POST /api/auth/social/pending': () =>
          json(200, { provider: 'KAKAO', email: 'harin.kim@kakao.com', name: '김하린', expiresAt: new Date(Date.now() + 300_000).toISOString() }),
        ...routes,
      }
      return (table[key] ?? (() => json(404, { code: 'NOT_FOUND' })))()
    }),
  )
}

function renderAt(entry: string | { pathname: string; state?: unknown }) {
  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[entry]}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </MemoryRouter>
    </ThemeProvider>,
  )
}

const flush = () =>
  act(async () => {
    await new Promise((r) => setTimeout(r, 30))
  })

beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()
})
afterEach(() => {
  cleanup()
  vi.useRealTimers()
  vi.unstubAllGlobals()
  localStorage.clear()
})

describe('TASK-018 RC-2 인증 화면 판별은 라우터 매칭과 같다', () => {
  test.each(['/login/', '/Login', '/LOGIN', '/signup/', '/Signup', '/oauth2/role/', '/oauth2/redirect/', '/joined/', '/Joined'])(
    '%s 는 인증 화면이라 전역 헤더가 없다',
    async (path) => {
      stubServer()
      renderAt(path)
      await flush()
      expect(screen.queryByRole('link', { name: '가입' })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: '로그아웃' })).not.toBeInTheDocument()
    },
  )

  test.each(['/', '/performances/new'])('%s 는 인증 화면이 아니라 헤더가 있다', async (path) => {
    stubServer()
    renderAt(path)
    expect(await screen.findByRole('link', { name: '가입' })).toBeInTheDocument()
  })
})

describe('TASK-018 RC-3 시안과 같은 모양', () => {
  test('D1 흰 바탕 화면의 앱바는 흰색이다 (시안 .plain .appbar)', async () => {
    stubServer()
    renderAt('/signup')
    const bar = screen.getByRole('button', { name: '뒤로' }).closest('header')!
    const page = screen.getByRole('main')
    // 앱바는 아래 흰 바탕(--c-surface)과 같은 색이고, 회색 페이지 바탕(--c-bg)이 아니다
    expect(getComputedStyle(bar).backgroundColor).toBe(getComputedStyle(page).backgroundColor)
    expect(getComputedStyle(bar).backgroundColor).not.toBe(getComputedStyle(document.body).backgroundColor || 'transparent')
  })

  test.each(['/login', '/signup'])('D2 머리글 제목의 자간은 -0.03em 이다 (시안 .title) — %s', async (path) => {
    stubServer()
    renderAt(path)
    const title = screen.getAllByRole('heading', { level: 1 }).find((h) => h.textContent)!
    expect(getComputedStyle(title).letterSpacing).toBe(TITLE_TRACKING_PX)
  })

  test('D2 콜백 화면 제목의 자간도 -0.03em 이다', async () => {
    stubServer({ 'GET /api/auth/me': () => new Promise<Response>(() => {}) })
    renderAt('/oauth2/redirect?token=tok-1')
    expect(getComputedStyle(await screen.findByRole('heading', { name: '소셜 계정을 확인하고 있어요' })).letterSpacing).toBe(TITLE_TRACKING_PX)
  })

  test('D3 역할 행 일러스트는 36px 이다 (시안 .row-art img)', async () => {
    stubServer()
    renderAt({ pathname: '/oauth2/role', state: { pendingToken: 'pend-1' } })
    const row = await screen.findByRole('radio', { name: /^관객/ })
    const img = row.querySelector('img')!
    expect(getComputedStyle(img).width).toBe('36px')
    expect(getComputedStyle(img).height).toBe('36px')
  })

  test('D4 화면에 머물다 만료되면 윗줄·이메일·역할 라벨·오류 아이콘이 남는다 (시안 role-select--error)', async () => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout', 'Date'] })
    vi.setSystemTime(new Date('2026-11-11T08:35:00Z'))
    stubServer({
      'POST /api/auth/social/pending': () =>
        json(200, { provider: 'KAKAO', email: 'harin.kim@kakao.com', name: '김하린', expiresAt: '2026-11-11T08:40:00Z' }),
    })
    renderAt({ pathname: '/oauth2/role', state: { pendingToken: 'pend-1' } })
    for (let i = 0; i < 5; i++) await act(() => vi.advanceTimersByTimeAsync(0))
    await act(() => vi.advanceTimersByTimeAsync(300_000))

    expect(screen.getByRole('heading', { name: '가입 시간이 지났어요' })).toBeInTheDocument()
    expect(screen.getByText('카카오로 처음 가입')).toBeInTheDocument()
    expect(screen.getByText('harin.kim@kakao.com(김하린)')).toBeInTheDocument()
    expect(screen.getByText('역할')).toBeInTheDocument()
    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent('5분이 지나 역할을 고를 수 없어요. 다시 로그인해 주세요')
    expect(alert.querySelector('svg')).not.toBeNull()
  })

  test('D4 서버가 처음부터 만료라고 하면 대기 정보를 모르므로 윗줄 없이 같은 안내만', async () => {
    stubServer({ 'POST /api/auth/social/pending': () => json(401, { code: 'PENDING_SIGNUP_EXPIRED' }) })
    renderAt({ pathname: '/oauth2/role', state: { pendingToken: 'pend-1' } })
    expect(await screen.findByRole('heading', { name: '가입 시간이 지났어요' })).toBeInTheDocument()
    expect(screen.queryByText('카카오로 처음 가입')).not.toBeInTheDocument()
    expect(within(screen.getByRole('alert')).getByText('5분이 지나 역할을 고를 수 없어요. 다시 로그인해 주세요')).toBeInTheDocument()
  })

  test('D5 화살표 링크의 치수는 리터럴이 아니라 componentSizes 에서 온다', () => {
    expect(authPartsSource).not.toMatch(/width="18"|height="18"|gap: '2px'/)
    expect(componentSizes).toHaveProperty('moreIcon', '18px')
  })

  test('D6 아이디 글자는 시안 .id-value 의 줄 높이 1.15 다', async () => {
    localStorage.setItem('accessToken', 'tok-1')
    stubServer()
    renderAt({ pathname: '/joined', state: { via: 'email' } })
    expect(getComputedStyle(await screen.findByText('wave7302')).lineHeight).toBe('1.15')
  })

  test('D7 콜백 화면의 진행 표시는 시안 .wait-spin 의 32px 이다', async () => {
    stubServer({ 'GET /api/auth/me': () => new Promise<Response>(() => {}) })
    renderAt('/oauth2/redirect?token=tok-1')
    const spinner = await screen.findByRole('progressbar')
    expect(getComputedStyle(spinner).width).toBe('32px')
  })
})
