import { useEffect, useRef, useState } from 'react'
import Box from '@mui/material/Box'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { AUTH_MESSAGES } from '../auth/messages'
import { providerNameOf } from '../auth/socialProvider'
import ticketArt from '../assets/fluent/ticket.svg'
import { Block, CtaBar, Hero, Info, Page, Toast } from '../components/ui'
import { componentSizes, token } from '../theme/tokens'

const ROLE_NAMES = { AUDIENCE: '관객', ORGANIZER: '주최자' } as const

// 가입 경로 — 이동할 때 넘긴 state.via ('email' 또는 대기 정보의 제공자 enum). 새로고침으로 사라지면 역할만 보인다
function viaName(state: unknown): string | null {
  const via = (state as { via?: unknown } | null)?.via
  return via === 'email' ? '이메일' : providerNameOf(via)
}

type Copy = 'copied' | 'fallback' | null

export function JoinedPage() {
  const { user, restoring } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [copy, setCopy] = useState<Copy>(null)
  const handleRef = useRef<HTMLParagraphElement>(null)
  // 복사가 끝나기 전에 화면을 떠나면 결과를 쓰지 않는다
  const mounted = useRef(true)
  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])

  if (restoring) return null
  if (!user) return <Navigate to="/login" replace />

  const via = viaName(location.state)
  const roleName = ROLE_NAMES[user.role]
  const handle = user.handle

  // 클립보드가 막혀 있으면 아이디 글자를 선택해 두고 길게 눌러 복사하라고 안내한다
  function selectHandle() {
    const node = handleRef.current
    const selection = window.getSelection()
    if (!node || !selection) return
    const range = document.createRange()
    range.selectNodeContents(node)
    selection.removeAllRanges()
    selection.addRange(range)
  }

  async function copyHandle() {
    setCopy(null)
    try {
      if (!navigator.clipboard?.writeText) throw new Error('클립보드를 쓸 수 없습니다')
      await navigator.clipboard.writeText(handle)
      if (mounted.current) setCopy('copied')
    } catch {
      if (!mounted.current) return
      setCopy('fallback')
      selectHandle()
    }
  }

  return (
    <>
      <Page hasCta>
        <Box sx={{ padding: `0 ${token('--page-x')}` }}>
          <Hero eyebrow={via ? `${via} · ${roleName}` : roleName} title="가입이 끝났어요" />
        </Box>
        <Block>
          <Box>
            <Box component="p" sx={{ margin: 0, fontSize: token('--fs-sub'), fontWeight: Number(token('--fw-strong')), color: token('--c-text-2') }}>
              내 티켓온 아이디
            </Box>
            <Box
              component="p"
              ref={handleRef}
              sx={{
                margin: 0,
                // 시안 .id-value — 제목 크기의 1.6배
                fontSize: `calc(${token('--fs-title')} * 1.6)`,
                fontWeight: Number(token('--fw-title')),
                lineHeight: componentSizes.idLineHeight,
                letterSpacing: componentSizes.titleTracking,
                overflowWrap: 'anywhere',
                userSelect: 'all',
              }}
            >
              {handle}
            </Box>
          </Box>
          <Box
            component="button"
            type="button"
            onClick={() => void copyHandle()}
            sx={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: token('--stack-s'),
              height: token('--h-btn'),
              border: 0,
              borderRadius: token('--r-m'),
              fontFamily: 'inherit',
              fontSize: token('--fs-strong'),
              fontWeight: Number(token('--fw-strong')),
              cursor: 'pointer',
              backgroundColor: token('--c-brand-tint'),
              color: token('--c-brand-tint-text'),
              '&:focus-visible': { outline: `2px solid ${token('--c-focus')}`, outlineOffset: '2px' },
            }}
          >
            <svg aria-hidden="true" width={componentSizes.iconSmall} height={componentSizes.iconSmall} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
              <rect x="9" y="9" width="11" height="11" rx="2" />
              <path d="M5 15V6a2 2 0 0 1 2-2h9" />
            </svg>
            아이디 복사
          </Box>
          {copy === 'fallback' && <Info>{AUTH_MESSAGES.copyFallback}</Info>}
          <Info icon={<Box component="img" src={ticketArt} alt="" sx={{ width: componentSizes.iconSmall, height: componentSizes.iconSmall }} />}>
            일행에게 아이디를 보내면 표를 받을 수 있어요
          </Info>
        </Block>
      </Page>
      <CtaBar onClick={() => navigate('/')}>시작하기</CtaBar>
      <Toast message={copy === 'copied' ? AUTH_MESSAGES.copied : null} onClose={() => setCopy(null)} />
    </>
  )
}
