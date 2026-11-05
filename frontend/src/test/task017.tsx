import { render } from '@testing-library/react'
import { ThemeProvider } from '@mui/material/styles'
import type { ReactElement } from 'react'
import { createAppTheme } from '../theme'

/** TASK-017 테스트 공용 — 새 테마로 그린다. */
export function renderWithTheme(ui: ReactElement) {
  const theme = createAppTheme()
  const result = render(<ThemeProvider theme={theme}>{ui}</ThemeProvider>)
  return {
    ...result,
    rerenderWithTheme: (next: ReactElement) => result.rerender(<ThemeProvider theme={theme}>{next}</ThemeProvider>),
  }
}

/** prefers-reduced-motion 등 미디어 쿼리 결과를 고정한다 */
export function mockMatchMedia(matches: (query: string) => boolean) {
  window.matchMedia = ((query: string) => ({
    matches: matches(query),
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  })) as unknown as typeof window.matchMedia
}
