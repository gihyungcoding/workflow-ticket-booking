import { useState } from 'react'
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
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import { EMAIL_ALREADY_EXISTS, signUp } from '../api/auth'
import type { UserRole } from '../api/auth'
import { ApiError, WRITE_REJECTED_MESSAGE, isWriteKeyRejected } from '../api/performances'
import { AUTH_MESSAGES } from '../auth/messages'

const EMAIL_MAX = 255
const PASSWORD_MIN = 8
const PASSWORD_MAX = 72
const NAME_MAX = 100

const MISMATCH = '비밀번호가 일치하지 않습니다'

interface FieldErrors {
  email: string | null
  password: string | null
  confirm: string | null
  name: string | null
  role: string | null
}

type ServerError = 'duplicate' | 'rejected' | 'failed' | null

// 길이는 서버 @Size 와 같게 UTF-16 코드 유닛 수(string.length)로 센다. 입력은 변환하지 않는다.
function validate(email: string, password: string, confirm: string, name: string, role: UserRole | null): FieldErrors {
  const trimmedEmail = email.trim()
  return {
    email:
      trimmedEmail === ''
        ? AUTH_MESSAGES.emailRequired
        : trimmedEmail.length > EMAIL_MAX
          ? `이메일은 ${EMAIL_MAX}자 이하여야 합니다`
          : null,
    password:
      password.trim() === ''
        ? AUTH_MESSAGES.passwordRequired
        : password.length < PASSWORD_MIN
          ? `비밀번호는 ${PASSWORD_MIN}자 이상이어야 합니다`
          : password.length > PASSWORD_MAX
            ? `비밀번호는 ${PASSWORD_MAX}자 이하여야 합니다`
            : null,
    confirm: confirm === '' ? '비밀번호를 한 번 더 입력해 주세요' : confirm !== password ? MISMATCH : null,
    name:
      name.trim() === ''
        ? '이름을 입력해 주세요'
        : name.length > NAME_MAX
          ? `이름은 ${NAME_MAX}자 이하여야 합니다`
          : null,
    role: role === null ? '역할을 선택해 주세요' : null,
  }
}

export function SignUpPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [name, setName] = useState('')
  const [role, setRole] = useState<UserRole | null>(null)
  const [submitted, setSubmitted] = useState(false)
  const [serverError, setServerError] = useState<ServerError>(null)
  const [submitting, setSubmitting] = useState(false)

  // 제출을 시도한 뒤에는 입력할 때마다 다시 계산한다 — 불일치를 고치면 바로 사라진다.
  const errors = validate(email, password, confirm, name, role)
  const shown = (field: keyof FieldErrors) => (submitted ? errors[field] : null)

  async function submit() {
    setServerError(null)
    setSubmitted(true)
    if (Object.values(errors).some(Boolean) || role === null) return
    setSubmitting(true)
    try {
      await signUp({ email: email.trim(), password, passwordConfirm: confirm, name, role })
      navigate('/login')
    } catch (error) {
      if (isWriteKeyRejected(error)) {
        setServerError('rejected')
      } else if (error instanceof ApiError && error.code === EMAIL_ALREADY_EXISTS) {
        setServerError('duplicate')
      } else {
        setServerError('failed')
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
      <Typography variant="h5">회원가입</Typography>
      {serverError === 'duplicate' && (
        <Alert severity="error">
          이미 가입된 이메일입니다.{' '}
          <Link component={RouterLink} to="/login">
            로그인
          </Link>
          해 주세요
        </Alert>
      )}
      {serverError === 'rejected' && <Alert severity="error">{WRITE_REJECTED_MESSAGE}</Alert>}
      {serverError === 'failed' && <Alert severity="error">{AUTH_MESSAGES.signUpFailed}</Alert>}
      <TextField
        label="이메일"
        autoFocus
        autoComplete="username"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        error={!!shown('email')}
        helperText={shown('email')}
      />
      <TextField
        label="비밀번호"
        type="password"
        autoComplete="new-password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        error={!!shown('password')}
        helperText={shown('password')}
      />
      <TextField
        label="비밀번호 확인"
        type="password"
        autoComplete="new-password"
        value={confirm}
        onChange={(e) => setConfirm(e.target.value)}
        error={!!shown('confirm')}
        helperText={shown('confirm')}
      />
      <TextField
        label="이름"
        autoComplete="name"
        value={name}
        onChange={(e) => setName(e.target.value)}
        error={!!shown('name')}
        helperText={shown('name')}
      />
      <FormControl error={!!shown('role')}>
        <FormLabel id="signup-role-label">역할</FormLabel>
        <RadioGroup
          row
          aria-labelledby="signup-role-label"
          value={role ?? ''}
          onChange={(e) => setRole(e.target.value as UserRole)}
        >
          <FormControlLabel value="AUDIENCE" control={<Radio />} label="관객" />
          <FormControlLabel value="ORGANIZER" control={<Radio />} label="주최자" />
        </RadioGroup>
        {shown('role') && <FormHelperText>{shown('role')}</FormHelperText>}
      </FormControl>
      <Button type="submit" variant="contained" disabled={submitting}>
        가입하기
      </Button>
    </Stack>
  )
}
