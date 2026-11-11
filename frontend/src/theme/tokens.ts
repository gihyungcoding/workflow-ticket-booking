/**
 * docs/product/design-tokens.css 의 값을 그대로 미러링한다 (값의 출처 out/tokens.css — ADR-0022).
 * 정본은 그 파일이다 — 값을 고치면 그 파일부터 고치고 여기로 옮긴다. 새 색·새 여백 값을 만들지 않는다.
 * task017.theme.test.tsx 가 두 파일을 한 글자씩 대조한다.
 */

export const designTokens = {
  '--c-bg': '#F1F2F6',
  '--c-surface': '#FEFEFE',
  '--c-line': '#E5E6ED',
  '--c-text-1': '#1A1C28',
  '--c-text-2': '#4C4F66',
  '--c-text-3': '#696C86',
  '--c-brand': '#4154FF',
  '--c-on-brand': '#FFFFFF',
  '--c-brand-line': '#4154FF',
  '--c-brand-text': '#4154FF',
  '--c-brand-tint': '#EBEDFE',
  '--c-brand-tint-text': '#4154FF',
  '--c-danger': '#E1292F',
  '--c-danger-tint': '#FCEFF0',
  '--c-danger-tint-text': '#D61E24',
  '--c-accent': '#4154FF',
  '--c-accent-text': '#4154FF',
  '--c-success': '#1A8347',
  '--c-success-tint': '#E7F4ED',
  '--c-success-tint-text': '#187B43',
  '--c-warning': '#9B6C00',
  '--c-warning-tint': '#FBF4E5',
  '--c-warning-tint-text': '#916500',
  '--c-info': '#7257FF',
  '--c-info-tint': '#F0EEFE',
  '--c-info-tint-text': '#6647FF',
  '--c-draft': '#0B8086',
  '--c-draft-tint': '#ECF7F8',
  '--c-draft-tint-text': '#0B7D82',
  '--c-brand-pressed': '#3C4DE5',
  '--c-focus': '#4154FF',
  '--c-line-strong': '#BEBEC2',
  '--c-text-disabled': '#9E9FA4',
  '--c-disabled-bg': '#E5E6ED',
  '--c-overlay': 'rgba(26, 28, 40, .45)',
  '--shadow-1': '0 2px 8px rgba(26, 28, 40, .08)',
  '--shadow-2': '0 8px 24px rgba(26, 28, 40, .14)',
  '--font': "'Pretendard', -apple-system, BlinkMacSystemFont, sans-serif",
  '--tracking': '-0.02em',
  '--fs-title': '28px',
  '--fw-title': '700',
  '--lh-title': '1.36',
  '--fs-h2': '20px',
  '--fw-h2': '700',
  '--lh-h2': '1.4',
  '--fs-body': '16px',
  '--fw-body': '400',
  '--lh-body': '1.55',
  '--fs-strong': '16px',
  '--fw-strong': '600',
  '--lh-strong': '1.5',
  '--fs-sub': '14px',
  '--fw-sub': '400',
  '--lh-sub': '1.5',
  '--fs-caption': '12px',
  '--fw-caption': '500',
  '--lh-caption': '1.4',
  '--page-x': '24px',
  '--block-pad': '24px',
  '--block-gap': '12px',
  '--row-py': '16px',
  '--stack-s': '8px',
  '--stack-m': '16px',
  '--stack-l': '32px',
  '--r-s': '6px',
  '--r-m': '12px',
  '--r-l': '20px',
  '--r-pill': '999px',
  '--h-appbar': '56px',
  '--h-btn': '56px',
  '--h-btn-s': '44px',
  '--h-tab': '64px',
  '--h-row': '64px',
  '--icon': '24px',
} as const

/** .text-large — 큰 글자 설정에서 덮어쓰는 글자 크기 */
export const textLargeTokens = {
  '--fs-title': '28px',
  '--fw-title': '700',
  '--lh-title': '1.36',
  '--fs-h2': '20px',
  '--fw-h2': '700',
  '--lh-h2': '1.4',
  '--fs-body': '18px',
  '--fw-body': '400',
  '--lh-body': '1.55',
  '--fs-strong': '18px',
  '--fw-strong': '600',
  '--lh-strong': '1.5',
  '--fs-sub': '16px',
  '--fw-sub': '400',
  '--lh-sub': '1.5',
  '--fs-caption': '16px',
  '--fw-caption': '500',
  '--lh-caption': '1.4',
} as const

export type TokenName = keyof typeof designTokens

/** 토큰 값 하나 */
export function token(name: TokenName): string {
  return designTokens[name]
}

/** 토큰의 px 값을 숫자로 (MUI 가 숫자를 받는 자리용) */
export function px(name: TokenName): number {
  return parseFloat(designTokens[name])
}

/**
 * 부품 고정 치수 — design-tokens.css 에는 없고 시안에만 있는 값 (새 값이 아니다).
 * 출처: out/components.css (줄 번호), 한 열 폭은 out/screens/*.html 의 meta viewport.
 */
export const componentSizes = {
  touch: '44px', // .appbar-btn·.row-lead 44px (components.css 37·62행)
  appbarSide: '48px', // .appbar grid-template-columns minmax(48px, auto) (34행)
  badgeHeight: '24px', // .badge height (76행)
  rowGap: '4px', // .row-main gap (66행)
  iconSmall: '20px', // .btn svg·.row-trail svg (70·88행)
  appMaxWidth: '375px', // 시안 뷰포트 width=375
  waitArt: '72px', // oauth-callback .wait-art 72px (screens/oauth-callback.html)
  waitSpin: '32px', // oauth-callback .wait-spin 32px
  waitSpinBorder: '3px', // .wait-spin border 3px
  rowArt: '36px', // .row-art img 36px (components.css 165행)
  moreIcon: '18px', // .hero-more svg 18px (47행)
  fieldMsgIcon: '18px', // .field-msg svg 18px (130행)
  moreGap: '2px', // .hero-more gap 2px (46행)
  titleTracking: '-0.03em', // .title letter-spacing (43행)
  idLineHeight: '1.15', // joined .id-value line-height (screens/joined.html)
} as const

/** 토스트가 사라지기까지 — 동작 값(디자인 토큰 아님, PLAN unresolved #4 확정) */
export const TOAST_DURATION_MS = 2500
