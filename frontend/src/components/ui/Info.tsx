import Box from '@mui/material/Box'
import type { ReactNode } from 'react'
import { componentSizes, token } from '../../theme/tokens'

export interface InfoProps {
  /** 앞 아이콘 — 없으면 시계 모양 안내 아이콘 */
  icon?: ReactNode
  children: ReactNode
}

/** 시안 .info — 아이콘 + 안내 한 줄 */
export function Info({ icon, children }: InfoProps) {
  return (
    <Box component="p" sx={{ margin: 0, display: 'flex', alignItems: 'flex-start', gap: token('--stack-s'), fontSize: token('--fs-sub'), lineHeight: Number(token('--lh-sub')), color: token('--c-text-2') }}>
      <Box component="span" aria-hidden="true" sx={{ flex: 'none', display: 'grid', placeItems: 'center', width: componentSizes.iconSmall, height: componentSizes.iconSmall, color: token('--c-text-3') }}>
        {icon ?? (
          <svg width={componentSizes.iconSmall} height={componentSizes.iconSmall} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="9" />
            <path d="M12 7.5V12l3 2" />
          </svg>
        )}
      </Box>
      <span>{children}</span>
    </Box>
  )
}
