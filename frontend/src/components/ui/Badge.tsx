import Box from '@mui/material/Box'
import type { ReactNode } from 'react'
import { componentSizes, token } from '../../theme/tokens'
import type { TokenName } from '../../theme/tokens'
import { isBlank } from './text'

export type BadgeTone = 'brand' | 'info' | 'danger' | 'warning' | 'success' | 'neutral'
/**
 * 비공개(`draft`)는 주최자 화면의 7번째 뜻이다(design.md §5). 관객 쪽 여섯 톤(`BadgeTone`)에 섞지 않는다 —
 * 승인된 TASK-017 테스트가 `Record<ExpectedTone>` 로 BadgeTone 을 인덱싱하므로 타입을 넓히면 그 테스트가 깨진다.
 */
export type BadgeToneOrDraft = BadgeTone | 'draft'

export interface BadgeProps {
  tone: BadgeToneOrDraft
  children?: ReactNode
}

// 시안 .badge.is-* — 뜻 하나에 색 하나 (design.md §5). neutral 바탕은 놓인 면의 채움색(시안 .badge 의 var(--c-fill))
const TONE: Record<Exclude<BadgeToneOrDraft, 'neutral'>, [TokenName, TokenName]> = {
  brand: ['--c-brand-tint', '--c-brand-tint-text'],
  info: ['--c-info-tint', '--c-info-tint-text'],
  danger: ['--c-danger-tint', '--c-danger-tint-text'],
  warning: ['--c-warning-tint', '--c-warning-tint-text'],
  success: ['--c-success-tint', '--c-success-tint-text'],
  draft: ['--c-draft-tint', '--c-draft-tint-text'], // 비공개 — 주최자가 공개하기 전 (design.md §5)
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
