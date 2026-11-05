/**
 * TASK-017 테스트의 기대 색값 — 색 리터럴은 theme/ 안에만 둔다(DESIGN-002).
 * 값의 출처는 SCENARIO_TASK-017.md 규약 표(= docs/product/design-tokens.css).
 */
export type ExpectedTone = 'brand' | 'info' | 'danger' | 'warning' | 'success' | 'neutral'

export const TONE_COLORS: Record<ExpectedTone, [string, string]> = {
  brand: ['#EBEDFE', '#4154FF'],
  info: ['#F0EEFE', '#6647FF'],
  danger: ['#FCEFF0', '#D61E24'],
  warning: ['#FBF4E5', '#916500'],
  success: ['#E7F4ED', '#187B43'],
  neutral: ['#F1F2F6', '#4C4F66'],
}

export const EXPECTED = {
  brand: '#4154FF',
  onBrand: '#FFFFFF',
  surface: '#FEFEFE',
  bg: '#F1F2F6',
  danger: '#E1292F',
  dangerTint: '#FCEFF0',
  disabledBg: '#E5E6ED',
  textDisabled: '#9E9FA4',
}

/** '#4154FF' → jsdom 의 계산된 색 표기 'rgb(65, 84, 255)' */
export function computedColor(hex: string): string {
  const value = hex.replace('#', '')
  const n = (i: number) => parseInt(value.slice(i, i + 2), 16)
  return `rgb(${n(0)}, ${n(2)}, ${n(4)})`
}

/** 기존 이름 — theme/ 안의 테스트용 */
export const rgb = computedColor
