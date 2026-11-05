import { act, fireEvent, screen } from '@testing-library/react'
import { StatusBadge } from '../StatusBadge'
import Card from '@mui/material/Card'
import { AppBar, Badge, Block, CtaBar, Field, Row, Sheet, Toast } from './index'
import { GlobalTokens } from '../../theme/GlobalTokens'
import type { BadgeTone } from './index'
import { mockMatchMedia, renderWithTheme } from '../../test/task017'
import { EXPECTED, SURFACE_FILL, TONE_COLORS, computedColor } from '../../theme/task017.expected'

// SoT: workflow_design/05_scenario/SCENARIO_TASK-017.md — 규약 표의 배지 색·토스트 2,500ms

const OLD_LABELS = ['예매가능', '예매예정', '예매마감', '공연취소']
const A = '아이디를 복사했어요'
const INVALID: [string, string | null | undefined][] = [
  ['빈 문자열', ''],
  ['공백만', '   '],
  ['null', null],
  ['undefined', undefined],
]

function badgeColors(text: string): [string, string] {
  const style = getComputedStyle(screen.getByText(text))
  return [style.backgroundColor, style.color]
}

function input(label?: string) {
  return (label ? screen.getByLabelText(label) : screen.getByRole('textbox')) as HTMLInputElement
}

function expectNoFieldError(element: HTMLInputElement) {
  expect(element).not.toHaveAttribute('aria-invalid')
  expect(element).not.toHaveAttribute('aria-describedby')
  expect(document.querySelector('[data-field-error]')).toBeNull()
  expect(getComputedStyle(element).borderColor).not.toBe(computedColor(EXPECTED.danger))
}

function expectFieldError(element: HTMLInputElement, message: string) {
  expect(element).toHaveAttribute('aria-invalid', 'true')
  const describedBy = element.getAttribute('aria-describedby')
  expect(describedBy).toBeTruthy()
  expect(document.getElementById(describedBy!)).toHaveTextContent(message)
  expect(document.getElementById(describedBy!)!.querySelector('svg[aria-hidden="true"]')).not.toBeNull()
  expect(getComputedStyle(element).borderColor).toBe(computedColor(EXPECTED.danger))
}

beforeEach(() => {
  mockMatchMedia(() => false)
})

afterEach(() => {
  vi.useRealTimers()
})

describe('TASK-017 배지', () => {
  test('TASK-017 SC-03 (happy) 공연 상태 배지가 design.md §5 의 뜻·문구·색으로 나온다', () => {
    const rows: [string, string, BadgeTone][] = [
      ['OPEN', '예매 가능', 'brand'],
      ['UPCOMING', '예정', 'info'],
      ['SOLD_OUT', '매진', 'danger'],
      ['CLOSED', '마감', 'neutral'],
      ['CANCELLED', '공연 취소', 'neutral'],
    ]
    for (const [status, label, tone] of rows) {
      const { unmount } = renderWithTheme(<StatusBadge status={status as never} />)
      expect(screen.getByText(label), status).toBeInTheDocument()
      expect(badgeColors(label), status).toEqual(TONE_COLORS[tone].map(computedColor))
      for (const old of OLD_LABELS) expect(screen.queryByText(old)).toBeNull()
      unmount()
    }
  })

  test('TASK-017 SC-04 (happy) Badge 의 tone 여섯 가지가 서로 다른 색이다', () => {
    const rows: [BadgeTone, string][] = [
      ['brand', '예매 가능'],
      ['info', '예정'],
      ['danger', '매진'],
      ['neutral', '마감'],
      ['warning', '입금 대기'],
      ['success', '예매 확정'],
    ]
    renderWithTheme(
      <>
        {rows.map(([tone, label]) => (
          <Badge key={tone} tone={tone}>
            {label}
          </Badge>
        ))}
      </>,
    )
    for (const [tone, label] of rows) {
      expect(badgeColors(label), tone).toEqual(TONE_COLORS[tone].map(computedColor))
    }
    // neutral 을 뺀 다섯 쌍이 서로 다르고, neutral 글자색은 다섯 글자색과 모두 다르다 (neutral 바탕의 실제 색은 SC-19)
    const others = rows.filter(([tone]) => tone !== 'neutral').map(([, label]) => badgeColors(label))
    expect(new Set(others.map((pair) => pair.join('/'))).size).toBe(5)
    const neutralText = badgeColors('마감')[1]
    for (const [, text] of others) expect(text).not.toBe(neutralText)
  })

  test('TASK-017 SC-05 (error) 알 수 없는 상태값이면 배지를 그리지 않는다', () => {
    for (const value of ['POSTPONED', '', '   ', null, undefined]) {
      const { container, unmount } = renderWithTheme(<StatusBadge status={value as never} />)
      expect(container, String(value)).toBeEmptyDOMElement()
      unmount()
    }
  })
})

