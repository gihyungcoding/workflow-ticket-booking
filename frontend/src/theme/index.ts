import { createTheme } from '@mui/material/styles'
import type { Theme } from '@mui/material/styles'
import {
  colorCanvas,
  colorInk,
  colorInkMuted,
  colorOpen,
  colorRule,
  colorSurface,
  fontText,
  radiusBadge,
  radiusSurface,
} from './tokens'

const noShadows = Array(25).fill('none') as unknown as Theme['shadows']

export function createAppTheme(): Theme {
  return createTheme({
    palette: {
      background: {
        default: colorCanvas,
        paper: colorSurface,
      },
      text: {
        primary: colorInk,
        secondary: colorInkMuted,
      },
      success: {
        main: colorOpen,
      },
    },
    typography: {
      fontFamily: fontText,
    },
    shape: {
      borderRadius: radiusSurface,
    },
    shadows: noShadows,
    components: {
      // 그림자를 전부 없앴다 — 카드 위계는 그림자 대신 구분선으로 만든다 (design.md §4)
      MuiCard: {
        defaultProps: {
          variant: 'outlined',
        },
      },
      MuiPaper: {
        styleOverrides: {
          outlined: {
            borderColor: colorRule,
          },
        },
      },
      // MUI Chip은 border-radius를 32/2(=16px)로 하드코딩한다 — 배지 전용 토큰을 명시적으로 주입
      MuiChip: {
        styleOverrides: {
          root: {
            borderRadius: radiusBadge,
          },
        },
      },
    },
  })
}

export const theme = createAppTheme()
