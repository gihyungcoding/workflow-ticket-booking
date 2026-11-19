import Box from '@mui/material/Box'
import type { ReactNode } from 'react'
import { Link as RouterLink, Navigate, useLocation, useNavigate } from 'react-router-dom'
import idCardArt from '../assets/fluent/idcard.svg'
import { AppBar, Block, Page } from '../components/ui'
import { componentSizes, token } from '../theme/tokens'
import { useAuth } from './AuthContext'

/**
 * 시안 organizer-only 의 본문(.empty) — 그림·제목·부제·「공연 둘러보기」. 가드와 쓰기 403 이 같은 화면을 쓴다.
 * appBar 는 한 장 전체 화면(`/organizer`)에서만 붙인다 — 시안의 앱바 「내 공연」(뒤로 → 홈).
 * 등록·수정은 전역 헤더가 이미 있어 앱바를 더하면 헤더가 둘이 된다.
 */
export function OrganizerOnlyNotice({ appBar }: { appBar?: boolean }) {
  const navigate = useNavigate()
  const notice = (
    <Block>
      <Box sx={{ padding: `${token('--stack-l')} ${token('--block-pad')}`, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: token('--stack-s'), textAlign: 'center' }}>
        <Box component="img" src={idCardArt} alt="" />
        <Box component="p" sx={{ margin: 0, fontSize: token('--fs-strong'), fontWeight: Number(token('--fw-strong')), lineHeight: Number(token('--lh-strong')), textWrap: 'balance' }}>
          주최자만 이용할 수 있어요
        </Box>
        <Box component="p" sx={{ margin: 0, maxWidth: componentSizes.emptySubMax, fontSize: token('--fs-sub'), lineHeight: Number(token('--lh-sub')), color: token('--c-text-2'), textWrap: 'pretty' }}>
          공연 등록과 관리는 주최자 계정에서 할 수 있어요
        </Box>
        <Box
          component={RouterLink}
          to="/"
          sx={{
            // 시안 .block .btn { align-self: stretch } — 블록 안 버튼은 가로를 채운다
            alignSelf: 'stretch',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            height: token('--h-btn'),
            padding: `0 ${token('--block-pad')}`,
            borderRadius: token('--r-m'),
            fontSize: token('--fs-strong'),
            fontWeight: Number(token('--fw-strong')),
            textDecoration: 'none',
            backgroundColor: token('--c-brand'),
            color: token('--c-on-brand'),
            '&:active': { backgroundColor: token('--c-brand-pressed') },
            '&:focus-visible': { outline: `2px solid ${token('--c-focus')}`, outlineOffset: '2px' },
          }}
        >
          공연 둘러보기
        </Box>
      </Box>
    </Block>
  )
  if (!appBar) return notice
  return (
    <>
      <AppBar title="내 공연" onBack={() => navigate('/')} />
      <Page>{notice}</Page>
    </>
  )
}

/**
 * 등록·수정 화면 가드 (기능 문서 §4). 화면을 가르는 편의일 뿐 최종 판단은 서버가 한다.
 * 복원 중에는 안내·이동 없이 아무것도 그리지 않는다 — 복원이 끝나면 다시 판정한다.
 */
export function RequireOrganizer({ children, appBar }: { children: ReactNode; appBar?: boolean }) {
  const { user, restoring } = useAuth()
  const location = useLocation()
  if (restoring) return null
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search + location.hash }} />
  }
  if (user.role !== 'ORGANIZER') return <OrganizerOnlyNotice appBar={appBar} />
  return children
}
