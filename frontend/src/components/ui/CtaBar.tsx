import Box from '@mui/material/Box'
import type { ReactNode } from 'react'
import { componentSizes, token } from '../../theme/tokens'

export interface CtaBarProps {
  children: ReactNode
  onClick?: () => void
  disabled?: boolean
}

/** 시안 .cta + .btn.primary.full — 화면 아래 고정 주 버튼 */
export function CtaBar({ children, onClick, disabled }: CtaBarProps) {
  return (
    <Box
      sx={{
        position: 'fixed',
        left: 0,
        right: 0,
        bottom: 0,
        maxWidth: componentSizes.appMaxWidth,
        margin: '0 auto',
        padding: `${token('--stack-m')} ${token('--page-x')} calc(${token('--stack-m')} + env(safe-area-inset-bottom))`,
        background: `linear-gradient(to top, ${token('--c-bg')} 70%, transparent)`,
      }}
    >
      <Box
        component="button"
        type="button"
        disabled={disabled}
        onClick={onClick}
        sx={{
          width: '100%',
          height: token('--h-btn'),
          padding: `0 ${token('--block-pad')}`,
          border: 0,
          borderRadius: token('--r-m'),
          fontFamily: 'inherit',
          fontSize: token('--fs-strong'),
          fontWeight: Number(token('--fw-strong')),
          cursor: disabled ? 'default' : 'pointer',
          backgroundColor: token(disabled ? '--c-disabled-bg' : '--c-brand'),
          color: token(disabled ? '--c-text-disabled' : '--c-on-brand'),
          '&:active': disabled ? {} : { backgroundColor: token('--c-brand-pressed') },
          '&:focus-visible': { outline: `2px solid ${token('--c-focus')}`, outlineOffset: '2px' },
        }}
      >
        {children}
      </Box>
    </Box>
  )
}
