import { useEffect, useId, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import Box from '@mui/material/Box'
import CircularProgress from '@mui/material/CircularProgress'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import {
  EMAIL_ALREADY_EXISTS,
  PENDING_SIGNUP_EXPIRED,
  SOCIAL_ACCOUNT_ALREADY_LINKED,
  completeSocialSignUp,
  fetchPendingSignup,
} from '../api/auth'
import type { PendingSignup, UserRole } from '../api/auth'
import { ApiError, WRITE_REJECTED_MESSAGE, isWriteKeyRejected } from '../api/performances'
import { useAuth } from '../auth/AuthContext'
import { MoreLink, TopNotice } from '../auth/AuthParts'
import { AUTH_MESSAGES } from '../auth/messages'
import { providerNameOf } from '../auth/socialProvider'
import clipboardArt from '../assets/fluent/clipboard.svg'
import ticketArt from '../assets/fluent/ticket.svg'
import { AppBar, CtaBar, Hero, Info, Page } from '../components/ui'
import { componentSizes, token } from '../theme/tokens'

const FORM_ID = 'role-form'
// setTimeout 은 2^31-1ms(약 24.8일)를 넘기면 바로 실행된다 — 먼 만료 시각은 나눠서 기다린다
const MAX_TIMER_MS = 2_147_483_647

const ROLE_ROWS = [
  { value: 'AUDIENCE', title: '관객', sub: '공연을 찾고 예매해요', art: ticketArt },
  { value: 'ORGANIZER', title: '주최자', sub: '공연을 등록하고 관리해요', art: clipboardArt },
] as const satisfies readonly { value: UserRole; title: string; sub: string; art: string }[]

type View =
  | { kind: 'loading' }
  | { kind: 'failed'; rejected: boolean }
  | { kind: 'ready'; pending: PendingSignup }
  // 화면에 머물다 만료되면 대기 정보를 안다 — 서버가 처음부터 만료라고 하면 모른다
  | { kind: 'expired'; pending: PendingSignup | null }

type ServerError = 'exists' | 'rejected' | 'failed' | null

// 콜백 페이지가 이동하면서 넘긴 pendingToken. 없거나 문자열이 아니면 이 화면은 열지 않는다. 값은 바꾸지 않고 보낸다.
function pendingTokenFrom(state: unknown): string | null {
  const value = (state as { pendingToken?: unknown } | null)?.pendingToken
  return typeof value === 'string' && value.trim() !== '' ? value : null
}

const isExpiredError = (error: unknown) => error instanceof ApiError && error.code === PENDING_SIGNUP_EXPIRED

export function RoleSelectionPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const pendingToken = pendingTokenFrom(location.state)
  const [view, setView] = useState<View>({ kind: 'loading' })
  // 다시 시도하면 올린다 — 대기 정보 조회를 다시 돌리는 기준
  const [attempt, setAttempt] = useState(0)
  const [role, setRole] = useState<UserRole | null>(null)
  const [submitted, setSubmitted] = useState(false)
  const [serverError, setServerError] = useState<ServerError>(null)
  const [submitting, setSubmitting] = useState(false)
  const inFlight = useRef(false)
  const roleErrorId = useId()
  // 화면을 떠난 뒤에 도착한 응답은 로그인·이동·오류 표시 어디에도 쓰지 않는다 (ADR-0018).
  const mounted = useRef(true)
  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])

  // 대기 정보 — 어느 계정으로 가입 중인지와 만료 시각. 표시만 하고 세션은 건드리지 않는다
  useEffect(() => {
    if (pendingToken === null) return
    let cancelled = false
    fetchPendingSignup(pendingToken)
      .then((pending) => {
        if (!cancelled) setView({ kind: 'ready', pending })
      })
      .catch((error: unknown) => {
        if (cancelled) return
        setView(isExpiredError(error) ? { kind: 'expired', pending: null } : { kind: 'failed', rejected: isWriteKeyRejected(error) })
      })
    return () => {
      cancelled = true
    }
  }, [pendingToken, attempt])

  // 화면에 머물러 있어도 expiresAt 이 되는 순간 만료 상태로 — 계속하기가 가는 중이면 판정을 서버에 맡긴다
  const ready = view.kind === 'ready' ? view.pending : null
  const expiresAt = ready?.expiresAt ?? null
  useEffect(() => {
    if (expiresAt === null || submitting) return
    let timer: ReturnType<typeof setTimeout>
    const wait = () => {
      const left = expiresAt - Date.now()
      timer = setTimeout(() => {
        if (Date.now() >= expiresAt) setView({ kind: 'expired', pending: ready })
        else wait()
      }, Math.max(0, Math.min(left, MAX_TIMER_MS)))
    }
    wait()
    return () => clearTimeout(timer)
  }, [expiresAt, ready, submitting])

  if (pendingToken === null) return <Navigate to="/login" replace />

  const roleError = submitted && role === null ? AUTH_MESSAGES.roleRequired : null

  async function submit() {
    if (inFlight.current || view.kind !== 'ready' || pendingToken === null) return
    setServerError(null)
    setSubmitted(true)
    if (role === null) return
    const { pending } = view
    inFlight.current = true
    setSubmitting(true)
    try {
      const result = await completeSocialSignUp(pendingToken, role)
      if (!mounted.current) return
      auth.login(result.accessToken, result.user)
      navigate('/joined', { replace: true, state: { via: pending.provider } })
    } catch (error) {
      if (!mounted.current) return
      // 서버가 만료라고 하거나, 응답을 기다리는 사이 이미 시간이 지났으면 만료 상태
      if (isExpiredError(error) || Date.now() >= pending.expiresAt) {
        setView({ kind: 'expired', pending })
      } else if (isWriteKeyRejected(error)) {
        setServerError('rejected')
      } else if (error instanceof ApiError && (error.code === EMAIL_ALREADY_EXISTS || error.code === SOCIAL_ACCOUNT_ALREADY_LINKED)) {
        setServerError('exists')
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

  const back = <AppBar plain onBack={() => navigate('/login')} />

  if (view.kind === 'loading') {
    return (
      <>
        {back}
        <Page plain>
          <Box sx={{ display: 'grid', placeItems: 'center', paddingTop: token('--stack-l') }}>
            <CircularProgress aria-label={AUTH_MESSAGES.pendingLoading} />
          </Box>
        </Page>
      </>
    )
  }

  if (view.kind === 'failed') {
    return (
      <>
        {back}
        <Page plain hasCta>
          <Hero title={view.rejected ? WRITE_REJECTED_MESSAGE : AUTH_MESSAGES.pendingFailed} />
        </Page>
        <CtaBar
          onClick={() => {
            setView({ kind: 'loading' })
            setAttempt((n) => n + 1)
          }}
        >
          다시 시도하기
        </CtaBar>
      </>
    )
  }

  if (view.kind === 'expired') {
    // 시안 role-select--error — 어느 계정이었는지는 남기고, 역할 행 대신 만료 안내와 다시 로그인
    const expired = view.pending
    return (
      <>
        {back}
        <Page plain hasCta>
          <Box sx={{ display: 'flex', flexDirection: 'column' }}>
            <Hero
              eyebrow={expired ? `${providerNameOf(expired.provider)}로 처음 가입` : undefined}
              title={AUTH_MESSAGES.expiredTitle}
              lead={expired ? <Box component="span" sx={{ overflowWrap: 'anywhere' }}>{`${expired.email}(${expired.name})`}</Box> : undefined}
            />
          </Box>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-s') }}>
            <RoleLabel />
            <Box component="p" role="alert" sx={{ margin: 0, display: 'flex', alignItems: 'center', gap: componentSizes.rowGap, fontSize: token('--fs-sub'), lineHeight: Number(token('--lh-sub')), color: token('--c-danger') }}>
              <ErrorIcon />
              {AUTH_MESSAGES.expiredBody}
            </Box>
          </Box>
        </Page>
        <CtaBar onClick={() => navigate('/login')}>다시 로그인하기</CtaBar>
      </>
    )
  }

  const { pending } = view
  return (
    <>
      {back}
      <Page plain hasCta>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-m') }}>
          {serverError === 'exists' && (
            <TopNotice>
              {AUTH_MESSAGES.alreadyJoined} <MoreLink to="/login">로그인하기</MoreLink>
            </TopNotice>
          )}
          {serverError === 'rejected' && <TopNotice>{WRITE_REJECTED_MESSAGE}</TopNotice>}
          {serverError === 'failed' && <TopNotice>{AUTH_MESSAGES.completeFailed}</TopNotice>}
          <Hero
            eyebrow={`${providerNameOf(pending.provider)}로 처음 가입`}
            title={
              <>
                <Box component="span" sx={{ overflowWrap: 'anywhere' }}>
                  {pending.email}
                </Box>
                ({pending.name})으로 가입하고 있어요
              </>
            }
          />
        </Box>
        <Box component="form" id={FORM_ID} noValidate onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-s') }}>
          <RoleLabel />
          <Box role="radiogroup" aria-label="역할" aria-describedby={roleError ? roleErrorId : undefined} sx={{ display: 'flex', flexDirection: 'column', gap: token('--stack-s') }}>
            {ROLE_ROWS.map((row) => {
              const selected = role === row.value
              return (
                <Box
                  key={row.value}
                  component="button"
                  type="button"
                  role="radio"
                  aria-checked={selected}
                  onClick={() => setRole(row.value)}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: token('--stack-m'),
                    minHeight: token('--h-row'),
                    padding: `${token('--row-py')} ${token('--stack-m')}`,
                    border: '1px solid',
                    borderColor: selected ? token('--c-brand-line') : token('--c-line'),
                    borderRadius: token('--r-m'),
                    backgroundColor: token('--c-surface'),
                    fontFamily: 'inherit',
                    color: 'inherit',
                    textAlign: 'left',
                    cursor: 'pointer',
                    '&:focus-visible': { outline: `2px solid ${token('--c-focus')}`, outlineOffset: '2px' },
                  }}
                >
                  <Box component="span" sx={{ flex: 'none', display: 'grid', placeItems: 'center', width: componentSizes.touch, height: componentSizes.touch }}>
                    <Box component="img" src={row.art} alt="" sx={{ width: componentSizes.rowArt, height: componentSizes.rowArt }} />
                  </Box>
                  <Box sx={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: componentSizes.rowGap }}>
                    <Box component="span" sx={{ display: 'block', fontSize: token('--fs-strong'), fontWeight: Number(token('--fw-strong')), color: selected ? token('--c-brand-text') : 'inherit' }}>
                      {row.title}
                    </Box>
                    <Box component="span" sx={{ display: 'block', fontSize: token('--fs-sub'), color: token('--c-text-2') }}>
                      {row.sub}
                    </Box>
                  </Box>
                  <Box component="span" aria-hidden="true" sx={{ flex: 'none', visibility: selected ? 'visible' : 'hidden', color: token('--c-brand-line') }}>
                    <svg width={componentSizes.iconSmall} height={componentSizes.iconSmall} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
                      <path d="m6.5 12.5 3.5 3.5 7.5-8" />
                    </svg>
                  </Box>
                </Box>
              )
            })}
          </Box>
          {roleError && (
            <Box component="p" id={roleErrorId} data-field-error="" sx={{ margin: 0, display: 'flex', alignItems: 'center', gap: componentSizes.rowGap, fontSize: token('--fs-sub'), color: token('--c-danger') }}>
              <ErrorIcon />
              {roleError}
            </Box>
          )}
        </Box>
        <Info>{AUTH_MESSAGES.roleNotice}</Info>
      </Page>
      <CtaBar form={FORM_ID} disabled={submitting}>
        계속하기
      </CtaBar>
    </>
  )
}

function RoleLabel() {
  return (
    <Box component="span" sx={{ fontSize: token('--fs-sub'), fontWeight: Number(token('--fw-strong')), color: token('--c-text-2') }}>
      역할
    </Box>
  )
}

// 시안 .field-msg 의 경고 아이콘 — 오류를 색만으로 전하지 않는다
function ErrorIcon() {
  return (
    <svg aria-hidden="true" width={componentSizes.fieldMsgIcon} height={componentSizes.fieldMsgIcon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" style={{ flex: 'none' }}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 8v5M12 16h.01" />
    </svg>
  )
}
