import Container from '@mui/material/Container'
import { Route, Routes } from 'react-router-dom'
import { PerformanceDetailPage } from './pages/PerformanceDetailPage'
import { PerformanceEditPage } from './pages/PerformanceEditPage'
import { PerformanceListPage } from './pages/PerformanceListPage'
import { PerformanceRegisterPage } from './pages/PerformanceRegisterPage'

function App() {
  return (
    <Container maxWidth="sm" sx={{ py: 4 }}>
      <Routes>
        <Route path="/" element={<PerformanceListPage />} />
        <Route path="/performances/new" element={<PerformanceRegisterPage />} />
        <Route path="/performances/:id" element={<PerformanceDetailPage />} />
        <Route path="/performances/:id/edit" element={<PerformanceEditPage />} />
      </Routes>
    </Container>
  )
}

export default App
