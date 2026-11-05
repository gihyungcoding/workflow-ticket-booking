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
    // (TASK-017: 정본이 out/ 시안 토큰으로 바뀌어 기대값을 새 토큰으로 교체 — ADR-0022)

    // When
    // 이 테마 객체의 palette·shape·shadows·typography 를 확인한다
    const theme = createAppTheme()

    // Then
    expect(theme.palette.success.main.toUpperCase()).toBe('#1A8347')
    expect(theme.palette.background.default.toUpperCase()).toBe('#F1F2F6')
    expect(theme.palette.background.paper.toUpperCase()).toBe('#FEFEFE')
    expect(theme.palette.text.primary.toUpperCase()).toBe('#1A1C28')
    expect(theme.palette.text.secondary.toUpperCase()).toBe('#4C4F66')
    expect(theme.shape.borderRadius).toBe(12)
    expect(theme.shadows[1]).toBe('0 2px 8px rgba(26, 28, 40, .08)')

    // 제목(h2)의 행간은 --lh-h2(1.4), 본문(body1)은 --lh-body(1.55), 보조(body2)는 --lh-sub(1.5)다
    expect(theme.typography.h2.lineHeight).toBe(1.4)
    expect(theme.typography.h1.lineHeight).toBe(1.36)
    expect(theme.typography.body1.lineHeight).toBe(1.55)
    expect(theme.typography.body2.lineHeight).toBe(1.5)
  })

  test('SC-01 (happy) 배지·카드에 토큰이 실제로 렌더된다', () => {
    // Given
    // 테마가 적용된 Chip과 outlined Card를 렌더한다
    const theme = createAppTheme()

    // When
    const { getByText, container } = render(
      <ThemeProvider theme={theme}>
        <Chip label="예매 가능" color="success" size="small" />
        <Card>카드 내용</Card>
        <Typography color="textSecondary">보조 텍스트</Typography>
      </ThemeProvider>,
    )
    const chip = getByText('예매 가능').closest('.MuiChip-root')
    const card = container.querySelector('.MuiCard-root')
    const secondaryText = getByText('보조 텍스트')

    // Then
    // 배지(Chip)의 border-radius가 --r-s(6px)로 실제 렌더된다 (MUI 기본 pill 16px이 아니다)
    expect(getComputedStyle(chip!).borderRadius).toBe('6px')

    // 카드(Card)가 --c-line(#E5E6ED) 테두리로 렌더된다
    expect(getComputedStyle(card!).borderColor).toBe('rgb(229, 230, 237)')

    // color="textSecondary" 로 렌더한 텍스트가 --c-text-2(#4C4F66)로 렌더된다
    // (MUI v9에서 color="text.secondary" 점 표기는 매칭되지 않아 본문색으로 새는 회귀가 있었다)
    expect(getComputedStyle(secondaryText).color).toBe('rgb(76, 79, 102)')
  })
})
