import Container from '@mui/material/Container'
import { Route, Routes, matchPath, useLocation } from 'react-router-dom'
import { AppHeader } from './components/AppHeader'
import { JoinedPage } from './pages/JoinedPage'
import { LoginPage } from './pages/LoginPage'
import { OAuthRedirectPage } from './pages/OAuthRedirectPage'
import { PerformanceDetailPage } from './pages/PerformanceDetailPage'
import { PerformanceEditPage } from './pages/PerformanceEditPage'
import { PerformanceListPage } from './pages/PerformanceListPage'
import { PerformanceRegisterPage } from './pages/PerformanceRegisterPage'
import { RoleSelectionPage } from './pages/RoleSelectionPage'
import { SignUpPage } from './pages/SignUpPage'

// 인증 화면은 시안대로 한 장 전체를 쓴다 — 전역 헤더(로그인·가입·로그아웃)를 그리지 않는다 (TASK-018 사용자 결정)
// 판별은 라우터와 같은 규칙(matchPath — 끝 슬래시·대소문자 무시)으로 한다. 문자열 정확 일치는 /login/·/Login 을 놓쳤다
const AUTH_PATHS = ['/login', '/signup', '/oauth2/redirect', '/oauth2/role', '/joined']

function App() {
  const { pathname } = useLocation()
  const routes = (
    <Routes>
      <Route path="/" element={<PerformanceListPage />} />
      <Route path="/performances/new" element={<PerformanceRegisterPage />} />
      <Route path="/performances/:id" element={<PerformanceDetailPage />} />
      <Route path="/performances/:id/edit" element={<PerformanceEditPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignUpPage />} />
      <Route path="/oauth2/redirect" element={<OAuthRedirectPage />} />
      <Route path="/oauth2/role" element={<RoleSelectionPage />} />
      <Route path="/joined" element={<JoinedPage />} />
    </Routes>
  )
  if (AUTH_PATHS.some((path) => matchPath(path, pathname))) return routes
  return (
    <Container maxWidth="sm" sx={{ py: 4 }}>
      <AppHeader />
      {routes}
    </Container>
  )
}

export default App
