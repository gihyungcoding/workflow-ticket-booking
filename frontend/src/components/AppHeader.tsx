import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { Link as RouterLink, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function AppHeader() {
  const { user, restoring, logout } = useAuth()
  const location = useLocation()
  const from = location.pathname + location.search + location.hash

  return (
    <Stack direction="row" spacing={1} sx={{ alignItems: 'center', justifyContent: 'flex-end', mb: 2, minHeight: 36 }}>
      {restoring ? null : user ? (
        <>
          <Typography variant="body2">{user.name}</Typography>
          <Button size="small" onClick={logout}>
            로그아웃
          </Button>
        </>
      ) : (
        <>
          <Button size="small" component={RouterLink} to="/login" state={{ from }}>
            로그인
          </Button>
          <Button size="small" component={RouterLink} to="/signup">
            가입
          </Button>
        </>
      )}
    </Stack>
  )
}
