import Box from '@mui/material/Box'
import type { ReactNode } from 'react'
import { componentSizes, token } from '../../theme/tokens'

export interface RowProps {
  lead?: ReactNode
  title: ReactNode
  sub?: ReactNode
  trail?: ReactNode
}

/** 시안 .row — 앞(아이콘) · 제목/보조 · 뒤(값·화살표) */
export function Row({ lead, title, sub, trail }: RowProps) {
  return (
    <Box
      data-row=""
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: token('--stack-m'),
        minHeight: token('--h-row'),
        padding: `${token('--row-py')} 0`,
      }}
    >
      {lead && (
        <Box
          sx={{
            flex: 'none',
            width: componentSizes.touch,
            height: componentSizes.touch,
            borderRadius: token('--r-pill'),
            backgroundColor: 'var(--c-fill)',
            display: 'grid',
            placeItems: 'center',
            color: token('--c-text-2'),
          }}
        >
          {lead}
        </Box>
      )}
      <Box sx={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: componentSizes.rowGap }}>
        <Box sx={{ fontSize: token('--fs-strong'), fontWeight: Number(token('--fw-strong')), lineHeight: Number(token('--lh-strong')) }}>
          {title}
        </Box>
        {sub && (
          <Box sx={{ fontSize: token('--fs-sub'), lineHeight: Number(token('--lh-sub')), color: token('--c-text-2') }}>{sub}</Box>
        )}
      </Box>
      {trail && (
        <Box sx={{ flex: 'none', fontSize: token('--fs-sub'), color: token('--c-text-2'), display: 'flex', alignItems: 'center', gap: token('--stack-s') }}>
          {trail}
        </Box>
      )}
    </Box>
  )
}
