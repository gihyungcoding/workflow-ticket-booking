import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import type { ReactNode } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { componentSizes, token } from '../theme/tokens'

// 인증 화면 다섯 곳이 함께 쓰는 시안 조각 — out/screens/login·signup·role-select·joined 의 화면 고유 클래스.

/** 시안 .btn.primary.full — 화면 안의 주 버튼 (하단 고정이 아닌 것) */
export function PrimaryButton({ children, disabled, type = 'submit' }: { children: ReactNode; disabled?: boolean; type?: 'submit' | 'button' }) {
  return (
    <Box
      component="button"
      type={type}
      disabled={disabled}
      sx={{
        width: '100%',
        height: token('--h-btn'),
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
  )
}

/** 시안 .hero-more — 화살표가 붙은 글자 링크 */
export function MoreLink({ to, children }: { to: string; children: ReactNode }) {
  return (
    <Box
      component={RouterLink}
      to={to}
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '2px',
        minHeight: componentSizes.touch,
        fontSize: token('--fs-sub'),
        fontWeight: Number(token('--fw-strong')),
        color: token('--c-brand-text'),
        textDecoration: 'none',
      }}
    >
      {children}
      <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
        <path d="M9 6l6 6-6 6" />
      </svg>
    </Box>
  )
}

/** 시안 .join·.login — 가운데 정렬한 안내 + 링크 한 줄 */
export function LinkLine({ children }: { children: ReactNode }) {
  return (
    <Box component="p" sx={{ margin: 0, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: token('--stack-s'), fontSize: token('--fs-sub'), color: token('--c-text-2') }}>
      {children}
    </Box>
  )
}

/** 시안 .or — 양옆 선이 있는 '또는' */
export function OrDivider() {
  return (
    <Box
      component="p"
      sx={{
        margin: 0,
        display: 'flex',
        alignItems: 'center',
        gap: token('--stack-m'),
        fontSize: token('--fs-sub'),
        color: token('--c-text-3'),
        '&::before, &::after': { content: '""', flex: 1, height: '1px', backgroundColor: token('--c-line') },
      }}
    >
      또는
    </Box>
  )
}

/** 맨 위 안내 — 실패는 error, 가입 뒤 로그인 안내는 info */
export function TopNotice({ severity = 'error', children }: { severity?: 'error' | 'info'; children: ReactNode }) {
  return <Alert severity={severity}>{children}</Alert>
}
