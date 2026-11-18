import Box from '@mui/material/Box'
import type { ReactNode } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { componentSizes, token } from '../../theme/tokens'

export interface FabProps {
  to: string
  children: ReactNode
}

/** 시안 .fab — 화면 위에 떠 있는 주 행동 하나(공연 등록). 스크롤해도 제자리, 아래 안전 영역을 비운다 */
export function Fab({ to, children }: FabProps) {
  return (
    <Box
      component={RouterLink}
      to={to}
      sx={{
        position: 'fixed',
        right: token('--page-x'),
        bottom: `calc(${token('--stack-l')} + env(safe-area-inset-bottom))`,
        zIndex: 6,
        height: componentSizes.fab,
        minWidth: componentSizes.fab,
        padding: `0 ${token('--stack-m')}`,
        borderRadius: token('--r-pill'),
        backgroundColor: token('--c-brand'),
        color: token('--c-on-brand'),
        boxShadow: token('--shadow-2'),
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: componentSizes.fabGap,
        fontSize: token('--fs-strong'),
        fontWeight: Number(token('--fw-strong')),
        textDecoration: 'none',
        flexShrink: 0,
        '&:active': { backgroundColor: token('--c-brand-pressed') },
        '&:focus-visible': { outline: `2px solid ${token('--c-focus')}`, outlineOffset: '2px' },
      }}
    >
      <svg aria-hidden="true" width={token('--icon')} height={token('--icon')} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 5v14" />
        <path d="M5 12h14" />
      </svg>
      {children}
    </Box>
  )
}
