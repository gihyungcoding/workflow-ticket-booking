import { useState } from 'react'
import type { FormEvent } from 'react'
import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { useLocation, useNavigate } from 'react-router-dom'
import { INVALID_CREDENTIALS, login } from '../api/auth'
import { ApiError, WRITE_REJECTED_MESSAGE, isWriteKeyRejected } from '../api/performances'
import { useAuth } from '../auth/AuthContext'
import { AUTH_MESSAGES } from '../auth/messages'

// 앱 안의 경로만 믿는다 — 외부 주소·프로토콜 상대 주소·스킴·로그인/가입 순환은 홈으로.
function safeDestination(from: unknown): string {
  if (typeof from !== 'string' || !from.startsWith('/') || from.startsWith('//') || from.includes('\\')) {
    return '/'
  }
  const pathname = from.split(/[?#]/)[0]
  return pathname === '/login' || pathname === '/signup' ? '/' : from
}

export function LoginPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [serverError, setServerError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // 로그인은 길이 상한을 검증하지 않는다 — 판정은 서버가 한다.
  const emailError = email.trim() === '' ? AUTH_MESSAGES.emailRequired : null
  const passwordError = password === '' ? AUTH_MESSAGES.passwordRequired : null

  async function submit() {
    setServerError(null)
    setSubmitted(true)
    if (emailError || passwordError) return
    setSubmitting(true)
    try {
      const result = await login({ email: email.trim(), password })
      auth.login(result.accessToken, result.user)
      navigate(safeDestination((location.state as { from?: unknown } | null)?.from), { replace: true })
    } catch (error) {
      if (isWriteKeyRejected(error)) {
        setServerError(WRITE_REJECTED_MESSAGE)
      } else if (error instanceof ApiError && error.code === INVALID_CREDENTIALS) {
        setServerError(AUTH_MESSAGES.badCredentials)
      } else {
        setServerError(AUTH_MESSAGES.loginFailed)
      }
    } finally {
      setSubmitting(false)
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    void submit()
  }

  return (
    <Stack component="form" noValidate spacing={2} onSubmit={handleSubmit}>
      <Typography variant="h5">로그인</Typography>
      {serverError && <Alert severity="error">{serverError}</Alert>}
      <TextField
        label="이메일"
        autoFocus
        autoComplete="username"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        error={submitted && !!emailError}
        helperText={submitted ? emailError : null}
      />
      <TextField
        label="비밀번호"
        type="password"
        autoComplete="current-password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        error={submitted && !!passwordError}
        helperText={submitted ? passwordError : null}
      />
      <Button type="submit" variant="contained" disabled={submitting}>
        로그인
      </Button>
    </Stack>
  )
}
