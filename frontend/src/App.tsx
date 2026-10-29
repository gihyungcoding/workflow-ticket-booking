import Container from '@mui/material/Container'
import { Route, Routes } from 'react-router-dom'
import { AppHeader } from './components/AppHeader'
import { LoginPage } from './pages/LoginPage'
import { OAuthRedirectPage } from './pages/OAuthRedirectPage'
import { PerformanceDetailPage } from './pages/PerformanceDetailPage'
import { PerformanceEditPage } from './pages/PerformanceEditPage'
import { PerformanceListPage } from './pages/PerformanceListPage'
import { PerformanceRegisterPage } from './pages/PerformanceRegisterPage'
import { RoleSelectionPage } from './pages/RoleSelectionPage'
import { SignUpPage } from './pages/SignUpPage'

function App() {
  return (
    <Container maxWidth="sm" sx={{ py: 4 }}>
      <AppHeader />
      <Routes>
        <Route path="/" element={<PerformanceListPage />} />
        <Route path="/performances/new" element={<PerformanceRegisterPage />} />
        <Route path="/performances/:id" element={<PerformanceDetailPage />} />
        <Route path="/performances/:id/edit" element={<PerformanceEditPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignUpPage />} />
        <Route path="/oauth2/redirect" element={<OAuthRedirectPage />} />
        <Route path="/oauth2/role" element={<RoleSelectionPage />} />
      </Routes>
    </Container>
  )
}

export default App
