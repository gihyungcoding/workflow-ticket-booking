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

// 시안 .badge.is-* — 뜻 하나에 색 하나 (design.md §5). neutral 바탕은 놓인 면의 채움색(시안 .badge 의 var(--c-fill))
const TONE: Record<Exclude<BadgeTone, 'neutral'>, [TokenName, TokenName]> = {
  brand: ['--c-brand-tint', '--c-brand-tint-text'],
  info: ['--c-info-tint', '--c-info-tint-text'],
  danger: ['--c-danger-tint', '--c-danger-tint-text'],
  warning: ['--c-warning-tint', '--c-warning-tint-text'],
  success: ['--c-success-tint', '--c-success-tint-text'],
}

export function Badge({ tone, children }: BadgeProps) {
  if (children == null || typeof children === 'boolean' || (typeof children === 'string' && isBlank(children))) return null
  const [background, color]: [string, string] =
    tone === 'neutral' ? ['var(--c-fill)', token('--c-text-2')] : [token(TONE[tone][0]), token(TONE[tone][1])]
  return (
    <Box
      component="span"
      sx={{
        alignSelf: 'flex-start',
        // 가로 줄(flex) 안에서 줄어들면 짧은 문구도 말줄임된다 — 칸보다 긴 문구만 max-width 에서 자른다
        flexShrink: 0,
        display: 'inline-block',
        height: componentSizes.badgeHeight,
        lineHeight: componentSizes.badgeHeight,
        padding: `0 ${token('--stack-s')}`,
        borderRadius: token('--r-s'),
        fontSize: token('--fs-caption'),
        fontWeight: Number(token('--fw-strong')),
        backgroundColor: background,
        color,
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
