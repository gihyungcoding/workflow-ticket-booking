import Container from '@mui/material/Container'
import { Route, Routes } from 'react-router-dom'
import { AppHeader } from './components/AppHeader'
import { LoginPage } from './pages/LoginPage'
import { PerformanceDetailPage } from './pages/PerformanceDetailPage'
import { PerformanceEditPage } from './pages/PerformanceEditPage'
import { PerformanceListPage } from './pages/PerformanceListPage'
import { PerformanceRegisterPage } from './pages/PerformanceRegisterPage'
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
      </Routes>
    </Container>
  )
}

export default App
