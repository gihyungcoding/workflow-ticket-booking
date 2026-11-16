import Box from '@mui/material/Box'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { AppBar, Hero, Page } from '../components/ui'
import { token } from '../theme/tokens'

// 흐름 3(대기열)이 이 화면을 교체한다 — 지금은 안내와 돌아가기뿐이다.
export function QueuePlaceholderPage() {
  const { id = '' } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const detail = `/performances/${encodeURIComponent(id)}`

  return (
    <>
      <AppBar plain onBack={() => navigate(detail)} />
      <Page plain>
        <Hero title="대기열을 준비하고 있어요" />
        <Box
          component={Link}
          to={detail}
          sx={{ color: token('--c-brand-text'), fontWeight: Number(token('--fw-strong')), fontSize: token('--fs-sub'), textDecoration: 'none' }}
        >
          공연으로 돌아가기
        </Box>
      </Page>
    </>
  )
}
