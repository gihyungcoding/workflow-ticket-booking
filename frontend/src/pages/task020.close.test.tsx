import { act, fireEvent, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
  ASYNC_HISTORY_MS,
  DETAIL,
  P_OPEN,
  browserBack,
  renderAt,
  res,
  resetDocumentVisibility,
  stubServer,
  where,
} from '../test/task020'

// TASK-020 시나리오 SC-41 — 시트 닫기 입력이 반복되거나 시트가 이미 열린 채 들어온 주소에서도 상세를 떠나지 않는다.
// 비동기 history = 한 칸 뒤로 가기가 ASYNC_HISTORY_MS 뒤에 반영된다(브라우저 history.go 와 같다) — 하네스 renderAt({ asyncHistory }).
// SoT: workflow_design/05_scenario/SCENARIO_TASK-020.md

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  resetDocumentVisibility()
  localStorage.clear()
})

const LIST = { 'GET /api/performances': () => res(200, { content: [], page: 0, size: 20, totalElements: 0 }) }
const DETAIL_H1 = { level: 1, name: '지금 예매할 수 있어요' } as const
const pause = (ms: number) => act(async () => { await new Promise((r) => setTimeout(r, ms)) })
// 첫 입력의 뒤로 가기가 반영되기 전(타이머 0 이후, popstate 이전)의 틈
const GAP = 5
const SETTLE = ASYNC_HISTORY_MS * 3

const esc = () => fireEvent.keyDown(document, { key: 'Escape' })
const backdrop = () => fireEvent.click(screen.getByTestId('sheet-backdrop'))
const confirm = () => fireEvent.click(screen.getByRole('button', { name: '확인' }))

type Input = () => void
interface Row {
  entries: Array<string | { pathname: string; state: unknown }>
  asyncHistory: boolean
  /** 이 화면에서 「구역 위치 보기」를 눌러 열었다(true) / 열린 채 들어왔다(false) */
  pressed: boolean
  inputs: Input[]
  /** 입력 사이를 비동기 history 의 반영 전(GAP)만큼 띄운다 */
  spaced: boolean
  sameTick: boolean
  afterBack: string
}
const SHEET = { zoneSheet: true }
const PUSHED = ['/', '/performances/7']
const ROWS: Record<string, [string, Row]> = {
  a: ['동기 history · Esc 를 한 틱에 두 번', { entries: PUSHED, asyncHistory: false, pressed: true, inputs: [esc, esc], spaced: false, sameTick: true, afterBack: '/' }],
  b: ['비동기 history · Esc 두 번(첫 반영 전)', { entries: PUSHED, asyncHistory: true, pressed: true, inputs: [esc, esc], spaced: true, sameTick: false, afterBack: '/' }],
  c: ['비동기 history · 흐린 바탕 두 번', { entries: PUSHED, asyncHistory: true, pressed: true, inputs: [backdrop, backdrop], spaced: true, sameTick: false, afterBack: '/' }],
  d: ['비동기 history · 「확인」 두 번', { entries: PUSHED, asyncHistory: true, pressed: true, inputs: [confirm, confirm], spaced: true, sameTick: false, afterBack: '/' }],
  e: ['비동기 history · 「확인」 뒤 Esc', { entries: PUSHED, asyncHistory: true, pressed: true, inputs: [confirm, esc], spaced: true, sameTick: false, afterBack: '/' }],
  f: ['비동기 history · Esc 뒤 흐린 바탕', { entries: PUSHED, asyncHistory: true, pressed: true, inputs: [esc, backdrop], spaced: true, sameTick: false, afterBack: '/' }],
  g: ['시트가 열린 한 칸뿐 · 「확인」', { entries: [{ pathname: '/performances/7', state: SHEET }], asyncHistory: false, pressed: false, inputs: [confirm], spaced: false, sameTick: false, afterBack: '/performances/7' }],
  h: ['시트가 열린 한 칸뿐 · Esc 두 번(같은 틱)', { entries: [{ pathname: '/performances/7', state: SHEET }], asyncHistory: false, pressed: false, inputs: [esc, esc], spaced: false, sameTick: true, afterBack: '/performances/7' }],
  i: ['[/, 시트가 열린 상세] · 「확인」', { entries: ['/', { pathname: '/performances/7', state: SHEET }], asyncHistory: false, pressed: false, inputs: [confirm], spaced: false, sameTick: false, afterBack: '/' }],
}

describe('TASK-020 SC-41 시트 닫기 입력 반복·열린 채 들어온 주소', () => {
  test.each(Object.entries(ROWS).map(([row, [label, spec]]) => [row, label, spec] as const))(
    'SC-41 (boundary) 닫기 입력이 반복되거나 시트가 열린 채 들어온 주소에서도 상세를 떠나지 않는다 — %s %s',
    async (_row, _label, spec) => {
      // Given 시트가 열려 있다 — 경위·history 는 표의 Given 이다
      stubServer({ [DETAIL]: () => res(200, P_OPEN()), ...LIST })
      renderAt(spec.entries, { asyncHistory: spec.asyncHistory })
      await screen.findByRole('heading', DETAIL_H1)
      if (spec.pressed) await userEvent.click(screen.getByText('구역 위치 보기'))
      expect(screen.getByRole('dialog', { name: '구역 위치' })).toBeInTheDocument()

      // When 표의 닫는 입력을 준다
      if (spec.sameTick) {
        await act(async () => spec.inputs.forEach((input) => input()))
      } else {
        for (const [i, input] of spec.inputs.entries()) {
          if (i > 0 && spec.spaced) await pause(GAP)
          await act(async () => input())
        }
      }
      await pause(SETTLE)

      // Then dialog 가 사라지고 주소는 /performances/7 이며 상세 화면이 그대로 있다
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(where()).toBe('/performances/7')
      expect(screen.getByRole('heading', DETAIL_H1)).toBeInTheDocument()
      // And 이어서 브라우저 뒤로 가기를 한 번 누르면 표의 주소다
      await browserBack()
      await pause(SETTLE)
      expect(where()).toBe(spec.afterBack)
    },
  )
})
