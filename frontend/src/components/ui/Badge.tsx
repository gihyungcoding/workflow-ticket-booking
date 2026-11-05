import Box from '@mui/material/Box'
import type { ReactNode } from 'react'
import { componentSizes, token } from '../../theme/tokens'
import type { TokenName } from '../../theme/tokens'
import { isBlank } from './text'

export type BadgeTone = 'brand' | 'info' | 'danger' | 'warning' | 'success' | 'neutral'

export interface BadgeProps {
  tone: BadgeTone
  children?: ReactNode
}

// 시안 .badge.is-* — 뜻 하나에 색 하나 (design.md §5)
const TONE: Record<BadgeTone, [TokenName, TokenName]> = {
  brand: ['--c-brand-tint', '--c-brand-tint-text'],
  info: ['--c-info-tint', '--c-info-tint-text'],
  danger: ['--c-danger-tint', '--c-danger-tint-text'],
  warning: ['--c-warning-tint', '--c-warning-tint-text'],
  success: ['--c-success-tint', '--c-success-tint-text'],
  neutral: ['--c-bg', '--c-text-2'],
}

export function Badge({ tone, children }: BadgeProps) {
  if (children == null || (typeof children === 'string' && isBlank(children))) return null
  const [background, color] = TONE[tone]
  return (
    <Box
      component="span"
      sx={{
        alignSelf: 'flex-start',
        display: 'inline-block',
        height: componentSizes.badgeHeight,
        lineHeight: componentSizes.badgeHeight,
        padding: `0 ${token('--stack-s')}`,
        borderRadius: token('--r-s'),
        fontSize: token('--fs-caption'),
        fontWeight: Number(token('--fw-strong')),
        backgroundColor: token(background),
        color: token(color),
        whiteSpace: 'nowrap',
        maxWidth: '100%',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        verticalAlign: 'middle',
      }}
    >
      {children}
    </Box>
  )
}