describe('TASK-017 토스트', () => {
  test('TASK-017 SC-06 (happy) 토스트가 문구를 띄우고 스스로 사라진다', () => {
    vi.useFakeTimers()
    const onClose = vi.fn()
    const message = '아이디를 복사했어요 — 일행에게 붙여넣어 보내세요'
    renderWithTheme(<Toast message={message} onClose={onClose} />)
    expect(screen.getByRole('status')).toHaveTextContent(message)
    act(() => vi.advanceTimersByTime(2499))
    expect(screen.getByText(message)).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
    act(() => vi.advanceTimersByTime(1))
    expect(screen.queryByText(message)).toBeNull()
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  test('TASK-017 SC-07 (error) 빈 문구로는 토스트가 뜨지 않는다', () => {
    vi.useFakeTimers()
    for (const [label, value] of INVALID) {
      const onClose = vi.fn()
      const { unmount } = renderWithTheme(<Toast message={value} onClose={onClose} />)
      expect(screen.queryByRole('status')?.textContent?.trim() ?? '', label).toBe('')
      act(() => vi.advanceTimersByTime(5000))
      expect(onClose, label).not.toHaveBeenCalled()
      unmount()
    }
  })

  test('TASK-017 SC-08 (happy) 움직임 줄이기 설정이면 토스트에 전환 효과가 없다', () => {
    mockMatchMedia((query) => query.includes('prefers-reduced-motion: reduce'))
    const reduced = renderWithTheme(<Toast message={A} />)
    expect(getComputedStyle(screen.getByRole('status')).transition).toBe('none')
    reduced.unmount()

    mockMatchMedia(() => false)
    renderWithTheme(<Toast message={A} />)
    const transition = getComputedStyle(screen.getByRole('status')).transition
    expect(transition).not.toBe('none')
    expect(transition).not.toBe('')
  })

  test('TASK-017 SC-14 (happy) 토스트 타이머는 문구가 바뀌면 새로 세고, 사라지기 전에 닫히면 정리된다', () => {
    vi.useFakeTimers()
    // 1·2행: 문구 A → 2,000ms → B → 2,499ms → +1ms
    const onClose = vi.fn()
    const t = renderWithTheme(<Toast message={A} onClose={onClose} />)
    act(() => vi.advanceTimersByTime(2000))
    t.rerenderWithTheme(<Toast message="B" onClose={onClose} />)
    act(() => vi.advanceTimersByTime(2499))
    expect(screen.getByText('B')).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
    act(() => vi.advanceTimersByTime(1))
    expect(screen.queryByText('B')).toBeNull()
    expect(onClose).toHaveBeenCalledTimes(1)
    t.unmount()

    // 3행: A → 1,000ms → 언마운트 → 5,000ms
    const onClose2 = vi.fn()
    const u = renderWithTheme(<Toast message={A} onClose={onClose2} />)
    act(() => vi.advanceTimersByTime(1000))
    u.unmount()
    act(() => vi.advanceTimersByTime(5000))
    expect(onClose2).not.toHaveBeenCalled()
    expect(vi.getTimerCount()).toBe(0)

    // 4·5행: A → 1,000ms → 빈 문구 / 공백만
    for (const next of ['', '   ']) {
      const onClose3 = vi.fn()
      const r = renderWithTheme(<Toast message={A} onClose={onClose3} />)
      act(() => vi.advanceTimersByTime(1000))
      r.rerenderWithTheme(<Toast message={next} onClose={onClose3} />)
      expect(screen.queryByText(A)).toBeNull()
      expect(onClose3).toHaveBeenCalledTimes(1)
      act(() => vi.advanceTimersByTime(5000))
      expect(onClose3).toHaveBeenCalledTimes(1)
      r.unmount()
    }

    // 6행: 300자 공백 없는 문구 — 줄을 바꾼다
    const long = '가'.repeat(300)
    renderWithTheme(<Toast message={long} />)
    expect(screen.getByText(long)).toBeInTheDocument()
    expect(getComputedStyle(screen.getByRole('status')).overflowWrap).toBe('anywhere')
  })

  test('TASK-017 SC-17 (error) 토스트 문구와 Field 오류의 유효↔무효 전환은 방향과 값마다 같은 규칙이다', () => {
    vi.useFakeTimers()
    for (const [label, value] of INVALID) {
      // Toast A → 무효
      const onClose = vi.fn()
      const t = renderWithTheme(<Toast message={A} onClose={onClose} />)
      act(() => vi.advanceTimersByTime(1000))
      t.rerenderWithTheme(<Toast message={value} onClose={onClose} />)
      expect(screen.queryByText(A), `A→${label}`).toBeNull()
      expect(onClose, `A→${label}`).toHaveBeenCalledTimes(1)
      act(() => vi.advanceTimersByTime(5000))
      expect(onClose, `A→${label} 이후`).toHaveBeenCalledTimes(1)
      t.unmount()

      // Toast 무효 → A
      const onClose2 = vi.fn()
      const u = renderWithTheme(<Toast message={value} onClose={onClose2} />)
      act(() => vi.advanceTimersByTime(1000))
      u.rerenderWithTheme(<Toast message={A} onClose={onClose2} />)
      expect(screen.getByText(A), `${label}→A`).toBeInTheDocument()
      act(() => vi.advanceTimersByTime(2499))
      expect(onClose2, `${label}→A 2,499ms`).not.toHaveBeenCalled()
      act(() => vi.advanceTimersByTime(1))
      expect(screen.queryByText(A)).toBeNull()
      expect(onClose2, `${label}→A 2,500ms`).toHaveBeenCalledTimes(1)
      u.unmount()

      // Field 유효 → 무효
      const f = renderWithTheme(<Field label="이메일" error="이메일을 입력해 주세요" />)
      f.rerenderWithTheme(<Field label="이메일" error={value} />)
      expectNoFieldError(input('이메일'))
      f.unmount()

      // Field 무효 → 유효
      const g = renderWithTheme(<Field label="이메일" error={value} />)
      g.rerenderWithTheme(<Field label="이메일" error="이메일을 입력해 주세요" />)
      expectFieldError(input('이메일'), '이메일을 입력해 주세요')
      g.unmount()
    }
  })
})

describe('TASK-017 공통 부품', () => {
  test('TASK-017 SC-09 (happy) 공통 부품이 토큰 값으로 그려진다', () => {
    const onBack = vi.fn()
    renderWithTheme(
      <>
        <AppBar title="예매 확인" onBack={onBack} />
        <Block title="결제 수단">블록 내용</Block>
        <Row title="행 제목" />
        <CtaBar>가입하기</CtaBar>
        <Field label="이메일" error="이메일을 입력해 주세요" />
        <Sheet open>시트 내용</Sheet>
      </>,
    )
    const header = screen.getByRole('banner')
    expect(getComputedStyle(header).height).toBe('56px')
    expect(getComputedStyle(screen.getByRole('heading', { level: 1 })).fontWeight).toBe('600')
    const back = screen.getByRole('button', { name: '뒤로' })
    expect(getComputedStyle(back).width).toBe('44px')
    expect(getComputedStyle(back).height).toBe('44px')

    const block = screen.getByText('블록 내용').closest('section')!
    expect(getComputedStyle(block).backgroundColor).toBe(computedColor(EXPECTED.surface))
    expect(getComputedStyle(block).borderRadius).toBe('20px')
    expect(getComputedStyle(block).padding).toBe('24px')

    expect(getComputedStyle(screen.getByText('행 제목').closest('[data-row]')!).minHeight).toBe('64px')

    const cta = screen.getByRole('button', { name: '가입하기' })
    expect(getComputedStyle(cta).height).toBe('56px')
    expect(getComputedStyle(cta).backgroundColor).toBe(computedColor(EXPECTED.brand))
    expect(getComputedStyle(cta).color).toBe(computedColor(EXPECTED.onBrand))
    expect(getComputedStyle(cta).borderRadius).toBe('12px')

    const email = input('이메일')
    expect(getComputedStyle(email).borderColor).toBe(computedColor(EXPECTED.danger))
    expect(getComputedStyle(email).backgroundColor).toBe(computedColor(EXPECTED.dangerTint))
    const message = screen.getByText('이메일을 입력해 주세요')
    expect(getComputedStyle(message).color).toBe(computedColor(EXPECTED.danger))
    expect(message.parentElement!.querySelector('svg[aria-hidden="true"]') ?? message.querySelector('svg[aria-hidden="true"]')).not.toBeNull()

    expect(getComputedStyle(screen.getByRole('dialog')).borderRadius).toBe('20px 20px 0 0')

    expectFieldError(email, '이메일을 입력해 주세요')
    fireEvent.click(back)
    expect(onBack).toHaveBeenCalledTimes(1)
  })

  test('TASK-017 SC-15 (happy) 켜졌던 오류·시트가 꺼지면 흔적이 남지 않는다', () => {
    const f = renderWithTheme(<Field label="이메일" error="이메일을 입력해 주세요" />)
    f.rerenderWithTheme(<Field label="이메일" />)
    expect(screen.queryByText('이메일을 입력해 주세요')).toBeNull()
    expectNoFieldError(input('이메일'))
    f.unmount()

    for (const empty of ['', '   ']) {
      const g = renderWithTheme(<Field label="이메일" error={empty} />)
      expectNoFieldError(input('이메일'))
      g.unmount()
    }

    const onClose = vi.fn()
    const s = renderWithTheme(
      <Sheet open onClose={onClose}>
        시트 내용
      </Sheet>,
    )
    fireEvent.click(screen.getByText('시트 내용'))
    expect(onClose).not.toHaveBeenCalled()
    fireEvent.click(screen.getByTestId('sheet-backdrop'))
    expect(onClose).toHaveBeenCalledTimes(1)
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledTimes(2)
    s.rerenderWithTheme(
      <Sheet open={false} onClose={onClose}>
        시트 내용
      </Sheet>,
    )
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  test('TASK-017 SC-16 (error) 부품 prop 의 경계값과 전환·정리가 PLAN prop_schema 대로다', () => {
    vi.useFakeTimers()
    // Toast 빈 문구 → A
    const c1 = vi.fn()
    const t1 = renderWithTheme(<Toast message="" onClose={c1} />)
    act(() => vi.advanceTimersByTime(1000))
    t1.rerenderWithTheme(<Toast message={A} onClose={c1} />)
    act(() => vi.advanceTimersByTime(2499))
    expect(screen.getByText(A)).toBeInTheDocument()
    expect(c1).not.toHaveBeenCalled()
    act(() => vi.advanceTimersByTime(1))
    expect(screen.queryByText(A)).toBeNull()
    expect(c1).toHaveBeenCalledTimes(1)
    t1.unmount()
    // Toast null → A
    const c2 = vi.fn()
    const t2 = renderWithTheme(<Toast message={null} onClose={c2} />)
    t2.rerenderWithTheme(<Toast message={A} onClose={c2} />)
    act(() => vi.advanceTimersByTime(2500))
    expect(c2).toHaveBeenCalledTimes(1)
    t2.unmount()
    // Toast 같은 문구 재렌더는 타이머를 다시 시작하지 않는다
    const c3 = vi.fn()
    const t3 = renderWithTheme(<Toast message={A} onClose={c3} />)
    act(() => vi.advanceTimersByTime(2000))
    t3.rerenderWithTheme(<Toast message={A} onClose={c3} />)
    act(() => vi.advanceTimersByTime(500))
    expect(screen.queryByText(A)).toBeNull()
    expect(c3).toHaveBeenCalledTimes(1)
    t3.unmount()
    // Toast 닫힘 콜백 없이
    const t4 = renderWithTheme(<Toast message={A} />)
    expect(() => act(() => vi.advanceTimersByTime(2500))).not.toThrow()
    expect(screen.queryByText(A)).toBeNull()
    t4.unmount()
    vi.useRealTimers()

    // Field
    const f1 = renderWithTheme(<Field label="이메일" error={null} />)
    expectNoFieldError(input('이메일'))
    f1.unmount()
    const f2 = renderWithTheme(<Field label="이메일" error={undefined} />)
    f2.rerenderWithTheme(<Field label="이메일" error="이메일을 입력해 주세요" />)
    expectFieldError(input('이메일'), '이메일을 입력해 주세요')
    f2.unmount()
    const f3 = renderWithTheme(<Field label="이메일" />)
    expect(screen.getByLabelText('이메일')).toBeInTheDocument()
    f3.unmount()

    // Sheet 닫힌 뒤 / 언마운트 뒤 Esc, 콜백 없음
    const s1c = vi.fn()
    const s1 = renderWithTheme(<Sheet open onClose={s1c}>내용</Sheet>)
    s1.rerenderWithTheme(<Sheet open={false} onClose={s1c}>내용</Sheet>)
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(s1c).not.toHaveBeenCalled()
    s1.unmount()
    const s2c = vi.fn()
    const s2 = renderWithTheme(<Sheet open onClose={s2c}>내용</Sheet>)
    s2.unmount()
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(s2c).not.toHaveBeenCalled()
    const s3 = renderWithTheme(<Sheet open>내용</Sheet>)
    expect(() => {
      fireEvent.click(screen.getByTestId('sheet-backdrop'))
      fireEvent.keyDown(document, { key: 'Escape' })
    }).not.toThrow()
    s3.unmount()

    // AppBar
    const a1 = renderWithTheme(<AppBar title="예매 확인" />)
    expect(screen.queryByRole('button', { name: '뒤로' })).toBeNull()
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('예매 확인')
    a1.unmount()
    const a2 = renderWithTheme(<AppBar onBack={() => {}} />)
    expect(screen.getByRole('button', { name: '뒤로' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('')
    a2.unmount()

    // CtaBar
    const click1 = vi.fn()
    const b1 = renderWithTheme(
      <CtaBar disabled onClick={click1}>
        가입하기
      </CtaBar>,
    )
    const disabled = screen.getByRole('button', { name: '가입하기' })
    expect(disabled).toBeDisabled()
    fireEvent.click(disabled)
    expect(click1).not.toHaveBeenCalled()
    expect(getComputedStyle(disabled).backgroundColor).toBe(computedColor(EXPECTED.disabledBg))
    expect(getComputedStyle(disabled).color).toBe(computedColor(EXPECTED.textDisabled))
    b1.unmount()
    const click2 = vi.fn()
    renderWithTheme(<CtaBar onClick={click2}>가입하기</CtaBar>)
    fireEvent.click(screen.getByRole('button', { name: '가입하기' }))
    expect(click2).toHaveBeenCalledTimes(1)

    // Badge 무효 문구
    for (const [label, value] of INVALID) {
      const { container, unmount } = renderWithTheme(<Badge tone="brand">{value}</Badge>)
      expect(container, label).toBeEmptyDOMElement()
      unmount()
    }
  })

  test('TASK-017 SC-18 (error) 문구가 비었거나 길어도 부품이 칸을 벗어나지 않는다', () => {
    for (const [label, value] of INVALID) {
      const { container, unmount } = renderWithTheme(<Field label={value} />)
      expect(screen.getByRole('textbox'), label).toBeInTheDocument()
      expect(container.querySelector('label'), label).toBeNull()
      unmount()
    }
    const longLabel = '가'.repeat(100)
    const l = renderWithTheme(<Field label={longLabel} />)
    expect(getComputedStyle(screen.getByText(longLabel)).overflowWrap).toBe('anywhere')
    l.unmount()
    const longError = '가'.repeat(300)
    const e = renderWithTheme(<Field label="이메일" error={longError} />)
    expect(getComputedStyle(screen.getByText(longError)).overflowWrap).toBe('anywhere')
    e.unmount()
    for (const title of ['', '   ']) {
      const a = renderWithTheme(<AppBar title={title} onBack={() => {}} />)
      expect(screen.getByRole('heading', { level: 1 }).textContent?.trim()).toBe('')
      expect(screen.getByRole('button', { name: '뒤로' })).toBeInTheDocument()
      a.unmount()
    }
    const longTitle = '가'.repeat(60)
    const t = renderWithTheme(<AppBar title={longTitle} onBack={() => {}} />)
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(longTitle)
    expect(getComputedStyle(screen.getByRole('heading', { level: 1 })).whiteSpace).not.toBe('nowrap')
    t.unmount()
    const longBadge = '가'.repeat(40)
    renderWithTheme(<Badge tone="neutral">{longBadge}</Badge>)
    const badge = getComputedStyle(screen.getByText(longBadge))
    expect(badge.whiteSpace).toBe('nowrap')
    expect(badge.maxWidth).toBe('100%')
    expect(badge.textOverflow).toBe('ellipsis')
  })
})

describe('TASK-017 면에 따른 모양 (Phase 4 RC-1)', () => {
  function fillOf(element: Element) {
    return getComputedStyle(element).getPropertyValue('--c-fill').trim().toUpperCase()
  }

  test('TASK-017 SC-19 (happy) 회색 배지는 놓인 면과 다른 색이라 모양이 보인다', () => {
    const rows: [string, (badge: React.ReactElement) => React.ReactElement, () => Element, string, string][] = [
      ['페이지', (b) => <><GlobalTokens />{b}</>, () => document.body, EXPECTED.bg, SURFACE_FILL.page],
      ['Block', (b) => <Block>{b}</Block>, () => document.querySelector('section')!, EXPECTED.surface, SURFACE_FILL.block],
      ['Sheet', (b) => <Sheet open>{b}</Sheet>, () => screen.getByRole('dialog'), EXPECTED.surface, SURFACE_FILL.sheet],
      ['Card', (b) => <Card>{b}</Card>, () => document.querySelector('.MuiCard-root')!, EXPECTED.surface, SURFACE_FILL.card],
    ]
    for (const [name, wrap, surfaceOf, surfaceBg, fill] of rows) {
      const { unmount } = renderWithTheme(wrap(<Badge tone="neutral">마감</Badge>))
      expect(badgeColors('마감'), name).toEqual(TONE_COLORS.neutral.map(computedColor))
      const surface = surfaceOf()
      expect(fillOf(surface), name).toBe(fill)
      expect(getComputedStyle(surface).backgroundColor, name).toBe(computedColor(surfaceBg))
      expect(computedColor(fill), name).not.toBe(computedColor(surfaceBg))
      unmount()
    }
  })

  test('TASK-017 SC-20 (error) 좁은 줄에 놓여도 짧은 배지 문구는 잘리지 않는다', () => {
    const longTitle = '가'.repeat(60)
    const rows: [string, React.ReactElement][] = [
      ['예매 가능', <StatusBadge status="OPEN" />],
      ['공연 취소', <StatusBadge status="CANCELLED" />],
      ['가'.repeat(40), <Badge tone="neutral">{'가'.repeat(40)}</Badge>],
    ]
    for (const [text, badge] of rows) {
      const { unmount } = renderWithTheme(
        <div style={{ display: 'flex' }}>
          <span>{longTitle}</span>
          {badge}
        </div>,
      )
      const style = getComputedStyle(screen.getByText(text))
      expect(style.flexShrink, text).toBe('0')
      expect(style.maxWidth, text).toBe('100%')
      expect(style.textOverflow, text).toBe('ellipsis')
      unmount()
    }
  })

  test('TASK-017 SC-21 (happy) 토스트가 떠 있어도 아래 화면을 누를 수 있다', () => {
    renderWithTheme(<Toast message={A} />)
    expect(getComputedStyle(screen.getByRole('status')).pointerEvents).toBe('none')
  })

  test('TASK-017 SC-22 (happy) 앱바 좌우 칸은 시안처럼 최소 48px 이고 내용만큼 늘어난다', () => {
    renderWithTheme(<AppBar title="예매 확인" onBack={() => {}} />)
    expect(getComputedStyle(screen.getByRole('banner')).gridTemplateColumns).toBe('minmax(48px, auto) 1fr minmax(48px, auto)')
    const back = screen.getByRole('button', { name: '뒤로' })
    expect(getComputedStyle(back).width).toBe('44px')
    expect(getComputedStyle(back).height).toBe('44px')
  })

  test('TASK-017 SC-23 (happy) 행 앞 아이콘 칸과 입력칸의 회색 바탕도 놓인 면을 따른다', () => {
    const row = renderWithTheme(
      <Block>
        <Row lead={<span data-testid="lead-icon" />} title="행 제목" />
      </Block>,
    )
    expect(getComputedStyle(screen.getByTestId('lead-icon').parentElement!).backgroundColor).toBe('var(--c-fill)')
    row.unmount()
    renderWithTheme(
      <Block>
        <Field label="이메일" />
      </Block>,
    )
    expect(getComputedStyle(input('이메일')).backgroundColor).toBe('var(--c-fill)')
  })
})
