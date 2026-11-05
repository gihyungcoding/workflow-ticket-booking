import { createTheme } from '@mui/material/styles'
import type { Theme } from '@mui/material/styles'
import { px, token } from './tokens'

// 그림자는 두 단계뿐이다 — 떠 있는 카드(--shadow-1)와 시트·토스트(--shadow-2)
const shadows = [
  'none',
  token('--shadow-1'),
  ...Array(23).fill(token('--shadow-2')),
] as unknown as Theme['shadows']

export function createAppTheme(): Theme {
  return createTheme({
    palette: {
      primary: { main: token('--c-brand'), dark: token('--c-brand-pressed'), contrastText: token('--c-on-brand') },
      error: { main: token('--c-danger') },
      success: { main: token('--c-success') },
      warning: { main: token('--c-warning') },
      info: { main: token('--c-info') },
      background: {
        default: token('--c-bg'),
        paper: token('--c-surface'),
      },
      text: {
        primary: token('--c-text-1'),
        secondary: token('--c-text-2'),
        disabled: token('--c-text-disabled'),
      },
      divider: token('--c-line'),
    },
    typography: {
      fontFamily: token('--font'),
      // 서체는 Pretendard 한 가지 — 위계는 크기·굵기로만 만든다 (design.md §4)
      h1: { fontSize: token('--fs-title'), fontWeight: Number(token('--fw-title')), lineHeight: Number(token('--lh-title')) },
      h2: { fontSize: token('--fs-h2'), fontWeight: Number(token('--fw-h2')), lineHeight: Number(token('--lh-h2')) },
      h5: { fontSize: token('--fs-h2'), fontWeight: Number(token('--fw-h2')), lineHeight: Number(token('--lh-h2')) },
      h6: { fontSize: token('--fs-strong'), fontWeight: Number(token('--fw-strong')), lineHeight: Number(token('--lh-strong')) },
      body1: { fontSize: token('--fs-body'), fontWeight: Number(token('--fw-body')), lineHeight: Number(token('--lh-body')) },
      body2: { fontSize: token('--fs-sub'), fontWeight: Number(token('--fw-sub')), lineHeight: Number(token('--lh-sub')) },
      caption: { fontSize: token('--fs-caption'), fontWeight: Number(token('--fw-caption')), lineHeight: Number(token('--lh-caption')) },
      button: { textTransform: 'none', fontWeight: Number(token('--fw-strong')) },
    },
    shape: {
      borderRadius: px('--r-m'),
    },
    shadows,
    components: {
      MuiCard: {
        defaultProps: {
          variant: 'outlined',
        },
      },
      MuiPaper: {
        styleOverrides: {
          // 흰 면 안의 채움 부품(배지·입력)은 회색 — 시안 .block 과 같은 규칙
          root: {
            '--c-fill': token('--c-bg'),
            '--c-track': token('--c-bg'),
          },
          outlined: {
            borderColor: token('--c-line'),
          },
        },
      },
      // MUI Chip은 border-radius를 32/2(=16px)로 하드코딩한다 — 배지 모서리(--r-s)를 명시적으로 주입
      MuiChip: {
        styleOverrides: {
          root: {
            borderRadius: token('--r-s'),
          },
        },
      },
    },
  })
}

export const theme = createAppTheme()
