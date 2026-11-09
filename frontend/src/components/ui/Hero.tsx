import Box from '@mui/material/Box'
import type { ReactNode } from 'react'
import { token } from '../../theme/tokens'

export interface HeroProps {
  eyebrow?: ReactNode
  title: ReactNode
  lead?: ReactNode
}

/** 시안 .hero·.eyebrow·.title·.lead — 화면 맨 위 머리글 */
export function Hero({ eyebrow, title, lead }: HeroProps) {
  return (
    <Box component="header" sx={{ padding: `${token('--stack-l')} 0 ${token('--stack-m')}`, display: 'flex', flexDirection: 'column', gap: token('--stack-s') }}>
      {eyebrow && (
        <Box component="p" sx={{ margin: 0, fontSize: token('--fs-sub'), fontWeight: Number(token('--fw-strong')), lineHeight: Number(token('--lh-sub')), color: token('--c-text-2') }}>
          {eyebrow}
        </Box>
      )}
      <Box
        component="h1"
        sx={{
          margin: 0,
          fontSize: token('--fs-title'),
          fontWeight: Number(token('--fw-title')),
          lineHeight: Number(token('--lh-title')),
          overflowWrap: 'anywhere',
          textWrap: 'balance',
        }}
      >
        {title}
      </Box>
      {lead && (
        <Box component="p" sx={{ margin: 0, fontSize: token('--fs-body'), lineHeight: Number(token('--lh-body')), color: token('--c-text-2'), textWrap: 'pretty' }}>
          {lead}
        </Box>
      )}
    </Box>
  )
}
