import GlobalStyles from '@mui/material/GlobalStyles'
import { componentSizes, designTokens, token } from './tokens'

/** :root 토큰 CSS 변수·body 기본 스타일·#root 한 열 (TASK-017). 시안 out/components.css 의 body 규칙과 같다. */
export function GlobalTokens() {
  return (
    <GlobalStyles
      styles={{
        ':root': designTokens,
        body: {
          margin: 0,
          fontFamily: token('--font'),
          letterSpacing: token('--tracking'),
          fontSize: token('--fs-body'),
          fontWeight: Number(token('--fw-body')),
          lineHeight: Number(token('--lh-body')),
          color: token('--c-text-1'),
          backgroundColor: token('--c-bg'),
          // 채움 부품의 바탕 — 회색 페이지 위에서는 흰색, 흰 블록 안에서는 회색 (components.css body·.block)
          '--c-fill': token('--c-surface'),
          '--c-track': token('--c-line'),
          WebkitFontSmoothing: 'antialiased',
          minHeight: '100vh',
          // 한국어: 단어 중간에서 끊지 않는다
          wordBreak: 'keep-all',
          overflowWrap: 'anywhere',
        },
        '#root': {
          maxWidth: componentSizes.appMaxWidth,
          marginLeft: 'auto',
          marginRight: 'auto',
          minHeight: '100vh',
        },
      }}
    />
  )
}
