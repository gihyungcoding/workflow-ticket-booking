import Box from '@mui/material/Box'
import { componentSizes, token } from '../../theme/tokens'

export interface AppBarProps {
  title?: string
  onBack?: () => void
}

/** 시안 .appbar — 가운데 제목, 왼쪽 뒤로 버튼(onBack 이 있을 때만) */
export function AppBar({ title, onBack }: AppBarProps) {
  return (
    <Box
      component="header"
      sx={{
        position: 'sticky',
        top: 0,
        zIndex: 5,
        height: token('--h-appbar'),
        display: 'grid',
        gridTemplateColumns: `minmax(${componentSizes.appbarSide}, auto) 1fr minmax(${componentSizes.appbarSide}, auto)`,
        alignItems: 'center',
        padding: `0 ${token('--stack-s')}`,
        backgroundColor: token('--c-bg'),
      }}
    >
      <Box>
        {onBack && (
          <Box
            component="button"
            type="button"
            aria-label="뒤로"
            onClick={onBack}
            sx={{
              width: componentSizes.touch,
              height: componentSizes.touch,
              display: 'grid',
              placeItems: 'center',
              padding: 0,
              border: 0,
              background: 'none',
              cursor: 'pointer',
              color: token('--c-text-1'),
            }}
          >
            <svg aria-hidden="true" width={token('--icon')} height={token('--icon')} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M15 18l-6-6 6-6" />
            </svg>
          </Box>
        )}
      </Box>
      <Box
        component="h1"
        sx={{
          margin: 0,
          textAlign: 'center',
          fontSize: token('--fs-strong'),
          fontWeight: Number(token('--fw-strong')),
          lineHeight: Number(token('--lh-strong')),
          overflowWrap: 'anywhere',
          textWrap: 'balance',
        }}
      >
        {title ?? ''}
      </Box>
      <Box />
    </Box>
  )
}
