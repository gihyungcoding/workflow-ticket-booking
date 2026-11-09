import { useEffect, useRef, useState } from 'react'
import Box from '@mui/material/Box'
import { useLocation, useNavigate } from 'react-router-dom'
import { fetchMe } from '../api/auth'
import { useAuth } from '../auth/AuthContext'
import { AUTH_MESSAGES } from '../auth/messages'
import { rememberedProviderName } from '../auth/socialProvider'
import idCardArt from '../assets/fluent/idcard.svg'
import { Page } from '../components/ui'
import { componentSizes, token } from '../theme/tokens'

type Callback = { kind: 'token'; value: string } | { kind: 'pending'; value: string } | { kind: 'invalid' }

// 콜백 주소에서 토큰을 읽는 곳은 여기 한 곳이다 — ADR-0021 로 query 가 fragment 로 바뀌면 이 함수만 고친다.
// 공백을 뺀 값이 있는 쪽이 정확히 하나일 때만 정상이다. 빈 값은 없는 것으로 본다.
function parseCallback(search: string): Callback {
  const params = new URLSearchParams(search)
  const token = (params.get('token') ?? '').trim()
  const pending = (params.get('pendingToken') ?? '').trim()
  if (token !== '' && pending === '') return { kind: 'token', value: token }
  if (pending !== '' && token === '') return { kind: 'pending', value: pending }
  return { kind: 'invalid' }
}

const FAILURE = '/login?error=social'

export function OAuthRedirectPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  // 첫 렌더에서 한 번만 읽는다 — 주소를 지운 뒤 개발용 이중 실행이 빈 주소를 읽으면 실패로 오판한다.
  const [callback] = useState(() => parseCallback(location.search))
  // 소셜 버튼을 누를 때 남긴 제공자 — 없거나 모르면 '소셜 계정'
  const [providerName] = useState(rememberedProviderName)

  // 화면을 떠난 뒤에 도착한 응답은 로그인·이동 어디에도 영향을 주지 않는다 (ADR-0018).
  const mounted = useRef(true)
  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])

  useEffect(() => {
    // 읽은 즉시 주소에서 토큰을 지운다 (접근 로그·Referer·히스토리 노출 완화).
    if (callback.kind === 'pending') {
      navigate('/oauth2/role', { replace: true, state: { pendingToken: callback.value } })
      return
    }
    if (callback.kind === 'invalid') {
      navigate(FAILURE, { replace: true })
      return
    }
    navigate('/oauth2/redirect', { replace: true })
    const accessToken = callback.value
    fetchMe(accessToken)
      .then((user) => {
        if (!mounted.current) return
        auth.login(accessToken, user)
        navigate('/', { replace: true })
      })
      .catch(() => {
        if (mounted.current) navigate(FAILURE, { replace: true })
      })
    // auth·navigate 는 렌더마다 바뀔 수 있어도 콜백 처리는 한 번이다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [callback])

  // 시안 oauth-callback 의 .wait — 일러스트 · 제목 · 진행 표시를 가운데에
  return (
    <Page plain>
      <Box sx={{ minHeight: `calc(100vh - ${token('--h-appbar')} * 2)`, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: token('--stack-l'), textAlign: 'center' }}>
        <Box component="img" src={idCardArt} alt="" sx={{ width: componentSizes.waitArt, height: componentSizes.waitArt }} />
        <Box component="h1" sx={{ margin: 0, fontSize: token('--fs-title'), fontWeight: Number(token('--fw-title')), lineHeight: Number(token('--lh-title')), letterSpacing: componentSizes.titleTracking, textWrap: 'balance' }}>
          {AUTH_MESSAGES.socialChecking(providerName)}
        </Box>
        {/* 시안 .wait-spin — 트랙(--c-line) 위에서 호(--c-brand)가 돈다. 움직임을 줄이면 돌지 않는다 */}
        <Box
          component="span"
          role="progressbar"
          aria-label={AUTH_MESSAGES.socialCheckingProgress}
          sx={{
            boxSizing: 'border-box',
            width: componentSizes.waitSpin,
            height: componentSizes.waitSpin,
            borderRadius: token('--r-pill'),
            borderWidth: componentSizes.waitSpinBorder,
            borderStyle: 'solid',
            borderColor: token('--c-line'),
            borderTopColor: token('--c-brand'),
            '@keyframes waitSpin': { to: { transform: 'rotate(360deg)' } },
            animation: 'waitSpin .8s linear infinite',
            '@media (prefers-reduced-motion: reduce)': { animation: 'none' },
          }}
        />
      </Box>
    </Page>
  )
}
