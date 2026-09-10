import { render } from '@testing-library/react'
import Card from '@mui/material/Card'
import Chip from '@mui/material/Chip'
import Typography from '@mui/material/Typography'
import { ThemeProvider } from '@mui/material/styles'
import { createAppTheme } from './index'

describe('createAppTheme', () => {
  test('SC-01 (happy) 테마가 토큰 값으로 초기화된다', () => {
    // Given
    // frontend/src/theme/index.ts 가 docs/product/design-tokens.css 의 값으로 MUI 테마를 만든다

    // When
    // 이 테마 객체의 palette·shape·shadows·typography 를 확인한다
    const theme = createAppTheme()

    // Then
    expect(theme.palette.success.main).toBe('#0E6E52')
    expect(theme.palette.background.default).toBe('#F7F8F9')
    expect(theme.palette.background.paper).toBe('#FFFFFF')
    expect(theme.palette.text.primary).toBe('#1A2027')
    expect(theme.palette.text.secondary).toBe('#5A6570')
    expect(theme.shape.borderRadius).toBe(2)
    expect(theme.shadows.every((shadow) => shadow === 'none')).toBe(true)

    // 공연명(h5/h6)의 행간은 --line-height-display(1.35), 본문(body1/body2)은 --line-height-text(1.6)다
    expect(theme.typography.h5.lineHeight).toBe(1.35)
    expect(theme.typography.h6.lineHeight).toBe(1.35)
    expect(theme.typography.body1.lineHeight).toBe(1.6)
    expect(theme.typography.body2.lineHeight).toBe(1.6)
  })

  test('SC-01 (happy) 배지·카드에 토큰이 실제로 렌더된다', () => {
    // Given
    // 테마가 적용된 Chip과 outlined Card를 렌더한다
    const theme = createAppTheme()

    // When
    const { getByText, container } = render(
      <ThemeProvider theme={theme}>
        <Chip label="예매가능" color="success" size="small" />
        <Card>카드 내용</Card>
        <Typography color="textSecondary">보조 텍스트</Typography>
      </ThemeProvider>,
    )
    const chip = getByText('예매가능').closest('.MuiChip-root')
    const card = container.querySelector('.MuiCard-root')
    const secondaryText = getByText('보조 텍스트')

    // Then
    // 배지(Chip)의 border-radius가 --radius-badge(2px)로 실제 렌더된다 (MUI 기본 pill 16px이 아니다)
    expect(getComputedStyle(chip!).borderRadius).toBe('2px')

    // 카드(Card)가 --color-rule(#D3D8DC) 테두리로 렌더된다
    expect(getComputedStyle(card!).borderColor).toBe('rgb(211, 216, 220)')

    // color="textSecondary" 로 렌더한 텍스트가 --color-ink-muted(#5A6570)로 렌더된다
    // (MUI v9에서 color="text.secondary" 점 표기는 매칭되지 않아 본문색으로 새는 회귀가 있었다)
    expect(getComputedStyle(secondaryText).color).toBe('rgb(90, 101, 112)')
  })
})
