import Container from '@mui/material/Container'
import { Route, Routes } from 'react-router-dom'
import { PerformanceDetailPage } from './pages/PerformanceDetailPage'
import { PerformanceListPage } from './pages/PerformanceListPage'

function App() {
  return (
    <Container maxWidth="sm" sx={{ py: 4 }}>
      <Routes>
        <Route path="/" element={<PerformanceListPage />} />
        <Route path="/performances/:id" element={<PerformanceDetailPage />} />
      </Routes>
    </Container>
  )
}

export default App
