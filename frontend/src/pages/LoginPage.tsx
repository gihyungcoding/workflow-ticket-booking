import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import Box from '@mui/material/Box'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { INVALID_CREDENTIALS, login } from '../api/auth'
import { ApiError } from '../api/performances'
import { useAuth } from '../auth/AuthContext'
import { isOneOf } from '../auth/authPaths'
import { LinkLine, MoreLink, OrDivider, PrimaryButton, TopNotice } from '../auth/AuthParts'
import { AUTH_MESSAGES } from '../auth/messages'
import { SocialLoginButtons } from '../components/SocialLoginButtons'
import { Field, Hero, Page } from '../components/ui'
import { token } from '../theme/tokens'

// 앱 안의 경로만 믿는다 — 외부 주소·프로토콜 상대 주소·스킴·로그인/가입 순환은 홈으로.
function safeDestination(from: unknown): string {
  if (typeof from !== 'string' || !from.startsWith('/') || from.startsWith('//') || from.includes('\\')) {
    return '/'
  }
  // 로그인·가입으로 되돌아가는 순환 — 라우터와 같은 규칙으로 본다 (/login/·/Login 도 같은 화면)
  return isOneOf(['/login', '/signup'], from.split(/[?#]/)[0]) ? '/' : from
}

type ServerError = 'credentials' | 'failed' | null

interface LoginState {
  from?: unknown
  // 가입은 됐는데 자동 로그인을 못 한 경우 — 가입 화면이 이메일을 넘긴다
  signedUpEmail?: unknown
}

export function LoginPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const state = (location.state ?? null) as LoginState | null
  const signedUpEmail = typeof state?.signedUpEmail === 'string' ? state.signedUpEmail : null
  const [email, setEmail] = useState(signedUpEmail ?? '')
  const [password, setPassword] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [serverError, setServerError] = useState<ServerError>(null)
  const [submitting, setSubmitting] = useState(false)
  // 맨 위 안내(소셜 실패·가입 뒤 로그인)는 다시 제출하면 사라진다
  const [notice, setNotice] = useState<'social' | 'joined' | null>(
    signedUpEmail !== null ? 'joined' : searchParams.get('error') === 'social' ? 'social' : null,
  )
  // 렌더 사이 두 번 제출되는 것(Enter 연타)을 막는다 — 상태는 다음 렌더에야 보인다
  const inFlight = useRef(false)
  // 화면을 떠난 뒤에 도착한 응답은 로그인·이동·오류 표시 어디에도 쓰지 않는다 (ADR-0018)
  const mounted = useRef(true)
  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])

  // 로그인은 길이 상한을 검증하지 않는다 — 판정은 서버가 한다.
  const emailError = email.trim() === '' ? AUTH_MESSAGES.emailRequired : null
  const passwordError = password === '' ? AUTH_MESSAGES.passwordRequired : null

  async function submit() {
    if (inFlight.current) return
    setServerError(null)
    setNotice(null)
    setSubmitted(true)
    if (emailError || passwordError) return
    inFlight.current = true
    setSubmitting(true)
    try {
      const result = await login({ email: email.trim(), password })
      if (!mounted.current) return
      auth.login(result.accessToken, result.user)
      navigate(safeDestination(state?.from), { replace: true })
    } catch (error) {
      if (!mounted.current) return
      if (error instanceof ApiError && error.code === INVALID_CREDENTIALS) {
        setServerError('credentials')
      } else {
        setServerError('failed')
      }
    } finally {
      inFlight.current = false
      if (mounted.current) setSubmitting(false)
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    void submit()
  }

  // 401 은 어느 칸이 틀렸는지 알려 주지 않는다 — 두 칸을 함께 오류로, 문구는 비밀번호 칸 아래 하나
  const credentials = serverError === 'credentials'

  return (
    <Page plain>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-m') }}>
        {notice === 'social' && <TopNotice>{AUTH_MESSAGES.socialFailed}</TopNotice>}
        {notice === 'joined' && <TopNotice severity="info">{AUTH_MESSAGES.joinedThenLogin}</TopNotice>}
        {serverError === 'failed' && <TopNotice>{AUTH_MESSAGES.loginFailed}</TopNotice>}
        <Hero eyebrow="티켓온" title="쓰던 계정으로 바로 들어와요" lead="카카오·네이버·구글·애플 계정이면 처음이어도 버튼 한 번으로 시작해요" />
      </Box>
      <Box component="form" noValidate onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-m') }}>
        <Field
          label="이메일"
          type="email"
          autoFocus
          autoComplete="username"
          placeholder="이메일 주소"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          aria-invalid={credentials || undefined}
          error={submitted ? emailError : null}
        />
        <Field
          label="비밀번호"
          type="password"
          autoComplete="current-password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={credentials ? AUTH_MESSAGES.badCredentials : submitted ? passwordError : null}
        />
        <PrimaryButton disabled={submitting}>로그인하기</PrimaryButton>
      </Box>
      <OrDivider />
      <SocialLoginButtons />
      <LinkLine>
        계정이 없나요?
        <MoreLink to="/signup">이메일로 가입</MoreLink>
      </LinkLine>
    </Page>
  )
}
