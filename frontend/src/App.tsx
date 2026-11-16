import Container from '@mui/material/Container'
import { matchRoutes, Route, Routes, useLocation } from 'react-router-dom'
import { AUTH_PATHS, isOneOf } from './auth/authPaths'
import { RequireOrganizer } from './auth/RequireOrganizer'
import { AppHeader } from './components/AppHeader'
import { JoinedPage } from './pages/JoinedPage'
import { LoginPage } from './pages/LoginPage'
import { OAuthRedirectPage } from './pages/OAuthRedirectPage'
import { PerformanceDetailPage } from './pages/PerformanceDetailPage'
import { PerformanceEditPage } from './pages/PerformanceEditPage'
import { PerformanceListPage } from './pages/PerformanceListPage'
import { PerformanceRegisterPage } from './pages/PerformanceRegisterPage'
import { QueuePlaceholderPage } from './pages/QueuePlaceholderPage'
import { RoleSelectionPage } from './pages/RoleSelectionPage'
import { SignUpPage } from './pages/SignUpPage'

// 인증 화면은 시안대로 한 장 전체를 쓴다 — 전역 헤더(로그인·가입·로그아웃)를 그리지 않는다 (TASK-018 사용자 결정)
// 공연 상세와 대기열 임시 화면도 시안대로 한 장을 쓴다 (TASK-020 사용자 결정). 판별은 라우터가 화면을 고르는 순위와 같아야 한다 —
// `/performances/:id` 단독으로 맞추면 `/performances/new` 도 잡히므로 형제 경로와 함께 매칭해 가장 위에 오른 것을 본다.
const PAGE_ROUTES = [
  { path: '/performances/new' },
  { path: '/performances/:id/edit' },
  { path: '/performances/:id' },
  { path: '/performances/:id/queue' },
]
const FULL_PAGE_PATHS: Array<string | undefined> = ['/performances/:id', '/performances/:id/queue']
function isFullPage(pathname: string): boolean {
  return FULL_PAGE_PATHS.includes(matchRoutes(PAGE_ROUTES, pathname)?.[0].route.path)
}

function App() {
  const { pathname } = useLocation()
  const routes = (
    <Routes>
      <Route path="/" element={<PerformanceListPage />} />
      <Route path="/performances/new" element={<RequireOrganizer><PerformanceRegisterPage /></RequireOrganizer>} />
      <Route path="/performances/:id" element={<PerformanceDetailPage />} />
      <Route path="/performances/:id/queue" element={<QueuePlaceholderPage />} />
      <Route path="/performances/:id/edit" element={<RequireOrganizer><PerformanceEditPage /></RequireOrganizer>} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignUpPage />} />
      <Route path="/oauth2/redirect" element={<OAuthRedirectPage />} />
      <Route path="/oauth2/role" element={<RoleSelectionPage />} />
      <Route path="/joined" element={<JoinedPage />} />
    </Routes>
  )
  if (isOneOf(AUTH_PATHS, pathname) || isFullPage(pathname)) return routes
  return (
    <Container maxWidth="sm" sx={{ py: 4 }}>
      <AppHeader />
      {routes}
    </Container>
  )
}

export default App
