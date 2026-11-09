import Box from '@mui/material/Box'
import type { ReactNode } from 'react'
import { token } from '../../theme/tokens'

export interface PageProps {
  children?: ReactNode
  hasCta?: boolean
  /** 시안 body.plain — 흰 바탕 한 장(폼 화면). 채움 부품은 회색, 좌우 여백 --page-x */
  plain?: boolean
}

/** 시안 .page — 블록 사이 간격, 하단 고정 CTA 가 있으면 그만큼 아래 여백 */
export function Page({ children, hasCta, plain }: PageProps) {
  return (
    <Box
      component="main"
      sx={{
        padding: `${token('--stack-s')} 0 ${token('--stack-l')}`,
        display: 'flex',
        flexDirection: 'column',
        gap: token('--block-gap'),
        ...(plain && {
          minHeight: '100vh',
          boxSizing: 'border-box',
          backgroundColor: token('--c-surface'),
          '--c-fill': token('--c-bg'),
          '--c-track': token('--c-bg'),
          gap: token('--stack-l'),
          paddingLeft: token('--page-x'),
          paddingRight: token('--page-x'),
        }),
        ...(hasCta && {
          paddingBottom: `calc(${token('--h-btn')} + ${token('--stack-l')} * 2 + env(safe-area-inset-bottom))`,
        }),
      }}
    >
      {children}
    </Box>
  )
}
