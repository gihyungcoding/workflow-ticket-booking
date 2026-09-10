import { createTheme } from '@mui/material/styles'
import type { Theme } from '@mui/material/styles'
import {
  colorCanvas,
  colorInk,
  colorInkMuted,
  colorOpen,
  colorSurface,
  fontText,
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
  })
}

export const theme = createAppTheme()
