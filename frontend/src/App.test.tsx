import { render, screen } from '@testing-library/react'
import { ThemeProvider } from '@mui/material/styles'
import { MemoryRouter } from 'react-router-dom'
import { getPerformance } from './api/performances'
import App from './App'
import { AuthProvider } from './auth/AuthContext'
import { theme } from './theme'

vi.mock('./api/performances', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./api/performances')>()
  return { ...actual, getPerformance: vi.fn() }
})

describe('App', () => {
  beforeEach(() => {
    vi.mocked(getPerformance).mockReset()
  })
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  test('SC-10 (regression) 새 등록 라우트가 기존 상세 라우트를 가리지 않는다', async () => {
    // Given
    // App 에 /performances/new(신규)와 /performances/:id(기존)가 함께 등록되어 있다
    // 주최자로 로그인해 있다 — 등록 화면은 주최자만 그린다 (TASK-024 가드)
    localStorage.setItem('accessToken', 'tok-org')
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        new Response(JSON.stringify({ id: 1, email: 'org@ticket.on', name: '주최자', role: 'ORGANIZER', handle: 'org01' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )

    // When
    // /performances/new 로 진입한다
    render(
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={['/performances/new']}>
          <AuthProvider>
            <App />
          </AuthProvider>
        </MemoryRouter>
      </ThemeProvider>,
    )

    // Then
    // 공연명 입력란(라벨 "공연명")이 화면에 표시된다 — 등록 폼에만 있고 상세 화면에는 없는 요소다
    expect(await screen.findByLabelText(/^공연명/)).toBeInTheDocument()

    // GET /api/performances/new 요청이 발생하지 않는다(상세 페이지로 오인되지 않았다는 증거)
    expect(getPerformance).not.toHaveBeenCalled()
  })
})
