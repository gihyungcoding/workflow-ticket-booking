import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import FormControl from '@mui/material/FormControl'
import FormControlLabel from '@mui/material/FormControlLabel'
import FormHelperText from '@mui/material/FormHelperText'
import FormLabel from '@mui/material/FormLabel'
import Link from '@mui/material/Link'
import Radio from '@mui/material/Radio'
import RadioGroup from '@mui/material/RadioGroup'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { Link as RouterLink, Navigate, useLocation, useNavigate } from 'react-router-dom'
import {
  EMAIL_ALREADY_EXISTS,
  PENDING_SIGNUP_EXPIRED,
  SOCIAL_ACCOUNT_ALREADY_LINKED,
  completeSocialSignUp,
  readPendingSignup,
} from '../api/auth'
import type { UserRole } from '../api/auth'
import { ApiError, WRITE_REJECTED_MESSAGE, isWriteKeyRejected } from '../api/performances'
import { useAuth } from '../auth/AuthContext'
import { AUTH_MESSAGES } from '../auth/messages'

type ServerError = 'expired' | 'exists' | 'rejected' | 'failed' | null

// 콜백 페이지가 이동하면서 넘긴 pendingToken. 없거나 문자열이 아니면 이 화면은 열지 않는다.
function pendingTokenFrom(state: unknown): string | null {
  const value = (state as { pendingToken?: unknown } | null)?.pendingToken
  return typeof value === 'string' && value.trim() !== '' ? value : null
}

export function RoleSelectionPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const pendingToken = pendingTokenFrom(location.state)
  const [role, setRole] = useState<UserRole | null>(null)
  const [submitted, setSubmitted] = useState(false)
  const [serverError, setServerError] = useState<ServerError>(null)
  const [submitting, setSubmitting] = useState(false)
  // 화면을 떠난 뒤에 도착한 응답은 로그인·이동·오류 표시 어디에도 쓰지 않는다 (ADR-0018).
  const mounted = useRef(true)
  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])

  if (pendingToken === null) return <Navigate to="/login" replace />

  const pending = readPendingSignup(pendingToken)
  const roleError = submitted && role === null ? AUTH_MESSAGES.roleRequired : null

  async function submit() {
    setServerError(null)
    setSubmitted(true)
    if (role === null || pendingToken === null) return
    setSubmitting(true)
    try {
      const result = await completeSocialSignUp(pendingToken, role)
      if (!mounted.current) return
      auth.login(result.accessToken, result.user)
      navigate('/', { replace: true })
    } catch (error) {
      if (!mounted.current) return
      if (isWriteKeyRejected(error)) {
        setServerError('rejected')
      } else if (error instanceof ApiError && error.code === PENDING_SIGNUP_EXPIRED) {
        setServerError('expired')
      } else if (
        error instanceof ApiError &&
        (error.code === EMAIL_ALREADY_EXISTS || error.code === SOCIAL_ACCOUNT_ALREADY_LINKED)
      ) {
        setServerError('exists')
      } else {
        setServerError('failed')
      }
    } finally {
      if (mounted.current) setSubmitting(false)
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    void submit()
  }

  if (serverError === 'expired') {
    return (
      <Stack spacing={2}>
        <Typography variant="h5">거의 다 됐어요</Typography>
        <Alert severity="error">{AUTH_MESSAGES.pendingExpired}</Alert>
        <Button variant="contained" onClick={() => navigate('/login')}>
          로그인 화면으로
        </Button>
      </Stack>
    )
  }

  return (
    <Stack component="form" noValidate spacing={2} onSubmit={handleSubmit}>
      <Typography variant="h5">거의 다 됐어요</Typography>
      <Typography>
        {pending ? `${pending.name}님, ${pending.email} 으로 가입합니다` : '소셜 계정으로 가입합니다'}
      </Typography>
      {serverError === 'exists' && (
        <Alert severity="error">
          이미 가입된 계정입니다.{' '}
          <Link component={RouterLink} to="/login">
            로그인
          </Link>
          해 주세요
        </Alert>
      )}
      {serverError === 'rejected' && <Alert severity="error">{WRITE_REJECTED_MESSAGE}</Alert>}
      {serverError === 'failed' && <Alert severity="error">{AUTH_MESSAGES.completeFailed}</Alert>}
      <FormControl error={!!roleError}>
        <FormLabel id="social-role-label">역할</FormLabel>
        <RadioGroup
          row
          aria-labelledby="social-role-label"
          value={role ?? ''}
          onChange={(e) => setRole(e.target.value as UserRole)}
        >
          <FormControlLabel value="AUDIENCE" control={<Radio />} label="관객" />
          <FormControlLabel value="ORGANIZER" control={<Radio />} label="주최자" />
        </RadioGroup>
        {roleError && <FormHelperText>{roleError}</FormHelperText>}
      </FormControl>
      <Button type="submit" variant="contained" disabled={submitting}>
        계속하기
      </Button>
    </Stack>
  )
}
