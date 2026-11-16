import { fireEvent, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
  DETAIL,
  P_OPEN,
  P_UP,
  SECTIONS_P,
  TIME,
  browserBack,
  css,
  isTokenColor,
  renderAt,
  res,
  resetDocumentVisibility,
  squash,
  stubServer,
  timeRoute,
  where,
} from '../test/task020'
import type { Section } from '../test/task020'
import { token } from '../theme/tokens'

// TASK-020 시나리오 — 구역 위치 시트. SoT: workflow_design/05_scenario/SCENARIO_TASK-020.md
// 묶음 행은 Row 부품의 data-row 로 찾고, 행의 글자는 제목·설명·가격이 이어진 것(textContent)으로 단언한다.
// 포스터 칸은 data-testid="poster" 를 단다고 약속한다(task020.detail.test.tsx 와 같다).

const ARIA_P =
  '고척스카이돔 구역 위치. 무대 바로 앞부터 F1, F2, F3, F4 순서이고, 바깥으로 1층 지정석, 2층 지정석 순서예요'
const LIST = { 'GET /api/performances': () => res(200, { content: [], page: 0, size: 20, totalElements: 0 }) }

beforeEach(() => {
  localStorage.clear()
})
afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  resetDocumentVisibility()
  localStorage.clear()
})

/** [묶음 이름, 등급들] 목록 → sections (등급마다 한 구역, stageOrder 는 이어서 1, 2, …) */
function bundles(spec: Array<[string, string[]]>, price = 100000): Section[] {
  let order = 0
  return spec.flatMap(([area, grades]) =>
    grades.map((grade) => ({ grade, price, seatCount: 10, area, stageOrder: ++order })),
  )
}

async function openSheet(sections: Section[] | unknown, entries: string[] = ['/performances/7'], strict = false) {
  stubServer({ [DETAIL]: () => res(200, P_OPEN({ sections })), ...LIST })
  renderAt(entries, { strict })
  await screen.findByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })
  await userEvent.click(screen.getByText('구역 위치 보기'))
  return screen.getByRole('dialog', { name: '구역 위치' })
}

const rowsOf = (dialog: HTMLElement) => Array.from(dialog.querySelectorAll('[data-row]')).map((r) => r.textContent)
const labelsOf = (map: Element) => Array.from(map.querySelectorAll('text')).map((t) => t.textContent)

