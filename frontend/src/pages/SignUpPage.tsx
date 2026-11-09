import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import Box from '@mui/material/Box'
import { useNavigate } from 'react-router-dom'
import { EMAIL_ALREADY_EXISTS, login, signUp } from '../api/auth'
import type { UserRole } from '../api/auth'
import { ApiError, WRITE_REJECTED_MESSAGE, isWriteKeyRejected } from '../api/performances'
import { useAuth } from '../auth/AuthContext'
import { LinkLine, MoreLink, TopNotice } from '../auth/AuthParts'
import { AUTH_MESSAGES } from '../auth/messages'
import { AppBar, CtaBar, Field, Hero, Page, Segmented } from '../components/ui'
import { token } from '../theme/tokens'

const EMAIL_MAX = 255
const PASSWORD_MIN = 8
const PASSWORD_MAX = 72
const NAME_MAX = 100
const FORM_ID = 'signup-form'

const ROLE_OPTIONS = [
  { value: 'AUDIENCE', label: '관객' },
  { value: 'ORGANIZER', label: '주최자' },
] as const satisfies readonly { value: UserRole; label: string }[]

interface FieldErrors {
  email: string | null
  password: string | null
  confirm: string | null
  name: string | null
  role: string | null
}

type ServerError = 'duplicate' | 'rejected' | 'failed' | null

// 길이는 서버 @Size 와 같게 UTF-16 코드 유닛 수(string.length)로 센다. 입력은 변환하지 않는다 (ADR-0019).
// 이메일만 앞뒤 공백을 걷고, 길이도 걷은 값으로 센다.
function validate(email: string, password: string, confirm: string, name: string, role: UserRole | null): FieldErrors {
  const trimmedEmail = email.trim()
  return {
    email: trimmedEmail === '' ? AUTH_MESSAGES.emailRequired : trimmedEmail.length > EMAIL_MAX ? AUTH_MESSAGES.emailMax : null,
    // 길이 위반은 안내 문구 자리가 그대로 오류로 바뀐다 (시안 정책)
    password:
      password.trim() === ''
        ? AUTH_MESSAGES.passwordRequired
        : password.length < PASSWORD_MIN || password.length > PASSWORD_MAX
          ? AUTH_MESSAGES.passwordHelp
          : null,
    confirm: confirm === '' ? AUTH_MESSAGES.confirmRequired : confirm !== password ? AUTH_MESSAGES.mismatch : null,
    name: name.trim() === '' ? AUTH_MESSAGES.nameRequired : name.length > NAME_MAX ? AUTH_MESSAGES.nameMax : null,
    role: role === null ? AUTH_MESSAGES.roleRequired : null,
  }
}

export function SignUpPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [name, setName] = useState('')
  const [role, setRole] = useState<UserRole | null>(null)
  const [submitted, setSubmitted] = useState(false)
  const [serverError, setServerError] = useState<ServerError>(null)
  const [submitting, setSubmitting] = useState(false)
  const inFlight = useRef(false)
  // 화면을 떠난 뒤에 도착한 응답은 자동 로그인·이동·오류 표시 어디에도 쓰지 않는다 (ADR-0018)
  const mounted = useRef(true)
  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])

  // 제출을 시도한 뒤에는 입력할 때마다 다시 계산한다 — 불일치를 고치면 바로 사라진다.
  const errors = validate(email, password, confirm, name, role)
  const shown = (field: keyof FieldErrors) => (submitted ? errors[field] : null)

  async function submit() {
    if (inFlight.current) return
    setServerError(null)
    setSubmitted(true)
    if (Object.values(errors).some(Boolean) || role === null) return
    inFlight.current = true
    setSubmitting(true)
    const trimmedEmail = email.trim()
    try {
      await signUp({ email: trimmedEmail, password, passwordConfirm: confirm, name, role })
    } catch (error) {
      inFlight.current = false
      if (!mounted.current) return
      setSubmitting(false)
      if (isWriteKeyRejected(error)) {
        setServerError('rejected')
      } else if (error instanceof ApiError && error.code === EMAIL_ALREADY_EXISTS) {
        setServerError('duplicate')
      } else {
        setServerError('failed')
      }
      return
    }
    if (!mounted.current) {
      inFlight.current = false
      return
    }
    // 가입하면 바로 시작 — 같은 자격 증명으로 로그인한다. 계정은 이미 있으므로 실패하면 로그인 화면에서 안내한다
    try {
      const result = await login({ email: trimmedEmail, password })
      if (!mounted.current) return
      auth.login(result.accessToken, result.user)
      navigate('/joined', { replace: true, state: { via: 'email' } })
    } catch {
      if (!mounted.current) return
      navigate('/login', { replace: true, state: { signedUpEmail: trimmedEmail } })
    } finally {
      inFlight.current = false
    }
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    void submit()
  }

  const duplicate = serverError === 'duplicate'

  return (
    <>
      <AppBar plain onBack={() => navigate('/login')} />
      <Page plain hasCta>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-m') }}>
          {serverError === 'rejected' && <TopNotice>{WRITE_REJECTED_MESSAGE}</TopNotice>}
          {serverError === 'failed' && <TopNotice>{AUTH_MESSAGES.signUpFailed}</TopNotice>}
          <Hero title="이메일로 가입" lead="이메일과 역할은 가입한 뒤에 바꿀 수 없어요" />
        </Box>
        <Box component="form" id={FORM_ID} noValidate onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-m') }}>
          <Field
            label="이메일"
            type="email"
            autoFocus
            autoComplete="username"
            placeholder="이메일 주소"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={duplicate ? AUTH_MESSAGES.duplicateEmail : shown('email')}
            errorAction={duplicate ? <MoreLink to="/login">로그인하기</MoreLink> : null}
          />
          <Field
            label="비밀번호"
            type="password"
            autoComplete="new-password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            help={AUTH_MESSAGES.passwordHelp}
            error={shown('password')}
          />
          <Field
            label="비밀번호 확인"
            type="password"
            autoComplete="new-password"
            placeholder="한 번 더 입력"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            error={shown('confirm')}
          />
          <Field
            label="이름"
            autoComplete="name"
            placeholder="화면에 보일 이름"
            value={name}
            onChange={(e) => setName(e.target.value)}
            error={shown('name')}
          />
          <Segmented label="역할" options={ROLE_OPTIONS} value={role} onChange={setRole} error={shown('role')} />
        </Box>
        {/* 이메일 칸 안의 로그인 링크와 같은 목적이라 중복일 때는 뺀다 (시안 signup--error) */}
        {!duplicate && (
          <LinkLine>
            이미 계정이 있나요?
            <MoreLink to="/login">로그인</MoreLink>
          </LinkLine>
        )}
      </Page>
      <CtaBar form={FORM_ID} disabled={submitting}>
        가입하기
      </CtaBar>
    </>
  )
}
