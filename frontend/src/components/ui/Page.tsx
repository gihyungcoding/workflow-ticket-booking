import Box from '@mui/material/Box'
import type { ReactNode } from 'react'
import { token } from '../../theme/tokens'

export interface PageProps {
  children?: ReactNode
  hasCta?: boolean
}

/** 시안 .page — 블록 사이 간격, 하단 고정 CTA 가 있으면 그만큼 아래 여백 */
export function Page({ children, hasCta }: PageProps) {
  return (
    <Box
      component="main"
      sx={{
        padding: `${token('--stack-s')} 0 ${token('--stack-l')}`,
        display: 'flex',
        flexDirection: 'column',
        gap: token('--block-gap'),
        ...(hasCta && {
          paddingBottom: `calc(${token('--h-btn')} + ${token('--stack-l')} * 2 + env(safe-area-inset-bottom))`,
        }),
      }}
    >
      {children}
    </Box>
  )
}
