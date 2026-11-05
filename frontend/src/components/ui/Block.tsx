import Box from '@mui/material/Box'
import type { ReactNode } from 'react'
import { token } from '../../theme/tokens'

export interface BlockProps {
  title?: string
  note?: ReactNode
  list?: boolean
  children?: ReactNode
}

/** 시안 .block·.block-head·.block-note — 흰 섹션 한 덩어리. list 면 행 목록용 여백 */
export function Block({ title, note, list, children }: BlockProps) {
  return (
    <Box
      component="section"
      sx={{
        backgroundColor: token('--c-surface'),
        '--c-fill': token('--c-bg'),
        '--c-track': token('--c-bg'),
        margin: `0 ${token('--stack-m')}`,
        borderRadius: token('--r-l'),
        padding: token('--block-pad'),
        display: 'flex',
        flexDirection: 'column',
        gap: token('--stack-m'),
        ...(list && { paddingTop: token('--stack-m'), paddingBottom: token('--stack-s'), gap: 0 }),
      }}
    >
      {title && (
        <Box
          component="h2"
          sx={{
            margin: 0,
            paddingBottom: list ? token('--stack-s') : 0,
            fontSize: token('--fs-h2'),
            fontWeight: Number(token('--fw-h2')),
            lineHeight: Number(token('--lh-h2')),
          }}
        >
          {title}
        </Box>
      )}
      {children}
      {note && (
        <Box component="p" sx={{ margin: 0, fontSize: token('--fs-sub'), lineHeight: Number(token('--lh-sub')), color: token('--c-text-2') }}>
          {note}
        </Box>
      )}
    </Box>
  )
}
