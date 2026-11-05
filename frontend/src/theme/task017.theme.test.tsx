import { render } from '@testing-library/react'
import { ThemeProvider } from '@mui/material/styles'
import * as tokensModule from './tokens'
import { createAppTheme } from './index'
import { GlobalTokens } from './GlobalTokens'
import { rgb } from './task017.expected'

// SoT: workflow_design/05_scenario/SCENARIO_TASK-017.md
// 정본 CSS 는 Vite 루트(frontend/) 밖이라 ?raw 로 못 읽는다 — node 모듈을 타입 없이 동적으로 불러온다(앱 tsconfig 에 node 타입 없음).

const FS: string = 'node:fs'
const TOKENS_CSS = decodeURIComponent(import.meta.url.replace('file://', '').split('/frontend/')[0]) + '/docs/product/design-tokens.css'


/** 블록 하나(:root 또는 .text-large)의 `--이름: 값` 쌍 */
function declarations(css: string, selector: string): Record<string, string> {
  const start = css.indexOf(`${selector} {`)
  const body = css.slice(css.indexOf('{', start) + 1, css.indexOf('}', start))
  const out: Record<string, string> = {}
  for (const match of body.replace(/\/\*[\s\S]*?\*\//g, '').matchAll(/(--[a-z0-9-]+)\s*:\s*([^;]+);/g)) {
    out[match[1]] = match[2].trim()
  }
  return out
}

function normalized(value: string): string {
  return /^#[0-9a-f]{3,8}$/i.test(value) ? value.toUpperCase() : value
}

describe('TASK-017 테마·토큰', () => {
  test('TASK-017 SC-01 (happy) tokens.ts 의 값이 design-tokens.css 와 하나도 다르지 않다', async () => {
    // Given design-tokens.css 의 :root 와 .text-large 선언을 모았다
    const { readFileSync } = (await import(/* @vite-ignore */ FS)) as {
      readFileSync: (path: string, encoding: 'utf8') => string
    }
    const css = readFileSync(TOKENS_CSS, 'utf8')
    const root = declarations(css, ':root')
    const large = declarations(css, '.text-large')
    expect(Object.keys(root).length).toBeGreaterThan(50)

    // When tokens.ts 의 토큰 표와 대조한다
    const { designTokens, textLargeTokens } = tokensModule as unknown as {
      designTokens: Record<string, string>
      textLargeTokens: Record<string, string>
    }

    // Then 정본의 모든 이름이 있고 값이 같다 (색은 대소문자 무시)
    expect(designTokens).toBeDefined()
    expect(textLargeTokens).toBeDefined()
    for (const [name, value] of Object.entries(root)) {
      expect(normalized(designTokens[name] ?? '<없음>'), name).toBe(normalized(value))
    }
    for (const [name, value] of Object.entries(large)) {
      expect(normalized(textLargeTokens[name] ?? '<없음>'), `.text-large ${name}`).toBe(normalized(value))
    }
    // And 정본에 없는 이름이 없다
    expect(Object.keys(designTokens).sort()).toEqual(Object.keys(root).sort())
    expect(Object.keys(textLargeTokens).sort()).toEqual(Object.keys(large).sort())
  })

  test('TASK-017 SC-02 (happy) 테마가 토큰 값으로 만들어진다', () => {
    // Given / When 새 테마를 만들어 값을 읽는다
    const theme = createAppTheme()

    // Then 표와 같다
    expect(theme.palette.primary.main.toUpperCase()).toBe('#4154FF')
    expect(theme.palette.background.default.toUpperCase()).toBe('#F1F2F6')
    expect(theme.palette.background.paper.toUpperCase()).toBe('#FEFEFE')
    expect(theme.palette.text.primary.toUpperCase()).toBe('#1A1C28')
    expect(theme.palette.text.secondary.toUpperCase()).toBe('#4C4F66')
    expect(theme.palette.error.main.toUpperCase()).toBe('#E1292F')
    expect(String(theme.typography.fontFamily)).toMatch(/^'?Pretendard/)
    expect(theme.typography.h1.fontSize).toBe('28px')
    expect(theme.typography.h1.fontWeight).toBe(700)
    expect(theme.typography.body1.fontSize).toBe('16px')
    expect(theme.typography.body1.lineHeight).toBe(1.55)
    expect(theme.shape.borderRadius).toBe(12)
  })

  test('TASK-017 SC-12 (happy) 앱은 375px 한 열로 가운데 놓인다', () => {
    // Given 전역 토큰 스타일을 적용했다
    const root = document.createElement('div')
    root.id = 'root'
    document.body.appendChild(root)
    render(
      <ThemeProvider theme={createAppTheme()}>
        <GlobalTokens />
      </ThemeProvider>,
    )

    // When #root 의 계산된 스타일을 읽는다
    const rootStyle = getComputedStyle(root)
    const bodyStyle = getComputedStyle(document.body)

    // Then 최대 폭 375px, 좌우 바깥 여백 auto
    expect(rootStyle.maxWidth).toBe('375px')
    expect(rootStyle.marginLeft).toBe('auto')
    expect(rootStyle.marginRight).toBe('auto')
    // And body 바탕 #F1F2F6, overflow-x 가 hidden 이 아니고, 줄바꿈 keep-all
    expect(bodyStyle.backgroundColor).toBe(rgb('#F1F2F6'))
    expect(bodyStyle.overflowX).not.toBe('hidden')
    expect(bodyStyle.wordBreak).toBe('keep-all')
    root.remove()
  })
})