describe('TASK-020 — 구역 위치 시트', () => {
  test('SC-19 (happy) 「구역 위치 보기」를 누르면 시트가 열리고 배치도와 묶음 행이 stageOrder 순으로 보인다', async () => {
    // Given P_OPEN 상세가 보이고 시트는 닫혀 있다
    stubServer({ [DETAIL]: () => res(200, P_OPEN()), ...LIST })
    renderAt('/performances/7')
    await screen.findByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    // When 「구역 위치 보기」 행을 누른다
    await userEvent.click(screen.getByText('구역 위치 보기'))

    // Then 이름이 「구역 위치」 인 dialog 가 열린다
    const dialog = screen.getByRole('dialog', { name: '구역 위치' })
    // And 그 안에 이름이 배치도 설명인 이미지(role="img")가 하나 있다
    const maps = within(dialog).getAllByRole('img')
    expect(maps).toHaveLength(1)
    expect(maps[0]).toHaveAccessibleName(ARIA_P)
    // And 그 이미지 안의 글자는 무대·F1~F4·1층·2층 이 이 순서로 나타난다
    expect(labelsOf(maps[0])).toEqual(['무대', 'F1 · 무대 바로 앞', 'F2', 'F3', 'F4 · 맨 뒤', '1층 지정석', '2층 지정석'])
    // And 묶음 행이 세 개이고 순서·내용은 시안과 같다
    expect(rowsOf(dialog)).toEqual([
      '플로어 F1~F4무대 앞 바닥, F1이 가장 앞150,000원',
      '1층 지정석플로어를 둘러싼 아래층132,000원',
      '2층 지정석가장 바깥 위층110,000원',
    ])
    // And 「확인」 버튼이 있다
    expect(within(dialog).getByRole('button', { name: '확인' })).toBeInTheDocument()
    // And 주소는 /performances/7 그대로이고 뒤의 상세 화면(h1)도 그대로다
    expect(where()).toBe('/performances/7')
    expect(screen.getByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })).toBeInTheDocument()
  })

  test.each([
    ['a', '「확인」 버튼', async () => userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: '확인' }))],
    ['b', '흐린 바탕', async () => userEvent.click(screen.getByTestId('sheet-backdrop'))],
    ['c', 'Escape 키', async () => void fireEvent.keyDown(document, { key: 'Escape' })],
    ['d', '브라우저 뒤로 가기', async () => browserBack()],
  ])('SC-20 (happy) 시트는 확인·흐린 바탕·Esc·뒤로 가기로 닫히고 상세를 떠나지 않는다 — %s %s', async (_row, _label, close) => {
    // Given 홈(/)에서 /performances/7 로 들어왔고 P_OPEN 상세에서 시트가 열려 있다
    await openSheet(SECTIONS_P, ['/', '/performances/7'])

    // When 표의 방법으로 닫는다
    await close()

    // Then dialog 가 사라진다
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    // And 주소는 /performances/7 이다 — 이전 화면(홈)으로 가지 않는다
    expect(where()).toBe('/performances/7')
    // And 상세 화면(h1 「지금 예매할 수 있어요」)이 그대로 있다
    expect(screen.getByRole('heading', { level: 1, name: '지금 예매할 수 있어요' })).toBeInTheDocument()
  })

  const FLOOR2 = ['플로어', ['F1', 'F2']] as [string, string[]]
  test.each([
    ['a', [['플로어', ['F1', 'F2', 'F3']]], ['무대', 'F1 · 무대 바로 앞', 'F2', 'F3 · 맨 뒤'], 1],
    ['b', [FLOOR2, ['1층 지정석', ['1층']]], ['무대', 'F1 · 무대 바로 앞', 'F2 · 맨 뒤', '1층 지정석'], 2],
    ['c', null, ['무대', 'F1 · 무대 바로 앞', 'F2', 'F3', 'F4 · 맨 뒤', '1층 지정석', '2층 지정석'], 3],
    ['d', [FLOOR2, ['A', ['A']], ['B', ['B']], ['C', ['C']]], ['무대', 'F1 · 무대 바로 앞', 'F2 · 맨 뒤', 'A', 'B', 'C'], 4],
    ['e', [FLOOR2, ['A', ['A']], ['B', ['B']], ['C', ['C']], ['D', ['D']]], null, 5],
    ['f', [['가', ['가']], ['나', ['나']], ['다', ['다']], ['라', ['라']], ['마', ['마']], ['바', ['바']]], null, 6],
  ] as Array<[string, Array<[string, string[]]> | null, string[] | null, number]>)(
    'SC-21 (boundary) 묶음 개수에 따라 배치도의 고리가 늘고 5개 이상이면 배치도 없이 행만 보인다 — %s',
    async (_row, spec, texts, rowCount) => {
      // Given P_OPEN 에서 sections 를 표의 묶음 구성으로 바꾸고 시트를 연다
      // When 「구역 위치 보기」 행을 누른다
      const dialog = await openSheet(spec ? bundles(spec) : SECTIONS_P)

      // Then 배치도 이미지(role="img")는 표대로 있거나 없다
      if (texts) {
        const map = within(dialog).getByRole('img')
        // And 배치도가 있으면 그 안 글자 순서는 무대, 첫 묶음의 등급(위→아래), 이어서 바깥 묶음 이름(안→바깥)이다
        expect(labelsOf(map)).toEqual(texts)
        // And 배치도의 max-height 는 280px, width 는 100% 다
        expect(css(map, 'max-height')).toBe('280px')
        expect(css(map, 'width')).toBe('100%')
      } else {
        expect(within(dialog).queryByRole('img')).not.toBeInTheDocument()
      }
      // And 묶음 행의 개수는 표와 같다
      expect(dialog.querySelectorAll('[data-row]')).toHaveLength(rowCount)
    },
  )

  // ---------- SC-22 ----------

  const noNote = (rows: Array<[string, string, number, number]>): Section[] =>
    rows.map(([area, grade, price, stageOrder]) => ({ grade, price, seatCount: 10, area, stageOrder }))
  const withNote = (note: string): Section[] =>
    ['가', '나', '다', '라', '마'].map((g, i) => ({ grade: g, price: 1000, seatCount: 10, area: `구역${g}`, areaNote: note, stageOrder: i + 1 }))
  const BLANKS = ['', '   ', '　', ' ', '​']
  test.each([
    ['a', 'VIP·R·S 미등록', noNote([['VIP', 'VIP', 150000, 1], ['R', 'R', 120000, 2], ['S', 'S', 90000, 3]]), ['VIP150,000원', 'R120,000원', 'S90,000원']],
    ['b', 'areaNote 공백들', null, ['구역가1,000원', '구역나1,000원', '구역다1,000원', '구역라1,000원', '구역마1,000원']],
    ['c', '섞인 응답 순서 4·2·1·3', noNote([['D', 'd', 1000, 4], ['B', 'b', 1000, 2], ['A', 'a', 1000, 1], ['C', 'c', 1000, 3]]), ['A1,000원', 'B1,000원', 'C1,000원', 'D1,000원']],
    ['d', 'stageOrder 동률 2,2,1', noNote([['X', 'x', 1000, 2], ['Y', 'y', 1000, 2], ['W', 'w', 1000, 1]]), ['W1,000원', 'X1,000원', 'Y1,000원']],
    ['e', 'stageOrder 0·-3·1', noNote([['P0', 'p0', 1000, 0], ['M3', 'm3', 1000, -3], ['P1', 'p1', 1000, 1]]), ['M31,000원', 'P01,000원', 'P11,000원']],
    ['f', 'price 0', noNote([['Z0', 'z0', 0, 1], ['Z1', 'z1', 1000, 2]]), ['Z00원', 'Z11,000원']],
    ['g', '같은 area 의 가격이 다름', noNote([['플로어', 'F1', 150000, 1], ['플로어', 'F2', 130000, 2]]), ['플로어 F1~F2130,000~150,000원']],
    ['h', 'area 가 떨어져 나옴 A·B·A', noNote([['A', 'a1', 1000, 1], ['B', 'b1', 1000, 2], ['A', 'a2', 1000, 3]]), ['A1,000원', 'B1,000원', 'A1,000원']],
  ] as Array<[string, string, Section[] | null, string[]]>)('SC-22 (boundary) 구역 위치 미등록·설명 공백·섞인 순서·0 원 같은 값도 묶음 행이 규칙대로 보인다 — %s %s', async (row, _label, sections, expected) => {
    // Given P_OPEN 에서 sections 를 표처럼 바꾸고 시트를 연다
    // When 「구역 위치 보기」 행을 누른다
    if (row === 'b') {
      // 다섯 묶음 각각이 표의 공백 설명을 갖는다 — 한 응답에 다섯 구역(area 서로 다름)
      const mixed = withNote('x').map((s, i) => ({ ...s, areaNote: BLANKS[i] }))
      const dialog = await openSheet(mixed)
      expect(rowsOf(dialog)).toEqual(expected)
    } else {
      const dialog = await openSheet(sections)
      // Then 묶음 행은 표의 제목·설명·가격 그대로이고 순서는 표와 같다
      expect(rowsOf(dialog)).toEqual(expected)
    }
    // And 화면은 오류 화면이 아니다
    expect(screen.queryByText('공연 정보를 불러오지 못했어요')).not.toBeInTheDocument()
  })

  // ---------- SC-23 ----------

  const G = (n: number) => '가'.repeat(n)
  const A = (n: number) => '나'.repeat(n)
  const N = (n: number) => '다'.repeat(n)
  const change = (index: number, over: Partial<Section>): Section[] => SECTIONS_P.map((s, i) => (i === index ? { ...s, ...over } : s))
  test.each([
    ['a', change(0, { grade: G(20) }), `${G(20)}`, `${G(11)}… · 무대 바로 앞`],
    ['b', change(0, { grade: G(21) }), `${G(21)}`, `${G(11)}… · 무대 바로 앞`],
    ['c', change(4, { area: A(30) }), `${A(30)}`, `${A(11)}…`],
    ['d', change(4, { area: A(31) }), `${A(31)}`, `${A(11)}…`],
    ['e', change(4, { areaNote: N(60) }), `${N(60)}`, null],
    ['f', change(4, { areaNote: N(61) }), `${N(61)}`, null],
    ['g', change(0, { grade: G(12) }), `${G(12)}`, `${G(12)} · 무대 바로 앞`],
  ] as Array<[string, Section[], string, string | null]>)('SC-23 (boundary) 이름이 길어도 묶음 행에는 전부 나오고 배치도 라벨만 줄여 쓴다 — %s', async (_row, sections, full, label) => {
    // Given P_OPEN 에서 sections 를 표처럼 바꾸고 시트를 연다
    // When 「구역 위치 보기」 행을 누른다
    const dialog = await openSheet(sections)
    const map = within(dialog).getByRole('img')

    // Then 묶음 행에는 바꾼 값 전체가 잘리지 않고 그대로 있다
    expect(rowsOf(dialog).some((text) => text?.includes(full))).toBe(true)
    // And 배치도 안 글자 중 등급·바깥 묶음 이름은 열두 글자를 넘으면 앞 열한 글자 + 「…」, 이하는 그대로다
    const labels = labelsOf(map)
    if (label) expect(labels).toContain(label)
    // And areaNote 는 배치도 안에 나오지 않는다
    expect(labels.some((text) => text?.includes('다다다'))).toBe(false)
    // And 배치도의 접근성 이름에는 줄이지 않은 전체 이름이 있다
    if (!full.startsWith('다')) expect(map.getAttribute('aria-label')).toContain(full)
    // And 화면은 오류 화면이 아니다
    expect(screen.queryByText('공연 정보를 불러오지 못했어요')).not.toBeInTheDocument()
  })

  // ---------- SC-24 ----------

  test.each([
    ['a', SECTIONS_P, 'F1 무대 바로 앞 → F4 맨 뒤'],
    ['b', bundles([['플로어', ['F1']], ['1층 지정석', ['1층']], ['2층 지정석', ['2층']]]), 'F1 무대 바로 앞 → 2층 맨 뒤'],
    ['c', bundles([['VIP', ['VIP']], ['R', ['R']], ['S', ['S']]]), 'VIP 무대 바로 앞 → S 맨 뒤'],
    ['d', bundles([['플로어', ['A', 'B']]]), 'A 무대 바로 앞 → B 맨 뒤'],
  ] as Array<[string, Section[], string]>)('SC-24 (happy) 「구역 위치 보기」 행의 아랫줄이 첫 묶음 또는 전체 구역의 앞·뒤를 말한다 — %s', async (_row, sections, line) => {
    // Given P_OPEN 에서 sections 를 표처럼 바꾼다
    stubServer({ [DETAIL]: () => res(200, P_OPEN({ sections })) })

    // When /performances/7 을 연다
    renderAt('/performances/7')

    // Then 「구역 위치 보기」 행의 아랫줄이 표의 문구다
    expect(await screen.findByText(line)).toBeInTheDocument()
  })

  // ---------- SC-33 ----------

  test('SC-33 (regression) 상세·시트의 렌더된 값이 시안의 규칙과 같다', async () => {
    // Given P_UP(카운트다운 중) 상세를 열고 시트를 열어 둔다 (포스터 이미지는 로드된다)
    const openAt = new Date(Date.now() + 272_000).toISOString()
    stubServer({ [DETAIL]: () => res(200, P_UP({ openAt })), [TIME]: timeRoute(), ...LIST })
    renderAt('/performances/7')
    await screen.findByRole('timer')
    await userEvent.click(screen.getByText('구역 위치 보기'))

    // When 렌더가 끝난다
    const dialog = screen.getByRole('dialog', { name: '구역 위치' })

    // Then 카운트다운 — font-weight 800, tabular-nums, --c-brand-text, block
    const clock = screen.getByRole('timer')
    expect(css(clock, 'font-weight')).toBe('800')
    expect(css(clock, 'font-variant-numeric')).toBe('tabular-nums')
    expect(isTokenColor(css(clock, 'color'), '--c-brand-text')).toBe(true)
    expect(css(clock, 'display')).toBe('block')
    // And 카운트다운 font-size 는 --fs-title 의 1.75배다
    expect(css(clock, 'font-size')).toBe(`${parseFloat(token('--fs-title')) * 1.75}px`)
    // And 가격 줄 — --fs-h2, 700
    const price = screen.getByText('110,000~150,000원')
    expect(css(price, 'font-size')).toBe(token('--fs-h2'))
    expect(css(price, 'font-weight')).toBe('700')
    // And 포스터 칸 — 4/3, hidden, --c-bg
    const frame = screen.getByTestId('poster')
    expect(squash(css(frame, 'aspect-ratio'))).toBe('4/3')
    expect(css(frame, 'overflow')).toBe('hidden')
    expect(isTokenColor(css(frame, 'background-color'), '--c-bg')).toBe(true)
    // And 꺼진 CTA — --c-disabled-bg, --c-text-disabled
    const cta = screen.getByRole('button', { name: /^예매하기 · / })
    expect(isTokenColor(css(cta, 'background-color'), '--c-disabled-bg')).toBe(true)
    expect(isTokenColor(css(cta, 'color'), '--c-text-disabled')).toBe(true)
    // And 시트 바탕(흐린 막) — z-index 20, --c-overlay
    const backdrop = screen.getByTestId('sheet-backdrop')
    expect(css(backdrop, 'z-index')).toBe('20')
    expect(isTokenColor(css(backdrop, 'background-color'), '--c-overlay')).toBe(true)
    // And 시트 본체 — z-index 21, --c-surface
    expect(css(dialog, 'z-index')).toBe('21')
    expect(isTokenColor(css(dialog, 'background-color'), '--c-surface')).toBe(true)
    // And 시트 안 묶음 행의 가격 — --c-text-1, 700
    const sheetPrice = within(dialog).getByText('150,000원')
    expect(isTokenColor(css(sheetPrice, 'color'), '--c-text-1')).toBe(true)
    expect(css(sheetPrice, 'font-weight')).toBe('700')
    // And 배치도 도형 — 플로어(--c-brand-tint·--c-brand-line), 1층(--c-bg·--c-line-strong), 2층(--c-surface·--c-line-strong·4 3)
    const shapes = Array.from(within(dialog).getByRole('img').querySelectorAll('rect, path'))
    const fill = (el: Element) => css(el, 'fill')
    const stroke = (el: Element) => css(el, 'stroke')
    expect(shapes.some((el) => isTokenColor(fill(el), '--c-brand-tint') && isTokenColor(stroke(el), '--c-brand-line'))).toBe(true)
    expect(shapes.some((el) => isTokenColor(fill(el), '--c-bg') && isTokenColor(stroke(el), '--c-line-strong'))).toBe(true)
    expect(
      shapes.some(
        (el) => isTokenColor(fill(el), '--c-surface') && isTokenColor(stroke(el), '--c-line-strong') && css(el, 'stroke-dasharray') === '4 3',
      ),
    ).toBe(true)
  })

  // ---------- SC-37 ----------

  test.each([
    ['a', '「구역 위치 보기」 → 「확인」', false],
    ['b', '「구역 위치 보기」 → 브라우저 뒤로 가기로 닫기', false],
    ['c', '(StrictMode) 「구역 위치 보기」 → 「확인」', true],
  ])('SC-37 (boundary) 시트를 열고 닫는 일이 뒤로 가기 칸을 남기지 않는다 — %s %s', async (row, _label, strict) => {
    // Given 홈(/)에서 /performances/7 로 들어와 P_OPEN 상세가 보인다. 표의 방법으로 시트를 열고 닫았다
    const dialog = await openSheet(SECTIONS_P, ['/', '/performances/7'], strict)
    if (row === 'b') await browserBack()
    else await userEvent.click(within(dialog).getByRole('button', { name: '확인' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    // When 브라우저 뒤로 가기를 한 번 더 누른다
    await browserBack()

    // Then 주소가 / 다 — 시트를 열고 닫는 데 쓴 칸이 남아 상세에 한 번 더 머물지 않는다
    expect(where()).toBe('/')
  })
})
