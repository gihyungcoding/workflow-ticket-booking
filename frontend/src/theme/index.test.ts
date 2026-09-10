import { createAppTheme } from './index'

describe('createAppTheme', () => {
  test('SC-01 (happy) 테마가 토큰 값으로 초기화된다', () => {
    // Given
    // frontend/src/theme/index.ts 가 docs/product/design-tokens.css 의 값으로 MUI 테마를 만든다

    // When
    // 이 테마 객체의 palette·shape·shadows·components 를 확인한다
    const theme = createAppTheme()

    // Then
    expect(theme.palette.success.main).toBe('#0E6E52')
    expect(theme.palette.background.default).toBe('#F7F8F9')
    expect(theme.palette.background.paper).toBe('#FFFFFF')
    expect(theme.palette.text.primary).toBe('#1A2027')
    expect(theme.palette.text.secondary).toBe('#5A6570')
    expect(theme.shape.borderRadius).toBe(2)
    expect(theme.shadows.every((shadow) => shadow === 'none')).toBe(true)

    // 배지(Chip)의 border-radius 오버라이드가 2(--radius-badge)다
    expect(theme.components?.MuiChip?.styleOverrides?.root).toMatchObject({ borderRadius: 2 })

    // 카드(Paper outlined variant)의 테두리 색 오버라이드가 '#D3D8DC'(--color-rule)다
    expect(theme.components?.MuiPaper?.styleOverrides?.outlined).toMatchObject({
      borderColor: '#D3D8DC',
    })
  })
})
